package com.tuanistack.cranhua.finca.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tuanistack.cranhua.finca.Cosecha;
import com.tuanistack.cranhua.finca.EstadoCosecha;
import com.tuanistack.cranhua.finca.Proceso;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CosechaResponse(
    UUID id,
    UUID fincaId,
    String variedad,
    Integer alturaMsnm,
    LocalDate fechaInicio,
    LocalDate fechaFin,
    List<Reparto> procesos,
    BigDecimal cantidadCosechadaKg,
    BigDecimal pesoTrasBeneficiadoKg,
    BigDecimal pesoTrasSeleccionKg,
    BigDecimal caracolilloKg,
    String notasSabor,
    BigDecimal puntajeSca,
    BigDecimal precioKg,
    BigDecimal kgDisponibles,
    EstadoCosecha estado,
    String codigoLote,
    // Solo viene al consultar una cosecha suelta
    @JsonInclude(JsonInclude.Include.NON_NULL) String fincaNombre) {

  public record Reparto(Proceso proceso, Integer porcentaje) {}

  public static CosechaResponse de(Cosecha cosecha) {
    return de(cosecha, null);
  }

  public static CosechaResponse conFinca(Cosecha cosecha) {
    return de(cosecha, cosecha.getFinca().getNombre());
  }

  private static CosechaResponse de(Cosecha cosecha, String fincaNombre) {
    return new CosechaResponse(
        cosecha.getId(),
        cosecha.getFinca().getId(),
        cosecha.getVariedad(),
        cosecha.getAlturaMsnm(),
        cosecha.getFechaInicio(),
        cosecha.getFechaFin(),
        cosecha.getProcesos().stream()
            .map(p -> new Reparto(p.getProceso(), p.getPorcentaje()))
            .toList(),
        cosecha.getCantidadCosechadaKg(),
        cosecha.getPesoTrasBeneficiadoKg(),
        cosecha.getPesoTrasSeleccionKg(),
        cosecha.getCaracolilloKg(),
        cosecha.getNotasSabor(),
        cosecha.getPuntajeSca(),
        cosecha.getPrecioKg(),
        cosecha.getKgDisponibles(),
        cosecha.getEstado(),
        cosecha.getCodigoLote(),
        fincaNombre);
  }
}
