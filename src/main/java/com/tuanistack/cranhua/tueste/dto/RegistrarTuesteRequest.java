package com.tuanistack.cranhua.tueste.dto;

import com.tuanistack.cranhua.tueste.NivelTueste;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RegistrarTuesteRequest(
    @NotNull(message = "Elegí el lote.") UUID loteId,
    @NotNull(message = "Elegí el nivel de tueste.") NivelTueste nivel,
    @NotNull(message = "Ingresá el peso verde.")
        @DecimalMin(value = "0.01", message = "El peso verde tiene que ser mayor a 0.")
        @Digits(integer = 8, fraction = 2, message = "El peso verde no es válido.")
        BigDecimal pesoVerdeKg,
    @NotNull(message = "Ingresá el peso tostado.")
        @DecimalMin(value = "0.01", message = "El peso tostado tiene que ser mayor a 0.")
        @Digits(integer = 8, fraction = 2, message = "El peso tostado no es válido.")
        BigDecimal pesoTostadoKg,
    @NotNull(message = "Ingresá la fecha del tueste.") LocalDate fechaTueste,
    @Size(max = 500, message = "Las notas no pueden tener más de 500 caracteres.") String notas) {}
