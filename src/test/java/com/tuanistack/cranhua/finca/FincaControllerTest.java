package com.tuanistack.cranhua.finca;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import com.tuanistack.cranhua.finca.dto.Coordenada;
import jakarta.servlet.http.Cookie;
import java.math.BigDecimal;
import java.time.LocalDate;
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

  private static final List<Map<String, Double>> LEJANO =
      List.of(
          punto(9.700, -84.100),
          punto(9.700, -84.090),
          punto(9.710, -84.090),
          punto(9.710, -84.100));

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private CookieSesion cookieSesion;

  @Autowired private CuentaRepository cuentaRepository;

  @Autowired private FincaRepository fincaRepository;

  @Autowired private CosechaRepository cosechaRepository;

  private Cuenta andres;

  private Cuenta lucia;

  @BeforeEach
  void crearCuentas() {
    andres = cuenta("Andrés Solano", "andres@ejemplo.cr", Rol.CAFETALERO);
    lucia = cuenta("Lucía Mora", "lucia@ejemplo.cr", Rol.CAFETALERO);
  }

  @AfterEach
  void limpiar() {
    cosechaRepository.deleteAll();
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
  void cuentaEliminadaDa401ConMensaje() throws Exception {
    Cookie cookie = sesion(andres);
    andres.eliminar();
    cuentaRepository.saveAndFlush(andres);

    mockMvc
        .perform(
            post("/api/fincas")
                .cookie(cookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(finca("Sin cuenta", CUADRADO))))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.mensaje").value("Tenés que iniciar sesión."));
    assertThat(fincaRepository.count()).isZero();
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
        .andExpect(jsonPath("$.conflicto.duenos", hasSize(1)))
        .andExpect(jsonPath("$.conflicto.duenos[0].id").value(lucia.getId().toString()))
        .andExpect(jsonPath("$.conflicto.duenos[0].nombreCompleto").value("Lucía Mora"))
        .andExpect(jsonPath("$.conflicto.perimetro", hasSize(4)))
        .andExpect(jsonPath("$.conflicto.perimetro[0].lat").value(9.650))
        .andExpect(jsonPath("$.conflicto.perimetro[0].lng").value(-84.030))
        .andExpect(jsonPath("$.conflicto.perimetro[2].lat").value(9.660))
        .andExpect(jsonPath("$.conflicto.perimetro[2].lng").value(-84.020));
    assertThat(fincaRepository.count()).isEqualTo(1);
  }

  @Test
  void losDuenosDelConflictoSalenOrdenadosPorNombre() throws Exception {
    Cuenta zoe = cuenta("Zoe Quirós", "zoe@ejemplo.cr", Rol.CAFETALERO);
    Cuenta carlos = cuenta("Carlos Brenes", "carlos@ejemplo.cr", Rol.CAFETALERO);
    Finca tucan =
        new Finca(
            "Finca El Tucán",
            GeometriaUtil.poligono(coordenadas(CUADRADO)),
            GeometriaUtil.punto(new Coordenada(9.655, -84.025)),
            "Tarrazú",
            "San José");
    tucan.agregarDueno(zoe);
    tucan.agregarDueno(lucia);
    tucan.agregarDueno(carlos);
    fincaRepository.saveAndFlush(tucan);

    registrar(andres, finca("Finca La Catalina", SOBREPUESTO))
        .andExpect(status().isConflict())
        .andExpect(
            jsonPath("$.mensaje")
                .value(
                    "El terreno se sobrepone con la finca \"Finca El Tucán\" de Carlos Brenes,"
                        + " Lucía Mora y Zoe Quirós."))
        .andExpect(
            jsonPath(
                "$.conflicto.duenos[*].nombreCompleto",
                contains("Carlos Brenes", "Lucía Mora", "Zoe Quirós")))
        .andExpect(
            jsonPath(
                "$.conflicto.duenos[*].id",
                contains(
                    carlos.getId().toString(), lucia.getId().toString(), zoe.getId().toString())));
  }

  private static List<Coordenada> coordenadas(List<Map<String, Double>> puntos) {
    return puntos.stream().map(p -> new Coordenada(p.get("lat"), p.get("lng"))).toList();
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

  private Finca fincaDe(Cuenta... duenos) {
    Finca finca =
        new Finca(
            "Finca La Catalina",
            GeometriaUtil.poligono(coordenadas(CUADRADO)),
            GeometriaUtil.punto(new Coordenada(9.655, -84.025)),
            "Tarrazú",
            "San José");
    for (Cuenta dueno : duenos) {
      finca.agregarDueno(dueno);
    }
    return fincaRepository.saveAndFlush(finca);
  }

  private ResultActions agregarDueno(Cuenta cuenta, UUID fincaId, String correo) throws Exception {
    return mockMvc.perform(
        post("/api/fincas/{id}/duenos", fincaId)
            .cookie(sesion(cuenta))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of("correo", correo))));
  }

  private ResultActions quitarDueno(Cuenta cuenta, UUID fincaId, UUID duenoId) throws Exception {
    return mockMvc.perform(
        delete("/api/fincas/{id}/duenos/{cuentaId}", fincaId, duenoId).cookie(sesion(cuenta)));
  }

  @Test
  void agregarUnDuenoPorCorreo() throws Exception {
    Finca finca = fincaDe(andres);

    agregarDueno(andres, finca.getId(), "lucia@ejemplo.cr")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(finca.getId().toString()))
        .andExpect(jsonPath("$.duenos[*].nombreCompleto", contains("Andrés Solano", "Lucía Mora")))
        .andExpect(
            jsonPath(
                "$.duenos[*].id", contains(andres.getId().toString(), lucia.getId().toString())));

    for (Cuenta dueno : List.of(andres, lucia)) {
      assertThat(fincaRepository.findByDuenosIdAndEliminadaFalse(dueno.getId()))
          .extracting(Finca::getId)
          .containsExactly(finca.getId());
    }
  }

  @Test
  void elCorreoConMayusculasYEspaciosEncuentraLaCuenta() throws Exception {
    Finca finca = fincaDe(andres);

    agregarDueno(andres, finca.getId(), "  LUCIA@Ejemplo.CR ")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.duenos", hasSize(2)));
  }

  @Test
  void correoSinCuentaDeCafetaleroActivaDa404() throws Exception {
    Finca finca = fincaDe(andres);
    cuenta("Mario Brenes", "mario@ejemplo.cr", Rol.TOSTADOR);
    Cuenta eliminada = cuenta("Ana Quirós", "ana@ejemplo.cr", Rol.CAFETALERO);
    eliminada.eliminar();
    cuentaRepository.saveAndFlush(eliminada);

    for (String correo : List.of("nadie@ejemplo.cr", "mario@ejemplo.cr", "ana@ejemplo.cr")) {
      agregarDueno(andres, finca.getId(), correo)
          .andExpect(status().isNotFound())
          .andExpect(
              jsonPath("$.mensaje")
                  .value("No encontramos una cuenta de cafetalero con ese correo."));
    }
    assertThat(fincaRepository.findByDuenosIdAndEliminadaFalse(andres.getId())).hasSize(1);
  }

  @Test
  void agregarAQuienYaEsDuenoDa409() throws Exception {
    Finca finca = fincaDe(andres, lucia);

    agregarDueno(andres, finca.getId(), "lucia@ejemplo.cr")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.mensaje").value("Esa cuenta ya es dueña de esta finca."));
  }

  @Test
  void soloUnDuenoPuedeAgregarDuenos() throws Exception {
    Finca finca = fincaDe(andres);
    Cuenta tostador = cuenta("Mario Brenes", "mario@ejemplo.cr", Rol.TOSTADOR);

    agregarDueno(lucia, finca.getId(), "lucia@ejemplo.cr")
        .andExpect(status().isForbidden())
        .andExpect(
            jsonPath("$.mensaje").value("Solo un dueño de la finca puede administrar sus dueños."));
    agregarDueno(tostador, finca.getId(), "lucia@ejemplo.cr").andExpect(status().isForbidden());
    mockMvc
        .perform(
            post("/api/fincas/{id}/duenos", finca.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\": \"lucia@ejemplo.cr\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void fincaInexistenteOEliminadaDa404() throws Exception {
    Finca eliminada = fincaDe(andres);
    eliminada.eliminar();
    fincaRepository.saveAndFlush(eliminada);

    for (UUID id : List.of(UUID.randomUUID(), eliminada.getId())) {
      agregarDueno(andres, id, "lucia@ejemplo.cr")
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.mensaje").value("No encontramos esa finca."));
      quitarDueno(andres, id, andres.getId()).andExpect(status().isNotFound());
    }
  }

  @Test
  void correoMalFormadoDa400() throws Exception {
    Finca finca = fincaDe(andres);

    agregarDueno(andres, finca.getId(), "lucia@ejemplo")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errores.correo").value("Ingresá un correo válido."));
    // Vacío falla @NotBlank y @Email, y el orden de los dos mensajes no es fijo
    agregarDueno(andres, finca.getId(), " ")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errores.correo").exists());
  }

  @Test
  void quitarAUnDueno() throws Exception {
    Finca finca = fincaDe(andres, lucia);

    quitarDueno(andres, finca.getId(), lucia.getId())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.duenos[*].nombreCompleto", contains("Andrés Solano")));
    assertThat(fincaRepository.findByDuenosIdAndEliminadaFalse(lucia.getId())).isEmpty();
  }

  @Test
  void unDuenoPuedeQuitarseASiMismo() throws Exception {
    Finca finca = fincaDe(andres, lucia);

    quitarDueno(lucia, finca.getId(), lucia.getId())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.duenos[*].nombreCompleto", contains("Andrés Solano")));
  }

  @Test
  void quitarAlUltimoDuenoDa409() throws Exception {
    Finca finca = fincaDe(andres);

    quitarDueno(andres, finca.getId(), andres.getId())
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.mensaje").value("No se puede quitar al último dueño de la finca."));
    assertThat(fincaRepository.findByDuenosIdAndEliminadaFalse(andres.getId())).hasSize(1);
  }

  @Test
  void quitarAQuienNoEsDuenoDa404() throws Exception {
    Finca finca = fincaDe(andres);

    quitarDueno(andres, finca.getId(), lucia.getId())
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.mensaje").value("Esa cuenta no es dueña de esta finca."));
  }

  @Test
  void soloUnDuenoPuedeQuitarDuenos() throws Exception {
    Finca finca = fincaDe(andres, lucia);
    Cuenta carlos = cuenta("Carlos Brenes", "carlos@ejemplo.cr", Rol.CAFETALERO);

    quitarDueno(carlos, finca.getId(), lucia.getId())
        .andExpect(status().isForbidden())
        .andExpect(
            jsonPath("$.mensaje").value("Solo un dueño de la finca puede administrar sus dueños."));
    mockMvc
        .perform(delete("/api/fincas/{id}/duenos/{cuentaId}", finca.getId(), lucia.getId()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void misFincasDevuelveSoloLasDelUsuarioSinEliminadas() throws Exception {
    // Se registran fuera del orden por nombre para comprobar que no salen por fecha
    registrar(andres, finca("Finca La Catalina", CUADRADO)).andExpect(status().isCreated());
    registrar(andres, finca("Finca El Roble", COLINDANTE)).andExpect(status().isCreated());
    registrar(andres, finca("Finca Alta Vista", LEJANO)).andExpect(status().isCreated());
    Finca eliminada = fincaDe(andres);
    eliminada.eliminar();
    fincaRepository.saveAndFlush(eliminada);
    fincaDe(lucia);

    mockMvc
        .perform(get("/api/fincas").cookie(sesion(andres)))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath(
                "$[*].nombre", contains("Finca Alta Vista", "Finca El Roble", "Finca La Catalina")))
        .andExpect(jsonPath("$[0].areaHectareas", greaterThan(0.0)))
        .andExpect(jsonPath("$[0].duenos[*].nombreCompleto", contains("Andrés Solano")));
  }

  @Test
  void misFincasIncluyeLasCompartidas() throws Exception {
    Finca finca = fincaDe(andres);
    agregarDueno(andres, finca.getId(), "lucia@ejemplo.cr").andExpect(status().isOk());

    mockMvc
        .perform(get("/api/fincas").cookie(sesion(lucia)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].id", contains(finca.getId().toString())));
  }

  @Test
  void misFincasSinFincasDevuelveListaVacia() throws Exception {
    mockMvc
        .perform(get("/api/fincas").cookie(sesion(andres)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(0)));
  }

  @Test
  void misFincasPideSesionYRolCafetalero() throws Exception {
    Cuenta tostador = cuenta("Mario Brenes", "mario@ejemplo.cr", Rol.TOSTADOR);

    mockMvc.perform(get("/api/fincas")).andExpect(status().isUnauthorized());
    mockMvc.perform(get("/api/fincas").cookie(sesion(tostador))).andExpect(status().isForbidden());
  }

  @Test
  void obtenerUnaFincaPropia() throws Exception {
    Finca finca = fincaDe(andres);

    mockMvc
        .perform(get("/api/fincas/{id}", finca.getId()).cookie(sesion(andres)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(finca.getId().toString()))
        .andExpect(jsonPath("$.nombre").value("Finca La Catalina"))
        .andExpect(jsonPath("$.perimetro", hasSize(4)))
        .andExpect(jsonPath("$.duenos[*].nombreCompleto", contains("Andrés Solano")));
  }

  @Test
  void obtenerUnaFincaAjenaDa403() throws Exception {
    Finca finca = fincaDe(andres);

    mockMvc
        .perform(get("/api/fincas/{id}", finca.getId()).cookie(sesion(lucia)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.mensaje").value("Solo un dueño de la finca puede verla."));
  }

  @Test
  void obtenerUnaFincaInexistenteOEliminadaDa404() throws Exception {
    Finca eliminada = fincaDe(andres);
    eliminada.eliminar();
    fincaRepository.saveAndFlush(eliminada);

    for (UUID id : List.of(UUID.randomUUID(), eliminada.getId())) {
      mockMvc
          .perform(get("/api/fincas/{id}", id).cookie(sesion(andres)))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.mensaje").value("No encontramos esa finca."));
    }
  }

  // Sin kgDisponibles queda en beneficiado; con kgDisponibles queda DISPONIBLE
  private Cosecha cosecha(Finca finca, String variedad, LocalDate inicio, String kgDisponibles) {
    Cosecha cosecha =
        new Cosecha(
            finca,
            variedad,
            1450,
            inicio,
            inicio.plusMonths(1),
            new BigDecimal("1200.00"),
            new BigDecimal("9.75"),
            List.of(
                new ProcesoReparto(Proceso.ANAEROBICO, 40),
                new ProcesoReparto(Proceso.RED_HONEY, 60)));
    if (kgDisponibles != null) {
      cosecha.registrarPesoTrasSeleccion(new BigDecimal(kgDisponibles));
    }
    return cosechaRepository.saveAndFlush(cosecha);
  }

  @Test
  void misFincasTraeElResumenDeCosechasDisponibles() throws Exception {
    Finca conCosechas = fincaDe(andres);
    cosecha(conCosechas, "Caturra", LocalDate.of(2026, 1, 5), "300.50");
    cosecha(conCosechas, "Geisha", LocalDate.of(2026, 2, 5), "100.00");
    Cosecha vendida = cosecha(conCosechas, "Catuaí", LocalDate.of(2026, 3, 5), "50.00");
    vendida.descontarDisponible(new BigDecimal("50.00"));
    cosechaRepository.saveAndFlush(vendida);
    cosecha(conCosechas, "Villa Sarchí", LocalDate.of(2026, 4, 5), null);
    registrar(andres, finca("Finca El Roble", LEJANO)).andExpect(status().isCreated());

    mockMvc
        .perform(get("/api/fincas").cookie(sesion(andres)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].nombre", contains("Finca El Roble", "Finca La Catalina")))
        .andExpect(jsonPath("$[0].cosechasActivas").value(0))
        .andExpect(jsonPath("$[0].kgDisponibles").value(0))
        .andExpect(jsonPath("$[0].areaHectareas", closeTo(121.0, 1.0)))
        .andExpect(jsonPath("$[1].cosechasActivas").value(2))
        .andExpect(jsonPath("$[1].kgDisponibles").value(400.5))
        .andExpect(jsonPath("$[1].areaHectareas", closeTo(121.0, 1.0)))
        .andExpect(jsonPath("$[1].cosechas").doesNotExist());
  }

  @Test
  void obtenerTraeLasCosechasConSuProcesoPrincipal() throws Exception {
    Finca finca = fincaDe(andres);
    Cosecha disponible = cosecha(finca, "Caturra", LocalDate.of(2026, 1, 5), "300.00");
    disponible.setCodigoLote("crh-2026-0001");
    cosechaRepository.saveAndFlush(disponible);
    cosecha(finca, "Geisha", LocalDate.of(2026, 3, 5), null);

    mockMvc
        .perform(get("/api/fincas/{id}", finca.getId()).cookie(sesion(andres)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.cosechasActivas").doesNotExist())
        .andExpect(jsonPath("$.cosechas[*].variedad", contains("Geisha", "Caturra")))
        .andExpect(jsonPath("$.cosechas[0].procesoPrincipal").value("Red honey"))
        .andExpect(jsonPath("$.cosechas[0].estado").value("EN_BENEFICIADO"))
        .andExpect(jsonPath("$.cosechas[0].kgDisponibles").value(nullValue()))
        .andExpect(jsonPath("$.cosechas[0].codigoLote").value(nullValue()))
        .andExpect(jsonPath("$.cosechas[1].id").value(disponible.getId().toString()))
        .andExpect(jsonPath("$.cosechas[1].codigoLote").value("CRH-2026-0001"))
        .andExpect(jsonPath("$.cosechas[1].estado").value("DISPONIBLE"))
        .andExpect(jsonPath("$.cosechas[1].kgDisponibles").value(300.0))
        .andExpect(jsonPath("$.cosechas[1].precioKg").value(9.75));
  }

  @Test
  void obtenerSinCosechasDevuelveListaVacia() throws Exception {
    Finca finca = fincaDe(andres);

    mockMvc
        .perform(get("/api/fincas/{id}", finca.getId()).cookie(sesion(andres)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.cosechas", hasSize(0)));
  }

  @Test
  void cosechasDeUnaFincaPropia() throws Exception {
    Finca finca = fincaDe(andres);
    Cosecha cosecha = cosecha(finca, "Caturra", LocalDate.of(2026, 1, 5), "300.00");
    Finca ajena = fincaDe(lucia);
    cosecha(ajena, "Geisha", LocalDate.of(2026, 1, 5), null);

    mockMvc
        .perform(get("/api/fincas/{id}/cosechas", finca.getId()).cookie(sesion(andres)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].id").value(cosecha.getId().toString()))
        .andExpect(jsonPath("$[0].fincaId").value(finca.getId().toString()))
        .andExpect(jsonPath("$[0].variedad").value("Caturra"))
        .andExpect(jsonPath("$[0].alturaMsnm").value(1450))
        .andExpect(jsonPath("$[0].fechaInicio").value("2026-01-05"))
        .andExpect(jsonPath("$[0].fechaFin").value("2026-02-05"))
        .andExpect(
            jsonPath("$[0].procesos[*].proceso", containsInAnyOrder("Anaeróbico", "Red honey")))
        .andExpect(jsonPath("$[0].procesos[*].porcentaje", containsInAnyOrder(40, 60)))
        .andExpect(jsonPath("$[0].cantidadCosechadaKg").value(1200.0))
        .andExpect(jsonPath("$[0].pesoTrasSeleccionKg").value(300.0))
        .andExpect(jsonPath("$[0].precioKg").value(9.75))
        .andExpect(jsonPath("$[0].kgDisponibles").value(300.0))
        .andExpect(jsonPath("$[0].estado").value("DISPONIBLE"))
        .andExpect(jsonPath("$[0].pesoTrasBeneficiadoKg").value(nullValue()))
        .andExpect(jsonPath("$[0].caracolilloKg").value(nullValue()))
        .andExpect(jsonPath("$[0].notasSabor").value(nullValue()))
        .andExpect(jsonPath("$[0].puntajeSca").value(nullValue()))
        .andExpect(jsonPath("$[0].codigoLote").value(nullValue()))
        .andExpect(jsonPath("$[0].fincaNombre").doesNotExist());
  }

  @Test
  void cosechasDeUnaFincaSinCosechasDevuelveListaVacia() throws Exception {
    Finca finca = fincaDe(andres);

    mockMvc
        .perform(get("/api/fincas/{id}/cosechas", finca.getId()).cookie(sesion(andres)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(0)));
  }

  @Test
  void cosechasDeUnaFincaAjenaDa403() throws Exception {
    Finca finca = fincaDe(lucia);

    mockMvc
        .perform(get("/api/fincas/{id}/cosechas", finca.getId()).cookie(sesion(andres)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.mensaje").value("Solo un dueño de la finca puede verla."));
  }

  @Test
  void cosechasDeUnaFincaInexistenteOEliminadaDa404() throws Exception {
    Finca eliminada = fincaDe(andres);
    cosecha(eliminada, "Caturra", LocalDate.of(2026, 1, 5), null);
    eliminada.eliminar();
    fincaRepository.saveAndFlush(eliminada);

    for (UUID id : List.of(UUID.randomUUID(), eliminada.getId())) {
      mockMvc
          .perform(get("/api/fincas/{id}/cosechas", id).cookie(sesion(andres)))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.mensaje").value("No encontramos esa finca."));
    }
  }

  @Test
  void consultarUnaFincaPideSesionYRolCafetalero() throws Exception {
    Finca finca = fincaDe(andres);
    Cuenta tostador = cuenta("Mario Brenes", "mario@ejemplo.cr", Rol.TOSTADOR);

    for (String ruta :
        List.of("/api/fincas/" + finca.getId(), "/api/fincas/" + finca.getId() + "/cosechas")) {
      mockMvc.perform(get(ruta)).andExpect(status().isUnauthorized());
      mockMvc.perform(get(ruta).cookie(sesion(tostador))).andExpect(status().isForbidden());
    }
  }
}
