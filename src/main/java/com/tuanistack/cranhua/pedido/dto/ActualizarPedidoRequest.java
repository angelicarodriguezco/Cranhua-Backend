package com.tuanistack.cranhua.pedido.dto;

import com.tuanistack.cranhua.pedido.EstadoPedido;
import jakarta.validation.constraints.NotNull;

public record ActualizarPedidoRequest(
    @NotNull(message = "Ingresá el estado del pedido.") EstadoPedido estado) {}
