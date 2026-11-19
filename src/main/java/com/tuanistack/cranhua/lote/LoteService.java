package com.tuanistack.cranhua.lote;

import com.tuanistack.cranhua.lote.dto.FichaCodigoResponse;
import com.tuanistack.cranhua.lote.dto.LoteCompradoResponse;
import com.tuanistack.cranhua.lote.dto.LotesCompradosResponse;
import com.tuanistack.cranhua.lote.dto.LotesCompradosResponse.Indicadores;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LoteService {

  private final LoteRepository loteRepository;

  public LoteService(LoteRepository loteRepository) {
    this.loteRepository = loteRepository;
  }

  @Transactional(readOnly = true)
  public LotesCompradosResponse listar(UUID tostadorId) {
    List<LoteCompradoResponse> lotes =
        loteRepository.findDelTostador(tostadorId).stream().map(LoteCompradoResponse::de).toList();

    BigDecimal kgComprados =
        lotes.stream().map(LoteCompradoResponse::kg).reduce(BigDecimal.ZERO, BigDecimal::add);
    long conCodigo = lotes.stream().filter(l -> l.estado() == EstadoTrazabilidad.ACTIVO).count();

    return new LotesCompradosResponse(new Indicadores(lotes.size(), kgComprados, conCodigo), lotes);
  }

  @Transactional
  public FichaCodigoResponse activarCodigo(UUID loteId, UUID tostadorId) {
    Lote lote =
        loteRepository
            .findById(loteId)
            .orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "No encontramos ese lote."));
    if (!lote.getSolicitud().getTostador().getId().equals(tostadorId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Este lote no es tuyo.");
    }
    lote.activar();
    return FichaCodigoResponse.de(lote);
  }
}
