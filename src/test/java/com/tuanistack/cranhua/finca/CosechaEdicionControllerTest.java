package com.tuanistack.cranhua.finca;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuanistack.cranhua.BasePostgis;
import com.tuanistack.cranhua.auth.CookieSesion;
import com.tuanistack.cranhua.cuenta.Cuenta;
import com.tuanistack.cranhua.cuenta.CuentaRepository;
import com.tuanistack.cranhua.cuenta.Rol;
import com.tuanistack.cranhua.lote.Lote;
import com.tuanistack.cranhua.lote.LoteRepository;
import com.tuanistack.cranhua.solicitud.EstadoSolicitud;
import com.tuanistack.cranhua.solicitud.Solicitud;
import com.tuanistack.cranhua.solicitud.SolicitudRepository;
import jakarta.servlet.http.Cookie;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
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
class CosechaEdicionControllerTest {

  private static final String BLOQUEADO = "No se puede cambiar porque ya está en códigos impresos.";

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private CookieSesion cookieSesion;

  @Autowired private CuentaRepository cuentaRepository;

  @Autowired private FincaRepository fincaRepository;

  @Autowired private CosechaRepository cosechaRepository;

  @Autowired private SolicitudRepository solicitudRepository;

  @Autowired private LoteRepository loteRepository;

  private Cuenta andres;

  private Cuenta mario;

  private Finca finca;

  @BeforeEach
  void crearDatos() {
    andres = cuenta("Andrés Solano", "andres@ejemplo.cr", Rol.CAFETALERO);
    mario = cuenta("Mario Brenes", "mario@ejemplo.cr", Rol.TOSTADOR);
    Finca nueva = FincaRepositoryTest.nuevaFinca();
    nueva.agregarDueno(andres);
    finca = fincaRepository.saveAndFlush(nueva);
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

  private Cookie sesion(Cuenta cuenta) {
    return new Cookie(CookieSesion.NOMBRE, cookieSesion.crear(cuenta).getValue());
  }

  private Cosecha cosechaPublicada() {
    Cosecha cosecha =
        new Cosecha(
            finca,
            "Caturra",
            1450,
            LocalDate.of(2026, 1, 5),
            LocalDate.of(2026, 2, 28),
            new BigDecimal("1200.00"),
            new BigDecimal("9.75"),
            List.of(new ProcesoReparto(Proceso.LAVADO, 100)));
    cosecha.setPesoTrasBeneficiadoKg(new BigDecimal("1000.00"));
    cosecha.setNotasSabor("Chocolate");
    cosecha.registrarPesoTrasSeleccion(new BigDecimal("800.00"));
    cosecha.publicar();
    return cosechaRepository.saveAndFlush(cosecha);
  }

  private Solicitud solicitudPendiente(Cosecha cosecha) {
    return solicitudRepository.saveAndFlush(new Solicitud(cosecha, mario, new BigDecimal("50.00")));
  }

  private Solicitud conLote(Cosecha cosecha) {
    Solicitud solicitud = new Solicitud(cosecha, mario, new BigDecimal("100.00"));
    solicitud.aprobar();
    solicitud = solicitudRepository.saveAndFlush(solicitud);
    cosecha.descontarDisponible(new BigDecimal("100.00"));
    cosechaRepository.saveAndFlush(cosecha);
    loteRepository.saveAndFlush(new Lote(solicitud, "CRH-2026-0001"));
    return solicitud;
  }

  // El formulario completo con los mismos valores que tiene cosechaPublicada()
  private Map<String, Object> formulario() {
    Map<String, Object> cosecha = new LinkedHashMap<>();
    cosecha.put("variedad", "Caturra");
    cosecha.put("alturaMsnm", 1450);
    cosecha.put("fechaInicio", "2026-01-05");
    cosecha.put("fechaFin", "2026-02-28");
    cosecha.put("procesos", List.of(Map.of("proceso", "Lavado", "porcentaje", 100)));
    cosecha.put("cantidadCosechadaKg", 1200);
    cosecha.put("pesoTrasBeneficiadoKg", 1000);
    cosecha.put("pesoTrasSeleccionKg", 800);
    cosecha.put("notasSabor", "Chocolate");
    cosecha.put("precioKg", 9.75);
    return cosecha;
  }

  private ResultActions editar(Cuenta cuenta, UUID cosechaId, Map<String, Object> cosecha)
      throws Exception {
    return mockMvc.perform(
        patch("/api/cosechas/{id}", cosechaId)
            .cookie(sesion(cuenta))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(cosecha)));
  }

  private ResultActions eliminar(Cuenta cuenta, UUID cosechaId) throws Exception {
    return mockMvc.perform(delete("/api/cosechas/{id}", cosechaId).cookie(sesion(cuenta)));
  }

  private EstadoSolicitud estadoDe(Solicitud solicitud) {
    return solicitudRepository.findById(solicitud.getId()).orElseThrow().getEstado();
  }

  @Test
  void editarSinLotesGuardaTodosLosCambios() throws Exception {
    Cosecha cosecha = cosechaPublicada();
    Map<String, Object> cambios = formulario();
    cambios.put("variedad", "Geisha");
    cambios.put("alturaMsnm", 1600);
    cambios.put(
        "procesos",
        List.of(
            Map.of("proceso", "Lavado", "porcentaje", 60),
            Map.of("proceso", "Natural", "porcentaje", 40)));
    cambios.put("pesoTrasSeleccionKg", 750);
    cambios.put("caracolilloKg", 30);
    cambios.put("puntajeSca", 87.5);
    cambios.put("precioKg", 12.5);

    editar(andres, cosecha.getId(), cambios)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(cosecha.getId().toString()))
        .andExpect(jsonPath("$.fincaNombre").value("Finca El Tucán"))
        .andExpect(jsonPath("$.variedad").value("Geisha"))
        .andExpect(jsonPath("$.alturaMsnm").value(1600))
        .andExpect(jsonPath("$.procesos", hasSize(2)))
        .andExpect(jsonPath("$.caracolilloKg").value(30.0))
        .andExpect(jsonPath("$.kgDisponibles").value(750.0))
        .andExpect(jsonPath("$.precioKg").value(12.5))
        .andExpect(jsonPath("$.estado").value("PUBLICADA"));

    Cosecha guardada = cosechaRepository.findById(cosecha.getId()).orElseThrow();
    assertThat(guardada.getVariedad()).isEqualTo("Geisha");
    assertThat(guardada.getPesoTrasSeleccionKg()).isEqualByComparingTo("750.00");
    assertThat(guardada.getKgDisponibles()).isEqualByComparingTo("750.00");
    assertThat(guardada.getPrecioKg()).isEqualByComparingTo("12.50");
  }

  @Test
  void editarValidaIgualQueRegistrar() throws Exception {
    UUID id = cosechaPublicada().getId();

    Map<String, Object> cosecha = formulario();
    cosecha.put("procesos", List.of(Map.of("proceso", "Lavado", "porcentaje", 90)));
    editar(andres, id, cosecha)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.mensaje").value("Revisá los datos ingresados."))
        .andExpect(
            jsonPath("$.errores.procesos")
                .value("Los porcentajes de los procesos tienen que sumar 100."));

    cosecha.put(
        "procesos",
        List.of(
            Map.of("proceso", "Lavado", "porcentaje", 50),
            Map.of("proceso", "Lavado", "porcentaje", 50)));
    editar(andres, id, cosecha)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errores.procesos").value("No repitás un proceso."));

    cosecha = formulario();
    cosecha.put("pesoTrasSeleccionKg", 1001);
    editar(andres, id, cosecha)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errores.pesoTrasSeleccionKg").exists());

    cosecha = formulario();
    cosecha.put("caracolilloKg", 801);
    editar(andres, id, cosecha)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errores.caracolilloKg").exists());

    cosecha = formulario();
    cosecha.put("precioKg", 0);
    editar(andres, id, cosecha)
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.errores.precioKg").value("El precio por kg tiene que ser mayor a 0."));

    cosecha = formulario();
    cosecha.put("fechaFin", "2026-01-04");
    editar(andres, id, cosecha)
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.errores.fechaFin")
                .value("La fecha de fin no puede ser antes de la de inicio."));

    Cosecha guardada = cosechaRepository.findById(id).orElseThrow();
    assertThat(guardada.getPrecioKg()).isEqualByComparingTo("9.75");
    assertThat(guardada.getPesoTrasSeleccionKg()).isEqualByComparingTo("800.00");
  }

  @Test
  void quitarElPesoTrasSeleccionAUnaPublicadaLaDejaEnBorradorYRechazaPendientes() throws Exception {
    Cosecha cosecha = cosechaPublicada();
    Solicitud pendiente = solicitudPendiente(cosecha);
    Map<String, Object> cambios = formulario();
    cambios.remove("pesoTrasSeleccionKg");

    editar(andres, cosecha.getId(), cambios)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estado").value("BORRADOR"))
        .andExpect(jsonPath("$.pesoTrasSeleccionKg").value(nullValue()))
        .andExpect(jsonPath("$.kgDisponibles").value(nullValue()));

    assertThat(estadoDe(pendiente)).isEqualTo(EstadoSolicitud.RECHAZADA);
  }

  @Test
  void conLotesSePuedenCambiarPrecioYNotasSinTocarLasSolicitudes() throws Exception {
    Cosecha cosecha = cosechaPublicada();
    Solicitud comprada = conLote(cosecha);
    Map<String, Object> cambios = formulario();
    cambios.put("precioKg", 15);
    cambios.put("notasSabor", "Chocolate y naranja");

    editar(andres, cosecha.getId(), cambios)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.precioKg").value(15.0))
        .andExpect(jsonPath("$.notasSabor").value("Chocolate y naranja"))
        .andExpect(jsonPath("$.kgDisponibles").value(700.0));

    Solicitud leida = solicitudRepository.findById(comprada.getId()).orElseThrow();
    assertThat(leida.getPrecioKg()).isEqualByComparingTo("9.75");
    assertThat(leida.getTotalEstimado()).isEqualByComparingTo("975.00");
  }

  @Test
  void conLotesCambiarOtrosCamposDa409PorCampo() throws Exception {
    Cosecha cosecha = cosechaPublicada();
    conLote(cosecha);
    Map<String, Object> cambios = formulario();
    cambios.put("variedad", "Geisha");
    cambios.put(
        "procesos",
        List.of(
            Map.of("proceso", "Lavado", "porcentaje", 50),
            Map.of("proceso", "Natural", "porcentaje", 50)));
    cambios.put("pesoTrasBeneficiadoKg", 990);
    cambios.put("precioKg", 15);

    editar(andres, cosecha.getId(), cambios)
        .andExpect(status().isConflict())
        .andExpect(
            jsonPath("$.mensaje")
                .value(
                    "Esta cosecha ya tiene lotes comprados. Solo podés cambiar el precio y las"
                        + " notas de sabor."))
        .andExpect(jsonPath("$.errores.variedad").value(BLOQUEADO))
        .andExpect(jsonPath("$.errores.procesos").value(BLOQUEADO))
        .andExpect(jsonPath("$.errores.pesoTrasBeneficiadoKg").value(BLOQUEADO))
        .andExpect(jsonPath("$.errores.precioKg").doesNotExist())
        .andExpect(jsonPath("$.errores.alturaMsnm").doesNotExist());

    // Mismo valor escrito distinto no cuenta como cambio
    Map<String, Object> mismoPeso = formulario();
    mismoPeso.put("pesoTrasSeleccionKg", "800.0");
    editar(andres, cosecha.getId(), mismoPeso).andExpect(status().isOk());

    Cosecha guardada = cosechaRepository.findById(cosecha.getId()).orElseThrow();
    assertThat(guardada.getVariedad()).isEqualTo("Caturra");
    assertThat(guardada.getPrecioKg()).isEqualByComparingTo("9.75");
  }

  @Test
  void eliminarSinLotesDa204RechazaPendientesYLaOcultaEnTodosLados() throws Exception {
    Cosecha cosecha = cosechaPublicada();
    Solicitud pendiente = solicitudPendiente(cosecha);
    mockMvc
        .perform(get("/api/cosechas/disponibles").cookie(sesion(mario)))
        .andExpect(jsonPath("$", hasSize(1)));

    eliminar(andres, cosecha.getId()).andExpect(status().isNoContent());

    assertThat(estadoDe(pendiente)).isEqualTo(EstadoSolicitud.RECHAZADA);
    assertThat(cosechaRepository.findById(cosecha.getId()).orElseThrow().isEliminada()).isTrue();
    mockMvc
        .perform(get("/api/fincas/{id}/cosechas", finca.getId()).cookie(sesion(andres)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(0)));
    mockMvc
        .perform(get("/api/fincas").cookie(sesion(andres)))
        .andExpect(jsonPath("$[0].cosechasActivas").value(0));
    mockMvc
        .perform(get("/api/cosechas/disponibles").cookie(sesion(mario)))
        .andExpect(jsonPath("$", hasSize(0)));
    mockMvc
        .perform(get("/api/cosechas/disponibles/{id}", cosecha.getId()).cookie(sesion(mario)))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(get("/api/cafetalero/dashboard").cookie(sesion(andres)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.indicadores.cosechasActivas").value(0))
        .andExpect(jsonPath("$.cosechasRecientes", hasSize(0)));
    mockMvc
        .perform(get("/api/cafetalero/historial").cookie(sesion(andres)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.indicadores.cosechasRegistradas").value(0))
        .andExpect(jsonPath("$.cosechas.contenido", hasSize(0)));
  }

  @Test
  void eliminarConLotesDa409() throws Exception {
    Cosecha cosecha = cosechaPublicada();
    conLote(cosecha);

    eliminar(andres, cosecha.getId())
        .andExpect(status().isConflict())
        .andExpect(
            jsonPath("$.mensaje")
                .value(
                    "No podés eliminar esta cosecha porque ya tiene lotes comprados con código"
                        + " de trazabilidad. Si no querés recibir más solicitudes, retirala."));
    assertThat(cosechaRepository.findById(cosecha.getId()).orElseThrow().isEliminada()).isFalse();
  }

  @Test
  void unaCosechaEliminadaOInexistenteDa404() throws Exception {
    Cosecha cosecha = cosechaPublicada();
    eliminar(andres, cosecha.getId()).andExpect(status().isNoContent());

    for (UUID id : List.of(cosecha.getId(), UUID.randomUUID())) {
      mockMvc
          .perform(get("/api/cosechas/{id}", id).cookie(sesion(andres)))
          .andExpect(status().isNotFound());
      editar(andres, id, formulario())
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.mensaje").value("No encontramos esa cosecha."));
      eliminar(andres, id)
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.mensaje").value("No encontramos esa cosecha."));
    }
  }

  @Test
  void editarOEliminarCosechaAjenaDa403() throws Exception {
    Cuenta lucia = cuenta("Lucía Mora", "lucia@ejemplo.cr", Rol.CAFETALERO);
    Cosecha cosecha = cosechaPublicada();

    editar(lucia, cosecha.getId(), formulario())
        .andExpect(status().isForbidden())
        .andExpect(
            jsonPath("$.mensaje").value("Solo un dueño de la finca puede editar esta cosecha."));
    eliminar(lucia, cosecha.getId())
        .andExpect(status().isForbidden())
        .andExpect(
            jsonPath("$.mensaje").value("Solo un dueño de la finca puede eliminar esta cosecha."));
    assertThat(cosechaRepository.findById(cosecha.getId()).orElseThrow().isEliminada()).isFalse();
  }

  @Test
  void editarYEliminarPidenSesionYRolCafetalero() throws Exception {
    UUID id = cosechaPublicada().getId();

    mockMvc
        .perform(
            patch("/api/cosechas/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(formulario())))
        .andExpect(status().isUnauthorized());
    mockMvc.perform(delete("/api/cosechas/{id}", id)).andExpect(status().isUnauthorized());
    editar(mario, id, formulario()).andExpect(status().isForbidden());
    eliminar(mario, id).andExpect(status().isForbidden());
    assertThat(cosechaRepository.findById(id).orElseThrow().isEliminada()).isFalse();
  }

  @Test
  void retirarUnaCosechaRechazaSusSolicitudesPendientes() throws Exception {
    Cosecha cosecha = cosechaPublicada();
    Solicitud pendiente = solicitudPendiente(cosecha);

    mockMvc
        .perform(
            patch("/api/cosechas/{id}/estado", cosecha.getId())
                .cookie(sesion(andres))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("estado", "BORRADOR"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estado").value("BORRADOR"));

    assertThat(estadoDe(pendiente)).isEqualTo(EstadoSolicitud.RECHAZADA);
  }

  @Test
  void eliminarLaFincaRechazaLasPendientesSoloCuandoSaleElUltimoDueno() throws Exception {
    Cuenta lucia = cuenta("Lucía Mora", "lucia@ejemplo.cr", Rol.CAFETALERO);
    finca.agregarDueno(lucia);
    finca = fincaRepository.saveAndFlush(finca);
    Solicitud pendiente = solicitudPendiente(cosechaPublicada());

    mockMvc
        .perform(delete("/api/fincas/{id}", finca.getId()).cookie(sesion(lucia)))
        .andExpect(status().isNoContent());
    assertThat(estadoDe(pendiente)).isEqualTo(EstadoSolicitud.PENDIENTE);

    mockMvc
        .perform(delete("/api/fincas/{id}", finca.getId()).cookie(sesion(andres)))
        .andExpect(status().isNoContent());
    assertThat(estadoDe(pendiente)).isEqualTo(EstadoSolicitud.RECHAZADA);
  }
}
