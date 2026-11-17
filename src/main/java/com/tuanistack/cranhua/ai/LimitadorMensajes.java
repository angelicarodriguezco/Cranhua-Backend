package com.tuanistack.cranhua.ai;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LimitadorMensajes {

    private static final int LIMPIAR_DESDE = 10_000;

    private record Ventana(Instant inicio, int usados) {}

    private final int maximo;
    private final Duration duracion;
    private final Clock clock;
    private final ConcurrentHashMap<String, Ventana> ventanas = new ConcurrentHashMap<>();

    @Autowired
    public LimitadorMensajes(@Value("${app.ai.mensajes-por-minuto:20}") int maximo) {
        this(maximo, Duration.ofMinutes(1), Clock.systemUTC());
    }

    LimitadorMensajes(int maximo, Duration duracion, Clock clock) {
        this.maximo = maximo;
        this.duracion = duracion;
        this.clock = clock;
    }

    public boolean permitir(String visitante) {
        Instant ahora = clock.instant();
        if (ventanas.size() > LIMPIAR_DESDE) {
            ventanas.values().removeIf(v -> vencida(v, ahora));
        }
        Ventana ventana =
                ventanas.compute(
                        visitante,
                        (clave, actual) ->
                                actual == null || vencida(actual, ahora)
                                        ? new Ventana(ahora, 1)
                                        : new Ventana(actual.inicio(), actual.usados() + 1));
        return ventana.usados() <= maximo;
    }

    private boolean vencida(Ventana ventana, Instant ahora) {
        return !ahora.isBefore(ventana.inicio().plus(duracion));
    }
}