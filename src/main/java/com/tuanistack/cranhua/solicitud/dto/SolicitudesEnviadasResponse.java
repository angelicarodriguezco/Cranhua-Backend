package com.tuanistack.cranhua.solicitud.dto;

import java.util.List;

public record SolicitudesEnviadasResponse(
    Indicadores indicadores, List<SolicitudEnviadaResponse> solicitudes) {

  public record Indicadores(long pendientes, long esteMes, long fincasContactadas) {}
}
