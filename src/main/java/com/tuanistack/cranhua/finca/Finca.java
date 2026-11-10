package com.tuanistack.cranhua.finca;

import com.tuanistack.cranhua.cuenta.Cuenta;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

@Entity
@Table(name = "finca")
public class Finca {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, length = 120)
  private String nombre;

  @Column(length = 300)
  private String descripcion;

  @Column(name = "zona_cafetalera", length = 30)
  private ZonaCafetalera zonaCafetalera;

  // Los dos van en SRID 4326 (lat/lng de Google Maps)
  @Column(nullable = false)
  private Polygon perimetro;

  @Column(nullable = false)
  private Point ubicacion;

  @Column(nullable = false, length = 60)
  private String canton;

  @Column(nullable = false, length = 30)
  private String provincia;

  @Column(nullable = false)
  private boolean eliminada = false;

  @Column(name = "eliminada_en")
  private Instant eliminadaEn;

  @Column(name = "creada_en", nullable = false, updatable = false)
  private Instant creadaEn;

  @ManyToMany
  @JoinTable(
      name = "finca_dueno",
      joinColumns = @JoinColumn(name = "finca_id"),
      inverseJoinColumns = @JoinColumn(name = "cuenta_id"))
  private Set<Cuenta> duenos = new HashSet<>();

  protected Finca() {}

  public Finca(String nombre, Polygon perimetro, Point ubicacion, String canton, String provincia) {
    this.nombre = nombre;
    this.perimetro = perimetro;
    this.ubicacion = ubicacion;
    this.canton = canton;
    this.provincia = provincia;
  }

  private static String limpiar(String texto) {
    return texto == null ? null : texto.strip();
  }

  @PrePersist
  void alCrear() {
    creadaEn = Instant.now();
    alActualizar();
  }

  @PreUpdate
  void alActualizar() {
    nombre = limpiar(nombre);
    descripcion = limpiar(descripcion);
    canton = limpiar(canton);
    provincia = limpiar(provincia);
  }

  public void agregarDueno(Cuenta cuenta) {
    duenos.add(cuenta);
  }

  public void quitarDueno(UUID cuentaId) {
    if (esDueno(cuentaId) && duenos.size() == 1) {
      throw new UltimoDuenoException();
    }
    duenos.removeIf(cuenta -> cuenta.getId().equals(cuentaId));
  }

  public boolean esDueno(UUID cuentaId) {
    return duenos.stream().anyMatch(cuenta -> cuenta.getId().equals(cuentaId));
  }

  // Borrado lógico: la finca y sus cosechas se conservan
  public void eliminar() {
    eliminada = true;
    eliminadaEn = Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public String getNombre() {
    return nombre;
  }

  public void setNombre(String nombre) {
    this.nombre = nombre;
  }

  public String getDescripcion() {
    return descripcion;
  }

  public void setDescripcion(String descripcion) {
    this.descripcion = descripcion;
  }

  public ZonaCafetalera getZonaCafetalera() {
    return zonaCafetalera;
  }

  public void setZonaCafetalera(ZonaCafetalera zonaCafetalera) {
    this.zonaCafetalera = zonaCafetalera;
  }

  public Polygon getPerimetro() {
    return perimetro;
  }

  public void setPerimetro(Polygon perimetro) {
    this.perimetro = perimetro;
  }

  public Point getUbicacion() {
    return ubicacion;
  }

  public void setUbicacion(Point ubicacion) {
    this.ubicacion = ubicacion;
  }

  public String getCanton() {
    return canton;
  }

  public void setCanton(String canton) {
    this.canton = canton;
  }

  public String getProvincia() {
    return provincia;
  }

  public void setProvincia(String provincia) {
    this.provincia = provincia;
  }

  public boolean isEliminada() {
    return eliminada;
  }

  public Instant getEliminadaEn() {
    return eliminadaEn;
  }

  public Instant getCreadaEn() {
    return creadaEn;
  }

  public Set<Cuenta> getDuenos() {
    return duenos;
  }
}
