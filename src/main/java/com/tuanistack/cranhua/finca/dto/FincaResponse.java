package com.tuanistack.cranhua.finca.dto;

import com.tuanistack.cranhua.cuenta.Cuenta;
import com.tuanistack.cranhua.finca.Finca;
import com.tuanistack.cranhua.finca.GeometriaUtil;
import com.tuanistack.cranhua.finca.ZonaCafetalera;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public record FincaResponse(
    UUID id,
    String nombre,
    String descripcion,
    ZonaCafetalera zonaCafetalera,
    List<Coordenada> perimetro,
    Coordenada ubicacion,
    String canton,
    String provincia,
    BigDecimal areaHectareas,
    List<Dueno> duenos) {

  public record Dueno(UUID id, String nombreCompleto) {}

  public static FincaResponse de(Finca finca, BigDecimal areaHectareas) {
    return new FincaResponse(
        finca.getId(),
        finca.getNombre(),
        finca.getDescripcion(),
        finca.getZonaCafetalera(),
        GeometriaUtil.puntos(finca.getPerimetro()),
        GeometriaUtil.coordenada(finca.getUbicacion()),
        finca.getCanton(),
        finca.getProvincia(),
        areaHectareas,
        finca.getDuenos().stream()
            .sorted(Comparator.comparing(Cuenta::getNombreCompleto))
            .map(cuenta -> new Dueno(cuenta.getId(), cuenta.getNombreCompleto()))
            .toList());
  }
}
