package com.tuanistack.cranhua.tueste.dto;

import com.tuanistack.cranhua.tueste.NivelTueste;
import com.tuanistack.cranhua.tueste.Tueste;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record TuesteResponse(
    UUID id,
    UUID loteId,
    NivelTueste nivel,
    BigDecimal pesoVerdeKg,
    BigDecimal pesoTostadoKg,
    BigDecimal merma,
    LocalDate fechaTueste,
    String notas) {

  public static TuesteResponse de(Tueste tueste) {
    return new TuesteResponse(
        tueste.getId(),
        tueste.getLote().getId(),
        tueste.getNivel(),
        tueste.getPesoVerdeKg(),
        tueste.getPesoTostadoKg(),
        tueste.getMermaPorcentaje(),
        tueste.getFechaTueste(),
        tueste.getNotas());
  }
}
