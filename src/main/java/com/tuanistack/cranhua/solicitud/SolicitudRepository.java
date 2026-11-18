package com.tuanistack.cranhua.solicitud;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SolicitudRepository extends JpaRepository<Solicitud, UUID> {

  List<Solicitud> findByCosechaIdAndEstado(UUID cosechaId, EstadoSolicitud estado);

  List<Solicitud> findByTostadorIdOrderBySolicitadaEnDesc(UUID tostadorId);
}
