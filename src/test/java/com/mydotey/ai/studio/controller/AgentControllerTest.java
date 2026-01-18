package com.mydotey.ai.studio.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mydotey.ai.studio.dto.AgentExecutionRequest;
import com.mydotey.ai.studio.dto.AgentExecutionResponse;
import com.mydotey.ai.studio.dto.CreateAgentRequest;
import com.mydotey.ai.studio.dto.AgentResponse;
import com.mydotey.ai.studio.entity.Agent;
import com.mydotey.ai.studio.service.AgentExecutionService;
import com.mydotey.ai.studio.service.AgentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AgentController.class)
@ContextConfiguration(classes = {AgentController.class, com.mydotey.ai.studio.config.TestConfig.class})
public class AgentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AgentService agentService;

    @MockBean
    private AgentExecutionService agentExecutionService;

    @Test
    void testCreateAgent_Success() throws Exception {
        // Given
        CreateAgentRequest request = new CreateAgentRequest();
        request.setName("Test Agent");
        request.setSystemPrompt("You are helpful");
        request.setModelConfig("{\"model\":\"gpt-4\"}");
        request.setWorkflowType(com.mydotey.ai.studio.dto.WorkflowType.REACT);
        request.setKnowledgeBaseIds(List.of(1L));
        request.setToolIds(List.of(1L));

        Agent agent = new Agent();
        agent.setId(1L);
        agent.setName("Test Agent");
        agent.setWorkflowType("REACT");

        when(agentService.createAgent(any(), any(), any())).thenReturn(agent);
        when(agentService.getAgentKnowledgeBaseIds(any())).thenReturn(List.of(1L));
        when(agentService.getAgentToolIds(any())).thenReturn(List.of(1L));

        // When & Then
        mockMvc.perform(post("/api/agents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .requestAttr("userId", 1L)
                .requestAttr("orgId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("Test Agent"));
    }

    @Test
    void testExecuteAgent_Success() throws Exception {
        // Given
        AgentExecutionRequest request = new AgentExecutionRequest();
        request.setQuery("What is AI?");

        AgentExecutionResponse response = AgentExecutionResponse.builder()
            .answer("AI is artificial intelligence")
            .isComplete(true)
            .build();

        when(agentExecutionService.executeAgent(any(), any(), any())).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/agents/1/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.answer").value("AI is artificial intelligence"))
                .andExpect(jsonPath("$.data.isComplete").value(true));
    }
}
