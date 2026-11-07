package com.tuanistack.cranhua.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record VerificarCodigoRequest(
    @NotBlank(message = "Ingresá tu correo.") String correo,
    @NotNull(message = "Ingresá el código.")
        @Pattern(regexp = "^\\d{6}$", message = "El código tiene 6 dígitos.")
        String codigo) {}
