package com.mydotey.ai.studio.service;

import com.mydotey.ai.studio.dto.CreateAgentRequest;
import com.mydotey.ai.studio.dto.UpdateAgentRequest;
import com.mydotey.ai.studio.entity.Agent;
import com.mydotey.ai.studio.mapper.AgentKnowledgeBaseMapper;
import com.mydotey.ai.studio.mapper.AgentMapper;
import com.mydotey.ai.studio.mapper.AgentToolMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class AgentServiceTest {

    private AgentMapper agentMapper = mock(AgentMapper.class);
    private AgentKnowledgeBaseMapper agentKbMapper = mock(AgentKnowledgeBaseMapper.class);
    private AgentToolMapper agentToolMapper = mock(AgentToolMapper.class);
    private AgentService agentService = new AgentService(agentMapper, agentKbMapper, agentToolMapper);

    @Test
    void testCreateAgent_Success() {
        // Given
        CreateAgentRequest request = new CreateAgentRequest();
        request.setName("Test Agent");
        request.setSystemPrompt("You are a helpful assistant");
        request.setModelConfig("{\"model\":\"gpt-4\"}");
        request.setWorkflowType(com.mydotey.ai.studio.dto.WorkflowType.REACT);
        request.setKnowledgeBaseIds(List.of(1L, 2L));
        request.setToolIds(List.of(1L));

        // Mock the insert to set the ID on the agent object
        doAnswer(invocation -> {
            Agent agent = invocation.getArgument(0);
            agent.setId(1L);
            return 1;
        }).when(agentMapper).insert(any(Agent.class));

        // When
        agentService.createAgent(request, 1L, 1L);

        // Then
        ArgumentCaptor<Agent> captor = ArgumentCaptor.forClass(Agent.class);
        verify(agentMapper).insert(captor.capture());

        Agent agent = captor.getValue();
        assertEquals("Test Agent", agent.getName());
        assertEquals("You are a helpful assistant", agent.getSystemPrompt());
        assertEquals(com.mydotey.ai.studio.dto.WorkflowType.REACT.name(), agent.getWorkflowType());
        assertEquals(1L, agent.getId());

        verify(agentKbMapper, times(2)).insert(any(com.mydotey.ai.studio.entity.AgentKnowledgeBase.class));
        verify(agentToolMapper, times(1)).insert(any(com.mydotey.ai.studio.entity.AgentTool.class));
    }

    @Test
    void testUpdateAgent_Success() {
        // Given
        Long agentId = 1L;
        Agent existingAgent = new Agent();
        existingAgent.setId(agentId);
        existingAgent.setName("Old Name");

        UpdateAgentRequest request = new UpdateAgentRequest();
        request.setName("Updated Name");
        request.setSystemPrompt("New prompt");
        request.setWorkflowType(com.mydotey.ai.studio.dto.WorkflowType.CUSTOM);
        request.setKnowledgeBaseIds(List.of(1L));

        when(agentMapper.selectById(agentId)).thenReturn(existingAgent);
        when(agentMapper.updateById(any(Agent.class))).thenReturn(1);

        // When
        agentService.updateAgent(agentId, request, 1L);

        // Then
        ArgumentCaptor<Agent> captor = ArgumentCaptor.forClass(Agent.class);
        verify(agentMapper).updateById(captor.capture());

        Agent updatedAgent = captor.getValue();
        assertEquals("Updated Name", updatedAgent.getName());
        assertEquals("New prompt", updatedAgent.getSystemPrompt());
    }

    @Test
    void testGetAgent_NotFound() {
        // Given
        when(agentMapper.selectById(1L)).thenReturn(null);

        // When & Then
        assertThrows(com.mydotey.ai.studio.common.exception.BusinessException.class, () -> {
            agentService.getAgent(1L);
        });
    }
}
