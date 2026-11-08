package com.tuanistack.cranhua.tueste.dto;

import com.tuanistack.cranhua.tueste.NivelTueste;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record RegistrarNegocioRequest(
    @NotBlank(message = "Ingresá el nombre del negocio.")
        @Size(max = 120, message = "El nombre no puede tener más de 120 caracteres.")
        String nombre,
    @NotBlank(message = "Ingresá el nombre de contacto.")
        @Size(max = 120, message = "El nombre de contacto no puede tener más de 120 caracteres.")
        String nombreContacto,
    @NotBlank(message = "Ingresá el teléfono.")
        @Size(max = 30, message = "El teléfono no puede tener más de 30 caracteres.")
        String telefono,
    @NotBlank(message = "Ingresá el correo.")
        @Email(regexp = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$", message = "Ingresá un correo válido.")
        @Size(max = 160, message = "El correo no puede tener más de 160 caracteres.")
        String correo,
    @Size(max = 300, message = "La descripción no puede tener más de 300 caracteres.")
        String descripcion,
    @NotBlank(message = "Ingresá el cantón.")
        @Size(max = 60, message = "El cantón no puede tener más de 60 caracteres.")
        String canton,
    @NotBlank(message = "Ingresá la provincia.")
        @Size(max = 30, message = "La provincia no puede tener más de 30 caracteres.")
        String provincia,
    @NotEmpty(message = "Elegí al menos un estilo de tueste.")
        Set<@NotNull(message = "Estilo de tueste inválido.") NivelTueste> estilos,
    @Size(max = 500, message = "La URL de la portada no puede tener más de 500 caracteres.")
        String portadaUrl) {}
