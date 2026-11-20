package com.tuanistack.cranhua.tueste;

import com.tuanistack.cranhua.lote.Lote;
import com.tuanistack.cranhua.lote.LoteRepository;
import com.tuanistack.cranhua.tueste.dto.ActualizarTuesteRequest;
import com.tuanistack.cranhua.tueste.dto.TuesteResponse;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TuesteService {

  private final TuesteRepository tuesteRepository;
  private final LoteRepository loteRepository;
  private final KgPedidosTueste kgPedidosTueste;

  public TuesteService(
      TuesteRepository tuesteRepository,
      LoteRepository loteRepository,
      KgPedidosTueste kgPedidosTueste) {
    this.tuesteRepository = tuesteRepository;
    this.loteRepository = loteRepository;
    this.kgPedidosTueste = kgPedidosTueste;
  }

  @Transactional(readOnly = true)
  public List<TuesteResponse> listarPorLote(UUID loteId, UUID tostadorId) {
    Lote lote = loteDelTostador(loteId, tostadorId);
    return tuesteRepository.findByLoteIdOrderByFechaTuesteDesc(lote.getId()).stream()
        .map(TuesteResponse::de)
        .toList();
  }

  @Transactional
  public TuesteResponse actualizar(
      UUID tuesteId, ActualizarTuesteRequest request, UUID tostadorId) {
    Tueste tueste =
        tuesteRepository
            .findById(tuesteId)
            .orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "No encontramos ese tueste."));
    Lote lote = tueste.getLote();
    if (!lote.getSolicitud().getTostador().getId().equals(tostadorId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Este lote no es tuyo.");
    }

    BigDecimal pesoVerde =
        Optional.ofNullable(request.pesoVerdeKg()).orElse(tueste.getPesoVerdeKg());
    BigDecimal pesoTostado =
        Optional.ofNullable(request.pesoTostadoKg()).orElse(tueste.getPesoTostadoKg());

    validarPesos(tueste, pesoVerde, pesoTostado);

    BigDecimal kgPedidos = kgPedidosTueste.total(tueste.getId());
    if (pesoTostado.compareTo(kgPedidos) < 0) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "El peso tostado no puede ser menor a los kg ya pedidos por cafeterías.");
    }

    Optional.ofNullable(request.nivel()).ifPresent(tueste::setNivel);
    Optional.ofNullable(request.pesoVerdeKg()).ifPresent(tueste::setPesoVerdeKg);
    Optional.ofNullable(request.pesoTostadoKg()).ifPresent(tueste::setPesoTostadoKg);
    Optional.ofNullable(request.fechaTueste()).ifPresent(tueste::setFechaTueste);
    Optional.ofNullable(request.notas()).ifPresent(tueste::setNotas);

    return TuesteResponse.de(tuesteRepository.saveAndFlush(tueste));
  }

  private Lote loteDelTostador(UUID loteId, UUID tostadorId) {
    Lote lote =
        loteRepository
            .findById(loteId)
            .orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "No encontramos ese lote."));
    if (!lote.getSolicitud().getTostador().getId().equals(tostadorId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Este lote no es tuyo.");
    }
    return lote;
  }

  private void validarPesos(Tueste tueste, BigDecimal pesoVerde, BigDecimal pesoTostado) {
    Map<String, String> errores = new LinkedHashMap<>();

    if (pesoTostado.compareTo(pesoVerde) > 0) {
      errores.put("pesoTostadoKg", "El peso tostado no puede ser mayor al peso verde.");
    }

    BigDecimal pesoOtros =
        tuesteRepository.sumPesoVerdeKgByLoteIdExcluding(tueste.getLote().getId(), tueste.getId());
    BigDecimal kgPorTostar = tueste.getLote().getSolicitud().getCantidadKg().subtract(pesoOtros);
    if (pesoVerde.compareTo(kgPorTostar) > 0) {
      errores.put("pesoVerdeKg", "El peso verde supera los kg disponibles del lote.");
    }

    if (!errores.isEmpty()) {
      throw new TuesteInvalidoException(errores);
    }
  }
}
