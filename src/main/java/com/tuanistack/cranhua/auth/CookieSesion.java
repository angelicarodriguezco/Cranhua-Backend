package com.tuanistack.cranhua.auth;

import com.tuanistack.cranhua.cuenta.Cuenta;
import com.tuanistack.cranhua.cuenta.Rol;
import java.time.Duration;
import java.time.Instant;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

@Component
public class CookieSesion {

  public static final String NOMBRE = "cranhua_sesion";
  public static final String CLAIM_ROLES = "roles";
  public static final String CLAIM_VERSION = "version_sesion";

  private final JwtEncoder jwtEncoder;
  private final Duration duracion;

  public CookieSesion(JwtEncoder jwtEncoder, SesionProperties propiedades) {
    this.jwtEncoder = jwtEncoder;
    this.duracion = propiedades.duracion();
  }

  // El sub es el id de la cuenta; la cookie vence junto con el token
  public ResponseCookie crear(Cuenta cuenta) {
    Instant ahora = Instant.now();
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .subject(cuenta.getId().toString())
            .issuedAt(ahora)
            .expiresAt(ahora.plus(duracion))
            .claim(CLAIM_ROLES, cuenta.getRoles().stream().sorted().map(Rol::name).toList())
            .claim(CLAIM_VERSION, cuenta.getVersionSesion())
            .build();
    JwsHeader cabecera = JwsHeader.with(MacAlgorithm.HS256).build();
    String token = jwtEncoder.encode(JwtEncoderParameters.from(cabecera, claims)).getTokenValue();
    return cookie(token, duracion);
  }

  public ResponseCookie borrar() {
    return cookie("", Duration.ZERO);
  }

  // Strict: el navegador no la manda en peticiones que vienen de otro sitio
  private static ResponseCookie cookie(String valor, Duration maxAge) {
    return ResponseCookie.from(NOMBRE, valor)
        .httpOnly(true)
        .secure(true)
        .sameSite("Strict")
        .path("/")
        .maxAge(maxAge)
        .build();
  }
}
