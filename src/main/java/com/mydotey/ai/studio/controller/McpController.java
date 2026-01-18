package com.mydotey.ai.studio.controller;

import com.mydotey.ai.studio.annotation.AuditLog;
import com.mydotey.ai.studio.common.ApiResponse;
import com.mydotey.ai.studio.dto.CreateMcpServerRequest;
import com.mydotey.ai.studio.dto.McpServerResponse;
import com.mydotey.ai.studio.dto.UpdateMcpServerRequest;
import com.mydotey.ai.studio.entity.McpServer;
import com.mydotey.ai.studio.service.McpServerService;
import com.mydotey.ai.studio.service.McpToolSyncService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/mcp")
@RequiredArgsConstructor
public class McpController {

    private final McpServerService mcpServerService;
    private final McpToolSyncService mcpToolSyncService;

    @PostMapping("/servers")
    @AuditLog(action = "CREATE_MCP_SERVER", resourceType = "McpServer")
    public ApiResponse<McpServerResponse> createMcpServer(
            @Valid @RequestBody CreateMcpServerRequest request,
            @RequestAttribute("userId") Long userId) {

        McpServer server = mcpServerService.createMcpServer(request, userId);
        return ApiResponse.success(toResponse(server));
    }

    @PutMapping("/servers/{id}")
    @AuditLog(action = "UPDATE_MCP_SERVER", resourceType = "McpServer", resourceIdParam = "id")
    public ApiResponse<Void> updateMcpServer(
            @PathVariable Long id,
            @Valid @RequestBody UpdateMcpServerRequest request,
            @RequestAttribute("userId") Long userId) {

        mcpServerService.updateMcpServer(id, request, userId);
        return ApiResponse.success(null);
    }

    @GetMapping("/servers/{id}")
    public ApiResponse<McpServerResponse> getMcpServer(@PathVariable Long id) {
        McpServer server = mcpServerService.getMcpServer(id);
        return ApiResponse.success(toResponse(server));
    }

    @GetMapping("/servers")
    public ApiResponse<List<McpServerResponse>> listMcpServers() {
        List<McpServer> servers = mcpServerService.getAllMcpServers();
        List<McpServerResponse> responses = servers.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ApiResponse.success(responses);
    }

    @DeleteMapping("/servers/{id}")
    @AuditLog(action = "DELETE_MCP_SERVER", resourceType = "McpServer", resourceIdParam = "id")
    public ApiResponse<Void> deleteMcpServer(
            @PathVariable Long id,
            @RequestAttribute("userId") Long userId) {
        mcpServerService.deleteMcpServer(id, userId);
        return ApiResponse.success(null);
    }

    @PostMapping("/servers/{id}/sync-tools")
    @AuditLog(action = "SYNC_MCP_TOOLS", resourceType = "McpServer", resourceIdParam = "id")
    public ApiResponse<Void> syncTools(
            @PathVariable Long id,
            @RequestAttribute("userId") Long userId) throws Exception {
        mcpToolSyncService.syncToolsFromServer(id, userId);
        return ApiResponse.success(null);
    }

    private McpServerResponse toResponse(McpServer server) {
        return McpServerResponse.builder()
                .id(server.getId())
                .name(server.getName())
                .description(server.getDescription())
                .connectionType(server.getConnectionType())
                .command(server.getCommand())
                .workingDir(server.getWorkingDir())
                .endpointUrl(server.getEndpointUrl())
                .headers(server.getHeaders())
                .authType(server.getAuthType())
                .status(server.getStatus())
                .createdBy(server.getCreatedBy())
                .createdAt(server.getCreatedAt())
                .updatedAt(server.getUpdatedAt())
                .build();
    }
}
