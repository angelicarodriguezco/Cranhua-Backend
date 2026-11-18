package com.tuanistack.cranhua.lote;

import com.tuanistack.cranhua.solicitud.EstadoSolicitud;
import com.tuanistack.cranhua.solicitud.Solicitud;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "lote")
public class Lote {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "solicitud_id", nullable = false, unique = true, updatable = false)
  private Solicitud solicitud;

  @Column(nullable = false, unique = true, length = 20, updatable = false)
  private String codigo;

  @Enumerated(EnumType.STRING)
  @Column(name = "estado_trazabilidad", nullable = false, length = 20)
  private EstadoTrazabilidad estadoTrazabilidad = EstadoTrazabilidad.PENDIENTE;

  @Column(name = "activado_en")
  private Instant activadoEn;

  @Column(name = "creado_en", nullable = false, updatable = false)
  private Instant creadoEn;

  protected Lote() {}

  public Lote(Solicitud solicitud, String codigo) {
    if (solicitud.getEstado() != EstadoSolicitud.APROBADA) {
      throw new IllegalStateException("Solo una solicitud aprobada genera un lote.");
    }
    this.solicitud = solicitud;
    this.codigo = codigo;
  }

  public static String formatearCodigo(int anio, long numero) {
    return "CRH-%04d-%04d".formatted(anio, numero);
  }

  @PrePersist
  void alCrear() {
    creadoEn = Instant.now();
  }

  public void activar() {
    if (estadoTrazabilidad == EstadoTrazabilidad.ACTIVO) {
      return;
    }
    estadoTrazabilidad = EstadoTrazabilidad.ACTIVO;
    activadoEn = Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public Solicitud getSolicitud() {
    return solicitud;
  }

  public String getCodigo() {
    return codigo;
  }

  public EstadoTrazabilidad getEstadoTrazabilidad() {
    return estadoTrazabilidad;
  }

  public Instant getActivadoEn() {
    return activadoEn;
  }

  public Instant getCreadoEn() {
    return creadoEn;
  }
}
