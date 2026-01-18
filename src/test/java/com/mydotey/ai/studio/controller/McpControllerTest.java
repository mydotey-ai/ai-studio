package com.mydotey.ai.studio.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mydotey.ai.studio.dto.CreateMcpServerRequest;
import com.mydotey.ai.studio.dto.McpServerResponse;
import com.mydotey.ai.studio.dto.UpdateMcpServerRequest;
import com.mydotey.ai.studio.entity.McpServer;
import com.mydotey.ai.studio.service.McpServerService;
import com.mydotey.ai.studio.service.McpToolSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(McpController.class)
@ContextConfiguration(classes = {McpController.class, com.mydotey.ai.studio.config.TestConfig.class})
public class McpControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private McpServerService mcpServerService;

    @MockBean
    private McpToolSyncService mcpToolSyncService;

    @Test
    void testCreateMcpServer_Success() throws Exception {
        // Given
        CreateMcpServerRequest request = new CreateMcpServerRequest();
        request.setName("Test Server");
        request.setConnectionType("STDIO");
        request.setCommand("npx test-server");

        McpServer server = new McpServer();
        server.setId(1L);
        server.setName("Test Server");

        when(mcpServerService.createMcpServer(any(), any())).thenReturn(server);

        // When & Then
        mockMvc.perform(post("/api/mcp/servers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("Test Server"));
    }

    @Test
    void testUpdateMcpServer_Success() throws Exception {
        // Given
        UpdateMcpServerRequest request = new UpdateMcpServerRequest();
        request.setName("Updated Server");
        request.setConnectionType("STDIO");
        request.setCommand("npx updated-server");

        // When & Then
        mockMvc.perform(put("/api/mcp/servers/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .requestAttr("userId", 1L))
                .andExpect(status().isOk());
    }
}
