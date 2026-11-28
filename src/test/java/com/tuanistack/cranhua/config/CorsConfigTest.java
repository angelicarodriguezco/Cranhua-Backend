package com.tuanistack.cranhua.config;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tuanistack.cranhua.ai.AiChatController;
import com.tuanistack.cranhua.ai.AiChatService;
import com.tuanistack.cranhua.ai.LimitadorMensajes;
import com.tuanistack.cranhua.auth.CookieSesion;
import com.tuanistack.cranhua.cuenta.CuentaController;
import com.tuanistack.cranhua.cuenta.CuentaService;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({AiChatController.class, CuentaController.class})
@Import({
  SecurityConfig.class,
  CorsConfig.class,
  JwtConfig.class,
  LimitadorMensajes.class,
  CookieSesion.class
})
@ActiveProfiles("test")
class CorsConfigTest {

  private static final String ANGULAR_ORIGIN = "http://localhost:4200";
  private static final String ORIGEN_AJENO = "http://evil.example";

  @Autowired private MockMvc mockMvc;

  @Autowired private JwtEncoder jwtEncoder;

  @MockitoBean private AiChatService aiChatService;

  @MockitoBean private CuentaService cuentaService;

  private Cookie sesionValida() {
    Instant ahora = Instant.now();
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .subject(UUID.randomUUID().toString())
            .issuedAt(ahora)
            .expiresAt(ahora.plusSeconds(60))
            .claim(CookieSesion.CLAIM_ROLES, List.of("CAFETERIA"))
            .build();
    JwsHeader cabecera = JwsHeader.with(MacAlgorithm.HS256).build();
    String token = jwtEncoder.encode(JwtEncoderParameters.from(cabecera, claims)).getTokenValue();
    return new Cookie(CookieSesion.NOMBRE, token);
  }

  @Test
  void preflightFromAngularIsAllowed() throws Exception {
    mockMvc
        .perform(
            options("/api/ai/chat")
                .header(HttpHeaders.ORIGIN, ANGULAR_ORIGIN)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ANGULAR_ORIGIN));
  }

  @Test
  void postFromAngularIncludesCorsHeaders() throws Exception {
    mockMvc
        .perform(
            post("/api/ai/chat")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CAFETERIA")))
                .header(HttpHeaders.ORIGIN, ANGULAR_ORIGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"Hola\"}"))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ANGULAR_ORIGIN));
  }

  @Test
  void preflightFromOtherOriginIsRejected() throws Exception {
    mockMvc
        .perform(
            options("/api/ai/chat")
                .header(HttpHeaders.ORIGIN, "https://otro-sitio.com")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
        .andExpect(status().isForbidden())
        .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
  }

  @Test
  void getConOrigenAjenoYSesionValidaDa403SinCabecerasCors() throws Exception {
    mockMvc
        .perform(get("/api/cuenta").cookie(sesionValida()).header(HttpHeaders.ORIGIN, ORIGEN_AJENO))
        .andExpect(status().isForbidden())
        .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
  }

  @Test
  void postConOrigenAjenoYSesionValidaDa403SinCabecerasCors() throws Exception {
    mockMvc
        .perform(
            post("/api/ai/chat")
                .cookie(sesionValida())
                .header(HttpHeaders.ORIGIN, ORIGEN_AJENO)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"Hola\"}"))
        .andExpect(status().isForbidden())
        .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
  }

  @Test
  void getDesdeAngularConSesionPermiteOrigenYCredenciales() throws Exception {
    mockMvc
        .perform(
            get("/api/cuenta").cookie(sesionValida()).header(HttpHeaders.ORIGIN, ANGULAR_ORIGIN))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ANGULAR_ORIGIN))
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
  }

  @Test
  void postDesdeAngularConSesionPermiteOrigenYCredenciales() throws Exception {
    mockMvc
        .perform(
            post("/api/ai/chat")
                .cookie(sesionValida())
                .header(HttpHeaders.ORIGIN, ANGULAR_ORIGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"Hola\"}"))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ANGULAR_ORIGIN))
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
  }
}
