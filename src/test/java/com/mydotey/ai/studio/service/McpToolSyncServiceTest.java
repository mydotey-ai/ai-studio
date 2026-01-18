package com.mydotey.ai.studio.service;

import com.mydotey.ai.studio.entity.McpServer;
import com.mydotey.ai.studio.entity.McpTool;
import com.mydotey.ai.studio.mapper.McpServerMapper;
import com.mydotey.ai.studio.mapper.McpToolMapper;
import com.mydotey.ai.studio.service.mcp.McpRpcClient;
import com.mydotey.ai.studio.service.mcp.McpRpcClient.ToolDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class McpToolSyncServiceTest {

    private McpServerMapper mcpServerMapper;
    private McpToolMapper mcpToolMapper;
    private McpRpcClient mcpRpcClient;
    private McpToolSyncService mcpToolSyncService;

    @BeforeEach
    void setUp() {
        mcpServerMapper = mock(McpServerMapper.class);
        mcpToolMapper = mock(McpToolMapper.class);
        mcpRpcClient = mock(McpRpcClient.class);
        mcpToolSyncService = new McpToolSyncService(mcpServerMapper, mcpToolMapper, mcpRpcClient);
    }

    @Test
    void testSyncToolsFromServer_Success() throws Exception {
        // Given
        Long serverId = 1L;
        McpServer server = new McpServer();
        server.setId(serverId);
        server.setName("Test Server");

        ToolDefinition toolDef1 = new ToolDefinition();
        toolDef1.name = "tool1";
        toolDef1.description = "Test Tool 1";
        toolDef1.inputSchema = "{\"type\":\"object\"}";

        List<ToolDefinition> tools = List.of(toolDef1);

        when(mcpServerMapper.selectById(serverId)).thenReturn(server);
        when(mcpRpcClient.listTools(server)).thenReturn(tools);
        when(mcpToolMapper.selectOne(any())).thenReturn(null);
        when(mcpToolMapper.insert(any(McpTool.class))).thenReturn(1);

        // When
        mcpToolSyncService.syncToolsFromServer(serverId, 1L);

        // Then
        verify(mcpToolMapper).insert(any(McpTool.class));
    }

    @Test
    void testSyncToolsFromServer_UpdateExistingTool() throws Exception {
        // Given
        Long serverId = 1L;
        McpServer server = new McpServer();
        server.setId(serverId);
        server.setName("Test Server");

        ToolDefinition toolDef1 = new ToolDefinition();
        toolDef1.name = "tool1";
        toolDef1.description = "Updated Description";
        toolDef1.inputSchema = "{\"type\":\"string\"}";

        List<ToolDefinition> tools = List.of(toolDef1);

        McpTool existingTool = new McpTool();
        existingTool.setId(100L);
        existingTool.setServerId(serverId);
        existingTool.setToolName("tool1");

        when(mcpServerMapper.selectById(serverId)).thenReturn(server);
        when(mcpRpcClient.listTools(server)).thenReturn(tools);
        when(mcpToolMapper.selectOne(any())).thenReturn(existingTool);
        when(mcpToolMapper.updateById(any(McpTool.class))).thenReturn(1);

        // When
        mcpToolSyncService.syncToolsFromServer(serverId, 1L);

        // Then
        verify(mcpToolMapper).updateById(any(McpTool.class));
        verify(mcpToolMapper, never()).insert(any(McpTool.class));
    }
}
