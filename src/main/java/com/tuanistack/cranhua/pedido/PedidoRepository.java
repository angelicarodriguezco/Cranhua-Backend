package com.tuanistack.cranhua.pedido;

import com.tuanistack.cranhua.pedido.dto.PedidoRecienteResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PedidoRepository extends JpaRepository<Pedido, UUID> {

  List<Pedido> findByCafeteriaIdOrderByPedidoEnDesc(UUID cafeteriaId);

  long countByCafeteriaIdAndEstado(UUID cafeteriaId, EstadoPedido estado);

  @Query(
      """
      select coalesce(sum(p.cantidadKg), 0)
      from Pedido p
      where p.tueste.id = :tuesteId
        and p.estado in (
          com.tuanistack.cranhua.pedido.EstadoPedido.EN_CAMINO,
          com.tuanistack.cranhua.pedido.EstadoPedido.ENTREGADO)
      """)
  BigDecimal kgComprometidos(@Param("tuesteId") UUID tuesteId);

  @Query(
      """
      select coalesce(sum(p.cantidadKg), 0)
      from Pedido p
      where p.cafeteria.id = :cafeteriaId
        and p.estado = com.tuanistack.cranhua.pedido.EstadoPedido.ENTREGADO
      """)
  BigDecimal kgEntregados(@Param("cafeteriaId") UUID cafeteriaId);

  // Un tostador cuenta si tiene al menos un pedido sin cancelar
  @Query(
      """
      select count(distinct s.tostador.id)
      from Pedido p
      join p.tueste t
      join t.lote l
      join l.solicitud s
      where p.cafeteria.id = :cafeteriaId
        and p.estado <> com.tuanistack.cranhua.pedido.EstadoPedido.CANCELADO
      """)
  long contarTostadoresActivos(@Param("cafeteriaId") UUID cafeteriaId);

  @Query(
      """
      select new com.tuanistack.cranhua.pedido.dto.PedidoRecienteResponse(
        p.id, coalesce(n.nombre, c.nombreCompleto), p.cantidadKg, p.pedidoEn, p.estado)
      from Pedido p
      join p.tueste t
      join t.lote l
      join l.solicitud s
      join s.tostador c
      left join NegocioTueste n on n.cuenta = c
      where p.cafeteria.id = :cafeteriaId
      order by p.pedidoEn desc, p.id
      """)
  List<PedidoRecienteResponse> recientes(@Param("cafeteriaId") UUID cafeteriaId, Pageable pageable);
}
