package com.tuanistack.cranhua.tueste;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NegocioTuesteRepository extends JpaRepository<NegocioTueste, UUID> {

  Optional<NegocioTueste> findByIdAndEliminadoFalse(UUID id);

  Optional<NegocioTueste> findByCuentaIdAndEliminadoFalse(UUID cuentaId);

  boolean existsByCuentaId(UUID cuentaId);
}
