package com.tuanistack.cranhua.ai;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tuanistack.cranhua.config.JwtConfig;
import com.tuanistack.cranhua.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AiChatController.class)
@Import({SecurityConfig.class, JwtConfig.class})
@ActiveProfiles("test")
class AiChatControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private AiChatService aiChatService;

  @Test
  void chatReturnsReplyFromService() throws Exception {
    when(aiChatService.chat(eq("Hola"))).thenReturn("Hola, ¿en qué puedo ayudarte?");

    mockMvc
        .perform(
            post("/api/ai/chat")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CAFETERIA")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"Hola\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.reply").value("Hola, ¿en qué puedo ayudarte?"));
  }

  @Test
  void chatSinSesionDa401() throws Exception {
    mockMvc
        .perform(
            post("/api/ai/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"Hola\"}"))
        .andExpect(status().isUnauthorized());
  }
}
