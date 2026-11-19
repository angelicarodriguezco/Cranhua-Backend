package com.tuanistack.cranhua.solicitud.dto;

import com.tuanistack.cranhua.solicitud.EstadoSolicitud;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SolicitudRecibidaResponse(
    UUID id,
    UUID cosechaId,
    String nombreTostador,
    String variedad,
    String finca,
    BigDecimal cantidadKg,
    Instant solicitadaEn,
    EstadoSolicitud estado) {}
