package com.tuanistack.cranhua.historial.dto;

import java.math.BigDecimal;

public record HistorialCafetaleroResponse(
    Indicadores indicadores,
    Pagina<CosechaHistorialResponse> cosechas,
    Pagina<LoteVendidoResponse> lotesVendidos) {

  public record Indicadores(
      long cosechasRegistradas, BigDecimal produccionTotalKg, long lotesVendidos) {}
}
