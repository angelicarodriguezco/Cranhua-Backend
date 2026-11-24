package com.tuanistack.cranhua.finca;

import com.tuanistack.cranhua.finca.dto.ActualizarEstadoCosechaRequest;
import com.tuanistack.cranhua.finca.dto.CosechaResponse;
import com.tuanistack.cranhua.finca.dto.DatosCosecha;
import com.tuanistack.cranhua.finca.dto.EditarCosechaRequest;
import com.tuanistack.cranhua.finca.dto.RegistrarCosechaRequest;
import com.tuanistack.cranhua.finca.dto.RegistrarCosechaRequest.RepartoRequest;
import com.tuanistack.cranhua.lote.LoteRepository;
import com.tuanistack.cranhua.solicitud.SolicitudService;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CosechaService {

  private static final String CON_LOTES_EDITAR =
      "Esta cosecha ya tiene lotes comprados. Solo podés cambiar el precio y las notas de sabor.";
  private static final String CAMPO_BLOQUEADO =
      "No se puede cambiar porque ya está en códigos impresos.";
  private static final String CON_LOTES_ELIMINAR =
      "No podés eliminar esta cosecha porque ya tiene lotes comprados con código de"
          + " trazabilidad. Si no querés recibir más solicitudes, retirala.";

  private final CosechaRepository cosechaRepository;
  private final FincaRepository fincaRepository;
  private final LoteRepository loteRepository;
  private final SolicitudService solicitudService;

  public CosechaService(
      CosechaRepository cosechaRepository,
      FincaRepository fincaRepository,
      LoteRepository loteRepository,
      SolicitudService solicitudService) {
    this.cosechaRepository = cosechaRepository;
    this.fincaRepository = fincaRepository;
    this.loteRepository = loteRepository;
    this.solicitudService = solicitudService;
  }

  @Transactional
  public CosechaResponse registrar(RegistrarCosechaRequest request, UUID cuentaId) {
    Finca finca =
        fincaRepository
            .findByIdAndEliminadaFalse(request.fincaId())
            .orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "No encontramos esa finca."));
    if (!finca.esDueno(cuentaId)) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Solo un dueño de la finca puede registrar cosechas en ella.");
    }
    // El constructor de Cosecha tira IllegalArgumentException con porcentajes inválidos
    validar(request);

    Cosecha cosecha =
        new Cosecha(
            finca,
            request.variedad(),
            request.alturaMsnm(),
            request.fechaInicio(),
            request.fechaFin(),
            request.cantidadCosechadaKg(),
            request.precioKg(),
            procesos(request));
    aplicarPesosYCatacion(cosecha, request);
    cosecha = cosechaRepository.saveAndFlush(cosecha);

    return CosechaResponse.conFinca(cosecha);
  }

  @Transactional(readOnly = true)
  public CosechaResponse obtener(UUID cosechaId, UUID cuentaId) {
    Cosecha cosecha = cosechaVisible(cosechaId);
    if (!cosecha.getFinca().esDueno(cuentaId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Esta cosecha no es tuya.");
    }
    return CosechaResponse.conFinca(cosecha);
  }

  @Transactional
  public CosechaResponse editar(UUID cosechaId, EditarCosechaRequest request, UUID cuentaId) {
    Cosecha cosecha = cosechaParaModificar(cosechaId);
    if (!cosecha.getFinca().esDueno(cuentaId)) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Solo un dueño de la finca puede editar esta cosecha.");
    }
    validar(request);

    // Las solicitudes conservan su propio precioKg, así que cambiar el precio no las toca
    if (loteRepository.existsBySolicitudCosechaId(cosechaId)) {
      Map<String, String> bloqueados = camposCambiados(cosecha, request);
      if (!bloqueados.isEmpty()) {
        throw new CosechaBloqueadaException(CON_LOTES_EDITAR, bloqueados);
      }
      cosecha.setPrecioKg(request.precioKg());
      cosecha.setNotasSabor(request.notasSabor());
      return CosechaResponse.conFinca(cosechaRepository.saveAndFlush(cosecha));
    }

    cosecha.setVariedad(request.variedad());
    cosecha.setAlturaMsnm(request.alturaMsnm());
    cosecha.setFechaInicio(request.fechaInicio());
    cosecha.setFechaFin(request.fechaFin());
    cosecha.setCantidadCosechadaKg(request.cantidadCosechadaKg());
    cosecha.setPrecioKg(request.precioKg());
    cosecha.setProcesos(procesos(request));
    aplicarPesosYCatacion(cosecha, request);
    if (cosecha.getKgDisponibles() == null) {
      solicitudService.rechazarPendientesDe(cosechaId);
    }
    return CosechaResponse.conFinca(cosechaRepository.saveAndFlush(cosecha));
  }

  @Transactional
  public void eliminar(UUID cosechaId, UUID cuentaId) {
    Cosecha cosecha = cosechaParaModificar(cosechaId);
    if (!cosecha.getFinca().esDueno(cuentaId)) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Solo un dueño de la finca puede eliminar esta cosecha.");
    }
    if (loteRepository.existsBySolicitudCosechaId(cosechaId)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, CON_LOTES_ELIMINAR);
    }
    cosecha.eliminar();
    solicitudService.rechazarPendientesDe(cosechaId);
  }

  @Transactional
  public CosechaResponse actualizarEstado(
      UUID cosechaId, ActualizarEstadoCosechaRequest request, UUID cuentaId) {
    Cosecha cosecha = cosechaVisible(cosechaId);
    if (!cosecha.getFinca().esDueno(cuentaId)) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Solo un dueño de la finca puede publicar o retirar esta cosecha.");
    }

    EstadoCosecha nuevo = request.estado();
    if (nuevo != EstadoCosecha.PUBLICADA && nuevo != EstadoCosecha.BORRADOR) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Solo se puede publicar o retirar una cosecha.");
    }

    try {
      if (nuevo == EstadoCosecha.PUBLICADA) {
        cosecha.publicar();
      } else {
        cosecha.retirar();
        solicitudService.rechazarPendientesDe(cosechaId);
      }
    } catch (IllegalStateException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    return CosechaResponse.conFinca(cosecha);
  }

  private Cosecha cosechaVisible(UUID cosechaId) {
    return cosechaRepository
        .findById(cosechaId)
        .filter(CosechaService::esVisible)
        .orElseThrow(CosechaService::noEncontrada);
  }

  // Bloquea la fila igual que al aprobar una solicitud, para no pisar un descuento de kg
  private Cosecha cosechaParaModificar(UUID cosechaId) {
    Cosecha cosecha = cosechaRepository.findParaDescontar(cosechaId);
    if (cosecha == null || !esVisible(cosecha)) {
      throw noEncontrada();
    }
    return cosecha;
  }

  private static boolean esVisible(Cosecha cosecha) {
    return !cosecha.isEliminada() && !cosecha.getFinca().isEliminada();
  }

  private static ResponseStatusException noEncontrada() {
    return new ResponseStatusException(HttpStatus.NOT_FOUND, "No encontramos esa cosecha.");
  }

  private static List<ProcesoReparto> procesos(DatosCosecha datos) {
    return datos.procesos().stream()
        .map(p -> new ProcesoReparto(p.proceso(), p.porcentaje()))
        .toList();
  }

  private static void aplicarPesosYCatacion(Cosecha cosecha, DatosCosecha datos) {
    cosecha.setPesoTrasBeneficiadoKg(datos.pesoTrasBeneficiadoKg());
    cosecha.setCaracolilloKg(datos.caracolilloKg());
    cosecha.setNotasSabor(datos.notasSabor());
    cosecha.setPuntajeSca(datos.puntajeSca());
    cosecha.registrarPesoTrasSeleccion(datos.pesoTrasSeleccionKg());
  }

  private static Map<String, String> camposCambiados(Cosecha cosecha, DatosCosecha datos) {
    Map<String, String> errores = new LinkedHashMap<>();
    if (!datos.variedad().strip().equals(cosecha.getVariedad())) {
      errores.put("variedad", CAMPO_BLOQUEADO);
    }
    if (!datos.alturaMsnm().equals(cosecha.getAlturaMsnm())) {
      errores.put("alturaMsnm", CAMPO_BLOQUEADO);
    }
    if (!datos.fechaInicio().equals(cosecha.getFechaInicio())) {
      errores.put("fechaInicio", CAMPO_BLOQUEADO);
    }
    if (!datos.fechaFin().equals(cosecha.getFechaFin())) {
      errores.put("fechaFin", CAMPO_BLOQUEADO);
    }
    Map<Proceso, Integer> procesosActuales =
        cosecha.getProcesos().stream()
            .collect(Collectors.toMap(ProcesoReparto::getProceso, ProcesoReparto::getPorcentaje));
    Map<Proceso, Integer> procesosNuevos =
        datos.procesos().stream()
            .collect(Collectors.toMap(RepartoRequest::proceso, RepartoRequest::porcentaje));
    if (!procesosNuevos.equals(procesosActuales)) {
      errores.put("procesos", CAMPO_BLOQUEADO);
    }
    if (!mismoValor(datos.cantidadCosechadaKg(), cosecha.getCantidadCosechadaKg())) {
      errores.put("cantidadCosechadaKg", CAMPO_BLOQUEADO);
    }
    if (!mismoValor(datos.pesoTrasBeneficiadoKg(), cosecha.getPesoTrasBeneficiadoKg())) {
      errores.put("pesoTrasBeneficiadoKg", CAMPO_BLOQUEADO);
    }
    if (!mismoValor(datos.pesoTrasSeleccionKg(), cosecha.getPesoTrasSeleccionKg())) {
      errores.put("pesoTrasSeleccionKg", CAMPO_BLOQUEADO);
    }
    if (!mismoValor(datos.caracolilloKg(), cosecha.getCaracolilloKg())) {
      errores.put("caracolilloKg", CAMPO_BLOQUEADO);
    }
    if (!mismoValor(datos.puntajeSca(), cosecha.getPuntajeSca())) {
      errores.put("puntajeSca", CAMPO_BLOQUEADO);
    }
    return errores;
  }

  private static boolean mismoValor(BigDecimal nuevo, BigDecimal actual) {
    if (nuevo == null || actual == null) {
      return Objects.equals(nuevo, actual);
    }
    return nuevo.compareTo(actual) == 0;
  }

  private static void validar(DatosCosecha datos) {
    Map<String, String> errores = new LinkedHashMap<>();

    long distintos = datos.procesos().stream().map(RepartoRequest::proceso).distinct().count();
    int total = datos.procesos().stream().mapToInt(RepartoRequest::porcentaje).sum();
    if (distintos < datos.procesos().size()) {
      errores.put("procesos", "No repitás un proceso.");
    } else if (total != 100) {
      errores.put("procesos", "Los porcentajes de los procesos tienen que sumar 100.");
    }

    if (datos.fechaFin().isBefore(datos.fechaInicio())) {
      errores.put("fechaFin", "La fecha de fin no puede ser antes de la de inicio.");
    }

    BigDecimal beneficiado = datos.pesoTrasBeneficiadoKg();
    BigDecimal seleccion = datos.pesoTrasSeleccionKg();
    BigDecimal caracolillo = datos.caracolilloKg();
    if (beneficiado != null && beneficiado.compareTo(datos.cantidadCosechadaKg()) > 0) {
      errores.put(
          "pesoTrasBeneficiadoKg",
          "El peso tras el beneficiado no puede ser mayor a la cantidad cosechada.");
    }
    if (seleccion != null) {
      if (beneficiado == null) {
        errores.put("pesoTrasSeleccionKg", "Ingresá primero el peso tras el beneficiado.");
      } else if (seleccion.compareTo(beneficiado) > 0) {
        errores.put(
            "pesoTrasSeleccionKg",
            "El peso tras la selección no puede ser mayor al peso tras el beneficiado.");
      }
    }
    if (caracolillo != null) {
      if (seleccion == null) {
        errores.put("caracolilloKg", "Ingresá primero el peso tras la selección.");
      } else if (caracolillo.compareTo(seleccion) > 0) {
        errores.put(
            "caracolilloKg", "El caracolillo no puede ser mayor al peso tras la selección.");
      }
    }

    if (!errores.isEmpty()) {
      throw new CosechaInvalidaException(errores);
    }
  }
}
