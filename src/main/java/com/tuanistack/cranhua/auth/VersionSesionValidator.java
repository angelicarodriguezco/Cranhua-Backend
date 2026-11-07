package com.tuanistack.cranhua.auth;

import com.tuanistack.cranhua.cuenta.CuentaRepository;
import java.util.UUID;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

// Rechaza tokens emitidos antes del último cambio de contraseña
@Component
public class VersionSesionValidator implements OAuth2TokenValidator<Jwt> {

  private static final OAuth2TokenValidatorResult SESION_CERRADA =
      OAuth2TokenValidatorResult.failure(
          new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN, "La sesión ya no es válida.", null));

  private final CuentaRepository cuentaRepository;

  public VersionSesionValidator(CuentaRepository cuentaRepository) {
    this.cuentaRepository = cuentaRepository;
  }

  // Un token sin el claim cuenta como versión 0
  @Override
  public OAuth2TokenValidatorResult validate(Jwt jwt) {
    UUID id;
    try {
      id = UUID.fromString(jwt.getSubject());
    } catch (IllegalArgumentException | NullPointerException e) {
      return SESION_CERRADA;
    }
    Number claim = jwt.getClaim(CookieSesion.CLAIM_VERSION);
    int version = claim == null ? 0 : claim.intValue();
    return cuentaRepository.buscarVersionSesion(id).filter(v -> v == version).isPresent()
        ? OAuth2TokenValidatorResult.success()
        : SESION_CERRADA;
  }
}
