package com.tuanistack.cranhua.finca;

import com.tuanistack.cranhua.finca.dto.CosechaResponse;
import com.tuanistack.cranhua.finca.dto.RegistrarCosechaRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cosechas")
public class CosechaController {

  private final CosechaService cosechaService;

  public CosechaController(CosechaService cosechaService) {
    this.cosechaService = cosechaService;
  }

  @PostMapping
  @PreAuthorize("hasRole('CAFETALERO')")
  public ResponseEntity<CosechaResponse> registrar(
      @Valid @RequestBody RegistrarCosechaRequest request, Authentication authentication) {
    CosechaResponse cosecha =
        cosechaService.registrar(request, UUID.fromString(authentication.getName()));
    return ResponseEntity.created(URI.create("/api/cosechas/" + cosecha.id())).body(cosecha);
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasRole('CAFETALERO')")
  public CosechaResponse obtener(@PathVariable UUID id, Authentication authentication) {
    return cosechaService.obtener(id, UUID.fromString(authentication.getName()));
  }
}
