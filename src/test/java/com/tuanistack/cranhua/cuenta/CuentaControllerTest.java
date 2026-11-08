package com.tuanistack.cranhua.cuenta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tuanistack.cranhua.BasePostgis;
import com.tuanistack.cranhua.auth.CookieSesion;
import com.tuanistack.cranhua.storage.AlmacenamientoImagenes;
import jakarta.servlet.http.Cookie;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(BasePostgis.class)
class CuentaControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private CuentaRepository cuentaRepository;

  @Autowired private PasswordEncoder passwordEncoder;

  @MockitoBean private AlmacenamientoImagenes almacenamientoImagenes;

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
    cuenta.setBannerUrl("https://ejemplo.cr/banner.jpg");
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
        .andExpect(jsonPath("$.bannerUrl").value("https://ejemplo.cr/banner.jpg"))
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
                      "bannerUrl": "https://cdn.ejemplo.cr/banner.png",
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
        .andExpect(jsonPath("$.bannerUrl").value("https://cdn.ejemplo.cr/banner.png"))
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
        .andExpect(jsonPath("$.bannerUrl").value("https://cdn.ejemplo.cr/banner.png"))
        .andExpect(jsonPath("$.redes", hasSize(2)))
        .andExpect(jsonPath("$.correo").value("rodrigo@ejemplo.cr"))
        .andExpect(jsonPath("$.roles", contains("TOSTADOR")));
  }

  @Test
  void postFotoGuardaUrlDeCloudinaryYReemplazoActualizaUrl() throws Exception {
    guardarCuenta("rodrigo@ejemplo.cr");
    Cookie sesion = iniciarSesion("rodrigo@ejemplo.cr");
    MockMultipartFile archivo =
        new MockMultipartFile("archivo", "foto.jpg", "image/jpeg", new byte[] {1, 2, 3});

    when(almacenamientoImagenes.subirFoto(any()))
        .thenReturn("https://res.cloudinary.com/cranhua/image/upload/v1/cranhua/fotos/a.jpg")
        .thenReturn("https://res.cloudinary.com/cranhua/image/upload/v1/cranhua/fotos/b.jpg");

    mockMvc
        .perform(multipart("/api/cuenta/foto").file(archivo).cookie(sesion))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.fotoUrl")
                .value("https://res.cloudinary.com/cranhua/image/upload/v1/cranhua/fotos/a.jpg"));

    mockMvc
        .perform(multipart("/api/cuenta/foto").file(archivo).cookie(sesion))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.fotoUrl")
                .value("https://res.cloudinary.com/cranhua/image/upload/v1/cranhua/fotos/b.jpg"));

    Cuenta actualizada =
        cuentaRepository.findByCorreoAndEliminadaFalse("rodrigo@ejemplo.cr").orElseThrow();
    assertThat(actualizada.getFotoUrl())
        .isEqualTo("https://res.cloudinary.com/cranhua/image/upload/v1/cranhua/fotos/b.jpg");
  }

  @Test
  void postBannerGuardaUrlDeCloudinaryYReemplazoActualizaUrl() throws Exception {
    guardarCuenta("rodrigo@ejemplo.cr");
    Cookie sesion = iniciarSesion("rodrigo@ejemplo.cr");
    MockMultipartFile archivo =
        new MockMultipartFile("archivo", "banner.png", "image/png", new byte[] {4, 5, 6});

    when(almacenamientoImagenes.subirBanner(any()))
        .thenReturn("https://res.cloudinary.com/cranhua/image/upload/v1/cranhua/banners/a.png")
        .thenReturn("https://res.cloudinary.com/cranhua/image/upload/v1/cranhua/banners/b.png");

    mockMvc
        .perform(multipart("/api/cuenta/banner").file(archivo).cookie(sesion))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.bannerUrl")
                .value("https://res.cloudinary.com/cranhua/image/upload/v1/cranhua/banners/a.png"));

    mockMvc
        .perform(multipart("/api/cuenta/banner").file(archivo).cookie(sesion))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.bannerUrl")
                .value("https://res.cloudinary.com/cranhua/image/upload/v1/cranhua/banners/b.png"));

    Cuenta actualizada =
        cuentaRepository.findByCorreoAndEliminadaFalse("rodrigo@ejemplo.cr").orElseThrow();
    assertThat(actualizada.getBannerUrl())
        .isEqualTo("https://res.cloudinary.com/cranhua/image/upload/v1/cranhua/banners/b.png");
  }

  @Test
  void postFotoConMimeInvalidoDa400() throws Exception {
    guardarCuenta("rodrigo@ejemplo.cr");
    Cookie sesion = iniciarSesion("rodrigo@ejemplo.cr");
    MockMultipartFile archivo =
        new MockMultipartFile("archivo", "doc.pdf", "application/pdf", new byte[] {1});

    when(almacenamientoImagenes.subirFoto(any()))
        .thenThrow(
            new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Solo se aceptan imágenes JPG, PNG o WebP."));

    mockMvc
        .perform(multipart("/api/cuenta/foto").file(archivo).cookie(sesion))
        .andExpect(status().isBadRequest());

    verify(almacenamientoImagenes).subirFoto(any());
    verify(almacenamientoImagenes, never()).subirBanner(any());

    Cuenta sinCambios =
        cuentaRepository.findByCorreoAndEliminadaFalse("rodrigo@ejemplo.cr").orElseThrow();
    assertThat(sinCambios.getFotoUrl()).isNull();
  }

  @Test
  void postBannerConMimeInvalidoDa400() throws Exception {
    guardarCuenta("rodrigo@ejemplo.cr");
    Cookie sesion = iniciarSesion("rodrigo@ejemplo.cr");
    MockMultipartFile archivo =
        new MockMultipartFile("archivo", "doc.gif", "image/gif", new byte[] {1});

    when(almacenamientoImagenes.subirBanner(any()))
        .thenThrow(
            new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Solo se aceptan imágenes JPG, PNG o WebP."));

    mockMvc
        .perform(multipart("/api/cuenta/banner").file(archivo).cookie(sesion))
        .andExpect(status().isBadRequest());

    verify(almacenamientoImagenes).subirBanner(any());
    verify(almacenamientoImagenes, never()).subirFoto(any());

    Cuenta sinCambios =
        cuentaRepository.findByCorreoAndEliminadaFalse("rodrigo@ejemplo.cr").orElseThrow();
    assertThat(sinCambios.getBannerUrl()).isNull();
  }

  @Test
  void postFotoConArchivoDemasiadoGrandeDa400() throws Exception {
    guardarCuenta("rodrigo@ejemplo.cr");
    Cookie sesion = iniciarSesion("rodrigo@ejemplo.cr");
    MockMultipartFile archivo =
        new MockMultipartFile("archivo", "grande.jpg", "image/jpeg", new byte[] {1, 2, 3});

    when(almacenamientoImagenes.subirFoto(any()))
        .thenThrow(
            new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "La imagen no puede superar los 5 MB."));

    mockMvc
        .perform(multipart("/api/cuenta/foto").file(archivo).cookie(sesion))
        .andExpect(status().isBadRequest());

    verify(almacenamientoImagenes).subirFoto(any());
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
