package com.tuanistack.cranhua.finca.dto;

import com.tuanistack.cranhua.finca.ZonaCafetalera;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record RegistrarFincaRequest(
    @NotBlank(message = "Ingresá el nombre de la finca.")
        @Size(max = 120, message = "El nombre no puede tener más de 120 caracteres.")
        String nombre,
    @Size(max = 300, message = "La descripción no puede tener más de 300 caracteres.")
        String descripcion,
    ZonaCafetalera zonaCafetalera,
    @NotNull(message = "Dibujá el perímetro de la finca.")
        List<@NotNull(message = "Falta un punto del perímetro.") @Valid Coordenada> perimetro,
    @NotNull(message = "Marcá la ubicación de la finca.") @Valid Coordenada ubicacion,
    @NotBlank(message = "Ingresá el cantón.")
        @Size(max = 60, message = "El cantón no puede tener más de 60 caracteres.")
        String canton,
    @NotBlank(message = "Ingresá la provincia.")
        @Size(max = 30, message = "La provincia no puede tener más de 30 caracteres.")
        String provincia) {}
