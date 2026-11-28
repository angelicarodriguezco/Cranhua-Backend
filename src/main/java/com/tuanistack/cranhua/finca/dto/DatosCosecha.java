package com.tuanistack.cranhua.finca.dto;

import com.tuanistack.cranhua.finca.dto.RegistrarCosechaRequest.RepartoRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface DatosCosecha {

  String variedad();

  Integer alturaMsnm();

  LocalDate fechaInicio();

  LocalDate fechaFin();

  List<RepartoRequest> procesos();

  BigDecimal cantidadCosechadaKg();

  BigDecimal pesoTrasBeneficiadoKg();

  BigDecimal pesoTrasSeleccionKg();

  BigDecimal caracolilloKg();

  String notasSabor();

  BigDecimal puntajeSca();

  BigDecimal precioKg();
}
