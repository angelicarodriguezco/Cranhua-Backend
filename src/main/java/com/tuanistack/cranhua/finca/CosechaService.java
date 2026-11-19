package com.tuanistack.cranhua.finca;

import com.tuanistack.cranhua.finca.dto.ActualizarEstadoCosechaRequest;
import com.tuanistack.cranhua.finca.dto.CosechaResponse;
import com.tuanistack.cranhua.finca.dto.RegistrarCosechaRequest;
import com.tuanistack.cranhua.finca.dto.RegistrarCosechaRequest.RepartoRequest;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CosechaService {

  private final CosechaRepository cosechaRepository;
  private final FincaRepository fincaRepository;

  public CosechaService(CosechaRepository cosechaRepository, FincaRepository fincaRepository) {
    this.cosechaRepository = cosechaRepository;
    this.fincaRepository = fincaRepository;
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
            request.procesos().stream()
                .map(p -> new ProcesoReparto(p.proceso(), p.porcentaje()))
                .toList());
    cosecha.setPesoTrasBeneficiadoKg(request.pesoTrasBeneficiadoKg());
    cosecha.setCaracolilloKg(request.caracolilloKg());
    cosecha.setNotasSabor(request.notasSabor());
    cosecha.setPuntajeSca(request.puntajeSca());
    if (request.pesoTrasSeleccionKg() != null) {
      cosecha.registrarPesoTrasSeleccion(request.pesoTrasSeleccionKg());
    }
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
      }
    } catch (IllegalStateException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    return CosechaResponse.conFinca(cosecha);
  }

  private Cosecha cosechaVisible(UUID cosechaId) {
    return cosechaRepository
        .findById(cosechaId)
        .filter(c -> !c.getFinca().isEliminada())
        .orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "No encontramos esa cosecha."));
  }

  private static void validar(RegistrarCosechaRequest request) {
    Map<String, String> errores = new LinkedHashMap<>();

    long distintos = request.procesos().stream().map(RepartoRequest::proceso).distinct().count();
    int total = request.procesos().stream().mapToInt(RepartoRequest::porcentaje).sum();
    if (distintos < request.procesos().size()) {
      errores.put("procesos", "No repitás un proceso.");
    } else if (total != 100) {
      errores.put("procesos", "Los porcentajes de los procesos tienen que sumar 100.");
    }

    if (request.fechaFin().isBefore(request.fechaInicio())) {
      errores.put("fechaFin", "La fecha de fin no puede ser antes de la de inicio.");
    }

    BigDecimal beneficiado = request.pesoTrasBeneficiadoKg();
    BigDecimal seleccion = request.pesoTrasSeleccionKg();
    BigDecimal caracolillo = request.caracolilloKg();
    if (beneficiado != null && beneficiado.compareTo(request.cantidadCosechadaKg()) > 0) {
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
