package com.tuanistack.cranhua.tueste;

import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class KgPedidosTueste {

  public BigDecimal total(UUID tuesteId) {
    return BigDecimal.ZERO;
  }
}
