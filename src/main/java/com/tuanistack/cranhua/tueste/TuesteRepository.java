package com.tuanistack.cranhua.tueste;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TuesteRepository extends JpaRepository<Tueste, UUID> {

  List<Tueste> findByLoteIdOrderByFechaTuesteDesc(UUID loteId);

  @Query(
      """
      select coalesce(sum(t.pesoVerdeKg), 0)
      from Tueste t
      where t.lote.id = :loteId
        and (:tuesteId is null or t.id <> :tuesteId)
      """)
  BigDecimal sumPesoVerdeKgByLoteIdExcluding(
      @Param("loteId") UUID loteId, @Param("tuesteId") UUID tuesteId);
}
