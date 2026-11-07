package com.tuanistack.cranhua.finca;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Arrays;

public enum Proceso {
  HONEY("Honey"),
  LAVADO("Lavado"),
  NATURAL("Natural"),
  ANAEROBICO("Anaeróbico"),
  SEMI_LAVADO("Semi-lavado"),
  RED_HONEY("Red honey"),
  YELLOW_HONEY("Yellow honey");

  private final String nombre;

  Proceso(String nombre) {
    this.nombre = nombre;
  }

  public String getNombre() {
    return nombre;
  }

  public static Proceso desdeNombre(String nombre) {
    return Arrays.stream(values())
        .filter(proceso -> proceso.nombre.equals(nombre))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Proceso desconocido: " + nombre));
  }

  @Converter(autoApply = true)
  public static class Conversor implements AttributeConverter<Proceso, String> {

    @Override
    public String convertToDatabaseColumn(Proceso proceso) {
      return proceso == null ? null : proceso.nombre;
    }

    @Override
    public Proceso convertToEntityAttribute(String nombre) {
      return nombre == null ? null : desdeNombre(nombre);
    }
  }
}
