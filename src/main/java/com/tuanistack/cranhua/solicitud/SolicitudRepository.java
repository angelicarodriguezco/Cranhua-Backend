package com.tuanistack.cranhua.solicitud;

import com.tuanistack.cranhua.solicitud.dto.SolicitudEnviadaResponse;
import com.tuanistack.cranhua.solicitud.dto.SolicitudRecibidaResponse;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SolicitudRepository extends JpaRepository<Solicitud, UUID> {

  List<Solicitud> findByCosechaIdAndEstado(UUID cosechaId, EstadoSolicitud estado);

  List<Solicitud> findByTostadorIdOrderBySolicitadaEnDesc(UUID tostadorId);

  long countByTostadorIdAndEstadoIn(UUID tostadorId, Collection<EstadoSolicitud> estados);

  @Query(
      """
      select coalesce(sum(s.cantidadKg), 0)
      from Solicitud s
      where s.tostador.id = :tostadorId
        and s.estado = com.tuanistack.cranhua.solicitud.EstadoSolicitud.APROBADA
        and s.respondidaEn >= :desde
        and s.respondidaEn < :hasta
      """)
  BigDecimal kgAprobadosEntre(
      @Param("tostadorId") UUID tostadorId,
      @Param("desde") Instant desde,
      @Param("hasta") Instant hasta);

  // En bloque para no pisar una aprobación que el cafetalero esté haciendo al mismo tiempo
  @Modifying
  @Query(
      """
      update Solicitud s
      set s.estado = com.tuanistack.cranhua.solicitud.EstadoSolicitud.RECHAZADA,
          s.respondidaEn = :ahora
      where s.tostador.id = :tostadorId
        and s.estado = com.tuanistack.cranhua.solicitud.EstadoSolicitud.PENDIENTE
      """)
  void rechazarPendientes(@Param("tostadorId") UUID tostadorId, @Param("ahora") Instant ahora);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select s from Solicitud s where s.id = :id")
  Optional<Solicitud> findParaResponder(@Param("id") UUID id);

  @Query(
      """
      select new com.tuanistack.cranhua.solicitud.dto.SolicitudEnviadaResponse(
        s.id, c.id, f.id, f.nombre, c.variedad, l.codigo, s.cantidadKg, s.solicitadaEn, s.estado)
      from Solicitud s
      join s.cosecha c
      join c.finca f
      left join Lote l on l.solicitud = s
      where s.tostador.id = :tostadorId
      order by s.solicitadaEn desc
      """)
  List<SolicitudEnviadaResponse> enviadas(@Param("tostadorId") UUID tostadorId);

  @Query(
      """
      select new com.tuanistack.cranhua.solicitud.dto.SolicitudRecibidaResponse(
        s.id, c.id, t.nombreCompleto, c.variedad, f.nombre, s.cantidadKg, s.solicitadaEn, s.estado)
      from Solicitud s
      join s.tostador t
      join s.cosecha c
      join c.finca f
      join f.duenos d
      where d.id = :duenoId
        and f.eliminada = false
        and (:estado is null or s.estado = :estado)
      order by s.solicitadaEn desc
      """)
  List<SolicitudRecibidaResponse> recibidas(
      @Param("duenoId") UUID duenoId, @Param("estado") EstadoSolicitud estado);
}
