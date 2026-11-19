package com.tuanistack.cranhua.solicitud;

import com.tuanistack.cranhua.solicitud.dto.SolicitudEnviadaResponse;
import com.tuanistack.cranhua.solicitud.dto.SolicitudRecibidaResponse;
import com.tuanistack.cranhua.solicitud.dto.SolicitudesEnviadasResponse;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MisSolicitudesService {

  private static final ZoneId COSTA_RICA = ZoneId.of("America/Costa_Rica");

  private final SolicitudRepository solicitudRepository;

  public MisSolicitudesService(SolicitudRepository solicitudRepository) {
    this.solicitudRepository = solicitudRepository;
  }

  @Transactional(readOnly = true)
  public SolicitudesEnviadasResponse enviadas(UUID tostadorId) {
    List<SolicitudEnviadaResponse> solicitudes = solicitudRepository.enviadas(tostadorId);
    YearMonth mesActual = YearMonth.now(COSTA_RICA);
    long pendientes =
        solicitudes.stream().filter(s -> s.estado() == EstadoSolicitud.PENDIENTE).count();
    long esteMes =
        solicitudes.stream()
            .filter(s -> YearMonth.from(s.solicitadaEn().atZone(COSTA_RICA)).equals(mesActual))
            .count();
    long fincasContactadas =
        solicitudes.stream().map(SolicitudEnviadaResponse::fincaId).distinct().count();
    return new SolicitudesEnviadasResponse(
        new SolicitudesEnviadasResponse.Indicadores(pendientes, esteMes, fincasContactadas),
        solicitudes);
  }

  @Transactional(readOnly = true)
  public List<SolicitudRecibidaResponse> recibidas(UUID duenoId, EstadoSolicitud estado) {
    return solicitudRepository.recibidas(duenoId, estado);
  }
}
