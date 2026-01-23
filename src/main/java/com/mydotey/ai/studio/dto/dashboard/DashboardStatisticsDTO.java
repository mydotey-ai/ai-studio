package com.mydotey.ai.studio.dto.dashboard;

import lombok.Data;

@Data
public class DashboardStatisticsDTO {
    private KnowledgeBaseStats knowledgeBases;
    private AgentStats agents;
    private ChatbotStats chatbots;
    private DocumentStats documents;
    private UserStats users;
    private StorageStats storage;
}
