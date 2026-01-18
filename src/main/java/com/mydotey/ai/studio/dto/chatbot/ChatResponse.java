package com.mydotey.ai.studio.dto.chatbot;

import com.mydotey.ai.studio.dto.SourceDocument;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    private Long conversationId;
    private Long messageId;
    private String answer;
    private List<SourceDocument> sources;
    private Boolean isComplete = true;
}
