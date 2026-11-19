package com.tuanistack.cranhua.publico;

import com.tuanistack.cranhua.cafeteria.Cafeteria;
import com.tuanistack.cranhua.cafeteria.CafeteriaRepository;
import com.tuanistack.cranhua.cafeteria.CategoriaMenu;
import com.tuanistack.cranhua.cafeteria.ItemMenuRepository;
import com.tuanistack.cranhua.cuenta.Cuenta;
import com.tuanistack.cranhua.finca.Cosecha;
import com.tuanistack.cranhua.finca.CosechaRepository;
import com.tuanistack.cranhua.finca.EstadoCosecha;
import com.tuanistack.cranhua.finca.Finca;
import com.tuanistack.cranhua.finca.FincaRepository;
import com.tuanistack.cranhua.finca.GeometriaUtil;
import com.tuanistack.cranhua.finca.Proceso;
import com.tuanistack.cranhua.finca.ProcesoReparto;
import com.tuanistack.cranhua.publico.dto.EnlacePerfil;
import com.tuanistack.cranhua.publico.dto.PerfilCafeteriaPublicoResponse;
import com.tuanistack.cranhua.publico.dto.PerfilFincaPublicoResponse;
import com.tuanistack.cranhua.publico.dto.PerfilFincaPublicoResponse.CosechaActiva;
import com.tuanistack.cranhua.publico.dto.PerfilTostadorPublicoResponse;
import com.tuanistack.cranhua.tueste.NegocioTueste;
import com.tuanistack.cranhua.tueste.NegocioTuesteRepository;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PerfilPublicoService {

  private final FincaRepository fincaRepository;
  private final CosechaRepository cosechaRepository;
  private final CafeteriaRepository cafeteriaRepository;
  private final ItemMenuRepository itemMenuRepository;
  private final NegocioTuesteRepository negocioTuesteRepository;

  public PerfilPublicoService(
      FincaRepository fincaRepository,
      CosechaRepository cosechaRepository,
      CafeteriaRepository cafeteriaRepository,
      ItemMenuRepository itemMenuRepository,
      NegocioTuesteRepository negocioTuesteRepository) {
    this.fincaRepository = fincaRepository;
    this.cosechaRepository = cosechaRepository;
    this.cafeteriaRepository = cafeteriaRepository;
    this.itemMenuRepository = itemMenuRepository;
    this.negocioTuesteRepository = negocioTuesteRepository;
  }

  @Transactional(readOnly = true)
  public PerfilFincaPublicoResponse finca(UUID id) {
    Finca finca =
        fincaRepository
            .findByIdAndEliminadaFalse(id)
            .orElseThrow(() -> noEncontrado("No encontramos esa finca."));
    List<Cosecha> cosechas = cosechaRepository.findByFincaIdOrderByFechaInicioDescIdAsc(id);

    return new PerfilFincaPublicoResponse(
        finca.getId(),
        finca.getNombre(),
        textoOpcional(finca.getDescripcion()),
        finca.getZonaCafetalera(),
        finca.getCanton(),
        finca.getProvincia(),
        GeometriaUtil.coordenada(finca.getUbicacion()),
        GeometriaUtil.puntos(finca.getPerimetro()),
        finca.getDuenos().stream().map(Cuenta::getNombreCompleto).sorted().toList(),
        cosechas.stream().map(Cosecha::getVariedad).distinct().sorted().toList(),
        cosechas.stream()
            .flatMap(cosecha -> cosecha.getProcesos().stream())
            .map(ProcesoReparto::getProceso)
            .distinct()
            .sorted()
            .toList(),
        finca.getCreadaEn(),
        cosechas.stream()
            .filter(PerfilPublicoService::estaActiva)
            .map(PerfilPublicoService::cosechaActiva)
            .toList(),
        cafeteriaRepository.vendenCafeDeFinca(id).stream()
            .map(c -> new EnlacePerfil(c.getId(), c.getNombre(), c.getCanton(), c.getProvincia()))
            .toList());
  }

  @Transactional(readOnly = true)
  public PerfilCafeteriaPublicoResponse cafeteria(UUID id) {
    Cafeteria cafeteria =
        cafeteriaRepository
            .findByIdAndEliminadaFalse(id)
            .orElseThrow(() -> noEncontrado("No encontramos esa cafetería."));

    return new PerfilCafeteriaPublicoResponse(
        cafeteria.getId(),
        cafeteria.getNombre(),
        textoOpcional(cafeteria.getDescripcion()),
        textoOpcional(cafeteria.getHorario()),
        cafeteria.getCanton(),
        cafeteria.getProvincia(),
        GeometriaUtil.coordenada(cafeteria.getUbicacion()),
        cafeteria.getEstilos(),
        cafeteria.getPortadaUrl(),
        cafeteria.getCreadaEn(),
        itemMenuRepository.countByCafeteriaIdAndCategoriaAndCodigoTrazabilidadIsNotNull(
            id, CategoriaMenu.CAFE),
        fincaRepository.originanMenuDeCafeteria(id).stream()
            .map(f -> new EnlacePerfil(f.getId(), f.getNombre(), f.getCanton(), f.getProvincia()))
            .toList(),
        List.of());
  }

  @Transactional(readOnly = true)
  public PerfilTostadorPublicoResponse tostador(UUID id) {
    NegocioTueste negocio =
        negocioTuesteRepository
            .findByIdAndEliminadoFalse(id)
            .orElseThrow(() -> noEncontrado("No encontramos ese tostador."));

    return new PerfilTostadorPublicoResponse(
        negocio.getId(),
        negocio.getNombre(),
        textoOpcional(negocio.getDescripcion()),
        negocio.getCanton(),
        negocio.getProvincia(),
        negocio.getEstilos(),
        negocio.getPortadaUrl(),
        negocio.getCreadoEn(),
        // TODO: llenar cuando existan las compras y lotes de tueste
        List.of(),
        List.of());
  }

  private static boolean estaActiva(Cosecha cosecha) {
    return cosecha.getEstado() == EstadoCosecha.PUBLICADA
        && cosecha.getKgDisponibles() != null
        && cosecha.getKgDisponibles().signum() > 0;
  }

  private static CosechaActiva cosechaActiva(Cosecha cosecha) {
    Proceso principal =
        cosecha.getProcesos().stream()
            .max(Comparator.comparing(ProcesoReparto::getPorcentaje))
            .map(ProcesoReparto::getProceso)
            .orElse(null);
    return new CosechaActiva(
        cosecha.getId(),
        cosecha.getCodigoLote(),
        cosecha.getVariedad(),
        principal,
        cosecha.getAlturaMsnm(),
        cosecha.getKgDisponibles());
  }

  // Si el dueño no escribió nada (o solo espacios) el campo no se manda
  private static String textoOpcional(String texto) {
    return texto == null || texto.isBlank() ? null : texto;
  }

  private static ResponseStatusException noEncontrado(String mensaje) {
    return new ResponseStatusException(HttpStatus.NOT_FOUND, mensaje);
  }
}
