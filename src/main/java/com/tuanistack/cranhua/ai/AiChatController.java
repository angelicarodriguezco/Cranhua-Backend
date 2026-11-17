package com.tuanistack.cranhua.ai;

import com.tuanistack.cranhua.ai.dto.AiChatRequest;
import com.tuanistack.cranhua.ai.dto.AiChatResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/ai")
public class AiChatController {

  static final String DEMASIADOS_MENSAJES =
      "Enviaste muchos mensajes seguidos. Esperá un minuto e intentá de nuevo.";

  private final AiChatService aiChatService;
  private final LimitadorMensajes limitador;

  public AiChatController(AiChatService aiChatService, LimitadorMensajes limitador) {
    this.aiChatService = aiChatService;
    this.limitador = limitador;
  }

  @PostMapping("/chat")
  public AiChatResponse chat(
      @Valid @RequestBody AiChatRequest request, HttpServletRequest httpRequest) {
    if (!limitador.permitir(httpRequest.getRemoteAddr())) {
      throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, DEMASIADOS_MENSAJES);
    }
    String conversationId =
        request.conversationId() == null || request.conversationId().isBlank()
            ? UUID.randomUUID().toString()
            : request.conversationId();
    String respuesta = aiChatService.chat(conversationId, request.message());
    return new AiChatResponse(respuesta, conversationId);
  }
}
