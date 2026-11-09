package com.tuanistack.cranhua.cafeteria;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CafeteriaRepository extends JpaRepository<Cafeteria, UUID> {

    Optional<Cafeteria> findByIdAndEliminadaFalse(UUID id);

    Optional<Cafeteria> findByCuentaIdAndEliminadaFalse(UUID cuentaId);

    boolean existsByCuentaIdAndEliminadaFalse(UUID cuentaId);
}