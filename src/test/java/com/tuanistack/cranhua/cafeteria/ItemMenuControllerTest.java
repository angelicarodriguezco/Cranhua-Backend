package com.tuanistack.cranhua.cafeteria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import com.tuanistack.cranhua.finca.FincaRepository;
import com.tuanistack.cranhua.finca.GeometriaUtil;
import com.tuanistack.cranhua.finca.dto.Coordenada;
import com.tuanistack.cranhua.lote.Lote;
import com.tuanistack.cranhua.lote.LoteRepository;
import com.tuanistack.cranhua.solicitud.Solicitud;
import com.tuanistack.cranhua.solicitud.SolicitudRepository;
import com.tuanistack.cranhua.solicitud.SolicitudRepositoryTest;
import com.tuanistack.cranhua.tueste.NivelTueste;
import jakarta.servlet.http.Cookie;
import java.math.BigDecimal;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(BasePostgis.class)
class ItemMenuControllerTest {

  private static final String ACTIVO = "CRH-2026-0001";

  private static final String PENDIENTE = "CRH-2026-0002";

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private CookieSesion cookieSesion;

  @Autowired private CuentaRepository cuentaRepository;

  @Autowired private FincaRepository fincaRepository;

  @Autowired private CosechaRepository cosechaRepository;

  @Autowired private SolicitudRepository solicitudRepository;

  @Autowired private LoteRepository loteRepository;

  @Autowired private CafeteriaRepository cafeteriaRepository;

  @Autowired private ItemMenuRepository itemMenuRepository;

  private Cuenta sofia;

  private Cuenta diego;

  private Cafeteria deSofia;

  private Cafeteria deDiego;

  @BeforeEach
  void crearDatos() {
    sofia = cuenta("Sofía Jiménez", "sofia@ejemplo.cr", Rol.CAFETERIA);
    diego = cuenta("Diego Mora", "diego@ejemplo.cr", Rol.CAFETERIA);
    deSofia = cafeteria(sofia);
    deDiego = cafeteria(diego);

    Cuenta mario = cuenta("Mario Brenes", "mario@ejemplo.cr", Rol.TOSTADOR);
    Cosecha cosecha =
        cosechaRepository.saveAndFlush(
            SolicitudRepositoryTest.nuevaCosecha(
                fincaRepository.saveAndFlush(SolicitudRepositoryTest.finca())));
    Lote activo = lote(cosecha, mario, ACTIVO);
    activo.activar();
    loteRepository.saveAndFlush(activo);
    lote(cosecha, mario, PENDIENTE);
  }

  @AfterEach
  void limpiar() {
    itemMenuRepository.deleteAll();
    cafeteriaRepository.deleteAll();
    loteRepository.deleteAll();
    solicitudRepository.deleteAll();
    cosechaRepository.deleteAll();
    fincaRepository.deleteAll();
    cuentaRepository.deleteAll();
  }

  private Cuenta cuenta(String nombre, String correo, Rol rol) {
    return cuentaRepository.saveAndFlush(new Cuenta(nombre, correo, "hash", Set.of(rol)));
  }

  private Cafeteria cafeteria(Cuenta cuenta) {
    return cafeteriaRepository.saveAndFlush(
        new Cafeteria(
            cuenta,
            "Café Tuanis",
            cuenta.getNombreCompleto(),
            "+506 8888-1234",
            cuenta.getCorreo(),
            GeometriaUtil.punto(new Coordenada(9.9325, -84.0795)),
            "San José",
            "San José",
            Set.of(NivelTueste.MEDIO)));
  }

  private Lote lote(Cosecha cosecha, Cuenta tostador, String codigo) {
    Solicitud solicitud = new Solicitud(cosecha, tostador, new BigDecimal("60.00"));
    solicitud.aprobar();
    return loteRepository.saveAndFlush(
        new Lote(solicitudRepository.saveAndFlush(solicitud), codigo));
  }

  private Cookie sesion(Cuenta cuenta) {
    return new Cookie(CookieSesion.NOMBRE, cookieSesion.crear(cuenta).getValue());
  }

  private static Map<String, Object> item(String nombre, String categoria, String codigo) {
    Map<String, Object> item = new LinkedHashMap<>();
    item.put("nombre", nombre);
    item.put("categoria", categoria);
    item.put("codigoTrazabilidad", codigo);
    item.put("precio", 1800);
    return item;
  }

  private ResultActions publicar(Cuenta cuenta, Cafeteria cafeteria, Map<String, Object> item)
      throws Exception {
    return mockMvc.perform(
        post("/api/cafeterias/{id}/menu", cafeteria.getId())
            .cookie(sesion(cuenta))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(item)));
  }

  private ResultActions publicar(Map<String, Object> item) throws Exception {
    return publicar(sofia, deSofia, item);
  }

  private ResultActions listar(Cuenta cuenta, Cafeteria cafeteria) throws Exception {
    return mockMvc.perform(
        get("/api/cafeterias/{id}/menu", cafeteria.getId()).cookie(sesion(cuenta)));
  }

  private ResultActions quitar(Cuenta cuenta, Cafeteria cafeteria, UUID itemId) throws Exception {
    return mockMvc.perform(
        delete("/api/cafeterias/{id}/menu/{itemId}", cafeteria.getId(), itemId)
            .cookie(sesion(cuenta)));
  }

  private UUID itemDe(Cafeteria cafeteria) {
    return itemMenuRepository
        .saveAndFlush(
            new ItemMenu(cafeteria, "Queque", CategoriaMenu.OTRA_COMIDA, new BigDecimal("1500")))
        .getId();
  }

  @Test
  void publicaUnCafeConElCodigoDeUnLoteActivo() throws Exception {
    publicar(item(" Espresso ", "CAFE", " crh-2026-0001 "))
        .andExpect(status().isCreated())
        .andExpect(header().exists("Location"))
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.nombre").value("Espresso"))
        .andExpect(jsonPath("$.categoria").value("CAFE"))
        .andExpect(jsonPath("$.codigoTrazabilidad").value(ACTIVO))
        .andExpect(jsonPath("$.precio").value(1800));
  }

  @Test
  void elCodigoEsOpcional() throws Exception {
    publicar(item("Espresso", "CAFE", null))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.codigoTrazabilidad").value(nullValue()));
    publicar(item("Queque", "OTRA_COMIDA", " "))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.codigoTrazabilidad").value(nullValue()));
  }

  @Test
  void validaNombreCategoriaYPrecio() throws Exception {
    publicar(Map.of())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errores.nombre").exists())
        .andExpect(jsonPath("$.errores.categoria").exists())
        .andExpect(jsonPath("$.errores.precio").exists());

    Map<String, Object> gratis = item("Espresso", "CAFE", null);
    gratis.put("precio", 0);
    publicar(gratis)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errores.precio").value("El precio tiene que ser mayor a 0."));

    publicar(item("Espresso", "BEBIDA", null))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errores.categoria").exists());
  }

  @Test
  void unCodigoInexistenteOPendienteDa404() throws Exception {
    publicar(item("Espresso", "CAFE", "CRH-2026-9999"))
        .andExpect(status().isNotFound())
        .andExpect(
            jsonPath("$.mensaje").value("No hay un lote activo con el código CRH-2026-9999."));
    publicar(item("Espresso", "CAFE", PENDIENTE))
        .andExpect(status().isNotFound())
        .andExpect(
            jsonPath("$.mensaje").value("No hay un lote activo con el código CRH-2026-0002."));

    assertThat(itemMenuRepository.count()).isZero();
  }

  @Test
  void otraComidaConCodigoDa400() throws Exception {
    publicar(item("Queque", "OTRA_COMIDA", ACTIVO))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.mensaje").value("Solo los cafés llevan código de trazabilidad."));
  }

  @Test
  void repetirNombreYCodigoDa409() throws Exception {
    publicar(item("Espresso", "CAFE", ACTIVO)).andExpect(status().isCreated());
    publicar(item(" espresso", "CAFE", ACTIVO))
        .andExpect(status().isConflict())
        .andExpect(
            jsonPath("$.mensaje").value("Ya publicaste un ítem con ese nombre y ese código."));

    publicar(item("Queque", "OTRA_COMIDA", null)).andExpect(status().isCreated());
    publicar(item("Queque", "OTRA_COMIDA", null)).andExpect(status().isConflict());
  }

  @Test
  void soloChocaSiCoincidenNombreCodigoYCafeteria() throws Exception {
    publicar(item("Espresso", "CAFE", ACTIVO)).andExpect(status().isCreated());

    publicar(item("Capuchino", "CAFE", ACTIVO)).andExpect(status().isCreated());
    publicar(item("Espresso", "CAFE", null)).andExpect(status().isCreated());
    publicar(diego, deDiego, item("Espresso", "CAFE", ACTIVO)).andExpect(status().isCreated());
  }

  @Test
  void listaSoloElMenuDeLaCafeteria() throws Exception {
    publicar(item("Queque", "OTRA_COMIDA", null));
    publicar(item("Espresso", "CAFE", ACTIVO));
    itemDe(deDiego);

    listar(sofia, deSofia)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].nombre", contains("Espresso", "Queque")))
        .andExpect(jsonPath("$[*].categoria", contains("CAFE", "OTRA_COMIDA")))
        .andExpect(jsonPath("$[*].codigoTrazabilidad", contains(ACTIVO, null)))
        .andExpect(jsonPath("$[0].precio").value(1800.0));
  }

  @Test
  void quitaUnItemYPermiteVolverAPublicarlo() throws Exception {
    UUID itemId = itemDe(deSofia);

    quitar(sofia, deSofia, itemId).andExpect(status().isNoContent());
    listar(sofia, deSofia).andExpect(jsonPath("$.length()").value(0));
    quitar(sofia, deSofia, itemId).andExpect(status().isNotFound());
    publicar(item("Queque", "OTRA_COMIDA", null)).andExpect(status().isCreated());
  }

  @Test
  void otraCafeteriaRecibe403() throws Exception {
    UUID itemId = itemDe(deSofia);

    publicar(diego, deSofia, item("Espresso", "CAFE", null)).andExpect(status().isForbidden());
    listar(diego, deSofia).andExpect(status().isForbidden());
    quitar(diego, deSofia, itemId).andExpect(status().isForbidden());
    quitar(diego, deDiego, itemId).andExpect(status().isForbidden());

    assertThat(itemMenuRepository.findAll()).extracting(ItemMenu::getId).containsExactly(itemId);
  }

  @Test
  void otroRolRecibe403YSinSesion401() throws Exception {
    Cuenta ana = cuenta("Ana Vargas", "ana@ejemplo.cr", Rol.TOSTADOR);

    listar(ana, deSofia).andExpect(status().isForbidden());
    mockMvc
        .perform(get("/api/cafeterias/{id}/menu", deSofia.getId()))
        .andExpect(status().isUnauthorized());
  }
}
