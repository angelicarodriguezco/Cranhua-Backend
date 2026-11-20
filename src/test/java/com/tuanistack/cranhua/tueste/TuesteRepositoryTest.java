package com.tuanistack.cranhua.tueste;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tuanistack.cranhua.BasePostgis;
import com.tuanistack.cranhua.cuenta.Cuenta;
import com.tuanistack.cranhua.cuenta.CuentaRepository;
import com.tuanistack.cranhua.cuenta.Rol;
import com.tuanistack.cranhua.finca.Cosecha;
import com.tuanistack.cranhua.finca.CosechaRepository;
import com.tuanistack.cranhua.finca.FincaRepository;
import com.tuanistack.cranhua.lote.Lote;
import com.tuanistack.cranhua.lote.LoteRepository;
import com.tuanistack.cranhua.solicitud.Solicitud;
import com.tuanistack.cranhua.solicitud.SolicitudRepository;
import com.tuanistack.cranhua.solicitud.SolicitudRepositoryTest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(BasePostgis.class)
class TuesteRepositoryTest {

  @Autowired private TuesteRepository tuesteRepository;

  @Autowired private LoteRepository loteRepository;

  @Autowired private SolicitudRepository solicitudRepository;

  @Autowired private CosechaRepository cosechaRepository;

  @Autowired private FincaRepository fincaRepository;

  @Autowired private CuentaRepository cuentaRepository;

  @Autowired private TestEntityManager entityManager;

  private Lote lote;

  @BeforeEach
  void crearLote() {
    Cosecha cosecha =
        cosechaRepository.saveAndFlush(
            SolicitudRepositoryTest.nuevaCosecha(
                fincaRepository.saveAndFlush(SolicitudRepositoryTest.finca())));
    Cuenta tostador =
        cuentaRepository.saveAndFlush(
            new Cuenta("Mario Brenes", "mario@ejemplo.cr", "hash", Set.of(Rol.TOSTADOR)));
    Solicitud solicitud = new Solicitud(cosecha, tostador, new BigDecimal("100.00"));
    solicitud.aprobar();
    solicitudRepository.saveAndFlush(solicitud);
    lote = loteRepository.saveAndFlush(new Lote(solicitud, "CRH-2026-0001"));
  }

  private Tueste tueste(NivelTueste nivel, String pesoTostadoKg, LocalDate fecha) {
    return new Tueste(lote, nivel, new BigDecimal("50.00"), new BigDecimal(pesoTostadoKg), fecha);
  }

  @Test
  void unLoteTieneVariosTuestes() {
    Tueste primero = tueste(NivelTueste.CLARO, "43.00", LocalDate.of(2026, 3, 1));
    primero.setNotas("Primer tueste de prueba.");
    tuesteRepository.saveAndFlush(primero);
    Tueste segundo =
        tuesteRepository.saveAndFlush(
            tueste(NivelTueste.OSCURO, "40.00", LocalDate.of(2026, 3, 8)));
    entityManager.clear();

    assertThat(tuesteRepository.findByLoteIdOrderByFechaTuesteDesc(lote.getId()))
        .extracting(Tueste::getId)
        .containsExactly(segundo.getId(), primero.getId());

    Tueste leido = tuesteRepository.findById(primero.getId()).orElseThrow();
    assertThat(leido.getLote().getId()).isEqualTo(lote.getId());
    assertThat(leido.getNivel()).isEqualTo(NivelTueste.CLARO);
    assertThat(leido.getPesoVerdeKg()).isEqualByComparingTo("50.00");
    assertThat(leido.getPesoTostadoKg()).isEqualByComparingTo("43.00");
    assertThat(leido.getMermaPorcentaje()).isEqualByComparingTo("14.00");
    assertThat(leido.getNotas()).isEqualTo("Primer tueste de prueba.");
    assertThat(leido.getCreadoEn()).isNotNull();
  }

  @Test
  void pesoTostadoMayorAlVerdeFallaEnBd() {
    Tueste tueste = tueste(NivelTueste.MEDIO, "50.01", LocalDate.of(2026, 3, 1));

    assertThatThrownBy(() -> tuesteRepository.saveAndFlush(tueste))
        .isInstanceOf(DataIntegrityViolationException.class)
        .hasMessageContaining("ck_tueste_pesos");
  }

  @Test
  void sumaPesoVerdeDelLoteExcluyendoTueste() {
    Tueste primero =
        tuesteRepository.saveAndFlush(
            tueste(NivelTueste.CLARO, "43.00", LocalDate.of(2026, 3, 1)));
    Tueste segundo =
        tuesteRepository.saveAndFlush(
            tueste(NivelTueste.OSCURO, "40.00", LocalDate.of(2026, 3, 8)));
    entityManager.clear();

    assertThat(tuesteRepository.sumPesoVerdeKgByLoteIdExcluding(lote.getId(), primero.getId()))
        .isEqualByComparingTo("50.00");
    assertThat(tuesteRepository.sumPesoVerdeKgByLoteIdExcluding(lote.getId(), segundo.getId()))
        .isEqualByComparingTo("50.00");
    assertThat(tuesteRepository.sumPesoVerdeKgByLoteIdExcluding(lote.getId(), UUID.randomUUID()))
        .isEqualByComparingTo("100.00");
  }
}
