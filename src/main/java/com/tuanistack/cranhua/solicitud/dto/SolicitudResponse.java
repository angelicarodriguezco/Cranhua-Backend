package com.tuanistack.cranhua.solicitud.dto;

import com.tuanistack.cranhua.solicitud.EstadoSolicitud;
import com.tuanistack.cranhua.solicitud.Solicitud;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SolicitudResponse(
    UUID id,
    UUID cosechaId,
    BigDecimal cantidadKg,
    BigDecimal precioKg,
    BigDecimal totalEstimado,
    EstadoSolicitud estado,
    Instant solicitadaEn,
    Instant respondidaEn,
    // Solo viene cuando la solicitud se aprobó
    String codigoLote) {

  public static SolicitudResponse de(Solicitud solicitud, String codigoLote) {
    return new SolicitudResponse(
        solicitud.getId(),
        solicitud.getCosecha().getId(),
        solicitud.getCantidadKg(),
        solicitud.getPrecioKg(),
        solicitud.getTotalEstimado(),
        solicitud.getEstado(),
        solicitud.getSolicitadaEn(),
        solicitud.getRespondidaEn(),
        codigoLote);
  }
}
