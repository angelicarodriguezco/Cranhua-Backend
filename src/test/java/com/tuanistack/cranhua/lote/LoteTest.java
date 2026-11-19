package com.tuanistack.cranhua.lote;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tuanistack.cranhua.cuenta.Cuenta;
import com.tuanistack.cranhua.cuenta.Rol;
import com.tuanistack.cranhua.finca.Cosecha;
import com.tuanistack.cranhua.finca.Proceso;
import com.tuanistack.cranhua.finca.ProcesoReparto;
import com.tuanistack.cranhua.solicitud.Solicitud;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class LoteTest {

  private static Solicitud nuevaSolicitud() {
    Cosecha cosecha =
        new Cosecha(
            null,
            "Catuaí",
            1300,
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 2, 1),
            new BigDecimal("800.00"),
            new BigDecimal("8.00"),
            List.of(new ProcesoReparto(Proceso.LAVADO, 100)));
    return new Solicitud(
        cosecha,
        new Cuenta("Mario Brenes", "mario@ejemplo.cr", "hash", Set.of(Rol.TOSTADOR)),
        new BigDecimal("100.00"));
  }

  private static Lote nuevoLote() {
    Solicitud solicitud = nuevaSolicitud();
    solicitud.aprobar();
    return new Lote(solicitud, "CRH-2026-0001");
  }

  @Test
  void unLoteNuevoEmpiezaPendiente() {
    Lote lote = nuevoLote();

    assertThat(lote.getCodigo()).isEqualTo("CRH-2026-0001");
    assertThat(lote.getEstadoTrazabilidad()).isEqualTo(EstadoTrazabilidad.PENDIENTE);
    assertThat(lote.getActivadoEn()).isNull();
  }

  @Test
  void soloUnaSolicitudAprobadaGeneraLote() {
    Solicitud pendiente = nuevaSolicitud();
    Solicitud rechazada = nuevaSolicitud();
    rechazada.rechazar();

    assertThatThrownBy(() -> new Lote(pendiente, "CRH-2026-0001"))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> new Lote(rechazada, "CRH-2026-0001"))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void activarDosVecesNoCambiaLaFecha() {
    Lote lote = nuevoLote();

    lote.activar();
    Instant activadoEn = lote.getActivadoEn();
    lote.activar();

    assertThat(lote.getEstadoTrazabilidad()).isEqualTo(EstadoTrazabilidad.ACTIVO);
    assertThat(activadoEn).isNotNull();
    assertThat(lote.getActivadoEn()).isSameAs(activadoEn);
  }

  @Test
  void formatearCodigoRellenaConCerosHasta4Digitos() {
    assertThat(Lote.formatearCodigo(2026, 7)).isEqualTo("CRH-2026-0007");
    assertThat(Lote.formatearCodigo(2026, 9999)).isEqualTo("CRH-2026-9999");
    assertThat(Lote.formatearCodigo(2026, 12345)).isEqualTo("CRH-2026-12345");
  }
}
