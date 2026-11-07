package com.tuanistack.cranhua.cuenta;

import com.tuanistack.cranhua.auth.CookieSesion;
import com.tuanistack.cranhua.cuenta.dto.ActualizarCuentaRequest;
import com.tuanistack.cranhua.cuenta.dto.CuentaResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cuenta")
public class CuentaController {

  private final CuentaService cuentaService;
  private final CookieSesion cookieSesion;

  public CuentaController(CuentaService cuentaService, CookieSesion cookieSesion) {
    this.cuentaService = cuentaService;
    this.cookieSesion = cookieSesion;
  }

  @GetMapping({"", "/actual"})
  public CuentaResponse obtener(Authentication authentication) {
    return cuentaService.obtener(idDe(authentication));
  }

  @PatchMapping
  public CuentaResponse actualizar(
      Authentication authentication, @Valid @RequestBody ActualizarCuentaRequest request) {
    return cuentaService.actualizar(idDe(authentication), request);
  }

  @DeleteMapping
  public ResponseEntity<Void> eliminar(Authentication authentication) {
    cuentaService.eliminar(idDe(authentication));
    return ResponseEntity.noContent()
        .header(HttpHeaders.SET_COOKIE, cookieSesion.borrar().toString())
        .build();
  }

  private static UUID idDe(Authentication authentication) {
    return UUID.fromString(authentication.getName());
  }
}
