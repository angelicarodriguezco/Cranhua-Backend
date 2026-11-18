package com.tuanistack.cranhua.finca.dto;

import com.tuanistack.cranhua.finca.ZonaCafetalera;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ActualizarFincaRequest(
    @Size(max = 120, message = "El nombre no puede tener más de 120 caracteres.")
        @Pattern(regexp = ".*\\S.*", message = "Ingresá el nombre de la finca.")
        String nombre,
    @Size(max = 300, message = "La descripción no puede tener más de 300 caracteres.")
        String descripcion,
    ZonaCafetalera zonaCafetalera,
    List<@NotNull(message = "Falta un punto del perímetro.") @Valid Coordenada> perimetro,
    @Valid Coordenada ubicacion,
    @Size(max = 60, message = "El cantón no puede tener más de 60 caracteres.")
        @Pattern(regexp = ".*\\S.*", message = "Ingresá el cantón.")
        String canton,
    @Size(max = 30, message = "La provincia no puede tener más de 30 caracteres.")
        @Pattern(regexp = ".*\\S.*", message = "Ingresá la provincia.")
        String provincia) {}
