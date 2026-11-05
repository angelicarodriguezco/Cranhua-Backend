package com.tuanistack.cranhua.auth;

import com.tuanistack.cranhua.auth.dto.RegistroGoogleRequest;
import com.tuanistack.cranhua.auth.dto.RegistroRequest;
import com.tuanistack.cranhua.cuenta.Cuenta;
import com.tuanistack.cranhua.cuenta.dto.CuentaResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final RegistroService registroService;
  private final SecurityContextRepository repositorioSesion =
      new HttpSessionSecurityContextRepository();

  public AuthController(RegistroService registroService) {
    this.registroService = registroService;
  }

  @PostMapping("/registro")
  @ResponseStatus(HttpStatus.CREATED)
  public CuentaResponse registrar(
      @Valid @RequestBody RegistroRequest request,
      HttpServletRequest httpRequest,
      HttpServletResponse httpResponse) {
    return iniciarSesion(registroService.registrar(request), httpRequest, httpResponse);
  }

  @PostMapping("/registro/google")
  @ResponseStatus(HttpStatus.CREATED)
  public CuentaResponse registrarConGoogle(
      @Valid @RequestBody RegistroGoogleRequest request,
      HttpServletRequest httpRequest,
      HttpServletResponse httpResponse) {
    return iniciarSesion(registroService.registrarConGoogle(request), httpRequest, httpResponse);
  }

  private CuentaResponse iniciarSesion(
      Cuenta cuenta, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
    List<SimpleGrantedAuthority> permisos =
        cuenta.getRoles().stream().map(rol -> new SimpleGrantedAuthority("ROLE_" + rol)).toList();
    SecurityContext contexto = SecurityContextHolder.createEmptyContext();
    contexto.setAuthentication(
        UsernamePasswordAuthenticationToken.authenticated(
            cuenta.getId().toString(), null, permisos));
    SecurityContextHolder.setContext(contexto);

    // Si ya había una sesión anónima, se cambia el id para evitar fijación de sesión
    if (httpRequest.getSession(false) != null) {
      httpRequest.changeSessionId();
    }
    repositorioSesion.saveContext(contexto, httpRequest, httpResponse);
    return CuentaResponse.de(cuenta);
  }
}
