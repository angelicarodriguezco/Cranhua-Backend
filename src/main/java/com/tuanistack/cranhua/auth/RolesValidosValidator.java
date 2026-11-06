package com.tuanistack.cranhua.auth;

import com.tuanistack.cranhua.cuenta.Rol;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RolesValidosValidator implements ConstraintValidator<RolesValidos, List<String>> {

  @Override
  public boolean isValid(List<String> roles, ConstraintValidatorContext context) {
    String error = buscarError(roles);
    if (error == null) {
      return true;
    }
    context.disableDefaultConstraintViolation();
    context.buildConstraintViolationWithTemplate(error).addConstraintViolation();
    return false;
  }

  private String buscarError(List<String> roles) {
    if (roles == null || roles.isEmpty()) {
      return "Elegí al menos un rol.";
    }
    Set<String> vistos = new HashSet<>();
    for (String rol : roles) {
      if (Arrays.stream(Rol.values()).noneMatch(valido -> valido.name().equals(rol))) {
        return "Rol inválido. Usá CAFETALERO, TOSTADOR o CAFETERIA.";
      }
      if (!vistos.add(rol)) {
        return "No repitas roles.";
      }
    }
    return null;
  }
}
