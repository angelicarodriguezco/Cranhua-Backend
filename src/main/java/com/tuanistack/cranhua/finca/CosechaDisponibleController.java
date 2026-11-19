package com.tuanistack.cranhua.finca;

import com.tuanistack.cranhua.finca.dto.CosechaDisponibleResponse;
import com.tuanistack.cranhua.finca.dto.DetalleCosechaDisponibleResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/cosechas/disponibles")
@PreAuthorize("hasRole('TOSTADOR')")
public class CosechaDisponibleController {

  private final CosechaDisponibleService cosechaDisponibleService;

  public CosechaDisponibleController(CosechaDisponibleService cosechaDisponibleService) {
    this.cosechaDisponibleService = cosechaDisponibleService;
  }

  // La zona viene con el nombre que muestra el frontend, por ejemplo "Tarrazú"
  @GetMapping
  public List<CosechaDisponibleResponse> listar(
      @RequestParam(required = false) String zona,
      @RequestParam(defaultValue = "false") boolean florales,
      @RequestParam(defaultValue = "false") boolean honey,
      Authentication authentication) {
    return cosechaDisponibleService.listar(
        UUID.fromString(authentication.getName()), zona(zona), florales, honey);
  }

  @GetMapping("/{id}")
  public DetalleCosechaDisponibleResponse obtener(
      @PathVariable UUID id, Authentication authentication) {
    return cosechaDisponibleService.obtener(id, UUID.fromString(authentication.getName()));
  }

  private static ZonaCafetalera zona(String nombre) {
    if (nombre == null || nombre.isBlank()) {
      return null;
    }
    try {
      return ZonaCafetalera.desdeNombre(nombre);
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Esa zona cafetalera no existe.");
    }
  }
}
