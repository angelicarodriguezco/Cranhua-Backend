package com.tuanistack.cranhua.finca.dto;

import com.tuanistack.cranhua.finca.Cosecha;
import com.tuanistack.cranhua.finca.Finca;
import com.tuanistack.cranhua.finca.Proceso;
import com.tuanistack.cranhua.finca.ZonaCafetalera;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public record CosechaDisponibleResponse(
    UUID id,
    UUID fincaId,
    String finca,
    ZonaCafetalera zona,
    String canton,
    String provincia,
    String variedad,
    Proceso proceso,
    List<String> notas,
    BigDecimal precioKg) {

  public static CosechaDisponibleResponse de(Cosecha cosecha) {
    Finca finca = cosecha.getFinca();
    return new CosechaDisponibleResponse(
        cosecha.getId(),
        finca.getId(),
        finca.getNombre(),
        finca.getZonaCafetalera(),
        finca.getCanton(),
        finca.getProvincia(),
        cosecha.getVariedad(),
        cosecha.getProcesoPrincipal(),
        notas(cosecha.getNotasSabor()),
        cosecha.getPrecioKg());
  }

  // Las notas de sabor se escriben como texto libre separado por comas
  static List<String> notas(String notasSabor) {
    if (notasSabor == null) {
      return List.of();
    }
    return Arrays.stream(notasSabor.split("[,;]"))
        .map(String::strip)
        .filter(n -> !n.isEmpty())
        .toList();
  }
}
