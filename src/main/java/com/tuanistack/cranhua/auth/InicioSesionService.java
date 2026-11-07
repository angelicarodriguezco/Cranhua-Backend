package com.tuanistack.cranhua.auth;

import com.tuanistack.cranhua.cuenta.Cuenta;
import com.tuanistack.cranhua.cuenta.CuentaRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class InicioSesionService {

  private final CuentaRepository cuentaRepository;
  private final PasswordEncoder passwordEncoder;
  private final VerificadorGoogle verificadorGoogle;

  // Si el correo no existe se compara contra este hash igual, para que el tiempo de respuesta
  // no delate qué correos tienen cuenta
  private final String hashDeRelleno;

  public InicioSesionService(
      CuentaRepository cuentaRepository,
      PasswordEncoder passwordEncoder,
      VerificadorGoogle verificadorGoogle) {
    this.cuentaRepository = cuentaRepository;
    this.passwordEncoder = passwordEncoder;
    this.verificadorGoogle = verificadorGoogle;
    this.hashDeRelleno = passwordEncoder.encode(UUID.randomUUID().toString());
  }

  // Las cuentas eliminadas y las que solo entran con Google fallan igual que una clave equivocada
  public Cuenta iniciarSesion(String correo, String contrasena) {
    Optional<Cuenta> cuenta =
        cuentaRepository.findByCorreoAndEliminadaFalse(Cuenta.normalizarCorreo(correo));
    String hash = cuenta.map(Cuenta::getContrasenaHash).orElse(null);
    boolean coincide = passwordEncoder.matches(contrasena, hash == null ? hashDeRelleno : hash);
    if (hash == null || !coincide) {
      throw new CredencialesInvalidasException();
    }
    return cuenta.get();
  }

  public Cuenta iniciarSesionConGoogle(String credencial) {
    DatosGoogle datos =
        verificadorGoogle.verificar(credencial).orElseThrow(GoogleNoVerificadoException::new);
    return cuentaRepository
        .findByGoogleIdAndEliminadaFalse(datos.googleId())
        .orElseThrow(CuentaGoogleNoRegistradaException::new);
  }
}
