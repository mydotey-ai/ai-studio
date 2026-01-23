package com.mydotey.ai.studio.dto.export;

import java.util.Set;

/**
 * 数据导出范围枚举
 */
public enum ExportScope {
    /**
     * 知识库数据
     */
    KNOWLEDGE_BASES(Set.of(
        "knowledge_bases",
        "kb_members",
        "documents",
        "document_chunks"
    )),

    /**
     * Agent 数据
     */
    AGENTS(Set.of(
        "agents",
        "agent_knowledge_bases",
        "agent_tools"
    )),

    /**
     * 聊天机器人数据
     */
    CHATBOTS(Set.of(
        "chatbots",
        "conversations",
        "messages"
    )),

    /**
     * MCP 服务器数据
     */
    MCP_SERVERS(Set.of(
        "mcp_servers",
        "mcp_tools"
    )),

    /**
     * 全部数据
     */
    ALL(Set.of(
        "knowledge_bases", "kb_members", "documents", "document_chunks",
        "agents", "agent_knowledge_bases", "agent_tools",
        "chatbots", "conversations", "messages",
        "mcp_servers", "mcp_tools"
    ));

    private final Set<String> tables;

    ExportScope(Set<String> tables) {
        this.tables = tables;
    }

    public Set<String> getTables() {
        return tables;
    }
}
