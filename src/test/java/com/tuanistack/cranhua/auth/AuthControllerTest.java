package com.tuanistack.cranhua.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tuanistack.cranhua.cuenta.Cuenta;
import com.tuanistack.cranhua.cuenta.CuentaRepository;
import com.tuanistack.cranhua.cuenta.Rol;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private CuentaRepository cuentaRepository;

  @Autowired private PasswordEncoder passwordEncoder;

  @MockitoBean private VerificadorGoogle verificadorGoogle;

  @AfterEach
  void limpiar() {
    cuentaRepository.deleteAll();
  }

  private static String registro(String nombre, String correo, String contrasena, String roles) {
    return """
    {"nombreCompleto": %s, "correo": %s, "contrasena": %s, "roles": %s}
    """
        .formatted(nombre, correo, contrasena, roles);
  }

  private static String registroValido(String correo, String roles) {
    return registro("\"Rodrigo Alvarado\"", "\"" + correo + "\"", "\"Cafe2026!\"", roles);
  }

  private MvcResult registrar(String body) throws Exception {
    return mockMvc
        .perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content(body))
        .andReturn();
  }

  private MvcResult registrarConGoogle(String credencial) throws Exception {
    return mockMvc
        .perform(
            post("/api/auth/registro/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"credencial": "%s", "roles": ["CAFETERIA"]}
                    """
                        .formatted(credencial)))
        .andReturn();
  }

  private static MockHttpSession sesionDe(MvcResult resultado) {
    MockHttpSession sesion = (MockHttpSession) resultado.getRequest().getSession(false);
    assertThat(sesion).isNotNull();
    assertThat(
            sesion.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY))
        .isNotNull();
    return sesion;
  }

  @Test
  void registraConUnRolYDejaLaSesionIniciada() throws Exception {
    MvcResult resultado = registrar(registroValido("Rodrigo@Ejemplo.cr", "[\"CAFETALERO\"]"));

    assertThat(resultado.getResponse().getStatus()).isEqualTo(201);
    assertThat(resultado.getResponse().getContentAsString()).doesNotContain("contrasena", "$2a$");

    Cuenta guardada = cuentaRepository.findByCorreoAndEliminadaFalse("rodrigo@ejemplo.cr").get();
    assertThat(guardada.getRoles()).containsExactly(Rol.CAFETALERO);
    assertThat(guardada.getContrasenaHash()).isNotEqualTo("Cafe2026!");
    assertThat(passwordEncoder.matches("Cafe2026!", guardada.getContrasenaHash())).isTrue();

    mockMvc
        .perform(get("/api/cuenta/actual").session(sesionDe(resultado)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(guardada.getId().toString()))
        .andExpect(jsonPath("$.nombreCompleto").value("Rodrigo Alvarado"))
        .andExpect(jsonPath("$.correo").value("rodrigo@ejemplo.cr"))
        .andExpect(jsonPath("$.roles", contains("CAFETALERO")));
  }

  @Test
  void registraConLosTresRoles() throws Exception {
    MvcResult resultado =
        registrar(
            registroValido("mariana@ejemplo.cr", "[\"CAFETERIA\", \"TOSTADOR\", \"CAFETALERO\"]"));

    assertThat(resultado.getResponse().getStatus()).isEqualTo(201);
    mockMvc
        .perform(get("/api/cuenta/actual").session(sesionDe(resultado)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.roles", contains("CAFETALERO", "TOSTADOR", "CAFETERIA")));
  }

  @Test
  void rechazaCorreoRepetidoAunqueCambienLasMayusculas() throws Exception {
    registrar(registroValido("lucia@ejemplo.cr", "[\"TOSTADOR\"]"));

    for (String correo : new String[] {"lucia@ejemplo.cr", "LUCIA@Ejemplo.CR"}) {
      mockMvc
          .perform(
              post("/api/auth/registro")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(registroValido(correo, "[\"CAFETERIA\"]")))
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.mensaje").value("Ya existe una cuenta con ese correo."));
    }
    assertThat(cuentaRepository.count()).isEqualTo(1);
  }

  static Stream<Arguments> datosInvalidos() {
    return Stream.of(
        Arguments.of(
            "correo",
            registro("\"Ana\"", "\"ana@ejemplo\"", "\"Cafe2026!\"", "[\"TOSTADOR\"]"),
            "Ingresá un correo válido."),
        Arguments.of(
            "contrasena",
            registro("\"Ana\"", "\"ana@ejemplo.cr\"", "\"cafe1\"", "[\"TOSTADOR\"]"),
            "La contraseña debe tener entre 8 y 72 caracteres."),
        Arguments.of(
            "contrasena",
            registro("\"Ana\"", "\"ana@ejemplo.cr\"", "\"cafecafe\"", "[\"TOSTADOR\"]"),
            "La contraseña debe tener al menos una letra y un número."),
        Arguments.of(
            "roles",
            registro("\"Ana\"", "\"ana@ejemplo.cr\"", "\"Cafe2026!\"", "[]"),
            "Elegí al menos un rol."),
        Arguments.of(
            "roles",
            registro("\"Ana\"", "\"ana@ejemplo.cr\"", "\"Cafe2026!\"", "[\"BARISTA\"]"),
            "Rol inválido. Usá CAFETALERO, TOSTADOR o CAFETERIA."),
        Arguments.of(
            "roles",
            registro(
                "\"Ana\"", "\"ana@ejemplo.cr\"", "\"Cafe2026!\"", "[\"TOSTADOR\", \"TOSTADOR\"]"),
            "No repitas roles."),
        Arguments.of(
            "nombreCompleto",
            registro("\"   \"", "\"ana@ejemplo.cr\"", "\"Cafe2026!\"", "[\"TOSTADOR\"]"),
            "Ingresá tu nombre completo."));
  }

  @ParameterizedTest(name = "{0}: {2}")
  @MethodSource("datosInvalidos")
  void rechazaDatosInvalidosConElDetallePorCampo(String campo, String body, String mensaje)
      throws Exception {
    mockMvc
        .perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.mensaje").value("Revisá los datos ingresados."))
        .andExpect(jsonPath("$.errores." + campo).value(mensaje));
    assertThat(cuentaRepository.count()).isZero();
  }

  @Test
  void rolesConTipoEquivocadoTambienVanPorCampo() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/registro")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registroValido("ana@ejemplo.cr", "{\"rol\": 1}")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errores.roles").value("Valor inválido."));
  }

  @Test
  void registraConGoogleSinContrasena() throws Exception {
    when(verificadorGoogle.verificar("token-valido"))
        .thenReturn(
            Optional.of(
                new DatosGoogle(
                    "google-sub-123",
                    "Andres.Solano@gmail.com",
                    "Andrés Solano",
                    "https://lh3.googleusercontent.com/a/foto")));

    MvcResult resultado = registrarConGoogle("token-valido");

    assertThat(resultado.getResponse().getStatus()).isEqualTo(201);
    Cuenta guardada =
        cuentaRepository.findByCorreoAndEliminadaFalse("andres.solano@gmail.com").get();
    assertThat(guardada.getGoogleId()).isEqualTo("google-sub-123");
    assertThat(guardada.getContrasenaHash()).isNull();
    assertThat(guardada.getNombreCompleto()).isEqualTo("Andrés Solano");
    assertThat(guardada.getFotoUrl()).isEqualTo("https://lh3.googleusercontent.com/a/foto");
    assertThat(guardada.getRoles()).containsExactly(Rol.CAFETERIA);

    mockMvc
        .perform(get("/api/cuenta/actual").session(sesionDe(resultado)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.correo").value("andres.solano@gmail.com"));
  }

  @Test
  void rechazaTokenDeGoogleInvalido() throws Exception {
    when(verificadorGoogle.verificar("token-falso")).thenReturn(Optional.empty());

    MvcResult resultado = registrarConGoogle("token-falso");

    assertThat(resultado.getResponse().getStatus()).isEqualTo(401);
    assertThat(resultado.getResponse().getContentAsString())
        .contains("No se pudo verificar la cuenta de Google.");
    assertThat(cuentaRepository.count()).isZero();
  }

  @Test
  void rechazaGoogleConCorreoExistente() throws Exception {
    cuentaRepository.saveAndFlush(
        new Cuenta(
            "Andrés Solano",
            "andres.solano@gmail.com",
            passwordEncoder.encode("Cafe2026!"),
            Set.of(Rol.CAFETERIA)));
    when(verificadorGoogle.verificar("token-valido"))
        .thenReturn(
            Optional.of(
                new DatosGoogle("google-sub-123", "andres.solano@gmail.com", "Andrés", null)));

    MvcResult resultado = registrarConGoogle("token-valido");

    assertThat(resultado.getResponse().getStatus()).isEqualTo(409);
    assertThat(resultado.getResponse().getContentAsString())
        .contains("Ya existe una cuenta con ese correo.");
  }

  @Test
  void cuentaActualSinSesionDa401() throws Exception {
    mockMvc.perform(get("/api/cuenta/actual")).andExpect(status().isUnauthorized());
  }
}
