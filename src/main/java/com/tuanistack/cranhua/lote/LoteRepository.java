package com.tuanistack.cranhua.lote;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LoteRepository extends JpaRepository<Lote, UUID> {

  Optional<Lote> findByCodigo(String codigo);

  @Query(value = "select nextval('lote_codigo_seq')", nativeQuery = true)
  long siguienteNumeroCodigo();

  @Query(
      """
      select l from Lote l
      join fetch l.solicitud s
      join fetch s.cosecha c
      join fetch c.finca
      where s.tostador.id = :tostadorId
      order by s.respondidaEn desc
      """)
  List<Lote> findDelTostador(@Param("tostadorId") UUID tostadorId);
}
