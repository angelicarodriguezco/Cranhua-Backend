package com.tuanistack.cranhua.tueste.dto;

import com.tuanistack.cranhua.tueste.NivelTueste;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Set;

// Los campos que no vienen se mantienen; descripción y portada en blanco se borran
public record ActualizarNegocioRequest(
    @Size(max = 120, message = "El nombre no puede tener más de 120 caracteres.")
        @Pattern(regexp = ".*\\S.*", message = "Ingresá el nombre del negocio.")
        String nombre,
    @Size(max = 120, message = "El nombre de contacto no puede tener más de 120 caracteres.")
        @Pattern(regexp = ".*\\S.*", message = "Ingresá el nombre de contacto.")
        String nombreContacto,
    @Size(max = 30, message = "El teléfono no puede tener más de 30 caracteres.")
        @Pattern(regexp = ".*\\S.*", message = "Ingresá el teléfono.")
        String telefono,
    @Email(regexp = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$", message = "Ingresá un correo válido.")
        @Size(max = 160, message = "El correo no puede tener más de 160 caracteres.")
        @Pattern(regexp = ".*\\S.*", message = "Ingresá el correo.")
        String correo,
    @Size(max = 300, message = "La descripción no puede tener más de 300 caracteres.")
        String descripcion,
    @Size(max = 60, message = "El cantón no puede tener más de 60 caracteres.")
        @Pattern(regexp = ".*\\S.*", message = "Ingresá el cantón.")
        String canton,
    @Size(max = 30, message = "La provincia no puede tener más de 30 caracteres.")
        @Pattern(regexp = ".*\\S.*", message = "Ingresá la provincia.")
        String provincia,
    @Size(min = 1, message = "Elegí al menos un estilo de tueste.")
        Set<@NotNull(message = "Estilo de tueste inválido.") NivelTueste> estilos,
    @Size(max = 500, message = "La URL de la portada no puede tener más de 500 caracteres.")
        String portadaUrl) {}
