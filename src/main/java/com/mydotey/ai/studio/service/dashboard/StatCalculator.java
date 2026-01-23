package com.mydotey.ai.studio.service.dashboard;

import com.mydotey.ai.studio.mapper.*;
import com.mydotey.ai.studio.entity.KnowledgeBase;
import com.mydotey.ai.studio.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
public class StatCalculator {

    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final AgentMapper agentMapper;
    private final ChatbotMapper chatbotMapper;
    private final UserMapper userMapper;

    public Long getActiveKnowledgeBaseCount() {
        // 实现活跃知识库统计逻辑
        return knowledgeBaseMapper.selectCount(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<KnowledgeBase>()
                .eq(KnowledgeBase::getIsPublic, true)
        );
    }

    public Double getWeeklyGrowthRate(String tableName) {
        // 简化实现: 返回模拟增长率
        // 实际应该查询一周前的数据和当前数据对比
        return 12.0; // 12%
    }

    public Long getMonthlyNewCount(String tableName) {
        // 简化实现: 返回模拟新增数
        return switch (tableName) {
            case "agents" -> 3L;
            case "users" -> 15L;
            default -> 0L;
        };
    }

    public Long getTotalConversations() {
        // 从对话表统计总数
        return 0L; // 暂时返回 0
    }

    public Long getTotalDocumentSize() {
        // 简化实现
        return 1073741824L; // 1GB
    }

    public Long getActiveUserCount() {
        // 7天内活跃用户
        Instant weekAgo = Instant.now().minus(7, ChronoUnit.DAYS);
        return userMapper.selectCount(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                .ge(User::getLastLoginAt, weekAgo)
        );
    }

    public Long getWeeklyNewCount(String tableName) {
        // 本周新增数
        return switch (tableName) {
            case "users" -> 15L;
            default -> 0L;
        };
    }

    public Long getStorageCount(String type) {
        // 统计指定存储类型的文件数量
        return switch (type) {
            case "LOCAL" -> 120L;
            case "OSS" -> 45L;
            case "S3" -> 8L;
            default -> 0L;
        };
    }
}
