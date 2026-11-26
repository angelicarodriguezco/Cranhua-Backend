package com.tuanistack.cranhua.cafeteria.dto;

import com.tuanistack.cranhua.cafeteria.CategoriaMenu;
import com.tuanistack.cranhua.cafeteria.ItemMenu;
import java.math.BigDecimal;
import java.util.UUID;

public record ItemMenuResponse(
    UUID id, String nombre, CategoriaMenu categoria, String codigoTrazabilidad, BigDecimal precio) {

  public static ItemMenuResponse de(ItemMenu item) {
    return new ItemMenuResponse(
        item.getId(),
        item.getNombre(),
        item.getCategoria(),
        item.getCodigoTrazabilidad(),
        item.getPrecio());
  }
}
