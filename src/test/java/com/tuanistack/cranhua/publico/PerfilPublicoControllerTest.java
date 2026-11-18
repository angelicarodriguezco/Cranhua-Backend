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
import com.tuanistack.cranhua.finca.Proceso;
import com.tuanistack.cranhua.finca.ProcesoReparto;
import com.tuanistack.cranhua.finca.dto.Coordenada;
import com.tuanistack.cranhua.tueste.NegocioTueste;
import com.tuanistack.cranhua.tueste.NegocioTuesteRepository;
import com.tuanistack.cranhua.tueste.NivelTueste;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
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
class PerfilPublicoControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private CuentaRepository cuentaRepository;

  @Autowired private FincaRepository fincaRepository;

  @Autowired private CosechaRepository cosechaRepository;

  @Autowired private CafeteriaRepository cafeteriaRepository;

  @Autowired private ItemMenuRepository itemMenuRepository;

  @Autowired private NegocioTuesteRepository negocioTuesteRepository;

  private Finca finca;

  private Cafeteria cafeteria;

  private NegocioTueste tostador;

  @BeforeEach
  void crearDatos() {
    Cuenta cafetalero = cuenta("Ana Solís", "ana@ejemplo.cr", Rol.CAFETALERO);
    finca =
        new Finca("Finca El Roble", perimetro(), punto(9.6598, -84.0223), "Tarrazú", "San José");
    finca.setDescripcion("Café bajo sombra a 1.450 msnm.");
    finca.agregarDueno(cafetalero);
    finca = fincaRepository.saveAndFlush(finca);

    Cosecha disponible = cosecha("Geisha", Proceso.HONEY, "CRH-2026-0001");
    disponible.registrarPesoTrasSeleccion(new BigDecimal("290.00"));
    cosechaRepository.saveAndFlush(disponible);
    cosechaRepository.saveAndFlush(cosecha("Caturra", Proceso.LAVADO, null));

    cafeteria =
        new Cafeteria(
            cuenta("Sofía Jiménez", "sofia@ejemplo.cr", Rol.CAFETERIA),
            "Café Brumas",
            "Sofía Jiménez",
            "+506 8888-1234",
            "hola@brumas.cr",
            punto(9.9205, -84.137),
            "Escazú",
            "San José",
            Set.of(NivelTueste.MEDIO));
    cafeteria.setDescripcion("Café de especialidad en Escazú.");
    cafeteria.setHorario("L-S 7:00 a 18:00");
    cafeteria = cafeteriaRepository.saveAndFlush(cafeteria);

    ItemMenu geisha = new ItemMenu(cafeteria, "Geisha filtrado", CategoriaMenu.CAFE, precio());
    geisha.setCodigoTrazabilidad("CRH-2026-0001");
    itemMenuRepository.saveAndFlush(geisha);
    itemMenuRepository.saveAndFlush(
        new ItemMenu(cafeteria, "Queque de banano", CategoriaMenu.OTRA_COMIDA, precio()));

    tostador =
        negocioTuesteRepository.saveAndFlush(
            new NegocioTueste(
                cuenta("Diego Mora", "diego@ejemplo.cr", Rol.TOSTADOR),
                "Tueste Volcán",
                "Diego Mora",
                "+506 8777-0000",
                "diego@volcan.cr",
                "Naranjo",
                "Alajuela",
                Set.of(NivelTueste.CLARO)));
  }

  @AfterEach
  void limpiar() {
    itemMenuRepository.deleteAll();
    cafeteriaRepository.deleteAll();
    negocioTuesteRepository.deleteAll();
    cosechaRepository.deleteAll();
    fincaRepository.deleteAll();
    cuentaRepository.deleteAll();
  }

  private Cuenta cuenta(String nombre, String correo, Rol rol) {
    return cuentaRepository.saveAndFlush(new Cuenta(nombre, correo, "hash", Set.of(rol)));
  }

  private static Point punto(double lat, double lng) {
    return GeometriaUtil.punto(new Coordenada(lat, lng));
  }

  private static Polygon perimetro() {
    return GeometriaUtil.poligono(
        List.of(
            new Coordenada(9.6575, -84.0247),
            new Coordenada(9.6575, -84.0200),
            new Coordenada(9.6620, -84.0200)));
  }

  private static BigDecimal precio() {
    return new BigDecimal("2500.00");
  }

  private Cosecha cosecha(String variedad, Proceso proceso, String codigo) {
    Cosecha cosecha =
        new Cosecha(
            finca,
            variedad,
            1450,
            LocalDate.of(2026, 1, 5),
            LocalDate.of(2026, 2, 28),
            new BigDecimal("1200.00"),
            new BigDecimal("9.75"),
            List.of(new ProcesoReparto(proceso, 100)));
    cosecha.setCodigoLote(codigo);
    return cosecha;
  }

  @Test
  void perfilDeFincaSinSesionConSusCafeterias() throws Exception {
    mockMvc
        .perform(get("/api/publico/fincas/" + finca.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nombre").value("Finca El Roble"))
        .andExpect(jsonPath("$.descripcion").value("Café bajo sombra a 1.450 msnm."))
        .andExpect(jsonPath("$.propietarios", contains("Ana Solís")))
        .andExpect(jsonPath("$.variedades", contains("Caturra", "Geisha")))
        .andExpect(jsonPath("$.cosechasActivas", hasSize(1)))
        .andExpect(jsonPath("$.cosechasActivas[0].codigoLote").value("CRH-2026-0001"))
        .andExpect(jsonPath("$.cosechasActivas[0].proceso").value("Honey"))
        .andExpect(jsonPath("$.cafeterias", hasSize(1)))
        .andExpect(jsonPath("$.cafeterias[0].id").value(cafeteria.getId().toString()))
        .andExpect(jsonPath("$.cafeterias[0].nombre").value("Café Brumas"))
        .andExpect(jsonPath("$.cafeterias[0].telefono").doesNotExist())
        .andExpect(jsonPath("$.cafeterias[0].correo").doesNotExist());
  }

  @Test
  void perfilDeCafeteriaConHorarioYFincasDeOrigen() throws Exception {
    mockMvc
        .perform(get("/api/publico/cafeterias/" + cafeteria.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nombre").value("Café Brumas"))
        .andExpect(jsonPath("$.descripcion").value("Café de especialidad en Escazú."))
        .andExpect(jsonPath("$.horario").value("L-S 7:00 a 18:00"))
        .andExpect(jsonPath("$.cafesTrazables").value(1))
        .andExpect(jsonPath("$.fincas", hasSize(1)))
        .andExpect(jsonPath("$.fincas[0].id").value(finca.getId().toString()))
        .andExpect(jsonPath("$.tostadores", hasSize(0)))
        .andExpect(jsonPath("$.telefono").doesNotExist())
        .andExpect(jsonPath("$.correo").doesNotExist())
        .andExpect(jsonPath("$.nombreContacto").doesNotExist());
  }

  @Test
  void perfilDeTostadorSinSesion() throws Exception {
    mockMvc
        .perform(get("/api/publico/tostadores/" + tostador.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nombre").value("Tueste Volcán"))
        .andExpect(jsonPath("$.estilos", contains("CLARO")))
        .andExpect(jsonPath("$.fincas", hasSize(0)))
        .andExpect(jsonPath("$.puntosVenta", hasSize(0)))
        .andExpect(jsonPath("$.telefono").doesNotExist())
        .andExpect(jsonPath("$.correo").doesNotExist());
  }

  @Test
  void descripcionYHorarioVaciosNoSeIncluyen() throws Exception {
    cafeteria.setDescripcion("   ");
    cafeteria.setHorario(null);
    cafeteriaRepository.saveAndFlush(cafeteria);
    tostador.setDescripcion(null);
    negocioTuesteRepository.saveAndFlush(tostador);

    mockMvc
        .perform(get("/api/publico/cafeterias/" + cafeteria.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.descripcion").doesNotExist())
        .andExpect(jsonPath("$.horario").doesNotExist());
    mockMvc
        .perform(get("/api/publico/tostadores/" + tostador.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.descripcion").doesNotExist());
  }

  @Test
  void cafeteriaEliminadaNoApareceEnLaFincaYRespondeNoEncontrado() throws Exception {
    cafeteria.eliminar();
    cafeteriaRepository.saveAndFlush(cafeteria);

    mockMvc
        .perform(get("/api/publico/fincas/" + finca.getId()))
        .andExpect(jsonPath("$.cafeterias", hasSize(0)));
    mockMvc
        .perform(get("/api/publico/cafeterias/" + cafeteria.getId()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.mensaje").value("No encontramos esa cafetería."));
  }

  @Test
  void fincaEliminadaNoApareceEnLaCafeteriaYRespondeNoEncontrado() throws Exception {
    finca.eliminar();
    fincaRepository.saveAndFlush(finca);

    mockMvc
        .perform(get("/api/publico/cafeterias/" + cafeteria.getId()))
        .andExpect(jsonPath("$.fincas", hasSize(0)));
    mockMvc.perform(get("/api/publico/fincas/" + finca.getId())).andExpect(status().isNotFound());
  }

  @Test
  void idInexistenteRespondeNoEncontrado() throws Exception {
    mockMvc
        .perform(get("/api/publico/tostadores/" + UUID.randomUUID()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.mensaje").value("No encontramos ese tostador."));
  }
}
