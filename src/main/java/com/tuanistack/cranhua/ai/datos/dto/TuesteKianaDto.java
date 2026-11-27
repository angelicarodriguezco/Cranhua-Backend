package com.tuanistack.cranhua.ai.datos.dto;

import com.tuanistack.cranhua.tueste.NivelTueste;
import java.math.BigDecimal;

public record TuesteKianaDto(
    NivelTueste nivel,
    String fincaOrigen,
    String variedad,
    BigDecimal kgDisponibles,
    String tostador) {}
