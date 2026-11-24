package com.tuanistack.cranhua.pedido.dto;

import com.tuanistack.cranhua.pedido.EstadoPedido;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PedidoRecienteResponse(
    UUID id, String tostador, BigDecimal cantidadKg, Instant pedidoEn, EstadoPedido estado) {}
