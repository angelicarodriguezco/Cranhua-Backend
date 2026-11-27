package com.tuanistack.cranhua.ai.datos.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tuanistack.cranhua.finca.ZonaCafetalera;
import com.tuanistack.cranhua.finca.dto.Coordenada;

public record FincaKianaDto(
    String nombre,
    @JsonInclude(JsonInclude.Include.NON_EMPTY) String descripcion,
    ZonaCafetalera zona,
    String canton,
    String provincia,
    Coordenada ubicacion) {}
