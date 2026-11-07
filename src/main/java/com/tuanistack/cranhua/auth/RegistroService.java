package com.tuanistack.cranhua.auth;

import com.tuanistack.cranhua.auth.dto.RegistroGoogleRequest;
import com.tuanistack.cranhua.auth.dto.RegistroRequest;
import com.tuanistack.cranhua.cuenta.Cuenta;
import com.tuanistack.cranhua.cuenta.CuentaRepository;
import com.tuanistack.cranhua.cuenta.Rol;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class RegistroService {

  private final CuentaRepository cuentaRepository;
  private final PasswordEncoder passwordEncoder;
  private final VerificadorGoogle verificadorGoogle;

  public RegistroService(
      CuentaRepository cuentaRepository,
      PasswordEncoder passwordEncoder,
      VerificadorGoogle verificadorGoogle) {
    this.cuentaRepository = cuentaRepository;
    this.passwordEncoder = passwordEncoder;
    this.verificadorGoogle = verificadorGoogle;
  }

  public Cuenta registrar(RegistroRequest request) {
    String correo = Cuenta.normalizarCorreo(request.correo());
    validarCorreoLibre(correo);

    Cuenta cuenta =
        new Cuenta(
            request.nombreCompleto().strip(),
            correo,
            passwordEncoder.encode(request.contrasena()),
            aRoles(request.roles()));
    if (request.telefono() != null && !request.telefono().isBlank()) {
      cuenta.setTelefono(request.telefono().strip());
    }
    return guardar(cuenta);
  }

  public Cuenta registrarConGoogle(RegistroGoogleRequest request) {
    DatosGoogle datos =
        verificadorGoogle
            .verificar(request.credencial())
            .orElseThrow(GoogleNoVerificadoException::new);
    String correo = Cuenta.normalizarCorreo(datos.correo());
    validarCorreoLibre(correo);

    String nombre =
        datos.nombre() == null || datos.nombre().isBlank()
            ? correo.substring(0, correo.indexOf('@'))
            : datos.nombre().strip();
    return guardar(
        Cuenta.deGoogle(
            nombre, correo, datos.googleId(), datos.fotoUrl(), aRoles(request.roles())));
  }

  // Incluye cuentas eliminadas: el correo no se puede reutilizar
  private void validarCorreoLibre(String correo) {
    if (cuentaRepository.existsByCorreo(correo)) {
      throw new CorreoEnUsoException();
    }
  }

  // Si dos registros con el mismo correo llegan a la vez, el único de la base corta el segundo
  private Cuenta guardar(Cuenta cuenta) {
    try {
      return cuentaRepository.saveAndFlush(cuenta);
    } catch (DataIntegrityViolationException e) {
      throw new CorreoEnUsoException();
    }
  }

  private Set<Rol> aRoles(List<String> roles) {
    return roles.stream().map(Rol::valueOf).collect(Collectors.toSet());
  }
}
