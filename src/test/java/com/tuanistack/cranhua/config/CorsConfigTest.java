package com.tuanistack.cranhua.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tuanistack.cranhua.ai.AiChatController;
import com.tuanistack.cranhua.ai.AiChatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AiChatController.class)
@Import({SecurityConfig.class, CorsConfig.class})
class CorsConfigTest {

  private static final String ANGULAR_ORIGIN = "http://localhost:4200";

  @Autowired private MockMvc mockMvc;

  @MockitoBean private AiChatService aiChatService;

  @Test
  void preflightFromAngularIsAllowed() throws Exception {
    mockMvc
        .perform(
            options("/api/ai/chat")
                .header(HttpHeaders.ORIGIN, ANGULAR_ORIGIN)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ANGULAR_ORIGIN));
  }

  @Test
  void postFromAngularIncludesCorsHeaders() throws Exception {
    mockMvc
        .perform(
            post("/api/ai/chat")
                .header(HttpHeaders.ORIGIN, ANGULAR_ORIGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"Hola\"}"))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ANGULAR_ORIGIN));
  }
}
