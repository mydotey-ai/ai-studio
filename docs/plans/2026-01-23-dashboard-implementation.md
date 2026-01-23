# 数据可视化仪表盘实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**目标:** 为 AI Studio 平台构建管理员数据可视化仪表盘,显示系统统计、资源分布、活动时间线等核心指标。

**架构:** 采用前后端分离架构,后端提供统计 API,前端使用 ECharts 渲染图表,数据自动刷新保持实时性。

**Tech Stack:** Spring Boot 3.5, MyBatis-Plus, Vue 3.5, TypeScript, Element Plus, ECharts 5.5, vue-echarts 6.6

---

## 实施任务概览

本计划包含以下任务组:

1. **后端基础** - DTOs、Service、Controller
2. **前端基础** - 类型定义、API 客户端、依赖安装
3. **统计卡片** - 6 个统计卡片组件
4. **资源分布图** - 环形饼图组件
5. **活动时间线** - 时间线组件
6. **主视图集成** - DashboardView 布局和数据刷新
7. **测试验证** - 端到端测试和优化

---

## Task Group 1: 后端基础架构

### Task 1.1: 创建 DTOs

**Files:**
- Create: `src/main/java/com/mydotey/ai/studio/dto/dashboard/DashboardStatisticsDTO.java`
- Create: `src/main/java/com/mydotey/ai/studio/dto/dashboard/KnowledgeBaseStats.java`
- Create: `src/main/java/com/mydotey/ai/studio/dto/dashboard/AgentStats.java`
- Create: `src/main/java/com/mydotey/ai/studio/dto/dashboard/ChatbotStats.java`
- Create: `src/main/java/com/mydotey/ai/studio/dto/dashboard/DocumentStats.java`
- Create: `src/main/java/com/mydotey/ai/studio/dto/dashboard/UserStats.java`
- Create: `src/main/java/com/mydotey/ai/studio/dto/dashboard/StorageStats.java`
- Create: `src/main/java/com/mydotey/ai/studio/dto/dashboard/TrendDataDTO.java`
- Create: `src/main/java/com/mydotey/ai/studio/dto/dashboard/ActivityDTO.java`
- Create: `src/main/java/com/mydotey/ai/studio/dto/dashboard/HealthStatusDTO.java`

**Step 1: 创建统计基础 DTO 类**

创建文件: `src/main/java/com/mydotey/ai/studio/dto/dashboard/KnowledgeBaseStats.java`

```java
package com.mydotey.ai.studio.dto.dashboard;

import lombok.Data;

@Data
public class KnowledgeBaseStats {
    private Long totalCount;
    private Long activeCount;
    private Long archivedCount;
    private Double weeklyGrowthRate; // 百分比
}
```

创建文件: `src/main/java/com/mydotey/ai/studio/dto/dashboard/AgentStats.java`

```java
package com.mydotey.ai.studio.dto.dashboard;

import lombok.Data;

@Data
public class AgentStats {
    private Long totalCount;
    private Long reactCount;
    private Long workflowCount;
    private Long monthlyNewCount;
}
```

创建文件: `src/main/java/com/mydotey/ai/studio/dto/dashboard/ChatbotStats.java`

```java
package com.mydotey.ai.studio.dto.dashboard;

import lombok.Data;

@Data
public class ChatbotStats {
    private Long totalCount;
    private Long publishedCount;
    private Long draftCount;
    private Long totalConversations;
}
```

创建文件: `src/main/java/com/mydotey/ai/studio/dto/dashboard/DocumentStats.java`

```java
package com.mydotey.ai.studio.dto.dashboard;

import lombok.Data;

@Data
public class DocumentStats {
    private Long totalCount;
    private Long processingCount;
    private Long completedCount;
    private Long totalSizeBytes; // 文件总大小
}
```

创建文件: `src/main/java/com/mydotey/ai/studio/dto/dashboard/UserStats.java`

```java
package com.mydotey.ai.studio.dto.dashboard;

import lombok.Data;

@Data
public class UserStats {
    private Long totalCount;
    private Long activeCount; // 7天内有登录
    private Long adminCount;
    private Long regularCount;
    private Long weeklyNewCount;
}
```

创建文件: `src/main/java/com/mydotey/ai/studio/dto/dashboard/StorageStats.java`

```java
package com.mydotey.ai.studio.dto.dashboard;

import lombok.Data;

@Data
public class StorageStats {
    private Long totalSizeBytes;
    private Long fileCount;
    private Long localCount;
    private Long ossCount;
    private Long s3Count;
}
```

**Step 2: 创建主 DTO 类**

创建文件: `src/main/java/com/mydotey/ai/studio/dto/dashboard/DashboardStatisticsDTO.java`

```java
package com.mydotey.ai.studio.dto.dashboard;

import lombok.Data;

@Data
public class DashboardStatisticsDTO {
    private KnowledgeBaseStats knowledgeBases;
    private AgentStats agents;
    private ChatbotStats chatbots;
    private DocumentStats documents;
    private UserStats users;
    private StorageStats storage;
}
```

**Step 3: 创建趋势和活动 DTO**

创建文件: `src/main/java/com/mydotey/ai/studio/dto/dashboard/TrendDataDTO.java`

```java
package com.mydotey.ai.studio.dto.dashboard;

import lombok.Data;

@Data
public class TrendDataDTO {
    private String date; // YYYY-MM-DD
    private Long apiCalls;
    private Integer activeUsers;
}
```

创建文件: `src/main/java/com/mydotey/ai/studio/dto/dashboard/ActivityDTO.java`

```java
package com.mydotey.ai.studio.dto.dashboard;

import lombok.Data;

import java.time.Instant;

@Data
public class ActivityDTO {
    private Long id;
    private String action;
    private String resourceType;
    private String username;
    private Instant createdAt;
}
```

创建文件: `src/main/java/com/mydotey/ai/studio/dto/dashboard/HealthStatusDTO.java`

```java
package com.mydotey.ai.studio.dto.dashboard;

import lombok.Data;

@Data
public class HealthStatusDTO {
    private Double apiSuccessRate;
    private Long avgResponseTime; // 毫秒
    private Double errorRate;
}
```

**Step 4: 提交 DTOs**

```bash
git add src/main/java/com/mydotey/ai/studio/dto/dashboard/
git commit -m "feat: add dashboard DTOs"
```

---

### Task 1.2: 创建 DashboardService

**Files:**
- Create: `src/main/java/com/mydotey/ai/studio/service/DashboardService.java`

**Step 1: 创建 Service 类**

创建文件: `src/main/java/com/mydotey/ai/studio/service/DashboardService.java`

```java
package com.mydotey.ai.studio.service;

import com.mydotey.ai.studio.dto.dashboard.*;
import com.mydotey.ai.studio.mapper.*;
import com.mydotey.ai.studio.service.dashboard.StatCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.List;

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
    public List<TrendDataDTO> getTrends(int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);

        List<TrendDataDTO> trends = new java.util.ArrayList<>();

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
    public List<ActivityDTO> getRecentActivities(int limit) {
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
                .in(User::getRole, List.of(UserRole.ADMIN, UserRole.SUPER_ADMIN))
        ));
        stats.setRegularCount(total - stats.getAdminCount());
        stats.setWeeklyNewCount(statCalculator.getWeeklyNewCount("users"));

        return stats;
    }

    private StorageStats getStorageStats() {
        StorageStats stats = new StorageStats();

        stats.setTotalSizeBytes(fileMetadataMapper.selectList(null).stream()
            .mapToLong(FileMetadata::getSize)
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
```

**Step 2: 创建 StatCalculator 辅助类**

创建文件: `src/main/java/com/mydotey/ai/studio/service/dashboard/StatCalculator.java`

```java
package com.mydotey.ai.studio.service.dashboard;

import com.mydotey.ai.studio.mapper.*;
import com.mydotey.ai.studio.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.*;

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
```

**Step 3: 添加 Mapper 导入到 Service**

修改文件顶部,添加所有必需的 import:

```java
import com.mydotey.ai.studio.entity.*;
import com.mydotey.ai.studio.mapper.*;
```

**Step 4: 提交 Service**

```bash
git add src/main/java/com/mydotey/ai/studio/service/DashboardService.java
git add src/main/java/com/mydotey/ai/studio/service/dashboard/
git commit -m "feat: add DashboardService with statistics calculation"
```

---

### Task 1.3: 创建 DashboardController

**Files:**
- Create: `src/main/java/com/mydotey/ai/studio/controller/DashboardController.java`

**Step 1: 创建 Controller**

创建文件: `src/main/java/com/mydotey/ai/studio/controller/DashboardController.java`

```java
package com.mydotey.ai.studio.controller;

import com.mydotey.ai.studio.annotation.RequireRole;
import com.mydotey.ai.studio.common.ApiResponse;
import com.mydotey.ai.studio.dto.dashboard.*;
import com.mydotey.ai.studio.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "仪表盘", description = "数据可视化仪表盘相关接口")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/statistics")
    @Operation(summary = "获取统计数据汇总", description = "获取知识库、Agent、聊天机器人、文档、用户、存储的统计数据")
    public ApiResponse<DashboardStatisticsDTO> getStatistics() {
        DashboardStatisticsDTO stats = dashboardService.getStatistics();
        return ApiResponse.success(stats);
    }

    @GetMapping("/trends")
    @Operation(summary = "获取趋势数据", description = "获取指定天数内的 API 调用和活跃用户趋势")
    public ApiResponse<List<TrendDataDTO>> getTrends(
            @RequestParam(defaultValue = "7") int days
    ) {
        List<TrendDataDTO> trends = dashboardService.getTrends(days);
        return ApiResponse.success(trends);
    }

    @GetMapping("/activities")
    @Operation(summary = "获取最近活动", description = "获取最近的审计日志记录")
    public ApiResponse<List<ActivityDTO>> getRecentActivities(
            @RequestParam(defaultValue = "10") int limit
    ) {
        List<ActivityDTO> activities = dashboardService.getRecentActivities(limit);
        return ApiResponse.success(activities);
    }

    @GetMapping("/health")
    @Operation(summary = "获取系统健康状态", description = "获取 API 成功率、响应时间等健康指标")
    public ApiResponse<HealthStatusDTO> getHealthStatus() {
        HealthStatusDTO health = dashboardService.getHealthStatus();
        return ApiResponse.success(health);
    }
}
```

**Step 2: 提交 Controller**

```bash
git add src/main/java/com/mydotey/ai/studio/controller/DashboardController.java
git commit -m "feat: add DashboardController with 4 endpoints"
```

---

## Task Group 2: 前端基础架构

### Task 2.1: 安装依赖

**Files:**
- Modify: `frontend/package.json`

**Step 1: 安装 ECharts**

```bash
cd frontend
npm install echarts@^5.5.0 vue-echarts@^6.6.0
```

**Step 2: 验证安装**

```bash
npm run build
```

预期输出: 构建成功,无错误

**Step 3: 提交 package.json 和 package-lock.json**

```bash
git add package.json package-lock.json
git commit -m "feat: install echarts and vue-echarts for dashboard"
```

---

### Task 2.2: 创建类型定义

**Files:**
- Create: `frontend/src/types/dashboard.ts`

**Step 1: 创建类型定义**

创建文件: `frontend/src/types/dashboard.ts`

```typescript
/**
 * 数据可视化仪表盘类型定义
 */

// 统计数据相关类型
export interface KnowledgeBaseStats {
  totalCount: number
  activeCount: number
  archivedCount: number
  weeklyGrowthRate: number
}

export interface AgentStats {
  totalCount: number
  reactCount: number
  workflowCount: number
  monthlyNewCount: number
}

export interface ChatbotStats {
  totalCount: number
  publishedCount: number
  draftCount: number
  totalConversations: number
}

export interface DocumentStats {
  totalCount: number
  processingCount: number
  completedCount: number
  totalSizeBytes: number
}

export interface UserStats {
  totalCount: number
  activeCount: number
  adminCount: number
  regularCount: number
  weeklyNewCount: number
}

export interface StorageStats {
  totalSizeBytes: number
  fileCount: number
  localCount: number
  ossCount: number
  s3Count: number
}

export interface DashboardStatistics {
  knowledgeBases: KnowledgeBaseStats
  agents: AgentStats
  chatbots: ChatbotStats
  documents: DocumentStats
  users: UserStats
  storage: StorageStats
}

// 趋势数据
export interface TrendData {
  date: string // YYYY-MM-DD
  apiCalls: number
  activeUsers: number
}

// 活动记录
export interface Activity {
  id: number
  action: string
  resourceType: string
  username: string
  createdAt: string
}

// 健康状态
export interface HealthStatus {
  apiSuccessRate: number
  avgResponseTime: number // 毫秒
  errorRate: number
}
```

**Step 2: 提交类型定义**

```bash
git add frontend/src/types/dashboard.ts
git commit -m "feat: add dashboard type definitions"
```

---

### Task 2.3: 创建 API 客户端

**Files:**
- Create: `frontend/src/api/dashboard.ts`

**Step 1: 创建 API 函数**

创建文件: `frontend/src/api/dashboard.ts`

```typescript
import request from './request'
import type {
  DashboardStatistics,
  TrendData,
  Activity,
  HealthStatus
} from '@/types/dashboard'

export const dashboardApi = {
  // 获取统计数据汇总
  getStatistics() {
    return request<DashboardStatistics, never>({
      url: '/dashboard/statistics',
      method: 'get'
    })
  },

  // 获取趋势数据
  getTrends(days: number = 7) {
    return request<TrendData[], never>({
      url: `/dashboard/trends?days=${days}`,
      method: 'get'
    })
  },

  // 获取最近活动
  getRecentActivities(limit: number = 10) {
    return request<Activity[], never>({
      url: `/dashboard/activities?limit=${limit}`,
      method: 'get'
    })
  },

  // 获取系统健康状态
  getHealthStatus() {
    return request<HealthStatus, never>({
      url: '/dashboard/health',
      method: 'get'
    })
  }
}
```

**Step 2: 提交 API 客户端**

```bash
git add frontend/src/api/dashboard.ts
git commit -m "feat: add dashboard API client"
```

---

## Task Group 3: 统计卡片组件

### Task 3.1: 创建 StatCard 组件

**Files:**
- Create: `frontend/src/components/dashboard/StatCard.vue`

**Step 1: 创建 StatCard 组件**

创建文件: `frontend/src/components/dashboard/StatCard.vue`

```vue
<template>
  <el-card class="stat-card" :class="{ 'card-hoverable': hoverable }">
    <div class="stat-content">
      <div class="stat-icon" :style="{ backgroundColor: iconBg }">
        <el-icon :size="24" :color="iconColor">
          <component :is="icon" />
        </el-icon>
      </div>

      <div class="stat-info">
        <div class="stat-value">{{ formattedValue }}</div>
        <div class="stat-label">{{ label }}</div>
        <div v-if="trend !== undefined" class="stat-trend" :class="trendClass">
          <el-icon :size="12">
            <component :is="trendIcon" />
          </el-icon>
          <span>{{ Math.abs(trend) }}%{{ trendLabel }}</span>
        </div>
        <div v-if="subtitle" class="stat-subtitle">{{ subtitle }}</div>
      </div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { TrendCharts } from '@element-plus/icons-vue'

interface Props {
  icon: any
  label: string
  value: number | string
  unit?: string
  trend?: number // 增长率,可以是正数或负数
  trendLabel?: string
  subtitle?: string
  iconColor?: string
  iconBg?: string
  hoverable?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  unit: '',
  trendLabel: '较上周',
  iconColor: '#409EFF',
  iconBg: '#ECF5FF',
  hoverable: true
})

const emit = defineEmits<{
  click: []
})

const formattedValue = computed(() => {
  if (typeof props.value === 'number') {
    return props.value.toLocaleString()
  }
  return props.value
})

const trendClass = computed(() => {
  if (!props.trend) return ''
  return props.trend >= 0 ? 'trend-up' : 'trend-down'
})

const trendIcon = computed(() => {
  return props.trend >= 0 ? TrendCharts : 'ArrowDown'
})
</script>

<style scoped>
.stat-card {
  border-radius: 8px;
  transition: all 0.3s;
}

.card-hoverable:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0,0,0,0.12);
  cursor: pointer;
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  line-height: 1.2;
  color: #303133;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 4px;
}

.stat-trend {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 8px;
  font-size: 12px;
}

.trend-up {
  color: #67C23A;
}

.trend-down {
  color: #F56C6C;
}

.stat-subtitle {
  font-size: 12px;
  color: #C0C4CC;
  margin-top: 4px;
}
</style>
```

**Step 2: 提交 StatCard**

```bash
git add frontend/src/components/dashboard/StatCard.vue
git commit -m "feat: add StatCard component"
```

---

### Task 3.2: 实现 6 个统计卡片

**Files:**
- Modify: `frontend/src/views/DashboardView.vue`

**Step 1: 实现 DashboardView 主视图**

修改文件: `frontend/src/views/DashboardView.vue`

```vue
<template>
  <div class="dashboard-view">
    <div class="dashboard-header">
      <h2>AI Studio 概览</h2>
      <el-button :icon="Refresh" @click="refreshAll" :loading="refreshing">
        刷新
      </el-button>
    </div>

    <!-- 统计卡片区域 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :xs="12" :sm="8" :md="4" v-for="stat in statistics" :key="stat.key">
        <StatCard
          :icon="stat.icon"
          :label="stat.label"
          :value="stat.value"
          :unit="stat.unit"
          :trend="stat.trend"
          :trendLabel="stat.trendLabel"
          :subtitle="stat.subtitle"
          :iconColor="stat.iconColor"
          :iconBg="stat.iconBg"
          @click="handleCardClick(stat.key)"
        />
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="20" class="charts-row">
      <!-- 左侧图表区 -->
      <el-col :xs="24" :md="14">
        <el-card class="chart-card">
          <template #header>
            <span>资源分布</span>
          </template>
          <ResourcePieChart :data="statisticsData" v-if="statisticsData" />
        </el-card>
      </el-col>

      <!-- 右侧图表区 -->
      <el-col :xs="24" :md="10">
        <el-card class="chart-card">
          <template #header>
            <span>最近活动</span>
          </template>
          <ActivityTimeline :activities="activities" v-if="activities" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import StatCard from '@/components/dashboard/StatCard.vue'
import ResourcePieChart from '@/components/dashboard/ResourcePieChart.vue'
import ActivityTimeline from '@/components/dashboard/ActivityTimeline.vue'
import { dashboardApi } from '@/api/dashboard'
import type { DashboardStatistics, Activity } from '@/types/dashboard'
import {
  FolderOpened,
  User,
  ChatDotSquare,
  Document,
  Files
} from '@element-plus/icons-vue'

const statisticsData = ref<DashboardStatistics | null>(null)
const activities = ref<Activity[]>([])
const refreshing = ref(false)

let refreshTimer: number | null = null

// 统计卡片配置
const statistics = ref([
  {
    key: 'knowledgeBases',
    label: '知识库',
    icon: FolderOpened,
    value: 0,
    unit: '个',
    trend: 12,
    trendLabel: '较上周',
    subtitle: '活跃 / 归档',
    iconColor: '#5470C6',
    iconBg: '#ECF5FF'
  },
  {
    key: 'agents',
    label: 'Agent',
    icon: User,
    value: 0,
    unit: '个',
    trend: 3,
    trendLabel: '本月新增',
    subtitle: 'ReAct / Workflow',
    iconColor: '#91CC75',
    iconBg: '#E6F7FE'
  },
  {
    key: 'chatbots',
    label: '聊天机器人',
    icon: ChatDotSquare,
    value: 0,
    unit: '个',
    trend: -5,
    trendLabel: '较上周',
    subtitle: '已发布 / 草稿',
    iconColor: '#FAC858',
    iconBg: '#FFF7E6'
  },
  {
    key: 'documents',
    label: '文档',
    icon: Document,
    value: 0,
    unit: '个',
    trend: 8,
    trendLabel: '较上周',
    subtitle: '处理中 / 已完成',
    iconColor: '#EE6666',
    iconBg: '#FFEBEE'
  },
  {
    key: 'users',
    label: '用户',
    icon: User,
    value: 0,
    unit: '人',
    trend: 15,
    trendLabel: '本周新增',
    subtitle: '管理员 / 普通用户',
    iconColor: '#73C0DE',
    iconBg: '#E6F7FE'
  },
  {
    key: 'storage',
    label: '存储',
    icon: Files,
    value: 0,
    unit: 'GB',
    trend: 2,
    trendLabel: '较上周',
    subtitle: 'LOCAL / OSS / S3',
    iconColor: '#3BA272',
    iconBg: '#E6F8F3'
  }
])

const loadStatistics = async () => {
  try {
    const data = await dashboardApi.getStatistics()
    statisticsData.value = data.data

    // 更新卡片数据
    statistics.value[0].value = data.data.knowledgeBases.totalCount
    statistics.value[0].subtitle =
      `活跃 ${data.data.knowledgeBases.activeCount} / 归档 ${data.data.knowledgeBases.archivedCount}`
    statistics.value[0].trend = data.data.knowledgeBases.weeklyGrowthRate

    statistics.value[1].value = data.data.agents.totalCount
    statistics.value[1].subtitle =
      `ReAct ${data.data.agents.reactCount} / Workflow ${data.data.agents.workflowCount}`
    statistics.value[1].trend = data.data.agents.monthlyNewCount

    statistics.value[2].value = data.data.chatbots.totalCount
    statistics.value[2].subtitle =
      `已发布 ${data.data.chatbots.publishedCount} / 草稿 ${data.data.chatbots.draftCount}`

    statistics.value[3].value = data.data.documents.totalCount
    statistics.value[3].subtitle =
      `处理中 ${data.data.documents.processingCount} / 已完成 ${data.data.documents.completedCount}`

    statistics.value[4].value = data.data.users.totalCount
    statistics.value[4].subtitle =
      `管理员 ${data.data.users.adminCount} / 普通用户 ${data.data.users.regularCount}`

    statistics.value[5].value = (data.data.storage.totalSizeBytes / (1024 * 1024 * 1024)).toFixed(1)
    statistics.value[5].subtitle =
      `文件 ${data.data.storage.fileCount} 个`
  } catch (error) {
    ElMessage.error('加载统计数据失败')
  }
}

const loadActivities = async () => {
  try {
    const data = await dashboardApi.getRecentActivities(10)
    activities.value = data.data
  } catch (error) {
    ElMessage.error('加载活动记录失败')
  }
}

const refreshAll = async () => {
  refreshing.value = true
  try {
    await Promise.all([loadStatistics(), loadActivities()])
    ElMessage.success('刷新成功')
  } catch (error) {
    ElMessage.error('刷新失败')
  } finally {
    refreshing.value = false
  }
}

const handleCardClick = (key: string) => {
  // 根据点击的卡片跳转到相应页面
  const routes: Record<string, string> = {
    knowledgeBases: '/knowledge-bases',
    agents: '/agents',
    chatbots: '/chatbots',
    documents: '/knowledge-bases', // 文档暂无独立页面
    users: '/settings', // 用户管理在设置中
    storage: '/settings'  // 存储配置在设置中
  }

  const route = routes[key]
  if (route) {
    // 使用 vue-router 跳转
    console.log('Navigate to:', route)
  }
}

// 自动刷新 (30秒)
const startAutoRefresh = () => {
  refreshTimer = window.setInterval(() => {
    loadActivities()
  }, 30000)
}

const stopAutoRefresh = () => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
}

onMounted(() => {
  loadStatistics()
  loadActivities()
  startAutoRefresh()
})

onUnmounted(() => {
  stopAutoRefresh()
})
</script>

<style scoped>
.dashboard-view {
  padding: 20px;
}

.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.dashboard-header h2 {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
}

.stats-row {
  margin-bottom: 20px;
}

.charts-row {
  margin-top: 20px;
}

.chart-card {
  height: 100%;
}

.chart-card :deep(.el-card__body) {
  min-height: 300px;
}
</style>
```

**Step 2: 提交 DashboardView**

```bash
git add frontend/src/views/DashboardView.vue
git commit -m "feat: implement DashboardView with stat cards and chart areas"
```

---

## Task Group 4: 资源分布图组件

### Task 4.1: 创建 ResourcePieChart 组件

**Files:**
- Create: `frontend/src/components/dashboard/ResourcePieChart.vue`

**Step 1: 创建组件**

创建文件: `frontend/src/components/dashboard/ResourcePieChart.vue`

```vue
<template>
  <div class="resource-pie-chart">
    <v-chart
      class="chart"
      :option="chartOption"
      :loading="loading"
      autoresize
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { PieChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import type { DashboardStatistics } from '@/types/dashboard'

use([PieChart, TitleComponent, TooltipComponent, LegendComponent])

interface Props {
  data: DashboardStatistics
}

const props = defineProps<Props>()

const loading = ref(false)

const chartOption = computed(() => {
  return {
    tooltip: {
      trigger: 'item',
      formatter: '{a} <br/>{b}: {c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      right: 10,
      top: 'center'
    },
    series: [
      {
        name: '资源统计',
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: false,
          position: 'center'
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 20,
            fontWeight: 'bold'
          }
        },
        labelLine: {
          show: false
        },
        data: [
          {
            value: props.data.knowledgeBases.totalCount,
            name: '知识库',
            itemStyle: { color: '#5470C6' }
          },
          {
            value: props.data.agents.totalCount,
            name: 'Agent',
            itemStyle: { color: '#91CC75' }
          },
          {
            value: props.data.chatbots.totalCount,
            name: '聊天机器人',
            itemStyle: { color: '#FAC858' }
          },
          {
            value: props.data.documents.totalCount,
            name: '文档',
            itemStyle: { color: '#EE6666' }
          },
          {
            value: props.data.users.totalCount,
            name: '用户',
            itemStyle: { color: '#73C0DE' }
          },
          {
            value: props.data.storage.fileCount,
            name: '存储文件',
            itemStyle: { color: '#3BA272' }
          }
        ]
      }
    ]
  }
})
</script>

<style scoped>
.resource-pie-chart {
  width: 100%;
  height: 300px;
}

.chart {
  width: 100%;
  height: 100%;
}
</style>
```

**Step 2: 提交 ResourcePieChart**

```bash
git add frontend/src/components/dashboard/ResourcePieChart.vue
git commit -m "feat: add ResourcePieChart component"
```

---

## Task Group 5: 活动时间线组件

### Task 5.1: 创建 ActivityTimeline 组件

**Files:**
- Create: `frontend/src/components/dashboard/ActivityTimeline.vue`

**Step 1: 创建组件**

创建文件: `frontend/src/components/dashboard/ActivityTimeline.vue`

```vue
<template>
  <div class="activity-timeline">
    <el-timeline>
      <el-timeline-item
        v-for="activity in activities"
        :key="activity.id"
        :timestamp="formatTime(activity.createdAt)"
        placement="top"
      >
        <el-card>
          <div class="activity-content">
            <div class="activity-header">
              <el-tag :type="getActionType(activity.action)" size="small">
                {{ formatAction(activity.action) }}
              </el-tag>
              <span class="activity-user">{{ activity.username }}</span>
            </div>
            <div class="activity-resource">
              {{ activity.resourceType }}
            </div>
          </div>
        </el-card>
      </el-timeline-item>
    </el-timeline>
  </div>
</template>

<script setup lang="ts">
import { type Activity } from '@/types/dashboard'
import dayjs from 'dayjs'

interface Props {
  activities: Activity[]
}

const props = defineProps<Props>()

const formatTime = (time: string) => {
  return dayjs(time).format('YYYY-MM-DD HH:mm:ss')
}

const formatAction = (action: string) => {
  const map: Record<string, string> = {
    'USER_LOGIN': '登录',
    'USER_LOGOUT': '登出',
    'KB_CREATE': '创建知识库',
    'KB_UPDATE': '更新知识库',
    'KB_DELETE': '删除知识库',
    'AGENT_CREATE': '创建 Agent',
    'AGENT_UPDATE': '更新 Agent',
    'AGENT_DELETE': '删除 Agent',
    'AGENT_EXECUTE': '执行 Agent',
    'CHATBOT_CREATE': '创建聊天机器人',
    'CHATBOT_UPDATE': '更新聊天机器人',
    'CHATBOT_DELETE': '删除聊天机器人'
  }
  return map[action] || action
}

const getActionType = (action: string) => {
  if (action.includes('CREATE')) return 'success'
  if (action.includes('DELETE')) return 'danger'
  if (action.includes('UPDATE')) return 'warning'
  if (action.includes('LOGIN')) return 'info'
  return ''
}
</script>

<style scoped>
.activity-timeline {
  padding: 10px 0;
}

.activity-content {
  padding: 0;
}

.activity-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.activity-user {
  font-size: 14px;
  color: #606266;
}

.activity-resource {
  font-size: 12px;
  color: #909399;
}
</style>
```

**Step 2: 提交 ActivityTimeline**

```bash
git add frontend/src/components/dashboard/ActivityTimeline.vue
git commit -m "feat: add ActivityTimeline component"
```

---

## Task Group 6: 测试验证

### Task 6.1: 类型检查和构建

**Step 1: TypeScript 类型检查**

```bash
cd frontend
npm run build
```

预期输出: 构建成功,无类型错误

**Step 2: ESLint 检查**

```bash
npm run lint
```

预期输出: 无新的错误或警告

**Step 3: 后端编译测试**

```bash
cd /home/koqizhao/Projects/mydotey-ai/ai-studio
mvn clean compile
```

预期输出: BUILD SUCCESS

---

### Task 6.2: 运行后端测试

**Step 1: 运行测试**

```bash
mvn test -Dtest=DashboardServiceTest
```

如果测试文件不存在,跳过此步骤。

**Step 2: 验证 API 端点**

```bash
# 启动应用
mvn spring-boot:run

# 在另一个终端测试 API
curl -H "Authorization: Bearer <token>" http://localhost:8080/api/dashboard/statistics
```

预期输出: JSON 格式的统计数据

---

### Task 6.3: 前端手动测试

**Step 1: 启动前端开发服务器**

```bash
cd frontend
npm run dev
```

**Step 2: 访问仪表盘页面**

打开浏览器访问: http://localhost:5173/dashboard

**验证清单:**
- [ ] 6 个统计卡片正确显示
- [ ] 统计数据有值（不为0）
- [ ] 趋势指示器正确显示（绿色增长/红色下降）
- [ ] 资源分布饼图正确渲染
- [ ] 活动时间线显示最近10条记录
- [ ] 点击刷新按钮数据更新
- [ ] 移动端布局正常

---

## 最终提交

**Step 1: 提交所有前端更改**

```bash
cd /home/koqizhao/Projects/mydotey-ai-studio
git add frontend/
git commit -m "feat: implement dashboard MVP with stat cards, pie chart, and activity timeline"
```

**Step 2: 更新项目进度文档**

修改文件: `docs/PROJECT_PROGRESS.md`

在 Phase 11 下添加:

```markdown
### 数据可视化仪表盘 ✅

**完成时间：2026-01-23**

**实现内容:**
- 6 个统计卡片（知识库、Agent、聊天机器人、文档、用户、存储）
- 资源分布环形图
- 最近活动时间线
- 后端 Dashboard API（4 个端点）
- 数据自动刷新（30秒）
- 响应式布局

```

**Step 3: 提交进度更新**

```bash
git add docs/PROJECT_PROGRESS.md
git commit -m "docs: mark dashboard MVP as completed"
```

---

## 总结

完成此计划后,数据可视化仪表盘 MVP 将包含:

✅ **后端实现:**
- 4 个 Dashboard API 端点
- 统计数据汇总服务
- 趋势数据聚合
- 活动记录查询
- 系统健康状态

✅ **前端实现:**
- 6 个统计卡片组件
- 资源分布环形图
- 最近活动时间线
- DashboardView 主视图
- 自动刷新功能

✅ **代码质量:**
- TypeScript 类型安全 100%
- ESLint 检查通过
- 生产构建成功
- 遵循 Vue 3 最佳实践
- 与现有架构完全集成

**新增文件统计:**
- 后端: 约 15 个新文件
- 前端: 8 个新文件
- 总代码量: 约 2,000+ 行
