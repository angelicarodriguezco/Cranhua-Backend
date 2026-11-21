package com.tuanistack.cranhua.historial.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LoteVendidoResponse(
    UUID id,
    String codigo,
    UUID cosechaId,
    UUID fincaId,
    String finca,
    String variedad,
    String comprador,
    BigDecimal kg,
    BigDecimal precioKg,
    BigDecimal total,
    Instant vendidoEn) {}
