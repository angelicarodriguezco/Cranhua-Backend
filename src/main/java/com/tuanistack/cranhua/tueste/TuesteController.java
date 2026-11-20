package com.tuanistack.cranhua.tueste;

import com.tuanistack.cranhua.tueste.dto.ActualizarTuesteRequest;
import com.tuanistack.cranhua.tueste.dto.TuesteResponse;
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
@RequestMapping("/api/tuestes")
public class TuesteController {

  private final TuesteService tuesteService;

  public TuesteController(TuesteService tuesteService) {
    this.tuesteService = tuesteService;
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
