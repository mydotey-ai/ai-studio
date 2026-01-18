package com.mydotey.ai.studio.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mydotey.ai.studio.common.exception.BusinessException;
import com.mydotey.ai.studio.dto.chatbot.ChatbotResponse;
import com.mydotey.ai.studio.dto.chatbot.CreateChatbotRequest;
import com.mydotey.ai.studio.dto.chatbot.UpdateChatbotRequest;
import com.mydotey.ai.studio.entity.Agent;
import com.mydotey.ai.studio.entity.Chatbot;
import com.mydotey.ai.studio.mapper.AgentMapper;
import com.mydotey.ai.studio.mapper.ChatbotMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotService {

    private final ChatbotMapper chatbotMapper;
    private final AgentMapper agentMapper;

    /**
     * 创建聊天机器人
     */
    @Transactional
    public ChatbotResponse create(CreateChatbotRequest request, Long ownerId) {
        // 验证 Agent 存在
        Agent agent = agentMapper.selectById(request.getAgentId());
        if (agent == null) {
            throw new BusinessException("Agent not found");
        }

        // 检查 Agent 是否属于当前用户或公开
        if (!agent.getOwnerId().equals(ownerId) && !agent.getIsPublic()) {
            throw new BusinessException("Permission denied");
        }

        Chatbot chatbot = new Chatbot();
        chatbot.setAgentId(request.getAgentId());
        chatbot.setName(request.getName());
        chatbot.setDescription(request.getDescription());
        chatbot.setWelcomeMessage(request.getWelcomeMessage());
        chatbot.setAvatarUrl(request.getAvatarUrl());
        chatbot.setOwnerId(ownerId);
        chatbot.setSettings(request.getSettings());
        chatbot.setStyleConfig(request.getStyleConfig());
        chatbot.setIsPublished(request.getIsPublished());
        chatbot.setAccessCount(0L);

        chatbotMapper.insert(chatbot);

        log.info("Created chatbot: {} for agent: {}", chatbot.getId(), chatbot.getAgentId());

        return toResponse(chatbot);
    }

    /**
     * 获取聊天机器人详情
     */
    public ChatbotResponse getById(Long chatbotId) {
        Chatbot chatbot = chatbotMapper.selectById(chatbotId);
        if (chatbot == null) {
            throw new BusinessException("Chatbot not found");
        }
        return toResponse(chatbot);
    }

    /**
     * 获取用户的所有聊天机器人
     */
    public List<ChatbotResponse> getByOwner(Long ownerId) {
        List<Chatbot> chatbots = chatbotMapper.selectList(
                new LambdaQueryWrapper<Chatbot>()
                        .eq(Chatbot::getOwnerId, ownerId)
                        .orderByDesc(Chatbot::getCreatedAt)
        );
        return chatbots.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 获取已发布的聊天机器人
     */
    public List<ChatbotResponse> getPublishedChatbots() {
        List<Chatbot> chatbots = chatbotMapper.selectList(
                new LambdaQueryWrapper<Chatbot>()
                        .eq(Chatbot::getIsPublished, true)
                        .orderByDesc(Chatbot::getAccessCount)
        );
        return chatbots.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 更新聊天机器人
     */
    @Transactional
    public void update(Long chatbotId, UpdateChatbotRequest request, Long ownerId) {
        Chatbot chatbot = chatbotMapper.selectById(chatbotId);
        if (chatbot == null) {
            throw new BusinessException("Chatbot not found");
        }

        // 验证权限
        if (!chatbot.getOwnerId().equals(ownerId)) {
            throw new BusinessException("Permission denied");
        }

        chatbot.setName(request.getName());
        chatbot.setDescription(request.getDescription());
        chatbot.setWelcomeMessage(request.getWelcomeMessage());
        chatbot.setAvatarUrl(request.getAvatarUrl());
        chatbot.setSettings(request.getSettings());
        chatbot.setStyleConfig(request.getStyleConfig());
        chatbot.setIsPublished(request.getIsPublished());

        chatbotMapper.updateById(chatbot);

        log.info("Updated chatbot: {}", chatbotId);
    }

    /**
     * 删除聊天机器人
     */
    @Transactional
    public void delete(Long chatbotId, Long ownerId) {
        Chatbot chatbot = chatbotMapper.selectById(chatbotId);
        if (chatbot == null) {
            throw new BusinessException("Chatbot not found");
        }

        // 验证权限
        if (!chatbot.getOwnerId().equals(ownerId)) {
            throw new BusinessException("Permission denied");
        }

        chatbotMapper.deleteById(chatbotId);

        log.info("Deleted chatbot: {}", chatbotId);
    }

    /**
     * 增加访问计数
     */
    @Transactional
    public void incrementAccessCount(Long chatbotId) {
        Chatbot chatbot = chatbotMapper.selectById(chatbotId);
        if (chatbot != null) {
            chatbot.setAccessCount(chatbot.getAccessCount() + 1);
            chatbotMapper.updateById(chatbot);
        }
    }

    /**
     * 转换为响应对象
     */
    private ChatbotResponse toResponse(Chatbot chatbot) {
        return ChatbotResponse.builder()
                .id(chatbot.getId())
                .agentId(chatbot.getAgentId())
                .name(chatbot.getName())
                .description(chatbot.getDescription())
                .welcomeMessage(chatbot.getWelcomeMessage())
                .avatarUrl(chatbot.getAvatarUrl())
                .ownerId(chatbot.getOwnerId())
                .settings(chatbot.getSettings())
                .styleConfig(chatbot.getStyleConfig())
                .isPublished(chatbot.getIsPublished())
                .accessCount(chatbot.getAccessCount())
                .createdAt(chatbot.getCreatedAt())
                .updatedAt(chatbot.getUpdatedAt())
                .build();
    }
}
