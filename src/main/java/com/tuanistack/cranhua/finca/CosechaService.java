package com.tuanistack.cranhua.finca;

import com.tuanistack.cranhua.finca.dto.CosechaResponse;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CosechaService {

  private final CosechaRepository cosechaRepository;

  public CosechaService(CosechaRepository cosechaRepository) {
    this.cosechaRepository = cosechaRepository;
  }

  @Transactional(readOnly = true)
  public CosechaResponse obtener(UUID cosechaId, UUID cuentaId) {
    Cosecha cosecha =
        cosechaRepository
            .findById(cosechaId)
            .filter(c -> !c.getFinca().isEliminada())
            .orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "No encontramos esa cosecha."));
    if (!cosecha.getFinca().esDueno(cuentaId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Esta cosecha no es tuya.");
    }
    return CosechaResponse.conFinca(cosecha);
  }
}
