package com.tuanistack.cranhua.cuenta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.tuanistack.cranhua.BasePostgis;
import com.tuanistack.cranhua.config.SecurityConfig;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

// Usa el PostGIS de Testcontainers con el esquema de las migraciones de Flyway
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(BasePostgis.class)
class CuentaRepositoryTest {

  private final PasswordEncoder passwordEncoder = new SecurityConfig().passwordEncoder();

  @Autowired private CuentaRepository cuentaRepository;

  @Autowired private TestEntityManager entityManager;

  @Autowired private JdbcTemplate jdbcTemplate;

  private Cuenta nuevaCuenta(String correo, Set<Rol> roles) {
    return new Cuenta("Rodrigo Alvarado", correo, passwordEncoder.encode("Cafe2026!"), roles);
  }

  private Cuenta guardarYLeer(Cuenta cuenta) {
    Cuenta guardada = cuentaRepository.saveAndFlush(cuenta);
    entityManager.clear();
    return cuentaRepository.findById(guardada.getId()).orElseThrow();
  }

  @Test
  void guardaYLeeCuentaConUnRol() {
    Cuenta leida = guardarYLeer(nuevaCuenta("rodrigo@ejemplo.cr", Set.of(Rol.CAFETALERO)));

    assertThat(leida.getId()).isNotNull();
    assertThat(leida.getNombreCompleto()).isEqualTo("Rodrigo Alvarado");
    assertThat(leida.getRoles()).containsExactly(Rol.CAFETALERO);
    assertThat(leida.getCreadaEn()).isNotNull();
    assertThat(leida.isEliminada()).isFalse();
  }

  @Test
  void guardaYLeeCuentaConLosTresRoles() {
    Cuenta leida = guardarYLeer(nuevaCuenta("mariana@ejemplo.cr", EnumSet.allOf(Rol.class)));

    assertThat(leida.getRoles())
        .containsExactlyInAnyOrder(Rol.CAFETALERO, Rol.TOSTADOR, Rol.CAFETERIA);
  }

  @Test
  void guardaElCorreoEnMinusculasYSinEspacios() {
    Cuenta leida = guardarYLeer(nuevaCuenta("  Andres.Solano@Ejemplo.CR ", Set.of(Rol.CAFETERIA)));

    assertThat(leida.getCorreo()).isEqualTo("andres.solano@ejemplo.cr");
    assertThat(cuentaRepository.existsByCorreo("andres.solano@ejemplo.cr")).isTrue();
  }

  @Test
  void noPermiteDosCuentasConElMismoCorreo() {
    cuentaRepository.saveAndFlush(nuevaCuenta("lucia@ejemplo.cr", Set.of(Rol.TOSTADOR)));

    assertThatThrownBy(
            () ->
                cuentaRepository.saveAndFlush(
                    nuevaCuenta("LUCIA@Ejemplo.cr", Set.of(Rol.CAFETERIA))))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void fallaSinCorreo() {
    assertThatThrownBy(() -> cuentaRepository.saveAndFlush(nuevaCuenta(null, Set.of(Rol.TOSTADOR))))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void fallaSinNombre() {
    Cuenta cuenta = nuevaCuenta("sin.nombre@ejemplo.cr", Set.of(Rol.TOSTADOR));
    cuenta.setNombreCompleto(null);

    assertThatThrownBy(() -> cuentaRepository.saveAndFlush(cuenta))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void fallaSinContrasena() {
    Cuenta cuenta = nuevaCuenta("sin.clave@ejemplo.cr", Set.of(Rol.TOSTADOR));
    cuenta.setContrasenaHash(null);

    assertThatThrownBy(() -> cuentaRepository.saveAndFlush(cuenta))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void telefonoFotoYRedesSonOpcionales() {
    Cuenta leida = guardarYLeer(nuevaCuenta("opcionales@ejemplo.cr", Set.of(Rol.CAFETALERO)));

    assertThat(leida.getTelefono()).isNull();
    assertThat(leida.getFotoUrl()).isNull();
    assertThat(leida.getRedes()).isEmpty();
  }

  @Test
  void guardaTelefonoFotoYRedes() {
    Cuenta cuenta = nuevaCuenta("redes@ejemplo.cr", Set.of(Rol.CAFETERIA));
    cuenta.setTelefono("+506 8888-1234");
    cuenta.setFotoUrl("https://res.cloudinary.com/cranhua/foto.jpg");
    cuenta.getRedes().add(new RedSocial(TipoRedSocial.INSTAGRAM, "@cafebrumas"));
    cuenta.getRedes().add(new RedSocial(TipoRedSocial.SITIO_WEB, "cafebrumas.cr"));

    Cuenta leida = guardarYLeer(cuenta);

    assertThat(leida.getTelefono()).isEqualTo("+506 8888-1234");
    assertThat(leida.getRedes())
        .extracting(RedSocial::getTipo, RedSocial::getValor)
        .containsExactlyInAnyOrder(
            tuple(TipoRedSocial.INSTAGRAM, "@cafebrumas"),
            tuple(TipoRedSocial.SITIO_WEB, "cafebrumas.cr"));
  }

  @Test
  void guardaLaContrasenaComoHashBcrypt() {
    Cuenta leida = guardarYLeer(nuevaCuenta("clave@ejemplo.cr", Set.of(Rol.TOSTADOR)));

    assertThat(leida.getContrasenaHash()).isNotEqualTo("Cafe2026!").startsWith("$2a$");
    assertThat(passwordEncoder.matches("Cafe2026!", leida.getContrasenaHash())).isTrue();
  }

  @Test
  void eliminarEsBorradoLogico() {
    Cuenta cuenta =
        cuentaRepository.saveAndFlush(nuevaCuenta("baja@ejemplo.cr", Set.of(Rol.TOSTADOR)));

    cuenta.eliminar();
    cuentaRepository.saveAndFlush(cuenta);
    entityManager.clear();

    assertThat(cuentaRepository.findByCorreoAndEliminadaFalse("baja@ejemplo.cr")).isEmpty();
    Cuenta leida = cuentaRepository.findById(cuenta.getId()).orElseThrow();
    assertThat(leida.isEliminada()).isTrue();
    assertThat(leida.getEliminadaEn()).isNotNull();
    assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from cuenta where correo = ?", Integer.class, "baja@ejemplo.cr"))
        .isEqualTo(1);
  }

  @Test
  void encuentraCuentasActivasPorCorreo() {
    cuentaRepository.saveAndFlush(nuevaCuenta("activa@ejemplo.cr", Set.of(Rol.CAFETALERO)));

    assertThat(cuentaRepository.findByCorreoAndEliminadaFalse("activa@ejemplo.cr")).isPresent();
  }
}
