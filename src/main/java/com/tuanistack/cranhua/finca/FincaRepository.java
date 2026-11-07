package com.tuanistack.cranhua.finca;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FincaRepository extends JpaRepository<Finca, UUID> {

  Optional<Finca> findByIdAndEliminadaFalse(UUID id);

  List<Finca> findByDuenosIdAndEliminadaFalse(UUID cuentaId);
}
