package com.tuanistack.cranhua.cafeteria.dto;

import com.tuanistack.cranhua.cafeteria.CategoriaMenu;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record PublicarItemMenuRequest(
    @NotBlank(message = "Ingresá el nombre del ítem.")
        @Size(max = 120, message = "El nombre no puede tener más de 120 caracteres.")
        String nombre,
    @NotNull(message = "Elegí la categoría.") CategoriaMenu categoria,
    String codigoTrazabilidad,
    @NotNull(message = "Ingresá el precio.")
        @DecimalMin(value = "0.01", message = "El precio tiene que ser mayor a 0.")
        @Digits(integer = 8, fraction = 2, message = "El precio no es válido.")
        BigDecimal precio) {}
