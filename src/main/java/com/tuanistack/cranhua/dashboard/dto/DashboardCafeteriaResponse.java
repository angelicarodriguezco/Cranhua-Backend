package com.tuanistack.cranhua.dashboard.dto;

import com.tuanistack.cranhua.pedido.dto.PedidoRecienteResponse;
import java.math.BigDecimal;
import java.util.List;

public record DashboardCafeteriaResponse(
    Indicadores indicadores, List<PedidoRecienteResponse> pedidosRecientes) {

  public record Indicadores(
      BigDecimal cafeRecibidoKg, long pedidosActivos, long tostadoresActivos) {}
}
