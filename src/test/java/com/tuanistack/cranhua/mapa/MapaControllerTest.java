package com.tuanistack.cranhua.mapa;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tuanistack.cranhua.BasePostgis;
import com.tuanistack.cranhua.cafeteria.Cafeteria;
import com.tuanistack.cranhua.cafeteria.CafeteriaRepository;
import com.tuanistack.cranhua.cuenta.Cuenta;
import com.tuanistack.cranhua.cuenta.CuentaRepository;
import com.tuanistack.cranhua.cuenta.Rol;
import com.tuanistack.cranhua.finca.Finca;
import com.tuanistack.cranhua.finca.FincaRepository;
import com.tuanistack.cranhua.finca.GeometriaUtil;
import com.tuanistack.cranhua.finca.dto.Coordenada;
import com.tuanistack.cranhua.tueste.NegocioTueste;
import com.tuanistack.cranhua.tueste.NegocioTuesteRepository;
import com.tuanistack.cranhua.tueste.NivelTueste;
import java.util.List;
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
class MapaControllerTest {

  private static final String RUTA = "/api/publico/mapa";

  @Autowired private MockMvc mockMvc;

  @Autowired private CuentaRepository cuentaRepository;

  @Autowired private FincaRepository fincaRepository;

  @Autowired private NegocioTuesteRepository negocioRepository;

  @Autowired private CafeteriaRepository cafeteriaRepository;

  @BeforeEach
  void crearDatos() {
    Finca fincaEliminada = finca("Finca Vieja");
    fincaEliminada.eliminar();
    fincaRepository.saveAll(List.of(finca("Finca El Tucán"), fincaEliminada));

    NegocioTueste negocioEliminado = negocio("Tostaduría Cerrada", "cerrada@ejemplo.cr");
    negocioEliminado.eliminar();
    negocioRepository.saveAll(
        List.of(negocio("Tostaduría Volcán", "volcan@ejemplo.cr"), negocioEliminado));

    Cafeteria cafeteriaEliminada = cafeteria("Café Cerrado", "cerrado@ejemplo.cr");
    cafeteriaEliminada.eliminar();
    cafeteriaRepository.saveAll(
        List.of(cafeteria("Café Brumas", "brumas@ejemplo.cr"), cafeteriaEliminada));
  }

  @AfterEach
  void limpiar() {
    cafeteriaRepository.deleteAll();
    negocioRepository.deleteAll();
    fincaRepository.deleteAll();
    cuentaRepository.deleteAll();
  }

  // Cuadrado de 9.65 a 9.67 de latitud y -84.03 a -84.01 de longitud; su centro es 9.66, -84.02
  private Finca finca(String nombre) {
    return new Finca(
        nombre,
        GeometriaUtil.poligono(
            List.of(
                new Coordenada(9.65, -84.03),
                new Coordenada(9.65, -84.01),
                new Coordenada(9.67, -84.01),
                new Coordenada(9.67, -84.03))),
        GeometriaUtil.punto(new Coordenada(9.651, -84.029)),
        "Tarrazú",
        "San José");
  }

  private Cuenta cuenta(String correo, Rol rol) {
    return cuentaRepository.save(new Cuenta("Mariana Castro", correo, "hash", Set.of(rol)));
  }

  private NegocioTueste negocio(String nombre, String correo) {
    return new NegocioTueste(
        cuenta(correo, Rol.TOSTADOR),
        nombre,
        "Mariana Castro",
        "+506 8888-1234",
        correo,
        "Escazú",
        "San José",
        Set.of(NivelTueste.MEDIO));
  }

  private Cafeteria cafeteria(String nombre, String correo) {
    return new Cafeteria(
        cuenta(correo, Rol.CAFETERIA),
        nombre,
        "Andrés Solano",
        "+506 8899-4410",
        correo,
        GeometriaUtil.punto(new Coordenada(9.9335, -84.0775)),
        "San José",
        "San José",
        Set.of(NivelTueste.CLARO));
  }

  @Test
  void respondeSinSesionYSinEliminados() throws Exception {
    mockMvc
        .perform(get(RUTA))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].tipo", contains("FINCA", "TOSTADOR", "CAFETERIA")))
        .andExpect(
            jsonPath(
                "$[*].nombre",
                containsInAnyOrder("Finca El Tucán", "Tostaduría Volcán", "Café Brumas")))
        .andExpect(jsonPath("$[*].telefono").doesNotExist())
        .andExpect(jsonPath("$[*].correo").doesNotExist());
  }

  @Test
  void laFincaUsaElCentroDeSuPoligono() throws Exception {
    mockMvc
        .perform(get(RUTA).param("tipo", "FINCA"))
        .andExpect(jsonPath("$[0].id").exists())
        .andExpect(jsonPath("$[0].canton").value("Tarrazú"))
        .andExpect(jsonPath("$[0].provincia").value("San José"))
        .andExpect(jsonPath("$[0].coordenada.lat", closeTo(9.66, 1e-9)))
        .andExpect(jsonPath("$[0].coordenada.lng", closeTo(-84.02, 1e-9)));
  }

  @Test
  void elTostadorUsaLaCabeceraDeSuCanton() throws Exception {
    mockMvc
        .perform(get(RUTA).param("tipo", "TOSTADOR"))
        .andExpect(jsonPath("$[0].canton").value("Escazú"))
        .andExpect(jsonPath("$[0].coordenada.lat").value(9.9189))
        .andExpect(jsonPath("$[0].coordenada.lng").value(-84.1397));
  }

  @Test
  void laCafeteriaUsaSuPin() throws Exception {
    mockMvc
        .perform(get(RUTA).param("tipo", "CAFETERIA"))
        .andExpect(jsonPath("$[*].tipo", contains("CAFETERIA")))
        .andExpect(jsonPath("$[0].coordenada.lat").value(9.9335))
        .andExpect(jsonPath("$[0].coordenada.lng").value(-84.0775));
  }

  @Test
  void tipoDesconocidoEsSolicitudInvalida() throws Exception {
    mockMvc.perform(get(RUTA).param("tipo", "BENEFICIO")).andExpect(status().isBadRequest());
  }
}
