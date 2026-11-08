package com.tuanistack.cranhua.finca;

public class UltimoDuenoException extends RuntimeException {

  public UltimoDuenoException() {
    super("No se puede quitar al último dueño de la finca.");
  }
}
