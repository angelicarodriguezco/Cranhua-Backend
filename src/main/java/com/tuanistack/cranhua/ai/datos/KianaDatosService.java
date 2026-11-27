package com.tuanistack.cranhua.ai.datos;

import com.tuanistack.cranhua.ai.datos.dto.CafeteriaKianaDto;
import com.tuanistack.cranhua.ai.datos.dto.CosechaActivaKianaDto;
import com.tuanistack.cranhua.ai.datos.dto.FincaKianaDto;
import com.tuanistack.cranhua.ai.datos.dto.TostadorKianaDto;
import com.tuanistack.cranhua.ai.datos.dto.TuesteKianaDto;
import com.tuanistack.cranhua.cafeteria.Cafeteria;
import com.tuanistack.cranhua.cafeteria.CafeteriaRepository;
import com.tuanistack.cranhua.finca.Cosecha;
import com.tuanistack.cranhua.finca.CosechaRepository;
import com.tuanistack.cranhua.finca.Finca;
import com.tuanistack.cranhua.finca.FincaRepository;
import com.tuanistack.cranhua.finca.GeometriaUtil;
import com.tuanistack.cranhua.solicitud.Solicitud;
import com.tuanistack.cranhua.tueste.NegocioTueste;
import com.tuanistack.cranhua.tueste.NegocioTuesteRepository;
import com.tuanistack.cranhua.tueste.Tueste;
import com.tuanistack.cranhua.tueste.TuesteRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class KianaDatosService {

  static final int LIMITE_RESULTADOS = 20;

  private final CafeteriaRepository cafeteriaRepository;
  private final FincaRepository fincaRepository;
  private final CosechaRepository cosechaRepository;
  private final NegocioTuesteRepository negocioTuesteRepository;
  private final TuesteRepository tuesteRepository;

  public KianaDatosService(
      CafeteriaRepository cafeteriaRepository,
      FincaRepository fincaRepository,
      CosechaRepository cosechaRepository,
      NegocioTuesteRepository negocioTuesteRepository,
      TuesteRepository tuesteRepository) {
    this.cafeteriaRepository = cafeteriaRepository;
    this.fincaRepository = fincaRepository;
    this.cosechaRepository = cosechaRepository;
    this.negocioTuesteRepository = negocioTuesteRepository;
    this.tuesteRepository = tuesteRepository;
  }

  public List<CafeteriaKianaDto> buscarCafeterias(String provincia, String canton, String nombre) {
    return cafeteriaRepository
        .buscarActivas(filtro(provincia), filtro(canton), filtro(nombre), pageable())
        .stream()
        .map(KianaDatosService::aCafeteria)
        .toList();
  }

  public List<FincaKianaDto> buscarFincas(String provincia, String canton, String nombre) {
    return fincaRepository
        .buscarActivas(filtro(provincia), filtro(canton), filtro(nombre), pageable())
        .stream()
        .map(KianaDatosService::aFinca)
        .toList();
  }

  public List<CosechaActivaKianaDto> listarCosechasActivas(
      String provincia, String canton, String nombre) {
    return cosechaRepository
        .buscarActivas(filtro(provincia), filtro(canton), filtro(nombre), pageable())
        .stream()
        .map(KianaDatosService::aCosecha)
        .toList();
  }

  public List<TostadorKianaDto> buscarTostadores(String provincia, String canton, String nombre) {
    return negocioTuesteRepository
        .buscarActivos(filtro(provincia), filtro(canton), filtro(nombre), pageable())
        .stream()
        .map(KianaDatosService::aTostador)
        .toList();
  }

  public List<TuesteKianaDto> listarTuestesDisponibles(
      String provincia, String canton, String nombre) {
    return tuesteRepository
        .buscarDisponibles(filtro(provincia), filtro(canton), filtro(nombre), pageable())
        .stream()
        .map(this::aTueste)
        .toList();
  }

  private TuesteKianaDto aTueste(Tueste tueste) {
    Solicitud solicitud = tueste.getLote().getSolicitud();
    Cosecha cosecha = solicitud.getCosecha();
    String tostador =
        negocioTuesteRepository
            .findByCuentaIdAndEliminadoFalse(solicitud.getTostador().getId())
            .map(NegocioTueste::getNombre)
            .orElse(null);
    BigDecimal kgDisponibles =
        tuesteRepository.kgDisponibles(tueste.getId()).orElse(BigDecimal.ZERO);

    return new TuesteKianaDto(
        tueste.getNivel(),
        cosecha.getFinca().getNombre(),
        cosecha.getVariedad(),
        kgDisponibles,
        tostador);
  }

  private static CafeteriaKianaDto aCafeteria(Cafeteria cafeteria) {
    return new CafeteriaKianaDto(
        cafeteria.getNombre(),
        textoOpcional(cafeteria.getDescripcion()),
        textoOpcional(cafeteria.getHorario()),
        cafeteria.getCanton(),
        cafeteria.getProvincia(),
        GeometriaUtil.coordenada(cafeteria.getUbicacion()),
        cafeteria.getEstilos());
  }

  private static FincaKianaDto aFinca(Finca finca) {
    return new FincaKianaDto(
        finca.getNombre(),
        textoOpcional(finca.getDescripcion()),
        finca.getZonaCafetalera(),
        finca.getCanton(),
        finca.getProvincia(),
        GeometriaUtil.coordenada(finca.getUbicacion()));
  }

  private static CosechaActivaKianaDto aCosecha(Cosecha cosecha) {
    return new CosechaActivaKianaDto(
        cosecha.getVariedad(),
        cosecha.getProcesoPrincipal(),
        cosecha.getAlturaMsnm(),
        cosecha.getKgDisponibles(),
        cosecha.getCodigoLote(),
        cosecha.getFinca().getNombre());
  }

  private static TostadorKianaDto aTostador(NegocioTueste negocio) {
    return new TostadorKianaDto(
        negocio.getNombre(),
        textoOpcional(negocio.getDescripcion()),
        negocio.getCanton(),
        negocio.getProvincia(),
        negocio.getEstilos());
  }

  private static PageRequest pageable() {
    return PageRequest.of(0, LIMITE_RESULTADOS);
  }

  private static String filtro(String valor) {
    return valor == null || valor.isBlank() ? null : valor.strip();
  }

  private static String textoOpcional(String texto) {
    return texto == null || texto.isBlank() ? null : texto;
  }
}
