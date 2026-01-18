package com.mydotey.ai.studio.service;

import com.mydotey.ai.studio.dto.chatbot.CreateChatbotRequest;
import com.mydotey.ai.studio.dto.chatbot.ChatbotResponse;
import com.mydotey.ai.studio.entity.Agent;
import com.mydotey.ai.studio.mapper.AgentMapper;
import com.mydotey.ai.studio.mapper.ChatbotMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("聊天机器人服务测试")
@ExtendWith(MockitoExtension.class)
class ChatbotServiceTest {

    @Mock
    private ChatbotMapper chatbotMapper;

    @Mock
    private AgentMapper agentMapper;

    @InjectMocks
    private ChatbotService chatbotService;

    @Test
    @DisplayName("应该能够创建聊天机器人")
    void testCreateChatbot() {
        Long agentId = 1L;
        Long ownerId = 1L;

        CreateChatbotRequest request = new CreateChatbotRequest();
        request.setName("测试机器人");
        request.setAgentId(agentId);
        request.setWelcomeMessage("你好");

        Agent agent = new Agent();
        agent.setId(agentId);
        agent.setOwnerId(ownerId);
        agent.setIsPublic(false);

        when(agentMapper.selectById(agentId)).thenReturn(agent);
        when(chatbotMapper.insert(any(com.mydotey.ai.studio.entity.Chatbot.class))).thenAnswer(invocation -> {
            com.mydotey.ai.studio.entity.Chatbot chatbot = invocation.getArgument(0);
            chatbot.setId(1L);
            return 1;
        });

        ChatbotResponse response = chatbotService.create(request, ownerId);

        assertNotNull(response);
        assertEquals("测试机器人", response.getName());
        assertEquals(agentId, response.getAgentId());
    }

    @Test
    @DisplayName("当 Agent 不存在时应该抛出异常")
    void testCreateChatbotWithNonExistentAgent() {
        CreateChatbotRequest request = new CreateChatbotRequest();
        request.setName("测试机器人");
        request.setAgentId(999L);

        when(agentMapper.selectById(999L)).thenReturn(null);

        assertThrows(Exception.class, () -> chatbotService.create(request, 1L));
    }
}
