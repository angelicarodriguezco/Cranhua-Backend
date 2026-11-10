package com.tuanistack.cranhua.auth;

public class CodigoInvalidoException extends RuntimeException {

  public CodigoInvalidoException() {
    this("El código es incorrecto.");
  }

  protected CodigoInvalidoException(String mensaje) {
    super(mensaje);
  }
}
