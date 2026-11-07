package com.tuanistack.cranhua.auth;

public class CodigoInvalidoException extends RuntimeException {

  public CodigoInvalidoException() {
    super("El código no es válido o ya venció.");
  }
}
