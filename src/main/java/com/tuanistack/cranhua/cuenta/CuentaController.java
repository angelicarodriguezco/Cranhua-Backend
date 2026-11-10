package com.tuanistack.cranhua.cuenta;

import com.tuanistack.cranhua.auth.CookieSesion;
import com.tuanistack.cranhua.cuenta.dto.ActualizarCuentaRequest;
import com.tuanistack.cranhua.cuenta.dto.CuentaResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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
  @PreAuthorize("hasAnyRole('CAFETALERO', 'TOSTADOR', 'CAFETERIA')")
  public CuentaResponse obtener(Authentication authentication) {
    return cuentaService.obtener(idDe(authentication));
  }

  @PatchMapping
  @PreAuthorize("hasAnyRole('CAFETALERO', 'TOSTADOR', 'CAFETERIA')")
  public CuentaResponse actualizar(
      Authentication authentication, @Valid @RequestBody ActualizarCuentaRequest request) {
    return cuentaService.actualizar(idDe(authentication), request);
  }

  @PostMapping(value = "/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasAnyRole('CAFETALERO', 'TOSTADOR', 'CAFETERIA')")
  public CuentaResponse actualizarFoto(
      Authentication authentication, @RequestPart("archivo") MultipartFile archivo) {
    return cuentaService.actualizarFoto(idDe(authentication), archivo);
  }

  @PostMapping(value = "/banner", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasAnyRole('CAFETALERO', 'TOSTADOR', 'CAFETERIA')")
  public CuentaResponse actualizarBanner(
      Authentication authentication, @RequestPart("archivo") MultipartFile archivo) {
    return cuentaService.actualizarBanner(idDe(authentication), archivo);
  }

  @DeleteMapping
  @PreAuthorize("hasAnyRole('CAFETALERO', 'TOSTADOR', 'CAFETERIA')")
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
