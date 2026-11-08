package com.tuanistack.cranhua.tueste;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuanistack.cranhua.BasePostgis;
import com.tuanistack.cranhua.auth.CookieSesion;
import com.tuanistack.cranhua.cuenta.Cuenta;
import com.tuanistack.cranhua.cuenta.CuentaRepository;
import com.tuanistack.cranhua.cuenta.Rol;
import jakarta.servlet.http.Cookie;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(BasePostgis.class)
class NegocioTuesteControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private CookieSesion cookieSesion;

  @Autowired private CuentaRepository cuentaRepository;

  @Autowired private NegocioTuesteRepository negocioRepository;

  private Cuenta mariana;

  private Cuenta pablo;

  @BeforeEach
  void crearCuentas() {
    mariana = cuenta("Mariana Castro", "mariana@ejemplo.cr", Rol.TOSTADOR);
    pablo = cuenta("Pablo Vargas", "pablo@ejemplo.cr", Rol.TOSTADOR);
  }

  @AfterEach
  void limpiar() {
    negocioRepository.deleteAll();
    cuentaRepository.deleteAll();
  }

  private Cuenta cuenta(String nombre, String correo, Rol rol) {
    return cuentaRepository.saveAndFlush(new Cuenta(nombre, correo, "hash", Set.of(rol)));
  }

  private Cookie sesion(Cuenta cuenta) {
    return new Cookie(CookieSesion.NOMBRE, cookieSesion.crear(cuenta).getValue());
  }

  private static Map<String, Object> negocio(String nombre) {
    Map<String, Object> negocio = new LinkedHashMap<>();
    negocio.put("nombre", nombre);
    negocio.put("nombreContacto", "Mariana Castro");
    negocio.put("telefono", "+506 8888-1234");
    negocio.put("correo", "Volcan@Ejemplo.CR");
    negocio.put("descripcion", "Perfiles claros para resaltar la acidez.");
    negocio.put("canton", "San José");
    negocio.put("provincia", "San José");
    negocio.put("estilos", List.of("MEDIO", "CLARO"));
    return negocio;
  }

  private ResultActions registrar(Cuenta cuenta, Map<String, Object> negocio) throws Exception {
    return mockMvc.perform(
        post("/api/tostadores")
            .cookie(sesion(cuenta))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(negocio)));
  }

  private String registrarId(Cuenta cuenta) throws Exception {
    String respuesta =
        registrar(cuenta, negocio("Tostaduría Volcán"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    return objectMapper.readTree(respuesta).get("id").asText();
  }

  private ResultActions editar(Cuenta cuenta, String id, Map<String, Object> cambios)
      throws Exception {
    return mockMvc.perform(
        patch("/api/tostadores/" + id)
            .cookie(sesion(cuenta))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(cambios)));
  }

  @Test
  void tostadorRegistraSuNegocio() throws Exception {
    registrar(mariana, negocio(" Tostaduría Volcán "))
        .andExpect(status().isCreated())
        .andExpect(header().exists("Location"))
        .andExpect(jsonPath("$.cuentaId").value(mariana.getId().toString()))
        .andExpect(jsonPath("$.nombre").value("Tostaduría Volcán"))
        .andExpect(jsonPath("$.correo").value("volcan@ejemplo.cr"))
        .andExpect(jsonPath("$.estilos", contains("CLARO", "MEDIO")))
        .andExpect(jsonPath("$.portadaUrl").doesNotExist());
  }

  @Test
  void segundoNegocioDeLaMismaCuentaResponde409() throws Exception {
    registrar(mariana, negocio("Tostaduría Volcán")).andExpect(status().isCreated());

    registrar(mariana, negocio("Otro Tostador")).andExpect(status().isConflict());
  }

  @Test
  void camposObligatoriosFaltantesResponden400PorCampo() throws Exception {
    registrar(mariana, Map.of("descripcion", "Sin datos"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errores.nombre").value("Ingresá el nombre del negocio."))
        .andExpect(jsonPath("$.errores.nombreContacto").exists())
        .andExpect(jsonPath("$.errores.telefono").exists())
        .andExpect(jsonPath("$.errores.correo").exists())
        .andExpect(jsonPath("$.errores.canton").exists())
        .andExpect(jsonPath("$.errores.provincia").exists())
        .andExpect(jsonPath("$.errores.estilos").exists());
  }

  @Test
  void soloUnTostadorRegistraYEdita() throws Exception {
    Cuenta cafetalero = cuenta("Andrés Solano", "andres@ejemplo.cr", Rol.CAFETALERO);
    String id = registrarId(mariana);

    registrar(cafetalero, negocio("Tostaduría Volcán")).andExpect(status().isForbidden());
    editar(cafetalero, id, Map.of("nombre", "Otro")).andExpect(status().isForbidden());
    mockMvc.perform(get("/api/tostadores/" + id)).andExpect(status().isUnauthorized());
  }

  @Test
  void cualquierRolConsultaElNegocio() throws Exception {
    Cuenta cafetalero = cuenta("Andrés Solano", "andres@ejemplo.cr", Rol.CAFETALERO);
    String id = registrarId(mariana);

    mockMvc
        .perform(get("/api/tostadores/" + id).cookie(sesion(cafetalero)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nombre").value("Tostaduría Volcán"));
  }

  @Test
  void editaLosDatosDelNegocio() throws Exception {
    String id = registrarId(mariana);
    Map<String, Object> cambios = new LinkedHashMap<>();
    cambios.put("nombre", "Volcán Tostadores");
    cambios.put("nombreContacto", "Luis Rojas");
    cambios.put("telefono", "+506 7777-0000");
    cambios.put("correo", "hola@volcan.cr");
    cambios.put("canton", "Escazú");
    cambios.put("provincia", "San José");
    cambios.put("descripcion", " ");
    cambios.put("estilos", List.of("OSCURO"));
    cambios.put("portadaUrl", "https://res.cloudinary.com/cranhua/portada.jpg");

    editar(mariana, id, cambios)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nombre").value("Volcán Tostadores"))
        .andExpect(jsonPath("$.nombreContacto").value("Luis Rojas"))
        .andExpect(jsonPath("$.telefono").value("+506 7777-0000"))
        .andExpect(jsonPath("$.correo").value("hola@volcan.cr"))
        .andExpect(jsonPath("$.canton").value("Escazú"))
        .andExpect(jsonPath("$.descripcion").doesNotExist())
        .andExpect(jsonPath("$.estilos", contains("OSCURO")))
        .andExpect(
            jsonPath("$.portadaUrl").value("https://res.cloudinary.com/cranhua/portada.jpg"));

    mockMvc
        .perform(get("/api/tostadores/" + id).cookie(sesion(mariana)))
        .andExpect(jsonPath("$.nombre").value("Volcán Tostadores"))
        .andExpect(jsonPath("$.estilos", contains("OSCURO")));
  }

  @Test
  void editarSoloCambiaLoQueViene() throws Exception {
    String id = registrarId(mariana);

    editar(mariana, id, Map.of("telefono", "+506 7777-0000"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nombre").value("Tostaduría Volcán"))
        .andExpect(jsonPath("$.descripcion").value("Perfiles claros para resaltar la acidez."))
        .andExpect(jsonPath("$.estilos", contains("CLARO", "MEDIO")));
  }

  @Test
  void editarConDatosInvalidosResponde400() throws Exception {
    String id = registrarId(mariana);

    editar(mariana, id, Map.of("nombre", " ", "estilos", List.of()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errores.nombre").exists())
        .andExpect(jsonPath("$.errores.estilos").exists());
  }

  @Test
  void editarNegocioAjenoResponde403() throws Exception {
    String id = registrarId(mariana);

    editar(pablo, id, Map.of("nombre", "Robado")).andExpect(status().isForbidden());
  }

  @Test
  void negocioInexistenteResponde404() throws Exception {
    mockMvc
        .perform(get("/api/tostadores/" + UUID.randomUUID()).cookie(sesion(mariana)))
        .andExpect(status().isNotFound());
  }
}
