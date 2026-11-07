package com.tuanistack.cranhua.finca;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "cosecha")
public class Cosecha {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "finca_id", nullable = false)
  private Finca finca;

  @Column(nullable = false, length = 60)
  private String variedad;

  @Column(name = "altura_msnm", nullable = false)
  private Integer alturaMsnm;

  @Column(name = "fecha_inicio", nullable = false)
  private LocalDate fechaInicio;

  @Column(name = "fecha_fin", nullable = false)
  private LocalDate fechaFin;

  @Column(name = "cantidad_cosechada_kg", nullable = false, precision = 10, scale = 2)
  private BigDecimal cantidadCosechadaKg;

  @Column(name = "peso_tras_beneficiado_kg", precision = 10, scale = 2)
  private BigDecimal pesoTrasBeneficiadoKg;

  @Column(name = "peso_tras_seleccion_kg", precision = 10, scale = 2)
  private BigDecimal pesoTrasSeleccionKg;

  @Column(name = "caracolillo_kg", precision = 10, scale = 2)
  private BigDecimal caracolilloKg;

  @Column(name = "notas_sabor", length = 500)
  private String notasSabor;

  @Column(name = "puntaje_sca", precision = 5, scale = 2)
  private BigDecimal puntajeSca;

  @Column(name = "precio_kg", nullable = false, precision = 12, scale = 2)
  private BigDecimal precioKg;

  @Column(name = "kg_disponibles", precision = 10, scale = 2)
  private BigDecimal kgDisponibles;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private EstadoCosecha estado = EstadoCosecha.EN_BENEFICIADO;

  @Column(name = "codigo_lote", unique = true, length = 20)
  private String codigoLote;

  @Column(name = "creada_en", nullable = false, updatable = false)
  private Instant creadaEn;

  @ElementCollection
  @CollectionTable(name = "cosecha_proceso", joinColumns = @JoinColumn(name = "cosecha_id"))
  private List<ProcesoReparto> procesos = new ArrayList<>();

  protected Cosecha() {}

  public Cosecha(
      Finca finca,
      String variedad,
      Integer alturaMsnm,
      LocalDate fechaInicio,
      LocalDate fechaFin,
      BigDecimal cantidadCosechadaKg,
      BigDecimal precioKg,
      List<ProcesoReparto> procesos) {
    this.finca = finca;
    this.variedad = variedad;
    this.alturaMsnm = alturaMsnm;
    this.fechaInicio = fechaInicio;
    this.fechaFin = fechaFin;
    this.cantidadCosechadaKg = cantidadCosechadaKg;
    this.precioKg = precioKg;
    setProcesos(procesos);
  }

  @PrePersist
  void alCrear() {
    creadaEn = Instant.now();
    alActualizar();
  }

  @PreUpdate
  void alActualizar() {
    variedad = variedad == null ? null : variedad.strip();
    codigoLote = codigoLote == null ? null : codigoLote.strip().toUpperCase(Locale.ROOT);
  }

  public void setProcesos(List<ProcesoReparto> procesos) {
    int total = procesos.stream().mapToInt(ProcesoReparto::getPorcentaje).sum();
    if (total != 100) {
      throw new IllegalArgumentException("Los porcentajes de los procesos tienen que sumar 100.");
    }
    this.procesos.clear();
    this.procesos.addAll(procesos);
  }

  // Al terminar la selección, lo que queda es lo que se puede vender
  public void registrarPesoTrasSeleccion(BigDecimal kg) {
    pesoTrasSeleccionKg = kg;
    kgDisponibles = kg;
    estado = EstadoCosecha.DISPONIBLE;
  }

  public void descontarDisponible(BigDecimal kg) {
    if (kg == null || kg.signum() <= 0) {
      throw new IllegalArgumentException("Los kg a descontar tienen que ser mayores a 0.");
    }
    if (kgDisponibles == null || kg.compareTo(kgDisponibles) > 0) {
      throw new IllegalStateException("No hay suficientes kg disponibles.");
    }
    kgDisponibles = kgDisponibles.subtract(kg);
  }

  public UUID getId() {
    return id;
  }

  public Finca getFinca() {
    return finca;
  }

  public String getVariedad() {
    return variedad;
  }

  public void setVariedad(String variedad) {
    this.variedad = variedad;
  }

  public Integer getAlturaMsnm() {
    return alturaMsnm;
  }

  public void setAlturaMsnm(Integer alturaMsnm) {
    this.alturaMsnm = alturaMsnm;
  }

  public LocalDate getFechaInicio() {
    return fechaInicio;
  }

  public void setFechaInicio(LocalDate fechaInicio) {
    this.fechaInicio = fechaInicio;
  }

  public LocalDate getFechaFin() {
    return fechaFin;
  }

  public void setFechaFin(LocalDate fechaFin) {
    this.fechaFin = fechaFin;
  }

  public BigDecimal getCantidadCosechadaKg() {
    return cantidadCosechadaKg;
  }

  public void setCantidadCosechadaKg(BigDecimal cantidadCosechadaKg) {
    this.cantidadCosechadaKg = cantidadCosechadaKg;
  }

  public BigDecimal getPesoTrasBeneficiadoKg() {
    return pesoTrasBeneficiadoKg;
  }

  public void setPesoTrasBeneficiadoKg(BigDecimal pesoTrasBeneficiadoKg) {
    this.pesoTrasBeneficiadoKg = pesoTrasBeneficiadoKg;
  }

  public BigDecimal getPesoTrasSeleccionKg() {
    return pesoTrasSeleccionKg;
  }

  public BigDecimal getCaracolilloKg() {
    return caracolilloKg;
  }

  public void setCaracolilloKg(BigDecimal caracolilloKg) {
    this.caracolilloKg = caracolilloKg;
  }

  public String getNotasSabor() {
    return notasSabor;
  }

  public void setNotasSabor(String notasSabor) {
    this.notasSabor = notasSabor;
  }

  public BigDecimal getPuntajeSca() {
    return puntajeSca;
  }

  public void setPuntajeSca(BigDecimal puntajeSca) {
    this.puntajeSca = puntajeSca;
  }

  public BigDecimal getPrecioKg() {
    return precioKg;
  }

  public void setPrecioKg(BigDecimal precioKg) {
    this.precioKg = precioKg;
  }

  public BigDecimal getKgDisponibles() {
    return kgDisponibles;
  }

  public EstadoCosecha getEstado() {
    return estado;
  }

  public String getCodigoLote() {
    return codigoLote;
  }

  public void setCodigoLote(String codigoLote) {
    this.codigoLote = codigoLote;
  }

  public Instant getCreadaEn() {
    return creadaEn;
  }

  public List<ProcesoReparto> getProcesos() {
    return procesos;
  }
}
