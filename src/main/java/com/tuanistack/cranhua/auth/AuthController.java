package com.tuanistack.cranhua.auth;

import com.tuanistack.cranhua.auth.dto.IniciarSesionGoogleRequest;
import com.tuanistack.cranhua.auth.dto.IniciarSesionRequest;
import com.tuanistack.cranhua.auth.dto.RegistroGoogleRequest;
import com.tuanistack.cranhua.auth.dto.RegistroRequest;
import com.tuanistack.cranhua.cuenta.Cuenta;
import com.tuanistack.cranhua.cuenta.dto.CuentaResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final RegistroService registroService;
  private final InicioSesionService inicioSesionService;
  private final CookieSesion cookieSesion;

  public AuthController(
      RegistroService registroService,
      InicioSesionService inicioSesionService,
      CookieSesion cookieSesion) {
    this.registroService = registroService;
    this.inicioSesionService = inicioSesionService;
    this.cookieSesion = cookieSesion;
  }

  @PostMapping("/registro")
  public ResponseEntity<CuentaResponse> registrar(@Valid @RequestBody RegistroRequest request) {
    return conSesion(HttpStatus.CREATED, registroService.registrar(request));
  }

  @PostMapping("/registro/google")
  public ResponseEntity<CuentaResponse> registrarConGoogle(
      @Valid @RequestBody RegistroGoogleRequest request) {
    return conSesion(HttpStatus.CREATED, registroService.registrarConGoogle(request));
  }

  @PostMapping("/iniciarSesion")
  public ResponseEntity<CuentaResponse> iniciarSesion(
      @Valid @RequestBody IniciarSesionRequest request) {
    return conSesion(
        HttpStatus.OK, inicioSesionService.iniciarSesion(request.correo(), request.contrasena()));
  }

  @PostMapping("/iniciarSesion/google")
  public ResponseEntity<CuentaResponse> iniciarSesionConGoogle(
      @Valid @RequestBody IniciarSesionGoogleRequest request) {
    return conSesion(
        HttpStatus.OK, inicioSesionService.iniciarSesionConGoogle(request.credencial()));
  }

  @PostMapping("/cerrarSesion")
  public ResponseEntity<Void> cerrarSesion() {
    return ResponseEntity.noContent()
        .header(HttpHeaders.SET_COOKIE, cookieSesion.borrar().toString())
        .build();
  }

  private ResponseEntity<CuentaResponse> conSesion(HttpStatus estado, Cuenta cuenta) {
    return ResponseEntity.status(estado)
        .header(HttpHeaders.SET_COOKIE, cookieSesion.crear(cuenta).toString())
        .body(CuentaResponse.de(cuenta));
  }
}