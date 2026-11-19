package com.tuanistack.cranhua.dashboard;

import com.tuanistack.cranhua.dashboard.dto.DashboardTostadorResponse;
import com.tuanistack.cranhua.lote.LoteRepository;
import com.tuanistack.cranhua.solicitud.EstadoSolicitud;
import com.tuanistack.cranhua.solicitud.SolicitudRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardTostadorService {

  private static final ZoneId COSTA_RICA = ZoneId.of("America/Costa_Rica");

  private static final List<EstadoSolicitud> ESTADOS_ACTIVOS =
      List.of(EstadoSolicitud.PENDIENTE, EstadoSolicitud.APROBADA);

  private final SolicitudRepository solicitudRepository;
  private final LoteRepository loteRepository;

  public DashboardTostadorService(
      SolicitudRepository solicitudRepository, LoteRepository loteRepository) {
    this.solicitudRepository = solicitudRepository;
    this.loteRepository = loteRepository;
  }

  @Transactional(readOnly = true)
  public DashboardTostadorResponse resumen(UUID tostadorId) {
    YearMonth mesActual = YearMonth.now(COSTA_RICA);
    Instant inicioMes = mesActual.atDay(1).atStartOfDay(COSTA_RICA).toInstant();
    Instant inicioMesSiguiente =
        mesActual.plusMonths(1).atDay(1).atStartOfDay(COSTA_RICA).toInstant();

    long solicitudesActivas =
        solicitudRepository.countByTostadorIdAndEstadoIn(tostadorId, ESTADOS_ACTIVOS);
    BigDecimal kgCompradosMes =
        solicitudRepository.kgAprobadosEntre(tostadorId, inicioMes, inicioMesSiguiente);
    long lotesConCodigo = loteRepository.countBySolicitudTostadorId(tostadorId);

    return new DashboardTostadorResponse(solicitudesActivas, kgCompradosMes, lotesConCodigo);
  }
}
