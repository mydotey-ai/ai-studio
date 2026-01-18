package com.mydotey.ai.studio.service;

import com.mydotey.ai.studio.common.exception.BusinessException;
import com.mydotey.ai.studio.dto.CreateMcpServerRequest;
import com.mydotey.ai.studio.dto.UpdateMcpServerRequest;
import com.mydotey.ai.studio.entity.McpServer;
import com.mydotey.ai.studio.mapper.McpServerMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class McpServerServiceTest {

    private McpServerMapper mcpServerMapper;
    private McpServerService mcpServerService;

    @BeforeEach
    void setUp() {
        mcpServerMapper = mock(McpServerMapper.class);
        mcpServerService = new McpServerService(mcpServerMapper);
    }

    @Test
    void testCreateMcpServer_Success() {
        // Given
        CreateMcpServerRequest request = new CreateMcpServerRequest();
        request.setName("Test Server");
        request.setDescription("Test Description");
        request.setConnectionType("STDIO");
        request.setCommand("npx -y @modelcontextprotocol/server-filesystem /tmp");

        when(mcpServerMapper.insert(any(McpServer.class))).thenReturn(1);

        // When
        mcpServerService.createMcpServer(request, 1L);

        // Then
        ArgumentCaptor<McpServer> captor = ArgumentCaptor.forClass(McpServer.class);
        verify(mcpServerMapper).insert(captor.capture());

        McpServer savedServer = captor.getValue();
        assertEquals("Test Server", savedServer.getName());
        assertEquals("Test Description", savedServer.getDescription());
        assertEquals("STDIO", savedServer.getConnectionType());
        assertEquals("npx -y @modelcontextprotocol/server-filesystem /tmp", savedServer.getCommand());
        assertEquals(1L, savedServer.getCreatedBy());
        assertEquals("ACTIVE", savedServer.getStatus());
        assertNotNull(savedServer.getCreatedAt(), "createdAt should be set");
        assertNotNull(savedServer.getUpdatedAt(), "updatedAt should be set");
    }

    @Test
    void testUpdateMcpServer_Success() {
        // Given
        Long serverId = 1L;
        McpServer existingServer = new McpServer();
        existingServer.setId(serverId);
        existingServer.setName("Old Name");

        UpdateMcpServerRequest request = new UpdateMcpServerRequest();
        request.setName("Updated Name");
        request.setDescription("Updated Description");

        when(mcpServerMapper.selectById(serverId)).thenReturn(existingServer);
        when(mcpServerMapper.updateById(any(McpServer.class))).thenReturn(1);

        // When
        mcpServerService.updateMcpServer(serverId, request, 1L);

        // Then
        ArgumentCaptor<McpServer> captor = ArgumentCaptor.forClass(McpServer.class);
        verify(mcpServerMapper).updateById(captor.capture());

        McpServer updatedServer = captor.getValue();
        assertEquals("Updated Name", updatedServer.getName());
        assertEquals("Updated Description", updatedServer.getDescription());
    }

    @Test
    void testGetMcpServer_Success() {
        // Given
        Long serverId = 1L;
        McpServer server = new McpServer();
        server.setId(serverId);
        server.setName("Test Server");
        server.setConnectionType("STDIO");
        server.setStatus("ACTIVE");

        when(mcpServerMapper.selectById(serverId)).thenReturn(server);

        // When
        McpServer result = mcpServerService.getMcpServer(serverId);

        // Then
        assertNotNull(result);
        assertEquals("Test Server", result.getName());
        assertEquals("STDIO", result.getConnectionType());
    }

    @Test
    void testDeleteMcpServer_Success() {
        // Given
        Long serverId = 1L;
        when(mcpServerMapper.deleteById(serverId)).thenReturn(1);

        // When
        mcpServerService.deleteMcpServer(serverId, 1L);

        // Then
        verify(mcpServerMapper).deleteById(serverId);
    }

    @Test
    void testCreateMcpServer_NameAlreadyExists() {
        // Given
        CreateMcpServerRequest request = new CreateMcpServerRequest();
        request.setName("Test Server");
        request.setConnectionType("STDIO");

        when(mcpServerMapper.selectCount(any())).thenReturn(1L);

        // When & Then
        assertThrows(BusinessException.class, () -> {
            mcpServerService.createMcpServer(request, 1L);
        });
    }

    @Test
    void testGetMcpServer_NotFound() {
        // Given
        when(mcpServerMapper.selectById(1L)).thenReturn(null);

        // When & Then
        assertThrows(BusinessException.class, () -> {
            mcpServerService.getMcpServer(1L);
        });
    }

    @Test
    void testGetAllMcpServers_Success() {
        // Given
        McpServer server1 = new McpServer();
        server1.setId(1L);
        McpServer server2 = new McpServer();
        server2.setId(2L);

        when(mcpServerMapper.selectList(any())).thenReturn(List.of(server1, server2));

        // When
        List<McpServer> result = mcpServerService.getAllMcpServers();

        // Then
        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals(2L, result.get(1).getId());
    }
}
