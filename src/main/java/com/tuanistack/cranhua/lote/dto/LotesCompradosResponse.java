package com.tuanistack.cranhua.lote.dto;

import java.math.BigDecimal;
import java.util.List;

public record LotesCompradosResponse(Indicadores indicadores, List<LoteCompradoResponse> lotes) {

  public record Indicadores(int lotesComprados, BigDecimal kgComprados, long lotesConCodigo) {}
}
