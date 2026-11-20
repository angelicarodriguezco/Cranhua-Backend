package com.tuanistack.cranhua.historial;

import com.tuanistack.cranhua.historial.dto.HistorialCafetaleroResponse;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cafetalero")
public class HistorialCafetaleroController {

  private final HistorialCafetaleroService historialService;

  public HistorialCafetaleroController(HistorialCafetaleroService historialService) {
    this.historialService = historialService;
  }

  @GetMapping("/historial")
  @PreAuthorize("hasRole('CAFETALERO')")
  public HistorialCafetaleroResponse historial(
      @RequestParam(required = false) UUID fincaId,
      @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate desde,
      @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate hasta,
      @RequestParam(defaultValue = "0") int paginaCosechas,
      @RequestParam(defaultValue = "0") int paginaLotes,
      @RequestParam(defaultValue = "10") int tamano,
      Authentication authentication) {
    return historialService.historial(
        UUID.fromString(authentication.getName()),
        fincaId,
        desde,
        hasta,
        paginaCosechas,
        paginaLotes,
        tamano);
  }
}
