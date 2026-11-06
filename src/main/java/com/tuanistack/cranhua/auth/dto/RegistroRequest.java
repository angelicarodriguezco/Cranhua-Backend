package com.tuanistack.cranhua.auth.dto;

import com.tuanistack.cranhua.auth.RolesValidos;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record RegistroRequest(
    @NotBlank(message = "Ingresá tu nombre completo.")
        @Size(max = 120, message = "El nombre no puede tener más de 120 caracteres.")
        String nombreCompleto,
    @NotBlank(message = "Ingresá tu correo.")
        @Email(regexp = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$", message = "Ingresá un correo válido.")
        @Size(max = 160, message = "El correo no puede tener más de 160 caracteres.")
        String correo,
    @NotNull(message = "Ingresá una contraseña.")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres.")
        @Pattern(
            regexp = "^(?=.*\\p{L})(?=.*\\d).*$",
            message = "La contraseña debe tener al menos una letra y un número.")
        String contrasena,
    @RolesValidos List<String> roles,
    @Size(max = 30, message = "El teléfono no puede tener más de 30 caracteres.")
        String telefono) {}
