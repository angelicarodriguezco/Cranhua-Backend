package com.tuanistack.cranhua.dashboard.dto;

import java.math.BigDecimal;

public record DashboardTostadorResponse(
    long solicitudesActivas, BigDecimal kgCompradosMes, long lotesConCodigo) {}
