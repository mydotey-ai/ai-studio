package com.mydotey.ai.studio.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mydotey.ai.studio.common.exception.BusinessException;
import com.mydotey.ai.studio.dto.CreateAgentRequest;
import com.mydotey.ai.studio.dto.UpdateAgentRequest;
import com.mydotey.ai.studio.entity.Agent;
import com.mydotey.ai.studio.entity.AgentKnowledgeBase;
import com.mydotey.ai.studio.entity.AgentTool;
import com.mydotey.ai.studio.mapper.AgentKnowledgeBaseMapper;
import com.mydotey.ai.studio.mapper.AgentMapper;
import com.mydotey.ai.studio.mapper.AgentToolMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentService {

    private final AgentMapper agentMapper;
    private final AgentKnowledgeBaseMapper agentKbMapper;
    private final AgentToolMapper agentToolMapper;

    /**
     * 创建 Agent
     */
    @Transactional
    public Agent createAgent(CreateAgentRequest request, Long orgId, Long userId) {
        log.info("Creating agent: {}, userId: {}", request.getName(), userId);

        // 创建 Agent
        Agent agent = new Agent();
        agent.setOrgId(orgId);
        agent.setName(request.getName());
        agent.setDescription(request.getDescription());
        agent.setSystemPrompt(request.getSystemPrompt());
        agent.setOwnerId(userId);
        agent.setIsPublic(request.getIsPublic());
        agent.setModelConfig(request.getModelConfig());
        agent.setWorkflowType(request.getWorkflowType().name());
        agent.setWorkflowConfig(request.getWorkflowConfig());
        agent.setMaxIterations(request.getMaxIterations());
        agent.setCreatedAt(Instant.now());
        agent.setUpdatedAt(Instant.now());

        agentMapper.insert(agent);

        if (agent.getId() == null) {
            throw new BusinessException("Failed to create agent - ID not generated");
        }

        // 关联知识库
        if (request.getKnowledgeBaseIds() != null) {
            for (Long kbId : request.getKnowledgeBaseIds()) {
                AgentKnowledgeBase agentKb = new AgentKnowledgeBase();
                agentKb.setAgentId(agent.getId());
                agentKb.setKbId(kbId);
                agentKb.setCreatedAt(Instant.now());
                agentKbMapper.insert(agentKb);
            }
        }

        // 关联工具
        if (request.getToolIds() != null) {
            for (Long toolId : request.getToolIds()) {
                AgentTool agentTool = new AgentTool();
                agentTool.setAgentId(agent.getId());
                agentTool.setToolId(toolId);
                agentTool.setCreatedAt(Instant.now());
                agentToolMapper.insert(agentTool);
            }
        }

        log.info("Agent created: {}", agent.getId());
        return agent;
    }

    /**
     * 更新 Agent
     */
    @Transactional
    public void updateAgent(Long agentId, UpdateAgentRequest request, Long userId) {
        log.info("Updating agent: {}, userId: {}", agentId, userId);

        Agent agent = getAgent(agentId);

        if (request.getName() != null) {
            agent.setName(request.getName());
        }
        if (request.getDescription() != null) {
            agent.setDescription(request.getDescription());
        }
        if (request.getSystemPrompt() != null) {
            agent.setSystemPrompt(request.getSystemPrompt());
        }
        if (request.getIsPublic() != null) {
            agent.setIsPublic(request.getIsPublic());
        }
        if (request.getModelConfig() != null) {
            agent.setModelConfig(request.getModelConfig());
        }
        if (request.getWorkflowType() != null) {
            agent.setWorkflowType(request.getWorkflowType().name());
        }
        if (request.getWorkflowConfig() != null) {
            agent.setWorkflowConfig(request.getWorkflowConfig());
        }
        if (request.getMaxIterations() != null) {
            agent.setMaxIterations(request.getMaxIterations());
        }
        agent.setUpdatedAt(Instant.now());

        agentMapper.updateById(agent);

        // 删除旧的关联
        LambdaQueryWrapper<AgentKnowledgeBase> kbQuery = new LambdaQueryWrapper<>();
        kbQuery.eq(AgentKnowledgeBase::getAgentId, agentId);
        agentKbMapper.delete(kbQuery);

        LambdaQueryWrapper<AgentTool> toolQuery = new LambdaQueryWrapper<>();
        toolQuery.eq(AgentTool::getAgentId, agentId);
        agentToolMapper.delete(toolQuery);

        // 添加新的关联
        if (request.getKnowledgeBaseIds() != null) {
            for (Long kbId : request.getKnowledgeBaseIds()) {
                AgentKnowledgeBase agentKb = new AgentKnowledgeBase();
                agentKb.setAgentId(agentId);
                agentKb.setKbId(kbId);
                agentKb.setCreatedAt(Instant.now());
                agentKbMapper.insert(agentKb);
            }
        }

        if (request.getToolIds() != null) {
            for (Long toolId : request.getToolIds()) {
                AgentTool agentTool = new AgentTool();
                agentTool.setAgentId(agentId);
                agentTool.setToolId(toolId);
                agentTool.setCreatedAt(Instant.now());
                agentToolMapper.insert(agentTool);
            }
        }

        log.info("Agent updated: {}", agentId);
    }

    /**
     * 获取 Agent
     */
    public Agent getAgent(Long agentId) {
        Agent agent = agentMapper.selectById(agentId);
        if (agent == null) {
            throw new BusinessException("Agent not found");
        }
        return agent;
    }

    /**
     * 删除 Agent
     */
    @Transactional
    public void deleteAgent(Long agentId, Long userId) {
        log.info("Deleting agent: {}, userId: {}", agentId, userId);
        agentMapper.deleteById(agentId);
        log.info("Agent deleted: {}", agentId);
    }

    /**
     * 获取 Agent 的知识库 IDs
     */
    public List<Long> getAgentKnowledgeBaseIds(Long agentId) {
        LambdaQueryWrapper<AgentKnowledgeBase> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AgentKnowledgeBase::getAgentId, agentId);
        List<AgentKnowledgeBase> agentKbs = agentKbMapper.selectList(queryWrapper);
        return agentKbs.stream().map(AgentKnowledgeBase::getKbId).toList();
    }

    /**
     * 获取 Agent 的工具 IDs
     */
    public List<Long> getAgentToolIds(Long agentId) {
        LambdaQueryWrapper<AgentTool> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AgentTool::getAgentId, agentId);
        List<AgentTool> agentTools = agentToolMapper.selectList(queryWrapper);
        return agentTools.stream().map(AgentTool::getToolId).toList();
    }
}
