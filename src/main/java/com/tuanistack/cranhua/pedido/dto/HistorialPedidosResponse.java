package com.tuanistack.cranhua.pedido.dto;

import java.math.BigDecimal;
import java.util.List;

public record HistorialPedidosResponse(
    Indicadores indicadores, List<PedidoRecienteResponse> pedidos) {

  public record Indicadores(
      long pedidosActivos, BigDecimal kgRecibidosEsteMes, long tostadoresActivos) {}
}
