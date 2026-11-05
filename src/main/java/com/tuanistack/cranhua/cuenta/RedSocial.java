package com.tuanistack.cranhua.cuenta;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Embeddable
public class RedSocial {

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private TipoRedSocial tipo;

  @Column(nullable = false, length = 200)
  private String valor;

  protected RedSocial() {}

  public RedSocial(TipoRedSocial tipo, String valor) {
    this.tipo = tipo;
    this.valor = valor;
  }

  public TipoRedSocial getTipo() {
    return tipo;
  }

  public String getValor() {
    return valor;
  }
}
