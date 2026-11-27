package com.tuanistack.cranhua.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;

class AiConfigTest {

  private static final String RECURSO_INSTRUCCIONES = "ai/kiana-instrucciones.txt";

  private final AiConfig config = new AiConfig();

  @Test
  void systemPromptSePresentaComoKiana() {
    assertThat(AiConfig.SYSTEM_PROMPT_KIANA)
        .containsIgnoringCase("Kiana")
        .containsIgnoringCase("Cránhua");
  }

  @Test
  void systemPromptDeclinaCalidadDeLoteCosechaYFinca() {
    assertThat(AiConfig.SYSTEM_PROMPT_KIANA)
        .containsIgnoringCase("calidad")
        .containsIgnoringCase("lote")
        .containsIgnoringCase("cosecha")
        .containsIgnoringCase("finca")
        .containsIgnoringCase("declina");
  }

  @Test
  void systemPromptRespondeEnEspanol() {
    assertThat(AiConfig.SYSTEM_PROMPT_KIANA).containsIgnoringCase("español");
  }

  @Test
  void systemPromptIndicaNoUsarVoseo() {
    assertThat(AiConfig.SYSTEM_PROMPT_KIANA)
        .containsIgnoringCase("No uses voseo")
        .containsIgnoringCase("tú");
  }

  @Test
  void systemPromptAdvierteQuePuedeCometerErrores() {
    assertThat(AiConfig.SYSTEM_PROMPT_KIANA).containsIgnoringCase("cometer errores");
  }

  @Test
  void systemPromptIndicaFueraDeAlcanceYQueSiPuedeResponder() {
    String prompt = AiConfig.SYSTEM_PROMPT_KIANA;
    assertThat(prompt).containsIgnoringCase("fuera de Cránhua");
    assertThat(prompt)
        .as("debe indicar qué sí puede responder dentro del alcance")
        .satisfiesAnyOf(
            p -> assertThat(p).containsIgnoringCase("qué sí puedes responder"),
            p -> assertThat(p).containsIgnoringCase("qué sí puede responder"));
  }

  @Test
  void systemPromptIncluyeGuiaDeUsoPorRol() {
    assertThat(AiConfig.SYSTEM_PROMPT_KIANA)
        .containsIgnoringCase("Registrar finca")
        .containsIgnoringCase("Registrar cosecha")
        .containsIgnoringCase("tueste")
        .containsIgnoringCase("cafetería")
        .containsIgnoringCase("Explorar")
        .containsIgnoringCase("código de trazabilidad");
  }

  @Test
  void systemPromptIndicaUsarHerramientasYNoInventar() {
    assertThat(AiConfig.SYSTEM_PROMPT_KIANA)
        .containsIgnoringCase("herramientas")
        .containsIgnoringCase("datos reales")
        .containsIgnoringCase("No inventes");
  }

  @Test
  void systemPromptProhibeContactosPreciosPedidosYSolicitudes() {
    assertThat(AiConfig.SYSTEM_PROMPT_KIANA)
        .containsIgnoringCase("contactos")
        .containsIgnoringCase("precios")
        .containsIgnoringCase("pedidos")
        .containsIgnoringCase("solicitudes");
  }

  @Test
  void systemPromptSeCargaDesdeElRecursoYNoEstaEnBlanco() throws Exception {
    ClassPathResource resource = new ClassPathResource(RECURSO_INSTRUCCIONES);
    assertThat(resource.exists()).isTrue();

    String desdeRecurso;
    try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
      desdeRecurso = FileCopyUtils.copyToString(reader).trim();
    }

    assertThat(desdeRecurso).isNotBlank();
    assertThat(AiConfig.SYSTEM_PROMPT_KIANA).isEqualTo(desdeRecurso).isNotBlank();
  }

  @Test
  void chatClientUsaElSystemPromptYLasHerramientasDeKiana() {
    ChatClient.Builder builder = mock(ChatClient.Builder.class);
    ChatMemory chatMemory = mock(ChatMemory.class);
    KianaHerramientas kianaHerramientas = mock(KianaHerramientas.class);
    ChatClient chatClient = mock(ChatClient.class);

    when(builder.defaultSystem(any(String.class))).thenReturn(builder);
    when(builder.defaultAdvisors(any(MessageChatMemoryAdvisor.class))).thenReturn(builder);
    when(builder.defaultTools(any())).thenReturn(builder);
    when(builder.build()).thenReturn(chatClient);

    assertThat(config.chatClient(builder, chatMemory, kianaHerramientas)).isSameAs(chatClient);
    verify(builder).defaultSystem(eq(AiConfig.SYSTEM_PROMPT_KIANA));
    verify(builder).defaultTools(kianaHerramientas);
  }
}
