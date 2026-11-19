package com.tuanistack.cranhua.lote.dto;

import com.tuanistack.cranhua.lote.EstadoTrazabilidad;
import com.tuanistack.cranhua.lote.Lote;
import com.tuanistack.cranhua.solicitud.Solicitud;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

public record LoteCompradoResponse(
    UUID id,
    String codigo,
    String finca,
    String variedad,
    BigDecimal kg,
    LocalDate fechaCompra,
    EstadoTrazabilidad estado) {

  private static final ZoneId ZONA = ZoneId.of("America/Costa_Rica");

  public static LoteCompradoResponse de(Lote lote) {
    Solicitud solicitud = lote.getSolicitud();
    return new LoteCompradoResponse(
        lote.getId(),
        lote.getCodigo(),
        solicitud.getCosecha().getFinca().getNombre(),
        solicitud.getCosecha().getVariedad(),
        solicitud.getCantidadKg(),
        LocalDate.ofInstant(solicitud.getRespondidaEn(), ZONA),
        lote.getEstadoTrazabilidad());
  }
}
