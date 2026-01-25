package com.mydotey.ai.studio.service;

import com.mydotey.ai.studio.annotation.PerformanceMonitor;
import com.mydotey.ai.studio.common.exception.BusinessException;
import com.mydotey.ai.studio.dto.AgentExecutionRequest;
import com.mydotey.ai.studio.dto.AgentExecutionResponse;
import com.mydotey.ai.studio.entity.Agent;
import com.mydotey.ai.studio.service.agent.WorkflowExecutor;
import com.mydotey.ai.studio.service.StreamingChatCallback;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentExecutionService {

    private final AgentService agentService;
    private final List<WorkflowExecutor> workflowExecutors;
    private Map<String, WorkflowExecutor> executorMap;

    @PostConstruct
    public void init() {
        this.executorMap = workflowExecutors.stream()
            .collect(Collectors.toMap(
                executor -> executor.getClass().getSimpleName()
                    .replace("WorkflowExecutor", "")
                    .toUpperCase(),
                executor -> executor
            ));
        log.info("Initialized workflow executors: {}", executorMap.keySet());
    }

    /**
     * 执行 Agent
     */
    @PerformanceMonitor(value = "Agent Execution", slowThreshold = 5000)
    public AgentExecutionResponse executeAgent(Long agentId, AgentExecutionRequest request, Long userId) {
        log.info("Executing agent: {}, query: {}, userId: {}", agentId, request.getQuery(), userId);

        // 获取 Agent 配置
        Agent agent = agentService.getAgent(agentId);

        // 根据工作流类型选择执行器
        String workflowType = agent.getWorkflowType();
        if (workflowType == null) {
            workflowType = "REACT";
        }

        WorkflowExecutor executor = executorMap.get(workflowType);
        if (executor == null) {
            throw new BusinessException("Unsupported workflow type: " + workflowType);
        }

        return executor.execute(agent, request, userId);
    }

    /**
     * 流式执行 Agent
     */
    public void executeAgentStream(Long agentId, AgentExecutionRequest request, Long userId,
                                   StreamingLlmService.StreamCallback callback) {
        log.info("Executing agent stream: {}, query: {}, userId: {}", agentId, request.getQuery(), userId);

        try {
            // 获取 Agent 配置
            Agent agent = agentService.getAgent(agentId);

            // 根据工作流类型选择执行器
            String workflowType = agent.getWorkflowType();
            if (workflowType == null) {
                workflowType = "REACT";
            }

            WorkflowExecutor executor = executorMap.get(workflowType);
            if (executor == null) {
                throw new BusinessException("Unsupported workflow type: " + workflowType);
            }

            // 转换回调接口
            executor.executeStream(agent, request, userId, new StreamingChatCallback() {
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
                    log.error("Error in agent stream execution", e);
                    callback.onError(e);
                }
            });

        } catch (Exception e) {
            log.error("Error in agent stream execution", e);
            callback.onError(e);
        }
    }
}
