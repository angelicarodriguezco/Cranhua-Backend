package com.tuanistack.cranhua.finca;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.locationtech.jts.geom.Polygon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FincaRepository extends JpaRepository<Finca, UUID> {

  Optional<Finca> findByIdAndEliminadaFalse(UUID id);

  List<Finca> findByDuenosIdAndEliminadaFalse(UUID cuentaId);

  List<Finca> findByDuenosIdAndEliminadaFalseOrderByNombreAscIdAsc(UUID cuentaId);

  // Compartir un borde con la finca vecina se permite; solaparse no
  @Query(
      value =
          """
          select * from finca f
          where f.eliminada = false
            and ST_Intersects(f.perimetro, :perimetro)
            and not ST_Touches(f.perimetro, :perimetro)
          limit 1
          """,
      nativeQuery = true)
  Optional<Finca> buscarSuperpuesta(@Param("perimetro") Polygon perimetro);

  @Query(
      value =
          "select round(cast(ST_Area(perimetro::geography) / 10000 as numeric), 2)"
              + " from finca where id = :id",
      nativeQuery = true)
  BigDecimal areaHectareas(@Param("id") UUID id);

  // Cada fila es [id, área en hectáreas]
  @Query(
      value =
          "select id, round(cast(ST_Area(perimetro::geography) / 10000 as numeric), 2)"
              + " from finca where id in (:ids)",
      nativeQuery = true)
  List<Object[]> areasHectareas(@Param("ids") Collection<UUID> ids);
}
