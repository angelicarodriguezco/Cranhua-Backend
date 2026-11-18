package com.tuanistack.cranhua.mapa;

import java.util.UUID;

// Proyección de las consultas del mapa; deja fuera teléfono y correo
public interface PuntoMapa {

  UUID getId();

  String getNombre();

  String getCanton();

  String getProvincia();

  Double getLat();

  Double getLng();
}
