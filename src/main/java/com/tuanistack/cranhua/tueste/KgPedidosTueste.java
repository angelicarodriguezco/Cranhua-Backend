package com.tuanistack.cranhua.tueste;

import com.tuanistack.cranhua.pedido.PedidoRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class KgPedidosTueste {

  private final PedidoRepository pedidoRepository;

  public KgPedidosTueste(PedidoRepository pedidoRepository) {
    this.pedidoRepository = pedidoRepository;
  }

  public BigDecimal total(UUID tuesteId) {
    return pedidoRepository.kgComprometidos(tuesteId);
  }
}
