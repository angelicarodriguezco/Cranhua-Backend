package com.tuanistack.cranhua.cafeteria;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItemMenuRepository extends JpaRepository<ItemMenu, UUID> {

  List<ItemMenu> findByCafeteriaIdAndCafeteriaEliminadaFalseOrderByNombre(UUID cafeteriaId);

  long countByCafeteriaIdAndCategoriaAndCodigoTrazabilidadIsNotNull(
      UUID cafeteriaId, CategoriaMenu categoria);

  @Query(
      """
      select i.codigoTrazabilidad from ItemMenu i
      where i.cafeteria.id = :cafeteriaId
        and i.codigoTrazabilidad is not null
      order by i.creadoEn desc, i.codigoTrazabilidad
      limit 1
      """)
  Optional<String> codigoDelMenu(@Param("cafeteriaId") UUID cafeteriaId);

  @Query(
      """
      select i.nombre as nombre, i.categoria as categoria, i.precio as precio,
        (select t.nivel from Tueste t
         where t.lote = l
         order by
           case when exists (
             select p.id from Pedido p
             where p.tueste = t and p.cafeteria = i.cafeteria
               and p.estado <> com.tuanistack.cranhua.pedido.EstadoPedido.CANCELADO)
             then 0 else 1 end,
           t.fechaTueste desc, t.creadoEn desc
         limit 1) as nivelTueste,
        co.notasSabor as notasSabor, f as finca
      from ItemMenu i
      left join Lote l on l.codigo = i.codigoTrazabilidad
      left join l.solicitud s
      left join Cosecha co on co = s.cosecha
        or (l.id is null and co.codigoLote = i.codigoTrazabilidad)
      left join co.finca f on f.eliminada = false
      where i.cafeteria.id = :cafeteriaId
      order by i.nombre, i.id
      """)
  List<ItemMenuPublico> menuPublico(@Param("cafeteriaId") UUID cafeteriaId);
}
