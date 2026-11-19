package com.tuanistack.cranhua.solicitud;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tuanistack.cranhua.cuenta.Cuenta;
import com.tuanistack.cranhua.cuenta.Rol;
import com.tuanistack.cranhua.finca.Cosecha;
import com.tuanistack.cranhua.finca.Proceso;
import com.tuanistack.cranhua.finca.ProcesoReparto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SolicitudTest {

  private static Cosecha cosechaConPrecio(String precioKg) {
    return new Cosecha(
        null,
        "Catuaí",
        1300,
        LocalDate.of(2026, 1, 1),
        LocalDate.of(2026, 2, 1),
        new BigDecimal("800.00"),
        new BigDecimal(precioKg),
        List.of(new ProcesoReparto(Proceso.LAVADO, 100)));
  }

  private static Solicitud nuevaSolicitud() {
    return new Solicitud(
        cosechaConPrecio("9.75"),
        new Cuenta("Mario Brenes", "mario@ejemplo.cr", "hash", Set.of(Rol.TOSTADOR)),
        new BigDecimal("120.00"));
  }

  @Test
  void tomaElPrecioDeLaCosechaYCalculaElTotal() {
    Solicitud solicitud = nuevaSolicitud();

    assertThat(solicitud.getPrecioKg()).isEqualByComparingTo("9.75");
    assertThat(solicitud.getTotalEstimado()).isEqualByComparingTo("1170.00");
    assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE);
    assertThat(solicitud.getRespondidaEn()).isNull();
  }

  @Test
  void elTotalSeRedondeaADosDecimales() {
    Solicitud solicitud = new Solicitud(cosechaConPrecio("9.99"), null, new BigDecimal("0.55"));

    // 0,55 * 9,99 = 5,4945
    assertThat(solicitud.getTotalEstimado()).isEqualTo(new BigDecimal("5.49"));

    solicitud = new Solicitud(cosechaConPrecio("3.33"), null, new BigDecimal("1.05"));

    // 1,05 * 3,33 = 3,4965
    assertThat(solicitud.getTotalEstimado()).isEqualTo(new BigDecimal("3.50"));
  }

  @Test
  void aprobarDesdePendiente() {
    Solicitud solicitud = nuevaSolicitud();

    solicitud.aprobar();

    assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.APROBADA);
    assertThat(solicitud.getRespondidaEn()).isNotNull();
  }

  @Test
  void rechazarDesdePendiente() {
    Solicitud solicitud = nuevaSolicitud();

    solicitud.rechazar();

    assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.RECHAZADA);
    assertThat(solicitud.getRespondidaEn()).isNotNull();
  }

  @Test
  void responderUnaSolicitudYaRespondidaFalla() {
    Solicitud aprobada = nuevaSolicitud();
    aprobada.aprobar();
    Solicitud rechazada = nuevaSolicitud();
    rechazada.rechazar();

    assertThatThrownBy(aprobada::aprobar).isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(aprobada::rechazar).isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(rechazada::aprobar).isInstanceOf(IllegalStateException.class);
    assertThat(aprobada.getEstado()).isEqualTo(EstadoSolicitud.APROBADA);
    assertThat(rechazada.getEstado()).isEqualTo(EstadoSolicitud.RECHAZADA);
  }
}
