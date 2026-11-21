package com.tuanistack.cranhua.pedido;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PedidoRepository extends JpaRepository<Pedido, UUID> {

  List<Pedido> findByCafeteriaIdOrderByPedidoEnDesc(UUID cafeteriaId);

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
}
