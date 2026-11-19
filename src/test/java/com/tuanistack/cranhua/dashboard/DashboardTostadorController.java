package com.tuanistack.cranhua.dashboard;

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
import java.time.temporal.ChronoUnit;
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
class DashboardTostadorControllerTest {

  private static final String RUTA = "/api/tostador/dashboard";

  @Autowired private MockMvc mockMvc;

  @Autowired private CookieSesion cookieSesion;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private CuentaRepository cuentaRepository;

  @Autowired private FincaRepository fincaRepository;

  @Autowired private CosechaRepository cosechaRepository;

  @Autowired private SolicitudRepository solicitudRepository;

  @Autowired private LoteRepository loteRepository;

  private Cuenta andres;

  private Cuenta mario;

  private Cuenta otraTostadora;

  private Cosecha cosecha;

  private int numeroLote;

  @BeforeEach
  void crearDatos() {
    andres = cuenta("Andrés Solano", "andres@ejemplo.cr", Rol.CAFETALERO);
    mario = cuenta("Mario Brenes", "mario@ejemplo.cr", Rol.TOSTADOR);
    otraTostadora = cuenta("Otra Tostadora", "otra@ejemplo.cr", Rol.TOSTADOR);
    Finca finca = SolicitudRepositoryTest.finca();
    finca.setNombre("Finca El Tucán");
    finca.agregarDueno(andres);
    cosecha =
        cosechaRepository.saveAndFlush(
            SolicitudRepositoryTest.nuevaCosecha(fincaRepository.saveAndFlush(finca)));
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

  private Solicitud pendiente(Cuenta tostador, String kg) {
    return solicitudRepository.saveAndFlush(new Solicitud(cosecha, tostador, new BigDecimal(kg)));
  }

  private Solicitud aprobadaConLote(Cuenta tostador, String kg) {
    Solicitud solicitud = pendiente(tostador, kg);
    solicitud.aprobar();
    solicitudRepository.saveAndFlush(solicitud);
    loteRepository.saveAndFlush(new Lote(solicitud, Lote.formatearCodigo(2026, ++numeroLote)));
    return solicitud;
  }

  private void respondidaEn(Solicitud solicitud, Instant fecha) {
    jdbcTemplate.update(
        "update solicitud set respondida_en = ? where id = ?",
        Timestamp.from(fecha),
        solicitud.getId());
  }

  private Cookie sesion(Cuenta cuenta) {
    return new Cookie(CookieSesion.NOMBRE, cookieSesion.crear(cuenta).getValue());
  }

  @Test
  void devuelveLosIndicadoresDelTostador() throws Exception {
    pendiente(mario, "10.00");
    aprobadaConLote(mario, "50.00");
    aprobadaConLote(mario, "20.50");
    Solicitud deOtroMes = aprobadaConLote(mario, "100.00");
    respondidaEn(deOtroMes, Instant.now().minus(60, ChronoUnit.DAYS));
    Solicitud rechazada = pendiente(mario, "30.00");
    rechazada.rechazar();
    solicitudRepository.saveAndFlush(rechazada);

    mockMvc
        .perform(get(RUTA).cookie(sesion(mario)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.solicitudesActivas").value(4))
        .andExpect(jsonPath("$.kgCompradosMes").value(70.5))
        .andExpect(jsonPath("$.lotesConCodigo").value(3));
  }

  @Test
  void cadaTostadorVeSoloSusDatos() throws Exception {
    pendiente(otraTostadora, "5.00");
    aprobadaConLote(otraTostadora, "40.00");

    mockMvc
        .perform(get(RUTA).cookie(sesion(mario)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.solicitudesActivas").value(0))
        .andExpect(jsonPath("$.kgCompradosMes").value(0))
        .andExpect(jsonPath("$.lotesConCodigo").value(0));
  }

  @Test
  void soloUnTostadorPuedeConsultarlo() throws Exception {
    mockMvc.perform(get(RUTA).cookie(sesion(andres))).andExpect(status().isForbidden());
    mockMvc.perform(get(RUTA)).andExpect(status().isUnauthorized());
  }
}
