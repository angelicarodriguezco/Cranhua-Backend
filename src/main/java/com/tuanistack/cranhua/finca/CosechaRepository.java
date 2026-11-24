package com.tuanistack.cranhua.finca;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CosechaRepository extends JpaRepository<Cosecha, UUID> {

  List<Cosecha> findByFincaIdAndEliminadaFalse(UUID fincaId);

  List<Cosecha> findByFincaIdAndEliminadaFalseOrderByFechaInicioDescIdAsc(UUID fincaId);

  @Query(
      """
      select c from Cosecha c join fetch c.finca f left join fetch c.procesos
      where c.estado = :estado and c.kgDisponibles > 0
        and c.eliminada = false and f.eliminada = false
        and not exists (select d from f.duenos d where d.id = :cuentaId)
      order by c.creadaEn desc, c.id
      """)
  List<Cosecha> disponiblesParaOtros(
      @Param("cuentaId") UUID cuentaId, @Param("estado") EstadoCosecha estado);

  @Query(
      """
      select c.finca.id, count(c), sum(c.kgDisponibles) from Cosecha c
      where c.finca.id in :fincaIds and c.estado = :estado and c.kgDisponibles > 0
        and c.eliminada = false
      group by c.finca.id
      """)
  List<Object[]> resumenDisponibles(
      @Param("fincaIds") Collection<UUID> fincaIds, @Param("estado") EstadoCosecha estado);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select c from Cosecha c where c.id = :id")
  Cosecha findParaDescontar(@Param("id") UUID id);

  @Query(
      """
      select count(c) from Cosecha c join c.finca f
      where c.estado = :estado and c.eliminada = false and f.eliminada = false
        and exists (select d from f.duenos d where d.id = :duenoId)
      """)
  long contarDelDueno(@Param("duenoId") UUID duenoId, @Param("estado") EstadoCosecha estado);

  @Query(
      """
      select coalesce(sum(c.kgDisponibles), 0) from Cosecha c join c.finca f
      where c.estado = :estado and c.eliminada = false and f.eliminada = false
        and exists (select d from f.duenos d where d.id = :duenoId)
      """)
  BigDecimal kgDisponiblesDelDueno(
      @Param("duenoId") UUID duenoId, @Param("estado") EstadoCosecha estado);

  @Query(
      """
      select c from Cosecha c join fetch c.finca f
      where c.eliminada = false and f.eliminada = false
        and exists (select d from f.duenos d where d.id = :duenoId)
      order by c.creadaEn desc, c.id
      """)
  List<Cosecha> recientesDelDueno(@Param("duenoId") UUID duenoId, Pageable pageable);

  @Query(
      value =
          """
          select c from Cosecha c join fetch c.finca f
          where c.eliminada = false and f.eliminada = false
            and exists (select d from f.duenos d where d.id = :duenoId)
            and (:fincaId is null or f.id = :fincaId)
            and c.fechaInicio between :desde and :hasta
          order by c.fechaInicio desc, c.id
          """,
      countQuery =
          """
          select count(c) from Cosecha c join c.finca f
          where c.eliminada = false and f.eliminada = false
            and exists (select d from f.duenos d where d.id = :duenoId)
            and (:fincaId is null or f.id = :fincaId)
            and c.fechaInicio between :desde and :hasta
          """)
  Page<Cosecha> historialDelDueno(
      @Param("duenoId") UUID duenoId,
      @Param("fincaId") UUID fincaId,
      @Param("desde") LocalDate desde,
      @Param("hasta") LocalDate hasta,
      Pageable pageable);

  @Query(
      """
      select coalesce(sum(c.cantidadCosechadaKg), 0) from Cosecha c join c.finca f
      where c.eliminada = false and f.eliminada = false
        and exists (select d from f.duenos d where d.id = :duenoId)
        and (:fincaId is null or f.id = :fincaId)
        and c.fechaInicio between :desde and :hasta
      """)
  BigDecimal produccionDelDueno(
      @Param("duenoId") UUID duenoId,
      @Param("fincaId") UUID fincaId,
      @Param("desde") LocalDate desde,
      @Param("hasta") LocalDate hasta);
}
