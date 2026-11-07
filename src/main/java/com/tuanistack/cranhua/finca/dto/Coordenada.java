package com.tuanistack.cranhua.finca.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record Coordenada(
    @NotNull(message = "Falta la latitud.")
        @DecimalMin(value = "-90", message = "La latitud tiene que estar entre -90 y 90.")
        @DecimalMax(value = "90", message = "La latitud tiene que estar entre -90 y 90.")
        Double lat,
    @NotNull(message = "Falta la longitud.")
        @DecimalMin(value = "-180", message = "La longitud tiene que estar entre -180 y 180.")
        @DecimalMax(value = "180", message = "La longitud tiene que estar entre -180 y 180.")
        Double lng) {}
