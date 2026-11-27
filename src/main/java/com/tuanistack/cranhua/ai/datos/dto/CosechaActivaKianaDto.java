package com.tuanistack.cranhua.ai.datos.dto;

import com.tuanistack.cranhua.finca.Proceso;
import java.math.BigDecimal;

public record CosechaActivaKianaDto(
    String variedad,
    Proceso proceso,
    Integer altura,
    BigDecimal kgDisponibles,
    String codigoLote,
    String finca) {}
