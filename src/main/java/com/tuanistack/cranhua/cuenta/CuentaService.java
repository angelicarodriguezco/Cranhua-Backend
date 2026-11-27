package com.tuanistack.cranhua.cuenta;

import com.tuanistack.cranhua.cafeteria.CafeteriaService;
import com.tuanistack.cranhua.cuenta.dto.ActualizarCuentaRequest;
import com.tuanistack.cranhua.cuenta.dto.CuentaResponse;
import com.tuanistack.cranhua.cuenta.dto.RedSocialRequest;
import com.tuanistack.cranhua.finca.FincaService;
import com.tuanistack.cranhua.lote.HistorialTrazabilidadService;
import com.tuanistack.cranhua.storage.AlmacenamientoImagenes;
import com.tuanistack.cranhua.tueste.NegocioTuesteService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CuentaService {

  private final CuentaRepository cuentaRepository;
  private final AlmacenamientoImagenes almacenamientoImagenes;
  private final HistorialTrazabilidadService historialTrazabilidadService;
  private final FincaService fincaService;
  private final NegocioTuesteService negocioTuesteService;
  private final CafeteriaService cafeteriaService;

  public CuentaService(
      CuentaRepository cuentaRepository,
      AlmacenamientoImagenes almacenamientoImagenes,
      HistorialTrazabilidadService historialTrazabilidadService,
      FincaService fincaService,
      NegocioTuesteService negocioTuesteService,
      CafeteriaService cafeteriaService) {
    this.cuentaRepository = cuentaRepository;
    this.almacenamientoImagenes = almacenamientoImagenes;
    this.historialTrazabilidadService = historialTrazabilidadService;
    this.fincaService = fincaService;
    this.negocioTuesteService = negocioTuesteService;
    this.cafeteriaService = cafeteriaService;
  }

  @Transactional(readOnly = true)
  public CuentaResponse obtener(UUID id) {
    return CuentaResponse.de(cargarActiva(id))
        .conHistorial(historialTrazabilidadService.deCuenta(id));
  }

  public CuentaResponse actualizar(UUID id, ActualizarCuentaRequest request) {
    Cuenta cuenta = cargarActiva(id);

    String nombreCompleto =
        request.nombreCompleto() != null
            ? request.nombreCompleto().strip()
            : cuenta.getNombreCompleto();
    String telefono =
        request.telefono() != null ? enBlancoANull(request.telefono()) : cuenta.getTelefono();
    String fotoUrl =
        request.fotoUrl() != null ? enBlancoANull(request.fotoUrl()) : cuenta.getFotoUrl();
    String bannerUrl =
        request.bannerUrl() != null ? enBlancoANull(request.bannerUrl()) : cuenta.getBannerUrl();
    List<RedSocial> redes =
        request.redes() != null ? aRedes(request.redes()) : List.copyOf(cuenta.getRedes());

    cuenta.actualizarPerfil(nombreCompleto, telefono, fotoUrl, bannerUrl, redes);
    return CuentaResponse.de(cuentaRepository.save(cuenta));
  }

  public CuentaResponse actualizarFoto(UUID id, MultipartFile archivo) {
    Cuenta cuenta = cargarActiva(id);
    cuenta.setFotoUrl(almacenamientoImagenes.subirFoto(archivo));
    return CuentaResponse.de(cuentaRepository.save(cuenta));
  }

  public CuentaResponse actualizarBanner(UUID id, MultipartFile archivo) {
    Cuenta cuenta = cargarActiva(id);
    cuenta.setBannerUrl(almacenamientoImagenes.subirBanner(archivo));
    return CuentaResponse.de(cuentaRepository.save(cuenta));
  }

  @Transactional
  public void eliminar(UUID id) {
    Cuenta cuenta = cargarActiva(id);
    fincaService.eliminarDeCuenta(id);
    negocioTuesteService.eliminarDeCuenta(id);
    cafeteriaService.eliminarDeCuenta(id);
    cuenta.eliminar();
    cuentaRepository.save(cuenta);
  }

  private Cuenta cargarActiva(UUID id) {
    return cuentaRepository
        .findById(id)
        .filter(cuenta -> !cuenta.isEliminada())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
  }

  private static List<RedSocial> aRedes(List<RedSocialRequest> redes) {
    return redes.stream().map(red -> new RedSocial(red.tipo(), red.valor().strip())).toList();
  }

  private static String enBlancoANull(String valor) {
    return valor.isBlank() ? null : valor.strip();
  }
}
