package com.tuanistack.cranhua.finca;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
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
class CosechaDisponibleControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private CookieSesion cookieSesion;

  @Autowired private CuentaRepository cuentaRepository;

  @Autowired private FincaRepository fincaRepository;

  @Autowired private CosechaRepository cosechaRepository;

  private Cuenta andres;

  private Cuenta mario;

  private Finca finca;

  @BeforeEach
  void crearDatos() {
    andres = cuenta("Andrés Solano", "andres@ejemplo.cr", Rol.CAFETALERO);
    andres.setTelefono("8888-1234");
    cuentaRepository.saveAndFlush(andres);
    mario = cuenta("Mario Brenes", "mario@ejemplo.cr", Rol.TOSTADOR);
    finca = fincaDe(andres, ZonaCafetalera.TARRAZU);
  }

  @AfterEach
  void limpiar() {
    cosechaRepository.deleteAll();
    fincaRepository.deleteAll();
    cuentaRepository.deleteAll();
  }

  private Cuenta cuenta(String nombre, String correo, Rol... roles) {
    return cuentaRepository.saveAndFlush(new Cuenta(nombre, correo, "hash", Set.of(roles)));
  }

  private Finca fincaDe(Cuenta dueno, ZonaCafetalera zona) {
    Finca nueva = FincaRepositoryTest.nuevaFinca();
    nueva.setZonaCafetalera(zona);
    nueva.agregarDueno(dueno);
    return fincaRepository.saveAndFlush(nueva);
  }

  private Cookie sesion(Cuenta cuenta) {
    return new Cookie(CookieSesion.NOMBRE, cookieSesion.crear(cuenta).getValue());
  }

  private Cosecha publicada(Finca finca, String notas, ProcesoReparto... procesos) {
    Cosecha cosecha =
        new Cosecha(
            finca,
            "Caturra",
            1450,
            LocalDate.of(2026, 1, 5),
            LocalDate.of(2026, 2, 28),
            new BigDecimal("1200.00"),
            new BigDecimal("5800.00"),
            List.of(procesos));
    cosecha.setNotasSabor(notas);
    cosecha.registrarPesoTrasSeleccion(new BigDecimal("320.00"));
    cosecha.publicar();
    return cosechaRepository.saveAndFlush(cosecha);
  }

  private Cosecha publicada(Finca finca) {
    return publicada(finca, "Panela, Naranja", new ProcesoReparto(Proceso.LAVADO, 100));
  }

  @Test
  void listaSoloPublicadasConKgDisponibles() throws Exception {
    Cosecha visible =
        publicada(
            finca,
            "Panela, Naranja ,Jazmín",
            new ProcesoReparto(Proceso.HONEY, 70),
            new ProcesoReparto(Proceso.LAVADO, 30));
    Cosecha borrador = publicada(finca);
    borrador.retirar();
    cosechaRepository.saveAndFlush(borrador);
    Cosecha agotada = publicada(finca);
    agotada.descontarDisponible(new BigDecimal("320.00"));
    cosechaRepository.saveAndFlush(agotada);

    mockMvc
        .perform(get("/api/cosechas/disponibles").cookie(sesion(mario)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].id").value(contains(visible.getId().toString())))
        .andExpect(jsonPath("$[0].fincaId").value(finca.getId().toString()))
        .andExpect(jsonPath("$[0].finca").value("Finca El Tucán"))
        .andExpect(jsonPath("$[0].zona").value("Tarrazú"))
        .andExpect(jsonPath("$[0].canton").value("Tarrazú"))
        .andExpect(jsonPath("$[0].provincia").value("San José"))
        .andExpect(jsonPath("$[0].variedad").value("Caturra"))
        .andExpect(jsonPath("$[0].proceso").value("Honey"))
        .andExpect(jsonPath("$[0].notas").value(contains("Panela", "Naranja", "Jazmín")))
        .andExpect(jsonPath("$[0].precioKg").value(5800.0));
  }

  @Test
  void noMuestraLasCosechasPropiasNiDeFincasEliminadas() throws Exception {
    Cuenta ana = cuenta("Ana Rojas", "ana@ejemplo.cr", Rol.CAFETALERO, Rol.TOSTADOR);
    Finca deAna = fincaDe(ana, ZonaCafetalera.OROSI);
    Cosecha propia = publicada(deAna);
    Cosecha ajena = publicada(finca);
    Finca eliminada = fincaDe(andres, ZonaCafetalera.BRUNCA);
    publicada(eliminada);
    eliminada.eliminar();
    fincaRepository.saveAndFlush(eliminada);

    mockMvc
        .perform(get("/api/cosechas/disponibles").cookie(sesion(ana)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].id").value(contains(ajena.getId().toString())));
    mockMvc
        .perform(get("/api/cosechas/disponibles").cookie(sesion(mario)))
        .andExpect(
            jsonPath("$[*].id")
                .value(containsInAnyOrder(ajena.getId().toString(), propia.getId().toString())));
  }

  @Test
  void filtraPorZonaFloralesYHoney() throws Exception {
    Cuenta lucia = cuenta("Lucía Mora", "lucia@ejemplo.cr", Rol.CAFETALERO);
    Finca orosi = fincaDe(lucia, ZonaCafetalera.OROSI);
    Cosecha floralHoney =
        publicada(finca, "Azahar, miel", new ProcesoReparto(Proceso.RED_HONEY, 100));
    Cosecha floralLavado =
        publicada(finca, "Flor de café", new ProcesoReparto(Proceso.LAVADO, 100));
    Cosecha deOrosi = publicada(orosi, "Cacao", new ProcesoReparto(Proceso.HONEY, 100));

    mockMvc
        .perform(get("/api/cosechas/disponibles").param("zona", "Orosi").cookie(sesion(mario)))
        .andExpect(jsonPath("$[*].id").value(contains(deOrosi.getId().toString())));
    mockMvc
        .perform(get("/api/cosechas/disponibles").param("florales", "true").cookie(sesion(mario)))
        .andExpect(
            jsonPath("$[*].id")
                .value(
                    containsInAnyOrder(
                        floralHoney.getId().toString(), floralLavado.getId().toString())));
    mockMvc
        .perform(
            get("/api/cosechas/disponibles")
                .param("zona", "Tarrazú")
                .param("florales", "true")
                .param("honey", "true")
                .cookie(sesion(mario)))
        .andExpect(jsonPath("$[*].id").value(contains(floralHoney.getId().toString())));
    mockMvc
        .perform(get("/api/cosechas/disponibles").param("zona", "Marte").cookie(sesion(mario)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.mensaje").value("Esa zona cafetalera no existe."));
  }

  @Test
  void elDetalleTraeAltitudKgNotasYContactoDelProductor() throws Exception {
    Cosecha cosecha = publicada(finca);
    cosecha.setPuntajeSca(new BigDecimal("86.50"));
    cosechaRepository.saveAndFlush(cosecha);

    mockMvc
        .perform(get("/api/cosechas/disponibles/{id}", cosecha.getId()).cookie(sesion(mario)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(cosecha.getId().toString()))
        .andExpect(jsonPath("$.finca").value("Finca El Tucán"))
        .andExpect(jsonPath("$.alturaMsnm").value(1450))
        .andExpect(jsonPath("$.kgDisponibles").value(320.0))
        .andExpect(jsonPath("$.notas").value(contains("Panela", "Naranja")))
        .andExpect(jsonPath("$.procesos[0].proceso").value("Lavado"))
        .andExpect(jsonPath("$.puntajeSca").value(86.5))
        .andExpect(jsonPath("$.productores[0].id").value(andres.getId().toString()))
        .andExpect(jsonPath("$.productores[0].nombreCompleto").value("Andrés Solano"))
        .andExpect(jsonPath("$.productores[0].correo").value("andres@ejemplo.cr"))
        .andExpect(jsonPath("$.productores[0].telefono").value("8888-1234"));
  }

  @Test
  void elDetalleDeUnaCosechaPropiaNoDisponibleOEliminadaDa404() throws Exception {
    Cuenta ana = cuenta("Ana Rojas", "ana@ejemplo.cr", Rol.CAFETALERO, Rol.TOSTADOR);
    Cosecha propia = publicada(fincaDe(ana, ZonaCafetalera.OROSI));
    Cosecha borrador = publicada(finca);
    borrador.retirar();
    cosechaRepository.saveAndFlush(borrador);
    Finca eliminada = fincaDe(andres, ZonaCafetalera.BRUNCA);
    Cosecha deEliminada = publicada(eliminada);
    eliminada.eliminar();
    fincaRepository.saveAndFlush(eliminada);

    for (UUID id :
        List.of(propia.getId(), borrador.getId(), deEliminada.getId(), UUID.randomUUID())) {
      mockMvc
          .perform(get("/api/cosechas/disponibles/{id}", id).cookie(sesion(ana)))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.mensaje").value("No encontramos esa cosecha."));
    }
  }

  @Test
  void pideSesionYRolTostador() throws Exception {
    Cosecha cosecha = publicada(finca);
    Cuenta lucia = cuenta("Lucía Mora", "lucia@ejemplo.cr", Rol.CAFETALERO);

    mockMvc.perform(get("/api/cosechas/disponibles")).andExpect(status().isUnauthorized());
    mockMvc
        .perform(get("/api/cosechas/disponibles").cookie(sesion(lucia)))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(get("/api/cosechas/disponibles/{id}", cosecha.getId()).cookie(sesion(lucia)))
        .andExpect(status().isForbidden());
  }
}
