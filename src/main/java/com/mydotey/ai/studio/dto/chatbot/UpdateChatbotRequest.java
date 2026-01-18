package com.mydotey.ai.studio.dto.chatbot;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateChatbotRequest {
    @NotBlank(message = "Chatbot name is required")
    private String name;

    private String description;

    private String welcomeMessage;

    private String avatarUrl;

    private String settings;

    private String styleConfig;

    private Boolean isPublished;
}
