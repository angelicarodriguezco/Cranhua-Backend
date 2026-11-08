package com.tuanistack.cranhua.finca;

import com.tuanistack.cranhua.cuenta.Cuenta;
import com.tuanistack.cranhua.cuenta.CuentaRepository;
import com.tuanistack.cranhua.cuenta.Rol;
import com.tuanistack.cranhua.finca.dto.ConflictoFinca;
import com.tuanistack.cranhua.finca.dto.Coordenada;
import com.tuanistack.cranhua.finca.dto.CosechaResponse;
import com.tuanistack.cranhua.finca.dto.FincaResponse;
import com.tuanistack.cranhua.finca.dto.RegistrarFincaRequest;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.locationtech.jts.geom.Polygon;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FincaService {

  private final FincaRepository fincaRepository;
  private final CuentaRepository cuentaRepository;
  private final CosechaRepository cosechaRepository;

  public FincaService(
      FincaRepository fincaRepository,
      CuentaRepository cuentaRepository,
      CosechaRepository cosechaRepository) {
    this.fincaRepository = fincaRepository;
    this.cuentaRepository = cuentaRepository;
    this.cosechaRepository = cosechaRepository;
  }

  @Transactional
  public FincaResponse registrar(RegistrarFincaRequest request, UUID cuentaId) {
    Cuenta dueno =
        cuentaRepository
            .findById(cuentaId)
            .filter(cuenta -> !cuenta.isEliminada())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

    Polygon perimetro = validarPerimetro(request);
    fincaRepository
        .buscarSuperpuesta(perimetro)
        .ifPresent(
            otra -> {
              throw new FincaSuperpuestaException(
                  new ConflictoFinca(
                      otra.getId(),
                      otra.getNombre(),
                      FincaResponse.duenos(otra),
                      GeometriaUtil.puntos(otra.getPerimetro())));
            });

    Finca finca =
        new Finca(
            request.nombre(),
            perimetro,
            GeometriaUtil.punto(request.ubicacion()),
            request.canton(),
            request.provincia());
    finca.setDescripcion(request.descripcion());
    finca.setZonaCafetalera(request.zonaCafetalera());
    finca.agregarDueno(dueno);
    finca = fincaRepository.saveAndFlush(finca);

    return FincaResponse.de(finca, fincaRepository.areaHectareas(finca.getId()));
  }

  @Transactional(readOnly = true)
  public List<FincaResponse> misFincas(UUID cuentaId) {
    List<Finca> fincas =
        fincaRepository.findByDuenosIdAndEliminadaFalseOrderByNombreAscIdAsc(cuentaId);
    if (fincas.isEmpty()) {
      return List.of();
    }
    List<UUID> ids = fincas.stream().map(Finca::getId).toList();
    Map<UUID, BigDecimal> areas = new HashMap<>();
    fincaRepository
        .areasHectareas(ids)
        .forEach(fila -> areas.put((UUID) fila[0], (BigDecimal) fila[1]));
    Map<UUID, Object[]> resumenes = new HashMap<>();
    cosechaRepository
        .resumenDisponibles(ids, EstadoCosecha.DISPONIBLE)
        .forEach(fila -> resumenes.put((UUID) fila[0], fila));

    return fincas.stream()
        .map(
            finca -> {
              Object[] resumen = resumenes.get(finca.getId());
              return FincaResponse.de(finca, areas.get(finca.getId()))
                  .conResumen(
                      resumen == null ? 0 : (Long) resumen[1],
                      resumen == null ? BigDecimal.ZERO : (BigDecimal) resumen[2]);
            })
        .toList();
  }

  @Transactional(readOnly = true)
  public FincaResponse obtener(UUID fincaId, UUID cuentaId) {
    Finca finca = fincaVisible(fincaId, cuentaId);
    return FincaResponse.de(finca, fincaRepository.areaHectareas(finca.getId()))
        .conCosechas(cosechaRepository.findByFincaIdOrderByFechaInicioDescIdAsc(fincaId));
  }

  @Transactional(readOnly = true)
  public List<CosechaResponse> cosechas(UUID fincaId, UUID cuentaId) {
    fincaVisible(fincaId, cuentaId);
    return cosechaRepository.findByFincaIdOrderByFechaInicioDescIdAsc(fincaId).stream()
        .map(CosechaResponse::de)
        .toList();
  }

  private Finca fincaVisible(UUID fincaId, UUID cuentaId) {
    Finca finca = fincaExistente(fincaId);
    if (!finca.esDueno(cuentaId)) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Solo un dueño de la finca puede verla.");
    }
    return finca;
  }

  @Transactional
  public FincaResponse agregarDueno(UUID fincaId, String correo, UUID cuentaId) {
    Finca finca = fincaAdministrable(fincaId, cuentaId);
    Cuenta nuevo =
        cuentaRepository
            .findByCorreoAndEliminadaFalse(Cuenta.normalizarCorreo(correo))
            .filter(cuenta -> cuenta.getRoles().contains(Rol.CAFETALERO))
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No encontramos una cuenta de cafetalero con ese correo."));
    if (finca.esDueno(nuevo.getId())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Esa cuenta ya es dueña de esta finca.");
    }
    finca.agregarDueno(nuevo);
    return FincaResponse.de(finca, fincaRepository.areaHectareas(finca.getId()));
  }

  @Transactional
  public FincaResponse quitarDueno(UUID fincaId, UUID duenoId, UUID cuentaId) {
    Finca finca = fincaAdministrable(fincaId, cuentaId);
    if (!finca.esDueno(duenoId)) {
      throw new ResponseStatusException(
          HttpStatus.NOT_FOUND, "Esa cuenta no es dueña de esta finca.");
    }
    finca.quitarDueno(duenoId);
    return FincaResponse.de(finca, fincaRepository.areaHectareas(finca.getId()));
  }

  private Finca fincaExistente(UUID fincaId) {
    return fincaRepository
        .findByIdAndEliminadaFalse(fincaId)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No encontramos esa finca."));
  }

  private Finca fincaAdministrable(UUID fincaId, UUID cuentaId) {
    Finca finca = fincaExistente(fincaId);
    if (!finca.esDueno(cuentaId)) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Solo un dueño de la finca puede administrar sus dueños.");
    }
    return finca;
  }

  private static Polygon validarPerimetro(RegistrarFincaRequest request) {
    // Si viene cerrado, el punto repetido no cuenta como otro punto
    if (new HashSet<Coordenada>(request.perimetro()).size() < 3) {
      throw new PerimetroInvalidoException("El perímetro necesita al menos 3 puntos.");
    }
    Polygon perimetro = GeometriaUtil.poligono(request.perimetro());
    if (!perimetro.isValid()) {
      throw new PerimetroInvalidoException("El perímetro tiene líneas que se cruzan.");
    }
    return perimetro;
  }
}
