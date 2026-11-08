package com.tuanistack.cranhua.tueste;

import com.tuanistack.cranhua.tueste.dto.ActualizarNegocioRequest;
import com.tuanistack.cranhua.tueste.dto.NegocioTuesteResponse;
import com.tuanistack.cranhua.tueste.dto.RegistrarNegocioRequest;
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
@RequestMapping("/api/tostadores")
public class NegocioTuesteController {

  private final NegocioTuesteService negocioService;

  public NegocioTuesteController(NegocioTuesteService negocioService) {
    this.negocioService = negocioService;
  }

  @PostMapping
  @PreAuthorize("hasRole('TOSTADOR')")
  public ResponseEntity<NegocioTuesteResponse> registrar(
      @Valid @RequestBody RegistrarNegocioRequest request, Authentication authentication) {
    NegocioTuesteResponse negocio = negocioService.registrar(request, idDe(authentication));
    return ResponseEntity.created(URI.create("/api/tostadores/" + negocio.id())).body(negocio);
  }

  @GetMapping("/actual")
  @PreAuthorize("hasRole('TOSTADOR')")
  public NegocioTuesteResponse actual(Authentication authentication) {
    return negocioService.actual(idDe(authentication));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('CAFETALERO', 'TOSTADOR', 'CAFETERIA')")
  public NegocioTuesteResponse obtener(@PathVariable UUID id) {
    return negocioService.obtener(id);
  }

  @PatchMapping("/{id}")
  @PreAuthorize("hasRole('TOSTADOR')")
  public NegocioTuesteResponse actualizar(
      @PathVariable UUID id,
      @Valid @RequestBody ActualizarNegocioRequest request,
      Authentication authentication) {
    return negocioService.actualizar(id, request, idDe(authentication));
  }

  private static UUID idDe(Authentication authentication) {
    return UUID.fromString(authentication.getName());
  }
}
