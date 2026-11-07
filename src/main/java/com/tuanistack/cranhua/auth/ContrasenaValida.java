package com.tuanistack.cranhua.auth;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Reglas de contraseña compartidas por el registro y el restablecimiento
@NotNull(message = "Ingresá una contraseña.")
@Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres.")
@Pattern(
    regexp = "^(?=.*\\p{L})(?=.*\\d).*$",
    message = "La contraseña debe tener al menos una letra y un número.")
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {})
public @interface ContrasenaValida {

  String message() default "Contraseña inválida.";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
