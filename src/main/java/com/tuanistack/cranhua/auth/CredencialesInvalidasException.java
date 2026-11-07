package com.tuanistack.cranhua.auth;

public class CredencialesInvalidasException extends RuntimeException {

  public CredencialesInvalidasException() {
    super("Correo o contraseña incorrectos.");
  }
}
