package com.tuanistack.cranhua.cafeteria;

import com.tuanistack.cranhua.finca.Finca;
import com.tuanistack.cranhua.tueste.NivelTueste;
import java.math.BigDecimal;

// Ítem del menú con el origen de su código; nivel, notas y finca son null si no tiene
public interface ItemMenuPublico {

  String getNombre();

  CategoriaMenu getCategoria();

  BigDecimal getPrecio();

  NivelTueste getNivelTueste();

  String getNotasSabor();

  Finca getFinca();
}
