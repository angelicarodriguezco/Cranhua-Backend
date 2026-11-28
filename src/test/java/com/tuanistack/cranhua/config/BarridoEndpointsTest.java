package com.tuanistack.cranhua.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tuanistack.cranhua.BasePostgis;
import com.tuanistack.cranhua.auth.CookieSesion;
import com.tuanistack.cranhua.cuenta.Cuenta;
import com.tuanistack.cranhua.cuenta.CuentaRepository;
import com.tuanistack.cranhua.cuenta.Rol;
import jakarta.servlet.http.Cookie;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.pattern.PathPattern;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(BasePostgis.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BarridoEndpointsTest {

  private static final Pattern VARIABLE = Pattern.compile("\\{([^}]+)}");
  private static final Pattern ROL = Pattern.compile("'([A-Z]+)'");

  private static final String UUID_CUALQUIERA = "\"" + UUID.randomUUID() + "\"";

  private static final String COSECHA =
      """
      "variedad": "Caturra", "alturaMsnm": 1500,
      "fechaInicio": "2026-01-05", "fechaFin": "2026-02-28",
      "procesos": [{"proceso": "Lavado", "porcentaje": 100}],
      "cantidadCosechadaKg": 500, "precioKg": 9.75
      """;

  private static final String COORDENADA = "{\"lat\": 9.65, \"lng\": -84.03}";

  // @Valid corre antes que @PreAuthorize: con {} estos endpoints darían 400 en vez de 403
  private static final Map<String, String> CUERPOS_VALIDOS =
      Map.ofEntries(
          Map.entry(
              "POST /api/cosechas", "{\"fincaId\": " + UUID_CUALQUIERA + ", " + COSECHA + "}"),
          Map.entry("PATCH /api/cosechas/{id}", "{" + COSECHA + "}"),
          Map.entry("PATCH /api/cosechas/{id}/estado", "{\"estado\": \"PUBLICADA\"}"),
          Map.entry(
              "POST /api/fincas",
              """
              {"nombre": "Finca Barrido", "perimetro": [%1$s, %1$s, %1$s],
               "ubicacion": %1$s, "canton": "Tarrazú", "provincia": "San José"}
              """
                  .formatted(COORDENADA)),
          Map.entry("POST /api/fincas/{id}/duenos", "{\"correo\": \"otro@barrido.cr\"}"),
          Map.entry(
              "POST /api/solicitudes",
              "{\"cosechaId\": " + UUID_CUALQUIERA + ", \"cantidadKg\": 10}"),
          Map.entry("PATCH /api/solicitudes/{id}", "{\"estado\": \"APROBADA\"}"),
          Map.entry(
              "POST /api/pedidos", "{\"tuesteId\": " + UUID_CUALQUIERA + ", \"cantidadKg\": 5}"),
          Map.entry("PATCH /api/pedidos/{id}", "{\"estado\": \"ENTREGADO\"}"),
          Map.entry(
              "POST /api/cafeterias",
              """
              {"nombre": "Cafetería Barrido", "nombreContacto": "Ana", "telefono": "8888-8888",
               "correo": "cafe@barrido.cr", "ubicacion": %s, "canton": "San José",
               "provincia": "San José", "estilos": ["MEDIO"]}
              """
                  .formatted(COORDENADA)),
          Map.entry(
              "POST /api/cafeterias/{id}/menu",
              "{\"nombre\": \"Latte\", \"categoria\": \"CAFE\", \"precio\": 2500}"),
          Map.entry(
              "POST /api/tostadores",
              """
              {"nombre": "Tostador Barrido", "nombreContacto": "Luis", "telefono": "8888-8888",
               "correo": "tueste@barrido.cr", "canton": "Tarrazú", "provincia": "San José",
               "estilos": ["MEDIO"]}
              """),
          Map.entry(
              "POST /api/tuestes",
              """
              {"loteId": %s, "nivel": "MEDIO", "pesoVerdeKg": 10, "pesoTostadoKg": 8.5,
               "fechaTueste": "2026-03-01"}
              """
                  .formatted(UUID_CUALQUIERA)));

  record Endpoint(String metodo, String ruta, String url, HandlerMethod handler) {

    @Override
    public String toString() {
      return metodo + " " + ruta;
    }
  }

  @Autowired private MockMvc mockMvc;

  @Autowired
  @Qualifier("requestMappingHandlerMapping")
  private RequestMappingHandlerMapping handlerMapping;

  @Autowired private CuentaRepository cuentaRepository;

  @Autowired private CookieSesion cookieSesion;

  private final Map<Rol, Cuenta> cuentasPorRol = new EnumMap<>(Rol.class);

  private Cuenta cuentaSinRoles;

  @BeforeAll
  void crearCuentas() {
    for (Rol rol : Rol.values()) {
      cuentasPorRol.put(rol, cuenta(rol.name().toLowerCase() + "@barrido.cr", Set.of(rol)));
    }
    cuentaSinRoles = cuenta("sin-roles@barrido.cr", Set.of());
  }

  @AfterAll
  void borrarCuentas() {
    cuentaRepository.deleteAll(cuentasPorRol.values());
    cuentaRepository.delete(cuentaSinRoles);
  }

  private Cuenta cuenta(String correo, Set<Rol> roles) {
    return cuentaRepository.saveAndFlush(new Cuenta("Cuenta Barrido", correo, "hash", roles));
  }

  Stream<Endpoint> endpointsProtegidos() {
    List<Endpoint> endpoints = new ArrayList<>();
    handlerMapping
        .getHandlerMethods()
        .forEach((info, handler) -> endpoints.addAll(endpoints(info, handler)));
    assertThat(endpoints).isNotEmpty();
    return endpoints.stream()
        .filter(endpoint -> !esPublico(endpoint))
        .sorted(Comparator.comparing(Endpoint::toString));
  }

  private static List<Endpoint> endpoints(RequestMappingInfo info, HandlerMethod handler) {
    if (!esDeLaAplicacion(handler.getBeanType())) {
      return List.of();
    }
    assertThat(info.getMethodsCondition().getMethods())
        .as("%s no declara método HTTP y el barrido no lo cubriría", handler)
        .isNotEmpty();
    List<Endpoint> endpoints = new ArrayList<>();
    for (PathPattern patron : info.getPathPatternsCondition().getPatterns()) {
      for (var metodo : info.getMethodsCondition().getMethods()) {
        String ruta = patron.getPatternString();
        endpoints.add(new Endpoint(metodo.name(), ruta, url(ruta, handler), handler));
      }
    }
    return endpoints;
  }

  // Deja afuera los controladores de Spring Boot y los que solo existen en los tests
  private static boolean esDeLaAplicacion(Class<?> tipo) {
    return tipo.getPackageName().startsWith("com.tuanistack.cranhua")
        && !tipo.getProtectionDomain()
            .getCodeSource()
            .getLocation()
            .getPath()
            .contains("test-classes");
  }

  private static boolean esPublico(Endpoint endpoint) {
    MockHttpServletRequest request = new MockHttpServletRequest(endpoint.metodo(), endpoint.url());
    return SecurityConfig.RUTAS_PUBLICAS.matches(request);
  }

  private static String url(String ruta, HandlerMethod handler) {
    Matcher variables = VARIABLE.matcher(ruta);
    StringBuilder url = new StringBuilder();
    while (variables.find()) {
      variables.appendReplacement(url, valorDeVariable(variables.group(1), handler));
    }
    variables.appendTail(url);
    return url.toString();
  }

  private static String valorDeVariable(String nombre, HandlerMethod handler) {
    return Arrays.stream(handler.getMethodParameters())
        .filter(parametro -> nombre.equals(nombreDeVariable(parametro)))
        .findFirst()
        .map(parametro -> parametro.getParameterType() == UUID.class ? UUID.randomUUID() : "1")
        .orElseThrow(() -> new IllegalStateException("Sin @PathVariable para {" + nombre + "}"))
        .toString();
  }

  private static String nombreDeVariable(MethodParameter parametro) {
    PathVariable variable = parametro.getParameterAnnotation(PathVariable.class);
    if (variable == null) {
      return null;
    }
    parametro.initParameterNameDiscovery(new DefaultParameterNameDiscoverer());
    return variable.value().isEmpty() ? parametro.getParameterName() : variable.value();
  }

  private static PreAuthorize preAuthorize(HandlerMethod handler) {
    PreAuthorize enMetodo =
        AnnotatedElementUtils.findMergedAnnotation(handler.getMethod(), PreAuthorize.class);
    return enMetodo != null
        ? enMetodo
        : AnnotatedElementUtils.findMergedAnnotation(handler.getBeanType(), PreAuthorize.class);
  }

  private MockHttpServletRequestBuilder peticion(Endpoint endpoint) {
    HttpMethod metodo = HttpMethod.valueOf(endpoint.metodo());
    if (tieneParametro(endpoint.handler(), RequestPart.class)) {
      return multipart(metodo, endpoint.url())
          .file(new MockMultipartFile("archivo", "foto.png", "image/png", new byte[] {1}));
    }
    MockHttpServletRequestBuilder peticion = request(metodo, endpoint.url());
    if (tieneParametro(endpoint.handler(), RequestBody.class)) {
      peticion
          .contentType(MediaType.APPLICATION_JSON)
          .content(CUERPOS_VALIDOS.getOrDefault(endpoint.toString(), "{}"));
    }
    return peticion;
  }

  private static boolean tieneParametro(
      HandlerMethod handler, Class<? extends Annotation> anotacion) {
    return Arrays.stream(handler.getMethodParameters())
        .anyMatch(parametro -> parametro.hasParameterAnnotation(anotacion));
  }

  private Cookie sesionSinPermiso(PreAuthorize regla) {
    Matcher roles = ROL.matcher(regla.value());
    List<Rol> permitidos = new ArrayList<>();
    while (roles.find()) {
      permitidos.add(Rol.valueOf(roles.group(1)));
    }
    Cuenta cuenta =
        Arrays.stream(Rol.values())
            .filter(rol -> !permitidos.contains(rol))
            .findFirst()
            .map(cuentasPorRol::get)
            .orElse(cuentaSinRoles);
    return new Cookie(CookieSesion.NOMBRE, cookieSesion.crear(cuenta).getValue());
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("endpointsProtegidos")
  void sinSesionDa401(Endpoint endpoint) throws Exception {
    mockMvc.perform(peticion(endpoint)).andExpect(status().isUnauthorized());
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("endpointsProtegidos")
  void tienePreAuthorize(Endpoint endpoint) {
    assertThat(preAuthorize(endpoint.handler()))
        .as("Falta @PreAuthorize en %s", endpoint.handler().getMethod())
        .isNotNull();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("endpointsProtegidos")
  void conUnRolQueNoEstaEnPreAuthorizeDa403(Endpoint endpoint) throws Exception {
    Cookie sesion = sesionSinPermiso(preAuthorize(endpoint.handler()));
    int estado =
        mockMvc.perform(peticion(endpoint).cookie(sesion)).andReturn().getResponse().getStatus();
    assertThat(estado).as("%s", endpoint).isEqualTo(403);
  }
}
