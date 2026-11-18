package com.tuanistack.cranhua.publico.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tuanistack.cranhua.finca.Proceso;
import com.tuanistack.cranhua.finca.ZonaCafetalera;
import com.tuanistack.cranhua.finca.dto.Coordenada;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PerfilFincaPublicoResponse(
    UUID id,
    String nombre,
    @JsonInclude(JsonInclude.Include.NON_EMPTY) String descripcion,
    ZonaCafetalera zonaCafetalera,
    String canton,
    String provincia,
    Coordenada ubicacion,
    List<Coordenada> perimetro,
    List<String> propietarios,
    List<String> variedades,
    List<Proceso> procesos,
    Instant enCranhuaDesde,
    List<CosechaActiva> cosechasActivas,
    List<EnlacePerfil> cafeterias) {

  public record CosechaActiva(
      UUID id,
      String codigoLote,
      String variedad,
      Proceso proceso,
      Integer alturaMsnm,
      BigDecimal kgDisponibles) {}
}
