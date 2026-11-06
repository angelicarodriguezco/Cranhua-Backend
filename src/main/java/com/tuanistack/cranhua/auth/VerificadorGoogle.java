package com.tuanistack.cranhua.auth;

import java.util.Optional;

public interface VerificadorGoogle {

  // Vacío si el token no es válido o el correo no está verificado
  Optional<DatosGoogle> verificar(String credencial);
}
