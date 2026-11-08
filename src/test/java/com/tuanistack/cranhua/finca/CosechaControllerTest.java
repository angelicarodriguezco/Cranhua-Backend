package com.tuanistack.cranhua.finca;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tuanistack.cranhua.BasePostgis;
import com.tuanistack.cranhua.auth.CookieSesion;
import com.tuanistack.cranhua.cuenta.Cuenta;
import com.tuanistack.cranhua.cuenta.CuentaRepository;
import com.tuanistack.cranhua.cuenta.Rol;
import jakarta.servlet.http.Cookie;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
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
class CosechaControllerTest {

  @Autowired private MockMvc mockMvc;

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
        .andExpect(jsonPath("$.estado").value("DISPONIBLE"));
  }

  @Test
  void losCamposOpcionalesVienenEnNull() throws Exception {
    Cosecha cosecha = cosecha(finca);

    mockMvc
        .perform(get("/api/cosechas/{id}", cosecha.getId()).cookie(sesion(andres)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estado").value("EN_BENEFICIADO"))
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
}
