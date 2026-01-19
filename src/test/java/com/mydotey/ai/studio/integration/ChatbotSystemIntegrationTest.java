package com.mydotey.ai.studio.integration;

import com.mydotey.ai.studio.dto.chatbot.*;
import com.mydotey.ai.studio.dto.agent.CreateAgentRequest;
import com.mydotey.ai.studio.dto.agent.AgentResponse;
import com.mydotey.ai.studio.service.AgentService;
import com.mydotey.ai.studio.service.ChatService;
import com.mydotey.ai.studio.service.ChatbotService;
import com.mydotey.ai.studio.service.ConversationService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@DisplayName("聊天机器人系统集成测试")
class ChatbotSystemIntegrationTest {

    @Autowired
    private ChatbotService chatbotService;

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private ChatService chatService;

    @Autowired
    private AgentService agentService;

    private Long testAgentId;
    private Long testChatbotId;
    private Long testUserId = 1L;

    @BeforeEach
    void setUp() {
        // 创建测试 Agent
        CreateAgentRequest agentRequest = new CreateAgentRequest();
        agentRequest.setName("测试Agent");
        agentRequest.setDescription("用于测试的Agent");
        agentRequest.setSystemPrompt("你是一个测试助手");
        agentRequest.setWorkflowType("REACT");

        AgentResponse agent = agentService.create(agentRequest, testUserId);
        testAgentId = agent.getId();
    }

    @AfterEach
    void tearDown() {
        // 清理测试数据（@Transactional 会自动回滚）
    }

    @Test
    @DisplayName("完整的聊天流程")
    void testCompleteChatFlow() {
        // 1. 创建聊天机器人
        CreateChatbotRequest createRequest = new CreateChatbotRequest();
        createRequest.setName("测试助手");
        createRequest.setAgentId(testAgentId);
        createRequest.setWelcomeMessage("你好，我是测试助手");

        ChatbotResponse chatbot = chatbotService.create(createRequest, testUserId);
        testChatbotId = chatbot.getId();

        assertNotNull(chatbot);
        assertEquals("测试助手", chatbot.getName());
        assertEquals(testAgentId, chatbot.getAgentId());

        // 2. 创建对话
        ConversationResponse conversation = conversationService.create(testChatbotId, testUserId);

        assertNotNull(conversation);
        assertEquals(testChatbotId, conversation.getChatbotId());

        // 3. 发送消息
        ChatRequest chatRequest = new ChatRequest();
        chatRequest.setChatbotId(testChatbotId);
        chatRequest.setConversationId(conversation.getId());
        chatRequest.setMessage("你好");

        ChatResponse chatResponse = chatService.chat(chatRequest, testUserId);

        assertNotNull(chatResponse);
        assertNotNull(chatResponse.getAnswer());
        assertTrue(chatResponse.getIsComplete());
        assertEquals(conversation.getId(), chatResponse.getConversationId());

        // 4. 获取对话详情
        ConversationResponse fullConversation = conversationService.getById(conversation.getId());

        assertNotNull(fullConversation);
        assertNotNull(fullConversation.getMessages());
        assertTrue(fullConversation.getMessages().size() >= 2); // 至少有用户消息和助手回复
    }

    @Test
    @DisplayName("应该能够获取我的聊天机器人列表")
    void testGetMyChatbots() {
        // 创建两个聊天机器人
        CreateChatbotRequest request1 = new CreateChatbotRequest();
        request1.setName("机器人1");
        request1.setAgentId(testAgentId);

        CreateChatbotRequest request2 = new CreateChatbotRequest();
        request2.setName("机器人2");
        request2.setAgentId(testAgentId);

        chatbotService.create(request1, testUserId);
        chatbotService.create(request2, testUserId);

        // 获取列表
        var chatbots = chatbotService.getByOwner(testUserId);

        assertNotNull(chatbots);
        assertTrue(chatbots.size() >= 2);
    }

    @Test
    @DisplayName("应该能够更新聊天机器人")
    void testUpdateChatbot() {
        // 创建聊天机器人
        CreateChatbotRequest createRequest = new CreateChatbotRequest();
        createRequest.setName("原始名称");
        createRequest.setAgentId(testAgentId);

        ChatbotResponse chatbot = chatbotService.create(createRequest, testUserId);

        // 更新
        UpdateChatbotRequest updateRequest = new UpdateChatbotRequest();
        updateRequest.setName("更新后的名称");
        updateRequest.setWelcomeMessage("欢迎");

        chatbotService.update(chatbot.getId(), updateRequest, testUserId);

        // 验证
        ChatbotResponse updated = chatbotService.getById(chatbot.getId());
        assertEquals("更新后的名称", updated.getName());
        assertEquals("欢迎", updated.getWelcomeMessage());
    }

    @Test
    @DisplayName("应该能够删除聊天机器人")
    void testDeleteChatbot() {
        // 创建聊天机器人
        CreateChatbotRequest request = new CreateChatbotRequest();
        request.setName("待删除");
        request.setAgentId(testAgentId);

        ChatbotResponse chatbot = chatbotService.create(request, testUserId);

        // 删除
        chatbotService.delete(chatbot.getId(), testUserId);

        // 验证（应该抛出异常）
        assertThrows(Exception.class, () -> chatbotService.getById(chatbot.getId()));
    }
}

