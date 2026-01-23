package com.mydotey.ai.studio.dto.dashboard;

import lombok.Data;

@Data
public class ChatbotStats {
    private Long totalCount;
    private Long publishedCount;
    private Long draftCount;
    private Long totalConversations;
}
