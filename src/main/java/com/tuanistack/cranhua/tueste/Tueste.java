package com.tuanistack.cranhua.tueste;

import com.tuanistack.cranhua.lote.Lote;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "tueste")
public class Tueste {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "lote_id", nullable = false, updatable = false)
  private Lote lote;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private NivelTueste nivel;

  @Column(name = "peso_verde_kg", nullable = false, precision = 10, scale = 2)
  private BigDecimal pesoVerdeKg;

  @Column(name = "peso_tostado_kg", nullable = false, precision = 10, scale = 2)
  private BigDecimal pesoTostadoKg;

  @Column(name = "fecha_tueste", nullable = false)
  private LocalDate fechaTueste;

  @Column(length = 500)
  private String notas;

  @Column(name = "creado_en", nullable = false, updatable = false)
  private Instant creadoEn;

  protected Tueste() {}

  public Tueste(
      Lote lote,
      NivelTueste nivel,
      BigDecimal pesoVerdeKg,
      BigDecimal pesoTostadoKg,
      LocalDate fechaTueste) {
    this.lote = lote;
    this.nivel = nivel;
    this.pesoVerdeKg = pesoVerdeKg;
    this.pesoTostadoKg = pesoTostadoKg;
    this.fechaTueste = fechaTueste;
  }

  @PrePersist
  void alCrear() {
    creadoEn = Instant.now();
  }

  public BigDecimal getMermaPorcentaje() {
    return pesoVerdeKg
        .subtract(pesoTostadoKg)
        .multiply(BigDecimal.valueOf(100))
        .divide(pesoVerdeKg, 2, RoundingMode.HALF_UP);
  }

  public UUID getId() {
    return id;
  }

  public Lote getLote() {
    return lote;
  }

  public NivelTueste getNivel() {
    return nivel;
  }

  public BigDecimal getPesoVerdeKg() {
    return pesoVerdeKg;
  }

  public BigDecimal getPesoTostadoKg() {
    return pesoTostadoKg;
  }

  public LocalDate getFechaTueste() {
    return fechaTueste;
  }

  public String getNotas() {
    return notas;
  }

  public void setNotas(String notas) {
    this.notas = notas;
  }

  public Instant getCreadoEn() {
    return creadoEn;
  }
}
