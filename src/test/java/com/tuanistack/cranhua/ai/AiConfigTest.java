package com.tuanistack.cranhua.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;

class AiConfigTest {

  private final AiConfig config = new AiConfig();

  @Test
  void systemPromptSePresentaComoKiana() {
    assertThat(AiConfig.SYSTEM_PROMPT_KIANA)
        .containsIgnoringCase("Kiana")
        .containsIgnoringCase("Cránhua");
  }

  @Test
  void systemPromptDeclinaCalidadDeLote() {
    assertThat(AiConfig.SYSTEM_PROMPT_KIANA)
        .containsIgnoringCase("calidad")
        .containsIgnoringCase("lote")
        .containsIgnoringCase("declina");
  }

  @Test
  void systemPromptRespondeEnEspanol() {
    assertThat(AiConfig.SYSTEM_PROMPT_KIANA).containsIgnoringCase("español");
  }

  @Test
  void chatClientUsaElSystemPromptDeKiana() {
    ChatClient.Builder builder = mock(ChatClient.Builder.class);
    ChatMemory chatMemory = mock(ChatMemory.class);
    ChatClient chatClient = mock(ChatClient.class);

    when(builder.defaultSystem(any(String.class))).thenReturn(builder);
    when(builder.defaultAdvisors(any(MessageChatMemoryAdvisor.class))).thenReturn(builder);
    when(builder.build()).thenReturn(chatClient);

    assertThat(config.chatClient(builder, chatMemory)).isSameAs(chatClient);
    verify(builder).defaultSystem(eq(AiConfig.SYSTEM_PROMPT_KIANA));
  }
}
