package com.tuanistack.cranhua.ai;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tuanistack.cranhua.config.JwtConfig;
import com.tuanistack.cranhua.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.server.ResponseStatusException;

@WebMvcTest(AiChatController.class)
@Import({SecurityConfig.class, JwtConfig.class, LimitadorMensajes.class})
@ActiveProfiles("test")
class AiChatControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private AiChatService aiChatService;

  private static MockHttpServletRequestBuilder chat(String ip, String json) {
    return post("/api/ai/chat")
        .contentType(MediaType.APPLICATION_JSON)
        .content(json)
        .with(request -> {
          request.setRemoteAddr(ip);
          return request;
        });
  }

  @Test
  void respondeSinSesionYCreaConversacion() throws Exception {
    when(aiChatService.chat(anyString(), eq("Hola"))).thenReturn("Hola, ¿en qué te ayudo?");

    mockMvc
        .perform(chat("10.0.0.1", "{\"message\":\"Hola\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.reply").value("Hola, ¿en qué te ayudo?"))
        .andExpect(jsonPath("$.conversationId").isNotEmpty());
  }

  @Test
  void mantieneLaConversacionQueManda() throws Exception {
    when(aiChatService.chat("visita-1", "¿Y en Tarrazú?")).thenReturn("En Tarrazú hay tres.");

    mockMvc
        .perform(
            chat("10.0.0.2", "{\"message\":\"¿Y en Tarrazú?\",\"conversationId\":\"visita-1\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.conversationId").value("visita-1"))
        .andExpect(jsonPath("$.reply").value("En Tarrazú hay tres."));
  }

  @Test
  void mensajeVacioDa400() throws Exception {
    mockMvc
        .perform(chat("10.0.0.3", "{\"message\":\"   \"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errores.message").value("Escribí un mensaje."));
    verify(aiChatService, never()).chat(anyString(), anyString());
  }

  @Test
  void mensajeDeMasDe500CaracteresDa400() throws Exception {
    String largo = "a".repeat(501);

    mockMvc
        .perform(chat("10.0.0.4", "{\"message\":\"" + largo + "\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.errores.message").value("El mensaje no puede pasar de 500 caracteres."));
  }

  @Test
  void mensajeDe500CaracteresPasa() throws Exception {
    when(aiChatService.chat(anyString(), anyString())).thenReturn("Ok");

    mockMvc
        .perform(chat("10.0.0.5", "{\"message\":\"" + "a".repeat(500) + "\"}"))
        .andExpect(status().isOk());
  }

  @Test
  void servicioCaidoDa503ConMensaje() throws Exception {
    when(aiChatService.chat(anyString(), eq("Hola")))
        .thenThrow(
            new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE, AiChatService.NO_DISPONIBLE));

    mockMvc
        .perform(chat("10.0.0.6", "{\"message\":\"Hola\"}"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.mensaje").value(AiChatService.NO_DISPONIBLE));
  }

  @Test
  void elMensaje21EnUnMinutoDa429() throws Exception {
    when(aiChatService.chat(anyString(), anyString())).thenReturn("Ok");

    for (int i = 0; i < 20; i++) {
      mockMvc.perform(chat("10.0.0.7", "{\"message\":\"Hola\"}")).andExpect(status().isOk());
    }
    mockMvc
        .perform(chat("10.0.0.7", "{\"message\":\"Hola\"}"))
        .andExpect(status().isTooManyRequests())
        .andExpect(jsonPath("$.mensaje").value(AiChatController.DEMASIADOS_MENSAJES));

    // Otro visitante no se ve afectado
    mockMvc.perform(chat("10.0.0.8", "{\"message\":\"Hola\"}")).andExpect(status().isOk());
  }
}
