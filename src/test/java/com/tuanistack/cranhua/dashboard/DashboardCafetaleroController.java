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
import com.tuanistack.cranhua.solicitud.Solicitud;
import com.tuanistack.cranhua.solicitud.SolicitudRepository;
import com.tuanistack.cranhua.solicitud.SolicitudRepositoryTest;
import jakarta.servlet.http.Cookie;
import java.math.BigDecimal;
import java.util.Set;
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
class DashboardCafetaleroControllerTest {

  private static final String RUTA = "/api/cafetalero/dashboard";

  @Autowired private MockMvc mockMvc;

  @Autowired private CookieSesion cookieSesion;

  @Autowired private CuentaRepository cuentaRepository;

  @Autowired private FincaRepository fincaRepository;

  @Autowired private CosechaRepository cosechaRepository;

  @Autowired private SolicitudRepository solicitudRepository;

  private Cuenta andres;

  private Cuenta lucia;

  private Cuenta mario;

  @BeforeEach
  void crearDatos() {
    andres = cuenta("Andrés Solano", "andres@ejemplo.cr", Rol.CAFETALERO);
    lucia = cuenta("Lucía Mora", "lucia@ejemplo.cr", Rol.CAFETALERO);
    mario = cuenta("Mario Brenes", "mario@ejemplo.cr", Rol.TOSTADOR);
  }

  @AfterEach
  void limpiar() {
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

  private Cosecha borrador(Finca finca) {
    return cosechaRepository.saveAndFlush(SolicitudRepositoryTest.nuevaCosecha(finca));
  }

  private Cosecha publicada(Finca finca, String kgDisponibles) {
    Cosecha cosecha = SolicitudRepositoryTest.nuevaCosecha(finca);
    cosecha.registrarPesoTrasSeleccion(new BigDecimal(kgDisponibles));
    cosecha.publicar();
    return cosechaRepository.saveAndFlush(cosecha);
  }

  private Solicitud solicitud(Cosecha cosecha, String kg) {
    return solicitudRepository.saveAndFlush(new Solicitud(cosecha, mario, new BigDecimal(kg)));
  }

  private Cookie sesion(Cuenta cuenta) {
    return new Cookie(CookieSesion.NOMBRE, cookieSesion.crear(cuenta).getValue());
  }

  @Test
  void devuelveLosIndicadoresDelCafetalero() throws Exception {
    Finca tucan = finca(andres, "Finca El Tucán");
    Cosecha activa = publicada(tucan, "500.00");
    publicada(finca(andres, "Finca La Loma"), "250.50");
    borrador(tucan);
    Finca eliminada = finca(andres, "Finca Vieja");
    publicada(eliminada, "999.00");
    eliminada.eliminar();
    fincaRepository.saveAndFlush(eliminada);
    solicitud(activa, "10.00");
    Solicitud aprobada = solicitud(activa, "20.00");
    aprobada.aprobar();
    solicitudRepository.saveAndFlush(aprobada);

    mockMvc
        .perform(get(RUTA).cookie(sesion(andres)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.indicadores.fincasRegistradas").value(2))
        .andExpect(jsonPath("$.indicadores.cosechasActivas").value(2))
        .andExpect(jsonPath("$.indicadores.kgDisponibles").value(750.5))
        .andExpect(jsonPath("$.indicadores.solicitudesPendientes").value(1))
        .andExpect(jsonPath("$.solicitudesRecientes.length()").value(1))
        .andExpect(jsonPath("$.solicitudesRecientes[0].nombreTostador").value("Mario Brenes"))
        .andExpect(jsonPath("$.solicitudesRecientes[0].estado").value("PENDIENTE"))
        .andExpect(jsonPath("$.cosechasRecientes.length()").value(3))
        .andExpect(jsonPath("$.cosechasRecientes[0].proceso").value("Lavado"));
  }

  @Test
  void limitaLasListasRecientes() throws Exception {
    Finca tucan = finca(andres, "Finca El Tucán");
    Cosecha cosecha = publicada(tucan, "500.00");
    for (int i = 0; i < 6; i++) {
      solicitud(cosecha, "1.00");
      borrador(tucan);
    }

    mockMvc
        .perform(get(RUTA).cookie(sesion(andres)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.indicadores.solicitudesPendientes").value(6))
        .andExpect(
            jsonPath("$.solicitudesRecientes.length()")
                .value(DashboardCafetaleroService.MAX_RECIENTES))
        .andExpect(
            jsonPath("$.cosechasRecientes.length()")
                .value(DashboardCafetaleroService.MAX_RECIENTES));
  }

  @Test
  void cadaCafetaleroVeSoloSusDatos() throws Exception {
    Cosecha deLucia = publicada(finca(lucia, "Finca de Lucía"), "300.00");
    solicitud(deLucia, "5.00");

    mockMvc
        .perform(get(RUTA).cookie(sesion(andres)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.indicadores.fincasRegistradas").value(0))
        .andExpect(jsonPath("$.indicadores.cosechasActivas").value(0))
        .andExpect(jsonPath("$.indicadores.kgDisponibles").value(0))
        .andExpect(jsonPath("$.indicadores.solicitudesPendientes").value(0))
        .andExpect(jsonPath("$.solicitudesRecientes.length()").value(0))
        .andExpect(jsonPath("$.cosechasRecientes.length()").value(0));
  }

  @Test
  void soloUnCafetaleroPuedeConsultarlo() throws Exception {
    mockMvc.perform(get(RUTA).cookie(sesion(mario))).andExpect(status().isForbidden());
    mockMvc.perform(get(RUTA)).andExpect(status().isUnauthorized());
  }
}
