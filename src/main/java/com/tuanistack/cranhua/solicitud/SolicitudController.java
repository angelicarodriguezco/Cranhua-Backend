package com.tuanistack.cranhua.solicitud;

import com.tuanistack.cranhua.solicitud.dto.ResponderSolicitudRequest;
import com.tuanistack.cranhua.solicitud.dto.SolicitudResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/solicitudes")
public class SolicitudController {

  private final SolicitudService solicitudService;

  public SolicitudController(SolicitudService solicitudService) {
    this.solicitudService = solicitudService;
  }

  @PatchMapping("/{id}")
  @PreAuthorize("hasRole('CAFETALERO')")
  public SolicitudResponse responder(
      @PathVariable UUID id,
      @Valid @RequestBody ResponderSolicitudRequest request,
      Authentication authentication) {
    return solicitudService.responder(
        id, request.estado(), UUID.fromString(authentication.getName()));
  }
}
