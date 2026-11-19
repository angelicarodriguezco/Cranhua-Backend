package com.tuanistack.cranhua.solicitud;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuanistack.cranhua.BasePostgis;
import com.tuanistack.cranhua.auth.CookieSesion;
import com.tuanistack.cranhua.cuenta.Cuenta;
import com.tuanistack.cranhua.cuenta.CuentaRepository;
import com.tuanistack.cranhua.cuenta.Rol;
import com.tuanistack.cranhua.finca.Cosecha;
import com.tuanistack.cranhua.finca.CosechaRepository;
import com.tuanistack.cranhua.finca.EstadoCosecha;
import com.tuanistack.cranhua.finca.Finca;
import com.tuanistack.cranhua.finca.FincaRepository;
import com.tuanistack.cranhua.lote.Lote;
import com.tuanistack.cranhua.lote.LoteRepository;
import jakarta.servlet.http.Cookie;
import java.math.BigDecimal;
import java.util.List;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(BasePostgis.class)
class SolicitudControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private CookieSesion cookieSesion;

  @Autowired private CuentaRepository cuentaRepository;

  @Autowired private FincaRepository fincaRepository;

  @Autowired private CosechaRepository cosechaRepository;

  @Autowired private SolicitudRepository solicitudRepository;

  @Autowired private LoteRepository loteRepository;

  private Cuenta andres;

  private Cuenta tostador;

  private Cosecha cosecha;

  @BeforeEach
  void crearDatos() {
    andres = cuenta("Andrés Solano", "andres@ejemplo.cr", Rol.CAFETALERO);
    tostador = cuenta("Mario Brenes", "mario@ejemplo.cr", Rol.TOSTADOR);
    Finca finca = SolicitudRepositoryTest.finca();
    finca.agregarDueno(andres);
    finca = fincaRepository.saveAndFlush(finca);
    cosecha = SolicitudRepositoryTest.nuevaCosecha(finca);
    cosecha.registrarPesoTrasSeleccion(new BigDecimal("500.00"));
    cosecha.publicar();
    cosecha = cosechaRepository.saveAndFlush(cosecha);
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

  private Solicitud solicitud(String kg) {
    return solicitudRepository.saveAndFlush(new Solicitud(cosecha, tostador, new BigDecimal(kg)));
  }

  private ResultActions responder(Cuenta cuenta, UUID solicitudId, EstadoSolicitud estado)
      throws Exception {
    return mockMvc.perform(
        patch("/api/solicitudes/{id}", solicitudId)
            .cookie(new Cookie(CookieSesion.NOMBRE, cookieSesion.crear(cuenta).getValue()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of("estado", estado))));
  }

  private BigDecimal kgDisponibles() {
    return cosechaRepository.findById(cosecha.getId()).orElseThrow().getKgDisponibles();
  }

  @Test
  void aprobarDescuentaLoSolicitadoYCreaElLote() throws Exception {
    Solicitud solicitud = solicitud("120.50");

    responder(andres, solicitud.getId(), EstadoSolicitud.APROBADA)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estado").value("APROBADA"))
        .andExpect(jsonPath("$.respondidaEn").isNotEmpty())
        .andExpect(jsonPath("$.codigoLote").value(matchesPattern("CRH-\\d{4}-\\d{4,}")));

    assertThat(kgDisponibles()).isEqualByComparingTo("379.50");
    List<Lote> lotes = loteRepository.findAll();
    assertThat(lotes).hasSize(1);
    assertThat(lotes.get(0).getSolicitud().getId()).isEqualTo(solicitud.getId());
    assertThat(solicitudRepository.findById(solicitud.getId()).orElseThrow().getEstado())
        .isEqualTo(EstadoSolicitud.APROBADA);
  }

  @Test
  void aprobarTodoLoDisponibleAgotaLaCosecha() throws Exception {
    responder(andres, solicitud("500.00").getId(), EstadoSolicitud.APROBADA)
        .andExpect(status().isOk());

    Cosecha leida = cosechaRepository.findById(cosecha.getId()).orElseThrow();
    assertThat(leida.getKgDisponibles()).isEqualByComparingTo("0");
    assertThat(leida.getEstado()).isEqualTo(EstadoCosecha.AGOTADA);
  }

  @Test
  void aprobarSinCantidadSuficienteDa409YNoCambiaNada() throws Exception {
    Solicitud primera = solicitud("400.00");
    Solicitud segunda = solicitud("200.00");
    responder(andres, primera.getId(), EstadoSolicitud.APROBADA).andExpect(status().isOk());

    responder(andres, segunda.getId(), EstadoSolicitud.APROBADA)
        .andExpect(status().isConflict())
        .andExpect(
            jsonPath("$.mensaje")
                .value(
                    "No hay suficiente café disponible: la solicitud pide 200.00 kg y quedan"
                        + " 100.00 kg."));

    assertThat(kgDisponibles()).isEqualByComparingTo("100.00");
    assertThat(loteRepository.count()).isEqualTo(1);
    assertThat(solicitudRepository.findById(segunda.getId()).orElseThrow().getEstado())
        .isEqualTo(EstadoSolicitud.PENDIENTE);
  }

  @Test
  void rechazarNoCambiaLaCantidadNiCreaLote() throws Exception {
    Solicitud solicitud = solicitud("120.00");

    responder(andres, solicitud.getId(), EstadoSolicitud.RECHAZADA)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estado").value("RECHAZADA"))
        .andExpect(jsonPath("$.codigoLote").doesNotExist());

    assertThat(kgDisponibles()).isEqualByComparingTo("500.00");
    assertThat(loteRepository.count()).isZero();
  }

  @Test
  void soloElDuenoDeLaFincaPuedeResponder() throws Exception {
    Cuenta lucia = cuenta("Lucía Mora", "lucia@ejemplo.cr", Rol.CAFETALERO);
    Solicitud solicitud = solicitud("120.00");

    responder(lucia, solicitud.getId(), EstadoSolicitud.APROBADA)
        .andExpect(status().isForbidden())
        .andExpect(
            jsonPath("$.mensaje")
                .value("Solo un dueño de la finca puede responder esta solicitud."));
    responder(tostador, solicitud.getId(), EstadoSolicitud.APROBADA)
        .andExpect(status().isForbidden());

    assertThat(kgDisponibles()).isEqualByComparingTo("500.00");
  }

  @Test
  void unaSolicitudYaRespondidaDa409() throws Exception {
    Solicitud solicitud = solicitud("120.00");
    responder(andres, solicitud.getId(), EstadoSolicitud.RECHAZADA).andExpect(status().isOk());

    responder(andres, solicitud.getId(), EstadoSolicitud.APROBADA)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.mensaje").value("Esta solicitud ya fue respondida."));

    assertThat(kgDisponibles()).isEqualByComparingTo("500.00");
    assertThat(loteRepository.count()).isZero();
  }

  @Test
  void responderConPendienteDa400() throws Exception {
    responder(andres, solicitud("120.00").getId(), EstadoSolicitud.PENDIENTE)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.mensaje").value("Solo se puede aprobar o rechazar una solicitud."));
  }

  @Test
  void unaSolicitudInexistenteDa404() throws Exception {
    responder(andres, UUID.randomUUID(), EstadoSolicitud.APROBADA)
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.mensaje").value("No encontramos esa solicitud."));
  }
}
