package com.tuanistack.cranhua.tueste;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class TuesteTest {

  private Tueste tueste(String pesoVerdeKg, String pesoTostadoKg) {
    return new Tueste(
        null,
        NivelTueste.MEDIO,
        new BigDecimal(pesoVerdeKg),
        new BigDecimal(pesoTostadoKg),
        LocalDate.of(2026, 3, 20));
  }

  @Test
  void laMermaSeCalculaSobreElPesoVerde() {
    assertThat(tueste("50.00", "42.50").getMermaPorcentaje()).isEqualTo(new BigDecimal("15.00"));
  }

  @Test
  void laMermaSeRedondeaADosDecimales() {
    // 1 / 3 = 33,333... %
    assertThat(tueste("3.00", "2.00").getMermaPorcentaje()).isEqualTo(new BigDecimal("33.33"));
    // 1 / 6 = 16,666... %
    assertThat(tueste("6.00", "5.00").getMermaPorcentaje()).isEqualTo(new BigDecimal("16.67"));
  }

  @Test
  void sinPerdidaLaMermaEsCero() {
    assertThat(tueste("10.00", "10.00").getMermaPorcentaje()).isEqualByComparingTo("0");
  }
}
