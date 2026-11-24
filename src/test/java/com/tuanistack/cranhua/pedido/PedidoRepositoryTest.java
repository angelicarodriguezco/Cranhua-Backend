package com.tuanistack.cranhua.pedido;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tuanistack.cranhua.BasePostgis;
import com.tuanistack.cranhua.cafeteria.Cafeteria;
import com.tuanistack.cranhua.cafeteria.CafeteriaRepository;
import com.tuanistack.cranhua.cuenta.Cuenta;
import com.tuanistack.cranhua.cuenta.CuentaRepository;
import com.tuanistack.cranhua.cuenta.Rol;
import com.tuanistack.cranhua.finca.Cosecha;
import com.tuanistack.cranhua.finca.CosechaRepository;
import com.tuanistack.cranhua.finca.FincaRepository;
import com.tuanistack.cranhua.finca.GeometriaUtil;
import com.tuanistack.cranhua.finca.dto.Coordenada;
import com.tuanistack.cranhua.lote.Lote;
import com.tuanistack.cranhua.lote.LoteRepository;
import com.tuanistack.cranhua.solicitud.Solicitud;
import com.tuanistack.cranhua.solicitud.SolicitudRepository;
import com.tuanistack.cranhua.solicitud.SolicitudRepositoryTest;
import com.tuanistack.cranhua.tueste.NivelTueste;
import com.tuanistack.cranhua.tueste.Tueste;
import com.tuanistack.cranhua.tueste.TuesteRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
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
class PedidoRepositoryTest {

  @Autowired private PedidoRepository pedidoRepository;

  @Autowired private TuesteRepository tuesteRepository;

  @Autowired private LoteRepository loteRepository;

  @Autowired private SolicitudRepository solicitudRepository;

  @Autowired private CosechaRepository cosechaRepository;

  @Autowired private FincaRepository fincaRepository;

  @Autowired private CafeteriaRepository cafeteriaRepository;

  @Autowired private CuentaRepository cuentaRepository;

  @Autowired private TestEntityManager entityManager;

  private Lote lote;

  private Tueste tueste;

  private Cafeteria cafeteria;

  @BeforeEach
  void crearDatos() {
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
    lote = new Lote(solicitud, "CRH-2026-0001");
    lote.activar();
    lote = loteRepository.saveAndFlush(lote);
    tueste =
        tuesteRepository.saveAndFlush(
            new Tueste(
                lote,
                NivelTueste.MEDIO,
                new BigDecimal("50.00"),
                new BigDecimal("42.00"),
                LocalDate.of(2026, 3, 1)));

    Cuenta duenoCafeteria =
        cuentaRepository.saveAndFlush(
            new Cuenta("Andrés Solano", "andres@ejemplo.cr", "hash", Set.of(Rol.CAFETERIA)));
    cafeteria =
        cafeteriaRepository.saveAndFlush(
            new Cafeteria(
                duenoCafeteria,
                "Café Brumas",
                "Andrés Solano",
                "+506 8899-4410",
                "hola@cafebrumas.cr",
                GeometriaUtil.punto(new Coordenada(9.9205, -84.137)),
                "Escazú",
                "San José",
                Set.of(NivelTueste.MEDIO)));
  }

  private Pedido pedir(String kg) {
    return pedidoRepository.saveAndFlush(new Pedido(cafeteria, tueste, new BigDecimal(kg)));
  }

  @Test
  void guardaYLeeUnPedido() {
    Pedido pedido = pedir("10.00");
    entityManager.clear();

    Pedido leido = pedidoRepository.findById(pedido.getId()).orElseThrow();

    assertThat(leido.getCafeteria().getId()).isEqualTo(cafeteria.getId());
    assertThat(leido.getTueste().getId()).isEqualTo(tueste.getId());
    assertThat(leido.getCantidadKg()).isEqualByComparingTo("10.00");
    assertThat(leido.getEstado()).isEqualTo(EstadoPedido.EN_CAMINO);
    assertThat(leido.getPedidoEn()).isNotNull();
    assertThat(leido.getCerradoEn()).isNull();
  }

  @Test
  void entregarGuardaLaFechaDeCierre() {
    Pedido pedido = pedir("10.00");
    pedido.entregar();
    pedidoRepository.saveAndFlush(pedido);
    entityManager.clear();

    Pedido leido = pedidoRepository.findById(pedido.getId()).orElseThrow();
    assertThat(leido.getEstado()).isEqualTo(EstadoPedido.ENTREGADO);
    assertThat(leido.getCerradoEn()).isNotNull();
  }

  @Test
  void cantidadCeroFallaEnBd() {
    Pedido pedido = new Pedido(cafeteria, tueste, BigDecimal.ZERO);

    assertThatThrownBy(() -> pedidoRepository.saveAndFlush(pedido))
        .isInstanceOf(DataIntegrityViolationException.class)
        .hasMessageContaining("ck_pedido_cantidad");
  }

  @Test
  void listaLosPedidosDeLaCafeteria() {
    Pedido primero = pedir("5.00");
    Pedido segundo = pedir("3.00");
    entityManager.clear();

    assertThat(pedidoRepository.findByCafeteriaIdOrderByPedidoEnDesc(cafeteria.getId()))
        .extracting(Pedido::getId)
        .containsExactly(segundo.getId(), primero.getId());
  }

  @Test
  void losKgDisponiblesDescuentanPedidosEnCaminoYEntregados() {
    pedir("10.00");
    Pedido entregado = pedir("5.00");
    entregado.entregar();
    Pedido cancelado = pedir("20.00");
    cancelado.cancelar();
    pedidoRepository.saveAndFlush(entregado);
    pedidoRepository.saveAndFlush(cancelado);
    entityManager.clear();

    assertThat(pedidoRepository.kgComprometidos(tueste.getId())).isEqualByComparingTo("15.00");
    assertThat(tuesteRepository.kgDisponibles(tueste.getId()))
        .hasValueSatisfying(kg -> assertThat(kg).isEqualByComparingTo("27.00"));
  }

  @Test
  void sinPedidosTodoElPesoTostadoEstaDisponible() {
    assertThat(pedidoRepository.kgComprometidos(tueste.getId())).isEqualByComparingTo("0");
    assertThat(tuesteRepository.kgDisponibles(tueste.getId()))
        .hasValueSatisfying(kg -> assertThat(kg).isEqualByComparingTo("42.00"));
  }

  @Test
  void historialListaTodosLosPedidosSinLimite() {
    Pedido primero = pedir("5.00");
    Pedido segundo = pedir("3.00");
    Pedido tercero = pedir("2.00");
    entityManager.clear();

    assertThat(pedidoRepository.historial(cafeteria.getId()))
        .extracting(p -> p.id())
        .containsExactly(tercero.getId(), segundo.getId(), primero.getId());
  }

  @Test
  void kgEntregadosEsteMesSoloSumaEntregadosDelMesActual() {
    ZoneId costaRica = ZoneId.of("America/Costa_Rica");
    YearMonth mesActual = YearMonth.now(costaRica);
    Instant desde = mesActual.atDay(1).atStartOfDay(costaRica).toInstant();
    Instant hasta = mesActual.plusMonths(1).atDay(1).atStartOfDay(costaRica).toInstant();

    Pedido entregado = pedir("10.00");
    entregado.entregar();
    Pedido cancelado = pedir("7.00");
    cancelado.cancelar();
    pedir("4.00");
    pedidoRepository.saveAndFlush(entregado);
    pedidoRepository.saveAndFlush(cancelado);
    entityManager.clear();

    assertThat(pedidoRepository.kgEntregadosEsteMes(cafeteria.getId(), desde, hasta))
        .isEqualByComparingTo("10.00");
  }

  @Test
  void unTuesteDeLoteSinCodigoActivoNoCuentaComoDisponible() {
    Cosecha cosecha =
        cosechaRepository.saveAndFlush(
            SolicitudRepositoryTest.nuevaCosecha(
                fincaRepository.saveAndFlush(SolicitudRepositoryTest.finca())));
    Cuenta tostador =
        cuentaRepository.saveAndFlush(
            new Cuenta("Laura Mora", "laura@ejemplo.cr", "hash", Set.of(Rol.TOSTADOR)));
    Solicitud solicitud = new Solicitud(cosecha, tostador, new BigDecimal("80.00"));
    solicitud.aprobar();
    solicitudRepository.saveAndFlush(solicitud);
    Lote pendiente = loteRepository.saveAndFlush(new Lote(solicitud, "CRH-2026-0002"));
    Tueste sinActivar =
        tuesteRepository.saveAndFlush(
            new Tueste(
                pendiente,
                NivelTueste.CLARO,
                new BigDecimal("30.00"),
                new BigDecimal("25.00"),
                LocalDate.of(2026, 3, 2)));
    entityManager.clear();

    assertThat(tuesteRepository.kgDisponibles(sinActivar.getId())).isEmpty();
  }
}
