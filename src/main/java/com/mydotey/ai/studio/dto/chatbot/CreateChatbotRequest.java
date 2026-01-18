package com.mydotey.ai.studio.dto.chatbot;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateChatbotRequest {
    @NotBlank(message = "Chatbot name is required")
    private String name;

    private String description;

    private String welcomeMessage = "你好，有什么可以帮助你的吗？";

    private String avatarUrl;

    @NotNull(message = "Agent ID is required")
    private Long agentId;

    private String settings = "{}";

    private String styleConfig = "{}";

    private Boolean isPublished = false;
}
