package com.tuanistack.cranhua.finca;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
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
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
class FincaControllerTest {

  // Cuadrado de unos 1,1 km por lado en Tarrazú, sin repetir el primer punto
  private static final List<Map<String, Double>> CUADRADO =
      List.of(
          punto(9.650, -84.030),
          punto(9.650, -84.020),
          punto(9.660, -84.020),
          punto(9.660, -84.030));

  private static final List<Map<String, Double>> SOBREPUESTO =
      List.of(
          punto(9.650, -84.025),
          punto(9.650, -84.015),
          punto(9.660, -84.015),
          punto(9.660, -84.025));

  // Comparte con CUADRADO el borde de lng -84.020
  private static final List<Map<String, Double>> COLINDANTE =
      List.of(
          punto(9.650, -84.020),
          punto(9.650, -84.010),
          punto(9.660, -84.010),
          punto(9.660, -84.020));

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private CookieSesion cookieSesion;

  @Autowired private CuentaRepository cuentaRepository;

  @Autowired private FincaRepository fincaRepository;

  private Cuenta andres;

  private Cuenta lucia;

  @BeforeEach
  void crearCuentas() {
    andres = cuenta("Andrés Solano", "andres@ejemplo.cr", Rol.CAFETALERO);
    lucia = cuenta("Lucía Mora", "lucia@ejemplo.cr", Rol.CAFETALERO);
  }

  @AfterEach
  void limpiar() {
    fincaRepository.deleteAll();
    cuentaRepository.deleteAll();
  }

  private static Map<String, Double> punto(double lat, double lng) {
    return Map.of("lat", lat, "lng", lng);
  }

  private Cuenta cuenta(String nombre, String correo, Rol rol) {
    return cuentaRepository.saveAndFlush(new Cuenta(nombre, correo, "hash", Set.of(rol)));
  }

  private Cookie sesion(Cuenta cuenta) {
    return new Cookie(CookieSesion.NOMBRE, cookieSesion.crear(cuenta).getValue());
  }

  private static Map<String, Object> finca(String nombre, List<Map<String, Double>> perimetro) {
    Map<String, Object> finca = new LinkedHashMap<>();
    finca.put("nombre", nombre);
    finca.put("descripcion", "Café de altura con sombra de guaba.");
    finca.put("zonaCafetalera", "Tarrazú");
    finca.put("perimetro", perimetro);
    finca.put("ubicacion", punto(9.655, -84.025));
    finca.put("canton", "Tarrazú");
    finca.put("provincia", "San José");
    return finca;
  }

  private ResultActions registrar(Cuenta cuenta, Map<String, Object> finca) throws Exception {
    return mockMvc.perform(
        post("/api/fincas")
            .cookie(sesion(cuenta))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(finca)));
  }

  @Test
  void cafetaleroRegistraUnaFinca() throws Exception {
    String respuesta =
        registrar(andres, finca("Finca La Catalina", CUADRADO))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", startsWith("/api/fincas/")))
            .andExpect(jsonPath("$.nombre").value("Finca La Catalina"))
            .andExpect(jsonPath("$.zonaCafetalera").value("Tarrazú"))
            .andExpect(jsonPath("$.ubicacion.lat").value(9.655))
            .andExpect(jsonPath("$.ubicacion.lng").value(-84.025))
            .andExpect(jsonPath("$.canton").value("Tarrazú"))
            .andExpect(jsonPath("$.provincia").value("San José"))
            .andExpect(jsonPath("$.areaHectareas", greaterThan(0.0)))
            .andExpect(jsonPath("$.duenos[*].nombreCompleto", contains("Andrés Solano")))
            .andReturn()
            .getResponse()
            .getContentAsString();

    UUID id = UUID.fromString(objectMapper.readTree(respuesta).get("id").asText());
    assertThat(fincaRepository.findByDuenosIdAndEliminadaFalse(andres.getId()))
        .extracting(Finca::getId)
        .containsExactly(id);
  }

  @Test
  void elAreaSeCalculaEnHectareas() throws Exception {
    // 0,01° de lat por 0,01° de lng en Tarrazú son unas 121 ha
    registrar(andres, finca("Finca La Catalina", CUADRADO))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.areaHectareas", closeTo(121.0, 1.0)));
  }

  // Google Maps manda el perímetro abierto: el backend cierra el anillo antes de guardarlo
  @Test
  void perimetroAbiertoSeCierraYCerradoTambienSeAcepta() throws Exception {
    registrar(andres, finca("Abierta", CUADRADO))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.perimetro", hasSize(4)))
        .andExpect(jsonPath("$.perimetro[0].lat").value(9.650))
        .andExpect(jsonPath("$.perimetro[0].lng").value(-84.030));
    fincaRepository.deleteAll();

    List<Map<String, Double>> cerrado =
        Stream.concat(CUADRADO.stream(), Stream.of(CUADRADO.get(0))).toList();
    registrar(andres, finca("Cerrada", cerrado))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.perimetro", hasSize(4)));

    Finca guardada = fincaRepository.findAll().get(0);
    assertThat(guardada.getPerimetro().getExteriorRing().isClosed()).isTrue();
    assertThat(guardada.getPerimetro().getExteriorRing().getNumPoints()).isEqualTo(5);
    assertThat(guardada.getPerimetro().getSRID()).isEqualTo(4326);
  }

  @Test
  void sinSesionDa401() throws Exception {
    mockMvc
        .perform(
            post("/api/fincas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(finca("Sin sesión", CUADRADO))))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void tostadorDa403() throws Exception {
    Cuenta tostador = cuenta("Mario Brenes", "mario@ejemplo.cr", Rol.TOSTADOR);

    registrar(tostador, finca("De tostador", CUADRADO)).andExpect(status().isForbidden());
    assertThat(fincaRepository.count()).isZero();
  }

  static Stream<Arguments> camposInvalidos() {
    return Stream.of(
        Arguments.of("nombre", "   ", "nombre", "Ingresá el nombre de la finca."),
        Arguments.of(
            "descripcion",
            "a".repeat(301),
            "descripcion",
            "La descripción no puede tener más de 300 caracteres."),
        Arguments.of("zonaCafetalera", "Zona Norte", "zonaCafetalera", "Valor inválido."),
        Arguments.of("canton", null, "canton", "Ingresá el cantón."),
        Arguments.of("ubicacion", null, "ubicacion", "Marcá la ubicación de la finca."),
        Arguments.of(
            "ubicacion",
            punto(95.0, -84.0),
            "ubicacion.lat",
            "La latitud tiene que estar entre -90 y 90."),
        Arguments.of(
            "perimetro",
            List.of(punto(9.65, -84.03), punto(9.65, -184.0), punto(9.66, -84.02)),
            "perimetro[1].lng",
            "La longitud tiene que estar entre -180 y 180."));
  }

  @ParameterizedTest
  @MethodSource("camposInvalidos")
  void datosInvalidosDan400PorCampo(String campo, Object valor, String error, String mensaje)
      throws Exception {
    Map<String, Object> finca = finca("Finca La Catalina", CUADRADO);
    finca.put(campo, valor);

    registrar(andres, finca)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errores['" + error + "']").value(mensaje));
    assertThat(fincaRepository.count()).isZero();
  }

  static Stream<Arguments> perimetrosInvalidos() {
    return Stream.of(
        Arguments.of(
            List.of(punto(9.650, -84.030), punto(9.660, -84.020)),
            "El perímetro necesita al menos 3 puntos."),
        Arguments.of(
            List.of(punto(9.650, -84.030), punto(9.650, -84.030), punto(9.650, -84.030)),
            "El perímetro necesita al menos 3 puntos."),
        // Forma de 8: el segundo y el cuarto lado se cruzan
        Arguments.of(
            List.of(
                punto(9.650, -84.030),
                punto(9.660, -84.020),
                punto(9.650, -84.020),
                punto(9.660, -84.030)),
            "El perímetro tiene líneas que se cruzan."),
        // Tres puntos en línea recta: área cero
        Arguments.of(
            List.of(punto(9.650, -84.030), punto(9.650, -84.025), punto(9.650, -84.020)),
            "El perímetro tiene líneas que se cruzan."));
  }

  @ParameterizedTest
  @MethodSource("perimetrosInvalidos")
  void perimetroInvalidoDa400(List<Map<String, Double>> perimetro, String mensaje)
      throws Exception {
    registrar(andres, finca("Finca La Catalina", perimetro))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errores.perimetro").value(mensaje));
    assertThat(fincaRepository.count()).isZero();
  }

  @Test
  void sobreponerseConOtraFincaDa409SinGuardar() throws Exception {
    String respuesta =
        registrar(lucia, finca("Finca El Tucán", CUADRADO))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String idTucan = objectMapper.readTree(respuesta).get("id").asText();

    registrar(andres, finca("Finca La Catalina", SOBREPUESTO))
        .andExpect(status().isConflict())
        .andExpect(
            jsonPath("$.mensaje")
                .value("El terreno se sobrepone con la finca \"Finca El Tucán\" de Lucía Mora."))
        .andExpect(jsonPath("$.conflicto.fincaId").value(idTucan))
        .andExpect(jsonPath("$.conflicto.nombre").value("Finca El Tucán"))
        .andExpect(jsonPath("$.conflicto.duenos", contains("Lucía Mora")))
        .andExpect(jsonPath("$.conflicto.perimetro", hasSize(4)))
        .andExpect(jsonPath("$.conflicto.perimetro[0].lat").value(9.650))
        .andExpect(jsonPath("$.conflicto.perimetro[0].lng").value(-84.030))
        .andExpect(jsonPath("$.conflicto.perimetro[2].lat").value(9.660))
        .andExpect(jsonPath("$.conflicto.perimetro[2].lng").value(-84.020));
    assertThat(fincaRepository.count()).isEqualTo(1);
  }

  @Test
  void compartirUnBordeSePermite() throws Exception {
    registrar(lucia, finca("Finca El Tucán", CUADRADO)).andExpect(status().isCreated());

    registrar(andres, finca("Finca La Catalina", COLINDANTE)).andExpect(status().isCreated());
    assertThat(fincaRepository.count()).isEqualTo(2);
  }

  @Test
  void sobreponerseConUnaFincaEliminadaSePermite() throws Exception {
    registrar(lucia, finca("Finca Vieja", CUADRADO)).andExpect(status().isCreated());
    Finca vieja = fincaRepository.findAll().get(0);
    vieja.eliminar();
    fincaRepository.saveAndFlush(vieja);

    registrar(andres, finca("Finca La Catalina", SOBREPUESTO)).andExpect(status().isCreated());
    assertThat(fincaRepository.count()).isEqualTo(2);
  }
}
