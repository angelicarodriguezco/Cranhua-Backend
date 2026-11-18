package com.tuanistack.cranhua.publico.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tuanistack.cranhua.finca.dto.Coordenada;
import com.tuanistack.cranhua.tueste.NivelTueste;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record PerfilCafeteriaPublicoResponse(
    UUID id,
    String nombre,
    @JsonInclude(JsonInclude.Include.NON_EMPTY) String descripcion,
    @JsonInclude(JsonInclude.Include.NON_EMPTY) String horario,
    String canton,
    String provincia,
    Coordenada ubicacion,
    Set<NivelTueste> estilos,
    @JsonInclude(JsonInclude.Include.NON_NULL) String portadaUrl,
    Instant miembroDesde,
    long cafesTrazables,
    List<EnlacePerfil> fincas,
    List<EnlacePerfil> tostadores) {}
