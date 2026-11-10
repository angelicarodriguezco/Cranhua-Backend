package com.tuanistack.cranhua.finca.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tuanistack.cranhua.cuenta.Cuenta;
import com.tuanistack.cranhua.finca.Cosecha;
import com.tuanistack.cranhua.finca.EstadoCosecha;
import com.tuanistack.cranhua.finca.Finca;
import com.tuanistack.cranhua.finca.GeometriaUtil;
import com.tuanistack.cranhua.finca.Proceso;
import com.tuanistack.cranhua.finca.ProcesoReparto;
import com.tuanistack.cranhua.finca.ZonaCafetalera;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

// El resumen de cosechas solo viene en GET /api/fincas y la lista de cosechas en GET /{id}
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
    List<Dueno> duenos,
    @JsonInclude(JsonInclude.Include.NON_NULL) Long cosechasActivas,
    @JsonInclude(JsonInclude.Include.NON_NULL) BigDecimal kgDisponibles,
    @JsonInclude(JsonInclude.Include.NON_NULL) List<CosechaFinca> cosechas) {

  public record Dueno(UUID id, String nombreCompleto) {}

  public record CosechaFinca(
      UUID id,
      String variedad,
      Proceso procesoPrincipal,
      String codigoLote,
      EstadoCosecha estado,
      BigDecimal kgDisponibles,
      BigDecimal precioKg) {

    static CosechaFinca de(Cosecha cosecha) {
      return new CosechaFinca(
          cosecha.getId(),
          cosecha.getVariedad(),
          cosecha.getProcesos().stream()
              .max(Comparator.comparing(ProcesoReparto::getPorcentaje))
              .map(ProcesoReparto::getProceso)
              .orElse(null),
          cosecha.getCodigoLote(),
          cosecha.getEstado(),
          cosecha.getKgDisponibles(),
          cosecha.getPrecioKg());
    }
  }

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
        duenos(finca),
        null,
        null,
        null);
  }

  public FincaResponse conResumen(long cosechasActivas, BigDecimal kgDisponibles) {
    return new FincaResponse(
        id,
        nombre,
        descripcion,
        zonaCafetalera,
        perimetro,
        ubicacion,
        canton,
        provincia,
        areaHectareas,
        duenos,
        cosechasActivas,
        kgDisponibles,
        null);
  }

  public FincaResponse conCosechas(List<Cosecha> cosechas) {
    return new FincaResponse(
        id,
        nombre,
        descripcion,
        zonaCafetalera,
        perimetro,
        ubicacion,
        canton,
        provincia,
        areaHectareas,
        duenos,
        null,
        null,
        cosechas.stream().map(CosechaFinca::de).toList());
  }

  // Ordenados por nombre para que la respuesta y el mensaje del 409 salgan siempre igual
  public static List<Dueno> duenos(Finca finca) {
    return finca.getDuenos().stream()
        .sorted(Comparator.comparing(Cuenta::getNombreCompleto).thenComparing(Cuenta::getId))
        .map(cuenta -> new Dueno(cuenta.getId(), cuenta.getNombreCompleto()))
        .toList();
  }
}
