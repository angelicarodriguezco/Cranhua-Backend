package com.tuanistack.cranhua.cafeteria;

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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "item_menu")
public class ItemMenu {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "cafeteria_id", nullable = false, updatable = false)
  private Cafeteria cafeteria;

  @Column(nullable = false, length = 120)
  private String nombre;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 15)
  private CategoriaMenu categoria;

  // Código del lote con que se prepara; solo aplica a ítems de café. La BD valida que exista
  @Column(name = "codigo_trazabilidad", length = 30)
  private String codigoTrazabilidad;

  // En colones
  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal precio;

  @Column(name = "creado_en", nullable = false, updatable = false)
  private Instant creadoEn;

  protected ItemMenu() {}

  public ItemMenu(Cafeteria cafeteria, String nombre, CategoriaMenu categoria, BigDecimal precio) {
    this.cafeteria = cafeteria;
    this.nombre = nombre;
    this.categoria = categoria;
    this.precio = precio;
  }

  @PrePersist
  void alCrear() {
    creadoEn = Instant.now();
    alActualizar();
  }

  @PreUpdate
  void alActualizar() {
    nombre = nombre == null ? null : nombre.strip();
    codigoTrazabilidad =
        codigoTrazabilidad == null || codigoTrazabilidad.isBlank()
            ? null
            : codigoTrazabilidad.strip().toUpperCase(Locale.ROOT);
  }

  public UUID getId() {
    return id;
  }

  public Cafeteria getCafeteria() {
    return cafeteria;
  }

  public String getNombre() {
    return nombre;
  }

  public void setNombre(String nombre) {
    this.nombre = nombre;
  }

  public CategoriaMenu getCategoria() {
    return categoria;
  }

  public void setCategoria(CategoriaMenu categoria) {
    if (categoria != CategoriaMenu.CAFE && codigoTrazabilidad != null) {
      throw new IllegalArgumentException("Solo los ítems de café llevan código de trazabilidad.");
    }
    this.categoria = categoria;
  }

  public String getCodigoTrazabilidad() {
    return codigoTrazabilidad;
  }

  public void setCodigoTrazabilidad(String codigoTrazabilidad) {
    if (codigoTrazabilidad != null && categoria != CategoriaMenu.CAFE) {
      throw new IllegalArgumentException("Solo los ítems de café llevan código de trazabilidad.");
    }
    this.codigoTrazabilidad = codigoTrazabilidad;
  }

  public BigDecimal getPrecio() {
    return precio;
  }

  public void setPrecio(BigDecimal precio) {
    this.precio = precio;
  }

  public Instant getCreadoEn() {
    return creadoEn;
  }
}
