package com.tuanistack.cranhua.finca.dto;

import com.tuanistack.cranhua.finca.EstadoCosecha;
import jakarta.validation.constraints.NotNull;

public record ActualizarEstadoCosechaRequest(
    @NotNull(message = "Ingresá el estado de la cosecha.") EstadoCosecha estado) {}
