package com.tuanistack.cranhua.finca;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class CosechaTest {

  private Cosecha nuevaCosecha(List<ProcesoReparto> procesos) {
    return new Cosecha(
        FincaRepositoryTest.nuevaFinca(),
        "Catuaí",
        1300,
        LocalDate.of(2026, 1, 1),
        LocalDate.of(2026, 2, 1),
        new BigDecimal("800.00"),
        new BigDecimal("8.00"),
        procesos);
  }

  private Cosecha cosechaLavada() {
    return nuevaCosecha(List.of(new ProcesoReparto(Proceso.LAVADO, 100)));
  }

  @Test
  void porcentajesQueNoSuman100Fallan() {
    assertThatThrownBy(
            () ->
                nuevaCosecha(
                    List.of(
                        new ProcesoReparto(Proceso.HONEY, 50),
                        new ProcesoReparto(Proceso.NATURAL, 40))))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void sinProcesosFalla() {
    assertThatThrownBy(() -> nuevaCosecha(List.of())).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void registrarPesoTrasSeleccionInicializaDisponibles() {
    Cosecha cosecha = cosechaLavada();

    cosecha.registrarPesoTrasSeleccion(new BigDecimal("640.00"));

    assertThat(cosecha.getPesoTrasSeleccionKg()).isEqualByComparingTo("640.00");
    assertThat(cosecha.getKgDisponibles()).isEqualByComparingTo("640.00");
    assertThat(cosecha.getEstado()).isEqualTo(EstadoCosecha.DISPONIBLE);
  }

  @Test
  void descontarDisponibleBajaLosKg() {
    Cosecha cosecha = cosechaLavada();
    cosecha.registrarPesoTrasSeleccion(new BigDecimal("640.00"));

    cosecha.descontarDisponible(new BigDecimal("140.00"));
    cosecha.descontarDisponible(new BigDecimal("500.00"));

    assertThat(cosecha.getKgDisponibles()).isEqualByComparingTo("0");
  }

  @Test
  void descontarMasDeLoDisponibleFalla() {
    Cosecha cosecha = cosechaLavada();
    cosecha.registrarPesoTrasSeleccion(new BigDecimal("100.00"));

    assertThatThrownBy(() -> cosecha.descontarDisponible(new BigDecimal("100.01")))
        .isInstanceOf(IllegalStateException.class);
    assertThat(cosecha.getKgDisponibles()).isEqualByComparingTo("100.00");
  }

  @Test
  void descontarAntesDeLaSeleccionFalla() {
    assertThatThrownBy(() -> cosechaLavada().descontarDisponible(BigDecimal.ONE))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void descontarCeroONegativoFalla() {
    Cosecha cosecha = cosechaLavada();
    cosecha.registrarPesoTrasSeleccion(new BigDecimal("100.00"));

    assertThatThrownBy(() -> cosecha.descontarDisponible(BigDecimal.ZERO))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> cosecha.descontarDisponible(new BigDecimal("-5")))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
