package com.tuanistack.cranhua.finca.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AgregarDuenoRequest(
    @NotBlank(message = "Ingresá el correo.")
        @Email(regexp = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$", message = "Ingresá un correo válido.")
        String correo) {

  // Se quitan los espacios antes de validar para que " ana@ejemplo.cr " no dé 400
  public AgregarDuenoRequest {
    correo = correo == null ? null : correo.strip();
  }
}
