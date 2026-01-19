package com.mydotey.ai.studio.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mydotey.ai.studio.dto.AgentExecutionRequest;
import com.mydotey.ai.studio.dto.AgentExecutionResponse;
import com.mydotey.ai.studio.dto.Message;
import com.mydotey.ai.studio.dto.MessageRole;
import com.mydotey.ai.studio.dto.SourceDocument;
import com.mydotey.ai.studio.dto.chatbot.ChatRequest;
import com.mydotey.ai.studio.dto.chatbot.ChatResponse;
import com.mydotey.ai.studio.dto.chatbot.ConversationResponse;
import com.mydotey.ai.studio.entity.Chatbot;
import com.mydotey.ai.studio.mapper.ChatbotMapper;
import com.mydotey.ai.studio.mapper.MessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatbotMapper chatbotMapper;
    private final ConversationService conversationService;
    private final MessageMapper messageMapper;
    private final AgentExecutionService agentExecutionService;
    private final ChatbotService chatbotService;
    private final ObjectMapper objectMapper;

    /**
     * 发送消息并获取回复（非流式）
     */
    @Transactional
    public ChatResponse chat(ChatRequest request, Long userId) {
        Long chatbotId = request.getChatbotId();
        Long conversationId = request.getConversationId();
        String userMessage = request.getMessage();

        log.info("Received chat request, chatbot: {}, conversation: {}, message: {}",
                chatbotId, conversationId, userMessage);

        // 1. 获取聊天机器人
        Chatbot chatbot = chatbotMapper.selectById(chatbotId);
        if (chatbot == null) {
            throw new RuntimeException("Chatbot not found");
        }

        // 增加访问计数
        chatbotService.incrementAccessCount(chatbotId);

        // 2. 获取或创建对话
        Long conversationIdToUse;
        if (conversationId == null) {
            ConversationResponse convResponse = conversationService.create(chatbotId, userId);
            conversationIdToUse = convResponse.getId();
        } else {
            conversationIdToUse = conversationId;
        }

        // 3. 保存用户消息
        com.mydotey.ai.studio.entity.Message userMsg = new com.mydotey.ai.studio.entity.Message();
        userMsg.setConversationId(conversationIdToUse);
        userMsg.setRole("user");
        userMsg.setContent(userMessage);
        userMsg.setCreatedAt(Instant.now());
        messageMapper.insert(userMsg);

        // 4. 加载对话历史
        List<com.mydotey.ai.studio.entity.Message> historyMessages = messageMapper.selectList(
                new LambdaQueryWrapper<com.mydotey.ai.studio.entity.Message>()
                        .eq(com.mydotey.ai.studio.entity.Message::getConversationId, conversationIdToUse)
                        .orderByAsc(com.mydotey.ai.studio.entity.Message::getCreatedAt)
        );

        List<Message> agentHistory = historyMessages.stream()
                .map(msg -> Message.builder()
                        .role(convertToMessageRole(msg.getRole()))
                        .content(msg.getContent())
                        .build())
                .collect(Collectors.toList());

        // 5. 调用 Agent
        AgentExecutionRequest agentRequest = new AgentExecutionRequest();
        agentRequest.setQuery(userMessage);
        agentRequest.setContext(null);

        AgentExecutionResponse agentResponse = agentExecutionService.executeAgent(
                chatbot.getAgentId(),
                agentRequest,
                userId
        );

        // 6. 保存助手回复
        com.mydotey.ai.studio.entity.Message assistantMsg = new com.mydotey.ai.studio.entity.Message();
        assistantMsg.setConversationId(conversationIdToUse);
        assistantMsg.setRole("assistant");
        assistantMsg.setContent(agentResponse.getAnswer());
        assistantMsg.setSources(formatSources(null));
        assistantMsg.setToolCalls(formatToolCalls(agentResponse.getToolCalls()));
        assistantMsg.setMetadata("{}");
        assistantMsg.setCreatedAt(Instant.now());
        messageMapper.insert(assistantMsg);

        // 7. 更新对话时间
        conversationService.touch(conversationIdToUse);

        // 8. 构建响应
        return ChatResponse.builder()
                .conversationId(conversationIdToUse)
                .messageId(assistantMsg.getId())
                .answer(agentResponse.getAnswer())
                .sources(null)
                .isComplete(true)
                .build();
    }

    /**
     * 将字符串角色转换为 MessageRole 枚举
     */
    private MessageRole convertToMessageRole(String role) {
        if (role == null) {
            return MessageRole.USER;
        }
        return switch (role.toLowerCase()) {
            case "user" -> MessageRole.USER;
            case "assistant" -> MessageRole.ASSISTANT;
            case "system" -> MessageRole.SYSTEM;
            default -> MessageRole.USER;
        };
    }

    /**
     * 格式化来源文档
     */
    private String formatSources(List<SourceDocument> sources) {
        if (sources == null || sources.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(sources);
        } catch (Exception e) {
            log.error("Failed to format sources", e);
            return "[]";
        }
    }

    /**
     * 格式化工具调用
     */
    private String formatToolCalls(Object toolCalls) {
        if (toolCalls == null) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(toolCalls);
        } catch (Exception e) {
            log.error("Failed to format tool calls", e);
            return "[]";
        }
    }
}
