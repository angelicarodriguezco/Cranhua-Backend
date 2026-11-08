package com.tuanistack.cranhua.tueste;

import com.tuanistack.cranhua.cuenta.Cuenta;
import com.tuanistack.cranhua.cuenta.CuentaRepository;
import com.tuanistack.cranhua.tueste.dto.ActualizarNegocioRequest;
import com.tuanistack.cranhua.tueste.dto.NegocioTuesteResponse;
import com.tuanistack.cranhua.tueste.dto.RegistrarNegocioRequest;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NegocioTuesteService {

  private static final String YA_TIENE_NEGOCIO = "Tu cuenta ya tiene un negocio registrado.";

  private final NegocioTuesteRepository negocioRepository;
  private final CuentaRepository cuentaRepository;

  public NegocioTuesteService(
      NegocioTuesteRepository negocioRepository, CuentaRepository cuentaRepository) {
    this.negocioRepository = negocioRepository;
    this.cuentaRepository = cuentaRepository;
  }

  @Transactional
  public NegocioTuesteResponse registrar(RegistrarNegocioRequest request, UUID cuentaId) {
    Cuenta cuenta =
        cuentaRepository
            .findById(cuentaId)
            .filter(c -> !c.isEliminada())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    if (negocioRepository.existsByCuentaId(cuentaId)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, YA_TIENE_NEGOCIO);
    }

    NegocioTueste negocio =
        new NegocioTueste(
            cuenta,
            request.nombre(),
            request.nombreContacto(),
            request.telefono(),
            request.correo(),
            request.canton(),
            request.provincia(),
            request.estilos());
    negocio.setDescripcion(enBlancoANull(request.descripcion()));
    negocio.setPortadaUrl(enBlancoANull(request.portadaUrl()));
    // Dos envíos simultáneos pasan el exists; la restricción única decide
    try {
      negocio = negocioRepository.saveAndFlush(negocio);
    } catch (DataIntegrityViolationException e) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, YA_TIENE_NEGOCIO);
    }
    return NegocioTuesteResponse.de(negocio);
  }

  @Transactional(readOnly = true)
  public NegocioTuesteResponse obtener(UUID id) {
    return NegocioTuesteResponse.de(negocioExistente(id));
  }

  @Transactional(readOnly = true)
  public NegocioTuesteResponse actual(UUID cuentaId) {
    return negocioRepository
        .findByCuentaIdAndEliminadoFalse(cuentaId)
        .map(NegocioTuesteResponse::de)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Tu cuenta todavía no tiene un negocio registrado."));
  }

  @Transactional
  public NegocioTuesteResponse actualizar(
      UUID id, ActualizarNegocioRequest request, UUID cuentaId) {
    NegocioTueste negocio = negocioExistente(id);
    if (!negocio.getCuenta().getId().equals(cuentaId)) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Solo el dueño del negocio puede editarlo.");
    }

    Optional.ofNullable(request.nombre()).ifPresent(negocio::setNombre);
    Optional.ofNullable(request.nombreContacto()).ifPresent(negocio::setNombreContacto);
    Optional.ofNullable(request.telefono()).ifPresent(negocio::setTelefono);
    Optional.ofNullable(request.correo()).ifPresent(negocio::setCorreo);
    Optional.ofNullable(request.canton()).ifPresent(negocio::setCanton);
    Optional.ofNullable(request.provincia()).ifPresent(negocio::setProvincia);
    Optional.ofNullable(request.estilos()).ifPresent(negocio::setEstilos);
    Optional.ofNullable(request.descripcion())
        .ifPresent(texto -> negocio.setDescripcion(enBlancoANull(texto)));
    Optional.ofNullable(request.portadaUrl())
        .ifPresent(url -> negocio.setPortadaUrl(enBlancoANull(url)));

    return NegocioTuesteResponse.de(negocioRepository.saveAndFlush(negocio));
  }

  private NegocioTueste negocioExistente(UUID id) {
    return negocioRepository
        .findByIdAndEliminadoFalse(id)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No encontramos ese negocio."));
  }

  private static String enBlancoANull(String valor) {
    return valor == null || valor.isBlank() ? null : valor.strip();
  }
}
