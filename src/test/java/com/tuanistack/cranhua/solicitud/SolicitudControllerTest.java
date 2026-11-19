package com.tuanistack.cranhua.solicitud;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import com.tuanistack.cranhua.finca.Cosecha;
import com.tuanistack.cranhua.finca.CosechaRepository;
import com.tuanistack.cranhua.finca.EstadoCosecha;
import com.tuanistack.cranhua.finca.Finca;
import com.tuanistack.cranhua.finca.FincaRepository;
import com.tuanistack.cranhua.lote.Lote;
import com.tuanistack.cranhua.lote.LoteRepository;
import jakarta.servlet.http.Cookie;
import java.math.BigDecimal;
import java.util.HashMap;
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

  private Cuenta cuenta(String nombre, String correo, Rol... roles) {
    return cuentaRepository.saveAndFlush(new Cuenta(nombre, correo, "hash", Set.of(roles)));
  }

  private Solicitud solicitud(String kg) {
    return solicitudRepository.saveAndFlush(new Solicitud(cosecha, tostador, new BigDecimal(kg)));
  }

  private ResultActions crear(Cuenta cuenta, UUID cosechaId, Object cantidadKg) throws Exception {
    Map<String, Object> body = new HashMap<>();
    body.put("cosechaId", cosechaId);
    if (cantidadKg != OMITIR) {
      body.put("cantidadKg", cantidadKg);
    }
    var request =
        post("/api/solicitudes")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(body));
    if (cuenta != null) {
      request.cookie(new Cookie(CookieSesion.NOMBRE, cookieSesion.crear(cuenta).getValue()));
    }
    return mockMvc.perform(request);
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

  private static final Object OMITIR = new Object();

  @Test
  void tostadorCreaUnaSolicitud() throws Exception {
    crear(tostador, cosecha.getId(), "120.50")
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", startsWith("/api/solicitudes/")))
        .andExpect(jsonPath("$.cosechaId").value(cosecha.getId().toString()))
        .andExpect(jsonPath("$.cantidadKg").value(120.5))
        .andExpect(jsonPath("$.precioKg").value(9.75))
        .andExpect(jsonPath("$.totalEstimado").value(1174.88))
        .andExpect(jsonPath("$.estado").value("PENDIENTE"))
        .andExpect(jsonPath("$.solicitadaEn").isNotEmpty())
        .andExpect(jsonPath("$.respondidaEn").doesNotExist())
        .andExpect(jsonPath("$.codigoLote").doesNotExist());

    List<Solicitud> guardadas = solicitudRepository.findAll();
    assertThat(guardadas).hasSize(1);
    assertThat(guardadas.get(0).getTostador().getId()).isEqualTo(tostador.getId());
    assertThat(guardadas.get(0).getCosecha().getId()).isEqualTo(cosecha.getId());
    assertThat(kgDisponibles()).isEqualByComparingTo("500.00");
  }

  @Test
  void cantidadInvalidaDa400() throws Exception {
    crear(tostador, cosecha.getId(), "0")
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.errores.cantidadKg").value("La cantidad tiene que ser mayor a 0."));
    crear(tostador, cosecha.getId(), OMITIR)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errores.cantidadKg").value("Ingresá la cantidad en kg."));

    assertThat(solicitudRepository.count()).isZero();
  }

  @Test
  void cantidadMayorALoDisponibleDa400() throws Exception {
    crear(tostador, cosecha.getId(), "500.01")
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.mensaje")
                .value(
                    "No hay suficiente café disponible: pedís 500.01 kg y quedan 500.00 kg."));

    assertThat(solicitudRepository.count()).isZero();
    assertThat(kgDisponibles()).isEqualByComparingTo("500.00");
  }

  @Test
  void noSePuedeSolicitarLaPropiaCosechaNiSinRolTostador() throws Exception {
    Cuenta ana = cuenta("Ana Rojas", "ana@ejemplo.cr", Rol.CAFETALERO, Rol.TOSTADOR);
    Finca finca = cosecha.getFinca();
    finca.agregarDueno(ana);
    fincaRepository.saveAndFlush(finca);

    crear(ana, cosecha.getId(), "100.00")
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.mensaje").value("No podés solicitar tu propia cosecha."));
    crear(andres, cosecha.getId(), "100.00").andExpect(status().isForbidden());

    assertThat(solicitudRepository.count()).isZero();
  }

  @Test
  void sinSesionDa401() throws Exception {
    crear(null, cosecha.getId(), "100.00").andExpect(status().isUnauthorized());
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
