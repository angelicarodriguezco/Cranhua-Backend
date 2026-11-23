package com.tuanistack.cranhua.tueste.dto;

import com.tuanistack.cranhua.tueste.NegocioTueste;
import com.tuanistack.cranhua.tueste.NivelTueste;
import java.util.List;
import java.util.UUID;

public record DetalleTostadorExploracionResponse(
    UUID id,
    String nombre,
    String nombreContacto,
    String telefono,
    String correo,
    String descripcion,
    String canton,
    String provincia,
    List<NivelTueste> estilos,
    String portadaUrl,
    List<TuesteDisponibleExploracionResponse> tuestes) {

  public static DetalleTostadorExploracionResponse de(
      NegocioTueste negocio, List<TuesteDisponibleExploracionResponse> tuestes) {
    return new DetalleTostadorExploracionResponse(
        negocio.getId(),
        negocio.getNombre(),
        negocio.getNombreContacto(),
        negocio.getTelefono(),
        negocio.getCorreo(),
        negocio.getDescripcion(),
        negocio.getCanton(),
        negocio.getProvincia(),
        negocio.getEstilos().stream().sorted().toList(),
        negocio.getPortadaUrl(),
        tuestes);
  }
}
