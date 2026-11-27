package com.tuanistack.cranhua.cafeteria;

import com.tuanistack.cranhua.mapa.PuntoMapa;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CafeteriaRepository extends JpaRepository<Cafeteria, UUID> {

  Optional<Cafeteria> findByIdAndEliminadaFalse(UUID id);

  Optional<Cafeteria> findByCuentaIdAndEliminadaFalse(UUID cuentaId);

  boolean existsByCuentaIdAndEliminadaFalse(UUID cuentaId);

  @Query(
      """
      select c from Cafeteria c
      where c.eliminada = false
        and (lower(c.provincia) = lower(:provincia) or :provincia is null)
        and (lower(c.canton) = lower(:canton) or :canton is null)
        and (lower(c.nombre) like lower(concat('%', :nombre, '%')) or :nombre is null)
      order by c.nombre, c.id
      """)
  List<Cafeteria> buscarActivas(
      @Param("provincia") String provincia,
      @Param("canton") String canton,
      @Param("nombre") String nombre,
      Pageable pageable);

  @Query(
      value =
          """
          select id, nombre, canton, provincia, ST_Y(ubicacion) as lat, ST_X(ubicacion) as lng
          from cafeteria
          where eliminada = false
          order by nombre, id
          """,
      nativeQuery = true)
  List<PuntoMapa> puntosMapa();

  @Query(
      """
      select distinct c from ItemMenu i
      join i.cafeteria c
      left join Lote l on l.codigo = i.codigoTrazabilidad
      left join l.solicitud s
      join Cosecha co on co = s.cosecha
        or (l.id is null and co.codigoLote = i.codigoTrazabilidad)
      where co.finca.id = :fincaId and c.eliminada = false
      order by c.nombre
      """)
  List<Cafeteria> vendenCafeDeFinca(@Param("fincaId") UUID fincaId);

  @Query(
      """
      select distinct c from ItemMenu i
      join i.cafeteria c
      where i.codigoTrazabilidad = :codigo and c.eliminada = false
      order by c.nombre
      """)
  List<Cafeteria> vendenLote(@Param("codigo") String codigo);
}
