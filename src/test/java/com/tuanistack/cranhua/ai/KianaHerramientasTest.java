package com.tuanistack.cranhua.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tuanistack.cranhua.ai.datos.KianaDatosService;
import com.tuanistack.cranhua.ai.datos.dto.CafeteriaKianaDto;
import com.tuanistack.cranhua.ai.datos.dto.CosechaActivaKianaDto;
import com.tuanistack.cranhua.ai.datos.dto.FincaKianaDto;
import com.tuanistack.cranhua.ai.datos.dto.TostadorKianaDto;
import com.tuanistack.cranhua.ai.datos.dto.TuesteKianaDto;
import com.tuanistack.cranhua.finca.Proceso;
import com.tuanistack.cranhua.finca.ZonaCafetalera;
import com.tuanistack.cranhua.finca.dto.Coordenada;
import com.tuanistack.cranhua.tueste.NivelTueste;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.tool.annotation.Tool;

@ExtendWith(MockitoExtension.class)
class KianaHerramientasTest {

  @Mock private KianaDatosService datos;

  @InjectMocks private KianaHerramientas herramientas;

  @Test
  void buscarCafeteriasSoloConsultaElServicio() {
    List<CafeteriaKianaDto> esperado =
        List.of(
            new CafeteriaKianaDto(
                "Café Guanacaste",
                null,
                null,
                "Liberia",
                "Guanacaste",
                new Coordenada(10.63, -85.44),
                Set.of(NivelTueste.MEDIO)));
    when(datos.buscarCafeterias("Guanacaste", null, null)).thenReturn(esperado);

    assertThat(herramientas.buscarCafeterias("Guanacaste", null, null)).isSameAs(esperado);
    verify(datos, only()).buscarCafeterias("Guanacaste", null, null);
  }

  @Test
  void buscarFincasSoloConsultaElServicio() {
    List<FincaKianaDto> esperado =
        List.of(
            new FincaKianaDto(
                "Finca Guanacaste",
                null,
                ZonaCafetalera.GUANACASTE,
                "Liberia",
                "Guanacaste",
                new Coordenada(10.63, -85.44)));
    when(datos.buscarFincas("Guanacaste", "Liberia", null)).thenReturn(esperado);

    assertThat(herramientas.buscarFincas("Guanacaste", "Liberia", null)).isSameAs(esperado);
    verify(datos, only()).buscarFincas("Guanacaste", "Liberia", null);
  }

  @Test
  void listarCosechasActivasSoloConsultaElServicio() {
    List<CosechaActivaKianaDto> esperado =
        List.of(
            new CosechaActivaKianaDto(
                "Geisha",
                Proceso.LAVADO,
                1400,
                new BigDecimal("100.00"),
                "CRH-2026-0001",
                "Finca Guanacaste"));
    when(datos.listarCosechasActivas(null, null, "Guanacaste")).thenReturn(esperado);

    assertThat(herramientas.listarCosechasActivas(null, null, "Guanacaste")).isSameAs(esperado);
    verify(datos, only()).listarCosechasActivas(null, null, "Guanacaste");
  }

  @Test
  void buscarTostadoresSoloConsultaElServicio() {
    List<TostadorKianaDto> esperado =
        List.of(
            new TostadorKianaDto(
                "Tueste Guanacaste", null, "Liberia", "Guanacaste", Set.of(NivelTueste.CLARO)));
    when(datos.buscarTostadores("Guanacaste", null, "Tueste")).thenReturn(esperado);

    assertThat(herramientas.buscarTostadores("Guanacaste", null, "Tueste")).isSameAs(esperado);
    verify(datos, only()).buscarTostadores("Guanacaste", null, "Tueste");
  }

  @Test
  void listarTuestesDisponiblesSoloConsultaElServicio() {
    List<TuesteKianaDto> esperado =
        List.of(
            new TuesteKianaDto(
                NivelTueste.MEDIO,
                "Finca Guanacaste",
                "Caturra",
                new BigDecimal("20.00"),
                "Tueste Guanacaste"));
    when(datos.listarTuestesDisponibles("Guanacaste", null, null)).thenReturn(esperado);

    assertThat(herramientas.listarTuestesDisponibles("Guanacaste", null, null)).isSameAs(esperado);
    verify(datos, only()).listarTuestesDisponibles("Guanacaste", null, null);
  }

  @Test
  void todasLasHerramientasSonDeSoloLecturaConTool() {
    List<String> metodosTool =
        Arrays.stream(KianaHerramientas.class.getDeclaredMethods())
            .filter(metodo -> metodo.isAnnotationPresent(Tool.class))
            .map(Method::getName)
            .sorted()
            .toList();

    assertThat(metodosTool)
        .containsExactly(
            "buscarCafeterias",
            "buscarFincas",
            "buscarTostadores",
            "listarCosechasActivas",
            "listarTuestesDisponibles");
    verifyNoInteractions(datos);
  }
}
