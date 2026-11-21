package com.tuanistack.cranhua.historial;

import com.tuanistack.cranhua.finca.Cosecha;
import com.tuanistack.cranhua.finca.CosechaRepository;
import com.tuanistack.cranhua.finca.FincaRepository;
import com.tuanistack.cranhua.historial.dto.CosechaHistorialResponse;
import com.tuanistack.cranhua.historial.dto.HistorialCafetaleroResponse;
import com.tuanistack.cranhua.historial.dto.HistorialCafetaleroResponse.Indicadores;
import com.tuanistack.cranhua.historial.dto.LoteVendidoResponse;
import com.tuanistack.cranhua.historial.dto.Pagina;
import com.tuanistack.cranhua.lote.LoteRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class HistorialCafetaleroService {

  private static final ZoneId COSTA_RICA = ZoneId.of("America/Costa_Rica");

  static final int TAMANO_MAXIMO = 50;

  private static final LocalDate PRIMERA_FECHA = LocalDate.of(1900, 1, 1);
  private static final LocalDate ULTIMA_FECHA = LocalDate.of(9999, 12, 30);

  private final FincaRepository fincaRepository;
  private final CosechaRepository cosechaRepository;
  private final LoteRepository loteRepository;

  public HistorialCafetaleroService(
      FincaRepository fincaRepository,
      CosechaRepository cosechaRepository,
      LoteRepository loteRepository) {
    this.fincaRepository = fincaRepository;
    this.cosechaRepository = cosechaRepository;
    this.loteRepository = loteRepository;
  }

  @Transactional(readOnly = true)
  public HistorialCafetaleroResponse historial(
      UUID cafetaleroId,
      UUID fincaId,
      LocalDate desde,
      LocalDate hasta,
      int paginaCosechas,
      int paginaLotes,
      int tamano) {
    validar(desde, hasta, paginaCosechas, paginaLotes, tamano);
    if (fincaId != null) {
      validarFinca(cafetaleroId, fincaId);
    }

    LocalDate inicio = desde == null ? PRIMERA_FECHA : desde;
    LocalDate fin = hasta == null ? ULTIMA_FECHA : hasta;

    Page<Cosecha> cosechas =
        cosechaRepository.historialDelDueno(
            cafetaleroId, fincaId, inicio, fin, PageRequest.of(paginaCosechas, tamano));
    BigDecimal produccion =
        cosechaRepository.produccionDelDueno(cafetaleroId, fincaId, inicio, fin);

    Instant vendidoDesde = inicio.atStartOfDay(COSTA_RICA).toInstant();
    Instant vendidoHasta = fin.plusDays(1).atStartOfDay(COSTA_RICA).toInstant();
    Page<LoteVendidoResponse> lotes =
        loteRepository.vendidosDelDueno(
            cafetaleroId, fincaId, vendidoDesde, vendidoHasta, PageRequest.of(paginaLotes, tamano));

    return new HistorialCafetaleroResponse(
        new Indicadores(cosechas.getTotalElements(), produccion, lotes.getTotalElements()),
        Pagina.de(cosechas, CosechaHistorialResponse::de),
        Pagina.de(lotes, Function.identity()));
  }

  private static void validar(
      LocalDate desde, LocalDate hasta, int paginaCosechas, int paginaLotes, int tamano) {
    if (desde != null && hasta != null && desde.isAfter(hasta)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "La fecha desde no puede ser posterior a la fecha hasta.");
    }
    if (paginaCosechas < 0 || paginaLotes < 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La página no puede ser negativa.");
    }
    if (tamano < 1 || tamano > TAMANO_MAXIMO) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "El tamaño de página tiene que estar entre 1 y 50.");
    }
  }

  private void validarFinca(UUID cafetaleroId, UUID fincaId) {
    boolean esSuya =
        fincaRepository
            .findByIdAndEliminadaFalse(fincaId)
            .map(finca -> finca.esDueno(cafetaleroId))
            .orElse(false);
    if (!esSuya) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No encontramos esa finca.");
    }
  }
}
