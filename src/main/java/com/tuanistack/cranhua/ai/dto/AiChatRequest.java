package com.tuanistack.cranhua.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiChatRequest(
        @NotBlank(message = "Escribí un mensaje.")
        @Size(max = 500, message = "El mensaje no puede pasar de 500 caracteres.")
        String message,
        @Size(max = 64, message = "Identificador de conversación inválido.") String conversationId) {}