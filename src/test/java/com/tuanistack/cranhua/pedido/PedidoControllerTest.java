package com.tuanistack.cranhua.pedido;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.tuanistack.cranhua.solicitud.Solicitud;
import com.tuanistack.cranhua.solicitud.SolicitudRepository;
import com.tuanistack.cranhua.solicitud.SolicitudRepositoryTest;
import com.tuanistack.cranhua.tueste.NegocioTueste;
import com.tuanistack.cranhua.tueste.NegocioTuesteRepository;
import com.tuanistack.cranhua.tueste.NivelTueste;
import com.tuanistack.cranhua.tueste.Tueste;
import com.tuanistack.cranhua.tueste.TuesteRepository;
import jakarta.servlet.http.Cookie;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.HashMap;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(BasePostgis.class)
class PedidoControllerTest {

  private static final ZoneId COSTA_RICA = ZoneId.of("America/Costa_Rica");

  private static final Object OMITIR = new Object();

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private CookieSesion cookieSesion;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private CuentaRepository cuentaRepository;

  @Autowired private NegocioTuesteRepository negocioTuesteRepository;

  @Autowired private FincaRepository fincaRepository;

  @Autowired private CosechaRepository cosechaRepository;

  @Autowired private SolicitudRepository solicitudRepository;

  @Autowired private LoteRepository loteRepository;

  @Autowired private TuesteRepository tuesteRepository;

  @Autowired private CafeteriaRepository cafeteriaRepository;

  @Autowired private PedidoRepository pedidoRepository;

  private Cuenta sofia;

  private Cuenta diego;

  private Cuenta mario;

  private Cafeteria brumas;

  private Cafeteria deDiego;

  private Tueste tueste;

  private int numeroLote;

  @BeforeEach
  void crearDatos() {
    sofia = cuenta("Sofía Jiménez", "sofia@ejemplo.cr", Rol.CAFETERIA);
    diego = cuenta("Diego Mora", "diego@ejemplo.cr", Rol.CAFETERIA);
    mario = cuenta("Mario Brenes", "mario@ejemplo.cr", Rol.TOSTADOR);
    negocioTuesteRepository.saveAndFlush(
        new NegocioTueste(
            mario,
            "Tostaduría Volcán",
            "Mario Brenes",
            "+506 8888-1234",
            "mario@ejemplo.cr",
            "San José",
            "San José",
            Set.of(NivelTueste.MEDIO)));
    brumas = cafeteria(sofia, "Café Brumas");
    deDiego = cafeteria(diego, "Café de Diego");
    Cosecha cosecha =
        cosechaRepository.saveAndFlush(
            SolicitudRepositoryTest.nuevaCosecha(
                fincaRepository.saveAndFlush(SolicitudRepositoryTest.finca())));
    tueste = tueste(cosecha, mario, "42.00");
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
    negocioTuesteRepository.deleteAll();
    cuentaRepository.deleteAll();
  }

  private Cuenta cuenta(String nombre, String correo, Rol rol) {
    return cuentaRepository.saveAndFlush(new Cuenta(nombre, correo, "hash", Set.of(rol)));
  }

  private Cafeteria cafeteria(Cuenta dueno, String nombre) {
    return cafeteriaRepository.saveAndFlush(
        new Cafeteria(
            dueno,
            nombre,
            dueno.getNombreCompleto(),
            "+506 8899-4410",
            dueno.getCorreo(),
            GeometriaUtil.punto(new Coordenada(9.9205, -84.137)),
            "Escazú",
            "San José",
            Set.of(NivelTueste.MEDIO)));
  }

  private Tueste tueste(Cosecha cosecha, Cuenta tostador, String pesoTostadoKg) {
    Solicitud solicitud = new Solicitud(cosecha, tostador, new BigDecimal("100.00"));
    solicitud.aprobar();
    solicitudRepository.saveAndFlush(solicitud);
    Lote lote = new Lote(solicitud, Lote.formatearCodigo(2026, ++numeroLote));
    lote.activar();
    loteRepository.saveAndFlush(lote);
    return tuesteRepository.saveAndFlush(
        new Tueste(
            lote,
            NivelTueste.MEDIO,
            new BigDecimal("50.00"),
            new BigDecimal(pesoTostadoKg),
            LocalDate.of(2026, 3, 1)));
  }

  private Pedido enCamino(Cafeteria cafeteria, String kg) {
    return pedidoRepository.saveAndFlush(new Pedido(cafeteria, tueste, new BigDecimal(kg)));
  }

  private Pedido entregado(Cafeteria cafeteria, String kg) {
    Pedido pedido = enCamino(cafeteria, kg);
    pedido.entregar();
    return pedidoRepository.saveAndFlush(pedido);
  }

  private void cerradoEn(Pedido pedido, Instant instante) {
    jdbcTemplate.update(
        "update pedido set cerrado_en = ? where id = ?", Timestamp.from(instante), pedido.getId());
  }

  private Cookie sesion(Cuenta cuenta) {
    return new Cookie(CookieSesion.NOMBRE, cookieSesion.crear(cuenta).getValue());
  }

  private BigDecimal kgDisponibles() {
    return tuesteRepository.kgDisponibles(tueste.getId()).orElseThrow();
  }

  private ResultActions crear(Cuenta cuenta, UUID tuesteId, Object cantidadKg) throws Exception {
    Map<String, Object> body = new HashMap<>();
    body.put("tuesteId", tuesteId);
    if (cantidadKg != OMITIR) {
      body.put("cantidadKg", cantidadKg);
    }
    var request =
        post("/api/pedidos")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(body));
    if (cuenta != null) {
      request.cookie(sesion(cuenta));
    }
    return mockMvc.perform(request);
  }

  private ResultActions historial(Cuenta cuenta) throws Exception {
    var request = get("/api/pedidos");
    if (cuenta != null) {
      request.cookie(sesion(cuenta));
    }
    return mockMvc.perform(request);
  }

  private ResultActions actualizar(Cuenta cuenta, UUID pedidoId, EstadoPedido estado)
      throws Exception {
    var request =
        patch("/api/pedidos/{id}", pedidoId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of("estado", estado)));
    if (cuenta != null) {
      request.cookie(sesion(cuenta));
    }
    return mockMvc.perform(request);
  }

  @Test
  void cafeteriaCreaUnPedidoEnCaminoYDescuentaKgDisponibles() throws Exception {
    assertThat(kgDisponibles()).isEqualByComparingTo("42.00");

    crear(sofia, tueste.getId(), "10.50")
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", startsWith("/api/pedidos/")))
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.tostador").value("Tostaduría Volcán"))
        .andExpect(jsonPath("$.cantidadKg").value(10.5))
        .andExpect(jsonPath("$.pedidoEn").isNotEmpty())
        .andExpect(jsonPath("$.estado").value("EN_CAMINO"));

    assertThat(pedidoRepository.findAll()).hasSize(1);
    assertThat(pedidoRepository.findAll().get(0).getEstado()).isEqualTo(EstadoPedido.EN_CAMINO);
    assertThat(kgDisponibles()).isEqualByComparingTo("31.50");
  }

  @Test
  void cantidadInvalidaOMayorALoDisponibleDa400() throws Exception {
    crear(sofia, tueste.getId(), "0")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errores.cantidadKg").value("La cantidad tiene que ser mayor a 0."));
    crear(sofia, tueste.getId(), OMITIR)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errores.cantidadKg").value("Ingresá la cantidad en kg."));
    crear(sofia, tueste.getId(), "42.01")
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.mensaje")
                .value("No hay suficiente café disponible: pedís 42.01 kg y quedan 42.00 kg."));

    assertThat(pedidoRepository.count()).isZero();
    assertThat(kgDisponibles()).isEqualByComparingTo("42.00");
  }

  @Test
  void historialDevuelvePedidosEIndicadoresDelMesActual() throws Exception {
    Pedido activo = enCamino(brumas, "8.00");
    Pedido entregadoEsteMes = entregado(brumas, "10.50");
    Pedido entregadoMesPasado = entregado(brumas, "5.00");
    YearMonth mesPasado = YearMonth.now(COSTA_RICA).minusMonths(1);
    cerradoEn(entregadoMesPasado, mesPasado.atDay(15).atStartOfDay(COSTA_RICA).toInstant());
    Pedido cancelado = enCamino(brumas, "3.00");
    cancelado.cancelar();
    pedidoRepository.saveAndFlush(cancelado);
    enCamino(deDiego, "20.00");

    historial(sofia)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.indicadores.pedidosActivos").value(1))
        .andExpect(jsonPath("$.indicadores.kgRecibidosEsteMes").value(10.5))
        .andExpect(jsonPath("$.indicadores.tostadoresActivos").value(1))
        .andExpect(jsonPath("$.pedidos.length()").value(4))
        .andExpect(jsonPath("$.pedidos[0].id").value(cancelado.getId().toString()))
        .andExpect(jsonPath("$.pedidos[0].tostador").value("Tostaduría Volcán"))
        .andExpect(jsonPath("$.pedidos[0].cantidadKg").value(3.0))
        .andExpect(jsonPath("$.pedidos[0].pedidoEn").isNotEmpty())
        .andExpect(jsonPath("$.pedidos[0].estado").value("CANCELADO"))
        .andExpect(jsonPath("$.pedidos[1].id").value(entregadoMesPasado.getId().toString()))
        .andExpect(jsonPath("$.pedidos[1].estado").value("ENTREGADO"))
        .andExpect(jsonPath("$.pedidos[2].id").value(entregadoEsteMes.getId().toString()))
        .andExpect(jsonPath("$.pedidos[2].estado").value("ENTREGADO"))
        .andExpect(jsonPath("$.pedidos[3].id").value(activo.getId().toString()))
        .andExpect(jsonPath("$.pedidos[3].estado").value("EN_CAMINO"));
  }

  @Test
  void actualizaAEntregadoOCanceladoYCancelarRestauraKg() throws Exception {
    Pedido aEntregar = enCamino(brumas, "10.00");
    Pedido aCancelar = enCamino(brumas, "5.00");
    assertThat(kgDisponibles()).isEqualByComparingTo("27.00");

    actualizar(sofia, aEntregar.getId(), EstadoPedido.ENTREGADO)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estado").value("ENTREGADO"))
        .andExpect(jsonPath("$.cantidadKg").value(10.0));
    assertThat(kgDisponibles()).isEqualByComparingTo("27.00");

    actualizar(sofia, aCancelar.getId(), EstadoPedido.CANCELADO)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estado").value("CANCELADO"));
    assertThat(kgDisponibles()).isEqualByComparingTo("32.00");

    actualizar(sofia, aEntregar.getId(), EstadoPedido.CANCELADO)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.mensaje").value("El pedido ya fue cerrado."));
    actualizar(sofia, aCancelar.getId(), EstadoPedido.ENTREGADO)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.mensaje").value("El pedido ya fue cerrado."));
    actualizar(sofia, aEntregar.getId(), EstadoPedido.EN_CAMINO)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.mensaje").value("Solo se puede marcar como entregado o cancelado."));
  }

  @Test
  void otraCafeteriaNoVeNiModificaPedidosAjenosYSinRolDa403O401() throws Exception {
    Pedido deSofia = enCamino(brumas, "10.00");

    historial(diego)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.indicadores.pedidosActivos").value(0))
        .andExpect(jsonPath("$.indicadores.kgRecibidosEsteMes").value(0))
        .andExpect(jsonPath("$.indicadores.tostadoresActivos").value(0))
        .andExpect(jsonPath("$.pedidos.length()").value(0));

    actualizar(diego, deSofia.getId(), EstadoPedido.ENTREGADO)
        .andExpect(status().isForbidden())
        .andExpect(
            jsonPath("$.mensaje").value("Solo la cafetería dueña del pedido puede actualizarlo."));

    historial(mario).andExpect(status().isForbidden());
    crear(mario, tueste.getId(), "1.00").andExpect(status().isForbidden());
    actualizar(mario, deSofia.getId(), EstadoPedido.CANCELADO).andExpect(status().isForbidden());

    historial(null).andExpect(status().isUnauthorized());
    crear(null, tueste.getId(), "1.00").andExpect(status().isUnauthorized());
    actualizar(null, deSofia.getId(), EstadoPedido.ENTREGADO).andExpect(status().isUnauthorized());

    assertThat(pedidoRepository.findById(deSofia.getId()).orElseThrow().getEstado())
        .isEqualTo(EstadoPedido.EN_CAMINO);
  }
}
