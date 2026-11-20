package com.tuanistack.cranhua.lote;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tuanistack.cranhua.BasePostgis;
import com.tuanistack.cranhua.auth.CookieSesion;
import com.tuanistack.cranhua.cuenta.Cuenta;
import com.tuanistack.cranhua.cuenta.CuentaRepository;
import com.tuanistack.cranhua.cuenta.Rol;
import com.tuanistack.cranhua.finca.Cosecha;
import com.tuanistack.cranhua.finca.CosechaRepository;
import com.tuanistack.cranhua.finca.FincaRepository;
import com.tuanistack.cranhua.solicitud.Solicitud;
import com.tuanistack.cranhua.solicitud.SolicitudRepository;
import com.tuanistack.cranhua.solicitud.SolicitudRepositoryTest;
import com.tuanistack.cranhua.tueste.NivelTueste;
import com.tuanistack.cranhua.tueste.Tueste;
import com.tuanistack.cranhua.tueste.TuesteRepository;
import jakarta.servlet.http.Cookie;
import java.math.BigDecimal;
import java.time.LocalDate;
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
class LoteControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private CookieSesion cookieSesion;

  @Autowired private CuentaRepository cuentaRepository;

  @Autowired private FincaRepository fincaRepository;

  @Autowired private CosechaRepository cosechaRepository;

  @Autowired private SolicitudRepository solicitudRepository;

  @Autowired private LoteRepository loteRepository;

  @Autowired private TuesteRepository tuesteRepository;

  private Cuenta mario;

  private Cosecha cosecha;

  @BeforeEach
  void crearDatos() {
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
    Tueste tueste =
        new Tueste(lote, nivel, new BigDecimal(pesoVerdeKg), new BigDecimal(pesoTostadoKg), fecha);
    return tuesteRepository.saveAndFlush(tueste);
  }

  @Test
  void listaLosLotesDelTostadorConIndicadores() throws Exception {
    Lote activo = lote(mario, "60.00", "CRH-2026-0001");
    activo.activar();
    loteRepository.saveAndFlush(activo);
    lote(mario, "40.50", "CRH-2026-0002");
    lote(cuenta("Ana Vargas", "ana@ejemplo.cr", Rol.TOSTADOR), "10.00", "CRH-2026-0003");

    mockMvc
        .perform(get("/api/lotes").cookie(sesion(mario)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.indicadores.lotesComprados").value(2))
        .andExpect(jsonPath("$.indicadores.kgComprados").value(100.5))
        .andExpect(jsonPath("$.indicadores.lotesConCodigo").value(1))
        .andExpect(jsonPath("$.lotes.length()").value(2))
        .andExpect(jsonPath("$.lotes[?(@.codigo == 'CRH-2026-0001')].estado").value("ACTIVO"))
        .andExpect(jsonPath("$.lotes[?(@.codigo == 'CRH-2026-0002')].estado").value("PENDIENTE"))
        .andExpect(jsonPath("$.lotes[0].finca").value("Finca El Tucán"))
        .andExpect(jsonPath("$.lotes[0].variedad").value("Caturra"))
        .andExpect(jsonPath("$.lotes[0].fechaCompra").exists());
  }

  @Test
  void sinLotesLosIndicadoresVanEnCero() throws Exception {
    mockMvc
        .perform(get("/api/lotes").cookie(sesion(mario)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.indicadores.lotesComprados").value(0))
        .andExpect(jsonPath("$.indicadores.kgComprados").value(0))
        .andExpect(jsonPath("$.indicadores.lotesConCodigo").value(0))
        .andExpect(jsonPath("$.lotes.length()").value(0));
  }

  @Test
  void activarDevuelveLaFichaDelLote() throws Exception {
    Lote lote = lote(mario, "60.00", "CRH-2026-0001");

    mockMvc
        .perform(post("/api/lotes/{id}/codigo", lote.getId()).cookie(sesion(mario)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.codigo").value("CRH-2026-0001"))
        .andExpect(jsonPath("$.finca").value("Finca El Tucán"))
        .andExpect(jsonPath("$.variedad").value("Caturra"))
        .andExpect(jsonPath("$.kg").value(60.0))
        .andExpect(jsonPath("$.tostador").value("Mario Brenes"))
        .andExpect(jsonPath("$.estado").value("ACTIVO"));

    Lote leido = loteRepository.findById(lote.getId()).orElseThrow();
    assertThat(leido.getEstadoTrazabilidad()).isEqualTo(EstadoTrazabilidad.ACTIVO);
    assertThat(leido.getActivadoEn()).isNotNull();
  }

  @Test
  void activarDosVecesNoCambiaElCodigo() throws Exception {
    Lote lote = lote(mario, "60.00", "CRH-2026-0001");
    mockMvc.perform(post("/api/lotes/{id}/codigo", lote.getId()).cookie(sesion(mario)));
    var primeraActivacion = loteRepository.findById(lote.getId()).orElseThrow().getActivadoEn();

    mockMvc
        .perform(post("/api/lotes/{id}/codigo", lote.getId()).cookie(sesion(mario)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.codigo").value("CRH-2026-0001"));

    Lote leido = loteRepository.findById(lote.getId()).orElseThrow();
    assertThat(leido.getCodigo()).isEqualTo("CRH-2026-0001");
    assertThat(leido.getActivadoEn()).isEqualTo(primeraActivacion);
  }

  @Test
  void activarUnLoteAjenoDa403() throws Exception {
    Cuenta ana = cuenta("Ana Vargas", "ana@ejemplo.cr", Rol.TOSTADOR);
    Lote lote = lote(mario, "60.00", "CRH-2026-0001");

    mockMvc
        .perform(post("/api/lotes/{id}/codigo", lote.getId()).cookie(sesion(ana)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.mensaje").value("Este lote no es tuyo."));

    assertThat(loteRepository.findById(lote.getId()).orElseThrow().getEstadoTrazabilidad())
        .isEqualTo(EstadoTrazabilidad.PENDIENTE);
  }

  @Test
  void activarUnLoteQueNoExisteDa404() throws Exception {
    mockMvc
        .perform(post("/api/lotes/{id}/codigo", UUID.randomUUID()).cookie(sesion(mario)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.mensaje").value("No encontramos ese lote."));
  }

  @Test
  void otroRolNoPuedeVerLotes() throws Exception {
    Cuenta andres = cuenta("Andrés Solano", "andres@ejemplo.cr", Rol.CAFETALERO);

    mockMvc.perform(get("/api/lotes").cookie(sesion(andres))).andExpect(status().isForbidden());
  }

  @Test
  void sinSesionDa401() throws Exception {
    mockMvc.perform(get("/api/lotes")).andExpect(status().isUnauthorized());
  }

  @Test
  void listaLosTuestesDeUnLotePropioOrdenadosPorFechaDesc() throws Exception {
    Lote lote = lote(mario, "100.00", "CRH-2026-0010");
    Tueste primero =
        tueste(lote, NivelTueste.CLARO, "50.00", "43.00", LocalDate.of(2026, 3, 1));
    primero.setNotas("Primer tueste.");
    tuesteRepository.saveAndFlush(primero);
    Tueste segundo =
        tueste(lote, NivelTueste.OSCURO, "40.00", "32.00", LocalDate.of(2026, 3, 8));
    lote(mario, "20.00", "CRH-2026-0011");

    mockMvc
        .perform(get("/api/lotes/{id}/tuestes", lote.getId()).cookie(sesion(mario)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].id").value(segundo.getId().toString()))
        .andExpect(jsonPath("$[0].loteId").value(lote.getId().toString()))
        .andExpect(jsonPath("$[0].nivel").value("OSCURO"))
        .andExpect(jsonPath("$[0].pesoVerdeKg").value(40.0))
        .andExpect(jsonPath("$[0].pesoTostadoKg").value(32.0))
        .andExpect(jsonPath("$[0].merma").value(20.0))
        .andExpect(jsonPath("$[0].fechaTueste").value("2026-03-08"))
        .andExpect(jsonPath("$[0].notas").value(nullValue()))
        .andExpect(jsonPath("$[1].id").value(primero.getId().toString()))
        .andExpect(jsonPath("$[1].nivel").value("CLARO"))
        .andExpect(jsonPath("$[1].pesoVerdeKg").value(50.0))
        .andExpect(jsonPath("$[1].pesoTostadoKg").value(43.0))
        .andExpect(jsonPath("$[1].merma").value(14.0))
        .andExpect(jsonPath("$[1].fechaTueste").value("2026-03-01"))
        .andExpect(jsonPath("$[1].notas").value("Primer tueste."));
  }

  @Test
  void listaVaciaSiElLoteNoTieneTuestes() throws Exception {
    Lote lote = lote(mario, "60.00", "CRH-2026-0012");

    mockMvc
        .perform(get("/api/lotes/{id}/tuestes", lote.getId()).cookie(sesion(mario)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(0)));
  }

  @Test
  void tuestesDeUnLoteAjenoDa403() throws Exception {
    Cuenta ana = cuenta("Ana Vargas", "ana@ejemplo.cr", Rol.TOSTADOR);
    Lote lote = lote(mario, "60.00", "CRH-2026-0013");

    mockMvc
        .perform(get("/api/lotes/{id}/tuestes", lote.getId()).cookie(sesion(ana)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.mensaje").value("Este lote no es tuyo."));
  }

  @Test
  void tuestesDeUnLoteInexistenteDa404() throws Exception {
    mockMvc
        .perform(get("/api/lotes/{id}/tuestes", UUID.randomUUID()).cookie(sesion(mario)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.mensaje").value("No encontramos ese lote."));
  }

  @Test
  void listarTuestesPideSesionYRolTostador() throws Exception {
    Lote lote = lote(mario, "60.00", "CRH-2026-0014");
    Cuenta andres = cuenta("Andrés Solano", "andres@ejemplo.cr", Rol.CAFETALERO);
    String ruta = "/api/lotes/" + lote.getId() + "/tuestes";

    mockMvc.perform(get(ruta)).andExpect(status().isUnauthorized());
    mockMvc.perform(get(ruta).cookie(sesion(andres))).andExpect(status().isForbidden());
  }
}
