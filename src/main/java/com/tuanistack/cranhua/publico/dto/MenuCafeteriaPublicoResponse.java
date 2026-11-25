package com.tuanistack.cranhua.publico.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tuanistack.cranhua.finca.dto.Coordenada;
import com.tuanistack.cranhua.tueste.NivelTueste;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record MenuCafeteriaPublicoResponse(
    UUID id,
    String nombre,
    String canton,
    String provincia,
    Coordenada ubicacion,
    List<Cafe> cafes,
    List<OtraComida> otrasComidas) {

  public record Cafe(
      String nombre,
      BigDecimal precio,
      @JsonInclude(JsonInclude.Include.NON_NULL) NivelTueste nivelTueste,
      @JsonInclude(JsonInclude.Include.NON_EMPTY) List<String> notas,
      @JsonInclude(JsonInclude.Include.NON_NULL) EnlacePerfil fincaOrigen) {}

  public record OtraComida(String nombre, BigDecimal precio) {}
}
