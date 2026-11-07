package com.tuanistack.cranhua.finca;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class ProcesoReparto {

  @Column(nullable = false, length = 20)
  private Proceso proceso;

  @Column(nullable = false)
  private Integer porcentaje;

  protected ProcesoReparto() {}

  public ProcesoReparto(Proceso proceso, Integer porcentaje) {
    this.proceso = proceso;
    this.porcentaje = porcentaje;
  }

  public Proceso getProceso() {
    return proceso;
  }

  public Integer getPorcentaje() {
    return porcentaje;
  }
}
