package com.tuanistack.cranhua.cafeteria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tuanistack.cranhua.BasePostgis;
import com.tuanistack.cranhua.cuenta.Cuenta;
import com.tuanistack.cranhua.cuenta.CuentaRepository;
import com.tuanistack.cranhua.cuenta.Rol;
import com.tuanistack.cranhua.finca.Cosecha;
import com.tuanistack.cranhua.finca.Finca;
import com.tuanistack.cranhua.finca.GeometriaUtil;
import com.tuanistack.cranhua.finca.Proceso;
import com.tuanistack.cranhua.finca.ProcesoReparto;
import com.tuanistack.cranhua.finca.dto.Coordenada;
import com.tuanistack.cranhua.tueste.NivelTueste;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
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
class CafeteriaRepositoryTest {

  @Autowired private CafeteriaRepository cafeteriaRepository;

  @Autowired private ItemMenuRepository itemMenuRepository;

  @Autowired private CuentaRepository cuentaRepository;

  @Autowired private TestEntityManager entityManager;

  private Cuenta nuevaCuenta(String correo, Rol rol) {
    return cuentaRepository.saveAndFlush(new Cuenta("Andrés Solano", correo, "hash", Set.of(rol)));
  }

  private Cafeteria nuevaCafeteria(Cuenta cuenta) {
    return new Cafeteria(
        cuenta,
        " Café Brumas ",
        "Andrés Solano",
        "+506 8899-4410",
        "Hola@CafeBrumas.CR",
        GeometriaUtil.punto(new Coordenada(9.9205, -84.137)),
        "Escazú",
        "San José",
        Set.of(NivelTueste.CLARO, NivelTueste.MEDIO));
  }

  private void nuevoLote(String codigo) {
    Finca finca =
        new Finca(
            "Finca El Tucán",
            GeometriaUtil.poligono(
                List.of(
                    new Coordenada(9.6575, -84.0247),
                    new Coordenada(9.6575, -84.0200),
                    new Coordenada(9.6620, -84.0200))),
            GeometriaUtil.punto(new Coordenada(9.6598, -84.0223)),
            "Tarrazú",
            "San José");
    Cosecha cosecha =
        new Cosecha(
            finca,
            "Caturra",
            1450,
            LocalDate.of(2026, 1, 5),
            LocalDate.of(2026, 2, 28),
            new BigDecimal("1200.00"),
            new BigDecimal("9.75"),
            List.of(new ProcesoReparto(Proceso.LAVADO, 100)));
    cosecha.setCodigoLote(codigo);
    entityManager.persist(finca);
    entityManager.persistAndFlush(cosecha);
  }

  @Test
  void guardaYLeeCafeteriaConVariosEstilos() {
    Cafeteria cafeteria = nuevaCafeteria(nuevaCuenta("cafeteria@ejemplo.cr", Rol.CAFETERIA));
    cafeteria.setDescripcion("Café de especialidad en Escazú.");
    cafeteria.setHorario("Lun–Sáb 7:00–19:00");
    cafeteria.setVolumenMensual(VolumenMensual.DE_25_A_50);
    cafeteria.setPortadaUrl("https://res.cloudinary.com/cranhua/portada.jpg");
    cafeteriaRepository.saveAndFlush(cafeteria);
    entityManager.clear();

    Cafeteria leida = cafeteriaRepository.findById(cafeteria.getId()).orElseThrow();

    assertThat(leida.getNombre()).isEqualTo("Café Brumas");
    assertThat(leida.getCorreo()).isEqualTo("hola@cafebrumas.cr");
    assertThat(leida.getUbicacion().getY()).isEqualTo(9.9205);
    assertThat(leida.getCanton()).isEqualTo("Escazú");
    assertThat(leida.getEstilos()).containsExactlyInAnyOrder(NivelTueste.CLARO, NivelTueste.MEDIO);
    assertThat(leida.getVolumenMensual()).isEqualTo(VolumenMensual.DE_25_A_50);
    assertThat(leida.getPortadaUrl()).isEqualTo("https://res.cloudinary.com/cranhua/portada.jpg");
  }

  @Test
  void unaCuentaTieneUnaSolaCafeteriaActiva() {
    Cuenta cuenta = nuevaCuenta("unica@ejemplo.cr", Rol.CAFETERIA);
    cafeteriaRepository.saveAndFlush(nuevaCafeteria(cuenta));

    assertThatThrownBy(() -> cafeteriaRepository.saveAndFlush(nuevaCafeteria(cuenta)))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void trasEliminarlaPuedeRegistrarOtra() {
    Cuenta cuenta = nuevaCuenta("otra@ejemplo.cr", Rol.CAFETERIA);
    Cafeteria vieja = cafeteriaRepository.saveAndFlush(nuevaCafeteria(cuenta));
    vieja.eliminar();
    cafeteriaRepository.saveAndFlush(vieja);

    Cafeteria nueva = cafeteriaRepository.saveAndFlush(nuevaCafeteria(cuenta));

    assertThat(cafeteriaRepository.findByCuentaIdAndEliminadaFalse(cuenta.getId()))
        .map(Cafeteria::getId)
        .contains(nueva.getId());
  }

  @Test
  void rechazaCuentaSinRolCafeteria() {
    Cuenta cuenta = nuevaCuenta("tostador@ejemplo.cr", Rol.TOSTADOR);

    assertThatThrownBy(() -> nuevaCafeteria(cuenta)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rechazaHorarioDeMasDe100Caracteres() {
    Cafeteria cafeteria = nuevaCafeteria(nuevaCuenta("horario@ejemplo.cr", Rol.CAFETERIA));
    cafeteria.setHorario("a".repeat(101));

    assertThatThrownBy(() -> cafeteriaRepository.saveAndFlush(cafeteria))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void elMenuDeUnaCafeteriaEliminadaSeConservaPeroNoSeMuestra() {
    Cafeteria cafeteria =
        cafeteriaRepository.saveAndFlush(
            nuevaCafeteria(nuevaCuenta("menu@ejemplo.cr", Rol.CAFETERIA)));
    nuevoLote("CRH-2026-0101");
    ItemMenu espresso =
        new ItemMenu(cafeteria, "Espresso Tarrazú", CategoriaMenu.CAFE, new BigDecimal("1800"));
    espresso.setCodigoTrazabilidad(" crh-2026-0101 ");
    itemMenuRepository.saveAndFlush(espresso);
    itemMenuRepository.saveAndFlush(
        new ItemMenu(cafeteria, "Queque", CategoriaMenu.OTRA_COMIDA, new BigDecimal("1500")));

    assertThat(
            itemMenuRepository.findByCafeteriaIdAndCafeteriaEliminadaFalseOrderByNombre(
                cafeteria.getId()))
        .extracting(ItemMenu::getCodigoTrazabilidad)
        .containsExactly("CRH-2026-0101", null);

    cafeteria.eliminar();
    cafeteriaRepository.saveAndFlush(cafeteria);
    entityManager.clear();

    assertThat(
            itemMenuRepository.findByCafeteriaIdAndCafeteriaEliminadaFalseOrderByNombre(
                cafeteria.getId()))
        .isEmpty();
    assertThat(itemMenuRepository.count()).isEqualTo(2);
  }

  @Test
  void soloLosItemsDeCafeLlevanCodigoDeTrazabilidad() {
    Cafeteria cafeteria =
        cafeteriaRepository.saveAndFlush(
            nuevaCafeteria(nuevaCuenta("codigo@ejemplo.cr", Rol.CAFETERIA)));
    nuevoLote("CRH-2026-0101");
    ItemMenu empanada =
        new ItemMenu(cafeteria, "Empanada", CategoriaMenu.OTRA_COMIDA, new BigDecimal("1200"));

    assertThatThrownBy(() -> empanada.setCodigoTrazabilidad("CRH-2026-0101"))
        .isInstanceOf(IllegalArgumentException.class);

    entityManager.flush();
    assertThatThrownBy(() ->
            entityManager
                .getEntityManager()
                .createNativeQuery(
                    "insert into item_menu (id, cafeteria_id, nombre, categoria,"
                        + " codigo_trazabilidad, precio, creado_en) values (gen_random_uuid(),"
                        + " ?1, 'Empanada', 'OTRA_COMIDA', 'CRH-2026-0101', 1200, now())")
                .setParameter(1, cafeteria.getId())
                .executeUpdate())
        .hasMessageContaining("ck_item_menu_trazabilidad");
  }

  @Test
  void rechazaCodigoQueNoEsDeUnLote() {
    Cafeteria cafeteria =
        cafeteriaRepository.saveAndFlush(
            nuevaCafeteria(nuevaCuenta("inexistente@ejemplo.cr", Rol.CAFETERIA)));
    ItemMenu espresso =
        new ItemMenu(cafeteria, "Espresso", CategoriaMenu.CAFE, new BigDecimal("1800"));
    espresso.setCodigoTrazabilidad("CRH-9999-0000");

    assertThatThrownBy(() -> itemMenuRepository.saveAndFlush(espresso))
        .isInstanceOf(DataIntegrityViolationException.class)
        .hasMessageContaining("CRH-9999-0000");
  }

  @Test
  void validaElCodigoTambienAlEditar() {
    Cafeteria cafeteria =
        cafeteriaRepository.saveAndFlush(
            nuevaCafeteria(nuevaCuenta("editar@ejemplo.cr", Rol.CAFETERIA)));
    ItemMenu espresso =
        itemMenuRepository.saveAndFlush(
            new ItemMenu(cafeteria, "Espresso", CategoriaMenu.CAFE, new BigDecimal("1800")));

    espresso.setCodigoTrazabilidad("TRZ-0101-MC");

    assertThatThrownBy(() -> itemMenuRepository.saveAndFlush(espresso))
        .isInstanceOf(DataIntegrityViolationException.class);
  }
}
