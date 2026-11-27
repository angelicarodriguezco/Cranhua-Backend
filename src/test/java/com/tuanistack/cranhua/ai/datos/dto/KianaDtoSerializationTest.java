package com.tuanistack.cranhua.ai.datos.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuanistack.cranhua.finca.Proceso;
import com.tuanistack.cranhua.finca.ZonaCafetalera;
import com.tuanistack.cranhua.finca.dto.Coordenada;
import com.tuanistack.cranhua.tueste.NivelTueste;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class KianaDtoSerializationTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static final Set<String> CAMPOS_PROHIBIDOS =
      Set.of(
          "telefono",
          "correo",
          "nombreContacto",
          "precioKg",
          "precio",
          "pedido",
          "pedidos",
          "solicitud",
          "solicitudes");

  @Test
  void fincaNoExponeContactosNiPrecios() throws Exception {
    assertSinCamposSensibles(
        new FincaKianaDto(
            "Finca El Roble",
            "Bajo sombra",
            ZonaCafetalera.TARRAZU,
            "Tarrazú",
            "San José",
            new Coordenada(9.66, -84.02)));
  }

  @Test
  void cosechaActivaNoExponePrecioKg() throws Exception {
    assertSinCamposSensibles(
        new CosechaActivaKianaDto(
            "Geisha",
            Proceso.HONEY,
            1450,
            new BigDecimal("200.00"),
            "CRH-2026-0001",
            "Finca El Roble"));
  }

  @Test
  void cafeteriaNoExponeTelefonoCorreoNiContacto() throws Exception {
    assertSinCamposSensibles(
        new CafeteriaKianaDto(
            "Café Brumas",
            "Especialidad",
            "L-S 7:00-18:00",
            "Escazú",
            "San José",
            new Coordenada(9.92, -84.14),
            Set.of(NivelTueste.MEDIO)));
  }

  @Test
  void tostadorNoExponeTelefonoCorreoNiContacto() throws Exception {
    assertSinCamposSensibles(
        new TostadorKianaDto(
            "Tueste Volcán", "Perfiles claros", "Naranjo", "Alajuela", Set.of(NivelTueste.CLARO)));
  }

  @Test
  void tuesteNoExponePedidosNiPrecios() throws Exception {
    assertSinCamposSensibles(
        new TuesteKianaDto(
            NivelTueste.MEDIO,
            "Finca El Roble",
            "Geisha",
            new BigDecimal("34.00"),
            "Tueste Volcán"));
  }

  @Test
  void recordsNoDeclaranCamposSensibles() {
    Set<String> componentes = new HashSet<>();
    for (Class<?> tipo :
        List.of(
            FincaKianaDto.class,
            CosechaActivaKianaDto.class,
            CafeteriaKianaDto.class,
            TostadorKianaDto.class,
            TuesteKianaDto.class)) {
      Arrays.stream(tipo.getRecordComponents())
          .map(RecordComponent::getName)
          .forEach(componentes::add);
    }

    assertThat(componentes).doesNotContainAnyElementsOf(CAMPOS_PROHIBIDOS);
  }

  private static void assertSinCamposSensibles(Object dto) throws Exception {
    JsonNode json = MAPPER.valueToTree(dto);
    assertThat(nombresDeCampo(json)).doesNotContainAnyElementsOf(CAMPOS_PROHIBIDOS);
  }

  private static Set<String> nombresDeCampo(JsonNode json) {
    Set<String> nombres = new HashSet<>();
    Iterator<String> it = json.fieldNames();
    while (it.hasNext()) {
      nombres.add(it.next());
    }
    return nombres;
  }
}
