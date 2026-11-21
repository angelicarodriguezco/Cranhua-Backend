package com.tuanistack.cranhua.tueste;

import java.util.Map;

public class TuesteInvalidoException extends RuntimeException {

  private final Map<String, String> errores;

  public TuesteInvalidoException(Map<String, String> errores) {
    super("Revisá los datos ingresados.");
    this.errores = errores;
  }

  public Map<String, String> getErrores() {
    return errores;
  }
}
