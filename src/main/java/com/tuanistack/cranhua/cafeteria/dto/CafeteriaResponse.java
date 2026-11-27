package com.tuanistack.cranhua.cafeteria.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tuanistack.cranhua.cafeteria.Cafeteria;
import com.tuanistack.cranhua.cafeteria.VolumenMensual;
import com.tuanistack.cranhua.finca.GeometriaUtil;
import com.tuanistack.cranhua.finca.dto.Coordenada;
import com.tuanistack.cranhua.lote.dto.HistorialTrazabilidad;
import com.tuanistack.cranhua.tueste.NivelTueste;
import java.util.List;
import java.util.UUID;

public record CafeteriaResponse(
    UUID id,
    UUID cuentaId,
    String nombre,
    String nombreContacto,
    String telefono,
    String correo,
    String descripcion,
    String horario,
    Coordenada ubicacion,
    String canton,
    String provincia,
    List<NivelTueste> estilos,
    VolumenMensual volumenMensual,
    String portadaUrl,
    @JsonInclude(JsonInclude.Include.NON_NULL) HistorialTrazabilidad historialTrazabilidad) {

  public static CafeteriaResponse de(Cafeteria cafeteria) {
    return new CafeteriaResponse(
        cafeteria.getId(),
        cafeteria.getCuenta().getId(),
        cafeteria.getNombre(),
        cafeteria.getNombreContacto(),
        cafeteria.getTelefono(),
        cafeteria.getCorreo(),
        cafeteria.getDescripcion(),
        cafeteria.getHorario(),
        GeometriaUtil.coordenada(cafeteria.getUbicacion()),
        cafeteria.getCanton(),
        cafeteria.getProvincia(),
        cafeteria.getEstilos().stream().sorted().toList(),
        cafeteria.getVolumenMensual(),
        cafeteria.getPortadaUrl(),
        null);
  }

  public CafeteriaResponse conHistorial(HistorialTrazabilidad historialTrazabilidad) {
    return new CafeteriaResponse(
        id,
        cuentaId,
        nombre,
        nombreContacto,
        telefono,
        correo,
        descripcion,
        horario,
        ubicacion,
        canton,
        provincia,
        estilos,
        volumenMensual,
        portadaUrl,
        historialTrazabilidad);
  }
}
