package com.tuanistack.cranhua.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.tuanistack.cranhua.auth.SesionProperties;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration
@EnableConfigurationProperties(SesionProperties.class)
public class JwtConfig {

  // HS256 necesita una clave de al menos 256 bits
  private static final int BYTES_MINIMOS = 32;

  @Bean
  JwtEncoder jwtEncoder(SesionProperties propiedades) {
    return new NimbusJwtEncoder(new ImmutableSecret<>(clave(propiedades)));
  }

  // Los validadores extra (por ejemplo la versión de sesión) se suman a los de vencimiento
  @Bean
  JwtDecoder jwtDecoder(
      SesionProperties propiedades, ObjectProvider<OAuth2TokenValidator<Jwt>> validadores) {
    NimbusJwtDecoder decoder =
        NimbusJwtDecoder.withSecretKey(clave(propiedades)).macAlgorithm(MacAlgorithm.HS256).build();
    List<OAuth2TokenValidator<Jwt>> todos = new ArrayList<>();
    todos.add(JwtValidators.createDefault());
    validadores.orderedStream().forEach(todos::add);
    decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(todos));
    return decoder;
  }

  private static SecretKey clave(SesionProperties propiedades) {
    String secreto = propiedades.secreto();
    byte[] bytes = secreto == null ? new byte[0] : secreto.getBytes(StandardCharsets.UTF_8);
    if (bytes.length < BYTES_MINIMOS) {
      throw new IllegalStateException(
          "JWT_SECRET debe tener al menos " + BYTES_MINIMOS + " caracteres.");
    }
    return new SecretKeySpec(bytes, "HmacSHA256");
  }
}
