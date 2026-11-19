package com.tuanistack.cranhua.solicitud.dto;

import com.tuanistack.cranhua.solicitud.EstadoSolicitud;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SolicitudEnviadaResponse(
    UUID id,
    UUID cosechaId,
    UUID fincaId,
    String finca,
    String variedad,
    // Solo viene cuando la solicitud se aprobó y generó un lote
    String codigoLote,
    BigDecimal cantidadKg,
    Instant solicitadaEn,
    EstadoSolicitud estado) {}
