package com.tuanistack.cranhua.publico;

import com.tuanistack.cranhua.cafeteria.CafeteriaRepository;
import com.tuanistack.cranhua.cuenta.Cuenta;
import com.tuanistack.cranhua.finca.Cosecha;
import com.tuanistack.cranhua.finca.Finca;
import com.tuanistack.cranhua.finca.GeometriaUtil;
import com.tuanistack.cranhua.finca.dto.CosechaDisponibleResponse;
import com.tuanistack.cranhua.lote.Lote;
import com.tuanistack.cranhua.lote.LoteRepository;
import com.tuanistack.cranhua.publico.dto.EnlacePerfil;
import com.tuanistack.cranhua.publico.dto.TrazabilidadPublicaResponse;
import com.tuanistack.cranhua.publico.dto.TrazabilidadPublicaResponse.CosechaOrigen;
import com.tuanistack.cranhua.publico.dto.TrazabilidadPublicaResponse.FincaOrigen;
import com.tuanistack.cranhua.publico.dto.TrazabilidadPublicaResponse.ProcesoLote;
import com.tuanistack.cranhua.publico.dto.TrazabilidadPublicaResponse.TostadorLote;
import com.tuanistack.cranhua.publico.dto.TrazabilidadPublicaResponse.TuesteLote;
import com.tuanistack.cranhua.tueste.NegocioTuesteRepository;
import com.tuanistack.cranhua.tueste.TuesteRepository;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TrazabilidadPublicaService {

  private static final String NO_ENCONTRADO =
      "No encontramos ese código de trazabilidad. Revisá que esté bien escrito.";

  private final LoteRepository loteRepository;
  private final TuesteRepository tuesteRepository;
  private final NegocioTuesteRepository negocioTuesteRepository;
  private final CafeteriaRepository cafeteriaRepository;

  public TrazabilidadPublicaService(
      LoteRepository loteRepository,
      TuesteRepository tuesteRepository,
      NegocioTuesteRepository negocioTuesteRepository,
      CafeteriaRepository cafeteriaRepository) {
    this.loteRepository = loteRepository;
    this.tuesteRepository = tuesteRepository;
    this.negocioTuesteRepository = negocioTuesteRepository;
    this.cafeteriaRepository = cafeteriaRepository;
  }

  @Transactional(readOnly = true)
  public TrazabilidadPublicaResponse porCodigo(String codigo) {
    Lote lote =
        loteRepository
            .findActivoParaTrazabilidad(normalizar(codigo))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, NO_ENCONTRADO));
    Cosecha cosecha = lote.getSolicitud().getCosecha();

    return new TrazabilidadPublicaResponse(
        lote.getCodigo(),
        lote.getActivadoEn(),
        finca(cosecha.getFinca()),
        cosecha(cosecha),
        tostador(lote.getSolicitud().getTostador()),
        tuesteRepository.findByLoteIdOrderByFechaTuesteDesc(lote.getId()).stream()
            .map(t -> new TuesteLote(t.getNivel(), t.getFechaTueste()))
            .toList(),
        cafeteriaRepository.vendenLote(lote.getCodigo()).stream()
            .map(c -> new EnlacePerfil(c.getId(), c.getNombre(), c.getCanton(), c.getProvincia()))
            .toList());
  }

  private static String normalizar(String codigo) {
    return codigo == null ? "" : codigo.strip().toUpperCase(Locale.ROOT);
  }

  private static FincaOrigen finca(Finca finca) {
    return new FincaOrigen(
        finca.getId(),
        finca.getNombre(),
        finca.getZonaCafetalera(),
        finca.getCanton(),
        finca.getProvincia(),
        GeometriaUtil.coordenada(finca.getUbicacion()),
        finca.getDuenos().stream().map(Cuenta::getNombreCompleto).sorted().toList(),
        !finca.isEliminada());
  }

  private static CosechaOrigen cosecha(Cosecha cosecha) {
    return new CosechaOrigen(
        cosecha.getVariedad(),
        cosecha.getAlturaMsnm(),
        cosecha.getFechaInicio(),
        cosecha.getFechaFin(),
        cosecha.getProcesos().stream()
            .map(p -> new ProcesoLote(p.getProceso(), p.getPorcentaje()))
            .toList(),
        CosechaDisponibleResponse.notas(cosecha.getNotasSabor()),
        cosecha.getPuntajeSca());
  }

  private TostadorLote tostador(Cuenta cuenta) {
    return negocioTuesteRepository
        .findByCuentaId(cuenta.getId())
        .map(n ->
            new TostadorLote(
                n.getId(), n.getNombre(), n.getCanton(), n.getProvincia(), !n.isEliminado()))
        .orElseGet(() -> new TostadorLote(null, cuenta.getNombreCompleto(), null, null, false));
  }
}
