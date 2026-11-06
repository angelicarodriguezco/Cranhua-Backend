package com.tuanistack.cranhua.cuenta;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "cuenta")
public class Cuenta {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "nombre_completo", nullable = false, length = 120)
  private String nombreCompleto;

  @Column(nullable = false, unique = true, length = 160)
  private String correo;

  @Column(length = 30)
  private String telefono;

  @Column(name = "foto_url", length = 500)
  private String fotoUrl;

  // Las cuentas de Google no tienen contraseña; la base exige una de las dos
  @Column(name = "contrasena_hash", length = 100)
  private String contrasenaHash;

  @Column(name = "google_id", unique = true)
  private String googleId;

  @Column(nullable = false)
  private boolean eliminada = false;

  @Column(name = "eliminada_en")
  private Instant eliminadaEn;

  @Column(name = "creada_en", nullable = false, updatable = false)
  private Instant creadaEn;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "cuenta_rol", joinColumns = @JoinColumn(name = "cuenta_id"))
  @Enumerated(EnumType.STRING)
  @Column(name = "rol", nullable = false, length = 20)
  private Set<Rol> roles = new HashSet<>();

  @ElementCollection
  @CollectionTable(name = "cuenta_red_social", joinColumns = @JoinColumn(name = "cuenta_id"))
  private List<RedSocial> redes = new ArrayList<>();

  protected Cuenta() {}

  public Cuenta(String nombreCompleto, String correo, String contrasenaHash, Set<Rol> roles) {
    this.nombreCompleto = nombreCompleto;
    this.correo = correo;
    this.contrasenaHash = contrasenaHash;
    this.roles = new HashSet<>(roles);
  }

  public static Cuenta deGoogle(
      String nombreCompleto, String correo, String googleId, String fotoUrl, Set<Rol> roles) {
    Cuenta cuenta = new Cuenta(nombreCompleto, correo, null, roles);
    cuenta.googleId = googleId;
    cuenta.fotoUrl = fotoUrl;
    return cuenta;
  }

  public static String normalizarCorreo(String correo) {
    return correo == null ? null : correo.strip().toLowerCase(Locale.ROOT);
  }

  @PrePersist
  void alCrear() {
    creadaEn = Instant.now();
    alActualizar();
  }

  @PreUpdate
  void alActualizar() {
    correo = normalizarCorreo(correo);
  }

  // Borrado lógico: la fila se conserva para no romper la trazabilidad
  public void eliminar() {
    eliminada = true;
    eliminadaEn = Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public String getNombreCompleto() {
    return nombreCompleto;
  }

  public void setNombreCompleto(String nombreCompleto) {
    this.nombreCompleto = nombreCompleto;
  }

  public String getCorreo() {
    return correo;
  }

  public void setCorreo(String correo) {
    this.correo = correo;
  }

  public String getTelefono() {
    return telefono;
  }

  public void setTelefono(String telefono) {
    this.telefono = telefono;
  }

  public String getFotoUrl() {
    return fotoUrl;
  }

  public void setFotoUrl(String fotoUrl) {
    this.fotoUrl = fotoUrl;
  }

  public String getContrasenaHash() {
    return contrasenaHash;
  }

  public void setContrasenaHash(String contrasenaHash) {
    this.contrasenaHash = contrasenaHash;
  }

  public String getGoogleId() {
    return googleId;
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

  public Set<Rol> getRoles() {
    return roles;
  }

  public List<RedSocial> getRedes() {
    return redes;
  }
}
