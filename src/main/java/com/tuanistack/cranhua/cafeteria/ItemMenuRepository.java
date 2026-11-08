package com.tuanistack.cranhua.cafeteria;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemMenuRepository extends JpaRepository<ItemMenu, UUID> {

  // Los ítems de una cafetería eliminada se conservan pero ya no se muestran
  List<ItemMenu> findByCafeteriaIdAndCafeteriaEliminadaFalseOrderByNombre(UUID cafeteriaId);
}
