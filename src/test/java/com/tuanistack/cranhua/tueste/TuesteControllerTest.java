package com.tuanistack.cranhua.tueste;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import com.tuanistack.cranhua.finca.FincaRepository;
import com.tuanistack.cranhua.lote.EstadoTrazabilidad;
import com.tuanistack.cranhua.lote.Lote;
import com.tuanistack.cranhua.lote.LoteRepository;
import com.tuanistack.cranhua.solicitud.Solicitud;
import com.tuanistack.cranhua.solicitud.SolicitudRepository;
import com.tuanistack.cranhua.solicitud.SolicitudRepositoryTest;
import jakarta.servlet.http.Cookie;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(BasePostgis.class)
class TuesteControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private CookieSesion cookieSesion;

  @Autowired private CuentaRepository cuentaRepository;

  @Autowired private FincaRepository fincaRepository;

  @Autowired private CosechaRepository cosechaRepository;

  @Autowired private SolicitudRepository solicitudRepository;

  @Autowired private LoteRepository loteRepository;

  @Autowired private TuesteRepository tuesteRepository;

  @MockitoBean private KgPedidosTueste kgPedidosTueste;

  private Cuenta mario;

  private Cosecha cosecha;

  @BeforeEach
  void crearDatos() {
    when(kgPedidosTueste.total(any())).thenReturn(BigDecimal.ZERO);
    mario = cuenta("Mario Brenes", "mario@ejemplo.cr", Rol.TOSTADOR);
    cosecha =
        cosechaRepository.saveAndFlush(
            SolicitudRepositoryTest.nuevaCosecha(
                fincaRepository.saveAndFlush(SolicitudRepositoryTest.finca())));
  }

  @AfterEach
  void limpiar() {
    tuesteRepository.deleteAll();
    loteRepository.deleteAll();
    solicitudRepository.deleteAll();
    cosechaRepository.deleteAll();
    fincaRepository.deleteAll();
    cuentaRepository.deleteAll();
  }

  private Cuenta cuenta(String nombre, String correo, Rol rol) {
    return cuentaRepository.saveAndFlush(new Cuenta(nombre, correo, "hash", Set.of(rol)));
  }

  private Cookie sesion(Cuenta cuenta) {
    return new Cookie(CookieSesion.NOMBRE, cookieSesion.crear(cuenta).getValue());
  }

  private Lote lote(Cuenta tostador, String kg, String codigo) {
    Solicitud solicitud = new Solicitud(cosecha, tostador, new BigDecimal(kg));
    solicitud.aprobar();
    solicitud = solicitudRepository.saveAndFlush(solicitud);
    return loteRepository.saveAndFlush(new Lote(solicitud, codigo));
  }

  private Tueste tueste(
      Lote lote, NivelTueste nivel, String pesoVerdeKg, String pesoTostadoKg, LocalDate fecha) {
    return tuesteRepository.saveAndFlush(
        new Tueste(lote, nivel, new BigDecimal(pesoVerdeKg), new BigDecimal(pesoTostadoKg), fecha));
  }

  private ResultActions actualizar(Cuenta cuenta, UUID tuesteId, Map<String, Object> body)
      throws Exception {
    return mockMvc.perform(
        patch("/api/tuestes/{id}", tuesteId)
            .cookie(sesion(cuenta))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(body)));
  }

  private ResultActions registrar(Cuenta cuenta, Map<String, Object> body) throws Exception {
    return mockMvc.perform(
        post("/api/tuestes")
            .cookie(sesion(cuenta))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(body)));
  }

  private Map<String, Object> nuevoTueste(Lote lote, Object pesoVerdeKg, Object pesoTostadoKg) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("loteId", lote.getId());
    body.put("nivel", "MEDIO");
    body.put("pesoVerdeKg", pesoVerdeKg);
    body.put("pesoTostadoKg", pesoTostadoKg);
    body.put("fechaTueste", "2026-03-01");
    return body;
  }

  @Test
  void registraElTuesteConLaMermaYActivaElCodigoDelLote() throws Exception {
    Lote lote = lote(mario, "100.00", "CRH-2026-0110");
    Map<String, Object> body = nuevoTueste(lote, 50, 43);
    body.put("notas", "Primer tueste");

    registrar(mario, body)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.loteId").value(lote.getId().toString()))
        .andExpect(jsonPath("$.codigoLote").value("CRH-2026-0110"))
        .andExpect(jsonPath("$.nivel").value("MEDIO"))
        .andExpect(jsonPath("$.pesoVerdeKg").value(50.0))
        .andExpect(jsonPath("$.pesoTostadoKg").value(43.0))
        .andExpect(jsonPath("$.merma").value(14.0))
        .andExpect(jsonPath("$.fechaTueste").value("2026-03-01"))
        .andExpect(jsonPath("$.notas").value("Primer tueste"));

    assertThat(tuesteRepository.findByLoteIdOrderByFechaTuesteDesc(lote.getId())).hasSize(1);
    assertThat(loteRepository.findById(lote.getId()).orElseThrow().getEstadoTrazabilidad())
        .isEqualTo(EstadoTrazabilidad.ACTIVO);
  }

  @Test
  void registraVariosTuestesHastaLosKgQueQuedanPorTostar() throws Exception {
    Lote lote = lote(mario, "100.00", "CRH-2026-0111");
    tueste(lote, NivelTueste.CLARO, "60.00", "51.00", LocalDate.of(2026, 3, 1));

    registrar(mario, nuevoTueste(lote, 40, 34)).andExpect(status().isCreated());
    registrar(mario, nuevoTueste(lote, 0.01, 0.01))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.errores.pesoVerdeKg")
                .value("El peso verde supera los kg disponibles del lote."));

    assertThat(tuesteRepository.findByLoteIdOrderByFechaTuesteDesc(lote.getId())).hasSize(2);
  }

  @Test
  void registrarConPesoTostadoMayorAlVerdeDa400() throws Exception {
    Lote lote = lote(mario, "100.00", "CRH-2026-0112");

    registrar(mario, nuevoTueste(lote, 40, 40.01))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.errores.pesoTostadoKg")
                .value("El peso tostado no puede ser mayor al peso verde."));

    assertThat(tuesteRepository.count()).isZero();
  }

  @Test
  void registrarConNivelInvalidoDa400() throws Exception {
    Lote lote = lote(mario, "100.00", "CRH-2026-0113");
    Map<String, Object> body = nuevoTueste(lote, 40, 34);
    body.put("nivel", "FUERTE");

    registrar(mario, body)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errores.nivel").value("Valor inválido."));
  }

  @Test
  void registrarEnUnLoteAjenoDa403YNoActivaElCodigo() throws Exception {
    Cuenta ana = cuenta("Ana Vargas", "ana@ejemplo.cr", Rol.TOSTADOR);
    Lote lote = lote(mario, "100.00", "CRH-2026-0114");

    registrar(ana, nuevoTueste(lote, 40, 34))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.mensaje").value("Este lote no es tuyo."));

    assertThat(tuesteRepository.count()).isZero();
    assertThat(loteRepository.findById(lote.getId()).orElseThrow().getEstadoTrazabilidad())
        .isEqualTo(EstadoTrazabilidad.PENDIENTE);
  }

  @Test
  void actualizaSoloLosCamposEnviadosYRecalculaLaMerma() throws Exception {
    Lote lote = lote(mario, "100.00", "CRH-2026-0100");
    Tueste tueste = tueste(lote, NivelTueste.CLARO, "50.00", "43.00", LocalDate.of(2026, 3, 1));
    tueste.setNotas("Original");
    tuesteRepository.saveAndFlush(tueste);

    Map<String, Object> body = new LinkedHashMap<>();
    body.put("nivel", "MEDIO");
    body.put("pesoTostadoKg", 40);
    body.put("notas", "Ajuste de perfil");

    actualizar(mario, tueste.getId(), body)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(tueste.getId().toString()))
        .andExpect(jsonPath("$.loteId").value(lote.getId().toString()))
        .andExpect(jsonPath("$.nivel").value("MEDIO"))
        .andExpect(jsonPath("$.pesoVerdeKg").value(50.0))
        .andExpect(jsonPath("$.pesoTostadoKg").value(40.0))
        .andExpect(jsonPath("$.merma").value(20.0))
        .andExpect(jsonPath("$.fechaTueste").value("2026-03-01"))
        .andExpect(jsonPath("$.notas").value("Ajuste de perfil"));

    Tueste leido = tuesteRepository.findById(tueste.getId()).orElseThrow();
    assertThat(leido.getLote().getId()).isEqualTo(lote.getId());
    assertThat(leido.getNivel()).isEqualTo(NivelTueste.MEDIO);
    assertThat(leido.getPesoVerdeKg()).isEqualByComparingTo("50.00");
    assertThat(leido.getPesoTostadoKg()).isEqualByComparingTo("40.00");
    assertThat(leido.getMermaPorcentaje()).isEqualByComparingTo("20.00");
    assertThat(leido.getFechaTueste()).isEqualTo(LocalDate.of(2026, 3, 1));
    assertThat(leido.getNotas()).isEqualTo("Ajuste de perfil");
  }

  @Test
  void noCambiaElLoteAunqueSeEnvieOtroId() throws Exception {
    Lote lote = lote(mario, "100.00", "CRH-2026-0101");
    Lote otro = lote(mario, "50.00", "CRH-2026-0102");
    Tueste tueste = tueste(lote, NivelTueste.CLARO, "40.00", "34.00", LocalDate.of(2026, 3, 1));

    Map<String, Object> body = new LinkedHashMap<>();
    body.put("loteId", otro.getId());
    body.put("notas", "Sin mover de lote");

    actualizar(mario, tueste.getId(), body)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.loteId").value(lote.getId().toString()))
        .andExpect(jsonPath("$.notas").value("Sin mover de lote"));

    assertThat(tuesteRepository.findById(tueste.getId()).orElseThrow().getLote().getId())
        .isEqualTo(lote.getId());
  }

  @Test
  void editarUnTuesteAjenoDa403() throws Exception {
    Cuenta ana = cuenta("Ana Vargas", "ana@ejemplo.cr", Rol.TOSTADOR);
    Lote lote = lote(mario, "60.00", "CRH-2026-0103");
    Tueste tueste = tueste(lote, NivelTueste.MEDIO, "40.00", "34.00", LocalDate.of(2026, 3, 1));

    actualizar(ana, tueste.getId(), Map.of("notas", "Intento ajeno"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.mensaje").value("Este lote no es tuyo."));
  }

  @Test
  void pesoTostadoMayorAlVerdeDa400EnSuCampo() throws Exception {
    Lote lote = lote(mario, "60.00", "CRH-2026-0104");
    Tueste tueste = tueste(lote, NivelTueste.CLARO, "40.00", "34.00", LocalDate.of(2026, 3, 1));

    actualizar(mario, tueste.getId(), Map.of("pesoTostadoKg", 40.01))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.mensaje").value("Revisá los datos ingresados."))
        .andExpect(
            jsonPath("$.errores.pesoTostadoKg")
                .value("El peso tostado no puede ser mayor al peso verde."));

    assertThat(tuesteRepository.findById(tueste.getId()).orElseThrow().getPesoTostadoKg())
        .isEqualByComparingTo("34.00");
  }

  @Test
  void pesoVerdeMayorALosKgDisponiblesDelLoteDa400() throws Exception {
    Lote lote = lote(mario, "100.00", "CRH-2026-0105");
    tueste(lote, NivelTueste.OSCURO, "50.00", "40.00", LocalDate.of(2026, 3, 1));
    Tueste segundo = tueste(lote, NivelTueste.CLARO, "40.00", "34.00", LocalDate.of(2026, 3, 8));

    actualizar(mario, segundo.getId(), Map.of("pesoVerdeKg", 50.01))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.mensaje").value("Revisá los datos ingresados."))
        .andExpect(
            jsonPath("$.errores.pesoVerdeKg")
                .value("El peso verde supera los kg disponibles del lote."));

    assertThat(tuesteRepository.findById(segundo.getId()).orElseThrow().getPesoVerdeKg())
        .isEqualByComparingTo("40.00");
  }

  @Test
  void pesoTostadoMenorALosKgPedidosDa409() throws Exception {
    Lote lote = lote(mario, "60.00", "CRH-2026-0106");
    Tueste tueste = tueste(lote, NivelTueste.MEDIO, "50.00", "43.00", LocalDate.of(2026, 3, 1));
    when(kgPedidosTueste.total(tueste.getId())).thenReturn(new BigDecimal("40.00"));

    actualizar(mario, tueste.getId(), Map.of("pesoTostadoKg", 39.99))
        .andExpect(status().isConflict())
        .andExpect(
            jsonPath("$.mensaje")
                .value("El peso tostado no puede ser menor a los kg ya pedidos por cafeterías."));

    assertThat(tuesteRepository.findById(tueste.getId()).orElseThrow().getPesoTostadoKg())
        .isEqualByComparingTo("43.00");
  }

  @Test
  void unTuesteInexistenteDa404() throws Exception {
    actualizar(mario, UUID.randomUUID(), Map.of("notas", "No existe"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.mensaje").value("No encontramos ese tueste."));
  }

  @Test
  void otroRolNoPuedeEditarYSinSesionDa401() throws Exception {
    Lote lote = lote(mario, "60.00", "CRH-2026-0107");
    Tueste tueste = tueste(lote, NivelTueste.CLARO, "40.00", "34.00", LocalDate.of(2026, 3, 1));
    Cuenta andres = cuenta("Andrés Solano", "andres@ejemplo.cr", Rol.CAFETALERO);

    actualizar(andres, tueste.getId(), Map.of("notas", "Sin permiso"))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(
            patch("/api/tuestes/{id}", tueste.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"notas\":\"Sin sesión\"}"))
        .andExpect(status().isUnauthorized());
  }
}
