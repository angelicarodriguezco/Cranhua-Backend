package com.tuanistack.cranhua.historial.dto;

import com.tuanistack.cranhua.finca.Cosecha;
import com.tuanistack.cranhua.finca.EstadoCosecha;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CosechaHistorialResponse(
    UUID id,
    UUID fincaId,
    String finca,
    String variedad,
    LocalDate fechaInicio,
    LocalDate fechaFin,
    BigDecimal cantidadCosechadaKg,
    EstadoCosecha estado) {

  public static CosechaHistorialResponse de(Cosecha cosecha) {
    return new CosechaHistorialResponse(
        cosecha.getId(),
        cosecha.getFinca().getId(),
        cosecha.getFinca().getNombre(),
        cosecha.getVariedad(),
        cosecha.getFechaInicio(),
        cosecha.getFechaFin(),
        cosecha.getCantidadCosechadaKg(),
        cosecha.getEstado());
  }
}
