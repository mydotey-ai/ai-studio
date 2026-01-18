package com.mydotey.ai.studio.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mydotey.ai.studio.common.exception.BusinessException;
import com.mydotey.ai.studio.entity.McpServer;
import com.mydotey.ai.studio.entity.McpTool;
import com.mydotey.ai.studio.mapper.McpServerMapper;
import com.mydotey.ai.studio.mapper.McpToolMapper;
import com.mydotey.ai.studio.service.mcp.McpRpcClient;
import com.mydotey.ai.studio.service.mcp.McpRpcClient.ToolDefinition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class McpToolSyncService {

    private final McpServerMapper mcpServerMapper;
    private final McpToolMapper mcpToolMapper;
    private final McpRpcClient mcpRpcClient;

    /**
     * 从 MCP 服务器同步工具列表
     */
    @Transactional
    public void syncToolsFromServer(Long serverId) throws Exception {
        log.info("Syncing tools from MCP server: {}", serverId);

        // 获取服务器配置
        McpServer server = mcpServerMapper.selectById(serverId);
        if (server == null) {
            throw new BusinessException("MCP server not found");
        }

        // 调用 MCP RPC 获取工具列表
        List<ToolDefinition> tools = mcpRpcClient.listTools(server);

        log.info("Found {} tools on server: {}", tools.size(), server.getName());

        // 同步到数据库
        for (ToolDefinition toolDef : tools) {
            upsertTool(serverId, toolDef);
        }

        log.info("Tool sync completed for server: {}", serverId);
    }

    private void upsertTool(Long serverId, ToolDefinition toolDef) {
        // 检查工具是否已存在
        LambdaQueryWrapper<McpTool> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(McpTool::getServerId, serverId);
        queryWrapper.eq(McpTool::getToolName, toolDef.name);

        McpTool existingTool = mcpToolMapper.selectOne(queryWrapper);

        if (existingTool != null) {
            // 更新现有工具
            existingTool.setDescription(toolDef.description);
            existingTool.setInputSchema(toolDef.inputSchema);
            existingTool.setUpdatedAt(Instant.now());
            mcpToolMapper.updateById(existingTool);
            log.debug("Updated tool: {}", toolDef.name);
        } else {
            // 创建新工具
            McpTool newTool = new McpTool();
            newTool.setServerId(serverId);
            newTool.setToolName(toolDef.name);
            newTool.setDescription(toolDef.description);
            newTool.setInputSchema(toolDef.inputSchema);
            newTool.setCreatedAt(Instant.now());
            newTool.setUpdatedAt(Instant.now());
            mcpToolMapper.insert(newTool);
            log.debug("Created tool: {}", toolDef.name);
        }
    }
}
