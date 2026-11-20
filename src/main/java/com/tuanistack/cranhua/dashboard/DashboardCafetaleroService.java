package com.tuanistack.cranhua.dashboard;

import com.tuanistack.cranhua.dashboard.dto.DashboardCafetaleroResponse;
import com.tuanistack.cranhua.dashboard.dto.DashboardCafetaleroResponse.CosechaReciente;
import com.tuanistack.cranhua.dashboard.dto.DashboardCafetaleroResponse.Indicadores;
import com.tuanistack.cranhua.finca.CosechaRepository;
import com.tuanistack.cranhua.finca.EstadoCosecha;
import com.tuanistack.cranhua.finca.FincaRepository;
import com.tuanistack.cranhua.solicitud.EstadoSolicitud;
import com.tuanistack.cranhua.solicitud.SolicitudRepository;
import com.tuanistack.cranhua.solicitud.dto.SolicitudRecibidaResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardCafetaleroService {

  static final int MAX_RECIENTES = 5;

  private final FincaRepository fincaRepository;
  private final CosechaRepository cosechaRepository;
  private final SolicitudRepository solicitudRepository;

  public DashboardCafetaleroService(
      FincaRepository fincaRepository,
      CosechaRepository cosechaRepository,
      SolicitudRepository solicitudRepository) {
    this.fincaRepository = fincaRepository;
    this.cosechaRepository = cosechaRepository;
    this.solicitudRepository = solicitudRepository;
  }

  @Transactional(readOnly = true)
  public DashboardCafetaleroResponse resumen(UUID cafetaleroId) {
    // Ya vienen ordenadas de la más nueva a la más vieja
    List<SolicitudRecibidaResponse> pendientes =
        solicitudRepository.recibidas(cafetaleroId, EstadoSolicitud.PENDIENTE);

    Indicadores indicadores =
        new Indicadores(
            fincaRepository.countByDuenosIdAndEliminadaFalse(cafetaleroId),
            cosechaRepository.contarDelDueno(cafetaleroId, EstadoCosecha.PUBLICADA),
            cosechaRepository.kgDisponiblesDelDueno(cafetaleroId, EstadoCosecha.PUBLICADA),
            pendientes.size());

    List<CosechaReciente> cosechasRecientes =
        cosechaRepository.recientesDelDueno(cafetaleroId, PageRequest.of(0, MAX_RECIENTES)).stream()
            .map(CosechaReciente::de)
            .toList();

    return new DashboardCafetaleroResponse(
        indicadores, pendientes.stream().limit(MAX_RECIENTES).toList(), cosechasRecientes);
  }
}
