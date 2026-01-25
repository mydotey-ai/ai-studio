package com.mydotey.ai.studio.dto.chatbot;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationPageResponse {
    private List<ConversationResponse> records;
    private long total;
    private long current;
    private long size;
}
