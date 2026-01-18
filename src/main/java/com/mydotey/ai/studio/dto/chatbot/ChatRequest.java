package com.mydotey.ai.studio.dto.chatbot;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChatRequest {
    @NotNull(message = "Chatbot ID is required")
    private Long chatbotId;

    private Long conversationId;

    @NotBlank(message = "Message is required")
    private String message;

    private Boolean stream = false;
}
