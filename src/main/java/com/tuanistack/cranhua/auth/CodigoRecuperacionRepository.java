package com.tuanistack.cranhua.auth;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface CodigoRecuperacionRepository extends JpaRepository<CodigoRecuperacion, UUID> {

  // Bloquea la fila para que intentos en paralelo no se salten el límite de fallos
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<CodigoRecuperacion> findByCuentaId(UUID cuentaId);
}
