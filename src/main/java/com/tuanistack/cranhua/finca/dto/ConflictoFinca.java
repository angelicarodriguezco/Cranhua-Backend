package com.tuanistack.cranhua.finca.dto;

import java.util.List;
import java.util.UUID;

public record ConflictoFinca(
    UUID fincaId, String nombre, List<FincaResponse.Dueno> duenos, List<Coordenada> perimetro) {}
