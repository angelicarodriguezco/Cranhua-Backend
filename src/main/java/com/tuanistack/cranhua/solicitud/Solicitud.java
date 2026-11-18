package com.tuanistack.cranhua.solicitud;

import com.tuanistack.cranhua.cuenta.Cuenta;
import com.tuanistack.cranhua.finca.Cosecha;
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
import java.util.UUID;

@Entity
@Table(name = "solicitud")
public class Solicitud {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "cosecha_id", nullable = false, updatable = false)
  private Cosecha cosecha;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "tostador_id", nullable = false, updatable = false)
  private Cuenta tostador;

  @Column(name = "cantidad_kg", nullable = false, precision = 10, scale = 2)
  private BigDecimal cantidadKg;

  // Precio de la cosecha al momento de solicitar; no cambia si la cosecha cambia de precio
  @Column(name = "precio_kg", nullable = false, precision = 12, scale = 2)
  private BigDecimal precioKg;

  @Column(name = "total_estimado", nullable = false, precision = 14, scale = 2)
  private BigDecimal totalEstimado;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private EstadoSolicitud estado = EstadoSolicitud.PENDIENTE;

  @Column(name = "solicitada_en", nullable = false, updatable = false)
  private Instant solicitadaEn;

  @Column(name = "respondida_en")
  private Instant respondidaEn;

  protected Solicitud() {}

  public Solicitud(Cosecha cosecha, Cuenta tostador, BigDecimal cantidadKg) {
    this.cosecha = cosecha;
    this.tostador = tostador;
    this.cantidadKg = cantidadKg;
    this.precioKg = cosecha.getPrecioKg();
    this.totalEstimado = cantidadKg.multiply(precioKg).setScale(2, RoundingMode.HALF_UP);
  }

  @PrePersist
  void alCrear() {
    solicitadaEn = Instant.now();
  }

  public void aprobar() {
    responder(EstadoSolicitud.APROBADA);
  }

  public void rechazar() {
    responder(EstadoSolicitud.RECHAZADA);
  }

  private void responder(EstadoSolicitud nuevoEstado) {
    if (estado != EstadoSolicitud.PENDIENTE) {
      throw new IllegalStateException("La solicitud ya fue respondida.");
    }
    estado = nuevoEstado;
    respondidaEn = Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public Cosecha getCosecha() {
    return cosecha;
  }

  public Cuenta getTostador() {
    return tostador;
  }

  public BigDecimal getCantidadKg() {
    return cantidadKg;
  }

  public BigDecimal getPrecioKg() {
    return precioKg;
  }

  public BigDecimal getTotalEstimado() {
    return totalEstimado;
  }

  public EstadoSolicitud getEstado() {
    return estado;
  }

  public Instant getSolicitadaEn() {
    return solicitadaEn;
  }

  public Instant getRespondidaEn() {
    return respondidaEn;
  }
}
