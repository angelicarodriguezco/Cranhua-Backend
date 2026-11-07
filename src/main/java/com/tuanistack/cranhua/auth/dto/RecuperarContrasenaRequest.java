package com.tuanistack.cranhua.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record RecuperarContrasenaRequest(@NotBlank(message = "Ingresá tu correo.") String correo) {}
