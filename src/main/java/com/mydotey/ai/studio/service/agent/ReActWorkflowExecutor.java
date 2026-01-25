package com.mydotey.ai.studio.service.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mydotey.ai.studio.common.exception.BusinessException;
import com.mydotey.ai.studio.dto.*;
import com.mydotey.ai.studio.entity.Agent;
import com.mydotey.ai.studio.service.*;
import com.mydotey.ai.studio.service.mcp.McpRpcClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReActWorkflowExecutor implements WorkflowExecutor {

    private final RagService ragService;
    private final LlmGenerationService llmGenerationService;
    private final McpRpcClient mcpRpcClient;
    private final AgentService agentService;
    private final ModelConfigService modelConfigService;
    private final ObjectMapper objectMapper;

    @Override
    public AgentExecutionResponse execute(Agent agent, AgentExecutionRequest request, Long userId) {
        log.info("Executing ReAct workflow for agent: {}, query: {}", agent.getId(), request.getQuery());

        // Input validation
        if (agent == null || request == null) {
            throw new BusinessException("Agent and request are required");
        }
        if (request.getQuery() == null || request.getQuery().isBlank()) {
            throw new BusinessException("Query is required");
        }

        List<Long> kbIds = agentService.getAgentKnowledgeBaseIds(agent.getId());
        List<Long> toolIds = agentService.getAgentToolIds(agent.getId());

        List<AgentExecutionResponse.ThoughtStep> thoughtSteps = new ArrayList<>();
        List<AgentExecutionResponse.ToolCallResult> toolCallResults = new ArrayList<>();

        StringBuilder finalAnswer = new StringBuilder();
        boolean isComplete = false;
        int iteration = 0;

        // Null safety for maxIterations with default value of 10
        int maxIterations = agent.getMaxIterations() != null ? agent.getMaxIterations() : 10;

        // 获取 Agent 的模型配置
        ModelConfigDto agentModelConfig = null;
        log.info("Agent llmModelConfigId: {}", agent.getLlmModelConfigId());
        if (agent.getLlmModelConfigId() != null) {
            try {
                agentModelConfig = modelConfigService.getConfigById(agent.getLlmModelConfigId());
                log.info("Using agent model config: {}, model: {}, endpoint: {}",
                        agentModelConfig.getName(), agentModelConfig.getModel(), agentModelConfig.getEndpoint());
            } catch (Exception e) {
                log.warn("Failed to get agent model config, using default: {}", e.getMessage());
            }
        }

        while (!isComplete && iteration < maxIterations) {
            iteration++;
            log.info("ReAct iteration: {}", iteration);

            try {
                // 1. 构建上下文
                String context = buildContext(agent, request.getQuery(), thoughtSteps, kbIds);

                // 2. LLM 推理 - 使用 Agent 的模型配置或全局配置
                LlmResponse llmResponse;
                if (agentModelConfig != null) {
                    log.info("DEBUG: Using Agent model config method");
                    // 使用 Agent 的模型配置
                    llmResponse = llmGenerationService.generate(
                            context,
                            buildReActPrompt(toolIds),
                            agentModelConfig
                    );
                } else {
                    log.info("DEBUG: Using global config method");
                    // 使用全局配置
                    llmResponse = llmGenerationService.generate(
                            context,
                            buildReActPrompt(toolIds),
                            null,
                            1000
                    );
                }

                // 3. 解析 LLM 响应
                if (llmResponse.getContent() != null && !llmResponse.getContent().isBlank()) {
                    finalAnswer.append(llmResponse.getContent());
                    isComplete = llmResponse.getFinishReason() != null;

                    thoughtSteps.add(AgentExecutionResponse.ThoughtStep.builder()
                        .step(iteration)
                        .thought("Providing final answer")
                        .observation(llmResponse.getContent())
                        .build());
                } else {
                    // No content generated, break to avoid infinite loop
                    break;
                }

            } catch (Exception e) {
                log.error("Error in ReAct iteration: {}", iteration, e);
                thoughtSteps.add(AgentExecutionResponse.ThoughtStep.builder()
                        .step(iteration)
                        .thought("Error occurred")
                        .observation("Error: " + e.getMessage())
                        .build());
                break;
            }
        }

        return AgentExecutionResponse.builder()
            .answer(finalAnswer.toString())
            .thoughtSteps(thoughtSteps)
            .toolCalls(toolCallResults)
            .isComplete(isComplete)
            .build();
    }

    private String buildContext(Agent agent, String query,
                               List<AgentExecutionResponse.ThoughtStep> thoughtSteps,
                               List<Long> kbIds) {
        StringBuilder context = new StringBuilder();
        String systemPrompt = agent.getSystemPrompt();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            context.append(systemPrompt).append("\n\n");
        } else {
            context.append("You are a helpful assistant.\n\n");
        }
        context.append("Current question: ").append(query);
        return context.toString();
    }

    private String buildReActPrompt(List<Long> toolIds) {
        StringBuilder prompt = new StringBuilder("\n\n");

        // 如果有工具可用，添加工具列表到提示词
        if (toolIds != null && !toolIds.isEmpty()) {
            prompt.append("You have access to the following tools:\n");
            for (Long toolId : toolIds) {
                prompt.append("- Tool ID: ").append(toolId).append("\n");
            }
            prompt.append("\nUse these tools when needed.\n");
        }

        prompt.append("Please provide a helpful answer.");
        return prompt.toString();
    }
}
