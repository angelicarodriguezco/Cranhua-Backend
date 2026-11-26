package com.tuanistack.cranhua.tueste;

import com.tuanistack.cranhua.tueste.dto.ActualizarNegocioRequest;
import com.tuanistack.cranhua.tueste.dto.DetalleTostadorExploracionResponse;
import com.tuanistack.cranhua.tueste.dto.NegocioTuesteResponse;
import com.tuanistack.cranhua.tueste.dto.RegistrarNegocioRequest;
import com.tuanistack.cranhua.tueste.dto.TostadorExploracionResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/tostadores")
public class NegocioTuesteController {

  private final NegocioTuesteService negocioService;
  private final TostadorExploracionService exploracionService;

  public NegocioTuesteController(
      NegocioTuesteService negocioService, TostadorExploracionService exploracionService) {
    this.negocioService = negocioService;
    this.exploracionService = exploracionService;
  }

  @PostMapping
  @PreAuthorize("hasRole('TOSTADOR')")
  public ResponseEntity<NegocioTuesteResponse> registrar(
      @Valid @RequestBody RegistrarNegocioRequest request, Authentication authentication) {
    NegocioTuesteResponse negocio = negocioService.registrar(request, idDe(authentication));
    return ResponseEntity.created(URI.create("/api/tostadores/" + negocio.id())).body(negocio);
  }

  @GetMapping
  @PreAuthorize("hasRole('CAFETERIA')")
  public List<TostadorExploracionResponse> listar(
      @RequestParam(required = false) String estilo,
      @RequestParam(defaultValue = "false") boolean disponible) {
    return exploracionService.listar(estilo(estilo), disponible);
  }

  @GetMapping("/actual")
  @PreAuthorize("hasRole('TOSTADOR')")
  public NegocioTuesteResponse actual(Authentication authentication) {
    return negocioService.actual(idDe(authentication));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('CAFETERIA', 'TOSTADOR')")
  public DetalleTostadorExploracionResponse obtener(
      @PathVariable UUID id, Authentication authentication) {
    return exploracionService.obtener(id, idDe(authentication), esCafeteria(authentication));
  }

  @PatchMapping("/{id}")
  @PreAuthorize("hasRole('TOSTADOR')")
  public NegocioTuesteResponse actualizar(
      @PathVariable UUID id,
      @Valid @RequestBody ActualizarNegocioRequest request,
      Authentication authentication) {
    return negocioService.actualizar(id, request, idDe(authentication));
  }

  @PostMapping(value = "/{id}/portada", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasRole('TOSTADOR')")
  public NegocioTuesteResponse actualizarPortada(
      @PathVariable UUID id,
      @RequestPart("archivo") MultipartFile archivo,
      Authentication authentication) {
    return negocioService.actualizarPortada(id, archivo, idDe(authentication));
  }

  private static UUID idDe(Authentication authentication) {
    return UUID.fromString(authentication.getName());
  }

  private static boolean esCafeteria(Authentication authentication) {
    return authentication.getAuthorities().stream()
        .anyMatch(a -> "ROLE_CAFETERIA".equals(a.getAuthority()));
  }

  private static NivelTueste estilo(String valor) {
    if (valor == null || valor.isBlank()) {
      return null;
    }
    try {
      return NivelTueste.valueOf(valor.strip().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ese estilo de tueste no existe.");
    }
  }
}
