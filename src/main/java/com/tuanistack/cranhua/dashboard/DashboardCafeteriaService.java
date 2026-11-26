package com.tuanistack.cranhua.dashboard;

import com.tuanistack.cranhua.cafeteria.CafeteriaService;
import com.tuanistack.cranhua.dashboard.dto.DashboardCafeteriaResponse;
import com.tuanistack.cranhua.dashboard.dto.DashboardCafeteriaResponse.Indicadores;
import com.tuanistack.cranhua.pedido.EstadoPedido;
import com.tuanistack.cranhua.pedido.PedidoRepository;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardCafeteriaService {

  private static final int MAX_RECIENTES = 4;

  private final CafeteriaService cafeteriaService;
  private final PedidoRepository pedidoRepository;

  public DashboardCafeteriaService(
      CafeteriaService cafeteriaService, PedidoRepository pedidoRepository) {
    this.cafeteriaService = cafeteriaService;
    this.pedidoRepository = pedidoRepository;
  }

  @Transactional(readOnly = true)
  public DashboardCafeteriaResponse resumen(UUID cafeteriaId, UUID cuentaId) {
    // Responde 404 si no existe y 403 si es de otra cuenta
    cafeteriaService.obtener(cafeteriaId, cuentaId);

    Indicadores indicadores =
        new Indicadores(
            pedidoRepository.kgEntregados(cafeteriaId),
            pedidoRepository.countByCafeteriaIdAndEstado(cafeteriaId, EstadoPedido.EN_CAMINO),
            pedidoRepository.contarTostadoresActivos(cafeteriaId));

    return new DashboardCafeteriaResponse(
        indicadores, pedidoRepository.recientes(cafeteriaId, PageRequest.of(0, MAX_RECIENTES)));
  }
}
