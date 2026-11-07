package com.tuanistack.cranhua.auth;

public class CodigoVencidoException extends CodigoInvalidoException {

  public CodigoVencidoException() {
    super("El código venció. Pedí uno nuevo.");
  }
}
