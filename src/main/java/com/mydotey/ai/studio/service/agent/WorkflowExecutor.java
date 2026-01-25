package com.mydotey.ai.studio.service.agent;

import com.mydotey.ai.studio.dto.AgentExecutionRequest;
import com.mydotey.ai.studio.dto.AgentExecutionResponse;
import com.mydotey.ai.studio.entity.Agent;
import com.mydotey.ai.studio.service.StreamingChatCallback;

public interface WorkflowExecutor {
    /**
     * 执行 Agent（非流式）
     */
    AgentExecutionResponse execute(Agent agent, AgentExecutionRequest request, Long userId);

    /**
     * 流式执行 Agent
     */
    void executeStream(Agent agent, AgentExecutionRequest request, Long userId, StreamingChatCallback callback);
}
