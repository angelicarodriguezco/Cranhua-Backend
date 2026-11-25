package com.tuanistack.cranhua.publico;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tuanistack.cranhua.BasePostgis;
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
import com.tuanistack.cranhua.lote.Lote;
import com.tuanistack.cranhua.lote.LoteRepository;
import com.tuanistack.cranhua.pedido.Pedido;
import com.tuanistack.cranhua.pedido.PedidoRepository;
import com.tuanistack.cranhua.solicitud.Solicitud;
import com.tuanistack.cranhua.solicitud.SolicitudRepository;
import com.tuanistack.cranhua.solicitud.SolicitudRepositoryTest;
import com.tuanistack.cranhua.tueste.NivelTueste;
import com.tuanistack.cranhua.tueste.Tueste;
import com.tuanistack.cranhua.tueste.TuesteRepository;
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
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(BasePostgis.class)
class MenuPublicoControllerTest {

  private static final String CODIGO = "CRH-2026-0001";

  @Autowired private MockMvc mockMvc;

  @Autowired private CuentaRepository cuentaRepository;

  @Autowired private FincaRepository fincaRepository;

  @Autowired private CosechaRepository cosechaRepository;

  @Autowired private SolicitudRepository solicitudRepository;

  @Autowired private LoteRepository loteRepository;

  @Autowired private TuesteRepository tuesteRepository;

  @Autowired private PedidoRepository pedidoRepository;

  @Autowired private CafeteriaRepository cafeteriaRepository;

  @Autowired private ItemMenuRepository itemMenuRepository;

  private Finca finca;

  private Cosecha cosecha;

  private Cafeteria cafeteria;

  @BeforeEach
  void crearDatos() {
    finca = fincaRepository.saveAndFlush(SolicitudRepositoryTest.finca());
    cosecha = SolicitudRepositoryTest.nuevaCosecha(finca);
    cosecha.setCodigoLote(CODIGO);
    cosecha.setNotasSabor("caramelo, cítricos");
    cosecha = cosechaRepository.saveAndFlush(cosecha);

    cafeteria =
        cafeteriaRepository.saveAndFlush(
            new Cafeteria(
                cuenta("Sofía Jiménez", "sofia@ejemplo.cr", Rol.CAFETERIA),
                "Café Brumas",
                "Sofía Jiménez",
                "+506 8888-1234",
                "hola@brumas.cr",
                GeometriaUtil.punto(new Coordenada(9.9205, -84.137)),
                "Escazú",
                "San José",
                Set.of(NivelTueste.MEDIO)));
  }

  @AfterEach
  void limpiar() {
    itemMenuRepository.deleteAll();
    pedidoRepository.deleteAll();
    tuesteRepository.deleteAll();
    loteRepository.deleteAll();
    solicitudRepository.deleteAll();
    cosechaRepository.deleteAll();
    fincaRepository.deleteAll();
    cafeteriaRepository.deleteAll();
    cuentaRepository.deleteAll();
  }

  private Cuenta cuenta(String nombre, String correo, Rol rol) {
    return cuentaRepository.saveAndFlush(new Cuenta(nombre, correo, "hash", Set.of(rol)));
  }

  private Lote lote() {
    Cuenta tostador = cuenta("Mario Brenes", "mario@ejemplo.cr", Rol.TOSTADOR);
    Solicitud solicitud = new Solicitud(cosecha, tostador, new BigDecimal("100.00"));
    solicitud.aprobar();
    Lote lote = new Lote(solicitudRepository.saveAndFlush(solicitud), CODIGO);
    lote.activar();
    return loteRepository.saveAndFlush(lote);
  }

  private Tueste tueste(Lote lote, NivelTueste nivel, LocalDate fecha) {
    return tuesteRepository.saveAndFlush(
        new Tueste(lote, nivel, new BigDecimal("40.00"), new BigDecimal("34.00"), fecha));
  }

  private void item(String nombre, CategoriaMenu categoria, String precio, String codigo) {
    ItemMenu item = new ItemMenu(cafeteria, nombre, categoria, new BigDecimal(precio));
    item.setCodigoTrazabilidad(codigo);
    itemMenuRepository.saveAndFlush(item);
  }

  private ResultActions menu(UUID cafeteriaId) throws Exception {
    return mockMvc.perform(get("/api/publico/cafeterias/{id}/menu", cafeteriaId));
  }

  @Test
  void separaCafesYOtrasComidasSinSesion() throws Exception {
    tueste(lote(), NivelTueste.CLARO, LocalDate.of(2026, 3, 1));
    item("Espresso", CategoriaMenu.CAFE, "1500.00", CODIGO);
    item("Café de la casa", CategoriaMenu.CAFE, "1200.00", null);
    item("Queque de banano", CategoriaMenu.OTRA_COMIDA, "1800.00", null);

    menu(cafeteria.getId())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nombre").value("Café Brumas"))
        .andExpect(jsonPath("$.canton").value("Escazú"))
        .andExpect(jsonPath("$.provincia").value("San José"))
        .andExpect(jsonPath("$.ubicacion.lat").value(9.9205))
        .andExpect(jsonPath("$.cafes[*].nombre", contains("Café de la casa", "Espresso")))
        .andExpect(jsonPath("$.cafes[0].precio").value(1200.0))
        .andExpect(jsonPath("$.cafes[0].nivelTueste").doesNotExist())
        .andExpect(jsonPath("$.cafes[0].notas").doesNotExist())
        .andExpect(jsonPath("$.cafes[0].fincaOrigen").doesNotExist())
        .andExpect(jsonPath("$.cafes[1].precio").value(1500.0))
        .andExpect(jsonPath("$.cafes[1].nivelTueste").value("CLARO"))
        .andExpect(jsonPath("$.cafes[1].notas", contains("caramelo", "cítricos")))
        .andExpect(jsonPath("$.cafes[1].fincaOrigen.id").value(finca.getId().toString()))
        .andExpect(jsonPath("$.cafes[1].fincaOrigen.nombre").value("Finca El Tucán"))
        .andExpect(jsonPath("$.otrasComidas", hasSize(1)))
        .andExpect(jsonPath("$.otrasComidas[0].nombre").value("Queque de banano"))
        .andExpect(jsonPath("$.otrasComidas[0].precio").value(1800.0))
        .andExpect(jsonPath("$.otrasComidas[0].length()").value(2));
  }

  @Test
  void conVariosTuestesUsaElMasRecienteSalvoQueHayaPedidoOtro() throws Exception {
    Lote lote = lote();
    Tueste claro = tueste(lote, NivelTueste.CLARO, LocalDate.of(2026, 3, 1));
    Tueste medio = tueste(lote, NivelTueste.MEDIO, LocalDate.of(2026, 3, 5));
    tueste(lote, NivelTueste.OSCURO, LocalDate.of(2026, 3, 10));
    item("Espresso", CategoriaMenu.CAFE, "1500.00", CODIGO);

    Pedido cancelado = new Pedido(cafeteria, medio, new BigDecimal("5.00"));
    cancelado.cancelar();
    pedidoRepository.saveAndFlush(cancelado);
    menu(cafeteria.getId())
        .andExpect(jsonPath("$.cafes", hasSize(1)))
        .andExpect(jsonPath("$.cafes[0].nivelTueste").value("OSCURO"));

    pedidoRepository.saveAndFlush(new Pedido(cafeteria, claro, new BigDecimal("5.00")));
    menu(cafeteria.getId())
        .andExpect(jsonPath("$.cafes", hasSize(1)))
        .andExpect(jsonPath("$.cafes[0].nivelTueste").value("CLARO"));
  }

  @Test
  void elCodigoDeUnaCosechaSinTostarTambienLlevaALaFinca() throws Exception {
    item("Espresso", CategoriaMenu.CAFE, "1500.00", CODIGO);

    menu(cafeteria.getId())
        .andExpect(jsonPath("$.cafes[0].nivelTueste").doesNotExist())
        .andExpect(jsonPath("$.cafes[0].notas", contains("caramelo", "cítricos")))
        .andExpect(jsonPath("$.cafes[0].fincaOrigen.id").value(finca.getId().toString()));
  }

  @Test
  void noEnlazaUnaFincaEliminada() throws Exception {
    item("Espresso", CategoriaMenu.CAFE, "1500.00", CODIGO);
    finca.eliminar();
    fincaRepository.saveAndFlush(finca);

    menu(cafeteria.getId())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.cafes", hasSize(1)))
        .andExpect(jsonPath("$.cafes[0].fincaOrigen").doesNotExist());
  }

  @Test
  void sinItemsDevuelveLasCategoriasVacias() throws Exception {
    menu(cafeteria.getId())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.cafes", hasSize(0)))
        .andExpect(jsonPath("$.otrasComidas", hasSize(0)));
  }

  @Test
  void cafeteriaEliminadaOInexistenteDa404() throws Exception {
    cafeteria.eliminar();
    cafeteriaRepository.saveAndFlush(cafeteria);

    menu(cafeteria.getId())
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.mensaje").value("No encontramos esa cafetería."));
    menu(UUID.randomUUID()).andExpect(status().isNotFound());
  }
}
