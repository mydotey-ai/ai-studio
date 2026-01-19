package com.mydotey.ai.studio.service;

import com.mydotey.ai.studio.dto.AgentExecutionRequest;
import com.mydotey.ai.studio.dto.AgentExecutionResponse;
import com.mydotey.ai.studio.dto.chatbot.ChatRequest;
import com.mydotey.ai.studio.dto.chatbot.ChatResponse;
import com.mydotey.ai.studio.entity.Chatbot;
import com.mydotey.ai.studio.entity.Conversation;
import com.mydotey.ai.studio.mapper.ChatbotMapper;
import com.mydotey.ai.studio.mapper.MessageMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("聊天服务测试")
@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatbotMapper chatbotMapper;

    @Mock
    private ConversationService conversationService;

    @Mock
    private MessageMapper messageMapper;

    @Mock
    private AgentExecutionService agentExecutionService;

    @Mock
    private ChatbotService chatbotService;

    @InjectMocks
    private ChatService chatService;

    @Test
    @DisplayName("应该能够发送消息并获取回复")
    void testSendMessage() {
        ChatRequest request = new ChatRequest();
        request.setChatbotId(1L);
        request.setMessage("你好");

        Chatbot chatbot = new Chatbot();
        chatbot.setId(1L);
        chatbot.setAgentId(1L);

        Conversation conversation = new Conversation();
        conversation.setId(1L);

        AgentExecutionResponse agentResponse = AgentExecutionResponse.builder()
                .answer("你好！有什么可以帮助你的吗？")
                .build();

        when(chatbotMapper.selectById(1L)).thenReturn(chatbot);
        when(conversationService.create(1L, null)).thenReturn(
                com.mydotey.ai.studio.dto.chatbot.ConversationResponse.builder()
                        .id(1L)
                        .chatbotId(1L)
                        .userId(null)
                        .title("新对话")
                        .build()
        );
        when(agentExecutionService.executeAgent(anyLong(), any(AgentExecutionRequest.class), any()))
                .thenReturn(agentResponse);

        ChatResponse response = chatService.chat(request, null);

        assertNotNull(response);
        assertEquals("你好！有什么可以帮助你的吗？", response.getAnswer());
    }
}
