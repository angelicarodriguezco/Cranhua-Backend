package com.tuanistack.cranhua.ai.datos.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tuanistack.cranhua.tueste.NivelTueste;
import java.util.Set;

public record TostadorKianaDto(
    String nombre,
    @JsonInclude(JsonInclude.Include.NON_EMPTY) String descripcion,
    String canton,
    String provincia,
    Set<NivelTueste> estilos) {}
