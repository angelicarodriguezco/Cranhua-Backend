package com.tuanistack.cranhua.finca;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CosechaRepository extends JpaRepository<Cosecha, UUID> {

  List<Cosecha> findByFincaId(UUID fincaId);

  List<Cosecha> findByFincaIdOrderByFechaInicioDescIdAsc(UUID fincaId);

  @Query(
      """
      select c.finca.id, count(c), sum(c.kgDisponibles) from Cosecha c
      where c.finca.id in :fincaIds and c.estado = :estado and c.kgDisponibles > 0
      group by c.finca.id
      """)
  List<Object[]> resumenDisponibles(
      @Param("fincaIds") Collection<UUID> fincaIds, @Param("estado") EstadoCosecha estado);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select c from Cosecha c where c.id = :id")
  Cosecha findParaDescontar(@Param("id") UUID id);
}
