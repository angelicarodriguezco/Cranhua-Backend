package com.tuanistack.cranhua.publico.dto;

import com.tuanistack.cranhua.finca.Proceso;
import com.tuanistack.cranhua.finca.ZonaCafetalera;
import com.tuanistack.cranhua.finca.dto.Coordenada;
import com.tuanistack.cranhua.tueste.NivelTueste;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record TrazabilidadPublicaResponse(
    String codigo,
    Instant activadoEn,
    FincaOrigen finca,
    CosechaOrigen cosecha,
    TostadorLote tostador,
    List<TuesteLote> tuestes,
    List<EnlacePerfil> cafeterias) {

  public record FincaOrigen(
      UUID id,
      String nombre,
      ZonaCafetalera zonaCafetalera,
      String canton,
      String provincia,
      Coordenada ubicacion,
      List<String> propietarios,
      boolean perfilDisponible) {}

  public record CosechaOrigen(
      String variedad,
      Integer alturaMsnm,
      LocalDate fechaInicio,
      LocalDate fechaFin,
      List<ProcesoLote> procesos,
      List<String> notasSabor,
      BigDecimal puntajeSca) {}

  public record ProcesoLote(Proceso proceso, Integer porcentaje) {}

  public record TostadorLote(
      UUID id, String nombre, String canton, String provincia, boolean perfilDisponible) {}

  public record TuesteLote(NivelTueste nivel, LocalDate fechaTueste) {}
}
