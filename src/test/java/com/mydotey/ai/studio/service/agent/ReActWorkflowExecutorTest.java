package com.mydotey.ai.studio.service.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mydotey.ai.studio.dto.AgentExecutionRequest;
import com.mydotey.ai.studio.dto.AgentExecutionResponse;
import com.mydotey.ai.studio.dto.RagQueryRequest;
import com.mydotey.ai.studio.dto.RagQueryResponse;
import com.mydotey.ai.studio.entity.Agent;
import com.mydotey.ai.studio.service.*;
import com.mydotey.ai.studio.service.mcp.McpRpcClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ReActWorkflowExecutorTest {

    private RagService ragService;
    private LlmGenerationService llmGenerationService;
    private McpRpcClient mcpRpcClient;
    private AgentService agentService;
    private ModelConfigService modelConfigService;
    private ObjectMapper objectMapper;
    private ReActWorkflowExecutor executor;

    @BeforeEach
    void setUp() {
        ragService = mock(RagService.class);
        llmGenerationService = mock(LlmGenerationService.class);
        mcpRpcClient = mock(McpRpcClient.class);
        agentService = mock(AgentService.class);
        modelConfigService = mock(ModelConfigService.class);
        objectMapper = mock(ObjectMapper.class);
        executor = new ReActWorkflowExecutor(ragService, llmGenerationService, mcpRpcClient, agentService, modelConfigService, objectMapper);
    }

    @Test
    void testExecute_WithoutTools_ReturnsDirectAnswer() throws Exception {
        // Given
        Agent agent = new Agent();
        agent.setId(1L);
        agent.setSystemPrompt("You are a helpful assistant");
        agent.setMaxIterations(5);

        AgentExecutionRequest request = new AgentExecutionRequest();
        request.setQuery("What is AI?");

        when(agentService.getAgentKnowledgeBaseIds(1L)).thenReturn(List.of(1L));
        when(agentService.getAgentToolIds(1L)).thenReturn(List.of());

        RagQueryResponse ragResponse = RagQueryResponse.builder()
                .answer("AI is artificial intelligence")
                .build();

        when(ragService.query(any(RagQueryRequest.class), any())).thenReturn(ragResponse);
        when(llmGenerationService.generate(any(), any(), any(), any())).thenReturn(
                com.mydotey.ai.studio.dto.LlmResponse.builder()
                        .content("AI is artificial intelligence")
                        .finishReason("stop")
                        .totalTokens(100)
                        .build());

        // When
        AgentExecutionResponse response = executor.execute(agent, request, 1L);

        // Then
        assertNotNull(response);
        assertNotNull(response.getAnswer());
        assertTrue(response.getIsComplete());
    }
}
