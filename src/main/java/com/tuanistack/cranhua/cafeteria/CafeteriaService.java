package com.tuanistack.cranhua.cafeteria;

import com.tuanistack.cranhua.cafeteria.dto.ActualizarCafeteriaRequest;
import com.tuanistack.cranhua.cafeteria.dto.CafeteriaResponse;
import com.tuanistack.cranhua.cafeteria.dto.RegistrarCafeteriaRequest;
import com.tuanistack.cranhua.cuenta.Cuenta;
import com.tuanistack.cranhua.cuenta.CuentaRepository;
import com.tuanistack.cranhua.finca.GeometriaUtil;
import com.tuanistack.cranhua.pedido.PedidoRepository;
import com.tuanistack.cranhua.storage.AlmacenamientoImagenes;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CafeteriaService {

  private static final String YA_TIENE_CAFETERIA = "Tu cuenta ya tiene una cafetería registrada.";

  private final CafeteriaRepository cafeteriaRepository;
  private final CuentaRepository cuentaRepository;
  private final PedidoRepository pedidoRepository;
  private final AlmacenamientoImagenes almacenamientoImagenes;

  public CafeteriaService(
      CafeteriaRepository cafeteriaRepository,
      CuentaRepository cuentaRepository,
      PedidoRepository pedidoRepository,
      AlmacenamientoImagenes almacenamientoImagenes) {
    this.cafeteriaRepository = cafeteriaRepository;
    this.cuentaRepository = cuentaRepository;
    this.pedidoRepository = pedidoRepository;
    this.almacenamientoImagenes = almacenamientoImagenes;
  }

  @Transactional
  public CafeteriaResponse registrar(RegistrarCafeteriaRequest request, UUID cuentaId) {
    Cuenta cuenta =
        cuentaRepository
            .findById(cuentaId)
            .filter(c -> !c.isEliminada())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    if (cafeteriaRepository.existsByCuentaIdAndEliminadaFalse(cuentaId)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, YA_TIENE_CAFETERIA);
    }

    Cafeteria cafeteria =
        new Cafeteria(
            cuenta,
            request.nombre(),
            request.nombreContacto(),
            request.telefono(),
            request.correo(),
            GeometriaUtil.punto(request.ubicacion()),
            request.canton(),
            request.provincia(),
            request.estilos());
    cafeteria.setDescripcion(enBlancoANull(request.descripcion()));
    cafeteria.setHorario(enBlancoANull(request.horario()));
    cafeteria.setVolumenMensual(request.volumenMensual());
    cafeteria.setPortadaUrl(enBlancoANull(request.portadaUrl()));
    try {
      cafeteria = cafeteriaRepository.saveAndFlush(cafeteria);
    } catch (DataIntegrityViolationException e) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, YA_TIENE_CAFETERIA);
    }
    return CafeteriaResponse.de(cafeteria);
  }

  @Transactional(readOnly = true)
  public CafeteriaResponse obtener(UUID id, UUID cuentaId) {
    return CafeteriaResponse.de(cafeteriaPropia(id, cuentaId));
  }

  @Transactional(readOnly = true)
  public CafeteriaResponse actual(UUID cuentaId) {
    return cafeteriaRepository
        .findByCuentaIdAndEliminadaFalse(cuentaId)
        .map(CafeteriaResponse::de)
        .orElseThrow(() ->
            new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Tu cuenta todavía no tiene una cafetería registrada."));
  }

  @Transactional
  public CafeteriaResponse actualizar(UUID id, ActualizarCafeteriaRequest request, UUID cuentaId) {
    Cafeteria cafeteria = cafeteriaPropia(id, cuentaId);

    Optional.ofNullable(request.nombre()).ifPresent(cafeteria::setNombre);
    Optional.ofNullable(request.nombreContacto()).ifPresent(cafeteria::setNombreContacto);
    Optional.ofNullable(request.telefono()).ifPresent(cafeteria::setTelefono);
    Optional.ofNullable(request.correo()).ifPresent(cafeteria::setCorreo);
    Optional.ofNullable(request.ubicacion())
        .ifPresent(punto -> cafeteria.setUbicacion(GeometriaUtil.punto(punto)));
    Optional.ofNullable(request.canton()).ifPresent(cafeteria::setCanton);
    Optional.ofNullable(request.provincia()).ifPresent(cafeteria::setProvincia);
    Optional.ofNullable(request.estilos()).ifPresent(cafeteria::setEstilos);
    Optional.ofNullable(request.volumenMensual()).ifPresent(cafeteria::setVolumenMensual);
    Optional.ofNullable(request.descripcion())
        .ifPresent(texto -> cafeteria.setDescripcion(enBlancoANull(texto)));
    Optional.ofNullable(request.horario())
        .ifPresent(texto -> cafeteria.setHorario(enBlancoANull(texto)));
    Optional.ofNullable(request.portadaUrl())
        .ifPresent(url -> cafeteria.setPortadaUrl(enBlancoANull(url)));

    return CafeteriaResponse.de(cafeteriaRepository.saveAndFlush(cafeteria));
  }

  @Transactional
  public CafeteriaResponse actualizarPortada(UUID id, MultipartFile archivo, UUID cuentaId) {
    Cafeteria cafeteria = cafeteriaPropia(id, cuentaId);
    cafeteria.setPortadaUrl(almacenamientoImagenes.subirPortada(archivo));
    return CafeteriaResponse.de(cafeteriaRepository.saveAndFlush(cafeteria));
  }

  // La cuenta sigue activa; el menú se conserva pero deja de mostrarse
  @Transactional
  public void eliminar(UUID id, UUID cuentaId) {
    Cafeteria cafeteria = cafeteriaPropia(id, cuentaId);
    cafeteria.eliminar();
    pedidoRepository.cancelarEnCamino(cafeteria.getId(), Instant.now());
  }

  public Cafeteria cafeteriaPropia(UUID id, UUID cuentaId) {
    Cafeteria cafeteria =
        cafeteriaRepository
            .findByIdAndEliminadaFalse(id)
            .orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "No encontramos esa cafetería."));
    if (!cafeteria.getCuenta().getId().equals(cuentaId)) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN,
          "Solo el dueño de la cafetería puede verla, editarla o eliminarla.");
    }
    return cafeteria;
  }

  private static String enBlancoANull(String valor) {
    return valor == null || valor.isBlank() ? null : valor.strip();
  }
}
