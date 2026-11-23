package com.tuanistack.cranhua.tueste;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tuanistack.cranhua.BasePostgis;
import com.tuanistack.cranhua.auth.CookieSesion;
import com.tuanistack.cranhua.cafeteria.Cafeteria;
import com.tuanistack.cranhua.cafeteria.CafeteriaRepository;
import com.tuanistack.cranhua.cuenta.Cuenta;
import com.tuanistack.cranhua.cuenta.CuentaRepository;
import com.tuanistack.cranhua.cuenta.Rol;
import com.tuanistack.cranhua.finca.Cosecha;
import com.tuanistack.cranhua.finca.CosechaRepository;
import com.tuanistack.cranhua.finca.FincaRepository;
import com.tuanistack.cranhua.finca.GeometriaUtil;
import com.tuanistack.cranhua.finca.dto.Coordenada;
import com.tuanistack.cranhua.lote.Lote;
import com.tuanistack.cranhua.lote.LoteRepository;
import com.tuanistack.cranhua.pedido.Pedido;
import com.tuanistack.cranhua.pedido.PedidoRepository;
import com.tuanistack.cranhua.solicitud.Solicitud;
import com.tuanistack.cranhua.solicitud.SolicitudRepository;
import com.tuanistack.cranhua.solicitud.SolicitudRepositoryTest;
import jakarta.servlet.http.Cookie;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(BasePostgis.class)
class TostadorExploracionControllerTest {

  private static final AtomicInteger CODIGO = new AtomicInteger(100);

  @Autowired private MockMvc mockMvc;

  @Autowired private CookieSesion cookieSesion;

  @Autowired private CuentaRepository cuentaRepository;

  @Autowired private NegocioTuesteRepository negocioRepository;

  @Autowired private FincaRepository fincaRepository;

  @Autowired private CosechaRepository cosechaRepository;

  @Autowired private SolicitudRepository solicitudRepository;

  @Autowired private LoteRepository loteRepository;

  @Autowired private TuesteRepository tuesteRepository;

  @Autowired private PedidoRepository pedidoRepository;

  @Autowired private CafeteriaRepository cafeteriaRepository;

  private Cuenta sofia;

  private Cuenta mario;

  private Cosecha cosecha;

  private Cafeteria cafeteria;

  @BeforeEach
  void crearDatos() {
    sofia = cuenta("Sofía Jiménez", "sofia@ejemplo.cr", Rol.CAFETERIA);
    mario = cuenta("Mario Brenes", "mario@ejemplo.cr", Rol.TOSTADOR);
    cosecha =
        cosechaRepository.saveAndFlush(
            SolicitudRepositoryTest.nuevaCosecha(
                fincaRepository.saveAndFlush(SolicitudRepositoryTest.finca())));
    cafeteria =
        cafeteriaRepository.saveAndFlush(
            new Cafeteria(
                sofia,
                "Café Brumas",
                "Sofía Jiménez",
                "+506 8899-4410",
                "hola@cafebrumas.cr",
                GeometriaUtil.punto(new Coordenada(9.9205, -84.137)),
                "Escazú",
                "San José",
                Set.of(NivelTueste.MEDIO)));
  }

  @AfterEach
  void limpiar() {
    pedidoRepository.deleteAll();
    tuesteRepository.deleteAll();
    loteRepository.deleteAll();
    solicitudRepository.deleteAll();
    cosechaRepository.deleteAll();
    fincaRepository.deleteAll();
    cafeteriaRepository.deleteAll();
    negocioRepository.deleteAll();
    cuentaRepository.deleteAll();
  }

  private Cuenta cuenta(String nombre, String correo, Rol... roles) {
    return cuentaRepository.saveAndFlush(new Cuenta(nombre, correo, "hash", Set.of(roles)));
  }

  private Cookie sesion(Cuenta cuenta) {
    return new Cookie(CookieSesion.NOMBRE, cookieSesion.crear(cuenta).getValue());
  }

  private NegocioTueste negocio(Cuenta tostador, String nombre, Set<NivelTueste> estilos) {
    NegocioTueste negocio =
        new NegocioTueste(
            tostador,
            nombre,
            tostador.getNombreCompleto(),
            "+506 8888-1234",
            tostador.getCorreo(),
            "San José",
            "San José",
            estilos);
    negocio.setDescripcion("Perfiles para cafeterías.");
    return negocioRepository.saveAndFlush(negocio);
  }

  private Tueste tuesteActivo(Cuenta tostador, NivelTueste nivel, String pesoTostadoKg) {
    Solicitud solicitud = new Solicitud(cosecha, tostador, new BigDecimal("100.00"));
    solicitud.aprobar();
    solicitudRepository.saveAndFlush(solicitud);
    Lote lote = new Lote(solicitud, Lote.formatearCodigo(2026, CODIGO.getAndIncrement()));
    lote.activar();
    lote = loteRepository.saveAndFlush(lote);
    return tuesteRepository.saveAndFlush(
        new Tueste(
            lote,
            nivel,
            new BigDecimal("50.00"),
            new BigDecimal(pesoTostadoKg),
            LocalDate.of(2026, 3, 1)));
  }

  @Test
  void listaNegociosActivosConCamposDeExploracion() throws Exception {
    NegocioTueste volcan =
        negocio(mario, "Tostaduría Volcán", Set.of(NivelTueste.CLARO, NivelTueste.MEDIO));
    Tueste tueste = tuesteActivo(mario, NivelTueste.MEDIO, "42.00");
    pedidoRepository.saveAndFlush(new Pedido(cafeteria, tueste, new BigDecimal("10.00")));

    Cuenta laura = cuenta("Laura Mora", "laura@ejemplo.cr", Rol.TOSTADOR);
    NegocioTueste sinStock = negocio(laura, "Tueste Seco", Set.of(NivelTueste.OSCURO));

    Cuenta diego = cuenta("Diego Mora", "diego@ejemplo.cr", Rol.TOSTADOR);
    NegocioTueste eliminado = negocio(diego, "Baja", Set.of(NivelTueste.CLARO));
    eliminado.eliminar();
    negocioRepository.saveAndFlush(eliminado);

    mockMvc
        .perform(get("/api/tostadores").cookie(sesion(sofia)))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$[*].id")
                .value(containsInAnyOrder(volcan.getId().toString(), sinStock.getId().toString())))
        .andExpect(
            jsonPath("$[?(@.id == '%s')].nombre", volcan.getId())
                .value(contains("Tostaduría Volcán")))
        .andExpect(
            jsonPath("$[?(@.id == '%s')].canton", volcan.getId()).value(contains("San José")))
        .andExpect(
            jsonPath("$[?(@.id == '%s')].provincia", volcan.getId()).value(contains("San José")))
        .andExpect(
            jsonPath("$[?(@.id == '%s')].estilos[*]", volcan.getId())
                .value(containsInAnyOrder("CLARO", "MEDIO")))
        .andExpect(
            jsonPath("$[?(@.id == '%s')].kgDisponibles", volcan.getId()).value(contains(32.0)))
        .andExpect(
            jsonPath("$[?(@.id == '%s')].kgDisponibles", sinStock.getId()).value(contains(0)));
  }

  @Test
  void filtraPorEstiloYDisponible() throws Exception {
    NegocioTueste claro =
        negocio(mario, "Claro Roasters", Set.of(NivelTueste.CLARO, NivelTueste.MEDIO));
    tuesteActivo(mario, NivelTueste.CLARO, "20.00");

    Cuenta laura = cuenta("Laura Mora", "laura@ejemplo.cr", Rol.TOSTADOR);
    NegocioTueste oscuro = negocio(laura, "Oscuro House", Set.of(NivelTueste.OSCURO));
    tuesteActivo(laura, NivelTueste.OSCURO, "15.00");

    Cuenta diego = cuenta("Diego Mora", "diego@ejemplo.cr", Rol.TOSTADOR);
    NegocioTueste sinStockClaro = negocio(diego, "Sin Stock Claro", Set.of(NivelTueste.CLARO));

    mockMvc
        .perform(get("/api/tostadores").param("estilo", "CLARO").cookie(sesion(sofia)))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$[*].id")
                .value(
                    containsInAnyOrder(
                        claro.getId().toString(), sinStockClaro.getId().toString())));

    mockMvc
        .perform(get("/api/tostadores").param("disponible", "true").cookie(sesion(sofia)))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$[*].id")
                .value(containsInAnyOrder(claro.getId().toString(), oscuro.getId().toString())));

    mockMvc
        .perform(
            get("/api/tostadores")
                .param("estilo", "CLARO")
                .param("disponible", "true")
                .cookie(sesion(sofia)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].id").value(contains(claro.getId().toString())));

    mockMvc
        .perform(get("/api/tostadores").param("estilo", "FUERTE").cookie(sesion(sofia)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.mensaje").value("Ese estilo de tueste no existe."));
  }

  @Test
  void elDetalleTraeContactoYTuestesConKgDisponibles() throws Exception {
    NegocioTueste negocio = negocio(mario, "Tostaduría Volcán", Set.of(NivelTueste.MEDIO));
    negocio.setPortadaUrl("https://res.cloudinary.com/cranhua/portada.jpg");
    negocioRepository.saveAndFlush(negocio);

    Tueste disponible = tuesteActivo(mario, NivelTueste.MEDIO, "42.00");
    pedidoRepository.saveAndFlush(new Pedido(cafeteria, disponible, new BigDecimal("12.00")));

    Tueste agotado = tuesteActivo(mario, NivelTueste.CLARO, "10.00");
    pedidoRepository.saveAndFlush(new Pedido(cafeteria, agotado, new BigDecimal("10.00")));

    mockMvc
        .perform(get("/api/tostadores/{id}", negocio.getId()).cookie(sesion(sofia)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(negocio.getId().toString()))
        .andExpect(jsonPath("$.nombre").value("Tostaduría Volcán"))
        .andExpect(jsonPath("$.nombreContacto").value("Mario Brenes"))
        .andExpect(jsonPath("$.telefono").value("+506 8888-1234"))
        .andExpect(jsonPath("$.correo").value("mario@ejemplo.cr"))
        .andExpect(jsonPath("$.descripcion").value("Perfiles para cafeterías."))
        .andExpect(jsonPath("$.canton").value("San José"))
        .andExpect(jsonPath("$.provincia").value("San José"))
        .andExpect(jsonPath("$.estilos", contains("MEDIO")))
        .andExpect(jsonPath("$.portadaUrl").value("https://res.cloudinary.com/cranhua/portada.jpg"))
        .andExpect(jsonPath("$.tuestes", hasSize(1)))
        .andExpect(jsonPath("$.tuestes[0].id").value(disponible.getId().toString()))
        .andExpect(jsonPath("$.tuestes[0].fincaOrigen").value("Finca El Tucán"))
        .andExpect(jsonPath("$.tuestes[0].variedad").value("Caturra"))
        .andExpect(jsonPath("$.tuestes[0].nivel").value("MEDIO"))
        .andExpect(jsonPath("$.tuestes[0].kgDisponibles").value(30.0));
  }

  @Test
  void elDetalleDeUnNegocioEliminadoDa404() throws Exception {
    NegocioTueste negocio = negocio(mario, "Tostaduría Volcán", Set.of(NivelTueste.MEDIO));
    negocio.eliminar();
    negocioRepository.saveAndFlush(negocio);

    mockMvc
        .perform(get("/api/tostadores/{id}", negocio.getId()).cookie(sesion(sofia)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.mensaje").value("No encontramos ese negocio."));
    mockMvc
        .perform(get("/api/tostadores/{id}", UUID.randomUUID()).cookie(sesion(sofia)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.mensaje").value("No encontramos ese negocio."));
  }

  @Test
  void cafeteriaYDuenoVenElDetalleOtrosRolesNo() throws Exception {
    NegocioTueste negocio = negocio(mario, "Tostaduría Volcán", Set.of(NivelTueste.MEDIO));
    Cuenta cafetalero = cuenta("Andrés Solano", "andres@ejemplo.cr", Rol.CAFETALERO);
    Cuenta otroTostador = cuenta("Laura Mora", "laura@ejemplo.cr", Rol.TOSTADOR);

    mockMvc
        .perform(get("/api/tostadores/{id}", negocio.getId()).cookie(sesion(sofia)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nombre").value("Tostaduría Volcán"));
    mockMvc
        .perform(get("/api/tostadores/{id}", negocio.getId()).cookie(sesion(mario)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.tuestes").isArray());
    mockMvc
        .perform(get("/api/tostadores/{id}", negocio.getId()).cookie(sesion(cafetalero)))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(get("/api/tostadores/{id}", negocio.getId()).cookie(sesion(otroTostador)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.mensaje").value("No tienes permiso para ver este negocio."));
  }

  @Test
  void pideSesionYRolCafeteriaParaElListado() throws Exception {
    mockMvc.perform(get("/api/tostadores")).andExpect(status().isUnauthorized());
    mockMvc.perform(get("/api/tostadores").cookie(sesion(mario))).andExpect(status().isForbidden());
    mockMvc
        .perform(
            get("/api/tostadores")
                .cookie(sesion(cuenta("Andrés Solano", "andres@ejemplo.cr", Rol.CAFETALERO))))
        .andExpect(status().isForbidden());
  }
}
