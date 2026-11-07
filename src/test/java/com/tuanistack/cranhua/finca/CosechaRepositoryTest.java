package com.tuanistack.cranhua.finca;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.tuanistack.cranhua.BasePostgis;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(BasePostgis.class)
class CosechaRepositoryTest {

  @Autowired private CosechaRepository cosechaRepository;

  @Autowired private FincaRepository fincaRepository;

  @Autowired private TestEntityManager entityManager;

  @Autowired private JdbcTemplate jdbcTemplate;

  private Finca finca;

  @BeforeEach
  void crearFinca() {
    finca = fincaRepository.saveAndFlush(FincaRepositoryTest.nuevaFinca());
  }

  private Cosecha nuevaCosecha() {
    return new Cosecha(
        finca,
        "Caturra",
        1450,
        LocalDate.of(2026, 1, 5),
        LocalDate.of(2026, 2, 28),
        new BigDecimal("1200.00"),
        new BigDecimal("9.75"),
        List.of(
            new ProcesoReparto(Proceso.RED_HONEY, 60), new ProcesoReparto(Proceso.ANAEROBICO, 40)));
  }

  private Cosecha guardarYLeer(Cosecha cosecha) {
    Cosecha guardada = cosechaRepository.saveAndFlush(cosecha);
    entityManager.clear();
    return cosechaRepository.findById(guardada.getId()).orElseThrow();
  }

  @Test
  void guardaYLeeCosechaConSusProcesos() {
    Cosecha leida = guardarYLeer(nuevaCosecha());

    assertThat(leida.getVariedad()).isEqualTo("Caturra");
    assertThat(leida.getPrecioKg()).isEqualByComparingTo("9.75");
    assertThat(leida.getEstado()).isEqualTo(EstadoCosecha.EN_BENEFICIADO);
    assertThat(leida.getProcesos())
        .extracting(ProcesoReparto::getProceso, ProcesoReparto::getPorcentaje)
        .containsExactlyInAnyOrder(tuple(Proceso.RED_HONEY, 60), tuple(Proceso.ANAEROBICO, 40));
    assertThat(cosechaRepository.findByFincaId(finca.getId())).hasSize(1);
  }

  @Test
  void guardaSemiLavadoConSuNombre() {
    Cosecha cosecha = nuevaCosecha();
    cosecha.setProcesos(
        List.of(
            new ProcesoReparto(Proceso.SEMI_LAVADO, 70), new ProcesoReparto(Proceso.HONEY, 30)));

    Cosecha leida = guardarYLeer(cosecha);

    assertThat(leida.getProcesos())
        .extracting(ProcesoReparto::getProceso, ProcesoReparto::getPorcentaje)
        .containsExactlyInAnyOrder(tuple(Proceso.SEMI_LAVADO, 70), tuple(Proceso.HONEY, 30));
    assertThat(
            jdbcTemplate.queryForObject(
                "select porcentaje from cosecha_proceso where cosecha_id = ? and proceso = ?",
                Integer.class,
                leida.getId(),
                "Semi-lavado"))
        .isEqualTo(70);
  }

  @Test
  void precioCeroFalla() {
    Cosecha cosecha = nuevaCosecha();
    cosecha.setPrecioKg(BigDecimal.ZERO);

    assertThatThrownBy(() -> cosechaRepository.saveAndFlush(cosecha))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void precioNegativoFalla() {
    Cosecha cosecha = nuevaCosecha();
    cosecha.setPrecioKg(new BigDecimal("-1.00"));

    assertThatThrownBy(() -> cosechaRepository.saveAndFlush(cosecha))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void fechaFinAntesDeInicioFalla() {
    Cosecha cosecha = nuevaCosecha();
    cosecha.setFechaFin(LocalDate.of(2026, 1, 4));

    assertThatThrownBy(() -> cosechaRepository.saveAndFlush(cosecha))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void puntajeMayorA100Falla() {
    Cosecha cosecha = nuevaCosecha();
    cosecha.setPuntajeSca(new BigDecimal("100.25"));

    assertThatThrownBy(() -> cosechaRepository.saveAndFlush(cosecha))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void pesoTrasSeleccionYCaracolilloSonOpcionales() {
    Cosecha leida = guardarYLeer(nuevaCosecha());

    assertThat(leida.getPesoTrasSeleccionKg()).isNull();
    assertThat(leida.getCaracolilloKg()).isNull();
    assertThat(leida.getKgDisponibles()).isNull();
  }

  @Test
  void guardaPesoTrasSeleccionYDisponibles() {
    Cosecha cosecha = nuevaCosecha();
    cosecha.setCaracolilloKg(new BigDecimal("35.50"));
    cosecha.registrarPesoTrasSeleccion(new BigDecimal("950.00"));
    cosecha.descontarDisponible(new BigDecimal("100.00"));

    Cosecha leida = guardarYLeer(cosecha);

    assertThat(leida.getPesoTrasSeleccionKg()).isEqualByComparingTo("950.00");
    assertThat(leida.getCaracolilloKg()).isEqualByComparingTo("35.50");
    assertThat(leida.getKgDisponibles()).isEqualByComparingTo("850.00");
    assertThat(leida.getEstado()).isEqualTo(EstadoCosecha.DISPONIBLE);
  }

  @Test
  void codigoLoteRepetidoFalla() {
    Cosecha primera = nuevaCosecha();
    primera.setCodigoLote("CRH-2026-0103");
    cosechaRepository.saveAndFlush(primera);

    Cosecha segunda = nuevaCosecha();
    segunda.setCodigoLote("crh-2026-0103");

    assertThatThrownBy(() -> cosechaRepository.saveAndFlush(segunda))
        .isInstanceOf(DataIntegrityViolationException.class);
  }
}
