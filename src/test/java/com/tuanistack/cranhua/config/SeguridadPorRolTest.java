package com.tuanistack.cranhua.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tuanistack.cranhua.auth.CookieSesion;
import com.tuanistack.cranhua.cuenta.Rol;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// Todavía no hay endpoints exclusivos de un rol, así que se prueba con unos hechos para el test
@WebMvcTest(SeguridadPorRolTest.EndpointsDePrueba.class)
@Import({SecurityConfig.class, JwtConfig.class, SeguridadPorRolTest.EndpointsDePrueba.class})
@ActiveProfiles("test")
class SeguridadPorRolTest {

  @RestController
  static class EndpointsDePrueba {

    @GetMapping("/api/prueba/cafetalero")
    @PreAuthorize("hasRole('CAFETALERO')")
    String cafetalero() {
      return "ok";
    }

    @GetMapping("/api/prueba/tostador")
    @PreAuthorize("hasRole('TOSTADOR')")
    String tostador() {
      return "ok";
    }

    @GetMapping("/api/prueba/cafeteria")
    @PreAuthorize("hasRole('CAFETERIA')")
    String cafeteria() {
      return "ok";
    }

    @GetMapping("/api/publico/prueba")
    String publico() {
      return "ok";
    }

    @GetMapping("/api/auth/prueba")
    String auth() {
      return "ok";
    }
  }

  @Autowired private MockMvc mockMvc;

  @Autowired private JwtEncoder jwtEncoder;

  private Cookie sesionCon(Rol... roles) {
    Instant ahora = Instant.now();
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .subject(UUID.randomUUID().toString())
            .issuedAt(ahora)
            .expiresAt(ahora.plusSeconds(60))
            .claim(CookieSesion.CLAIM_ROLES, Arrays.stream(roles).map(Rol::name).toList())
            .build();
    JwsHeader cabecera = JwsHeader.with(MacAlgorithm.HS256).build();
    String token = jwtEncoder.encode(JwtEncoderParameters.from(cabecera, claims)).getTokenValue();
    return new Cookie(CookieSesion.NOMBRE, token);
  }

  @Test
  void sinSesionDa401() throws Exception {
    mockMvc.perform(get("/api/prueba/cafetalero")).andExpect(status().isUnauthorized());
  }

  @Test
  void conElRolCorrectoDa200() throws Exception {
    mockMvc
        .perform(get("/api/prueba/cafetalero").cookie(sesionCon(Rol.CAFETALERO)))
        .andExpect(status().isOk());
  }

  @Test
  void sinElRolCorrectoDa403() throws Exception {
    mockMvc
        .perform(get("/api/prueba/cafetalero").cookie(sesionCon(Rol.TOSTADOR)))
        .andExpect(status().isForbidden());
  }

  @Test
  void sesionSinRolesDa403() throws Exception {
    mockMvc
        .perform(get("/api/prueba/cafetalero").cookie(sesionCon()))
        .andExpect(status().isForbidden());
  }

  @Test
  void conVariosRolesPuedeUsarLosEndpointsDeCadaUno() throws Exception {
    Cookie sesion = sesionCon(Rol.CAFETALERO, Rol.TOSTADOR);

    mockMvc.perform(get("/api/prueba/cafetalero").cookie(sesion)).andExpect(status().isOk());
    mockMvc.perform(get("/api/prueba/tostador").cookie(sesion)).andExpect(status().isOk());
    mockMvc.perform(get("/api/prueba/cafeteria").cookie(sesion)).andExpect(status().isForbidden());
  }

  @ParameterizedTest
  @ValueSource(strings = {"/api/publico/prueba", "/api/auth/prueba"})
  void lasRutasPublicasNoPidenSesion(String ruta) throws Exception {
    mockMvc.perform(get(ruta)).andExpect(status().isOk());
  }
}
