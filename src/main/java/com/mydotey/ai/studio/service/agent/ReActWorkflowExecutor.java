package com.mydotey.ai.studio.service.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mydotey.ai.studio.common.exception.BusinessException;
import com.mydotey.ai.studio.dto.*;
import com.mydotey.ai.studio.entity.Agent;
import com.mydotey.ai.studio.service.*;
import com.mydotey.ai.studio.service.StreamingChatCallback;
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
    private final StreamingLlmService streamingLlmService;
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
                // 1. 构建 ReAct 工作流的用户问题（包含查询和上下文）
                String userQuestion = buildReActUserPrompt(request.getQuery(), toolIds);

                // 2. 获取 Agent 的系统提示词
                String systemPrompt = getSystemPrompt(agent);

                // 3. LLM 推理 - 使用 Agent 的模型配置或全局配置
                LlmResponse llmResponse;
                if (agentModelConfig != null) {
                    log.info("DEBUG: Using Agent model config method");
                    // 使用 Agent 的模型配置
                    llmResponse = llmGenerationService.generate(
                            systemPrompt,
                            userQuestion,
                            agentModelConfig
                    );
                } else {
                    log.info("DEBUG: Using global config method");
                    // 使用全局配置
                    llmResponse = llmGenerationService.generate(
                            systemPrompt,
                            userQuestion,
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

    /**
     * 获取 Agent 的系统提示词
     */
    private String getSystemPrompt(Agent agent) {
        String systemPrompt = agent.getSystemPrompt();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            return systemPrompt;
        }
        return "You are a helpful assistant.";
    }

    /**
     * 构建 ReAct 工作流的用户提示词（包含查询和工具信息）
     */
    private String buildReActUserPrompt(String query, List<Long> toolIds) {
        StringBuilder prompt = new StringBuilder();

        // 添加当前问题
        prompt.append("Current question: ").append(query).append("\n\n");

        // 如果有工具可用，添加工具列表到提示词
        if (toolIds != null && !toolIds.isEmpty()) {
            prompt.append("You have access to following tools:\n");
            for (Long toolId : toolIds) {
                prompt.append("- Tool ID: ").append(toolId).append("\n");
            }
            prompt.append("\nUse these tools when needed.\n");
        }

        prompt.append("Please provide a helpful answer.");
        return prompt.toString();
    }

    /**
     * 流式执行 Agent
     */
    @Override
    public void executeStream(Agent agent, AgentExecutionRequest request, Long userId, StreamingChatCallback callback) {
        log.info("Executing ReAct workflow stream for agent: {}, query: {}", agent.getId(), request.getQuery());

        try {
            // 1. 获取 Agent 的系统提示词
            String systemPrompt = getSystemPrompt(agent);

            // 2. 构建用户问题
            List<Long> toolIds = agentService.getAgentToolIds(agent.getId());
            String userQuestion = buildReActUserPrompt(request.getQuery(), toolIds);

            // 3. 获取 Agent 的模型配置
            ModelConfigDto agentModelConfig = null;
            if (agent.getLlmModelConfigId() != null) {
                try {
                    agentModelConfig = modelConfigService.getConfigById(agent.getLlmModelConfigId());
                    log.info("Using agent model config: {}, model: {}, endpoint: {}",
                            agentModelConfig.getName(), agentModelConfig.getModel(), agentModelConfig.getEndpoint());
                } catch (Exception e) {
                    log.warn("Failed to get agent model config, using default: {}", e.getMessage());
                }
            }

            // 4. 流式生成回答
            if (agentModelConfig != null) {
                // 使用 Agent 的模型配置进行流式生成
                streamingLlmService.streamGenerateWithConfig(
                        systemPrompt,
                        userQuestion,
                        agentModelConfig,
                        new StreamingLlmService.StreamCallback() {
                            @Override
                            public void onContent(String content) {
                                callback.onContent(content);
                            }

                            @Override
                            public void onComplete() {
                                callback.onComplete();
                            }

                            @Override
                            public void onError(Exception e) {
                                log.error("Error in stream generation", e);
                                callback.onError(e);
                            }
                        }
                );
            } else {
                // 使用全局配置进行流式生成
                streamingLlmService.streamGenerate(
                        systemPrompt,
                        userQuestion,
                        null,
                        1000,
                        new StreamingLlmService.StreamCallback() {
                            @Override
                            public void onContent(String content) {
                                callback.onContent(content);
                            }

                            @Override
                            public void onComplete() {
                                callback.onComplete();
                            }

                            @Override
                            public void onError(Exception e) {
                                log.error("Error in stream generation", e);
                                callback.onError(e);
                            }
                        }
                );
            }

        } catch (Exception e) {
            log.error("Error in ReAct stream execution", e);
            callback.onError(e);
        }
    }
}
