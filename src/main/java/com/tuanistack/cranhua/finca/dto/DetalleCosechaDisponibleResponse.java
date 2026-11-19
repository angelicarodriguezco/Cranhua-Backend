package com.tuanistack.cranhua.finca.dto;

import com.tuanistack.cranhua.cuenta.Cuenta;
import com.tuanistack.cranhua.finca.Cosecha;
import com.tuanistack.cranhua.finca.Finca;
import com.tuanistack.cranhua.finca.Proceso;
import com.tuanistack.cranhua.finca.ZonaCafetalera;
import com.tuanistack.cranhua.finca.dto.CosechaResponse.Reparto;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public record DetalleCosechaDisponibleResponse(
    UUID id,
    UUID fincaId,
    String finca,
    ZonaCafetalera zona,
    String canton,
    String provincia,
    String variedad,
    Proceso proceso,
    List<Reparto> procesos,
    Integer alturaMsnm,
    BigDecimal kgDisponibles,
    List<String> notas,
    BigDecimal puntajeSca,
    BigDecimal precioKg,
    String codigoLote,
    List<Productor> productores) {

  public record Productor(UUID id, String nombreCompleto, String correo, String telefono) {}

  public static DetalleCosechaDisponibleResponse de(Cosecha cosecha) {
    Finca finca = cosecha.getFinca();
    return new DetalleCosechaDisponibleResponse(
        cosecha.getId(),
        finca.getId(),
        finca.getNombre(),
        finca.getZonaCafetalera(),
        finca.getCanton(),
        finca.getProvincia(),
        cosecha.getVariedad(),
        cosecha.getProcesoPrincipal(),
        cosecha.getProcesos().stream()
            .map(p -> new Reparto(p.getProceso(), p.getPorcentaje()))
            .toList(),
        cosecha.getAlturaMsnm(),
        cosecha.getKgDisponibles(),
        CosechaDisponibleResponse.notas(cosecha.getNotasSabor()),
        cosecha.getPuntajeSca(),
        cosecha.getPrecioKg(),
        cosecha.getCodigoLote(),
        finca.getDuenos().stream()
            .filter(dueno -> !dueno.isEliminada())
            .sorted(Comparator.comparing(Cuenta::getNombreCompleto))
            .map(d ->
                new Productor(d.getId(), d.getNombreCompleto(), d.getCorreo(), d.getTelefono()))
            .toList());
  }
}
