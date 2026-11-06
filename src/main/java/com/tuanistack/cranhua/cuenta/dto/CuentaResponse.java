package com.tuanistack.cranhua.cuenta.dto;

import com.tuanistack.cranhua.cuenta.Cuenta;
import com.tuanistack.cranhua.cuenta.Rol;
import java.util.List;
import java.util.UUID;

public record CuentaResponse(UUID id, String nombreCompleto, String correo, List<Rol> roles) {

  public static CuentaResponse de(Cuenta cuenta) {
    return new CuentaResponse(
        cuenta.getId(),
        cuenta.getNombreCompleto(),
        cuenta.getCorreo(),
        cuenta.getRoles().stream().sorted().toList());
  }
}
