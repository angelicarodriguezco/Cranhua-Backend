package com.tuanistack.cranhua.finca;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tuanistack.cranhua.BasePostgis;
import com.tuanistack.cranhua.cuenta.Cuenta;
import com.tuanistack.cranhua.cuenta.CuentaRepository;
import com.tuanistack.cranhua.cuenta.Rol;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
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
class FincaRepositoryTest {

  static final GeometryFactory GEOMETRIA = new GeometryFactory(new PrecisionModel(), 4326);

  @Autowired private FincaRepository fincaRepository;

  @Autowired private CosechaRepository cosechaRepository;

  @Autowired private CuentaRepository cuentaRepository;

  @Autowired private TestEntityManager entityManager;

  @Autowired private JdbcTemplate jdbcTemplate;

  static Polygon perimetro() {
    return GEOMETRIA.createPolygon(
        new Coordinate[] {
          new Coordinate(-84.0247, 9.6575),
          new Coordinate(-84.0200, 9.6575),
          new Coordinate(-84.0200, 9.6620),
          new Coordinate(-84.0247, 9.6620),
          new Coordinate(-84.0247, 9.6575)
        });
  }

  static Point pin() {
    return GEOMETRIA.createPoint(new Coordinate(-84.0223, 9.6598));
  }

  static Finca nuevaFinca() {
    return new Finca("Finca El Tucán", perimetro(), pin(), "Tarrazú", "San José");
  }

  private Cuenta nuevaCuenta(String correo) {
    return cuentaRepository.saveAndFlush(
        new Cuenta("Andrés Solano", correo, "hash", Set.of(Rol.CAFETALERO)));
  }

  private Finca guardarYLeer(Finca finca) {
    Finca guardada = fincaRepository.saveAndFlush(finca);
    entityManager.clear();
    return fincaRepository.findById(guardada.getId()).orElseThrow();
  }

  @Test
  void guardaYLeeFincaConPerimetroPinYUbicacion() {
    Finca leida = guardarYLeer(nuevaFinca());

    assertThat(leida.getId()).isNotNull();
    assertThat(leida.getNombre()).isEqualTo("Finca El Tucán");
    assertThat(leida.getPerimetro().equalsExact(perimetro())).isTrue();
    assertThat(leida.getPerimetro().getSRID()).isEqualTo(4326);
    assertThat(leida.getUbicacion().equalsExact(pin())).isTrue();
    assertThat(leida.getCanton()).isEqualTo("Tarrazú");
    assertThat(leida.getProvincia()).isEqualTo("San José");
    assertThat(leida.getCreadaEn()).isNotNull();
    assertThat(leida.isEliminada()).isFalse();
  }

  @Test
  void zonaCafetaleraEsOpcional() {
    Finca leida = guardarYLeer(nuevaFinca());

    assertThat(leida.getZonaCafetalera()).isNull();
  }

  @Test
  void guardaLaZonaConSuNombre() {
    Finca finca = nuevaFinca();
    finca.setZonaCafetalera(ZonaCafetalera.TRES_RIOS);

    Finca leida = guardarYLeer(finca);

    assertThat(leida.getZonaCafetalera()).isEqualTo(ZonaCafetalera.TRES_RIOS);
    assertThat(
            jdbcTemplate.queryForObject(
                "select zona_cafetalera from finca where id = ?", String.class, leida.getId()))
        .isEqualTo("Tres Ríos");
  }

  @Test
  void descripcionDeMasDe300CaracteresFalla() {
    Finca finca = nuevaFinca();
    finca.setDescripcion("a".repeat(301));

    assertThatThrownBy(() -> fincaRepository.saveAndFlush(finca))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void unaFincaPuedeTenerDosDuenos() {
    Cuenta andres = nuevaCuenta("andres@ejemplo.cr");
    Cuenta lucia = nuevaCuenta("lucia@ejemplo.cr");
    Finca finca = nuevaFinca();
    finca.agregarDueno(andres);
    finca.agregarDueno(lucia);

    Finca leida = guardarYLeer(finca);

    assertThat(leida.getDuenos())
        .extracting(Cuenta::getCorreo)
        .containsExactlyInAnyOrder("andres@ejemplo.cr", "lucia@ejemplo.cr");
    assertThat(leida.esDueno(andres.getId())).isTrue();
    assertThat(leida.esDueno(lucia.getId())).isTrue();
    assertThat(fincaRepository.findByDuenosIdAndEliminadaFalse(lucia.getId()))
        .extracting(Finca::getId)
        .containsExactly(leida.getId());
  }

  @Test
  void fincaEliminadaSeConservaConSusCosechas() {
    Cuenta duena = nuevaCuenta("duena@ejemplo.cr");
    Finca finca = nuevaFinca();
    finca.agregarDueno(duena);
    finca = fincaRepository.saveAndFlush(finca);
    cosechaRepository.saveAndFlush(
        new Cosecha(
            finca,
            "Geisha",
            1600,
            LocalDate.of(2026, 1, 10),
            LocalDate.of(2026, 3, 20),
            new BigDecimal("500.00"),
            new BigDecimal("12.50"),
            List.of(new ProcesoReparto(Proceso.LAVADO, 100))));

    finca.eliminar();
    fincaRepository.saveAndFlush(finca);
    entityManager.clear();

    assertThat(fincaRepository.findByIdAndEliminadaFalse(finca.getId())).isEmpty();
    assertThat(fincaRepository.findByDuenosIdAndEliminadaFalse(duena.getId())).isEmpty();
    Finca leida = fincaRepository.findById(finca.getId()).orElseThrow();
    assertThat(leida.isEliminada()).isTrue();
    assertThat(leida.getEliminadaEn()).isNotNull();
    assertThat(cosechaRepository.findByFincaId(finca.getId())).hasSize(1);
  }

  @Test
  void encuentraFincaActivaPorId() {
    Finca finca = fincaRepository.saveAndFlush(nuevaFinca());

    assertThat(fincaRepository.findByIdAndEliminadaFalse(finca.getId())).isPresent();
  }

  @Test
  void elPerimetroTieneIndiceGist() {
    String definicion =
        jdbcTemplate.queryForObject(
            "select indexdef from pg_indexes where tablename = 'finca' and indexname = ?",
            String.class,
            "idx_finca_perimetro");

    assertThat(definicion).containsIgnoringCase("using gist (perimetro)");
  }
}
