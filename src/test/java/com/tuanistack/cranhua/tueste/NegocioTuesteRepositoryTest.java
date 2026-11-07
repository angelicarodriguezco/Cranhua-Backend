package com.tuanistack.cranhua.tueste;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tuanistack.cranhua.BasePostgis;
import com.tuanistack.cranhua.cuenta.Cuenta;
import com.tuanistack.cranhua.cuenta.CuentaRepository;
import com.tuanistack.cranhua.cuenta.Rol;
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
class NegocioTuesteRepositoryTest {

  @Autowired private NegocioTuesteRepository negocioRepository;

  @Autowired private CuentaRepository cuentaRepository;

  @Autowired private TestEntityManager entityManager;

  private Cuenta nuevaCuenta(String correo, Rol rol) {
    return cuentaRepository.saveAndFlush(new Cuenta("Mariana Castro", correo, "hash", Set.of(rol)));
  }

  private NegocioTueste nuevoNegocio(Cuenta cuenta, Set<NivelTueste> estilos) {
    return new NegocioTueste(
        cuenta,
        " Tostaduría Volcán ",
        "Mariana Castro",
        "+506 8888-1234",
        "Volcan@Ejemplo.CR",
        "San José",
        "San José",
        estilos);
  }

  @Test
  void guardaYLeeNegocioConVariosEstilos() {
    NegocioTueste negocio =
        nuevoNegocio(
            nuevaCuenta("tostador@ejemplo.cr", Rol.TOSTADOR),
            Set.of(NivelTueste.CLARO, NivelTueste.OSCURO));
    negocio.setDescripcion("Perfiles claros para resaltar la acidez.");
    negocio.setPortadaUrl("https://res.cloudinary.com/cranhua/portada.jpg");
    negocioRepository.saveAndFlush(negocio);
    entityManager.clear();

    NegocioTueste leido = negocioRepository.findById(negocio.getId()).orElseThrow();

    assertThat(leido.getNombre()).isEqualTo("Tostaduría Volcán");
    assertThat(leido.getCorreo()).isEqualTo("volcan@ejemplo.cr");
    assertThat(leido.getCanton()).isEqualTo("San José");
    assertThat(leido.getEstilos()).containsExactlyInAnyOrder(NivelTueste.CLARO, NivelTueste.OSCURO);
    assertThat(leido.getPortadaUrl()).isEqualTo("https://res.cloudinary.com/cranhua/portada.jpg");
    assertThat(leido.getCreadoEn()).isNotNull();
  }

  @Test
  void unaCuentaTostadoraTieneUnSoloNegocio() {
    Cuenta cuenta = nuevaCuenta("unico@ejemplo.cr", Rol.TOSTADOR);
    negocioRepository.saveAndFlush(nuevoNegocio(cuenta, Set.of(NivelTueste.MEDIO)));

    assertThatThrownBy(
            () -> negocioRepository.saveAndFlush(nuevoNegocio(cuenta, Set.of(NivelTueste.CLARO))))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void rechazaCuentaSinRolTostador() {
    Cuenta cuenta = nuevaCuenta("cafetalero@ejemplo.cr", Rol.CAFETALERO);

    assertThatThrownBy(() -> nuevoNegocio(cuenta, Set.of(NivelTueste.MEDIO)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rechazaDescripcionDeMasDe300Caracteres() {
    NegocioTueste negocio =
        nuevoNegocio(nuevaCuenta("largo@ejemplo.cr", Rol.TOSTADOR), Set.of(NivelTueste.MEDIO));
    negocio.setDescripcion("a".repeat(301));

    assertThatThrownBy(() -> negocioRepository.saveAndFlush(negocio))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void eliminarEsBorradoLogico() {
    Cuenta cuenta = nuevaCuenta("baja@ejemplo.cr", Rol.TOSTADOR);
    NegocioTueste negocio =
        negocioRepository.saveAndFlush(nuevoNegocio(cuenta, Set.of(NivelTueste.MEDIO)));

    negocio.eliminar();
    negocioRepository.saveAndFlush(negocio);
    entityManager.clear();

    assertThat(negocioRepository.findByCuentaIdAndEliminadoFalse(cuenta.getId())).isEmpty();
    NegocioTueste leido = negocioRepository.findById(negocio.getId()).orElseThrow();
    assertThat(leido.isEliminado()).isTrue();
    assertThat(leido.getEliminadoEn()).isNotNull();
  }
}
