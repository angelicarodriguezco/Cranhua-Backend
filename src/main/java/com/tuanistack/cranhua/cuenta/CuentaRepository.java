package com.tuanistack.cranhua.cuenta;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CuentaRepository extends JpaRepository<Cuenta, UUID> {

  Optional<Cuenta> findByCorreoAndEliminadaFalse(String correo);

  boolean existsByCorreo(String correo);
}
