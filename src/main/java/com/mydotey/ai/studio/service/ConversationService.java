package com.mydotey.ai.studio.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mydotey.ai.studio.common.exception.BusinessException;
import com.mydotey.ai.studio.dto.chatbot.ConversationResponse;
import com.mydotey.ai.studio.dto.chatbot.MessageResponse;
import com.mydotey.ai.studio.entity.Chatbot;
import com.mydotey.ai.studio.entity.Conversation;
import com.mydotey.ai.studio.entity.Message;
import com.mydotey.ai.studio.mapper.ChatbotMapper;
import com.mydotey.ai.studio.mapper.ConversationMapper;
import com.mydotey.ai.studio.mapper.MessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationMapper conversationMapper;
    private final ChatbotMapper chatbotMapper;
    private final MessageMapper messageMapper;

    /**
     * 创建新对话
     */
    @Transactional
    public ConversationResponse create(Long chatbotId, Long userId) {
        // 验证聊天机器人存在
        Chatbot chatbot = chatbotMapper.selectById(chatbotId);
        if (chatbot == null) {
            throw new BusinessException("Chatbot not found");
        }

        Conversation conversation = new Conversation();
        conversation.setChatbotId(chatbotId);
        conversation.setUserId(userId);
        conversation.setTitle("新对话");

        conversationMapper.insert(conversation);

        log.info("Created conversation: {} for chatbot: {}", conversation.getId(), chatbotId);

        return toResponse(conversation);
    }

    /**
     * 获取对话详情
     */
    public ConversationResponse getById(Long conversationId) {
        Conversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null) {
            throw new BusinessException("Conversation not found");
        }

        // 加载消息
        List<Message> messages = messageMapper.selectList(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getConversationId, conversationId)
                        .orderByAsc(Message::getCreatedAt)
        );

        return toResponse(conversation, messages);
    }

    /**
     * 获取用户在某个聊天机器人下的所有对话
     */
    public List<ConversationResponse> getByChatbotAndUser(Long chatbotId, Long userId) {
        List<Conversation> conversations = conversationMapper.selectList(
                new LambdaQueryWrapper<Conversation>()
                        .eq(Conversation::getChatbotId, chatbotId)
                        .eq(Conversation::getUserId, userId)
                        .orderByDesc(Conversation::getUpdatedAt)
        );
        return conversations.stream()
                .map(conv -> toResponse(conv, List.of()))
                .collect(Collectors.toList());
    }

    /**
     * 分页获取用户在某个聊天机器人下的对话
     */
    public IPage<ConversationResponse> getByChatbotAndUser(Long chatbotId, Long userId, int page, int size) {
        Page<Conversation> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<Conversation>()
                .eq(Conversation::getChatbotId, chatbotId)
                .eq(Conversation::getUserId, userId)
                .orderByDesc(Conversation::getUpdatedAt);
        IPage<Conversation> result = conversationMapper.selectPage(pageParam, wrapper);
        return result.convert(conv -> toResponse(conv, List.of()));
    }

    /**
     * 更新对话标题
     */
    @Transactional
    public void updateTitle(Long conversationId, String title) {
        Conversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null) {
            throw new BusinessException("Conversation not found");
        }

        conversation.setTitle(title);
        conversationMapper.updateById(conversation);

        log.info("Updated conversation title: {}", conversationId);
    }

    /**
     * 删除对话
     */
    @Transactional
    public void delete(Long conversationId) {
        // 删除对话的消息
        messageMapper.delete(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getConversationId, conversationId)
        );

        // 删除对话
        conversationMapper.deleteById(conversationId);

        log.info("Deleted conversation: {}", conversationId);
    }

    /**
     * 更新对话的更新时间
     */
    @Transactional
    public void touch(Long conversationId) {
        Conversation conversation = new Conversation();
        conversation.setId(conversationId);
        conversation.setUpdatedAt(java.time.Instant.now());
        conversationMapper.updateById(conversation);
    }

    /**
     * 转换为响应对象
     */
    private ConversationResponse toResponse(Conversation conversation, List<Message> messages) {
        List<MessageResponse> messageResponses = messages.stream()
                .map(this::toMessageResponse)
                .collect(Collectors.toList());

        return ConversationResponse.builder()
                .id(conversation.getId())
                .chatbotId(conversation.getChatbotId())
                .userId(conversation.getUserId())
                .title(conversation.getTitle())
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt())
                .messages(messageResponses)
                .build();
    }

    private ConversationResponse toResponse(Conversation conversation) {
        return toResponse(conversation, List.of());
    }

    private MessageResponse toMessageResponse(Message message) {
        return MessageResponse.builder()
                .id(message.getId())
                .conversationId(message.getConversationId())
                .role(message.getRole())
                .content(message.getContent())
                .sources(parseSources(message.getSources()))
                .toolCalls(message.getToolCalls())
                .metadata(message.getMetadata())
                .createdAt(message.getCreatedAt())
                .build();
    }

    private List<com.mydotey.ai.studio.dto.SourceDocument> parseSources(String sourcesJson) {
        // 简化版：实际应该解析 JSON
        return List.of();
    }
}
