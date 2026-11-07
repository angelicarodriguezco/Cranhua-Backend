package com.tuanistack.cranhua.auth;

import com.tuanistack.cranhua.cuenta.Cuenta;
import com.tuanistack.cranhua.cuenta.CuentaRepository;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecuperacionContrasenaService {

  static final Duration VIGENCIA = Duration.ofMinutes(10);
  static final int INTENTOS_MAXIMOS = 5;

  private final CuentaRepository cuentaRepository;
  private final CodigoRecuperacionRepository codigoRepository;
  private final PasswordEncoder passwordEncoder;
  private final CorreoRecuperacion correoRecuperacion;
  private final SecureRandom random = new SecureRandom();

  public RecuperacionContrasenaService(
      CuentaRepository cuentaRepository,
      CodigoRecuperacionRepository codigoRepository,
      PasswordEncoder passwordEncoder,
      CorreoRecuperacion correoRecuperacion) {
    this.cuentaRepository = cuentaRepository;
    this.codigoRepository = codigoRepository;
    this.passwordEncoder = passwordEncoder;
    this.correoRecuperacion = correoRecuperacion;
  }

  // El código se genera y cifra aunque el correo no tenga cuenta, para que el tiempo no lo delate
  @Transactional
  public void solicitar(String correo) {
    Optional<Cuenta> cuenta =
        cuentaRepository.findByCorreoAndEliminadaFalse(Cuenta.normalizarCorreo(correo));
    String codigo = "%06d".formatted(random.nextInt(1_000_000));
    String hash = passwordEncoder.encode(codigo);
    if (cuenta.isEmpty()) {
      return;
    }
    codigoRepository.save(
        new CodigoRecuperacion(cuenta.get().getId(), hash, Instant.now().plus(VIGENCIA)));
    correoRecuperacion.enviarCodigo(cuenta.get().getCorreo(), codigo);
  }

  // Los fallos se guardan aunque la respuesta sea un error
  @Transactional(noRollbackFor = CodigoInvalidoException.class)
  public void verificar(String correo, String codigo) {
    validar(correo, codigo);
  }

  @Transactional(noRollbackFor = CodigoInvalidoException.class)
  public void restablecer(String correo, String codigo, String contrasena) {
    CodigoRecuperacion guardado = validar(correo, codigo);
    Cuenta cuenta = cuentaRepository.getReferenceById(guardado.getCuentaId());
    cuenta.cambiarContrasena(passwordEncoder.encode(contrasena));
    codigoRepository.delete(guardado);
  }

  private CodigoRecuperacion validar(String correo, String codigo) {
    CodigoRecuperacion guardado =
        cuentaRepository
            .findByCorreoAndEliminadaFalse(Cuenta.normalizarCorreo(correo))
            .flatMap(cuenta -> codigoRepository.findByCuentaId(cuenta.getId()))
            .orElseThrow(CodigoInvalidoException::new);
    if (guardado.vencido()) {
      codigoRepository.delete(guardado);
      throw new CodigoVencidoException();
    }
    if (!passwordEncoder.matches(codigo, guardado.getCodigoHash())) {
      if (guardado.registrarFallo() >= INTENTOS_MAXIMOS) {
        codigoRepository.delete(guardado);
      }
      throw new CodigoInvalidoException();
    }
    return guardado;
  }
}
