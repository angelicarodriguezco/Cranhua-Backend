package com.tuanistack.cranhua.lote.dto;

import java.util.Optional;

public record HistorialTrazabilidad(boolean tieneHistorial, String codigoEjemplo) {

  public static final HistorialTrazabilidad SIN_HISTORIAL = new HistorialTrazabilidad(false, null);

  public static HistorialTrazabilidad de(Optional<String> codigoEjemplo) {
    return codigoEjemplo
        .map(codigo -> new HistorialTrazabilidad(true, codigo))
        .orElse(SIN_HISTORIAL);
  }
}
