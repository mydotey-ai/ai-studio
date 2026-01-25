package com.mydotey.ai.studio.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mydotey.ai.studio.annotation.AuditLog;
import com.mydotey.ai.studio.common.ApiResponse;
import com.mydotey.ai.studio.dto.*;
import com.mydotey.ai.studio.entity.Agent;
import com.mydotey.ai.studio.service.AgentExecutionService;
import com.mydotey.ai.studio.service.AgentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/agents")
@RequiredArgsConstructor
@Tag(name = "Agent", description = "AI Agent 管理和执行相关接口")
@SecurityRequirement(name = "bearerAuth")
public class AgentController {

    private final AgentService agentService;
    private final AgentExecutionService agentExecutionService;

    /**
     * 创建 Agent
     */
    @PostMapping
    @AuditLog(action = "CREATE_AGENT", resourceType = "Agent")
    @Operation(summary = "创建 Agent", description = "创建新的 AI Agent")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "创建成功")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数错误")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权")
    public ApiResponse<AgentResponse> createAgent(
            @Valid @RequestBody CreateAgentRequest request,
            @RequestAttribute("userId") Long userId,
            @RequestAttribute("orgId") Long orgId) {

        Agent agent = agentService.createAgent(request, orgId, userId);

        // 使用 AgentService 中的 toResponse 方法来确保包含关联的模型配置信息
        AgentResponse response = agentService.toResponse(agent);

        return ApiResponse.success(response);
    }

    /**
     * 获取 Agent 详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取 Agent 详情", description = "获取指定 Agent 的详细信息")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Agent 不存在")
    public ApiResponse<AgentResponse> getAgent(@PathVariable Long id) {
        Agent agent = agentService.getAgent(id);

        // 使用 AgentService 中的 toResponse 方法来确保包含关联的模型配置信息
        AgentResponse response = agentService.toResponse(agent);

        return ApiResponse.success(response);
    }

    /**
     * 更新 Agent
     */
    @PutMapping("/{id}")
    @AuditLog(action = "UPDATE_AGENT", resourceType = "Agent", resourceIdParam = "id")
    @Operation(summary = "更新 Agent", description = "更新 Agent 信息")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "更新成功")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数错误")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Agent 不存在")
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
    @Operation(summary = "删除 Agent", description = "删除指定的 Agent")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "删除成功")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Agent 不存在")
    public ApiResponse<Void> deleteAgent(
            @PathVariable Long id,
            @RequestAttribute("userId") Long userId) {
        agentService.deleteAgent(id, userId);
        return ApiResponse.success(null);
    }

    /**
     * 获取 Agent 列表（分页）
     */
    @GetMapping
    @Operation(summary = "获取 Agent 列表", description = "分页获取用户的 Agent 列表")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权")
    public ApiResponse<IPage<AgentResponse>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestAttribute("userId") Long userId) {
        IPage<AgentResponse> response = agentService.list(userId, page, size);
        return ApiResponse.success(response);
    }

    /**
     * 执行 Agent
     */
    @PostMapping("/{id}/execute")
    @AuditLog(action = "EXECUTE_AGENT", resourceType = "Agent", resourceIdParam = "id")
    @Operation(summary = "执行 Agent", description = "执行指定的 AI Agent")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "执行成功")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数错误")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Agent 不存在")
    public ApiResponse<AgentExecutionResponse> executeAgent(
            @PathVariable Long id,
            @Valid @RequestBody AgentExecutionRequest request,
            @RequestAttribute("userId") Long userId) {

        log.info("Executing agent: {}, query: {}", id, request.getQuery());

        AgentExecutionResponse response = agentExecutionService.executeAgent(id, request, userId);

        return ApiResponse.success(response);
    }
}
