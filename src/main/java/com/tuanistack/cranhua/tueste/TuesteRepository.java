package com.tuanistack.cranhua.tueste;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TuesteRepository extends JpaRepository<Tueste, UUID> {

  List<Tueste> findByLoteIdOrderByFechaTuesteDesc(UUID loteId);
}
