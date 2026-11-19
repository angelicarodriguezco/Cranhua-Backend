package com.tuanistack.cranhua.solicitud;

import com.tuanistack.cranhua.solicitud.dto.CrearSolicitudRequest;
import com.tuanistack.cranhua.solicitud.dto.ResponderSolicitudRequest;
import com.tuanistack.cranhua.solicitud.dto.SolicitudResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

  @PostMapping
  @PreAuthorize("hasRole('TOSTADOR')")
  public ResponseEntity<SolicitudResponse> crear(
      @Valid @RequestBody CrearSolicitudRequest request, Authentication authentication) {
    SolicitudResponse solicitud =
        solicitudService.crear(request, UUID.fromString(authentication.getName()));
    return ResponseEntity.created(URI.create("/api/solicitudes/" + solicitud.id())).body(solicitud);
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
