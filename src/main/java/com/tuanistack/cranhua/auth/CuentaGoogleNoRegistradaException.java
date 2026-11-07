package com.tuanistack.cranhua.auth;

public class CuentaGoogleNoRegistradaException extends RuntimeException {

  public CuentaGoogleNoRegistradaException() {
    super("No hay una cuenta registrada con esa cuenta de Google.");
  }
}
