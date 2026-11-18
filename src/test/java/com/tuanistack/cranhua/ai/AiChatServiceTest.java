package com.tuanistack.cranhua.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.server.ResponseStatusException;

class AiChatServiceTest {

  private final ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
  private final AiChatService service = new AiChatService(chatClient);

  private ChatClient.CallResponseSpec llamada() {
    return chatClient.prompt().user("Hola").advisors(any(Consumer.class)).call();
  }

  private void modeloResponde(String respuesta) {
    when(llamada().content()).thenReturn(respuesta);
  }

  @Test
  void devuelveLaRespuestaDelModelo() {
    modeloResponde("¡Hola!");

    assertThat(service.chat("c1", "Hola")).isEqualTo("¡Hola!");
  }

  @Test
  void siElModeloFallaDa503() {
    when(llamada()).thenThrow(new RuntimeException("timeout"));

    assertThatThrownBy(() -> service.chat("c1", "Hola"))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("503")
        .hasMessageContaining(AiChatService.NO_DISPONIBLE);
  }

  @Test
  void siElModeloNoDevuelveTextoDa503() {
    modeloResponde(null);

    assertThatThrownBy(() -> service.chat("c1", "Hola"))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("503")
        .hasMessageContaining(AiChatService.NO_DISPONIBLE);
  }

  @Test
  void siElModeloDevuelveTextoEnBlancoDa503() {
    modeloResponde("   ");

    assertThatThrownBy(() -> service.chat("c1", "Hola"))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("503")
        .hasMessageContaining(AiChatService.NO_DISPONIBLE);
  }
}
