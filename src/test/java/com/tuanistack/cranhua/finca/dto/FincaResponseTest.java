package com.tuanistack.cranhua.finca.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.tuanistack.cranhua.cuenta.Cuenta;
import com.tuanistack.cranhua.cuenta.Rol;
import com.tuanistack.cranhua.finca.Finca;
import com.tuanistack.cranhua.finca.GeometriaUtil;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class FincaResponseTest {

  @Test
  void losDuenosSalenOrdenadosPorNombre() {
    List<Coordenada> cuadrado =
        List.of(
            new Coordenada(9.650, -84.030),
            new Coordenada(9.650, -84.020),
            new Coordenada(9.660, -84.020));
    Finca finca =
        new Finca(
            "Finca El Tucán",
            GeometriaUtil.poligono(cuadrado),
            GeometriaUtil.punto(cuadrado.get(0)),
            "Tarrazú",
            "San José");
    for (String nombre : List.of("Zoe Quirós", "Ana Brenes", "Lucía Mora")) {
      finca.agregarDueno(
          new Cuenta(nombre, nombre + "@ejemplo.cr", "hash", Set.of(Rol.CAFETALERO)));
    }

    assertThat(FincaResponse.de(finca, BigDecimal.ONE).duenos())
        .extracting(FincaResponse.Dueno::nombreCompleto)
        .containsExactly("Ana Brenes", "Lucía Mora", "Zoe Quirós");
  }
}
