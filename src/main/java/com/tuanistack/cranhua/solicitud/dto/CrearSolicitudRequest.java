package com.tuanistack.cranhua.solicitud.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record CrearSolicitudRequest(
    @NotNull(message = "Elegí la cosecha.") UUID cosechaId,
    @NotNull(message = "Ingresá la cantidad en kg.")
        @DecimalMin(value = "0.01", message = "La cantidad tiene que ser mayor a 0.")
        @Digits(integer = 8, fraction = 2, message = "La cantidad no es válida.")
        BigDecimal cantidadKg) {}
