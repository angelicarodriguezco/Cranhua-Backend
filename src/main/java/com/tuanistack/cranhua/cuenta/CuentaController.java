package com.tuanistack.cranhua.cuenta;

import com.tuanistack.cranhua.cuenta.dto.CuentaResponse;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/cuenta")
public class CuentaController {

  private final CuentaRepository cuentaRepository;

  public CuentaController(CuentaRepository cuentaRepository) {
    this.cuentaRepository = cuentaRepository;
  }

  @GetMapping("/actual")
  @PreAuthorize("hasAnyRole('CAFETALERO', 'TOSTADOR', 'CAFETERIA')")
  public CuentaResponse actual(Authentication authentication) {
    return cuentaRepository
        .findById(UUID.fromString(authentication.getName()))
        .filter(cuenta -> !cuenta.isEliminada())
        .map(CuentaResponse::de)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
  }
}