package com.tuanistack.cranhua.finca;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Arrays;

public enum ZonaCafetalera {
  TARRAZU("Tarrazú"),
  VALLE_CENTRAL("Valle Central"),
  VALLE_OCCIDENTAL("Valle Occidental"),
  TRES_RIOS("Tres Ríos"),
  OROSI("Orosi"),
  TURRIALBA("Turrialba"),
  BRUNCA("Brunca"),
  GUANACASTE("Guanacaste");

  private final String nombre;

  ZonaCafetalera(String nombre) {
    this.nombre = nombre;
  }

  public String getNombre() {
    return nombre;
  }

  public static ZonaCafetalera desdeNombre(String nombre) {
    return Arrays.stream(values())
        .filter(zona -> zona.nombre.equals(nombre))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Zona cafetalera desconocida: " + nombre));
  }

  // En la base se guarda el nombre con tildes, igual que en el frontend
  @Converter(autoApply = true)
  public static class Conversor implements AttributeConverter<ZonaCafetalera, String> {

    @Override
    public String convertToDatabaseColumn(ZonaCafetalera zona) {
      return zona == null ? null : zona.nombre;
    }

    @Override
    public ZonaCafetalera convertToEntityAttribute(String nombre) {
      return nombre == null ? null : desdeNombre(nombre);
    }
  }
}
