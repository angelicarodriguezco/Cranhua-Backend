package com.tuanistack.cranhua.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.tuanistack.cranhua.BasePostgis;
import com.tuanistack.cranhua.cuenta.Cuenta;
import com.tuanistack.cranhua.cuenta.CuentaRepository;
import com.tuanistack.cranhua.cuenta.Rol;
import com.tuanistack.cranhua.storage.AlmacenamientoImagenes;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(BasePostgis.class)
class AuthControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private CuentaRepository cuentaRepository;

  @Autowired private PasswordEncoder passwordEncoder;

  @Autowired private JwtEncoder jwtEncoder;

  @Autowired private JwtDecoder jwtDecoder;

  @MockitoBean private VerificadorGoogle verificadorGoogle;

  @MockitoBean private AlmacenamientoImagenes almacenamientoImagenes;

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

  private MvcResult iniciarSesion(String correo, String contrasena) throws Exception {
    return mockMvc
        .perform(
            post("/api/auth/iniciarSesion")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"correo": "%s", "contrasena": "%s"}
                    """
                        .formatted(correo, contrasena)))
        .andReturn();
  }

  private MvcResult iniciarSesionConGoogle(String credencial) throws Exception {
    return mockMvc
        .perform(
            post("/api/auth/iniciarSesion/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"credencial\": \"%s\"}".formatted(credencial)))
        .andReturn();
  }

  private static Cookie cookieDe(MvcResult resultado) {
    Cookie cookie = resultado.getResponse().getCookie(CookieSesion.NOMBRE);
    assertThat(cookie).isNotNull();
    assertThat(cookie.getValue()).isNotBlank();
    return cookie;
  }

  private Cuenta guardarCuenta(String correo, Set<Rol> roles) {
    return cuentaRepository.saveAndFlush(
        new Cuenta("Rodrigo Alvarado", correo, passwordEncoder.encode("Cafe2026!"), roles));
  }

  private Cuenta guardarCuentaGoogle(String correo, String googleId) {
    return cuentaRepository.saveAndFlush(
        Cuenta.deGoogle("Andrés Solano", correo, googleId, null, Set.of(Rol.CAFETERIA)));
  }

  private void googleDevuelve(String credencial, String googleId, String correo) {
    when(verificadorGoogle.verificar(credencial))
        .thenReturn(Optional.of(new DatosGoogle(googleId, correo, "Andrés Solano", null)));
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
        .perform(get("/api/cuenta/actual").cookie(cookieDe(resultado)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(guardada.getId().toString()))
        .andExpect(jsonPath("$.nombreCompleto").value("Rodrigo Alvarado"))
        .andExpect(jsonPath("$.correo").value("rodrigo@ejemplo.cr"))
        .andExpect(jsonPath("$.roles", contains("CAFETALERO")))
        .andExpect(jsonPath("$.telefono").doesNotExist())
        .andExpect(jsonPath("$.fotoUrl").doesNotExist())
        .andExpect(jsonPath("$.redes").isArray())
        .andExpect(jsonPath("$.redes").isEmpty());
  }

  @Test
  void registraConLosTresRoles() throws Exception {
    MvcResult resultado =
        registrar(
            registroValido("mariana@ejemplo.cr", "[\"CAFETERIA\", \"TOSTADOR\", \"CAFETALERO\"]"));

    assertThat(resultado.getResponse().getStatus()).isEqualTo(201);
    mockMvc
        .perform(get("/api/cuenta/actual").cookie(cookieDe(resultado)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.roles", contains("CAFETALERO", "TOSTADOR", "CAFETERIA")));
  }

  @Test
  void registraMultipartConFotoOpcional() throws Exception {
    MockMultipartFile datos =
        new MockMultipartFile(
            "datos",
            "",
            MediaType.APPLICATION_JSON_VALUE,
            registroValido("foto@ejemplo.cr", "[\"TOSTADOR\"]").getBytes(StandardCharsets.UTF_8));
    MockMultipartFile foto =
        new MockMultipartFile("foto", "perfil.webp", "image/webp", new byte[] {9, 8, 7});

    when(almacenamientoImagenes.subirFoto(any()))
        .thenReturn("https://res.cloudinary.com/cranhua/image/upload/v1/cranhua/fotos/reg.webp");

    MvcResult resultado =
        mockMvc
            .perform(multipart("/api/auth/registro").file(datos).file(foto))
            .andExpect(status().isCreated())
            .andExpect(
                jsonPath("$.fotoUrl")
                    .value(
                        "https://res.cloudinary.com/cranhua/image/upload/v1/cranhua/fotos/reg.webp"))
            .andReturn();

    Cuenta guardada =
        cuentaRepository.findByCorreoAndEliminadaFalse("foto@ejemplo.cr").orElseThrow();
    assertThat(guardada.getFotoUrl())
        .isEqualTo("https://res.cloudinary.com/cranhua/image/upload/v1/cranhua/fotos/reg.webp");
    assertThat(resultado.getResponse().getCookie(CookieSesion.NOMBRE)).isNotNull();
    verify(almacenamientoImagenes).subirFoto(any());
  }

  @Test
  void registraMultipartSinFotoNoSubeImagen() throws Exception {
    MockMultipartFile datos =
        new MockMultipartFile(
            "datos",
            "",
            MediaType.APPLICATION_JSON_VALUE,
            registroValido("sin-foto@ejemplo.cr", "[\"CAFETERIA\"]")
                .getBytes(StandardCharsets.UTF_8));

    mockMvc
        .perform(multipart("/api/auth/registro").file(datos))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.fotoUrl").doesNotExist());

    verify(almacenamientoImagenes, never()).subirFoto(any());
  }

  @Test
  void registraMultipartConMimeInvalidoDa400() throws Exception {
    MockMultipartFile datos =
        new MockMultipartFile(
            "datos",
            "",
            MediaType.APPLICATION_JSON_VALUE,
            registroValido("mime-malo@ejemplo.cr", "[\"TOSTADOR\"]")
                .getBytes(StandardCharsets.UTF_8));
    MockMultipartFile foto =
        new MockMultipartFile("foto", "doc.pdf", "application/pdf", new byte[] {1});

    when(almacenamientoImagenes.subirFoto(any()))
        .thenThrow(
            new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Solo se aceptan imágenes JPG, PNG o WebP."));

    mockMvc
        .perform(multipart("/api/auth/registro").file(datos).file(foto))
        .andExpect(status().isBadRequest());

    verify(almacenamientoImagenes).subirFoto(any());
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
        .perform(get("/api/cuenta/actual").cookie(cookieDe(resultado)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.correo").value("andres.solano@gmail.com"))
        .andExpect(jsonPath("$.fotoUrl").value("https://lh3.googleusercontent.com/a/foto"))
        .andExpect(jsonPath("$.telefono").doesNotExist())
        .andExpect(jsonPath("$.redes").isEmpty());
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

  @Test
  void loginValidoDevuelveLaCookieConElJwt() throws Exception {
    Cuenta cuenta = guardarCuenta("mariana@ejemplo.cr", EnumSet.allOf(Rol.class));

    MvcResult resultado = iniciarSesion("  Mariana@Ejemplo.CR ", "Cafe2026!");

    assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
    assertThat(resultado.getResponse().getContentAsString()).doesNotContain("contrasena", "$2a$");
    assertThat(resultado.getResponse().getHeader(HttpHeaders.SET_COOKIE))
        .startsWith(CookieSesion.NOMBRE + "=")
        .contains("Path=/", "Max-Age=28800", "HttpOnly", "Secure", "SameSite=Strict");

    mockMvc
        .perform(get("/api/cuenta/actual").cookie(cookieDe(resultado)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(cuenta.getId().toString()))
        .andExpect(jsonPath("$.roles", contains("CAFETALERO", "TOSTADOR", "CAFETERIA")))
        .andExpect(jsonPath("$.telefono").doesNotExist())
        .andExpect(jsonPath("$.fotoUrl").doesNotExist())
        .andExpect(jsonPath("$.redes").isEmpty());
  }

  @Test
  void elJwtTraeTodosLosRolesYVenceEnOchoHoras() throws Exception {
    Cuenta cuenta = guardarCuenta("lucia@ejemplo.cr", Set.of(Rol.TOSTADOR, Rol.CAFETERIA));

    Jwt jwt =
        jwtDecoder.decode(cookieDe(iniciarSesion("lucia@ejemplo.cr", "Cafe2026!")).getValue());

    assertThat(jwt.getSubject()).isEqualTo(cuenta.getId().toString());
    assertThat(jwt.getClaimAsStringList("roles")).containsExactly("TOSTADOR", "CAFETERIA");
    assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt()))
        .isEqualTo(Duration.ofHours(8));
  }

  // Correo inexistente y contraseña equivocada tienen que verse exactamente igual
  @ParameterizedTest(name = "{0} / {1}")
  @CsvSource({
    "rodrigo@ejemplo.cr, Equivocada1",
    "noexiste@ejemplo.cr, Cafe2026!",
    "rodrigo@ejemplo.cr, CAFE2026!"
  })
  void credencialesIncorrectasDan401ConMensajeGenerico(String correo, String contrasena)
      throws Exception {
    guardarCuenta("rodrigo@ejemplo.cr", Set.of(Rol.CAFETALERO));

    MvcResult resultado = iniciarSesion(correo, contrasena);

    assertThat(resultado.getResponse().getStatus()).isEqualTo(401);
    assertThat(resultado.getResponse().getContentAsString())
        .isEqualTo("{\"mensaje\":\"Correo o contraseña incorrectos.\"}");
    assertThat(resultado.getResponse().getHeader(HttpHeaders.SET_COOKIE)).isNull();
  }

  @Test
  void unaCuentaEliminadaNoPuedeIniciarSesion() throws Exception {
    Cuenta cuenta = guardarCuenta("baja@ejemplo.cr", Set.of(Rol.TOSTADOR));
    cuenta.eliminar();
    cuentaRepository.saveAndFlush(cuenta);

    MvcResult resultado = iniciarSesion("baja@ejemplo.cr", "Cafe2026!");

    assertThat(resultado.getResponse().getStatus()).isEqualTo(401);
    assertThat(resultado.getResponse().getContentAsString())
        .contains("Correo o contraseña incorrectos.");
    assertThat(resultado.getResponse().getHeader(HttpHeaders.SET_COOKIE)).isNull();
  }

  @Test
  void unaCuentaDeGoogleNoEntraConContrasena() throws Exception {
    guardarCuentaGoogle("andres.solano@gmail.com", "google-sub-123");

    MvcResult resultado = iniciarSesion("andres.solano@gmail.com", "Cafe2026!");

    assertThat(resultado.getResponse().getStatus()).isEqualTo(401);
    assertThat(resultado.getResponse().getContentAsString())
        .contains("Correo o contraseña incorrectos.");
  }

  @Test
  void loginSinDatosDa400() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/iniciarSesion")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\": \" \"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errores.correo").value("Ingresá tu correo."))
        .andExpect(jsonPath("$.errores.contrasena").value("Ingresá tu contraseña."));
  }

  @Test
  void iniciaSesionConGoogle() throws Exception {
    Cuenta cuenta = guardarCuentaGoogle("andres.solano@gmail.com", "google-sub-123");
    googleDevuelve("token-valido", "google-sub-123", "andres.solano@gmail.com");

    MvcResult resultado = iniciarSesionConGoogle("token-valido");

    assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
    Jwt jwt = jwtDecoder.decode(cookieDe(resultado).getValue());
    assertThat(jwt.getSubject()).isEqualTo(cuenta.getId().toString());
    assertThat(jwt.getClaimAsStringList("roles")).containsExactly("CAFETERIA");
  }

  @Test
  void googleSinCuentaRegistradaDa401() throws Exception {
    googleDevuelve("token-valido", "google-sub-999", "nadie@gmail.com");

    MvcResult resultado = iniciarSesionConGoogle("token-valido");

    assertThat(resultado.getResponse().getStatus()).isEqualTo(401);
    assertThat(resultado.getResponse().getContentAsString())
        .contains("No hay una cuenta registrada con esa cuenta de Google.");
    assertThat(resultado.getResponse().getHeader(HttpHeaders.SET_COOKIE)).isNull();
  }

  @Test
  void googleConCuentaEliminadaDa401() throws Exception {
    Cuenta cuenta = guardarCuentaGoogle("andres.solano@gmail.com", "google-sub-123");
    cuenta.eliminar();
    cuentaRepository.saveAndFlush(cuenta);
    googleDevuelve("token-valido", "google-sub-123", "andres.solano@gmail.com");

    assertThat(iniciarSesionConGoogle("token-valido").getResponse().getStatus()).isEqualTo(401);
  }

  @Test
  void googleConTokenInvalidoDa401() throws Exception {
    when(verificadorGoogle.verificar("token-falso")).thenReturn(Optional.empty());

    MvcResult resultado = iniciarSesionConGoogle("token-falso");

    assertThat(resultado.getResponse().getStatus()).isEqualTo(401);
    assertThat(resultado.getResponse().getContentAsString())
        .contains("No se pudo verificar la cuenta de Google.");
  }

  @Test
  void cerrarSesionBorraLaCookie() throws Exception {
    guardarCuenta("rodrigo@ejemplo.cr", Set.of(Rol.CAFETALERO));
    Cookie sesion = cookieDe(iniciarSesion("rodrigo@ejemplo.cr", "Cafe2026!"));

    MvcResult resultado =
        mockMvc.perform(post("/api/auth/cerrarSesion").cookie(sesion)).andReturn();

    assertThat(resultado.getResponse().getStatus()).isEqualTo(204);
    assertThat(resultado.getResponse().getHeader(HttpHeaders.SET_COOKIE))
        .startsWith(CookieSesion.NOMBRE + "=;")
        .contains("Path=/", "Max-Age=0", "HttpOnly", "Secure", "SameSite=Strict");
  }

  @Test
  void tokenVencidoDa401() throws Exception {
    Cuenta cuenta = guardarCuenta("rodrigo@ejemplo.cr", Set.of(Rol.CAFETALERO));
    Instant haceNueveHoras = Instant.now().minus(Duration.ofHours(9));
    String vencido =
        firmar(jwtEncoder, cuenta, haceNueveHoras, haceNueveHoras.plus(Duration.ofHours(8)));

    mockMvc
        .perform(get("/api/cuenta/actual").cookie(new Cookie(CookieSesion.NOMBRE, vencido)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void tokenFirmadoConOtraClaveDa401() throws Exception {
    Cuenta cuenta = guardarCuenta("rodrigo@ejemplo.cr", Set.of(Rol.CAFETALERO));
    byte[] otraClave = "otra-clave-que-no-es-la-del-backend!!".getBytes(StandardCharsets.UTF_8);
    JwtEncoder otroEmisor =
        new NimbusJwtEncoder(new ImmutableSecret<>(new SecretKeySpec(otraClave, "HmacSHA256")));
    Instant ahora = Instant.now();
    String falso = firmar(otroEmisor, cuenta, ahora, ahora.plus(Duration.ofHours(8)));

    mockMvc
        .perform(get("/api/cuenta/actual").cookie(new Cookie(CookieSesion.NOMBRE, falso)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void unaCookieInvalidaNoImpideVolverAIniciarSesion() throws Exception {
    guardarCuenta("rodrigo@ejemplo.cr", Set.of(Rol.CAFETALERO));

    mockMvc
        .perform(
            post("/api/auth/iniciarSesion")
                .cookie(new Cookie(CookieSesion.NOMBRE, "basura"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\": \"rodrigo@ejemplo.cr\", \"contrasena\": \"Cafe2026!\"}"))
        .andExpect(status().isOk());
  }

  private static String firmar(JwtEncoder emisor, Cuenta cuenta, Instant emitido, Instant vence) {
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .subject(cuenta.getId().toString())
            .issuedAt(emitido)
            .expiresAt(vence)
            .claim("roles", List.of("CAFETALERO"))
            .build();
    return emisor
        .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
        .getTokenValue();
  }
}
