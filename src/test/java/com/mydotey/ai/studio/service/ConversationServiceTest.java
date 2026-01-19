package com.mydotey.ai.studio.service;

import com.mydotey.ai.studio.dto.chatbot.ConversationResponse;
import com.mydotey.ai.studio.entity.Chatbot;
import com.mydotey.ai.studio.entity.Conversation;
import com.mydotey.ai.studio.mapper.ChatbotMapper;
import com.mydotey.ai.studio.mapper.ConversationMapper;
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

@DisplayName("对话服务测试")
@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

    @Mock
    private ConversationMapper conversationMapper;

    @Mock
    private ChatbotMapper chatbotMapper;

    @Mock
    private MessageMapper messageMapper;

    @InjectMocks
    private ConversationService conversationService;

    @Test
    @DisplayName("应该能够创建新对话")
    void testCreateConversation() {
        Long chatbotId = 1L;
        Long userId = 1L;

        Chatbot chatbot = new Chatbot();
        chatbot.setId(chatbotId);

        when(chatbotMapper.selectById(chatbotId)).thenReturn(chatbot);
        when(conversationMapper.insert(any(Conversation.class))).thenAnswer(invocation -> {
            Conversation conv = invocation.getArgument(0);
            conv.setId(1L);
            return 1;
        });

        ConversationResponse response = conversationService.create(chatbotId, userId);

        assertNotNull(response);
        assertEquals(chatbotId, response.getChatbotId());
    }

    @Test
    @DisplayName("当聊天机器人不存在时应该抛出异常")
    void testCreateConversationWithNonExistentChatbot() {
        when(chatbotMapper.selectById(999L)).thenReturn(null);

        assertThrows(Exception.class, () -> conversationService.create(999L, 1L));
    }
}
