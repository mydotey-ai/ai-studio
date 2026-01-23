package com.mydotey.ai.studio.service;

import com.mydotey.ai.studio.dto.dashboard.*;
import com.mydotey.ai.studio.entity.*;
import com.mydotey.ai.studio.mapper.*;
import com.mydotey.ai.studio.service.dashboard.StatCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final AgentMapper agentMapper;
    private final ChatbotMapper chatbotMapper;
    private final DocumentMapper documentMapper;
    private final UserMapper userMapper;
    private final FileMetadataMapper fileMetadataMapper;
    private final AuditLogMapper auditLogMapper;
    private final StatCalculator statCalculator;

    /**
     * 获取统计数据汇总
     */
    public DashboardStatisticsDTO getStatistics() {
        DashboardStatisticsDTO stats = new DashboardStatisticsDTO();

        stats.setKnowledgeBases(getKnowledgeBaseStats());
        stats.setAgents(getAgentStats());
        stats.setChatbots(getChatbotStats());
        stats.setDocuments(getDocumentStats());
        stats.setUsers(getUserStats());
        stats.setStorage(getStorageStats());

        return stats;
    }

    /**
     * 获取趋势数据
     */
    public java.util.List<TrendDataDTO> getTrends(int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);

        java.util.List<TrendDataDTO> trends = new java.util.ArrayList<>();

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            TrendDataDTO trend = new TrendDataDTO();
            trend.setDate(date.toString());

            // 从审计日志计算活跃用户和 API 调用
            Long activeUsers = countActiveUsers(date);
            Long apiCalls = countApiCalls(date);

            trend.setActiveUsers(activeUsers.intValue());
            trend.setApiCalls(apiCalls);

            trends.add(trend);
        }

        return trends;
    }

    /**
     * 获取最近活动
     */
    public java.util.List<ActivityDTO> getRecentActivities(int limit) {
        return auditLogMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AuditLog>()
                .orderByDesc(AuditLog::getCreatedAt)
                .last("LIMIT " + limit)
        ).stream().map(log -> {
            ActivityDTO dto = new ActivityDTO();
            dto.setId(log.getId());
            dto.setAction(log.getAction());
            dto.setResourceType(log.getResourceType());
            dto.setCreatedAt(log.getCreatedAt());
            dto.setUsername(log.getUserId() != null ? getUserById(log.getUserId()) : "System");
            return dto;
        }).toList();
    }

    /**
     * 获取系统健康状态
     */
    public HealthStatusDTO getHealthStatus() {
        HealthStatusDTO health = new HealthStatusDTO();

        // 计算 API 成功率 (基于最近 100 条审计日志)
        Double successRate = calculateApiSuccessRate();
        health.setApiSuccessRate(successRate);

        // 计算平均响应时间
        Long avgResponseTime = calculateAvgResponseTime();
        health.setAvgResponseTime(avgResponseTime);

        // 错误率
        health.setErrorRate(100.0 - successRate);

        return health;
    }

    private KnowledgeBaseStats getKnowledgeBaseStats() {
        KnowledgeBaseStats stats = new KnowledgeBaseStats();

        Long total = knowledgeBaseMapper.selectCount(null);
        stats.setTotalCount(total);
        stats.setActiveCount(statCalculator.getActiveKnowledgeBaseCount());
        stats.setArchivedCount(total - stats.getActiveCount());
        stats.setWeeklyGrowthRate(statCalculator.getWeeklyGrowthRate("knowledge_bases"));

        return stats;
    }

    private AgentStats getAgentStats() {
        AgentStats stats = new AgentStats();

        Long total = agentMapper.selectCount(null);
        stats.setTotalCount(total);
        stats.setReactCount(agentMapper.selectCount(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Agent>()
                .eq(Agent::getWorkflowType, "REACT")
        ));
        stats.setWorkflowCount(total - stats.getReactCount());
        stats.setMonthlyNewCount(statCalculator.getMonthlyNewCount("agents"));

        return stats;
    }

    private ChatbotStats getChatbotStats() {
        ChatbotStats stats = new ChatbotStats();

        Long total = chatbotMapper.selectCount(null);
        stats.setTotalCount(total);
        stats.setPublishedCount(chatbotMapper.selectCount(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Chatbot>()
                .eq(Chatbot::getIsPublished, true)
        ));
        stats.setDraftCount(total - stats.getPublishedCount());
        stats.setTotalConversations(statCalculator.getTotalConversations());

        return stats;
    }

    private DocumentStats getDocumentStats() {
        DocumentStats stats = new DocumentStats();

        Long total = documentMapper.selectCount(null);
        stats.setTotalCount(total);
        stats.setProcessingCount(documentMapper.selectCount(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Document>()
                .eq(Document::getStatus, "PROCESSING")
        ));
        stats.setCompletedCount(total - stats.getProcessingCount());
        stats.setTotalSizeBytes(statCalculator.getTotalDocumentSize());

        return stats;
    }

    private UserStats getUserStats() {
        UserStats stats = new UserStats();

        Long total = userMapper.selectCount(null);
        stats.setTotalCount(total);
        stats.setActiveCount(statCalculator.getActiveUserCount());
        stats.setAdminCount(userMapper.selectCount(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                .in(User::getRole, java.util.List.of("ADMIN", "SUPER_ADMIN"))
        ));
        stats.setRegularCount(total - stats.getAdminCount());
        stats.setWeeklyNewCount(statCalculator.getWeeklyNewCount("users"));

        return stats;
    }

    private StorageStats getStorageStats() {
        StorageStats stats = new StorageStats();

        stats.setTotalSizeBytes(fileMetadataMapper.selectList(null).stream()
            .mapToLong(FileMetadata::getFileSize)
            .sum());

        stats.setFileCount(fileMetadataMapper.selectCount(null));

        // 统计不同存储类型的数量
        stats.setLocalCount(statCalculator.getStorageCount("LOCAL"));
        stats.setOssCount(statCalculator.getStorageCount("OSS"));
        stats.setS3Count(statCalculator.getStorageCount("S3"));

        return stats;
    }

    private Long countActiveUsers(LocalDate date) {
        // 从审计日志统计当天活跃用户
        return auditLogMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AuditLog>()
                .ge(AuditLog::getCreatedAt, date.atStartOfDay(ZoneId.systemDefault()))
                .lt(AuditLog::getCreatedAt, date.plusDays(1).atStartOfDay(ZoneId.systemDefault()))
        ).stream()
        .map(AuditLog::getUserId)
        .distinct()
        .count();
    }

    private Long countApiCalls(LocalDate date) {
        // 从审计日志统计当天 API 调用次数
        return auditLogMapper.selectCount(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AuditLog>()
                .ge(AuditLog::getCreatedAt, date.atStartOfDay(ZoneId.systemDefault()))
                .lt(AuditLog::getCreatedAt, date.plusDays(1).atStartOfDay(ZoneId.systemDefault()))
        );
    }

    private Double calculateApiSuccessRate() {
        // 基于最近 100 条审计日志,计算成功率
        Long total = Math.min(auditLogMapper.selectCount(null), 100L);
        if (total == 0) return 100.0;

        Long successCount = auditLogMapper.selectCount(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AuditLog>()
                .notLike(AuditLog::getAction, "%ERROR%")
                .last("LIMIT 100")
        );

        return (successCount.doubleValue() / total) * 100.0;
    }

    private Long calculateAvgResponseTime() {
        // 简化实现: 返回估算的平均响应时间
        // 实际应该从性能监控数据获取
        return 150L; // 150ms
    }

    private String getUserById(Long userId) {
        User user = userMapper.selectById(userId);
        return user != null ? user.getUsername() : "Unknown";
    }
}
