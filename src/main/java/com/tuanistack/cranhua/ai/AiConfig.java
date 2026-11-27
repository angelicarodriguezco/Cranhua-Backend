package com.tuanistack.cranhua.ai;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;

@Configuration
public class AiConfig {

  private static final int MENSAJES_RECORDADOS = 20;
  private static final String RECURSO_INSTRUCCIONES = "ai/kiana-instrucciones.txt";

  static final String SYSTEM_PROMPT_KIANA = cargarInstrucciones();

  private static String cargarInstrucciones() {
    ClassPathResource resource = new ClassPathResource(RECURSO_INSTRUCCIONES);
    try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
      String prompt = FileCopyUtils.copyToString(reader).trim();
      if (prompt.isEmpty()) {
        throw new IllegalStateException("El recurso " + RECURSO_INSTRUCCIONES + " está vacío");
      }
      return prompt;
    } catch (IOException e) {
      throw new IllegalStateException("No se pudo cargar el recurso " + RECURSO_INSTRUCCIONES, e);
    }
  }

  @Bean
  ChatMemory chatMemory() {
    return MessageWindowChatMemory.builder()
        .chatMemoryRepository(new InMemoryChatMemoryRepository())
        .maxMessages(MENSAJES_RECORDADOS)
        .build();
  }

  @Bean
  ChatClient chatClient(
      ChatClient.Builder builder, ChatMemory chatMemory, KianaHerramientas kianaHerramientas) {
    return builder
        .defaultSystem(SYSTEM_PROMPT_KIANA)
        .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
        .defaultTools(kianaHerramientas)
        .build();
  }
}
