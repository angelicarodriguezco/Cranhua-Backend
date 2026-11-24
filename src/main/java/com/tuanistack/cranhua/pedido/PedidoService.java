package com.tuanistack.cranhua.pedido;

import com.tuanistack.cranhua.cafeteria.Cafeteria;
import com.tuanistack.cranhua.cafeteria.CafeteriaRepository;
import com.tuanistack.cranhua.cuenta.Cuenta;
import com.tuanistack.cranhua.pedido.dto.ActualizarPedidoRequest;
import com.tuanistack.cranhua.pedido.dto.CrearPedidoRequest;
import com.tuanistack.cranhua.pedido.dto.HistorialPedidosResponse;
import com.tuanistack.cranhua.pedido.dto.HistorialPedidosResponse.Indicadores;
import com.tuanistack.cranhua.pedido.dto.PedidoRecienteResponse;
import com.tuanistack.cranhua.tueste.NegocioTueste;
import com.tuanistack.cranhua.tueste.NegocioTuesteRepository;
import com.tuanistack.cranhua.tueste.Tueste;
import com.tuanistack.cranhua.tueste.TuesteRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PedidoService {

  private static final ZoneId COSTA_RICA = ZoneId.of("America/Costa_Rica");

  private final PedidoRepository pedidoRepository;
  private final CafeteriaRepository cafeteriaRepository;
  private final TuesteRepository tuesteRepository;
  private final NegocioTuesteRepository negocioTuesteRepository;

  public PedidoService(
      PedidoRepository pedidoRepository,
      CafeteriaRepository cafeteriaRepository,
      TuesteRepository tuesteRepository,
      NegocioTuesteRepository negocioTuesteRepository) {
    this.pedidoRepository = pedidoRepository;
    this.cafeteriaRepository = cafeteriaRepository;
    this.tuesteRepository = tuesteRepository;
    this.negocioTuesteRepository = negocioTuesteRepository;
  }

  @Transactional
  public PedidoRecienteResponse crear(CrearPedidoRequest request, UUID cuentaId) {
    Cafeteria cafeteria = cafeteriaDe(cuentaId);
    Tueste tueste =
        tuesteRepository
            .findById(request.tuesteId())
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No encontramos ese tueste."));

    BigDecimal disponibles =
        tuesteRepository.kgDisponibles(tueste.getId()).orElse(BigDecimal.ZERO);
    if (request.cantidadKg().compareTo(disponibles) > 0) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "No hay suficiente café disponible: pedís %s kg y quedan %s kg."
              .formatted(request.cantidadKg().toPlainString(), disponibles.toPlainString()));
    }

    try {
      Pedido pedido =
          pedidoRepository.saveAndFlush(new Pedido(cafeteria, tueste, request.cantidadKg()));
      return aResponse(pedido);
    } catch (IllegalStateException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
    }
  }

  @Transactional(readOnly = true)
  public HistorialPedidosResponse historial(UUID cuentaId) {
    Cafeteria cafeteria = cafeteriaDe(cuentaId);
    UUID cafeteriaId = cafeteria.getId();

    YearMonth mesActual = YearMonth.now(COSTA_RICA);
    Instant desde = mesActual.atDay(1).atStartOfDay(COSTA_RICA).toInstant();
    Instant hasta = mesActual.plusMonths(1).atDay(1).atStartOfDay(COSTA_RICA).toInstant();

    Indicadores indicadores =
        new Indicadores(
            pedidoRepository.countByCafeteriaIdAndEstado(cafeteriaId, EstadoPedido.EN_CAMINO),
            pedidoRepository.kgEntregadosEsteMes(cafeteriaId, desde, hasta),
            pedidoRepository.contarTostadoresActivos(cafeteriaId));

    return new HistorialPedidosResponse(indicadores, pedidoRepository.historial(cafeteriaId));
  }

  @Transactional
  public PedidoRecienteResponse actualizar(
      UUID pedidoId, ActualizarPedidoRequest request, UUID cuentaId) {
    if (request.estado() == EstadoPedido.EN_CAMINO) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Solo se puede marcar como entregado o cancelado.");
    }

    Pedido pedido =
        pedidoRepository
            .findById(pedidoId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No encontramos ese pedido."));

    if (!pedido.getCafeteria().getCuenta().getId().equals(cuentaId)) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Solo la cafetería dueña del pedido puede actualizarlo.");
    }

    try {
      if (request.estado() == EstadoPedido.ENTREGADO) {
        pedido.entregar();
      } else {
        pedido.cancelar();
      }
    } catch (IllegalStateException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    return aResponse(pedido);
  }

  private Cafeteria cafeteriaDe(UUID cuentaId) {
    return cafeteriaRepository
        .findByCuentaIdAndEliminadaFalse(cuentaId)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Tu cuenta todavía no tiene una cafetería registrada."));
  }

  private PedidoRecienteResponse aResponse(Pedido pedido) {
    Cuenta tostador = pedido.getTueste().getLote().getSolicitud().getTostador();
    String nombre =
        negocioTuesteRepository
            .findByCuentaIdAndEliminadoFalse(tostador.getId())
            .map(NegocioTueste::getNombre)
            .orElse(tostador.getNombreCompleto());
    return new PedidoRecienteResponse(
        pedido.getId(),
        nombre,
        pedido.getCantidadKg(),
        pedido.getPedidoEn(),
        pedido.getEstado());
  }
}
