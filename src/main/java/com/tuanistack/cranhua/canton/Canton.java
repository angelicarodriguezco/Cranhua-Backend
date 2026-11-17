package com.tuanistack.cranhua.canton;

import com.tuanistack.cranhua.finca.ZonaCafetalera;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.locationtech.jts.geom.Point;

@Entity
@Table(name = "canton")
public class Canton {

  @Id private Short id;

  @Column(nullable = false, length = 60)
  private String nombre;

  @Column(nullable = false, length = 30)
  private String provincia;

  @Column(name = "zona_cafetalera", length = 30)
  private ZonaCafetalera zonaCafetalera;

  @Column(nullable = false)
  private Point cabecera;

  protected Canton() {}

  public Short getId() {
    return id;
  }

  public String getNombre() {
    return nombre;
  }

  public String getProvincia() {
    return provincia;
  }

  public ZonaCafetalera getZonaCafetalera() {
    return zonaCafetalera;
  }

  public Point getCabecera() {
    return cabecera;
  }
}
