package com.tuanistack.cranhua.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record IniciarSesionRequest(
    @NotBlank(message = "Ingresá tu correo.") String correo,
    @NotBlank(message = "Ingresá tu contraseña.") String contrasena) {}
