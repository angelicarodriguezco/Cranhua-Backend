package com.tuanistack.cranhua.solicitud;

import static org.assertj.core.api.Assertions.assertThat;

import com.tuanistack.cranhua.BasePostgis;
import com.tuanistack.cranhua.cuenta.Cuenta;
import com.tuanistack.cranhua.cuenta.CuentaRepository;
import com.tuanistack.cranhua.cuenta.Rol;
import com.tuanistack.cranhua.finca.Cosecha;
import com.tuanistack.cranhua.finca.CosechaRepository;
import com.tuanistack.cranhua.finca.Finca;
import com.tuanistack.cranhua.finca.FincaRepository;
import com.tuanistack.cranhua.finca.GeometriaUtil;
import com.tuanistack.cranhua.finca.Proceso;
import com.tuanistack.cranhua.finca.ProcesoReparto;
import com.tuanistack.cranhua.finca.dto.Coordenada;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(BasePostgis.class)
public class SolicitudRepositoryTest {

  @Autowired private SolicitudRepository solicitudRepository;

  @Autowired private CosechaRepository cosechaRepository;

  @Autowired private FincaRepository fincaRepository;

  @Autowired private CuentaRepository cuentaRepository;

  @Autowired private TestEntityManager entityManager;

  private Cosecha cosecha;

  private Cuenta tostador;

  @BeforeEach
  void crearDatos() {
    cosecha = cosechaRepository.saveAndFlush(nuevaCosecha(fincaRepository.saveAndFlush(finca())));
    tostador =
        cuentaRepository.saveAndFlush(
            new Cuenta("Mario Brenes", "mario@ejemplo.cr", "hash", Set.of(Rol.TOSTADOR)));
  }

  public static Finca finca() {
    return new Finca(
        "Finca El Tucán",
        GeometriaUtil.poligono(
            List.of(
                new Coordenada(9.6575, -84.0247),
                new Coordenada(9.6575, -84.0200),
                new Coordenada(9.6620, -84.0200),
                new Coordenada(9.6620, -84.0247))),
        GeometriaUtil.punto(new Coordenada(9.6598, -84.0223)),
        "Tarrazú",
        "San José");
  }

  public static Cosecha nuevaCosecha(Finca finca) {
    return new Cosecha(
        finca,
        "Caturra",
        1450,
        LocalDate.of(2026, 1, 5),
        LocalDate.of(2026, 2, 28),
        new BigDecimal("1200.00"),
        new BigDecimal("9.75"),
        List.of(new ProcesoReparto(Proceso.LAVADO, 100)));
  }

  @Test
  void guardaYLeeUnaSolicitud() {
    Solicitud solicitud =
        solicitudRepository.saveAndFlush(
            new Solicitud(cosecha, tostador, new BigDecimal("120.00")));
    entityManager.clear();

    Solicitud leida = solicitudRepository.findById(solicitud.getId()).orElseThrow();

    assertThat(leida.getCosecha().getId()).isEqualTo(cosecha.getId());
    assertThat(leida.getTostador().getId()).isEqualTo(tostador.getId());
    assertThat(leida.getCantidadKg()).isEqualByComparingTo("120.00");
    assertThat(leida.getPrecioKg()).isEqualByComparingTo("9.75");
    assertThat(leida.getTotalEstimado()).isEqualByComparingTo("1170.00");
    assertThat(leida.getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE);
    assertThat(leida.getSolicitadaEn()).isNotNull();
    assertThat(leida.getRespondidaEn()).isNull();
  }

  @Test
  void guardaElPrecioVigenteAunqueCambieElDeLaCosecha() {
    Solicitud solicitud =
        solicitudRepository.saveAndFlush(
            new Solicitud(cosecha, tostador, new BigDecimal("100.00")));
    cosecha.setPrecioKg(new BigDecimal("12.00"));
    cosechaRepository.saveAndFlush(cosecha);
    entityManager.clear();

    Solicitud leida = solicitudRepository.findById(solicitud.getId()).orElseThrow();

    assertThat(leida.getCosecha().getPrecioKg()).isEqualByComparingTo("12.00");
    assertThat(leida.getPrecioKg()).isEqualByComparingTo("9.75");
    assertThat(leida.getTotalEstimado()).isEqualByComparingTo("975.00");
  }

  @Test
  void buscaPorCosechaYEstadoYPorTostador() {
    Solicitud pendiente = new Solicitud(cosecha, tostador, new BigDecimal("10.00"));
    Solicitud aprobada = new Solicitud(cosecha, tostador, new BigDecimal("20.00"));
    aprobada.aprobar();
    solicitudRepository.saveAndFlush(pendiente);
    solicitudRepository.saveAndFlush(aprobada);
    entityManager.clear();

    assertThat(
            solicitudRepository.findByCosechaIdAndEstado(cosecha.getId(), EstadoSolicitud.APROBADA))
        .extracting(Solicitud::getId)
        .containsExactly(aprobada.getId());
    assertThat(solicitudRepository.findByTostadorIdOrderBySolicitadaEnDesc(tostador.getId()))
        .extracting(Solicitud::getId)
        .containsExactly(aprobada.getId(), pendiente.getId());
  }
}
