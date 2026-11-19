package com.tuanistack.cranhua.lote;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tuanistack.cranhua.BasePostgis;
import com.tuanistack.cranhua.cuenta.Cuenta;
import com.tuanistack.cranhua.cuenta.CuentaRepository;
import com.tuanistack.cranhua.cuenta.Rol;
import com.tuanistack.cranhua.finca.Cosecha;
import com.tuanistack.cranhua.finca.CosechaRepository;
import com.tuanistack.cranhua.finca.FincaRepository;
import com.tuanistack.cranhua.solicitud.Solicitud;
import com.tuanistack.cranhua.solicitud.SolicitudRepository;
import com.tuanistack.cranhua.solicitud.SolicitudRepositoryTest;
import java.math.BigDecimal;
import java.util.Set;
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
class LoteRepositoryTest {

  @Autowired private LoteRepository loteRepository;

  @Autowired private SolicitudRepository solicitudRepository;

  @Autowired private CosechaRepository cosechaRepository;

  @Autowired private FincaRepository fincaRepository;

  @Autowired private CuentaRepository cuentaRepository;

  @Autowired private TestEntityManager entityManager;

  private Cosecha cosecha;

  private Cuenta tostador;

  @BeforeEach
  void crearDatos() {
    cosecha =
        cosechaRepository.saveAndFlush(
            SolicitudRepositoryTest.nuevaCosecha(
                fincaRepository.saveAndFlush(SolicitudRepositoryTest.finca())));
    tostador =
        cuentaRepository.saveAndFlush(
            new Cuenta("Mario Brenes", "mario@ejemplo.cr", "hash", Set.of(Rol.TOSTADOR)));
  }

  private Solicitud solicitudAprobada() {
    Solicitud solicitud = new Solicitud(cosecha, tostador, new BigDecimal("100.00"));
    solicitud.aprobar();
    return solicitudRepository.saveAndFlush(solicitud);
  }

  @Test
  void guardaYLeeUnLotePorCodigo() {
    Solicitud solicitud = solicitudAprobada();
    Lote lote = loteRepository.saveAndFlush(new Lote(solicitud, "CRH-2026-0007"));
    lote.activar();
    loteRepository.saveAndFlush(lote);
    entityManager.clear();

    Lote leido = loteRepository.findByCodigo("CRH-2026-0007").orElseThrow();

    assertThat(leido.getId()).isEqualTo(lote.getId());
    assertThat(leido.getSolicitud().getId()).isEqualTo(solicitud.getId());
    assertThat(leido.getEstadoTrazabilidad()).isEqualTo(EstadoTrazabilidad.ACTIVO);
    assertThat(leido.getActivadoEn()).isNotNull();
    assertThat(leido.getCreadoEn()).isNotNull();
    assertThat(loteRepository.findByCodigo("CRH-2026-0008")).isEmpty();
  }

  @Test
  void dosLotesConElMismoCodigoFallan() {
    loteRepository.saveAndFlush(new Lote(solicitudAprobada(), "CRH-2026-0001"));
    Lote repetido = new Lote(solicitudAprobada(), "CRH-2026-0001");

    assertThatThrownBy(() -> loteRepository.saveAndFlush(repetido))
        .isInstanceOf(DataIntegrityViolationException.class)
        .hasMessageContaining("uq_lote_codigo");
  }

  @Test
  void siguienteNumeroCodigoNoSeRepite() {
    long primero = loteRepository.siguienteNumeroCodigo();
    long segundo = loteRepository.siguienteNumeroCodigo();

    assertThat(segundo).isGreaterThan(primero);
  }
}
