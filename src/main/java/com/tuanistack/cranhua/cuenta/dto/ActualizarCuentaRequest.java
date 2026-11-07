package com.tuanistack.cranhua.cuenta.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ActualizarCuentaRequest(
    @Size(max = 120, message = "El nombre no puede tener más de 120 caracteres.")
        @Pattern(regexp = ".*\\S.*", message = "Ingresá tu nombre completo.")
        String nombreCompleto,
    @Size(max = 30, message = "El teléfono no puede tener más de 30 caracteres.") String telefono,
    @Size(max = 500, message = "La URL de la foto no puede tener más de 500 caracteres.")
        String fotoUrl,
    @Valid List<RedSocialRequest> redes) {}
