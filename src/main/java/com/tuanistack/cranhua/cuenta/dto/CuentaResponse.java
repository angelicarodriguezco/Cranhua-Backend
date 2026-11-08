package com.tuanistack.cranhua.cuenta.dto;

import com.tuanistack.cranhua.cuenta.Cuenta;
import com.tuanistack.cranhua.cuenta.Rol;
import java.util.List;
import java.util.UUID;

public record CuentaResponse(
    UUID id,
    String nombreCompleto,
    String correo,
    List<Rol> roles,
    String telefono,
    String fotoUrl,
    String bannerUrl,
    List<RedSocialResponse> redes) {

  public static CuentaResponse de(Cuenta cuenta) {
    return new CuentaResponse(
        cuenta.getId(),
        cuenta.getNombreCompleto(),
        cuenta.getCorreo(),
        cuenta.getRoles().stream().sorted().toList(),
        cuenta.getTelefono(),
        cuenta.getFotoUrl(),
        cuenta.getBannerUrl(),
        cuenta.getRedes().stream().map(RedSocialResponse::de).toList());
  }
}
