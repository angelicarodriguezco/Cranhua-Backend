package com.tuanistack.cranhua.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class LimitadorMensajesTest {

  private static final Instant INICIO = Instant.parse("2026-11-17T12:00:00Z");

  private static class RelojMovible extends Clock {
    private Instant ahora = INICIO;

    void avanzar(Duration duracion) {
      ahora = ahora.plus(duracion);
    }

    @Override
    public Instant instant() {
      return ahora;
    }

    @Override
    public ZoneId getZone() {
      return ZoneId.of("UTC");
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }
  }

  @Test
  void permiteHastaElMaximoYBloqueaElSiguiente() {
    LimitadorMensajes limitador =
        new LimitadorMensajes(3, Duration.ofMinutes(1), new RelojMovible());

    assertThat(limitador.permitir("a")).isTrue();
    assertThat(limitador.permitir("a")).isTrue();
    assertThat(limitador.permitir("a")).isTrue();
    assertThat(limitador.permitir("a")).isFalse();
    assertThat(limitador.permitir("b")).isTrue();
  }

  @Test
  void alPasarElMinutoVuelveAPermitir() {
    RelojMovible reloj = new RelojMovible();
    LimitadorMensajes limitador = new LimitadorMensajes(1, Duration.ofMinutes(1), reloj);

    assertThat(limitador.permitir("a")).isTrue();
    assertThat(limitador.permitir("a")).isFalse();
    reloj.avanzar(Duration.ofSeconds(60));
    assertThat(limitador.permitir("a")).isTrue();
  }
}
