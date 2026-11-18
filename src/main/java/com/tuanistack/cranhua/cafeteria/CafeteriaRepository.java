package com.tuanistack.cranhua.cafeteria;

import com.tuanistack.cranhua.mapa.PuntoMapa;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

public interface CafeteriaRepository extends JpaRepository<Cafeteria, UUID> {

  Optional<Cafeteria> findByIdAndEliminadaFalse(UUID id);

  Optional<Cafeteria> findByCuentaIdAndEliminadaFalse(UUID cuentaId);

  boolean existsByCuentaIdAndEliminadaFalse(UUID cuentaId);

  @Query(
      """
      select distinct c from ItemMenu i
      join i.cafeteria c
      join Cosecha co on co.codigoLote = i.codigoTrazabilidad
      where co.finca.id = :fincaId and c.eliminada = false
      order by c.nombre
      """)
  List<Cafeteria> vendenCafeDeFinca(@Param("fincaId") UUID fincaId);
      value =
          """
          select id, nombre, canton, provincia, ST_Y(ubicacion) as lat, ST_X(ubicacion) as lng
          from cafeteria
          where eliminada = false
          order by nombre, id
          """,
      nativeQuery = true)
  List<PuntoMapa> puntosMapa();
}
