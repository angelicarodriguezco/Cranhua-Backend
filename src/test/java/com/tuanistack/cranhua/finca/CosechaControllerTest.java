package com.tuanistack.cranhua.finca;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
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
import com.tuanistack.cranhua.cuenta.Cuenta;
import com.tuanistack.cranhua.cuenta.CuentaRepository;
import com.tuanistack.cranhua.cuenta.Rol;
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
class CosechaControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private CookieSesion cookieSesion;

  @Autowired private CuentaRepository cuentaRepository;

  @Autowired private FincaRepository fincaRepository;

  @Autowired private CosechaRepository cosechaRepository;

  private Cuenta andres;

  private Finca finca;

  @BeforeEach
  void crearDatos() {
    andres = cuenta("Andrés Solano", "andres@ejemplo.cr", Rol.CAFETALERO);
    finca = fincaDe(andres);
  }

  @AfterEach
  void limpiar() {
    cosechaRepository.deleteAll();
    fincaRepository.deleteAll();
    cuentaRepository.deleteAll();
  }

  private Cuenta cuenta(String nombre, String correo, Rol rol) {
    return cuentaRepository.saveAndFlush(new Cuenta(nombre, correo, "hash", Set.of(rol)));
  }

  private Finca fincaDe(Cuenta dueno) {
    Finca nueva = FincaRepositoryTest.nuevaFinca();
    nueva.agregarDueno(dueno);
    return fincaRepository.saveAndFlush(nueva);
  }

  private Cookie sesion(Cuenta cuenta) {
    return new Cookie(CookieSesion.NOMBRE, cookieSesion.crear(cuenta).getValue());
  }

  private Cosecha cosecha(Finca finca) {
    return cosechaRepository.saveAndFlush(
        new Cosecha(
            finca,
            "Caturra",
            1450,
            LocalDate.of(2026, 1, 5),
            LocalDate.of(2026, 2, 28),
            new BigDecimal("1200.00"),
            new BigDecimal("9.75"),
            List.of(new ProcesoReparto(Proceso.LAVADO, 100))));
  }

  @Test
  void obtenerUnaCosechaPropiaConElNombreDeLaFinca() throws Exception {
    Cosecha cosecha = cosecha(finca);
    cosecha.setNotasSabor("Chocolate y mandarina");
    cosecha.setPuntajeSca(new BigDecimal("86.50"));
    cosecha.registrarPesoTrasSeleccion(new BigDecimal("800.00"));
    cosechaRepository.saveAndFlush(cosecha);

    mockMvc
        .perform(get("/api/cosechas/{id}", cosecha.getId()).cookie(sesion(andres)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(cosecha.getId().toString()))
        .andExpect(jsonPath("$.fincaId").value(finca.getId().toString()))
        .andExpect(jsonPath("$.fincaNombre").value("Finca El Tucán"))
        .andExpect(jsonPath("$.procesos[0].proceso").value("Lavado"))
        .andExpect(jsonPath("$.procesos[0].porcentaje").value(100))
        .andExpect(jsonPath("$.notasSabor").value("Chocolate y mandarina"))
        .andExpect(jsonPath("$.puntajeSca").value(86.5))
        .andExpect(jsonPath("$.kgDisponibles").value(800.0))
        .andExpect(jsonPath("$.estado").value("BORRADOR"));
  }

  @Test
  void losCamposOpcionalesVienenEnNull() throws Exception {
    Cosecha cosecha = cosecha(finca);

    mockMvc
        .perform(get("/api/cosechas/{id}", cosecha.getId()).cookie(sesion(andres)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estado").value("BORRADOR"))
        .andExpect(jsonPath("$.pesoTrasBeneficiadoKg").value(nullValue()))
        .andExpect(jsonPath("$.pesoTrasSeleccionKg").value(nullValue()))
        .andExpect(jsonPath("$.caracolilloKg").value(nullValue()))
        .andExpect(jsonPath("$.notasSabor").value(nullValue()))
        .andExpect(jsonPath("$.puntajeSca").value(nullValue()))
        .andExpect(jsonPath("$.kgDisponibles").value(nullValue()))
        .andExpect(jsonPath("$.codigoLote").value(nullValue()));
  }

  @Test
  void unaCosechaDeFincaAjenaDa403() throws Exception {
    Cuenta lucia = cuenta("Lucía Mora", "lucia@ejemplo.cr", Rol.CAFETALERO);
    Cosecha cosecha = cosecha(finca);

    mockMvc
        .perform(get("/api/cosechas/{id}", cosecha.getId()).cookie(sesion(lucia)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.mensaje").value("Esta cosecha no es tuya."));
  }

  @Test
  void unaCosechaInexistenteODeFincaEliminadaDa404() throws Exception {
    Cosecha cosecha = cosecha(finca);
    finca.eliminar();
    fincaRepository.saveAndFlush(finca);

    for (UUID id : List.of(UUID.randomUUID(), cosecha.getId())) {
      mockMvc
          .perform(get("/api/cosechas/{id}", id).cookie(sesion(andres)))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.mensaje").value("No encontramos esa cosecha."));
    }
  }

  private Map<String, Object> nuevaCosecha() {
    Map<String, Object> cosecha = new LinkedHashMap<>();
    cosecha.put("fincaId", finca.getId());
    cosecha.put("variedad", "Geisha");
    cosecha.put("alturaMsnm", 1650);
    cosecha.put("fechaInicio", "2026-01-10");
    cosecha.put("fechaFin", "2026-03-15");
    cosecha.put(
        "procesos",
        List.of(
            Map.of("proceso", "Lavado", "porcentaje", 70),
            Map.of("proceso", "Natural", "porcentaje", 30)));
    cosecha.put("cantidadCosechadaKg", 1000);
    cosecha.put("pesoTrasBeneficiadoKg", 800);
    cosecha.put("pesoTrasSeleccionKg", 720);
    cosecha.put("caracolilloKg", 40);
    cosecha.put("notasSabor", "Jazmín y durazno");
    cosecha.put("puntajeSca", 88.25);
    cosecha.put("precioKg", 12.5);
    return cosecha;
  }

  private ResultActions registrar(Cuenta cuenta, Map<String, Object> cosecha) throws Exception {
    return mockMvc.perform(
        post("/api/cosechas")
            .cookie(sesion(cuenta))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(cosecha)));
  }

  private ResultActions actualizarEstado(Cuenta cuenta, UUID cosechaId, EstadoCosecha estado)
      throws Exception {
    return mockMvc.perform(
        patch("/api/cosechas/{id}/estado", cosechaId)
            .cookie(sesion(cuenta))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of("estado", estado.name()))));
  }

  @Test
  void cafetaleroRegistraUnaCosechaConMermas() throws Exception {
    String respuesta =
        registrar(andres, nuevaCosecha())
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", startsWith("/api/cosechas/")))
            .andExpect(jsonPath("$.fincaId").value(finca.getId().toString()))
            .andExpect(jsonPath("$.fincaNombre").value("Finca El Tucán"))
            .andExpect(jsonPath("$.variedad").value("Geisha"))
            .andExpect(jsonPath("$.procesos[0].proceso").value("Lavado"))
            .andExpect(jsonPath("$.procesos[1].porcentaje").value(30))
            .andExpect(jsonPath("$.mermaBeneficiadoPorcentaje").value(20.0))
            .andExpect(jsonPath("$.mermaSeleccionPorcentaje").value(10.0))
            .andExpect(jsonPath("$.caracolilloKg").value(40.0))
            .andExpect(jsonPath("$.puntajeSca").value(88.25))
            .andExpect(jsonPath("$.kgDisponibles").value(720.0))
            .andExpect(jsonPath("$.estado").value("BORRADOR"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    UUID id = UUID.fromString(objectMapper.readTree(respuesta).get("id").asText());
    assertThat(cosechaRepository.findById(id)).isPresent();
  }

  @Test
  void sinPesosPosterioresQuedaEnBorrador() throws Exception {
    Map<String, Object> cosecha = nuevaCosecha();
    cosecha.remove("pesoTrasBeneficiadoKg");
    cosecha.remove("pesoTrasSeleccionKg");
    cosecha.remove("caracolilloKg");

    registrar(andres, cosecha)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.estado").value("BORRADOR"))
        .andExpect(jsonPath("$.kgDisponibles").value(nullValue()))
        .andExpect(jsonPath("$.mermaBeneficiadoPorcentaje").value(nullValue()))
        .andExpect(jsonPath("$.mermaSeleccionPorcentaje").value(nullValue()));
  }

  @Test
  void porcentajesQueNoSuman100OProcesoRepetidoDa400() throws Exception {
    Map<String, Object> cosecha = nuevaCosecha();
    cosecha.put(
        "procesos",
        List.of(
            Map.of("proceso", "Lavado", "porcentaje", 60),
            Map.of("proceso", "Natural", "porcentaje", 30)));
    registrar(andres, cosecha)
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
    registrar(andres, cosecha)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errores.procesos").value("No repitás un proceso."));
    assertThat(cosechaRepository.count()).isZero();
  }

  @Test
  void pesosMayoresAlPasoAnteriorDan400EnSuCampo() throws Exception {
    Map<String, Object> cosecha = nuevaCosecha();
    cosecha.put("pesoTrasBeneficiadoKg", 1000.01);
    registrar(andres, cosecha)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errores.pesoTrasBeneficiadoKg").exists())
        .andExpect(jsonPath("$.errores.pesoTrasSeleccionKg").doesNotExist());

    cosecha = nuevaCosecha();
    cosecha.put("pesoTrasSeleccionKg", 801);
    registrar(andres, cosecha)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errores.pesoTrasSeleccionKg").exists())
        .andExpect(jsonPath("$.errores.pesoTrasBeneficiadoKg").doesNotExist());

    cosecha = nuevaCosecha();
    cosecha.put("caracolilloKg", 721);
    registrar(andres, cosecha)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errores.caracolilloKg").exists())
        .andExpect(jsonPath("$.errores.pesoTrasSeleccionKg").doesNotExist());
    assertThat(cosechaRepository.count()).isZero();
  }

  @Test
  void precioCeroOAusenteDa400() throws Exception {
    Map<String, Object> cosecha = nuevaCosecha();
    cosecha.put("precioKg", 0);
    registrar(andres, cosecha)
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.errores.precioKg").value("El precio por kg tiene que ser mayor a 0."));

    cosecha.remove("precioKg");
    registrar(andres, cosecha)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errores.precioKg").value("Ingresá el precio por kg."));
    assertThat(cosechaRepository.count()).isZero();
  }

  @Test
  void fechaFinAntesDelInicioDa400() throws Exception {
    Map<String, Object> cosecha = nuevaCosecha();
    cosecha.put("fechaFin", "2026-01-09");

    registrar(andres, cosecha)
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.errores.fechaFin")
                .value("La fecha de fin no puede ser antes de la de inicio."));
  }

  @Test
  void registrarEnFincaAjenaDa403() throws Exception {
    Cuenta lucia = cuenta("Lucía Mora", "lucia@ejemplo.cr", Rol.CAFETALERO);

    registrar(lucia, nuevaCosecha())
        .andExpect(status().isForbidden())
        .andExpect(
            jsonPath("$.mensaje")
                .value("Solo un dueño de la finca puede registrar cosechas en ella."));
    assertThat(cosechaRepository.count()).isZero();
  }

  @Test
  void registrarEnFincaEliminadaOInexistenteDa404() throws Exception {
    Map<String, Object> cosecha = nuevaCosecha();
    cosecha.put("fincaId", UUID.randomUUID());
    registrar(andres, cosecha)
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.mensaje").value("No encontramos esa finca."));

    finca.eliminar();
    fincaRepository.saveAndFlush(finca);
    registrar(andres, nuevaCosecha())
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.mensaje").value("No encontramos esa finca."));
    assertThat(cosechaRepository.count()).isZero();
  }

  @Test
  void registrarPideSesionYRolCafetalero() throws Exception {
    Cuenta tostador = cuenta("Mario Brenes", "mario@ejemplo.cr", Rol.TOSTADOR);

    mockMvc
        .perform(
            post("/api/cosechas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nuevaCosecha())))
        .andExpect(status().isUnauthorized());
    registrar(tostador, nuevaCosecha()).andExpect(status().isForbidden());
    assertThat(cosechaRepository.count()).isZero();
  }

  @Test
  void pideSesionYRolCafetalero() throws Exception {
    Cosecha cosecha = cosecha(finca);
    Cuenta tostador = cuenta("Mario Brenes", "mario@ejemplo.cr", Rol.TOSTADOR);

    mockMvc
        .perform(get("/api/cosechas/{id}", cosecha.getId()))
        .andExpect(status().isUnauthorized());
    mockMvc
        .perform(get("/api/cosechas/{id}", cosecha.getId()).cookie(sesion(tostador)))
        .andExpect(status().isForbidden());
  }

  @Test
  void elDuenioPublicaUnaCosechaEnBorrador() throws Exception {
    Cosecha cosecha = cosecha(finca);
    cosecha.registrarPesoTrasSeleccion(new BigDecimal("800.00"));
    cosechaRepository.saveAndFlush(cosecha);

    actualizarEstado(andres, cosecha.getId(), EstadoCosecha.PUBLICADA)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(cosecha.getId().toString()))
        .andExpect(jsonPath("$.fincaId").value(finca.getId().toString()))
        .andExpect(jsonPath("$.fincaNombre").value("Finca El Tucán"))
        .andExpect(jsonPath("$.estado").value("PUBLICADA"))
        .andExpect(jsonPath("$.kgDisponibles").value(800.0));

    assertThat(cosechaRepository.findById(cosecha.getId()).orElseThrow().getEstado())
        .isEqualTo(EstadoCosecha.PUBLICADA);
  }

  @Test
  void elDuenioRetiraUnaCosechaPublicada() throws Exception {
    Cosecha cosecha = cosecha(finca);
    cosecha.registrarPesoTrasSeleccion(new BigDecimal("800.00"));
    cosecha.publicar();
    cosechaRepository.saveAndFlush(cosecha);

    actualizarEstado(andres, cosecha.getId(), EstadoCosecha.BORRADOR)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estado").value("BORRADOR"))
        .andExpect(jsonPath("$.kgDisponibles").value(800.0));

    assertThat(cosechaRepository.findById(cosecha.getId()).orElseThrow().getEstado())
        .isEqualTo(EstadoCosecha.BORRADOR);
  }

  @Test
  void publicarORetirarEnFincaAjenaDa403() throws Exception {
    Cuenta lucia = cuenta("Lucía Mora", "lucia@ejemplo.cr", Rol.CAFETALERO);
    Cosecha cosecha = cosecha(finca);

    actualizarEstado(lucia, cosecha.getId(), EstadoCosecha.PUBLICADA)
        .andExpect(status().isForbidden())
        .andExpect(
            jsonPath("$.mensaje")
                .value("Solo un dueño de la finca puede publicar o retirar esta cosecha."));
  }

  @Test
  void publicarPideSesionYRolCafetalero() throws Exception {
    Cosecha cosecha = cosecha(finca);
    Cuenta tostador = cuenta("Mario Brenes", "mario@ejemplo.cr", Rol.TOSTADOR);

    mockMvc
        .perform(
            patch("/api/cosechas/{id}/estado", cosecha.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("estado", "PUBLICADA"))))
        .andExpect(status().isUnauthorized());
    actualizarEstado(tostador, cosecha.getId(), EstadoCosecha.PUBLICADA)
        .andExpect(status().isForbidden());
  }

  @Test
  void transicionInvalidaOAgotadaPorApiDa400() throws Exception {
    Cosecha publicada = cosecha(finca);
    publicada.registrarPesoTrasSeleccion(new BigDecimal("100.00"));
    publicada.publicar();
    cosechaRepository.saveAndFlush(publicada);

    actualizarEstado(andres, publicada.getId(), EstadoCosecha.PUBLICADA)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.mensaje").value("Solo se puede publicar una cosecha en borrador."));

    Cosecha borrador = cosecha(finca);
    actualizarEstado(andres, borrador.getId(), EstadoCosecha.BORRADOR)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.mensaje").value("Solo se puede retirar una cosecha publicada."));

    actualizarEstado(andres, borrador.getId(), EstadoCosecha.AGOTADA)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.mensaje").value("Solo se puede publicar o retirar una cosecha."));
  }

  @Test
  void publicarCosechaInexistenteODeFincaEliminadaDa404() throws Exception {
    Cosecha cosecha = cosecha(finca);
    finca.eliminar();
    fincaRepository.saveAndFlush(finca);

    for (UUID id : List.of(UUID.randomUUID(), cosecha.getId())) {
      actualizarEstado(andres, id, EstadoCosecha.PUBLICADA)
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.mensaje").value("No encontramos esa cosecha."));
    }
  }
}
