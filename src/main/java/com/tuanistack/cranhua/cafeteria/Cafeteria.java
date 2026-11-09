package com.tuanistack.cranhua.cafeteria;

import com.tuanistack.cranhua.cuenta.Cuenta;
import com.tuanistack.cranhua.cuenta.Rol;
import com.tuanistack.cranhua.tueste.NivelTueste;
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
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import org.locationtech.jts.geom.Point;

@Entity
@Table(name = "cafeteria")
public class Cafeteria {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  // ManyToOne porque una cuenta puede tener cafeterías eliminadas; la activa es única en la BD
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "cuenta_id", nullable = false, updatable = false)
  private Cuenta cuenta;

  @Column(nullable = false, length = 120)
  private String nombre;

  @Column(name = "nombre_contacto", nullable = false, length = 120)
  private String nombreContacto;

  @Column(nullable = false, length = 30)
  private String telefono;

  @Column(nullable = false, length = 160)
  private String correo;

  @Column(length = 300)
  private String descripcion;

  @Column(length = 100)
  private String horario;

  // Pin del mapa en SRID 4326; cantón y provincia se resuelven desde ahí
  @Column(nullable = false)
  private Point ubicacion;

  @Column(nullable = false, length = 60)
  private String canton;

  @Column(nullable = false, length = 30)
  private String provincia;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "cafeteria_estilo", joinColumns = @JoinColumn(name = "cafeteria_id"))
  @Enumerated(EnumType.STRING)
  @Column(name = "estilo", nullable = false, length = 10)
  private Set<NivelTueste> estilos = EnumSet.noneOf(NivelTueste.class);

  @Enumerated(EnumType.STRING)
  @Column(name = "volumen_mensual", length = 15)
  private VolumenMensual volumenMensual;

  // Solo la URL de Cloudinary
  @Column(name = "portada_url", length = 500)
  private String portadaUrl;

  @Column(nullable = false)
  private boolean eliminada = false;

  @Column(name = "eliminada_en")
  private Instant eliminadaEn;

  @Column(name = "creada_en", nullable = false, updatable = false)
  private Instant creadaEn;

  protected Cafeteria() {}

  public Cafeteria(
      Cuenta cuenta,
      String nombre,
      String nombreContacto,
      String telefono,
      String correo,
      Point ubicacion,
      String canton,
      String provincia,
      Set<NivelTueste> estilos) {
    if (!cuenta.getRoles().contains(Rol.CAFETERIA)) {
      throw new IllegalArgumentException("La cuenta no tiene rol cafetería");
    }
    this.cuenta = cuenta;
    this.nombre = nombre;
    this.nombreContacto = nombreContacto;
    this.telefono = telefono;
    this.correo = correo;
    this.ubicacion = ubicacion;
    this.canton = canton;
    this.provincia = provincia;
    this.estilos.addAll(estilos);
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
    nombreContacto = limpiar(nombreContacto);
    telefono = limpiar(telefono);
    correo = Cuenta.normalizarCorreo(correo);
    descripcion = limpiar(descripcion);
    horario = limpiar(horario);
    canton = limpiar(canton);
    provincia = limpiar(provincia);
  }

  // Borrado lógico: la cafetería y su menú se conservan, y la cuenta puede registrar otra
  public void eliminar() {
    eliminada = true;
    eliminadaEn = Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public Cuenta getCuenta() {
    return cuenta;
  }

  public String getNombre() {
    return nombre;
  }

  public void setNombre(String nombre) {
    this.nombre = nombre;
  }

  public String getNombreContacto() {
    return nombreContacto;
  }

  public void setNombreContacto(String nombreContacto) {
    this.nombreContacto = nombreContacto;
  }

  public String getTelefono() {
    return telefono;
  }

  public void setTelefono(String telefono) {
    this.telefono = telefono;
  }

  public String getCorreo() {
    return correo;
  }

  public void setCorreo(String correo) {
    this.correo = correo;
  }

  public String getDescripcion() {
    return descripcion;
  }

  public void setDescripcion(String descripcion) {
    this.descripcion = descripcion;
  }

  public String getHorario() {
    return horario;
  }

  public void setHorario(String horario) {
    this.horario = horario;
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

  public Set<NivelTueste> getEstilos() {
    return estilos;
  }

  public void setEstilos(Set<NivelTueste> estilos) {
    this.estilos.clear();
    this.estilos.addAll(estilos);
  }

  public VolumenMensual getVolumenMensual() {
    return volumenMensual;
  }

  public void setVolumenMensual(VolumenMensual volumenMensual) {
    this.volumenMensual = volumenMensual;
  }

  public String getPortadaUrl() {
    return portadaUrl;
  }

  public void setPortadaUrl(String portadaUrl) {
    this.portadaUrl = portadaUrl;
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
}
