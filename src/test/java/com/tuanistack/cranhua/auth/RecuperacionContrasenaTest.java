package com.tuanistack.cranhua.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tuanistack.cranhua.BasePostgis;
import com.tuanistack.cranhua.cuenta.Cuenta;
import com.tuanistack.cranhua.cuenta.CuentaRepository;
import com.tuanistack.cranhua.cuenta.Rol;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(BasePostgis.class)
class RecuperacionContrasenaTest {

  private static final String CORREO = "rodrigo@ejemplo.cr";
  private static final String CODIGO = "123456";

  @Autowired private MockMvc mockMvc;

  @Autowired private CuentaRepository cuentaRepository;

  @Autowired private CodigoRecuperacionRepository codigoRepository;

  @Autowired private PasswordEncoder passwordEncoder;

  @MockitoBean private JavaMailSender mailSender;

  private Cuenta cuenta;

  @BeforeEach
  void crearCuenta() {
    cuenta =
        cuentaRepository.saveAndFlush(
            new Cuenta(
                "Rodrigo Alvarado",
                CORREO,
                passwordEncoder.encode("Cafe2026!"),
                Set.of(Rol.CAFETALERO)));
  }

  @AfterEach
  void limpiar() {
    codigoRepository.deleteAll();
    cuentaRepository.deleteAll();
  }

  private ResultActions enviar(String ruta, String body) throws Exception {
    return mockMvc.perform(post(ruta).contentType(MediaType.APPLICATION_JSON).content(body));
  }

  private ResultActions recuperar(String correo) throws Exception {
    return enviar("/api/auth/recuperar", "{\"correo\": \"%s\"}".formatted(correo));
  }

  private ResultActions verificar(String codigo) throws Exception {
    return enviar(
        "/api/auth/verificarCodigo",
        "{\"correo\": \"%s\", \"codigo\": \"%s\"}".formatted(CORREO, codigo));
  }

  private ResultActions restablecer(String codigo, String contrasena) throws Exception {
    return enviar(
        "/api/auth/restablecer",
        "{\"correo\": \"%s\", \"codigo\": \"%s\", \"contrasena\": \"%s\"}"
            .formatted(CORREO, codigo, contrasena));
  }

  private ResultActions iniciarSesion(String contrasena) throws Exception {
    return enviar(
        "/api/auth/iniciarSesion",
        "{\"correo\": \"%s\", \"contrasena\": \"%s\"}".formatted(CORREO, contrasena));
  }

  private void guardarCodigo(String codigo, Instant venceEn) {
    codigoRepository.saveAndFlush(
        new CodigoRecuperacion(cuenta.getId(), passwordEncoder.encode(codigo), venceEn));
  }

  private void guardarCodigoVigente() {
    guardarCodigo(CODIGO, Instant.now().plus(RecuperacionContrasenaService.VIGENCIA));
  }

  @Test
  void conCorreoRegistradoMandaUnCodigoDeSeisDigitosYLoGuardaCifrado() throws Exception {
    recuperar("Rodrigo@Ejemplo.cr").andExpect(status().isNoContent());

    ArgumentCaptor<SimpleMailMessage> mensaje = ArgumentCaptor.forClass(SimpleMailMessage.class);
    verify(mailSender, timeout(2000)).send(mensaje.capture());
    assertThat(mensaje.getValue().getTo()).containsExactly(CORREO);
    String codigo = mensaje.getValue().getText().replaceAll("(?s).*?(\\d{6}).*", "$1");
    assertThat(codigo).matches("\\d{6}");

    CodigoRecuperacion guardado = codigoRepository.findById(cuenta.getId()).orElseThrow();
    assertThat(guardado.getCodigoHash()).isNotEqualTo(codigo);
    assertThat(passwordEncoder.matches(codigo, guardado.getCodigoHash())).isTrue();
    verificar(codigo).andExpect(status().isNoContent());
  }

  @Test
  void conCorreoNoRegistradoRespondeIgualYNoMandaNada() throws Exception {
    String registrado = recuperar(CORREO).andReturn().getResponse().getContentAsString();
    verify(mailSender, timeout(2000)).send(any(SimpleMailMessage.class));

    ResultActions inexistente = recuperar("nadie@ejemplo.cr").andExpect(status().isNoContent());

    assertThat(inexistente.andReturn().getResponse().getContentAsString()).isEqualTo(registrado);
    verify(mailSender, after(500).times(1)).send(any(SimpleMailMessage.class));
  }

  @Test
  void pedirOtroCodigoReemplazaAlAnterior() throws Exception {
    guardarCodigoVigente();

    recuperar(CORREO).andExpect(status().isNoContent());

    assertThat(codigoRepository.count()).isEqualTo(1);
    verificar(CODIGO).andExpect(status().isBadRequest());
  }

  @Test
  void codigoEquivocadoDa400() throws Exception {
    guardarCodigoVigente();

    verificar("654321")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.mensaje").value("El código es incorrecto."));
  }

  @Test
  void codigoVencidoDa400() throws Exception {
    guardarCodigo(CODIGO, Instant.now().minusSeconds(1));

    verificar(CODIGO)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.mensaje").value("El código venció. Pedí uno nuevo."));
    assertThat(codigoRepository.count()).isZero();
  }

  @Test
  void sinCodigoPedidoDa400() throws Exception {
    verificar(CODIGO).andExpect(status().isBadRequest());
  }

  @Test
  void codigoConFormatoInvalidoDa400PorCampo() throws Exception {
    verificar("12ab")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errores.codigo").value("El código tiene 6 dígitos."));
  }

  @Test
  void despuesDeCincoFallosElCodigoSeInvalida() throws Exception {
    guardarCodigoVigente();
    for (int i = 0; i < RecuperacionContrasenaService.INTENTOS_MAXIMOS - 1; i++) {
      verificar("000000").andExpect(status().isBadRequest());
    }
    assertThat(codigoRepository.findById(cuenta.getId()).orElseThrow().getIntentosFallidos())
        .isEqualTo(4);

    verificar("000000").andExpect(status().isBadRequest());

    assertThat(codigoRepository.count()).isZero();
    verificar(CODIGO).andExpect(status().isBadRequest());
  }

  @Test
  void restablecerCambiaLaContrasenaYGastaElCodigo() throws Exception {
    guardarCodigoVigente();

    restablecer(CODIGO, "Nueva2026")
        .andExpect(status().isNoContent())
        .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));

    iniciarSesion("Nueva2026").andExpect(status().isOk());
    iniciarSesion("Cafe2026!").andExpect(status().isUnauthorized());
    restablecer(CODIGO, "Otra2026").andExpect(status().isBadRequest());
  }

  @Test
  void restablecerCierraLasSesionesActivas() throws Exception {
    Cookie sesion =
        iniciarSesion("Cafe2026!").andReturn().getResponse().getCookie(CookieSesion.NOMBRE);
    mockMvc.perform(get("/api/cuenta").cookie(sesion)).andExpect(status().isOk());
    guardarCodigoVigente();

    restablecer(CODIGO, "Nueva2026").andExpect(status().isNoContent());

    mockMvc.perform(get("/api/cuenta").cookie(sesion)).andExpect(status().isUnauthorized());
    Cookie nueva =
        iniciarSesion("Nueva2026").andReturn().getResponse().getCookie(CookieSesion.NOMBRE);
    mockMvc.perform(get("/api/cuenta").cookie(nueva)).andExpect(status().isOk());
  }

  @Test
  void laNuevaContrasenaSigueLasReglasDelRegistro() throws Exception {
    guardarCodigoVigente();

    restablecer(CODIGO, "corta1")
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.errores.contrasena")
                .value("La contraseña debe tener entre 8 y 72 caracteres."));
    restablecer(CODIGO, "soloLetras")
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.errores.contrasena")
                .value("La contraseña debe tener al menos una letra y un número."));

    verificar(CODIGO).andExpect(status().isNoContent());
  }
}
