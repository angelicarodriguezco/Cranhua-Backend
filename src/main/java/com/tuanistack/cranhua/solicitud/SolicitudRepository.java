package com.tuanistack.cranhua.solicitud;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SolicitudRepository extends JpaRepository<Solicitud, UUID> {

  List<Solicitud> findByCosechaIdAndEstado(UUID cosechaId, EstadoSolicitud estado);

  List<Solicitud> findByTostadorIdOrderBySolicitadaEnDesc(UUID tostadorId);

  // Bloquea la fila para que dos respuestas a la misma solicitud no se crucen
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select s from Solicitud s where s.id = :id")
  Optional<Solicitud> findParaResponder(@Param("id") UUID id);
}
