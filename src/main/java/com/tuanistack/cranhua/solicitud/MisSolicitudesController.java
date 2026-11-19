package com.tuanistack.cranhua.solicitud;

import com.tuanistack.cranhua.solicitud.dto.SolicitudRecibidaResponse;
import com.tuanistack.cranhua.solicitud.dto.SolicitudesEnviadasResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/solicitudes")
public class MisSolicitudesController {

  private final MisSolicitudesService misSolicitudesService;

  public MisSolicitudesController(MisSolicitudesService misSolicitudesService) {
    this.misSolicitudesService = misSolicitudesService;
  }

  @GetMapping("/enviadas")
  @PreAuthorize("hasRole('TOSTADOR')")
  public SolicitudesEnviadasResponse enviadas(Authentication authentication) {
    return misSolicitudesService.enviadas(UUID.fromString(authentication.getName()));
  }

  @GetMapping("/recibidas")
  @PreAuthorize("hasRole('CAFETALERO')")
  public List<SolicitudRecibidaResponse> recibidas(
      @RequestParam(required = false) EstadoSolicitud estado, Authentication authentication) {
    return misSolicitudesService.recibidas(UUID.fromString(authentication.getName()), estado);
  }
}
