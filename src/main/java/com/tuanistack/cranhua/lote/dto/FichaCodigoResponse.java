package com.tuanistack.cranhua.lote.dto;

import com.tuanistack.cranhua.lote.EstadoTrazabilidad;
import com.tuanistack.cranhua.lote.Lote;
import com.tuanistack.cranhua.solicitud.Solicitud;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FichaCodigoResponse(
    UUID id,
    String codigo,
    String finca,
    String variedad,
    BigDecimal kg,
    String tostador,
    EstadoTrazabilidad estado,
    Instant activadoEn) {

  public static FichaCodigoResponse de(Lote lote) {
    Solicitud solicitud = lote.getSolicitud();
    return new FichaCodigoResponse(
        lote.getId(),
        lote.getCodigo(),
        solicitud.getCosecha().getFinca().getNombre(),
        solicitud.getCosecha().getVariedad(),
        solicitud.getCantidadKg(),
        solicitud.getTostador().getNombreCompleto(),
        lote.getEstadoTrazabilidad(),
        lote.getActivadoEn());
  }
}
