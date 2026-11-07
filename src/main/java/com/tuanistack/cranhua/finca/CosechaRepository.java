package com.tuanistack.cranhua.finca;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CosechaRepository extends JpaRepository<Cosecha, UUID> {

  List<Cosecha> findByFincaId(UUID fincaId);
}
