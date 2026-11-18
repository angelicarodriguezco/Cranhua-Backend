package com.tuanistack.cranhua.mapa;

import com.tuanistack.cranhua.cafeteria.CafeteriaRepository;
import com.tuanistack.cranhua.finca.FincaRepository;
import com.tuanistack.cranhua.mapa.dto.PuntoMapaResponse;
import com.tuanistack.cranhua.tueste.NegocioTuesteRepository;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MapaService {

  private final FincaRepository fincaRepository;
  private final NegocioTuesteRepository negocioRepository;
  private final CafeteriaRepository cafeteriaRepository;

  public MapaService(
      FincaRepository fincaRepository,
      NegocioTuesteRepository negocioRepository,
      CafeteriaRepository cafeteriaRepository) {
    this.fincaRepository = fincaRepository;
    this.negocioRepository = negocioRepository;
    this.cafeteriaRepository = cafeteriaRepository;
  }

  @Transactional(readOnly = true)
  public List<PuntoMapaResponse> listar(TipoPuntoMapa tipo) {
    return Arrays.stream(TipoPuntoMapa.values())
        .filter(t -> tipo == null || t == tipo)
        .flatMap(t -> puntos(t).stream().map(punto -> PuntoMapaResponse.de(t, punto)))
        .toList();
  }

  private List<PuntoMapa> puntos(TipoPuntoMapa tipo) {
    return switch (tipo) {
      case FINCA -> fincaRepository.puntosMapa();
      case TOSTADOR -> negocioRepository.puntosMapa();
      case CAFETERIA -> cafeteriaRepository.puntosMapa();
    };
  }
}
