package com.mydotey.ai.studio.service.agent;

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
        while (!isComplete && iteration < maxIterations) {
            iteration++;
            log.info("ReAct iteration: {}", iteration);

            try {
                // 1. 构建上下文
                String context = buildContext(agent, request.getQuery(), thoughtSteps, kbIds);

                // 2. LLM 推理
                LlmResponse llmResponse = llmGenerationService.generate(
                    context,
                    buildReActPrompt(toolIds),
                    null,
                    1000
                );

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
        return "\n\nPlease provide a helpful answer.";
    }
}
