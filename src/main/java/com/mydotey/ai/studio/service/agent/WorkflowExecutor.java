package com.mydotey.ai.studio.service.agent;

import com.mydotey.ai.studio.dto.AgentExecutionRequest;
import com.mydotey.ai.studio.dto.AgentExecutionResponse;
import com.mydotey.ai.studio.entity.Agent;

public interface WorkflowExecutor {
    AgentExecutionResponse execute(Agent agent, AgentExecutionRequest request, Long userId);
}
