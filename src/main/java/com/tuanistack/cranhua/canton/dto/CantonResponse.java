package com.tuanistack.cranhua.canton.dto;

import com.tuanistack.cranhua.canton.Canton;
import com.tuanistack.cranhua.finca.GeometriaUtil;
import com.tuanistack.cranhua.finca.ZonaCafetalera;
import com.tuanistack.cranhua.finca.dto.Coordenada;

public record CantonResponse(
    Short id, String nombre, String provincia, ZonaCafetalera zonaCafetalera, Coordenada cabecera) {

  public static CantonResponse de(Canton canton) {
    return new CantonResponse(
        canton.getId(),
        canton.getNombre(),
        canton.getProvincia(),
        canton.getZonaCafetalera(),
        GeometriaUtil.coordenada(canton.getCabecera()));
  }
}
