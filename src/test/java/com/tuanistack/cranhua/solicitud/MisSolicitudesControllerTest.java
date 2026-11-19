package com.tuanistack.cranhua.solicitud;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
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
class MisSolicitudesControllerTest {

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

  private Cosecha cosechaTucan;

  private Cosecha cosechaRoble;

  @BeforeEach
  void crearDatos() {
    andres = cuenta("Andrés Solano", "andres@ejemplo.cr", Rol.CAFETALERO);
    mario = cuenta("Mario Brenes", "mario@ejemplo.cr", Rol.TOSTADOR);
    cosechaTucan = cosecha(finca("Finca El Tucán", andres));
    cosechaRoble =
        cosecha(finca("Finca El Roble", cuenta("Lucía Mora", "lucia@ejemplo.cr", Rol.CAFETALERO)));
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

  private Finca finca(String nombre, Cuenta dueno) {
    Finca finca = SolicitudRepositoryTest.finca();
    finca.setNombre(nombre);
    finca.agregarDueno(dueno);
    return fincaRepository.saveAndFlush(finca);
  }

  private Cosecha cosecha(Finca finca) {
    return cosechaRepository.saveAndFlush(SolicitudRepositoryTest.nuevaCosecha(finca));
  }

  private Solicitud solicitud(Cosecha cosecha, Cuenta tostador, String kg) {
    return solicitudRepository.saveAndFlush(new Solicitud(cosecha, tostador, new BigDecimal(kg)));
  }

  private void solicitadaEn(Solicitud solicitud, Instant fecha) {
    jdbcTemplate.update(
        "update solicitud set solicitada_en = ? where id = ?",
        Timestamp.from(fecha),
        solicitud.getId());
  }

  private Cookie sesion(Cuenta cuenta) {
    return new Cookie(CookieSesion.NOMBRE, cookieSesion.crear(cuenta).getValue());
  }

  @Test
  void elTostadorVeSusSolicitudesConIndicadores() throws Exception {
    Solicitud vieja = solicitud(cosechaTucan, mario, "50.00");
    vieja.aprobar();
    solicitudRepository.saveAndFlush(vieja);
    loteRepository.saveAndFlush(new Lote(vieja, "CRH-2026-0001"));
    solicitadaEn(vieja, Instant.now().minus(90, ChronoUnit.DAYS));
    Solicitud reciente = solicitud(cosechaRoble, mario, "20.00");
    solicitud(cosechaTucan, cuenta("Otra Tostadora", "otra@ejemplo.cr", Rol.TOSTADOR), "5.00");

    mockMvc
        .perform(get("/api/solicitudes/enviadas").cookie(sesion(mario)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.indicadores.pendientes").value(1))
        .andExpect(jsonPath("$.indicadores.esteMes").value(1))
        .andExpect(jsonPath("$.indicadores.fincasContactadas").value(2))
        .andExpect(jsonPath("$.solicitudes", hasSize(2)))
        .andExpect(jsonPath("$.solicitudes[0].id").value(reciente.getId().toString()))
        .andExpect(jsonPath("$.solicitudes[0].finca").value("Finca El Roble"))
        .andExpect(jsonPath("$.solicitudes[0].variedad").value("Caturra"))
        .andExpect(jsonPath("$.solicitudes[0].codigoLote").value(nullValue()))
        .andExpect(jsonPath("$.solicitudes[0].cantidadKg").value(20.0))
        .andExpect(jsonPath("$.solicitudes[0].estado").value("PENDIENTE"))
        .andExpect(jsonPath("$.solicitudes[1].finca").value("Finca El Tucán"))
        .andExpect(jsonPath("$.solicitudes[1].codigoLote").value("CRH-2026-0001"))
        .andExpect(jsonPath("$.solicitudes[1].estado").value("APROBADA"));
  }

  @Test
  void elTostadorSinSolicitudesRecibeIndicadoresEnCero() throws Exception {
    mockMvc
        .perform(get("/api/solicitudes/enviadas").cookie(sesion(mario)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.indicadores.pendientes").value(0))
        .andExpect(jsonPath("$.indicadores.esteMes").value(0))
        .andExpect(jsonPath("$.indicadores.fincasContactadas").value(0))
        .andExpect(jsonPath("$.solicitudes", hasSize(0)));
  }

  @Test
  void elCafetaleroVeSoloLasSolicitudesDeSusFincas() throws Exception {
    Solicitud rechazada = solicitud(cosechaTucan, mario, "10.00");
    rechazada.rechazar();
    solicitudRepository.saveAndFlush(rechazada);
    Solicitud pendiente = solicitud(cosechaTucan, mario, "30.00");
    solicitud(cosechaRoble, mario, "40.00");

    mockMvc
        .perform(get("/api/solicitudes/recibidas").cookie(sesion(andres)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].id").value(pendiente.getId().toString()))
        .andExpect(jsonPath("$[0].nombreTostador").value("Mario Brenes"))
        .andExpect(jsonPath("$[0].variedad").value("Caturra"))
        .andExpect(jsonPath("$[0].finca").value("Finca El Tucán"))
        .andExpect(jsonPath("$[0].cantidadKg").value(30.0))
        .andExpect(jsonPath("$[0].estado").value("PENDIENTE"))
        .andExpect(jsonPath("$[1].estado").value("RECHAZADA"));
  }

  @Test
  void elCafetaleroFiltraPorEstado() throws Exception {
    Solicitud rechazada = solicitud(cosechaTucan, mario, "10.00");
    rechazada.rechazar();
    solicitudRepository.saveAndFlush(rechazada);
    Solicitud pendiente = solicitud(cosechaTucan, mario, "30.00");

    mockMvc
        .perform(
            get("/api/solicitudes/recibidas").param("estado", "PENDIENTE").cookie(sesion(andres)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].id").value(pendiente.getId().toString()));
  }

  @Test
  void noMuestraSolicitudesDeFincasEliminadas() throws Exception {
    solicitud(cosechaTucan, mario, "10.00");
    Finca finca = fincaRepository.findById(cosechaTucan.getFinca().getId()).orElseThrow();
    finca.eliminar();
    fincaRepository.saveAndFlush(finca);

    mockMvc
        .perform(get("/api/solicitudes/recibidas").cookie(sesion(andres)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(0)));
  }

  @Test
  void unEstadoInvalidoDevuelve400() throws Exception {
    mockMvc
        .perform(get("/api/solicitudes/recibidas").param("estado", "NADA").cookie(sesion(andres)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void cadaRolSoloAccedeASuEndpoint() throws Exception {
    mockMvc
        .perform(get("/api/solicitudes/enviadas").cookie(sesion(andres)))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(get("/api/solicitudes/recibidas").cookie(sesion(mario)))
        .andExpect(status().isForbidden());
    mockMvc.perform(get("/api/solicitudes/enviadas")).andExpect(status().isUnauthorized());
  }
}
