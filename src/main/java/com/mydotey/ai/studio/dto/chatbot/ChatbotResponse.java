package com.mydotey.ai.studio.dto.chatbot;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotResponse {
    private Long id;
    private Long agentId;
    private String name;
    private String description;
    private String welcomeMessage;
    private String avatarUrl;
    private Long ownerId;
    private String settings;
    private String styleConfig;
    private Boolean isPublished;
    private Long accessCount;
    private Instant createdAt;
    private Instant updatedAt;
}
