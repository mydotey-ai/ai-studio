package com.mydotey.ai.studio.service;

import com.mydotey.ai.studio.annotation.PerformanceMonitor;
import com.mydotey.ai.studio.common.exception.BusinessException;
import com.mydotey.ai.studio.dto.AgentExecutionRequest;
import com.mydotey.ai.studio.dto.AgentExecutionResponse;
import com.mydotey.ai.studio.entity.Agent;
import com.mydotey.ai.studio.service.agent.WorkflowExecutor;
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
}
