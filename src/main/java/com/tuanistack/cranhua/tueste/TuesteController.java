package com.tuanistack.cranhua.tueste;

import com.tuanistack.cranhua.tueste.dto.ActualizarTuesteRequest;
import com.tuanistack.cranhua.tueste.dto.RegistrarTuesteRequest;
import com.tuanistack.cranhua.tueste.dto.TuesteResponse;
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
@RequestMapping("/api/tuestes")
public class TuesteController {

  private final TuesteService tuesteService;

  public TuesteController(TuesteService tuesteService) {
    this.tuesteService = tuesteService;
  }

  @PostMapping
  @PreAuthorize("hasRole('TOSTADOR')")
  public ResponseEntity<TuesteResponse> registrar(
      @Valid @RequestBody RegistrarTuesteRequest request, Authentication authentication) {
    TuesteResponse tueste =
        tuesteService.registrar(request, UUID.fromString(authentication.getName()));
    return ResponseEntity.created(URI.create("/api/tuestes/" + tueste.id())).body(tueste);
  }

  @PatchMapping("/{id}")
  @PreAuthorize("hasRole('TOSTADOR')")
  public TuesteResponse actualizar(
      @PathVariable UUID id,
      @Valid @RequestBody ActualizarTuesteRequest request,
      Authentication authentication) {
    return tuesteService.actualizar(id, request, UUID.fromString(authentication.getName()));
  }
}
