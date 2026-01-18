package com.mydotey.ai.studio.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mydotey.ai.studio.common.exception.BusinessException;
import com.mydotey.ai.studio.dto.CreateMcpServerRequest;
import com.mydotey.ai.studio.dto.UpdateMcpServerRequest;
import com.mydotey.ai.studio.entity.McpServer;
import com.mydotey.ai.studio.mapper.McpServerMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class McpServerService {

    private final McpServerMapper mcpServerMapper;

    @Transactional
    public McpServer createMcpServer(CreateMcpServerRequest request, Long userId) {
        log.info("Creating MCP server: {}, userId: {}", request.getName(), userId);

        LambdaQueryWrapper<McpServer> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(McpServer::getName, request.getName());
        if (mcpServerMapper.selectCount(queryWrapper) > 0) {
            throw new BusinessException("MCP server name already exists");
        }

        McpServer server = new McpServer();
        server.setName(request.getName());
        server.setDescription(request.getDescription());
        server.setConnectionType(request.getConnectionType());
        server.setCommand(request.getCommand());
        server.setWorkingDir(request.getWorkingDir());
        server.setEndpointUrl(request.getEndpointUrl());
        server.setHeaders(request.getHeaders());
        server.setAuthType(request.getAuthType());
        server.setAuthConfig(request.getAuthConfig());
        server.setStatus("ACTIVE");
        server.setCreatedBy(userId);
        server.setCreatedAt(Instant.now());
        server.setUpdatedAt(Instant.now());

        mcpServerMapper.insert(server);

        log.info("MCP server created: {}", server.getId());
        return server;
    }

    @Transactional
    public void updateMcpServer(Long serverId, UpdateMcpServerRequest request) {
        log.info("Updating MCP server: {}", serverId);

        McpServer server = getMcpServer(serverId);

        server.setName(request.getName());
        server.setDescription(request.getDescription());
        server.setConnectionType(request.getConnectionType());
        server.setCommand(request.getCommand());
        server.setWorkingDir(request.getWorkingDir());
        server.setEndpointUrl(request.getEndpointUrl());
        server.setHeaders(request.getHeaders());
        server.setAuthType(request.getAuthType());
        server.setAuthConfig(request.getAuthConfig());
        server.setUpdatedAt(Instant.now());

        mcpServerMapper.updateById(server);

        log.info("MCP server updated: {}", serverId);
    }

    public McpServer getMcpServer(Long serverId) {
        McpServer server = mcpServerMapper.selectById(serverId);
        if (server == null) {
            throw new BusinessException("MCP server not found");
        }
        return server;
    }

    @Transactional
    public void deleteMcpServer(Long serverId) {
        log.info("Deleting MCP server: {}", serverId);
        mcpServerMapper.deleteById(serverId);
        log.info("MCP server deleted: {}", serverId);
    }

    public List<McpServer> getAllMcpServers() {
        return mcpServerMapper.selectList(new LambdaQueryWrapper<>());
    }
}
