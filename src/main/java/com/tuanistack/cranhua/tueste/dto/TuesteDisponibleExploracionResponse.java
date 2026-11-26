package com.tuanistack.cranhua.tueste.dto;

import com.tuanistack.cranhua.tueste.NivelTueste;
import java.math.BigDecimal;
import java.util.UUID;

public record TuesteDisponibleExploracionResponse(
    UUID id, String fincaOrigen, String variedad, NivelTueste nivel, BigDecimal kgDisponibles) {}
