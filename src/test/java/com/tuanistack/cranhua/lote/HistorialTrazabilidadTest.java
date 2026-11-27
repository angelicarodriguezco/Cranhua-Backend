package com.tuanistack.cranhua.lote;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tuanistack.cranhua.BasePostgis;
import com.tuanistack.cranhua.auth.CookieSesion;
import com.tuanistack.cranhua.cafeteria.Cafeteria;
import com.tuanistack.cranhua.cafeteria.CafeteriaRepository;
import com.tuanistack.cranhua.cafeteria.CategoriaMenu;
import com.tuanistack.cranhua.cafeteria.ItemMenu;
import com.tuanistack.cranhua.cafeteria.ItemMenuRepository;
import com.tuanistack.cranhua.cuenta.Cuenta;
import com.tuanistack.cranhua.cuenta.CuentaRepository;
import com.tuanistack.cranhua.cuenta.Rol;
import com.tuanistack.cranhua.finca.Cosecha;
import com.tuanistack.cranhua.finca.CosechaRepository;
import com.tuanistack.cranhua.finca.Finca;
import com.tuanistack.cranhua.finca.FincaRepository;
import com.tuanistack.cranhua.finca.GeometriaUtil;
import com.tuanistack.cranhua.finca.dto.Coordenada;
import com.tuanistack.cranhua.solicitud.Solicitud;
import com.tuanistack.cranhua.solicitud.SolicitudRepository;
import com.tuanistack.cranhua.solicitud.SolicitudRepositoryTest;
import com.tuanistack.cranhua.tueste.NegocioTueste;
import com.tuanistack.cranhua.tueste.NegocioTuesteRepository;
import com.tuanistack.cranhua.tueste.NivelTueste;
import com.tuanistack.cranhua.tueste.TuesteRepository;
import jakarta.servlet.http.Cookie;
import java.math.BigDecimal;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
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
class HistorialTrazabilidadTest {

  private static final String TRAZABILIDAD = "/api/publico/trazabilidad/";

  @Autowired private MockMvc mockMvc;
  @Autowired private CookieSesion cookieSesion;
  @Autowired private CuentaRepository cuentaRepository;
  @Autowired private FincaRepository fincaRepository;
  @Autowired private CosechaRepository cosechaRepository;
  @Autowired private SolicitudRepository solicitudRepository;
  @Autowired private LoteRepository loteRepository;
  @Autowired private TuesteRepository tuesteRepository;
  @Autowired private NegocioTuesteRepository negocioTuesteRepository;
  @Autowired private CafeteriaRepository cafeteriaRepository;
  @Autowired private ItemMenuRepository itemMenuRepository;

  @AfterEach
  void limpiar() {
    itemMenuRepository.deleteAll();
    cafeteriaRepository.deleteAll();
    tuesteRepository.deleteAll();
    negocioTuesteRepository.deleteAll();
    loteRepository.deleteAll();
    solicitudRepository.deleteAll();
    cosechaRepository.deleteAll();
    fincaRepository.deleteAll();
    cuentaRepository.deleteAll();
  }

  @Test
  void fincaConLoteActivoIndicaHistorialYDevuelveCodigoDeEjemplo() throws Exception {
    Cuenta ana = cuenta("ana@ejemplo.cr", Rol.CAFETALERO);
    Finca finca = finca(ana);
    lote(finca, cuenta("diego@ejemplo.cr", Rol.TOSTADOR), "CRH-2026-0101", true);

    mockMvc
        .perform(get("/api/fincas/" + finca.getId()).cookie(sesion(ana)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.historialTrazabilidad.tieneHistorial").value(true))
        .andExpect(jsonPath("$.historialTrazabilidad.codigoEjemplo").value("CRH-2026-0101"));
  }

  @Test
  void fincaConLoteSinCodigoActivoNoTieneHistorial() throws Exception {
    Cuenta ana = cuenta("ana@ejemplo.cr", Rol.CAFETALERO);
    Finca finca = finca(ana);
    lote(finca, cuenta("diego@ejemplo.cr", Rol.TOSTADOR), "CRH-2026-0102", false);

    mockMvc
        .perform(get("/api/fincas/" + finca.getId()).cookie(sesion(ana)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.historialTrazabilidad.tieneHistorial").value(false))
        .andExpect(jsonPath("$.historialTrazabilidad.codigoEjemplo").doesNotExist());
  }

  @Test
  void listaDeFincasNoTraeElHistorial() throws Exception {
    Cuenta ana = cuenta("ana@ejemplo.cr", Rol.CAFETALERO);
    finca(ana);

    mockMvc
        .perform(get("/api/fincas").cookie(sesion(ana)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].historialTrazabilidad").doesNotExist());
  }

  @Test
  void eliminarFincaConHistorialSePermiteYConservaElCodigo() throws Exception {
    Cuenta ana = cuenta("ana@ejemplo.cr", Rol.CAFETALERO);
    Finca finca = finca(ana);
    lote(finca, cuenta("diego@ejemplo.cr", Rol.TOSTADOR), "CRH-2026-0103", true);

    mockMvc
        .perform(delete("/api/fincas/" + finca.getId()).cookie(sesion(ana)))
        .andExpect(status().isNoContent());

    assertThat(fincaRepository.findById(finca.getId()).orElseThrow().isEliminada()).isTrue();
    assertThat(loteRepository.findByCodigo("CRH-2026-0103")).isPresent();
    mockMvc.perform(get(TRAZABILIDAD + "CRH-2026-0103")).andExpect(status().isOk());
  }

  @Test
  void negocioConLoteActivoIndicaHistorial() throws Exception {
    Cuenta diego = cuenta("diego@ejemplo.cr", Rol.TOSTADOR);
    negocio(diego);
    lote(finca(cuenta("ana@ejemplo.cr", Rol.CAFETALERO)), diego, "CRH-2026-0201", true);

    mockMvc
        .perform(get("/api/tostadores/actual").cookie(sesion(diego)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.historialTrazabilidad.tieneHistorial").value(true))
        .andExpect(jsonPath("$.historialTrazabilidad.codigoEjemplo").value("CRH-2026-0201"));
  }

  @Test
  void negocioSinLotesNoTieneHistorial() throws Exception {
    Cuenta diego = cuenta("diego@ejemplo.cr", Rol.TOSTADOR);
    negocio(diego);

    mockMvc
        .perform(get("/api/tostadores/actual").cookie(sesion(diego)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.historialTrazabilidad.tieneHistorial").value(false));
  }

  @Test
  void eliminarNegocioConHistorialSePermiteYConservaElCodigo() throws Exception {
    Cuenta diego = cuenta("diego@ejemplo.cr", Rol.TOSTADOR);
    NegocioTueste negocio = negocio(diego);
    lote(finca(cuenta("ana@ejemplo.cr", Rol.CAFETALERO)), diego, "CRH-2026-0202", true);

    mockMvc
        .perform(delete("/api/tostadores/" + negocio.getId()).cookie(sesion(diego)))
        .andExpect(status().isNoContent());

    assertThat(loteRepository.findByCodigo("CRH-2026-0202")).isPresent();
    mockMvc.perform(get(TRAZABILIDAD + "CRH-2026-0202")).andExpect(status().isOk());
  }

  @Test
  void cafeteriaConItemConCodigoIndicaHistorial() throws Exception {
    Cuenta sofia = cuenta("sofia@ejemplo.cr", Rol.CAFETERIA);
    Cafeteria cafeteria = cafeteria(sofia);
    lote(
        finca(cuenta("ana@ejemplo.cr", Rol.CAFETALERO)),
        cuenta("diego@ejemplo.cr", Rol.TOSTADOR),
        "CRH-2026-0301",
        true);
    item(cafeteria, "Caturra filtrado", "CRH-2026-0301");
    item(cafeteria, "Croissant", null);

    mockMvc
        .perform(get("/api/cafeterias/actual").cookie(sesion(sofia)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.historialTrazabilidad.tieneHistorial").value(true))
        .andExpect(jsonPath("$.historialTrazabilidad.codigoEjemplo").value("CRH-2026-0301"));
    mockMvc
        .perform(get("/api/cafeterias/" + cafeteria.getId()).cookie(sesion(sofia)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.historialTrazabilidad.tieneHistorial").value(true));
  }

  @Test
  void cafeteriaSinItemsConCodigoNoTieneHistorial() throws Exception {
    Cuenta sofia = cuenta("sofia@ejemplo.cr", Rol.CAFETERIA);
    item(cafeteria(sofia), "Croissant", null);

    mockMvc
        .perform(get("/api/cafeterias/actual").cookie(sesion(sofia)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.historialTrazabilidad.tieneHistorial").value(false))
        .andExpect(jsonPath("$.historialTrazabilidad.codigoEjemplo").doesNotExist());
  }

  @Test
  void eliminarCafeteriaConHistorialSePermiteYConservaElMenu() throws Exception {
    Cuenta sofia = cuenta("sofia@ejemplo.cr", Rol.CAFETERIA);
    Cafeteria cafeteria = cafeteria(sofia);
    lote(
        finca(cuenta("ana@ejemplo.cr", Rol.CAFETALERO)),
        cuenta("diego@ejemplo.cr", Rol.TOSTADOR),
        "CRH-2026-0302",
        true);
    item(cafeteria, "Caturra filtrado", "CRH-2026-0302");

    mockMvc
        .perform(delete("/api/cafeterias/" + cafeteria.getId()).cookie(sesion(sofia)))
        .andExpect(status().isNoContent());

    assertThat(itemMenuRepository.count()).isEqualTo(1);
    mockMvc.perform(get(TRAZABILIDAD + "CRH-2026-0302")).andExpect(status().isOk());
  }

  @Test
  void cuentaConHistorialEnAlgunoDeSusRecursosLoIndica() throws Exception {
    Cuenta ana = cuenta("ana@ejemplo.cr", Rol.CAFETALERO);
    lote(finca(ana), cuenta("diego@ejemplo.cr", Rol.TOSTADOR), "CRH-2026-0401", true);

    mockMvc
        .perform(get("/api/cuenta").cookie(sesion(ana)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.historialTrazabilidad.tieneHistorial").value(true))
        .andExpect(jsonPath("$.historialTrazabilidad.codigoEjemplo").value("CRH-2026-0401"));
  }

  @Test
  void cuentaSinRecursosNoTieneHistorial() throws Exception {
    Cuenta ana = cuenta("ana@ejemplo.cr", Rol.CAFETALERO);

    mockMvc
        .perform(get("/api/cuenta").cookie(sesion(ana)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.historialTrazabilidad.tieneHistorial").value(false));
  }

  @Test
  void eliminarCuentaEliminaSusFincasNegocioYCafeteriaSinBorrarLosCodigos() throws Exception {
    Cuenta todo = cuenta("todo@ejemplo.cr", Rol.CAFETALERO, Rol.TOSTADOR, Rol.CAFETERIA);
    Finca finca = finca(todo);
    NegocioTueste negocio = negocio(todo);
    Cafeteria cafeteria = cafeteria(todo);
    lote(finca, todo, "CRH-2026-0402", true);
    item(cafeteria, "Caturra filtrado", "CRH-2026-0402");

    mockMvc.perform(delete("/api/cuenta").cookie(sesion(todo))).andExpect(status().isNoContent());

    assertThat(fincaRepository.findById(finca.getId()).orElseThrow().isEliminada()).isTrue();
    assertThat(negocioTuesteRepository.findById(negocio.getId()).orElseThrow().isEliminado())
        .isTrue();
    assertThat(cafeteriaRepository.findById(cafeteria.getId()).orElseThrow().isEliminada())
        .isTrue();
    assertThat(loteRepository.findByCodigo("CRH-2026-0402")).isPresent();
    assertThat(itemMenuRepository.count()).isEqualTo(1);
    mockMvc.perform(get(TRAZABILIDAD + "CRH-2026-0402")).andExpect(status().isOk());
  }

  @Test
  void eliminarCuentaConFincaCompartidaSoloLaQuitaComoDueno() throws Exception {
    Cuenta ana = cuenta("ana@ejemplo.cr", Rol.CAFETALERO);
    Cuenta luis = cuenta("luis@ejemplo.cr", Rol.CAFETALERO);
    Finca finca = SolicitudRepositoryTest.finca();
    finca.agregarDueno(ana);
    finca.agregarDueno(luis);
    finca = fincaRepository.saveAndFlush(finca);

    mockMvc.perform(delete("/api/cuenta").cookie(sesion(ana))).andExpect(status().isNoContent());

    mockMvc
        .perform(get("/api/fincas/" + finca.getId()).cookie(sesion(luis)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.duenos.length()").value(1))
        .andExpect(jsonPath("$.duenos[0].id").value(luis.getId().toString()));
  }

  private Cookie sesion(Cuenta cuenta) {
    return new Cookie(CookieSesion.NOMBRE, cookieSesion.crear(cuenta).getValue());
  }

  private Cuenta cuenta(String correo, Rol... roles) {
    return cuentaRepository.saveAndFlush(
        new Cuenta("Persona Prueba", correo, "hash", Set.of(roles)));
  }

  private Finca finca(Cuenta dueno) {
    Finca finca = SolicitudRepositoryTest.finca();
    finca.agregarDueno(dueno);
    return fincaRepository.saveAndFlush(finca);
  }

  private NegocioTueste negocio(Cuenta tostador) {
    return negocioTuesteRepository.saveAndFlush(
        new NegocioTueste(
            tostador,
            "Tueste Volcán",
            "Diego Mora",
            "+506 8777-0000",
            "diego@volcan.cr",
            "Naranjo",
            "Alajuela",
            Set.of(NivelTueste.MEDIO)));
  }

  private Cafeteria cafeteria(Cuenta cuenta) {
    return cafeteriaRepository.saveAndFlush(
        new Cafeteria(
            cuenta,
            "Café Brumas",
            "Sofía Jiménez",
            "+506 8888-1234",
            "hola@brumas.cr",
            GeometriaUtil.punto(new Coordenada(9.9205, -84.137)),
            "Escazú",
            "San José",
            Set.of(NivelTueste.MEDIO)));
  }

  private void item(Cafeteria cafeteria, String nombre, String codigo) {
    ItemMenu item =
        new ItemMenu(
            cafeteria,
            nombre,
            codigo == null ? CategoriaMenu.OTRA_COMIDA : CategoriaMenu.CAFE,
            new BigDecimal("2500"));
    item.setCodigoTrazabilidad(codigo);
    itemMenuRepository.saveAndFlush(item);
  }

  private Lote lote(Finca finca, Cuenta tostador, String codigo, boolean activo) {
    Cosecha cosecha = cosechaRepository.saveAndFlush(SolicitudRepositoryTest.nuevaCosecha(finca));
    Solicitud solicitud = new Solicitud(cosecha, tostador, new BigDecimal("20.00"));
    solicitud.aprobar();
    solicitud = solicitudRepository.saveAndFlush(solicitud);
    Lote lote = new Lote(solicitud, codigo);
    if (activo) {
      lote.activar();
    }
    return loteRepository.saveAndFlush(lote);
  }
}
