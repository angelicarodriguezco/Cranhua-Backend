package com.tuanistack.cranhua.tueste;

import com.tuanistack.cranhua.tueste.dto.DetalleTostadorExploracionResponse;
import com.tuanistack.cranhua.tueste.dto.TostadorExploracionResponse;
import com.tuanistack.cranhua.tueste.dto.TuesteDisponibleExploracionResponse;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TostadorExploracionService {

  private final NegocioTuesteRepository negocioRepository;
  private final TuesteRepository tuesteRepository;

  public TostadorExploracionService(
      NegocioTuesteRepository negocioRepository, TuesteRepository tuesteRepository) {
    this.negocioRepository = negocioRepository;
    this.tuesteRepository = tuesteRepository;
  }

  @Transactional(readOnly = true)
  public List<TostadorExploracionResponse> listar(NivelTueste estilo, boolean disponible) {
    Map<UUID, BigDecimal> kgPorNegocio = kgDisponiblesPorNegocio();
    return negocioRepository.activosParaExplorar().stream()
        .filter(n -> estilo == null || n.getEstilos().contains(estilo))
        .map(n ->
            TostadorExploracionResponse.de(
                n, kgPorNegocio.getOrDefault(n.getId(), BigDecimal.ZERO)))
        .filter(r -> !disponible || r.kgDisponibles().signum() > 0)
        .toList();
  }

  @Transactional(readOnly = true)
  public DetalleTostadorExploracionResponse obtener(UUID id, UUID cuentaId, boolean esCafeteria) {
    NegocioTueste negocio =
        negocioRepository
            .findByIdAndEliminadoFalse(id)
            .orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "No encontramos ese negocio."));

    if (!esCafeteria && !negocio.getCuenta().getId().equals(cuentaId)) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "No tienes permiso para ver este negocio.");
    }

    List<TuesteDisponibleExploracionResponse> tuestes = tuesteRepository.disponiblesDelNegocio(id);
    return DetalleTostadorExploracionResponse.de(negocio, tuestes);
  }

  private Map<UUID, BigDecimal> kgDisponiblesPorNegocio() {
    Map<UUID, BigDecimal> kgPorNegocio = new HashMap<>();
    for (Object[] fila : tuesteRepository.kgDisponiblesPorNegocio()) {
      kgPorNegocio.put((UUID) fila[0], (BigDecimal) fila[1]);
    }
    return kgPorNegocio;
  }
}
