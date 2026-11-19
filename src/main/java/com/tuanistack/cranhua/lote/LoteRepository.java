package com.tuanistack.cranhua.lote;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface LoteRepository extends JpaRepository<Lote, UUID> {

  Optional<Lote> findByCodigo(String codigo);

  @Query(value = "select nextval('lote_codigo_seq')", nativeQuery = true)
  long siguienteNumeroCodigo();
}
