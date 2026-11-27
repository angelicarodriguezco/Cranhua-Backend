package com.tuanistack.cranhua.ai.datos.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tuanistack.cranhua.finca.dto.Coordenada;
import com.tuanistack.cranhua.tueste.NivelTueste;
import java.util.Set;

public record CafeteriaKianaDto(
    String nombre,
    @JsonInclude(JsonInclude.Include.NON_EMPTY) String descripcion,
    @JsonInclude(JsonInclude.Include.NON_EMPTY) String horario,
    String canton,
    String provincia,
    Coordenada ubicacion,
    Set<NivelTueste> estilos) {}
