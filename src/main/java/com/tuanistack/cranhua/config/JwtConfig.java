package com.tuanistack.cranhua.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.tuanistack.cranhua.auth.SesionProperties;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
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

  @Bean
  JwtDecoder jwtDecoder(SesionProperties propiedades) {
    return NimbusJwtDecoder.withSecretKey(clave(propiedades))
        .macAlgorithm(MacAlgorithm.HS256)
        .build();
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
