package com.tuanistack.cranhua.cuenta;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CuentaRepository extends JpaRepository<Cuenta, UUID> {

  Optional<Cuenta> findByCorreoAndEliminadaFalse(String correo);

  Optional<Cuenta> findByGoogleIdAndEliminadaFalse(String googleId);

  boolean existsByCorreo(String correo);

  @Query("select c.versionSesion from Cuenta c where c.id = :id")
  Optional<Integer> buscarVersionSesion(UUID id);
}
