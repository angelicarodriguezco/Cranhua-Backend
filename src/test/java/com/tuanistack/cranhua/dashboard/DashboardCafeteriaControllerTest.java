package com.tuanistack.cranhua.dashboard;

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
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(BasePostgis.class)
class DashboardCafeteriaControllerTest {

  private static final String RUTA = "/api/cafeterias/{id}/dashboard";

  @Autowired private MockMvc mockMvc;

  @Autowired private CookieSesion cookieSesion;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private CuentaRepository cuentaRepository;

  @Autowired private NegocioTuesteRepository negocioRepository;

  @Autowired private FincaRepository fincaRepository;

  @Autowired private CosechaRepository cosechaRepository;

  @Autowired private SolicitudRepository solicitudRepository;

  @Autowired private LoteRepository loteRepository;

  @Autowired private TuesteRepository tuesteRepository;

  @Autowired private CafeteriaRepository cafeteriaRepository;

  @Autowired private PedidoRepository pedidoRepository;

  private Cuenta sofia;

  private Cuenta mario;

  private Cafeteria brumas;

  private Cafeteria deDiego;

  private Tueste deMario;

  private Tueste deLaura;

  private int numeroLote;

  @BeforeEach
  void crearDatos() {
    sofia = cuenta("Sofía Jiménez", "sofia@ejemplo.cr", Rol.CAFETERIA);
    Cuenta diego = cuenta("Diego Mora", "diego@ejemplo.cr", Rol.CAFETERIA);
    mario = cuenta("Mario Brenes", "mario@ejemplo.cr", Rol.TOSTADOR);
    Cuenta laura = cuenta("Laura Mora", "laura@ejemplo.cr", Rol.TOSTADOR);
    negocioRepository.saveAndFlush(
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
    deMario = tueste(cosecha, mario);
    deLaura = tueste(cosecha, laura);
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

  private Tueste tueste(Cosecha cosecha, Cuenta tostador) {
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
            new BigDecimal("42.00"),
            LocalDate.of(2026, 3, 1)));
  }

  private Pedido enCamino(Cafeteria cafeteria, Tueste tueste, String kg) {
    return pedidoRepository.saveAndFlush(new Pedido(cafeteria, tueste, new BigDecimal(kg)));
  }

  private Pedido entregado(Cafeteria cafeteria, Tueste tueste, String kg) {
    Pedido pedido = enCamino(cafeteria, tueste, kg);
    pedido.entregar();
    return pedidoRepository.saveAndFlush(pedido);
  }

  private Pedido cancelado(Cafeteria cafeteria, Tueste tueste, String kg) {
    Pedido pedido = enCamino(cafeteria, tueste, kg);
    pedido.cancelar();
    return pedidoRepository.saveAndFlush(pedido);
  }

  private void pedidoEn(Pedido pedido, String fecha) {
    jdbcTemplate.update(
        "update pedido set pedido_en = ? where id = ?",
        Timestamp.from(Instant.parse(fecha)),
        pedido.getId());
  }

  private Cookie sesion(Cuenta cuenta) {
    return new Cookie(CookieSesion.NOMBRE, cookieSesion.crear(cuenta).getValue());
  }

  @Test
  void devuelveLosIndicadoresDeLaCafeteria() throws Exception {
    entregado(brumas, deMario, "10.50");
    entregado(brumas, deMario, "5.00");
    enCamino(brumas, deMario, "8.00");
    enCamino(brumas, deLaura, "3.00");
    cancelado(brumas, deLaura, "20.00");

    mockMvc
        .perform(get(RUTA, brumas.getId()).cookie(sesion(sofia)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.indicadores.cafeRecibidoKg").value(15.5))
        .andExpect(jsonPath("$.indicadores.pedidosActivos").value(2))
        .andExpect(jsonPath("$.indicadores.tostadoresActivos").value(2));
  }

  @Test
  void unTostadorConSoloPedidosCanceladosNoEstaActivo() throws Exception {
    entregado(brumas, deMario, "5.00");
    cancelado(brumas, deLaura, "20.00");

    mockMvc
        .perform(get(RUTA, brumas.getId()).cookie(sesion(sofia)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.indicadores.tostadoresActivos").value(1));
  }

  @Test
  void devuelveLosCuatroPedidosMasRecientes() throws Exception {
    pedidoEn(entregado(brumas, deMario, "1.00"), "2026-03-01T12:00:00Z");
    pedidoEn(entregado(brumas, deMario, "2.00"), "2026-03-02T12:00:00Z");
    pedidoEn(enCamino(brumas, deMario, "5.00"), "2026-03-05T12:00:00Z");
    pedidoEn(cancelado(brumas, deLaura, "4.00"), "2026-03-04T12:00:00Z");
    pedidoEn(entregado(brumas, deMario, "3.00"), "2026-03-03T12:00:00Z");

    mockMvc
        .perform(get(RUTA, brumas.getId()).cookie(sesion(sofia)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.pedidosRecientes.length()").value(4))
        .andExpect(jsonPath("$.pedidosRecientes[0].tostador").value("Tostaduría Volcán"))
        .andExpect(jsonPath("$.pedidosRecientes[0].cantidadKg").value(5.0))
        .andExpect(jsonPath("$.pedidosRecientes[0].pedidoEn").value("2026-03-05T12:00:00Z"))
        .andExpect(jsonPath("$.pedidosRecientes[0].estado").value("EN_CAMINO"))
        // Laura no tiene negocio registrado
        .andExpect(jsonPath("$.pedidosRecientes[1].tostador").value("Laura Mora"))
        .andExpect(jsonPath("$.pedidosRecientes[1].estado").value("CANCELADO"))
        .andExpect(jsonPath("$.pedidosRecientes[2].cantidadKg").value(3.0))
        .andExpect(jsonPath("$.pedidosRecientes[3].cantidadKg").value(2.0));
  }

  @Test
  void cadaCafeteriaVeSoloSusDatos() throws Exception {
    entregado(deDiego, deMario, "10.00");
    enCamino(deDiego, deLaura, "5.00");

    mockMvc
        .perform(get(RUTA, brumas.getId()).cookie(sesion(sofia)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.indicadores.cafeRecibidoKg").value(0))
        .andExpect(jsonPath("$.indicadores.pedidosActivos").value(0))
        .andExpect(jsonPath("$.indicadores.tostadoresActivos").value(0))
        .andExpect(jsonPath("$.pedidosRecientes.length()").value(0));
  }

  @Test
  void cafeteriaAjenaDa403() throws Exception {
    mockMvc
        .perform(get(RUTA, deDiego.getId()).cookie(sesion(sofia)))
        .andExpect(status().isForbidden());
  }

  @Test
  void cafeteriaInexistenteDa404() throws Exception {
    mockMvc
        .perform(get(RUTA, UUID.randomUUID()).cookie(sesion(sofia)))
        .andExpect(status().isNotFound());
  }

  @Test
  void soloUnaCafeteriaPuedeConsultarlo() throws Exception {
    mockMvc
        .perform(get(RUTA, brumas.getId()).cookie(sesion(mario)))
        .andExpect(status().isForbidden());
    mockMvc.perform(get(RUTA, brumas.getId())).andExpect(status().isUnauthorized());
  }
}
