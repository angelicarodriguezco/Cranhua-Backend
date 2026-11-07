package com.tuanistack.cranhua.finca;

import com.tuanistack.cranhua.cuenta.Cuenta;
import com.tuanistack.cranhua.cuenta.CuentaRepository;
import com.tuanistack.cranhua.finca.dto.ConflictoFinca;
import com.tuanistack.cranhua.finca.dto.Coordenada;
import com.tuanistack.cranhua.finca.dto.FincaResponse;
import com.tuanistack.cranhua.finca.dto.RegistrarFincaRequest;
import java.util.HashSet;
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

  public FincaService(FincaRepository fincaRepository, CuentaRepository cuentaRepository) {
    this.fincaRepository = fincaRepository;
    this.cuentaRepository = cuentaRepository;
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
        .ifPresent(otra -> {
          throw new FincaSuperpuestaException(
              new ConflictoFinca(
                  otra.getId(),
                  otra.getNombre(),
                  otra.getDuenos().stream().map(Cuenta::getNombreCompleto).sorted().toList()));
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
