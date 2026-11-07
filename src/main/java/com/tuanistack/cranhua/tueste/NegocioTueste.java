package com.tuanistack.cranhua.tueste;

import com.tuanistack.cranhua.cuenta.Cuenta;
import com.tuanistack.cranhua.cuenta.Rol;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "negocio_tueste")
public class NegocioTueste {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "cuenta_id", nullable = false, unique = true, updatable = false)
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

  // Cantón y provincia resueltos desde el pin del mapa
  @Column(nullable = false, length = 60)
  private String canton;

  @Column(nullable = false, length = 30)
  private String provincia;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(
      name = "negocio_tueste_estilo",
      joinColumns = @JoinColumn(name = "negocio_tueste_id"))
  @Enumerated(EnumType.STRING)
  @Column(name = "estilo", nullable = false, length = 10)
  private Set<NivelTueste> estilos = EnumSet.noneOf(NivelTueste.class);

  // Solo la URL de Cloudinary
  @Column(name = "portada_url", length = 500)
  private String portadaUrl;

  @Column(nullable = false)
  private boolean eliminado = false;

  @Column(name = "eliminado_en")
  private Instant eliminadoEn;

  @Column(name = "creado_en", nullable = false, updatable = false)
  private Instant creadoEn;

  protected NegocioTueste() {}

  public NegocioTueste(
      Cuenta cuenta,
      String nombre,
      String nombreContacto,
      String telefono,
      String correo,
      String canton,
      String provincia,
      Set<NivelTueste> estilos) {
    if (!cuenta.getRoles().contains(Rol.TOSTADOR)) {
      throw new IllegalArgumentException("La cuenta no tiene rol tostador");
    }
    this.cuenta = cuenta;
    this.nombre = nombre;
    this.nombreContacto = nombreContacto;
    this.telefono = telefono;
    this.correo = correo;
    this.canton = canton;
    this.provincia = provincia;
    this.estilos.addAll(estilos);
  }

  private static String limpiar(String texto) {
    return texto == null ? null : texto.strip();
  }

  @PrePersist
  void alCrear() {
    creadoEn = Instant.now();
    alActualizar();
  }

  @PreUpdate
  void alActualizar() {
    nombre = limpiar(nombre);
    nombreContacto = limpiar(nombreContacto);
    telefono = limpiar(telefono);
    correo = Cuenta.normalizarCorreo(correo);
    descripcion = limpiar(descripcion);
    canton = limpiar(canton);
    provincia = limpiar(provincia);
  }

  // Borrado lógico: el negocio, sus lotes y códigos se conservan
  public void eliminar() {
    eliminado = true;
    eliminadoEn = Instant.now();
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

  public String getPortadaUrl() {
    return portadaUrl;
  }

  public void setPortadaUrl(String portadaUrl) {
    this.portadaUrl = portadaUrl;
  }

  public boolean isEliminado() {
    return eliminado;
  }

  public Instant getEliminadoEn() {
    return eliminadoEn;
  }

  public Instant getCreadoEn() {
    return creadoEn;
  }
}
