package com.tuanistack.cranhua.cafeteria;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemMenuRepository extends JpaRepository<ItemMenu, UUID> {

  List<ItemMenu> findByCafeteriaIdAndCafeteriaEliminadaFalseOrderByNombre(UUID cafeteriaId);

  long countByCafeteriaIdAndCategoriaAndCodigoTrazabilidadIsNotNull(
      UUID cafeteriaId, CategoriaMenu categoria);
}
