package com.tuanistack.cranhua.tueste.dto;

import com.tuanistack.cranhua.tueste.NegocioTueste;
import com.tuanistack.cranhua.tueste.NivelTueste;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record TostadorExploracionResponse(
    UUID id,
    String nombre,
    String canton,
    String provincia,
    List<NivelTueste> estilos,
    BigDecimal kgDisponibles) {

  public static TostadorExploracionResponse de(NegocioTueste negocio, BigDecimal kgDisponibles) {
    return new TostadorExploracionResponse(
        negocio.getId(),
        negocio.getNombre(),
        negocio.getCanton(),
        negocio.getProvincia(),
        negocio.getEstilos().stream().sorted().toList(),
        kgDisponibles);
  }
}
