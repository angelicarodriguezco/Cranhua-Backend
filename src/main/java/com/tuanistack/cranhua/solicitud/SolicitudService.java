package com.tuanistack.cranhua.solicitud;

import com.tuanistack.cranhua.finca.Cosecha;
import com.tuanistack.cranhua.finca.CosechaRepository;
import com.tuanistack.cranhua.lote.Lote;
import com.tuanistack.cranhua.lote.LoteRepository;
import com.tuanistack.cranhua.solicitud.dto.SolicitudResponse;
import java.math.BigDecimal;
import java.time.Year;
import java.time.ZoneId;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SolicitudService {

  private static final ZoneId COSTA_RICA = ZoneId.of("America/Costa_Rica");

  private final SolicitudRepository solicitudRepository;
  private final CosechaRepository cosechaRepository;
  private final LoteRepository loteRepository;

  public SolicitudService(
      SolicitudRepository solicitudRepository,
      CosechaRepository cosechaRepository,
      LoteRepository loteRepository) {
    this.solicitudRepository = solicitudRepository;
    this.cosechaRepository = cosechaRepository;
    this.loteRepository = loteRepository;
  }

  @Transactional
  public SolicitudResponse responder(UUID solicitudId, EstadoSolicitud respuesta, UUID cuentaId) {
    if (respuesta == EstadoSolicitud.PENDIENTE) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Solo se puede aprobar o rechazar una solicitud.");
    }

    Solicitud solicitud =
        solicitudRepository
            .findParaResponder(solicitudId)
            .orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "No encontramos esa solicitud."));
    // También se bloquea la cosecha: otras solicitudes aprobadas a la vez descuentan de ella
    Cosecha cosecha = cosechaRepository.findParaDescontar(solicitud.getCosecha().getId());
    if (cosecha.getFinca().isEliminada()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No encontramos esa solicitud.");
    }
    if (!cosecha.getFinca().esDueno(cuentaId)) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Solo un dueño de la finca puede responder esta solicitud.");
    }
    if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta solicitud ya fue respondida.");
    }

    if (respuesta == EstadoSolicitud.RECHAZADA) {
      solicitud.rechazar();
      return SolicitudResponse.de(solicitud, null);
    }

    BigDecimal disponibles =
        cosecha.getKgDisponibles() == null ? BigDecimal.ZERO : cosecha.getKgDisponibles();
    if (solicitud.getCantidadKg().compareTo(disponibles) > 0) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "No hay suficiente café disponible: la solicitud pide %s kg y quedan %s kg."
              .formatted(solicitud.getCantidadKg().toPlainString(), disponibles.toPlainString()));
    }
    cosecha.descontarDisponible(solicitud.getCantidadKg());
    solicitud.aprobar();
    String codigo =
        Lote.formatearCodigo(
            Year.now(COSTA_RICA).getValue(), loteRepository.siguienteNumeroCodigo());
    Lote lote = loteRepository.save(new Lote(solicitud, codigo));
    return SolicitudResponse.de(solicitud, lote.getCodigo());
  }
}
