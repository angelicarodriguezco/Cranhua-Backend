package com.tuanistack.cranhua.cuenta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tuanistack.cranhua.BasePostgis;
import com.tuanistack.cranhua.auth.CookieSesion;
import jakarta.servlet.http.Cookie;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(BasePostgis.class)
class CuentaControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private CuentaRepository cuentaRepository;

  @Autowired private PasswordEncoder passwordEncoder;

  @AfterEach
  void limpiar() {
    cuentaRepository.deleteAll();
  }

  private Cuenta guardarCuenta(String correo) {
    return cuentaRepository.saveAndFlush(
        new Cuenta(
            "Rodrigo Alvarado", correo, passwordEncoder.encode("Cafe2026!"), Set.of(Rol.TOSTADOR)));
  }

  private Cookie iniciarSesion(String correo) throws Exception {
    MvcResult resultado =
        mockMvc
            .perform(
                post("/api/auth/iniciarSesion")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"correo": "%s", "contrasena": "Cafe2026!"}
                        """
                            .formatted(correo)))
            .andExpect(status().isOk())
            .andReturn();
    Cookie cookie = resultado.getResponse().getCookie(CookieSesion.NOMBRE);
    assertThat(cookie).isNotNull();
    assertThat(cookie.getValue()).isNotBlank();
    return cookie;
  }

  @Test
  void getSinSesionDa401() throws Exception {
    mockMvc.perform(get("/api/cuenta")).andExpect(status().isUnauthorized());
  }

  @Test
  void getConSesionDevuelvePerfilPersonalAmpliado() throws Exception {
    Cuenta cuenta = guardarCuenta("rodrigo@ejemplo.cr");
    cuenta.setTelefono("8888-1234");
    cuenta.setFotoUrl("https://ejemplo.cr/foto.jpg");
    cuenta.getRedes().add(new RedSocial(TipoRedSocial.INSTAGRAM, "@rodrigo"));
    cuentaRepository.saveAndFlush(cuenta);
    Cookie sesion = iniciarSesion("rodrigo@ejemplo.cr");

    mockMvc
        .perform(get("/api/cuenta").cookie(sesion))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(cuenta.getId().toString()))
        .andExpect(jsonPath("$.nombreCompleto").value("Rodrigo Alvarado"))
        .andExpect(jsonPath("$.correo").value("rodrigo@ejemplo.cr"))
        .andExpect(jsonPath("$.roles", contains("TOSTADOR")))
        .andExpect(jsonPath("$.telefono").value("8888-1234"))
        .andExpect(jsonPath("$.fotoUrl").value("https://ejemplo.cr/foto.jpg"))
        .andExpect(jsonPath("$.redes", hasSize(1)))
        .andExpect(jsonPath("$.redes[0].tipo").value("INSTAGRAM"))
        .andExpect(jsonPath("$.redes[0].valor").value("@rodrigo"));
  }

  @Test
  void aliasActualSigueFuncionando() throws Exception {
    Cuenta cuenta = guardarCuenta("rodrigo@ejemplo.cr");
    Cookie sesion = iniciarSesion("rodrigo@ejemplo.cr");

    mockMvc
        .perform(get("/api/cuenta/actual").cookie(sesion))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(cuenta.getId().toString()))
        .andExpect(jsonPath("$.correo").value("rodrigo@ejemplo.cr"))
        .andExpect(jsonPath("$.telefono").doesNotExist())
        .andExpect(jsonPath("$.fotoUrl").doesNotExist())
        .andExpect(jsonPath("$.redes", hasSize(0)));
  }

  @Test
  void patchPersisteCamposEditablesYSeVenEnGet() throws Exception {
    guardarCuenta("rodrigo@ejemplo.cr");
    Cookie sesion = iniciarSesion("rodrigo@ejemplo.cr");

    mockMvc
        .perform(
            patch("/api/cuenta")
                .cookie(sesion)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "nombreCompleto": "Rodrigo A. Alvarado",
                      "telefono": "7000-1111",
                      "fotoUrl": "https://cdn.ejemplo.cr/perfil.png",
                      "redes": [
                        {"tipo": "INSTAGRAM", "valor": "@rodrigo.cafe"},
                        {"tipo": "WHATSAPP", "valor": "70001111"}
                      ]
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nombreCompleto").value("Rodrigo A. Alvarado"))
        .andExpect(jsonPath("$.telefono").value("7000-1111"))
        .andExpect(jsonPath("$.fotoUrl").value("https://cdn.ejemplo.cr/perfil.png"))
        .andExpect(jsonPath("$.redes", hasSize(2)))
        .andExpect(jsonPath("$.redes[0].tipo").value("INSTAGRAM"))
        .andExpect(jsonPath("$.redes[0].valor").value("@rodrigo.cafe"))
        .andExpect(jsonPath("$.redes[1].tipo").value("WHATSAPP"))
        .andExpect(jsonPath("$.redes[1].valor").value("70001111"));

    mockMvc
        .perform(get("/api/cuenta").cookie(sesion))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nombreCompleto").value("Rodrigo A. Alvarado"))
        .andExpect(jsonPath("$.telefono").value("7000-1111"))
        .andExpect(jsonPath("$.fotoUrl").value("https://cdn.ejemplo.cr/perfil.png"))
        .andExpect(jsonPath("$.redes", hasSize(2)))
        .andExpect(jsonPath("$.correo").value("rodrigo@ejemplo.cr"))
        .andExpect(jsonPath("$.roles", contains("TOSTADOR")));
  }

  @Test
  void deleteMarcaEliminadaLimpiaCookieYBloqueaAccesoPosterior() throws Exception {
    Cuenta cuenta = guardarCuenta("baja@ejemplo.cr");
    Cookie sesion = iniciarSesion("baja@ejemplo.cr");

    MvcResult resultado = mockMvc.perform(delete("/api/cuenta").cookie(sesion)).andReturn();

    assertThat(resultado.getResponse().getStatus()).isEqualTo(204);
    assertThat(resultado.getResponse().getHeader(HttpHeaders.SET_COOKIE))
        .startsWith(CookieSesion.NOMBRE + "=;")
        .contains("Path=/", "Max-Age=0", "HttpOnly", "Secure", "SameSite=Strict");

    Cuenta eliminada = cuentaRepository.findById(cuenta.getId()).orElseThrow();
    assertThat(eliminada.isEliminada()).isTrue();
    assertThat(eliminada.getEliminadaEn()).isNotNull();

    mockMvc.perform(get("/api/cuenta").cookie(sesion)).andExpect(status().isUnauthorized());

    mockMvc
        .perform(
            post("/api/auth/iniciarSesion")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"correo": "baja@ejemplo.cr", "contrasena": "Cafe2026!"}
                    """))
        .andExpect(status().isUnauthorized());
  }
}
