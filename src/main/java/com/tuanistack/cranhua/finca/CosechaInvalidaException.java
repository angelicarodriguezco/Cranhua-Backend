package com.tuanistack.cranhua.finca;

import java.util.Map;

public class CosechaInvalidaException extends RuntimeException {

  private final Map<String, String> errores;

  public CosechaInvalidaException(Map<String, String> errores) {
    super("Revisá los datos ingresados.");
    this.errores = errores;
  }

  public Map<String, String> getErrores() {
    return errores;
  }
}
