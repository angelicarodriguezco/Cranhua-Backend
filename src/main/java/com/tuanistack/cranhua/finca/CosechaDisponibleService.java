package com.tuanistack.cranhua.finca;

import com.tuanistack.cranhua.finca.dto.CosechaDisponibleResponse;
import com.tuanistack.cranhua.finca.dto.DetalleCosechaDisponibleResponse;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CosechaDisponibleService {

  // Mismas notas que usa el filtro "Notas florales" del frontend
  private static final List<String> NOTAS_FLORALES =
      List.of("flor", "azahar", "hibisco", "jazmín", "jazmin", "lavanda", "rosa");

  private static final Set<Proceso> HONEY =
      Set.of(Proceso.HONEY, Proceso.RED_HONEY, Proceso.YELLOW_HONEY);

  private final CosechaRepository cosechaRepository;

  public CosechaDisponibleService(CosechaRepository cosechaRepository) {
    this.cosechaRepository = cosechaRepository;
  }

  @Transactional(readOnly = true)
  public List<CosechaDisponibleResponse> listar(
      UUID cuentaId, ZonaCafetalera zona, boolean florales, boolean honey) {
    return cosechaRepository.disponiblesParaOtros(cuentaId, EstadoCosecha.PUBLICADA).stream()
        .filter(c -> zona == null || zona == c.getFinca().getZonaCafetalera())
        .filter(c -> !florales || tieneNotasFlorales(c))
        .filter(c -> !honey || HONEY.contains(c.getProcesoPrincipal()))
        .map(CosechaDisponibleResponse::de)
        .toList();
  }

  @Transactional(readOnly = true)
  public DetalleCosechaDisponibleResponse obtener(UUID cosechaId, UUID cuentaId) {
    return cosechaRepository
        .findById(cosechaId)
        .filter(c -> estaDisponible(c) && !c.getFinca().esDueno(cuentaId))
        .map(DetalleCosechaDisponibleResponse::de)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No encontramos esa cosecha."));
  }

  private static boolean estaDisponible(Cosecha cosecha) {
    return cosecha.getEstado() == EstadoCosecha.PUBLICADA
        && cosecha.getKgDisponibles() != null
        && cosecha.getKgDisponibles().signum() > 0
        && !cosecha.isEliminada()
        && !cosecha.getFinca().isEliminada();
  }

  private static boolean tieneNotasFlorales(Cosecha cosecha) {
    if (cosecha.getNotasSabor() == null) {
      return false;
    }
    String notas = cosecha.getNotasSabor().toLowerCase(Locale.ROOT);
    return NOTAS_FLORALES.stream().anyMatch(notas::contains);
  }
}
