package com.mydotey.ai.studio.controller;

import com.mydotey.ai.studio.annotation.AuditLog;
import com.mydotey.ai.studio.common.ApiResponse;
import com.mydotey.ai.studio.dto.*;
import com.mydotey.ai.studio.entity.Agent;
import com.mydotey.ai.studio.service.AgentExecutionService;
import com.mydotey.ai.studio.service.AgentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/agents")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;
    private final AgentExecutionService agentExecutionService;

    /**
     * 创建 Agent
     */
    @PostMapping
    @AuditLog(action = "CREATE_AGENT", resourceType = "Agent")
    public ApiResponse<AgentResponse> createAgent(
            @Valid @RequestBody CreateAgentRequest request,
            @RequestAttribute("userId") Long userId,
            @RequestAttribute("orgId") Long orgId) {

        Agent agent = agentService.createAgent(request, orgId, userId);

        AgentResponse response = AgentResponse.builder()
                .id(agent.getId())
                .name(agent.getName())
                .description(agent.getDescription())
                .systemPrompt(agent.getSystemPrompt())
                .isPublic(agent.getIsPublic())
                .modelConfig(agent.getModelConfig())
                .workflowType(com.mydotey.ai.studio.dto.WorkflowType.valueOf(agent.getWorkflowType()))
                .maxIterations(agent.getMaxIterations())
                .workflowConfig(agent.getWorkflowConfig())
                .knowledgeBaseIds(agentService.getAgentKnowledgeBaseIds(agent.getId()))
                .toolIds(agentService.getAgentToolIds(agent.getId()))
                .createdAt(agent.getCreatedAt())
                .updatedAt(agent.getUpdatedAt())
                .build();

        return ApiResponse.success(response);
    }

    /**
     * 获取 Agent 详情
     */
    @GetMapping("/{id}")
    public ApiResponse<AgentResponse> getAgent(@PathVariable Long id) {
        Agent agent = agentService.getAgent(id);

        AgentResponse response = AgentResponse.builder()
                .id(agent.getId())
                .name(agent.getName())
                .description(agent.getDescription())
                .systemPrompt(agent.getSystemPrompt())
                .isPublic(agent.getIsPublic())
                .modelConfig(agent.getModelConfig())
                .workflowType(com.mydotey.ai.studio.dto.WorkflowType.valueOf(agent.getWorkflowType()))
                .maxIterations(agent.getMaxIterations())
                .workflowConfig(agent.getWorkflowConfig())
                .knowledgeBaseIds(agentService.getAgentKnowledgeBaseIds(agent.getId()))
                .toolIds(agentService.getAgentToolIds(agent.getId()))
                .createdAt(agent.getCreatedAt())
                .updatedAt(agent.getUpdatedAt())
                .build();

        return ApiResponse.success(response);
    }

    /**
     * 更新 Agent
     */
    @PutMapping("/{id}")
    @AuditLog(action = "UPDATE_AGENT", resourceType = "Agent", resourceIdParam = "id")
    public ApiResponse<Void> updateAgent(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAgentRequest request,
            @RequestAttribute("userId") Long userId) {

        agentService.updateAgent(id, request, userId);
        return ApiResponse.success(null);
    }

    /**
     * 删除 Agent
     */
    @DeleteMapping("/{id}")
    @AuditLog(action = "DELETE_AGENT", resourceType = "Agent", resourceIdParam = "id")
    public ApiResponse<Void> deleteAgent(
            @PathVariable Long id,
            @RequestAttribute("userId") Long userId) {
        agentService.deleteAgent(id, userId);
        return ApiResponse.success(null);
    }

    /**
     * 执行 Agent
     */
    @PostMapping("/{id}/execute")
    @AuditLog(action = "EXECUTE_AGENT", resourceType = "Agent", resourceIdParam = "id")
    public ApiResponse<AgentExecutionResponse> executeAgent(
            @PathVariable Long id,
            @Valid @RequestBody AgentExecutionRequest request,
            @RequestAttribute("userId") Long userId) {

        log.info("Executing agent: {}, query: {}", id, request.getQuery());

        AgentExecutionResponse response = agentExecutionService.executeAgent(id, request, userId);

        return ApiResponse.success(response);
    }
}
