package com.tuanistack.cranhua.auth;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Los roles llegan como texto para poder responder 400 con el detalle si alguno no existe
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = RolesValidosValidator.class)
public @interface RolesValidos {

  String message() default "Elegí al menos un rol.";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
