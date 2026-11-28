package com.tuanistack.cranhua.finca;

import java.util.Map;

public class CosechaBloqueadaException extends RuntimeException {

  private final Map<String, String> errores;

  public CosechaBloqueadaException(String mensaje, Map<String, String> errores) {
    super(mensaje);
    this.errores = errores;
  }

  public Map<String, String> getErrores() {
    return errores;
  }
}
