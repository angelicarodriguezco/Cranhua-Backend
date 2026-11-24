package com.tuanistack.cranhua.finca.dto;

import com.tuanistack.cranhua.finca.Proceso;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record RegistrarCosechaRequest(
    @NotNull(message = "Elegí la finca.") UUID fincaId,
    @NotBlank(message = "Ingresá la variedad.")
        @Size(max = 60, message = "La variedad no puede tener más de 60 caracteres.")
        String variedad,
    @NotNull(message = "Ingresá la altura.")
        @Positive(message = "La altura tiene que ser mayor a 0.")
        Integer alturaMsnm,
    @NotNull(message = "Ingresá la fecha de inicio.") LocalDate fechaInicio,
    @NotNull(message = "Ingresá la fecha de fin.") LocalDate fechaFin,
    @NotEmpty(message = "Agregá al menos un proceso.")
        List<@NotNull(message = "Falta un proceso.") @Valid RepartoRequest> procesos,
    @NotNull(message = "Ingresá la cantidad cosechada.")
        @DecimalMin(value = "0.01", message = "La cantidad cosechada tiene que ser mayor a 0.")
        @Digits(integer = 8, fraction = 2, message = "La cantidad cosechada no es válida.")
        BigDecimal cantidadCosechadaKg,
    @DecimalMin(value = "0.01", message = "El peso tras el beneficiado tiene que ser mayor a 0.")
        @Digits(integer = 8, fraction = 2, message = "El peso tras el beneficiado no es válido.")
        BigDecimal pesoTrasBeneficiadoKg,
    @DecimalMin(value = "0.01", message = "El peso tras la selección tiene que ser mayor a 0.")
        @Digits(integer = 8, fraction = 2, message = "El peso tras la selección no es válido.")
        BigDecimal pesoTrasSeleccionKg,
    @DecimalMin(value = "0.01", message = "El caracolillo tiene que ser mayor a 0.")
        @Digits(integer = 8, fraction = 2, message = "El caracolillo no es válido.")
        BigDecimal caracolilloKg,
    @Size(max = 500, message = "Las notas de sabor no pueden tener más de 500 caracteres.")
        String notasSabor,
    @DecimalMin(value = "0", message = "El puntaje SCA tiene que estar entre 0 y 100.")
        @DecimalMax(value = "100", message = "El puntaje SCA tiene que estar entre 0 y 100.")
        @Digits(integer = 3, fraction = 2, message = "El puntaje SCA no es válido.")
        BigDecimal puntajeSca,
    @NotNull(message = "Ingresá el precio por kg.")
        @DecimalMin(
            value = "0",
            inclusive = false,
            message = "El precio por kg tiene que ser mayor a 0.")
        @Digits(integer = 10, fraction = 2, message = "El precio por kg no es válido.")
        BigDecimal precioKg)
    implements DatosCosecha {

  public record RepartoRequest(
      @NotNull(message = "Elegí el proceso.") Proceso proceso,
      @NotNull(message = "Ingresá el porcentaje.")
          @Min(value = 1, message = "El porcentaje tiene que estar entre 1 y 100.")
          @Max(value = 100, message = "El porcentaje tiene que estar entre 1 y 100.")
          Integer porcentaje) {}
}
