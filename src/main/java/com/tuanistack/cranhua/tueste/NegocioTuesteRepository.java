package com.tuanistack.cranhua.tueste;

import com.tuanistack.cranhua.mapa.PuntoMapa;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface NegocioTuesteRepository extends JpaRepository<NegocioTueste, UUID> {

  Optional<NegocioTueste> findByIdAndEliminadoFalse(UUID id);

  Optional<NegocioTueste> findByCuentaIdAndEliminadoFalse(UUID cuentaId);

  boolean existsByCuentaId(UUID cuentaId);

  @Query(
      """
      select distinct n from NegocioTueste n
      left join fetch n.estilos
      where n.eliminado = false
      order by n.nombre, n.id
      """)
  List<NegocioTueste> activosParaExplorar();

  @Query(
      value =
          """
          select n.id, n.nombre, n.canton, n.provincia,
                 ST_Y(c.cabecera) as lat, ST_X(c.cabecera) as lng
          from negocio_tueste n
          join canton c on c.nombre = n.canton and c.provincia = n.provincia
          where n.eliminado = false
          order by n.nombre, n.id
          """,
      nativeQuery = true)
  List<PuntoMapa> puntosMapa();
}
