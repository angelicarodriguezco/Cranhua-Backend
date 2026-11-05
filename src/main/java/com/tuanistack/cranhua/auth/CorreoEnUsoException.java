package com.tuanistack.cranhua.auth;

public class CorreoEnUsoException extends RuntimeException {

  public CorreoEnUsoException() {
    super("Ya existe una cuenta con ese correo.");
  }
}
