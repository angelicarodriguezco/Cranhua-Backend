package com.tuanistack.cranhua.mapa.dto;

import com.tuanistack.cranhua.finca.dto.Coordenada;
import com.tuanistack.cranhua.mapa.PuntoMapa;
import com.tuanistack.cranhua.mapa.TipoPuntoMapa;
import java.util.UUID;

public record PuntoMapaResponse(
    TipoPuntoMapa tipo,
    UUID id,
    String nombre,
    String canton,
    String provincia,
    Coordenada coordenada) {

  public static PuntoMapaResponse de(TipoPuntoMapa tipo, PuntoMapa punto) {
    return new PuntoMapaResponse(
        tipo,
        punto.getId(),
        punto.getNombre(),
        punto.getCanton(),
        punto.getProvincia(),
        new Coordenada(punto.getLat(), punto.getLng()));
  }
}
