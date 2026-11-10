package com.tuanistack.cranhua.cafeteria;

import com.tuanistack.cranhua.cafeteria.dto.ActualizarCafeteriaRequest;
import com.tuanistack.cranhua.cafeteria.dto.CafeteriaResponse;
import com.tuanistack.cranhua.cafeteria.dto.RegistrarCafeteriaRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cafeterias")
public class CafeteriaController {

  private final CafeteriaService cafeteriaService;

  public CafeteriaController(CafeteriaService cafeteriaService) {
    this.cafeteriaService = cafeteriaService;
  }

  @PostMapping
  @PreAuthorize("hasRole('CAFETERIA')")
  public ResponseEntity<CafeteriaResponse> registrar(
      @Valid @RequestBody RegistrarCafeteriaRequest request, Authentication authentication) {
    CafeteriaResponse cafeteria = cafeteriaService.registrar(request, idDe(authentication));
    return ResponseEntity.created(URI.create("/api/cafeterias/" + cafeteria.id())).body(cafeteria);
  }

  @GetMapping("/actual")
  @PreAuthorize("hasRole('CAFETERIA')")
  public CafeteriaResponse actual(Authentication authentication) {
    return cafeteriaService.actual(idDe(authentication));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasRole('CAFETERIA')")
  public CafeteriaResponse obtener(@PathVariable UUID id, Authentication authentication) {
    return cafeteriaService.obtener(id, idDe(authentication));
  }

  @PatchMapping("/{id}")
  @PreAuthorize("hasRole('CAFETERIA')")
  public CafeteriaResponse actualizar(
      @PathVariable UUID id,
      @Valid @RequestBody ActualizarCafeteriaRequest request,
      Authentication authentication) {
    return cafeteriaService.actualizar(id, request, idDe(authentication));
  }

  private static UUID idDe(Authentication authentication) {
    return UUID.fromString(authentication.getName());
  }
}
