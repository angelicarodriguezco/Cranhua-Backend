package com.tuanistack.cranhua.canton;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CantonRepository extends JpaRepository<Canton, Short> {

  List<Canton> findAllByOrderByProvinciaAscNombreAsc();

  List<Canton> findByProvinciaIgnoreCaseOrderByNombreAsc(String provincia);
}
