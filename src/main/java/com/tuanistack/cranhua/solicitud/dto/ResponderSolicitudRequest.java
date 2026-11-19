package com.tuanistack.cranhua.solicitud.dto;

import com.tuanistack.cranhua.solicitud.EstadoSolicitud;
import jakarta.validation.constraints.NotNull;

public record ResponderSolicitudRequest(
    @NotNull(message = "Ingresá si aprobás o rechazás la solicitud.") EstadoSolicitud estado) {}
