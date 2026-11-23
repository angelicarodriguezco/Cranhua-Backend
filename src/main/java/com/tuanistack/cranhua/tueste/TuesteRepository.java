package com.tuanistack.cranhua.tueste;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
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

  @Query(
      """
      select t.pesoTostadoKg - coalesce(
        (select sum(p.cantidadKg) from Pedido p
         where p.tueste = t
           and p.estado in (
             com.tuanistack.cranhua.pedido.EstadoPedido.EN_CAMINO,
             com.tuanistack.cranhua.pedido.EstadoPedido.ENTREGADO)), 0)
      from Tueste t
      where t.id = :tuesteId
        and t.lote.estadoTrazabilidad = com.tuanistack.cranhua.lote.EstadoTrazabilidad.ACTIVO
      """)
  Optional<BigDecimal> kgDisponibles(@Param("tuesteId") UUID tuesteId);
}
