package com.mydotey.ai.studio.dto.chatbot;

import com.mydotey.ai.studio.dto.SourceDocument;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {
    private Long id;
    private Long conversationId;
    private String role;
    private String content;
    private List<SourceDocument> sources;
    private String toolCalls;
    private String metadata;
    private Instant createdAt;
}
