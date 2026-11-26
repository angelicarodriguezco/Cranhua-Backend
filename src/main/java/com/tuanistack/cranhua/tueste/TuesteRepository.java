package com.tuanistack.cranhua.tueste;

import com.tuanistack.cranhua.tueste.dto.TuesteDisponibleExploracionResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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
        and t.retirado = false
        and t.lote.estadoTrazabilidad = com.tuanistack.cranhua.lote.EstadoTrazabilidad.ACTIVO
      """)
  Optional<BigDecimal> kgDisponibles(@Param("tuesteId") UUID tuesteId);

  @Query(
      """
      select n.id, coalesce(sum(
        t.pesoTostadoKg - coalesce(
          (select sum(p.cantidadKg) from Pedido p
           where p.tueste = t
             and p.estado in (
               com.tuanistack.cranhua.pedido.EstadoPedido.EN_CAMINO,
               com.tuanistack.cranhua.pedido.EstadoPedido.ENTREGADO)), 0)
      ), 0)
      from Tueste t
      join t.lote l
      join l.solicitud s
      join NegocioTueste n on n.cuenta = s.tostador
      where n.eliminado = false
        and t.retirado = false
        and l.estadoTrazabilidad = com.tuanistack.cranhua.lote.EstadoTrazabilidad.ACTIVO
      group by n.id
      """)
  List<Object[]> kgDisponiblesPorNegocio();

  @Query(
      """
      select new com.tuanistack.cranhua.tueste.dto.TuesteDisponibleExploracionResponse(
        t.id,
        f.nombre,
        c.variedad,
        t.nivel,
        t.pesoTostadoKg - coalesce(
          (select sum(p.cantidadKg) from Pedido p
           where p.tueste = t
             and p.estado in (
               com.tuanistack.cranhua.pedido.EstadoPedido.EN_CAMINO,
               com.tuanistack.cranhua.pedido.EstadoPedido.ENTREGADO)), 0)
      )
      from Tueste t
      join t.lote l
      join l.solicitud s
      join s.cosecha c
      join c.finca f
      join NegocioTueste n on n.cuenta = s.tostador
      where n.id = :negocioId
        and t.retirado = false
        and l.estadoTrazabilidad = com.tuanistack.cranhua.lote.EstadoTrazabilidad.ACTIVO
        and t.pesoTostadoKg - coalesce(
          (select sum(p.cantidadKg) from Pedido p
           where p.tueste = t
             and p.estado in (
               com.tuanistack.cranhua.pedido.EstadoPedido.EN_CAMINO,
               com.tuanistack.cranhua.pedido.EstadoPedido.ENTREGADO)), 0) > 0
      order by t.fechaTueste desc, t.id
      """)
  List<TuesteDisponibleExploracionResponse> disponiblesDelNegocio(
      @Param("negocioId") UUID negocioId);

  @Modifying
  @Query(
      """
      update Tueste t set t.retirado = true
      where t.lote.id in (select l.id from Lote l where l.solicitud.tostador.id = :tostadorId)
      """)
  void retirarDelTostador(@Param("tostadorId") UUID tostadorId);
}
