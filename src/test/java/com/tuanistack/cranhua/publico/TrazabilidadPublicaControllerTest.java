package com.tuanistack.cranhua.publico;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
import com.tuanistack.cranhua.solicitud.Solicitud;
import com.tuanistack.cranhua.solicitud.SolicitudRepository;
import com.tuanistack.cranhua.solicitud.SolicitudRepositoryTest;
import com.tuanistack.cranhua.tueste.NegocioTueste;
import com.tuanistack.cranhua.tueste.NegocioTuesteRepository;
import com.tuanistack.cranhua.tueste.NivelTueste;
import com.tuanistack.cranhua.tueste.Tueste;
import com.tuanistack.cranhua.tueste.TuesteRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
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
class TrazabilidadPublicaControllerTest {

  private static final String URL = "/api/publico/trazabilidad/";

  @Autowired private MockMvc mockMvc;
  @Autowired private CuentaRepository cuentaRepository;
  @Autowired private FincaRepository fincaRepository;
  @Autowired private CosechaRepository cosechaRepository;
  @Autowired private SolicitudRepository solicitudRepository;
  @Autowired private LoteRepository loteRepository;
  @Autowired private TuesteRepository tuesteRepository;
  @Autowired private NegocioTuesteRepository negocioTuesteRepository;
  @Autowired private CafeteriaRepository cafeteriaRepository;
  @Autowired private ItemMenuRepository itemMenuRepository;

  private Finca finca;
  private Cosecha cosecha;
  private Cuenta tostador;
  private NegocioTueste negocio;
  private Cafeteria cafeteria;

  @BeforeEach
  void crearDatos() {
    finca = SolicitudRepositoryTest.finca();
    finca.agregarDueno(cuenta("Ana Solís", "ana@ejemplo.cr", Rol.CAFETALERO));
    finca = fincaRepository.saveAndFlush(finca);
    cosecha = SolicitudRepositoryTest.nuevaCosecha(finca);
    cosecha.setNotasSabor("Chocolate, caramelo");
    cosecha = cosechaRepository.saveAndFlush(cosecha);

    tostador = cuenta("Diego Mora", "diego@ejemplo.cr", Rol.TOSTADOR);
    negocio =
        negocioTuesteRepository.saveAndFlush(
            new NegocioTueste(
                tostador,
                "Tueste Volcán",
                "Diego Mora",
                "+506 8777-0000",
                "diego@volcan.cr",
                "Naranjo",
                "Alajuela",
                Set.of(NivelTueste.MEDIO)));

    cafeteria =
        new Cafeteria(
            cuenta("Sofía Jiménez", "sofia@ejemplo.cr", Rol.CAFETERIA),
            "Café Brumas",
            "Sofía Jiménez",
            "+506 8888-1234",
            "hola@brumas.cr",
            GeometriaUtil.punto(new Coordenada(9.9205, -84.137)),
            "Escazú",
            "San José",
            Set.of(NivelTueste.MEDIO));
    cafeteria = cafeteriaRepository.saveAndFlush(cafeteria);
  }

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
  void codigoActivoDevuelveLaCadenaCompletaSinSesion() throws Exception {
    Lote lote = lote("CRH-2026-0001", true);
    tuesteRepository.saveAndFlush(
        new Tueste(lote, NivelTueste.MEDIO, kg("10.00"), kg("8.40"), LocalDate.of(2026, 3, 10)));
    ItemMenu item = new ItemMenu(cafeteria, "Caturra filtrado", CategoriaMenu.CAFE, kg("2500"));
    item.setCodigoTrazabilidad("CRH-2026-0001");
    itemMenuRepository.saveAndFlush(item);

    mockMvc
        .perform(get(URL + "CRH-2026-0001"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.codigo").value("CRH-2026-0001"))
        .andExpect(jsonPath("$.finca.id").value(finca.getId().toString()))
        .andExpect(jsonPath("$.finca.nombre").value("Finca El Tucán"))
        .andExpect(jsonPath("$.finca.propietarios[0]").value("Ana Solís"))
        .andExpect(jsonPath("$.finca.perfilDisponible").value(true))
        .andExpect(jsonPath("$.cosecha.variedad").value("Caturra"))
        .andExpect(jsonPath("$.cosecha.procesos[0].proceso").value("Lavado"))
        .andExpect(jsonPath("$.cosecha.notasSabor", hasSize(2)))
        .andExpect(jsonPath("$.tostador.id").value(negocio.getId().toString()))
        .andExpect(jsonPath("$.tostador.nombre").value("Tueste Volcán"))
        .andExpect(jsonPath("$.tuestes", hasSize(1)))
        .andExpect(jsonPath("$.tuestes[0].nivel").value("MEDIO"))
        .andExpect(jsonPath("$.tuestes[0].fechaTueste").value("2026-03-10"))
        .andExpect(jsonPath("$.cafeterias", hasSize(1)))
        .andExpect(jsonPath("$.cafeterias[0].nombre").value("Café Brumas"));
  }

  @Test
  void noExponeTelefonosNiCorreos() throws Exception {
    lote("CRH-2026-0002", true);

    mockMvc
        .perform(get(URL + "CRH-2026-0002"))
        .andExpect(status().isOk())
        .andExpect(content().string(not(containsString("@"))))
        .andExpect(content().string(not(containsString("+506"))));
  }

  @Test
  void aceptaElCodigoEnMinusculasYConEspacios() throws Exception {
    lote("CRH-2026-0003", true);

    mockMvc.perform(get(URL + " crh-2026-0003 ")).andExpect(status().isOk());
  }

  @Test
  void codigoInexistenteResponde404ConMensaje() throws Exception {
    mockMvc
        .perform(get(URL + "CRH-1999-9999"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.mensaje").value(containsString("No encontramos ese código")));
  }

  @Test
  void loteSinCodigoActivoResponde404() throws Exception {
    lote("CRH-2026-0004", false);

    mockMvc.perform(get(URL + "CRH-2026-0004")).andExpect(status().isNotFound());
  }

  @Test
  void siguenConsultablesConFincaNegocioYCafeteriaEliminados() throws Exception {
    lote("CRH-2026-0005", true);
    ItemMenu item = new ItemMenu(cafeteria, "Caturra filtrado", CategoriaMenu.CAFE, kg("2500"));
    item.setCodigoTrazabilidad("CRH-2026-0005");
    itemMenuRepository.saveAndFlush(item);
    finca.eliminar();
    fincaRepository.saveAndFlush(finca);
    negocio.eliminar();
    negocioTuesteRepository.saveAndFlush(negocio);
    cafeteria.eliminar();
    cafeteriaRepository.saveAndFlush(cafeteria);

    mockMvc
        .perform(get(URL + "CRH-2026-0005"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.finca.nombre").value("Finca El Tucán"))
        .andExpect(jsonPath("$.finca.perfilDisponible").value(false))
        .andExpect(jsonPath("$.tostador.nombre").value("Tueste Volcán"))
        .andExpect(jsonPath("$.tostador.perfilDisponible").value(false))
        .andExpect(jsonPath("$.cafeterias", hasSize(0)));
  }

  private Lote lote(String codigo, boolean activo) {
    Solicitud solicitud = new Solicitud(cosecha, tostador, kg("20.00"));
    solicitud.aprobar();
    solicitud = solicitudRepository.saveAndFlush(solicitud);
    Lote lote = new Lote(solicitud, codigo);
    if (activo) {
      lote.activar();
    }
    return loteRepository.saveAndFlush(lote);
  }

  private Cuenta cuenta(String nombre, String correo, Rol rol) {
    return cuentaRepository.saveAndFlush(new Cuenta(nombre, correo, "hash", Set.of(rol)));
  }

  private static BigDecimal kg(String valor) {
    return new BigDecimal(valor);
  }
}
