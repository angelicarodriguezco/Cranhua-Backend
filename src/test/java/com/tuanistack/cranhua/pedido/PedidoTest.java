package com.tuanistack.cranhua.pedido;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tuanistack.cranhua.lote.EstadoTrazabilidad;
import com.tuanistack.cranhua.lote.Lote;
import com.tuanistack.cranhua.tueste.Tueste;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PedidoTest {

  private Tueste tueste(EstadoTrazabilidad estado) {
    Lote lote = mock(Lote.class);
    when(lote.getEstadoTrazabilidad()).thenReturn(estado);
    Tueste tueste = mock(Tueste.class);
    when(tueste.getLote()).thenReturn(lote);
    return tueste;
  }

  private Pedido pedido() {
    return new Pedido(null, tueste(EstadoTrazabilidad.ACTIVO), new BigDecimal("5.00"));
  }

  @Test
  void unPedidoNuevoEstaEnCaminoYSinCerrar() {
    Pedido pedido = pedido();

    assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.EN_CAMINO);
    assertThat(pedido.getCerradoEn()).isNull();
  }

  @Test
  void entregarCierraElPedido() {
    Pedido pedido = pedido();
    pedido.entregar();

    assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.ENTREGADO);
    assertThat(pedido.getCerradoEn()).isNotNull();
  }

  @Test
  void cancelarCierraElPedido() {
    Pedido pedido = pedido();
    pedido.cancelar();

    assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.CANCELADO);
    assertThat(pedido.getCerradoEn()).isNotNull();
  }

  @Test
  void unPedidoCerradoNoSePuedeVolverACerrar() {
    Pedido pedido = pedido();
    pedido.entregar();

    assertThatThrownBy(pedido::cancelar).isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(pedido::entregar).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void noSePuedePedirUnTuesteDeLoteSinCodigoActivo() {
    Tueste tueste = tueste(EstadoTrazabilidad.PENDIENTE);

    assertThatThrownBy(() -> new Pedido(null, tueste, new BigDecimal("5.00")))
        .isInstanceOf(IllegalStateException.class);
  }
}
