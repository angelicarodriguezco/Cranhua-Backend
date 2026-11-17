package com.tuanistack.cranhua.canton;

import com.tuanistack.cranhua.canton.dto.CantonResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CantonService {

  private final CantonRepository cantonRepository;

  public CantonService(CantonRepository cantonRepository) {
    this.cantonRepository = cantonRepository;
  }

  @Transactional(readOnly = true)
  public List<CantonResponse> listar(String provincia) {
    List<Canton> cantones =
        provincia == null || provincia.isBlank()
            ? cantonRepository.findAllByOrderByProvinciaAscNombreAsc()
            : cantonRepository.findByProvinciaIgnoreCaseOrderByNombreAsc(provincia.strip());
    return cantones.stream().map(CantonResponse::de).toList();
  }
}
