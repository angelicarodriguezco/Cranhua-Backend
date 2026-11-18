package com.tuanistack.cranhua.publico.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tuanistack.cranhua.tueste.NivelTueste;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record PerfilTostadorPublicoResponse(
    UUID id,
    String nombre,
    @JsonInclude(JsonInclude.Include.NON_EMPTY) String descripcion,
    String canton,
    String provincia,
    Set<NivelTueste> estilos,
    @JsonInclude(JsonInclude.Include.NON_NULL) String portadaUrl,
    Instant miembroDesde,
    List<EnlacePerfil> fincas,
    List<EnlacePerfil> puntosVenta) {}
