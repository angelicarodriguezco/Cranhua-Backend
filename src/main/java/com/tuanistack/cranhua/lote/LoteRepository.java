package com.tuanistack.cranhua.lote;

import com.tuanistack.cranhua.historial.dto.LoteVendidoResponse;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LoteRepository extends JpaRepository<Lote, UUID> {

  Optional<Lote> findByCodigo(String codigo);

  boolean existsByCodigoAndEstadoTrazabilidad(String codigo, EstadoTrazabilidad estado);

  long countBySolicitudTostadorId(UUID tostadorId);

  @Query(
      """
      select l from Lote l
      join fetch l.solicitud s
      join fetch s.cosecha c
      join fetch c.finca
      join fetch s.tostador
      where l.codigo = :codigo
        and l.estadoTrazabilidad = com.tuanistack.cranhua.lote.EstadoTrazabilidad.ACTIVO
      """)
  Optional<Lote> findActivoParaTrazabilidad(@Param("codigo") String codigo);

  @Query(
      """
      select l.codigo from Lote l
      where l.solicitud.cosecha.finca.id = :fincaId
        and l.estadoTrazabilidad = com.tuanistack.cranhua.lote.EstadoTrazabilidad.ACTIVO
      order by l.activadoEn desc, l.codigo
      limit 1
      """)
  Optional<String> codigoActivoDeFinca(@Param("fincaId") UUID fincaId);

  @Query(
      """
      select l.codigo from Lote l
      where l.solicitud.tostador.id = :tostadorId
        and l.estadoTrazabilidad = com.tuanistack.cranhua.lote.EstadoTrazabilidad.ACTIVO
      order by l.activadoEn desc, l.codigo
      limit 1
      """)
  Optional<String> codigoActivoDelTostador(@Param("tostadorId") UUID tostadorId);

  @Query(
      """
      select l.codigo from Lote l
      join l.solicitud s
      join s.cosecha c
      join c.finca f
      where f.eliminada = false
        and exists (select d from f.duenos d where d.id = :duenoId)
        and l.estadoTrazabilidad = com.tuanistack.cranhua.lote.EstadoTrazabilidad.ACTIVO
      order by l.activadoEn desc, l.codigo
      limit 1
      """)
  Optional<String> codigoActivoDeFincasDelDueno(@Param("duenoId") UUID duenoId);

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

  @Query(
      value =
          """
          select new com.tuanistack.cranhua.historial.dto.LoteVendidoResponse(
            l.id, l.codigo, c.id, f.id, f.nombre, c.variedad, t.nombreCompleto,
            s.cantidadKg, s.precioKg, s.totalEstimado, s.respondidaEn)
          from Lote l
          join l.solicitud s
          join s.tostador t
          join s.cosecha c
          join c.finca f
          where f.eliminada = false
            and exists (select d from f.duenos d where d.id = :duenoId)
            and (:fincaId is null or f.id = :fincaId)
            and s.respondidaEn >= :desde
            and s.respondidaEn < :hasta
          order by s.respondidaEn desc, l.id
          """,
      countQuery =
          """
          select count(l) from Lote l
          join l.solicitud s
          join s.cosecha c
          join c.finca f
          where f.eliminada = false
            and exists (select d from f.duenos d where d.id = :duenoId)
            and (:fincaId is null or f.id = :fincaId)
            and s.respondidaEn >= :desde
            and s.respondidaEn < :hasta
          """)
  Page<LoteVendidoResponse> vendidosDelDueno(
      @Param("duenoId") UUID duenoId,
      @Param("fincaId") UUID fincaId,
      @Param("desde") Instant desde,
      @Param("hasta") Instant hasta,
      Pageable pageable);
}
