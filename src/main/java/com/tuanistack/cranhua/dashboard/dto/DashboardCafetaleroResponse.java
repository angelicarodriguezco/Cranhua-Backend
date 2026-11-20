package com.tuanistack.cranhua.dashboard.dto;

import com.tuanistack.cranhua.finca.Cosecha;
import com.tuanistack.cranhua.finca.EstadoCosecha;
import com.tuanistack.cranhua.finca.Proceso;
import com.tuanistack.cranhua.solicitud.dto.SolicitudRecibidaResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record DashboardCafetaleroResponse(
    Indicadores indicadores,
    List<SolicitudRecibidaResponse> solicitudesRecientes,
    List<CosechaReciente> cosechasRecientes) {

  public record Indicadores(
      long fincasRegistradas,
      long cosechasActivas,
      BigDecimal kgDisponibles,
      long solicitudesPendientes) {}

  public record CosechaReciente(
      UUID id,
      UUID fincaId,
      String finca,
      String variedad,
      Proceso proceso,
      BigDecimal kgDisponibles,
      EstadoCosecha estado) {

    public static CosechaReciente de(Cosecha cosecha) {
      return new CosechaReciente(
          cosecha.getId(),
          cosecha.getFinca().getId(),
          cosecha.getFinca().getNombre(),
          cosecha.getVariedad(),
          cosecha.getProcesoPrincipal(),
          cosecha.getKgDisponibles(),
          cosecha.getEstado());
    }
  }
}
