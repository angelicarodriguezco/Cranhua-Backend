package com.tuanistack.cranhua.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AiChatService {

  static final String NO_DISPONIBLE =
          "Kiana no está disponible en este momento. Intentá de nuevo en un rato.";

  private static final Logger log = LoggerFactory.getLogger(AiChatService.class);

  private final ChatClient chatClient;

  public AiChatService(ChatClient chatClient) {
    this.chatClient = chatClient;
  }

  public String chat(String conversationId, String message) {
    String respuesta;
    try {
      respuesta =
              chatClient
                      .prompt()
                      .user(message)
                      .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                      .call()
                      .content();
    } catch (RuntimeException e) {
      log.warn("El modelo de IA no respondió: {}", e.getMessage());
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, NO_DISPONIBLE, e);
    }
    if (respuesta == null || respuesta.isBlank()) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, NO_DISPONIBLE);
    }
    return respuesta;
  }
}