package com.tuanistack.cranhua.pedido;

import com.tuanistack.cranhua.cafeteria.Cafeteria;
import com.tuanistack.cranhua.lote.EstadoTrazabilidad;
import com.tuanistack.cranhua.tueste.Tueste;
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
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pedido")
public class Pedido {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "cafeteria_id", nullable = false, updatable = false)
  private Cafeteria cafeteria;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "tueste_id", nullable = false, updatable = false)
  private Tueste tueste;

  @Column(name = "cantidad_kg", nullable = false, precision = 10, scale = 2, updatable = false)
  private BigDecimal cantidadKg;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private EstadoPedido estado = EstadoPedido.EN_CAMINO;

  @Column(name = "pedido_en", nullable = false, updatable = false)
  private Instant pedidoEn;

  @Column(name = "cerrado_en")
  private Instant cerradoEn;

  protected Pedido() {}

  public Pedido(Cafeteria cafeteria, Tueste tueste, BigDecimal cantidadKg) {
    if (tueste.getLote().getEstadoTrazabilidad() != EstadoTrazabilidad.ACTIVO) {
      throw new IllegalStateException("Solo se pueden pedir tuestes de lotes con código activo.");
    }
    this.cafeteria = cafeteria;
    this.tueste = tueste;
    this.cantidadKg = cantidadKg;
  }

  @PrePersist
  void alCrear() {
    pedidoEn = Instant.now();
  }

  public void entregar() {
    cerrar(EstadoPedido.ENTREGADO);
  }

  public void cancelar() {
    cerrar(EstadoPedido.CANCELADO);
  }

  private void cerrar(EstadoPedido nuevoEstado) {
    if (estado != EstadoPedido.EN_CAMINO) {
      throw new IllegalStateException("El pedido ya fue cerrado.");
    }
    estado = nuevoEstado;
    cerradoEn = Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public Cafeteria getCafeteria() {
    return cafeteria;
  }

  public Tueste getTueste() {
    return tueste;
  }

  public BigDecimal getCantidadKg() {
    return cantidadKg;
  }

  public EstadoPedido getEstado() {
    return estado;
  }

  public Instant getPedidoEn() {
    return pedidoEn;
  }

  public Instant getCerradoEn() {
    return cerradoEn;
  }
}
