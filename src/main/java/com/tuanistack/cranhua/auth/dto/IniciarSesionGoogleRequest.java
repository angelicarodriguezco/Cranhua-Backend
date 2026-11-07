package com.tuanistack.cranhua.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record IniciarSesionGoogleRequest(
    @NotBlank(message = "Falta la credencial de Google.") String credencial) {}
