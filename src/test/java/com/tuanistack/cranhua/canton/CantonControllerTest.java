package com.tuanistack.cranhua.canton;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuanistack.cranhua.BasePostgis;
import java.text.Collator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
class CantonControllerTest {

  private static final String RUTA = "/api/publico/cantones";

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Test
  void respondeSinSesionConLosCantonesCargadosPorMigracion() throws Exception {
    mockMvc
        .perform(get(RUTA))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(84)))
        .andExpect(jsonPath("$[0].id").exists())
        .andExpect(jsonPath("$[0].nombre").exists())
        .andExpect(jsonPath("$[0].provincia").exists())
        .andExpect(jsonPath("$[0].cabecera.lat").exists())
        .andExpect(jsonPath("$[0].cabecera.lng").exists());
  }

  @Test
  void devuelveLosDatosDeUnCanton() throws Exception {
    mockMvc
        .perform(get(RUTA).param("provincia", "San José"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.id == 105)].nombre").value("Tarrazú"))
        .andExpect(jsonPath("$[?(@.id == 105)].provincia").value("San José"))
        .andExpect(jsonPath("$[?(@.id == 105)].zonaCafetalera").value("Tarrazú"))
        .andExpect(jsonPath("$[?(@.id == 105)].cabecera.lat").value(9.6575))
        .andExpect(jsonPath("$[?(@.id == 105)].cabecera.lng").value(-84.0247));
  }

  @Test
  void filtraPorProvinciaSinImportarMayusculas() throws Exception {
    mockMvc
        .perform(get(RUTA).param("provincia", "cartago"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(8)))
        .andExpect(jsonPath("$[*].provincia", everyItem(is("Cartago"))));
  }

  @Test
  void provinciaDesconocidaDevuelveListaVacia() throws Exception {
    mockMvc
        .perform(get(RUTA).param("provincia", "Atlántida"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(0)));
  }

  @Test
  void provinciaEnBlancoDevuelveTodo() throws Exception {
    mockMvc
        .perform(get(RUTA).param("provincia", " "))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(84)));
  }

  @Test
  void vieneOrdenadoPorProvinciaYNombre() throws Exception {
    String cuerpo = mockMvc.perform(get(RUTA)).andReturn().getResponse().getContentAsString();
    List<String> claves = new ArrayList<>();
    for (JsonNode canton : objectMapper.readTree(cuerpo)) {
      claves.add(canton.get("provincia").asText() + "|" + canton.get("nombre").asText());
    }

    Collator espanol = Collator.getInstance(Locale.forLanguageTag("es-CR"));
    espanol.setStrength(Collator.PRIMARY);
    List<String> esperado = new ArrayList<>(claves);
    esperado.sort(espanol);
    assertThat(claves).isEqualTo(esperado);
  }
}
