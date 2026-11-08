package com.tuanistack.cranhua.finca;

import com.tuanistack.cranhua.finca.dto.AgregarDuenoRequest;
import com.tuanistack.cranhua.finca.dto.CosechaResponse;
import com.tuanistack.cranhua.finca.dto.FincaResponse;
import com.tuanistack.cranhua.finca.dto.RegistrarFincaRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fincas")
public class FincaController {

  private final FincaService fincaService;

  public FincaController(FincaService fincaService) {
    this.fincaService = fincaService;
  }

  @PostMapping
  @PreAuthorize("hasRole('CAFETALERO')")
  public ResponseEntity<FincaResponse> registrar(
      @Valid @RequestBody RegistrarFincaRequest request, Authentication authentication) {
    FincaResponse finca =
        fincaService.registrar(request, UUID.fromString(authentication.getName()));
    return ResponseEntity.created(URI.create("/api/fincas/" + finca.id())).body(finca);
  }

  @GetMapping
  @PreAuthorize("hasRole('CAFETALERO')")
  public List<FincaResponse> misFincas(Authentication authentication) {
    return fincaService.misFincas(UUID.fromString(authentication.getName()));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasRole('CAFETALERO')")
  public FincaResponse obtener(@PathVariable UUID id, Authentication authentication) {
    return fincaService.obtener(id, UUID.fromString(authentication.getName()));
  }

  @GetMapping("/{id}/cosechas")
  @PreAuthorize("hasRole('CAFETALERO')")
  public List<CosechaResponse> cosechas(@PathVariable UUID id, Authentication authentication) {
    return fincaService.cosechas(id, UUID.fromString(authentication.getName()));
  }

  @PostMapping("/{id}/duenos")
  @PreAuthorize("hasRole('CAFETALERO')")
  public FincaResponse agregarDueno(
      @PathVariable UUID id,
      @Valid @RequestBody AgregarDuenoRequest request,
      Authentication authentication) {
    return fincaService.agregarDueno(
        id, request.correo(), UUID.fromString(authentication.getName()));
  }

  @DeleteMapping("/{id}/duenos/{cuentaId}")
  @PreAuthorize("hasRole('CAFETALERO')")
  public FincaResponse quitarDueno(
      @PathVariable UUID id, @PathVariable UUID cuentaId, Authentication authentication) {
    return fincaService.quitarDueno(id, cuentaId, UUID.fromString(authentication.getName()));
  }
}
