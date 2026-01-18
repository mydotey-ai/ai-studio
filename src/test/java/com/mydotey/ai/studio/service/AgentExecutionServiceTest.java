package com.mydotey.ai.studio.service;

import com.mydotey.ai.studio.common.exception.BusinessException;
import com.mydotey.ai.studio.dto.AgentExecutionRequest;
import com.mydotey.ai.studio.dto.AgentExecutionResponse;
import com.mydotey.ai.studio.entity.Agent;
import com.mydotey.ai.studio.service.agent.WorkflowExecutor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AgentExecutionServiceTest {

    private AgentService agentService = mock(AgentService.class);

    // Test implementation of WorkflowExecutor that simulates ReActWorkflowExecutor
    // Note: The class name must end with "WorkflowExecutor" to be properly registered
    private static class ReActWorkflowExecutor implements WorkflowExecutor {
        @Override
        public AgentExecutionResponse execute(Agent agent, AgentExecutionRequest request, Long userId) {
            return AgentExecutionResponse.builder()
                .answer("Test answer")
                .isComplete(true)
                .build();
        }
    }

    @Test
    void testExecuteAgent_ReactWorkflow_Success() {
        // Given
        Long agentId = 1L;
        Agent agent = new Agent();
        agent.setId(agentId);
        agent.setWorkflowType("REACT");

        AgentExecutionRequest request = new AgentExecutionRequest();
        request.setQuery("Test query");

        WorkflowExecutor mockReActExecutor = new ReActWorkflowExecutor();
        AgentExecutionService executionService = new AgentExecutionService(agentService, java.util.List.of(mockReActExecutor));
        executionService.init();

        when(agentService.getAgent(agentId)).thenReturn(agent);

        // When
        AgentExecutionResponse response = executionService.executeAgent(agentId, request, 1L);

        // Then
        assertNotNull(response);
        assertEquals("Test answer", response.getAnswer());
        assertTrue(response.getIsComplete());
    }

    @Test
    void testExecuteAgent_NullWorkflowType_DefaultsToReact() {
        // Given
        Agent agent = new Agent();
        agent.setId(1L);
        agent.setWorkflowType(null);

        AgentExecutionRequest request = new AgentExecutionRequest();
        request.setQuery("Test query");

        WorkflowExecutor mockReActExecutor = new ReActWorkflowExecutor();
        AgentExecutionService executionService = new AgentExecutionService(agentService, java.util.List.of(mockReActExecutor));
        executionService.init();

        when(agentService.getAgent(1L)).thenReturn(agent);

        // When
        AgentExecutionResponse response = executionService.executeAgent(1L, request, 1L);

        // Then
        assertEquals("Test answer", response.getAnswer());
    }

    @Test
    void testExecuteAgent_UnsupportedWorkflowType_ThrowsException() {
        // Given
        Agent agent = new Agent();
        agent.setId(1L);
        agent.setWorkflowType("CUSTOM");

        AgentExecutionRequest request = new AgentExecutionRequest();
        request.setQuery("Test query");

        WorkflowExecutor mockReActExecutor = new ReActWorkflowExecutor();
        AgentExecutionService executionService = new AgentExecutionService(agentService, java.util.List.of(mockReActExecutor));
        executionService.init();

        when(agentService.getAgent(1L)).thenReturn(agent);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            executionService.executeAgent(1L, request, 1L);
        });

        assertTrue(exception.getMessage().contains("Unsupported workflow type: CUSTOM"));
    }
}
