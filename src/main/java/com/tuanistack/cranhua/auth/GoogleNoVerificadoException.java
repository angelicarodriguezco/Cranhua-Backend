package com.tuanistack.cranhua.auth;

public class GoogleNoVerificadoException extends RuntimeException {

  public GoogleNoVerificadoException() {
    super("No se pudo verificar la cuenta de Google.");
  }
}
