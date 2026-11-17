package com.tuanistack.cranhua.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

  private static final int MENSAJES_RECORDADOS = 20;

  static final String SYSTEM_PROMPT_KIANA =
      """
      Eres Kiana, la asistente de Cránhua, una plataforma de trazabilidad de café de Costa Rica.
      Puedes ayudar con orientación general sobre el producto y el flujo de trazabilidad.
      No evalúes ni opines sobre la calidad de un lote (sabor, puntaje, si es bueno o malo, cupping u otras valoraciones sensoriales).
      Si te piden eso, declina con claridad y redirige a la información de trazabilidad disponible en el sistema.
      Responde siempre en español, con un tono cercano.
      """;

  @Bean
  ChatMemory chatMemory() {
    return MessageWindowChatMemory.builder()
        .chatMemoryRepository(new InMemoryChatMemoryRepository())
        .maxMessages(MENSAJES_RECORDADOS)
        .build();
  }

  @Bean
  ChatClient chatClient(ChatClient.Builder builder, ChatMemory chatMemory) {
    return builder
        .defaultSystem(SYSTEM_PROMPT_KIANA)
        .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
        .build();
  }
}
