package com.tuanistack.cranhua.historial;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tuanistack.cranhua.BasePostgis;
import com.tuanistack.cranhua.auth.CookieSesion;
import com.tuanistack.cranhua.cuenta.Cuenta;
import com.tuanistack.cranhua.cuenta.CuentaRepository;
import com.tuanistack.cranhua.cuenta.Rol;
import com.tuanistack.cranhua.finca.Cosecha;
import com.tuanistack.cranhua.finca.CosechaRepository;
import com.tuanistack.cranhua.finca.Finca;
import com.tuanistack.cranhua.finca.FincaRepository;
import com.tuanistack.cranhua.lote.Lote;
import com.tuanistack.cranhua.lote.LoteRepository;
import com.tuanistack.cranhua.solicitud.Solicitud;
import com.tuanistack.cranhua.solicitud.SolicitudRepository;
import com.tuanistack.cranhua.solicitud.SolicitudRepositoryTest;
import jakarta.servlet.http.Cookie;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
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
class HistorialCafetaleroControllerTest {

  private static final String RUTA = "/api/cafetalero/historial";

  @Autowired private MockMvc mockMvc;

  @Autowired private CookieSesion cookieSesion;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private CuentaRepository cuentaRepository;

  @Autowired private FincaRepository fincaRepository;

  @Autowired private CosechaRepository cosechaRepository;

  @Autowired private SolicitudRepository solicitudRepository;

  @Autowired private LoteRepository loteRepository;

  private Cuenta andres;

  private Cuenta lucia;

  private Cuenta mario;

  private Finca tucan;

  private Finca loma;

  private int numeroLote;

  @BeforeEach
  void crearDatos() {
    andres = cuenta("Andrés Solano", "andres@ejemplo.cr", Rol.CAFETALERO);
    lucia = cuenta("Lucía Mora", "lucia@ejemplo.cr", Rol.CAFETALERO);
    mario = cuenta("Mario Brenes", "mario@ejemplo.cr", Rol.TOSTADOR);
    tucan = finca(andres, "Finca El Tucán");
    loma = finca(andres, "Finca La Loma");

    Cosecha tucan2026 = cosecha(tucan, LocalDate.of(2026, 1, 5), "1200.00");
    cosecha(tucan, LocalDate.of(2025, 1, 10), "800.00");
    Cosecha loma2026 = cosecha(loma, LocalDate.of(2026, 3, 1), "300.00");

    loteVendido(tucan2026, "50.00", Instant.parse("2026-02-10T15:00:00Z"));
    loteVendido(loma2026, "20.00", Instant.parse("2025-06-01T15:00:00Z"));
  }

  @AfterEach
  void limpiar() {
    loteRepository.deleteAll();
    solicitudRepository.deleteAll();
    cosechaRepository.deleteAll();
    fincaRepository.deleteAll();
    cuentaRepository.deleteAll();
  }

  private Cuenta cuenta(String nombre, String correo, Rol rol) {
    return cuentaRepository.saveAndFlush(new Cuenta(nombre, correo, "hash", Set.of(rol)));
  }

  private Finca finca(Cuenta dueno, String nombre) {
    Finca finca = SolicitudRepositoryTest.finca();
    finca.setNombre(nombre);
    finca.agregarDueno(dueno);
    return fincaRepository.saveAndFlush(finca);
  }

  private Cosecha cosecha(Finca finca, LocalDate inicio, String kg) {
    Cosecha cosecha = SolicitudRepositoryTest.nuevaCosecha(finca);
    cosecha.setFechaInicio(inicio);
    cosecha.setFechaFin(inicio.plusDays(30));
    cosecha.setCantidadCosechadaKg(new BigDecimal(kg));
    return cosechaRepository.saveAndFlush(cosecha);
  }

  private void loteVendido(Cosecha cosecha, String kg, Instant vendidoEn) {
    Solicitud solicitud =
        solicitudRepository.saveAndFlush(new Solicitud(cosecha, mario, new BigDecimal(kg)));
    solicitud.aprobar();
    solicitudRepository.saveAndFlush(solicitud);
    loteRepository.saveAndFlush(new Lote(solicitud, Lote.formatearCodigo(2026, ++numeroLote)));
    jdbcTemplate.update(
        "update solicitud set respondida_en = ? where id = ?",
        Timestamp.from(vendidoEn),
        solicitud.getId());
  }

  private Cookie sesion(Cuenta cuenta) {
    return new Cookie(CookieSesion.NOMBRE, cookieSesion.crear(cuenta).getValue());
  }

  @Test
  void listaCosechasYLotesVendidosDelCafetalero() throws Exception {
    mockMvc
        .perform(get(RUTA).cookie(sesion(andres)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.indicadores.cosechasRegistradas").value(3))
        .andExpect(jsonPath("$.indicadores.produccionTotalKg").value(2300))
        .andExpect(jsonPath("$.indicadores.lotesVendidos").value(2))
        .andExpect(jsonPath("$.cosechas.contenido.length()").value(3))
        .andExpect(jsonPath("$.cosechas.contenido[0].fechaInicio").value("2026-03-01"))
        .andExpect(jsonPath("$.cosechas.contenido[0].finca").value("Finca La Loma"))
        .andExpect(jsonPath("$.cosechas.contenido[2].fechaInicio").value("2025-01-10"))
        .andExpect(jsonPath("$.lotesVendidos.contenido.length()").value(2))
        .andExpect(jsonPath("$.lotesVendidos.contenido[0].codigo").value("CRH-2026-0001"))
        .andExpect(jsonPath("$.lotesVendidos.contenido[0].comprador").value("Mario Brenes"))
        .andExpect(jsonPath("$.lotesVendidos.contenido[0].kg").value(50))
        .andExpect(jsonPath("$.lotesVendidos.contenido[0].precioKg").value(9.75))
        .andExpect(jsonPath("$.lotesVendidos.contenido[0].total").value(487.5));
  }

  @Test
  void filtraPorFinca() throws Exception {
    mockMvc
        .perform(get(RUTA).param("fincaId", tucan.getId().toString()).cookie(sesion(andres)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.indicadores.cosechasRegistradas").value(2))
        .andExpect(jsonPath("$.indicadores.produccionTotalKg").value(2000))
        .andExpect(jsonPath("$.indicadores.lotesVendidos").value(1))
        .andExpect(jsonPath("$.lotesVendidos.contenido[0].finca").value("Finca El Tucán"));
  }

  @Test
  void filtraPorFecha() throws Exception {
    mockMvc
        .perform(get(RUTA).param("desde", "2026-01-01").cookie(sesion(andres)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.indicadores.cosechasRegistradas").value(2))
        .andExpect(jsonPath("$.indicadores.produccionTotalKg").value(1500))
        .andExpect(jsonPath("$.indicadores.lotesVendidos").value(1));

    mockMvc
        .perform(
            get(RUTA)
                .param("desde", "2025-01-01")
                .param("hasta", "2025-12-31")
                .cookie(sesion(andres)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.indicadores.cosechasRegistradas").value(1))
        .andExpect(jsonPath("$.indicadores.lotesVendidos").value(1))
        .andExpect(jsonPath("$.lotesVendidos.contenido[0].finca").value("Finca La Loma"));
  }

  @Test
  void pagina() throws Exception {
    mockMvc
        .perform(get(RUTA).param("tamano", "2").cookie(sesion(andres)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.cosechas.contenido.length()").value(2))
        .andExpect(jsonPath("$.cosechas.pagina").value(0))
        .andExpect(jsonPath("$.cosechas.tamano").value(2))
        .andExpect(jsonPath("$.cosechas.totalElementos").value(3))
        .andExpect(jsonPath("$.cosechas.totalPaginas").value(2))
        .andExpect(jsonPath("$.indicadores.cosechasRegistradas").value(3));

    mockMvc
        .perform(
            get(RUTA)
                .param("tamano", "2")
                .param("paginaCosechas", "1")
                .param("paginaLotes", "1")
                .cookie(sesion(andres)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.cosechas.contenido.length()").value(1))
        .andExpect(jsonPath("$.cosechas.contenido[0].fechaInicio").value("2025-01-10"))
        .andExpect(jsonPath("$.lotesVendidos.contenido.length()").value(0));
  }

  @Test
  void cadaCafetaleroVeSoloSusDatos() throws Exception {
    mockMvc
        .perform(get(RUTA).cookie(sesion(lucia)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.indicadores.cosechasRegistradas").value(0))
        .andExpect(jsonPath("$.indicadores.produccionTotalKg").value(0))
        .andExpect(jsonPath("$.indicadores.lotesVendidos").value(0))
        .andExpect(jsonPath("$.cosechas.contenido.length()").value(0))
        .andExpect(jsonPath("$.lotesVendidos.contenido.length()").value(0));

    mockMvc
        .perform(get(RUTA).param("fincaId", tucan.getId().toString()).cookie(sesion(lucia)))
        .andExpect(status().isNotFound());
  }

  @Test
  void rechazaFiltrosInvalidos() throws Exception {
    mockMvc
        .perform(
            get(RUTA)
                .param("desde", "2026-02-01")
                .param("hasta", "2026-01-01")
                .cookie(sesion(andres)))
        .andExpect(status().isBadRequest());
    mockMvc
        .perform(get(RUTA).param("tamano", "0").cookie(sesion(andres)))
        .andExpect(status().isBadRequest());
    mockMvc
        .perform(get(RUTA).param("paginaCosechas", "-1").cookie(sesion(andres)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void soloUnCafetaleroPuedeConsultarlo() throws Exception {
    mockMvc.perform(get(RUTA).cookie(sesion(mario))).andExpect(status().isForbidden());
    mockMvc.perform(get(RUTA)).andExpect(status().isUnauthorized());
  }
}
