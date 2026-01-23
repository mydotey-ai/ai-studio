package com.mydotey.ai.studio.dto.export;

import com.mydotey.ai.studio.entity.Agent;
import com.mydotey.ai.studio.entity.Chatbot;
import com.mydotey.ai.studio.entity.Document;
import com.mydotey.ai.studio.entity.KnowledgeBase;
import com.mydotey.ai.studio.entity.McpServer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 导出数据包
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportPackage {
    /**
     * 元数据
     */
    private ExportMetadata metadata;

    /**
     * 知识库数据
     */
    private List<KnowledgeBase> knowledgeBases;

    /**
     * 文档数据
     */
    private List<Document> documents;

    /**
     * Agent 数据
     */
    private List<Agent> agents;

    /**
     * 聊天机器人数据
     */
    private List<Chatbot> chatbots;

    /**
     * MCP 服务器数据
     */
    private List<McpServer> mcpServers;

    /**
     * 关联数据 (用于存储复杂关系)
     */
    private Map<String, Object> relations;
}
