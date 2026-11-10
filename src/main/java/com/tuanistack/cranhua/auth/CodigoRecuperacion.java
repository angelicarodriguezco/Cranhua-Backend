package com.tuanistack.cranhua.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

// Una cuenta tiene a lo sumo un código vigente: pedir otro reemplaza al anterior
@Entity
@Table(name = "codigo_recuperacion")
public class CodigoRecuperacion {

  @Id
  @Column(name = "cuenta_id")
  private UUID cuentaId;

  @Column(name = "codigo_hash", nullable = false, length = 100)
  private String codigoHash;

  @Column(name = "intentos_fallidos", nullable = false)
  private int intentosFallidos;

  @Column(name = "vence_en", nullable = false)
  private Instant venceEn;

  protected CodigoRecuperacion() {}

  public CodigoRecuperacion(UUID cuentaId, String codigoHash, Instant venceEn) {
    this.cuentaId = cuentaId;
    this.codigoHash = codigoHash;
    this.venceEn = venceEn;
  }

  public boolean vencido() {
    return !Instant.now().isBefore(venceEn);
  }

  public int registrarFallo() {
    return ++intentosFallidos;
  }

  public UUID getCuentaId() {
    return cuentaId;
  }

  public String getCodigoHash() {
    return codigoHash;
  }

  public int getIntentosFallidos() {
    return intentosFallidos;
  }
}
