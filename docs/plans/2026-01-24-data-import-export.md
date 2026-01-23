# Data Import/Export Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 实现数据导入导出功能,支持知识库、Agent、聊天机器人等核心数据的备份和迁移

**Architecture:**
- **后端:** 使用 Jackson JSON 序列化,支持增量导出和全量导出,提供异步导出任务
- **前端:** Element Plus Upload 组件实现文件上传,支持进度显示和错误处理
- **格式:** JSON 格式存储数据,包含元数据(版本、时间戳、导出范围)

**Tech Stack:**
- Jackson (JSON 处理)
- Apache POI (Excel 导出可选)
- Spring Async (异步任务)
- Element Plus (UI 组件)
- FileStorageService (文件存储)

---

## Task 1: 后端导入导出 DTO 设计

**Files:**
- Create: `src/main/java/com/mydotey/ai/studio/dto/export/DataExportRequest.java`
- Create: `src/main/java/com/mydotey/ai/studio/dto/export/DataExportResponse.java`
- Create: `src/main/java/com/mydotey/ai/studio/dto/export/DataImportRequest.java`
- Create: `src/main/java/com/mydotey/ai/studio/dto/export/DataImportResponse.java`
- Create: `src/main/java/com/mydotey/ai/studio/dto/export/ExportScope.java`
- Create: `src/main/java/com/mydotey/ai/studio/dto/export/ExportMetadata.java`
- Create: `src/main/java/com/mydotey/ai/studio/dto/export/ExportPackage.java`

**Step 1: 创建 ExportScope 枚举**

```java
// src/main/java/com/mydotey/ai/studio/dto/export/ExportScope.java
package com.mydotey.ai.studio.dto.export;

import java.util.Set;

/**
 * 数据导出范围枚举
 */
public enum ExportScope {
    /**
     * 知识库数据
     */
    KNOWLEDGE_BASES(Set.of(
        "knowledge_bases",
        "kb_members",
        "documents",
        "document_chunks"
    )),

    /**
     * Agent 数据
     */
    AGENTS(Set.of(
        "agents",
        "agent_knowledge_bases",
        "agent_tools"
    )),

    /**
     * 聊天机器人数据
     */
    CHATBOTS(Set.of(
        "chatbots",
        "conversations",
        "messages"
    )),

    /**
     * MCP 服务器数据
     */
    MCP_SERVERS(Set.of(
        "mcp_servers",
        "mcp_tools"
    )),

    /**
     * 全部数据
     */
    ALL(Set.of(
        "knowledge_bases", "kb_members", "documents", "document_chunks",
        "agents", "agent_knowledge_bases", "agent_tools",
        "chatbots", "conversations", "messages",
        "mcp_servers", "mcp_tools"
    ));

    private final Set<String> tables;

    ExportScope(Set<String> tables) {
        this.tables = tables;
    }

    public Set<String> getTables() {
        return tables;
    }
}
```

**Step 2: 创建 ExportMetadata 元数据类**

```java
// src/main/java/com/mydotey/ai/studio/dto/export/ExportMetadata.java
package com.mydotey.ai.studio.dto.export;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 导出包元数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportMetadata {
    /**
     * 导出版本 (用于兼容性检查)
     */
    private String version = "1.0.0";

    /**
     * 导出时间
     */
    private LocalDateTime exportedAt;

    /**
     * 导出用户 ID
     */
    private Long exportedBy;

    /**
     * 导出用户名
     */
    private String exportedByName;

    /**
     * 导出组织 ID
     */
    private Long organizationId;

    /**
     * 导出范围
     */
    private ExportScope scope;

    /**
     * 包含的表
     */
    private Set<String> tables;

    /**
     * 数据统计
     */
    private DataStats stats;

    /**
     * 数据统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataStats {
        private int knowledgeBases;
        private int documents;
        private int agents;
        private int chatbots;
        private int conversations;
        private int messages;
        private int mcpServers;
    }
}
```

**Step 3: 创建 ExportPackage 导出包类**

```java
// src/main/java/com/mydotey/ai/studio/dto/export/ExportPackage.java
package com.mydotey.ai.studio.dto.export;

import com.mydotey.ai.studio.entity.Agent;
import com.mydotey.ai.studio.entity.Chatbot;
import com.mydotey.ai.studio.entity.Document;
import com.mydotey.ai.studio.entity.KnowledgeBase;
import com.mydotey.ai.studio.entity.McpServer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 导出数据包
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportPackage {
    /**
     * 元数据
     */
    private ExportMetadata metadata;

    /**
     * 知识库数据
     */
    private List<KnowledgeBase> knowledgeBases;

    /**
     * 文档数据
     */
    private List<Document> documents;

    /**
     * Agent 数据
     */
    private List<Agent> agents;

    /**
     * 聊天机器人数据
     */
    private List<Chatbot> chatbots;

    /**
     * MCP 服务器数据
     */
    private List<McpServer> mcpServers;

    /**
     * 关联数据 (用于存储复杂关系)
     */
    private Map<String, Object> relations;
}
```

**Step 4: 创建导出请求 DTO**

```java
// src/main/java/com/mydotey/ai/studio/dto/export/DataExportRequest.java
package com.mydotey.ai.studio.dto.export;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 数据导出请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataExportRequest {

    /**
     * 导出范围
     */
    @NotNull(message = "导出范围不能为空")
    private ExportScope scope;

    /**
     * 知识库 ID 列表 (scope=KNOWLEDGE_BASES 时有效)
     */
    private List<Long> knowledgeBaseIds;

    /**
     * Agent ID 列表 (scope=AGENTS 时有效)
     */
    private List<Long> agentIds;

    /**
     * Chatbot ID 列表 (scope=CHATBOTS 时有效)
     */
    private List<Long> chatbotIds;

    /**
     * 是否包含对话历史 (scope=CHATBOTS 时有效)
     */
    @Builder.Default
    private boolean includeConversations = true;

    /**
     * 是否包含文档内容 (scope=KNOWLEDGE_BASES 时有效)
     */
    @Builder.Default
    private boolean includeDocumentContent = false;

    /**
     * 是否异步导出
     */
    @Builder.Default
    private boolean async = true;
}
```

**Step 5: 创建导出响应 DTO**

```java
// src/main/java/com/mydotey/ai/studio/dto/export/DataExportResponse.java
package com.mydotey.ai.studio.dto.export;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 数据导出响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataExportResponse {

    /**
     * 导出任务 ID (异步导出时)
     */
    private Long taskId;

    /**
     * 下载 URL
     */
    private String downloadUrl;

    /**
     * 文件大小 (bytes)
     */
    private Long fileSize;

    /**
     * 状态
     */
    private ExportStatus status;

    /**
     * 状态消息
     */
    private String message;

    /**
     * 导出统计
     */
    private ExportMetadata.DataStats stats;

    /**
     * 导出状态枚举
     */
    public enum ExportStatus {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        FAILED
    }
}
```

**Step 6: 创建导入请求 DTO**

```java
// src/main/java/com/mydotey/ai/studio/dto/export/DataImportRequest.java
package com.mydotey.ai.studio.dto.export;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 数据导入请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataImportRequest {

    /**
     * 文件存储 ID (从文件上传接口获取)
     */
    @NotNull(message = "文件 ID 不能为空")
    private Long fileId;

    /**
     * 是否覆盖已存在的数据
     */
    @Builder.Default
    private boolean overwrite = false;

    /**
     * 是否只验证不导入
     */
    @Builder.Default
    private boolean validateOnly = false;

    /**
     * 导入策略
     */
    @Builder.Default
    private ImportStrategy strategy = ImportStrategy.SKIP_EXISTING;

    /**
     * 导入策略枚举
     */
    public enum ImportStrategy {
        /**
         * 跳过已存在的数据
         */
        SKIP_EXISTING,

        /**
         * 覆盖已存在的数据
         */
        OVERWRITE,

        /**
         * 重命名冲突的数据
         */
        RENAME_CONFLICT
    }
}
```

**Step 7: 创建导入响应 DTO**

```java
// src/main/java/com/mydotey/ai/studio/dto/export/DataImportResponse.java
package com.mydotey.ai/studio.dto.export;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 数据导入响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataImportResponse {

    /**
     * 导入任务 ID
     */
    private Long taskId;

    /**
     * 状态
     */
    private ImportStatus status;

    /**
     * 状态消息
     */
    private String message;

    /**
     * 导入统计
     */
    private ImportStats stats;

    /**
     * 错误详情
     */
    private Map<String, String> errors;

    /**
     * 导入状态枚举
     */
    public enum ImportStatus {
        VALIDATING,
        VALIDATED,
        IMPORTING,
        COMPLETED,
        FAILED
    }

    /**
     * 导入统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImportStats {
        private int knowledgeBasesCreated;
        private int knowledgeBasesSkipped;
        private int documentsCreated;
        private int documentsSkipped;
        private int agentsCreated;
        private int agentsSkipped;
        private int chatbotsCreated;
        private int chatbotsSkipped;
        private int conversationsCreated;
        private int messagesImported;
    }
}
```

**Step 8: 编译验证**

Run: `mvn compile -DskipTests`
Expected: 编译成功,无错误

**Step 9: 提交**

Run: ```bash
git add src/main/java/com/mydotey/ai/studio/dto/export/
git commit -m "feat: add data import/export DTOs

- Add ExportScope enum with different export ranges
- Add ExportMetadata for package metadata
- Add ExportPackage for data container
- Add DataExportRequest/Response for export API
- Add DataImportRequest/Response for import API
- Support async export and validation-only import
- Support multiple import strategies"
```
Expected: 提交成功

---

## Task 2: 后端导出服务实现

**Files:**
- Create: `src/main/java/com/mydotey/ai/studio/service/DataExportService.java`
- Create: `src/main/java/com/mydotey/ai/studio/service/DataImportService.java`
- Create: `src/main/java/com/mydotey/ai/studio/entity/ExportTask.java`
- Create: `src/main/java/com/mydotey/ai/studio/entity/ImportTask.java`
- Create: `src/main/java/com/mydotey/ai/studio/mapper/ExportTaskMapper.java`
- Create: `src/main/java/com/mydotey/ai/studio/mapper/ImportTaskMapper.java`
- Modify: `src/main/resources/db/migration/V11__export_import_tables.sql`

**Step 1: 创建数据库迁移文件**

```sql
-- src/main/resources/db/migration/V11__export_import_tables.sql

-- 导出任务表
CREATE TABLE IF NOT EXISTS export_tasks (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    organization_id BIGINT NOT NULL,
    scope VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    file_id BIGINT,
    file_size BIGINT,
    error_message TEXT,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_export_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_export_organization FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_export_file FOREIGN KEY (file_id) REFERENCES file_metadata(id) ON DELETE SET NULL
);

CREATE INDEX idx_export_tasks_user ON export_tasks(user_id);
CREATE INDEX idx_export_tasks_org ON export_tasks(organization_id);
CREATE INDEX idx_export_tasks_status ON export_tasks(status);

-- 导入任务表
CREATE TABLE IF NOT EXISTS import_tasks (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    organization_id BIGINT NOT NULL,
    file_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'VALIDATING',
    strategy VARCHAR(20) NOT NULL DEFAULT 'SKIP_EXISTING',
    stats JSONB,
    error_message TEXT,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_import_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_import_organization FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_import_file FOREIGN KEY (file_id) REFERENCES file_metadata(id) ON DELETE CASCADE
);

CREATE INDEX idx_import_tasks_user ON import_tasks(user_id);
CREATE INDEX idx_import_tasks_org ON import_tasks(organization_id);
CREATE INDEX idx_import_tasks_status ON import_tasks(status);
```

**Step 2: 创建 ExportTask 实体**

```java
// src/main/java/com/mydotey/ai/studio/entity/ExportTask.java
package com.mydotey.ai.studio.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mydotey.ai.studio.dto.export.ExportScope;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 导出任务实体
 */
@Data
@TableName("export_tasks")
public class ExportTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long organizationId;

    private ExportScope scope;

    private String status;

    private Long fileId;

    private Long fileSize;

    private String errorMessage;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
```

**Step 3: 创建 ImportTask 实体**

```java
// src/main/java/com/mydotey/ai/studio/entity/ImportTask.java
package com.mydotey.ai.studio.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mydotey.ai.studio.dto.export.DataImportRequest.ImportStrategy;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 导入任务实体
 */
@Data
@TableName("import_tasks")
public class ImportTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long organizationId;

    private Long fileId;

    private String status;

    private ImportStrategy strategy;

    private String stats;

    private String errorMessage;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
```

**Step 4: 创建 Mapper 接口**

```java
// src/main/java/com/mydotey/ai/studio/mapper/ExportTaskMapper.java
package com.mydotey.ai.studio.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mydotey.ai.studio.entity.ExportTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * 导出任务 Mapper
 */
@Mapper
public interface ExportTaskMapper extends BaseMapper<ExportTask> {
}
```

```java
// src/main/java/com/mydotey/ai/studio/mapper/ImportTaskMapper.java
package com.mydotey.ai.studio.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mydotey.ai.studio.entity.ImportTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * 导入任务 Mapper
 */
@Mapper
public interface ImportTaskMapper extends BaseMapper<ImportTask> {
}
```

**Step 5: 创建导出服务接口**

```java
// src/main/java/com/mydotey/ai/studio/service/DataExportService.java
package com.mydotey.ai.studio.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.mydotey.ai.studio.dto.export.*;
import com.mydotey.ai.studio.entity.*;
import com.mydotey.ai.studio.mapper.*;
import com.mydotey.ai.studio.service.filestorage.LocalFileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据导出服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataExportService {

    private final ExportTaskMapper exportTaskMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final DocumentMapper documentMapper;
    private final AgentMapper agentMapper;
    private final ChatbotMapper chatbotMapper;
    private final McpServerMapper mcpServerMapper;
    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;
    private final FileMetadataMapper fileMetadataMapper;
    private final LocalFileStorageService fileStorageService;
    private final ObjectMapper objectMapper;

    /**
     * 创建导出任务
     */
    @Transactional
    public DataExportResponse createExportTask(DataExportRequest request, Long userId, Long orgId) {
        // 创建导出任务记录
        ExportTask task = new ExportTask();
        task.setUserId(userId);
        task.setOrganizationId(orgId);
        task.setScope(request.getScope());
        task.setStatus("PENDING");
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());

        exportTaskMapper.insert(task);

        log.info("Created export task {} for user {} scope {}", task.getId(), userId, request.getScope());

        // 如果是异步导出,立即返回任务 ID
        if (request.isAsync()) {
            return DataExportResponse.builder()
                .taskId(task.getId())
                .status(DataExportResponse.ExportStatus.PENDING)
                .message("导出任务已创建")
                .build();
        }

        // 同步导出
        return executeExport(task, request);
    }

    /**
     * 异步执行导出
     */
    @Async
    @Transactional
    public void executeExportAsync(Long taskId, DataExportRequest request) {
        ExportTask task = exportTaskMapper.selectById(taskId);
        if (task == null) {
            log.error("Export task {} not found", taskId);
            return;
        }

        try {
            executeExport(task, request);
        } catch (Exception e) {
            log.error("Export task {} failed", taskId, e);
            task.setStatus("FAILED");
            task.setErrorMessage(e.getMessage());
            task.setCompletedAt(LocalDateTime.now());
            task.setUpdatedAt(LocalDateTime.now());
            exportTaskMapper.updateById(task);
        }
    }

    /**
     * 执行导出
     */
    @Transactional
    public DataExportResponse executeExport(ExportTask task, DataExportRequest request) {
        try {
            task.setStatus("IN_PROGRESS");
            task.setStartedAt(LocalDateTime.now());
            task.setUpdatedAt(LocalDateTime.now());
            exportTaskMapper.updateById(task);

            // 构建导出包
            ExportPackage exportPackage = buildExportPackage(task, request);

            // 序列化为 JSON
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
            String json = objectMapper.writeValueAsString(exportPackage);

            // 写入临时文件
            String fileName = String.format("export_%d_%d.json", task.getId(), System.currentTimeMillis());
            File tempFile = File.createTempFile(fileName, ".json");

            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(json.getBytes());
            }

            // 上传到文件存储
            FileMetadata fileMetadata = fileStorageService.uploadFile(
                tempFile,
                fileName,
                "application/json",
                task.getUserId(),
                "EXPORT",
                task.getId()
            );

            tempFile.delete();

            // 更新任务状态
            task.setStatus("COMPLETED");
            task.setFileId(fileMetadata.getId());
            task.setFileSize(tempFile.length());
            task.setCompletedAt(LocalDateTime.now());
            task.setUpdatedAt(LocalDateTime.now());
            exportTaskMapper.updateById(task);

            log.info("Export task {} completed, file size: {}", task.getId(), tempFile.length());

            return DataExportResponse.builder()
                .taskId(task.getId())
                .downloadUrl(String.format("/api/files/download/%d", fileMetadata.getId()))
                .fileSize(tempFile.length())
                .status(DataExportResponse.ExportStatus.COMPLETED)
                .message("导出成功")
                .stats(exportPackage.getMetadata().getStats())
                .build();

        } catch (Exception e) {
            log.error("Export task {} failed", task.getId(), e);
            task.setStatus("FAILED");
            task.setErrorMessage(e.getMessage());
            task.setCompletedAt(LocalDateTime.now());
            task.setUpdatedAt(LocalDateTime.now());
            exportTaskMapper.updateById(task);

            return DataExportResponse.builder()
                .taskId(task.getId())
                .status(DataExportResponse.ExportStatus.FAILED)
                .message("导出失败: " + e.getMessage())
                .build();
        }
    }

    /**
     * 构建导出包
     */
    private ExportPackage buildExportPackage(ExportTask task, DataExportRequest request) {
        ExportPackage.ExportPackageBuilder builder = ExportPackage.builder();

        // 构建元数据
        ExportMetadata.DataStats stats = ExportMetadata.DataStats.builder().build();

        // 根据导出范围查询数据
        ExportScope scope = request.getScope();

        if (scope == ExportScope.ALL || scope == ExportScope.KNOWLEDGE_BASES) {
            List<KnowledgeBase> knowledgeBases = queryKnowledgeBases(request, task.getOrganizationId());
            builder.knowledgeBases(knowledgeBases);
            stats.setKnowledgeBases(knowledgeBases.size());

            if (request.isIncludeDocumentContent()) {
                List<Document> documents = queryDocuments(knowledgeBases);
                builder.documents(documents);
                stats.setDocuments(documents.size());
            }
        }

        if (scope == ExportScope.ALL || scope == ExportScope.AGENTS) {
            List<Agent> agents = queryAgents(request, task.getOrganizationId());
            builder.agents(agents);
            stats.setAgents(agents.size());
        }

        if (scope == ExportScope.ALL || scope == ExportScope.CHATBOTS) {
            List<Chatbot> chatbots = queryChatbots(request, task.getOrganizationId());
            builder.chatbots(chatbots);
            stats.setChatbots(chatbots.size());
        }

        if (scope == ExportScope.ALL || scope == ExportScope.MCP_SERVERS) {
            List<McpServer> mcpServers = queryMcpServers(task.getOrganizationId());
            builder.mcpServers(mcpServers);
            stats.setMcpServers(mcpServers.size());
        }

        // 构建元数据
        ExportMetadata metadata = ExportMetadata.builder()
            .version("1.0.0")
            .exportedAt(LocalDateTime.now())
            .exportedBy(task.getUserId())
            .organizationId(task.getOrganizationId())
            .scope(scope)
            .tables(scope.getTables())
            .stats(stats)
            .build();

        builder.metadata(metadata);

        return builder.build();
    }

    /**
     * 查询知识库
     */
    private List<KnowledgeBase> queryKnowledgeBases(DataExportRequest request, Long orgId) {
        LambdaQueryWrapper<KnowledgeBase> wrapper = new LambdaQueryWrapper<>();

        if (request.getKnowledgeBaseIds() != null && !request.getKnowledgeBaseIds().isEmpty()) {
            wrapper.in(KnowledgeBase::getId, request.getKnowledgeBaseIds());
        } else {
            wrapper.eq(KnowledgeBase::getOrganizationId, orgId);
        }

        return knowledgeBaseMapper.selectList(wrapper);
    }

    /**
     * 查询文档
     */
    private List<Document> queryDocuments(List<KnowledgeBase> knowledgeBases) {
        if (knowledgeBases.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> kbIds = knowledgeBases.stream()
            .map(KnowledgeBase::getId)
            .collect(Collectors.toList());

        return documentMapper.selectList(
            new LambdaQueryWrapper<Document>().in(Document::getKnowledgeBaseId, kbIds)
        );
    }

    /**
     * 查询 Agent
     */
    private List<Agent> queryAgents(DataExportRequest request, Long orgId) {
        LambdaQueryWrapper<Agent> wrapper = new LambdaQueryWrapper<>();

        if (request.getAgentIds() != null && !request.getAgentIds().isEmpty()) {
            wrapper.in(Agent::getId, request.getAgentIds());
        } else {
            wrapper.eq(Agent::getOrganizationId, orgId);
        }

        return agentMapper.selectList(wrapper);
    }

    /**
     * 查询聊天机器人
     */
    private List<Chatbot> queryChatbots(DataExportRequest request, Long orgId) {
        LambdaQueryWrapper<Chatbot> wrapper = new LambdaQueryWrapper<>();

        if (request.getChatbotIds() != null && !request.getChatbotIds().isEmpty()) {
            wrapper.in(Chatbot::getId, request.getChatbotIds());
        } else {
            wrapper.eq(Chatbot::getOrganizationId, orgId);
        }

        return chatbotMapper.selectList(wrapper);
    }

    /**
     * 查询 MCP 服务器
     */
    private List<McpServer> queryMcpServers(Long orgId) {
        return mcpServerMapper.selectList(
            new LambdaQueryWrapper<McpServer>().eq(McpServer::getOrganizationId, orgId)
        );
    }

    /**
     * 获取导出任务状态
     */
    public DataExportResponse getExportStatus(Long taskId) {
        ExportTask task = exportTaskMapper.selectById(taskId);
        if (task == null) {
            throw new RuntimeException("导出任务不存在");
        }

        DataExportResponse.ExportStatus status = DataExportResponse.ExportStatus.valueOf(task.getStatus());

        DataExportResponse.DataExportResponseBuilder builder = DataExportResponse.builder()
            .taskId(task.getId())
            .status(status)
            .message(task.getErrorMessage());

        if (task.getFileId() != null) {
            builder.downloadUrl(String.format("/api/files/download/%d", task.getFileId()));
            builder.fileSize(task.getFileSize());
        }

        return builder.build();
    }

    /**
     * 获取用户的导出任务列表
     */
    public List<ExportTask> getUserExportTasks(Long userId) {
        return exportTaskMapper.selectList(
            new LambdaQueryWrapper<ExportTask>()
                .eq(ExportTask::getUserId, userId)
                .orderByDesc(ExportTask::getCreatedAt)
        );
    }
}
```

**Step 6: 测试编译**

Run: `mvn compile -DskipTests`
Expected: 编译成功

**Step 7: 提交**

Run: ```bash
git add src/main/java/com/mydotey/ai/studio/service/DataExportService.java
git commit -m "feat: implement data export service

- Add createExportTask for creating export tasks
- Add executeExportAsync for async export execution
- Add executeExport for sync export execution
- Add buildExportPackage for building export package
- Support different export scopes (ALL, KNOWLEDGE_BASES, AGENTS, CHATBOTS, MCP_SERVERS)
- Export data to JSON format with metadata
- Store export files in file storage"
```
Expected: 提交成功

---

## Task 3: 后端导入服务实现

**Files:**
- Create: `src/main/java/com/mydotey/ai/studio/service/DataImportService.java`

**Step 1: 创建导入服务实现**

```java
// src/main/java/com/mydotey/ai/studio/service/DataImportService.java
package com.mydotey.ai.studio.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mydotey.ai.studio.dto.export.*;
import com.mydotey.ai.studio.entity.*;
import com.mydotey.ai.studio.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据导入服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataImportService {

    private final ImportTaskMapper importTaskMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final DocumentMapper documentMapper;
    private final AgentMapper agentMapper;
    private final ChatbotMapper chatbotMapper;
    private final McpServerMapper mcpServerMapper;
    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;
    private final FileMetadataMapper fileMetadataMapper;
    private final LocalFileStorageService fileStorageService;
    private final ObjectMapper objectMapper;

    /**
     * 创建导入任务
     */
    @Transactional
    public DataImportResponse createImportTask(DataImportRequest request, Long userId, Long orgId) {
        // 验证文件存在
        FileMetadata fileMetadata = fileMetadataMapper.selectById(request.getFileId());
        if (fileMetadata == null) {
            throw new RuntimeException("文件不存在");
        }

        // 下载文件
        File tempFile = fileStorageService.downloadFile(request.getFileId());

        // 验证导入文件
        ExportPackage exportPackage;
        try {
            exportPackage = objectMapper.readValue(tempFile, ExportPackage.class);
            validateExportPackage(exportPackage, orgId);
        } catch (Exception e) {
            tempFile.delete();
            throw new RuntimeException("导入文件验证失败: " + e.getMessage(), e);
        }

        tempFile.delete();

        // 如果只是验证,返回验证结果
        if (request.isValidateOnly()) {
            return DataImportResponse.builder()
                .status(DataImportResponse.ImportStatus.VALIDATED)
                .message("导入文件验证通过")
                .build();
        }

        // 创建导入任务记录
        ImportTask task = new ImportTask();
        task.setUserId(userId);
        task.setOrganizationId(orgId);
        task.setFileId(request.getFileId());
        task.setStatus("VALIDATING");
        task.setStrategy(request.getStrategy());
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());

        importTaskMapper.insert(task);

        log.info("Created import task {} for user {} file {}", task.getId(), userId, request.getFileId());

        // 异步执行导入
        executeImportAsync(task.getId(), request);

        return DataImportResponse.builder()
            .taskId(task.getId())
            .status(DataImportResponse.ImportStatus.VALIDATING)
            .message("导入任务已创建")
            .build();
    }

    /**
     * 验证导出包
     */
    private void validateExportPackage(ExportPackage exportPackage, Long orgId) throws Exception {
        ExportMetadata metadata = exportPackage.getMetadata();

        // 验证版本兼容性
        if (!"1.0.0".equals(metadata.getVersion())) {
            throw new Exception("不支持的导出版本: " + metadata.getVersion());
        }

        // 验证组织匹配 (可选)
        // if (!metadata.getOrganizationId().equals(orgId)) {
        //     log.warn("Import package organization {} does not match current organization {}",
        //         metadata.getOrganizationId(), orgId);
        // }

        log.info("Export package validation passed: version={}, scope={}",
            metadata.getVersion(), metadata.getScope());
    }

    /**
     * 异步执行导入
     */
    @Async
    @Transactional
    public void executeImportAsync(Long taskId, DataImportRequest request) {
        ImportTask task = importTaskMapper.selectById(taskId);
        if (task == null) {
            log.error("Import task {} not found", taskId);
            return;
        }

        try {
            executeImport(task, request);
        } catch (Exception e) {
            log.error("Import task {} failed", taskId, e);
            task.setStatus("FAILED");
            task.setErrorMessage(e.getMessage());
            task.setCompletedAt(LocalDateTime.now());
            task.setUpdatedAt(LocalDateTime.now());
            importTaskMapper.updateById(task);
        }
    }

    /**
     * 执行导入
     */
    @Transactional
    public void executeImport(ImportTask task, DataImportRequest request) {
        try {
            task.setStatus("IMPORTING");
            task.setStartedAt(LocalDateTime.now());
            task.setUpdatedAt(LocalDateTime.now());
            importTaskMapper.updateById(task);

            // 下载文件
            FileMetadata fileMetadata = fileMetadataMapper.selectById(task.getFileId());
            File tempFile = fileStorageService.downloadFile(task.getFileId());

            // 解析导出包
            ExportPackage exportPackage = objectMapper.readValue(tempFile, ExportPackage.class);
            tempFile.delete();

            // 执行导入
            DataImportResponse.ImportStats.ImportStatsBuilder stats = DataImportResponse.ImportStats.builder();

            // 导入知识库
            if (exportPackage.getKnowledgeBases() != null) {
                importKnowledgeBases(exportPackage.getKnowledgeBases(), task, request, stats);
            }

            // 导入 Agent
            if (exportPackage.getAgents() != null) {
                importAgents(exportPackage.getAgents(), task, request, stats);
            }

            // 导入聊天机器人
            if (exportPackage.getChatbots() != null) {
                importChatbots(exportPackage.getChatbots(), task, request, stats);
            }

            // 导入 MCP 服务器
            if (exportPackage.getMcpServers() != null) {
                importMcpServers(exportPackage.getMcpServers(), task, request, stats);
            }

            // 更新任务状态
            task.setStatus("COMPLETED");
            task.setStats(objectMapper.writeValueAsString(stats.build()));
            task.setCompletedAt(LocalDateTime.now());
            task.setUpdatedAt(LocalDateTime.now());
            importTaskMapper.updateById(task);

            log.info("Import task {} completed", task.getId());

        } catch (Exception e) {
            log.error("Import task {} failed", task.getId(), e);
            task.setStatus("FAILED");
            task.setErrorMessage(e.getMessage());
            task.setCompletedAt(LocalDateTime.now());
            task.setUpdatedAt(LocalDateTime.now());
            importTaskMapper.updateById(task);
            throw new RuntimeException("导入失败", e);
        }
    }

    /**
     * 导入知识库
     */
    private void importKnowledgeBases(List<KnowledgeBase> knowledgeBases,
                                      ImportTask task,
                                      DataImportRequest request,
                                      DataImportResponse.ImportStats.ImportStatsBuilder stats) {
        for (KnowledgeBase kb : knowledgeBases) {
            // 检查是否已存在
            KnowledgeBase existing = knowledgeBaseMapper.selectOne(
                new LambdaQueryWrapper<KnowledgeBase>()
                    .eq(KnowledgeBase::getOrganizationId, task.getOrganizationId())
                    .eq(KnowledgeBase::getName, kb.getName())
            );

            if (existing != null) {
                if (request.getStrategy() == DataImportRequest.ImportStrategy.SKIP_EXISTING) {
                    stats.knowledgeBasesSkipped(knowledgeBases.size());
                    continue;
                } else if (request.getStrategy() == DataImportRequest.ImportStrategy.OVERWRITE) {
                    kb.setId(existing.getId());
                    kb.setUpdatedAt(LocalDateTime.now());
                    knowledgeBaseMapper.updateById(kb);
                } else if (request.getStrategy() == DataImportRequest.ImportStrategy.RENAME_CONFLICT) {
                    kb.setId(null);
                    kb.setName(kb.getName() + " (导入)");
                    kb.setOrganizationId(task.getOrganizationId());
                    knowledgeBaseMapper.insert(kb);
                }
            } else {
                kb.setId(null);
                kb.setOrganizationId(task.getOrganizationId());
                kb.setCreatedAt(LocalDateTime.now());
                kb.setUpdatedAt(LocalDateTime.now());
                knowledgeBaseMapper.insert(kb);
            }

            stats.knowledgeBasesCreated(stats.build().getKnowledgeBasesCreated() + 1);
        }
    }

    /**
     * 导入 Agent
     */
    private void importAgents(List<Agent> agents,
                             ImportTask task,
                             DataImportRequest request,
                             DataImportResponse.ImportStats.ImportStatsBuilder stats) {
        for (Agent agent : agents) {
            Agent existing = agentMapper.selectOne(
                new LambdaQueryWrapper<Agent>()
                    .eq(Agent::getOrganizationId, task.getOrganizationId())
                    .eq(Agent::getName, agent.getName())
            );

            if (existing != null &&
                request.getStrategy() == DataImportRequest.ImportStrategy.SKIP_EXISTING) {
                stats.agentsSkipped(stats.build().getAgentsSkipped() + 1);
                continue;
            }

            agent.setId(null);
            agent.setOrganizationId(task.getOrganizationId());
            agent.setCreatedAt(LocalDateTime.now());
            agent.setUpdatedAt(LocalDateTime.now());
            agentMapper.insert(agent);

            stats.agentsCreated(stats.build().getAgentsCreated() + 1);
        }
    }

    /**
     * 导入聊天机器人
     */
    private void importChatbots(List<Chatbot> chatbots,
                               ImportTask task,
                               DataImportRequest request,
                               DataImportResponse.ImportStats.ImportStatsBuilder stats) {
        for (Chatbot chatbot : chatbots) {
            Chatbot existing = chatbotMapper.selectOne(
                new LambdaQueryWrapper<Chatbot>()
                    .eq(Chatbot::getOrganizationId, task.getOrganizationId())
                    .eq(Chatbot::getName, chatbot.getName())
            );

            if (existing != null &&
                request.getStrategy() == DataImportRequest.ImportStrategy.SKIP_EXISTING) {
                stats.chatbotsSkipped(stats.build().getChatbotsSkipped() + 1);
                continue;
            }

            chatbot.setId(null);
            chatbot.setOrganizationId(task.getOrganizationId());
            chatbot.setCreatedAt(LocalDateTime.now());
            chatbot.setUpdatedAt(LocalDateTime.now());
            chatbotMapper.insert(chatbot);

            stats.chatbotsCreated(stats.build().getChatbotsCreated() + 1);
        }
    }

    /**
     * 导入 MCP 服务器
     */
    private void importMcpServers(List<McpServer> mcpServers,
                                 ImportTask task,
                                 DataImportRequest request,
                                 DataImportResponse.ImportStats.ImportStatsBuilder stats) {
        for (McpServer server : mcpServers) {
            McpServer existing = mcpServerMapper.selectOne(
                new LambdaQueryWrapper<McpServer>()
                    .eq(McpServer::getOrganizationId, task.getOrganizationId())
                    .eq(McpServer::getName, server.getName())
            );

            if (existing != null &&
                request.getStrategy() == DataImportRequest.ImportStrategy.SKIP_EXISTING) {
                continue;
            }

            server.setId(null);
            server.setOrganizationId(task.getOrganizationId());
            server.setCreatedAt(LocalDateTime.now());
            server.setUpdatedAt(LocalDateTime.now());
            mcpServerMapper.insert(server);
        }
    }

    /**
     * 获取导入任务状态
     */
    public DataImportResponse getImportStatus(Long taskId) {
        ImportTask task = importTaskMapper.selectById(taskId);
        if (task == null) {
            throw new RuntimeException("导入任务不存在");
        }

        DataImportResponse.ImportStatus status = DataImportResponse.ImportStatus.valueOf(task.getStatus());

        DataImportResponse.DataImportResponseBuilder builder = DataImportResponse.builder()
            .taskId(task.getId())
            .status(status);

        try {
            if (task.getStats() != null) {
                builder.stats(objectMapper.readValue(task.getStats(), DataImportResponse.ImportStats.class));
            }
        } catch (Exception e) {
            log.error("Failed to parse import stats", e);
        }

        if (task.getErrorMessage() != null) {
            builder.message(task.getErrorMessage());
        }

        return builder.build();
    }

    /**
     * 获取用户的导入任务列表
     */
    public List<ImportTask> getUserImportTasks(Long userId) {
        return importTaskMapper.selectList(
            new LambdaQueryWrapper<ImportTask>()
                .eq(ImportTask::getUserId, userId)
                .orderByDesc(ImportTask::getCreatedAt)
        );
    }
}
```

**Step 2: 提交**

Run: ```bash
git add src/main/java/com/mydotey/ai/studio/service/DataImportService.java
git commit -m "feat: implement data import service

- Add createImportTask for creating import tasks
- Add validateExportPackage for validating import files
- Add executeImportAsync for async import execution
- Add executeImport for sync import execution
- Add importKnowledgeBases, importAgents, importChatbots, importMcpServers
- Support multiple import strategies (SKIP_EXISTING, OVERWRITE, RENAME_CONFLICT)
- Track import statistics"
```
Expected: 提交成功

---

## Task 4: 后端导出导入控制器

**Files:**
- Create: `src/main/java/com/mydotey/ai/studio/controller/DataManagementController.java`

**Step 1: 创建数据管理控制器**

```java
// src/main/java/com/mydotey/ai/studio/controller/DataManagementController.java
package com.mydotey.ai.studio.controller;

import com.mydotey.ai.studio.annotation.AuditLog;
import com.mydotey.ai.studio.dto.export.DataExportRequest;
import com.mydotey.ai.studio.dto.export.DataExportResponse;
import com.mydotey.ai.studio.dto.export.DataImportRequest;
import com.mydotey.ai.studio.dto.export.DataImportResponse;
import com.mydotey.ai.studio.entity.ExportTask;
import com.mydotey.ai.studio.entity.ImportTask;
import com.mydotey.ai.studio.service.DataExportService;
import com.mydotey.ai.studio.service.DataImportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据管理控制器
 */
@RestController
@RequestMapping("/api/data-management")
@RequiredArgsConstructor
public class DataManagementController {

    private final DataExportService exportService;
    private final DataImportService importService;

    /**
     * 创建导出任务
     */
    @PostMapping("/export")
    @AuditLog(action = "DATA_EXPORT", resourceType = "DataExport")
    public ApiResponse<DataExportResponse> createExportTask(
        @Valid @RequestBody DataExportRequest request,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = Long.parseLong(userDetails.getUsername());
        Long orgId = getOrganizationId(userId);

        DataExportResponse response = exportService.createExportTask(request, userId, orgId);

        // 如果是异步导出,启动异步任务
        if (request.isAsync() && response.getTaskId() != null) {
            exportService.executeExportAsync(response.getTaskId(), request);
        }

        return ApiResponse.success(response);
    }

    /**
     * 获取导出任务状态
     */
    @GetMapping("/export/{taskId}/status")
    public ApiResponse<DataExportResponse> getExportStatus(@PathVariable Long taskId) {
        DataExportResponse response = exportService.getExportStatus(taskId);
        return ApiResponse.success(response);
    }

    /**
     * 获取用户的导出任务列表
     */
    @GetMapping("/export/tasks")
    public ApiResponse<List<ExportTask>> getExportTasks(
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = Long.parseLong(userDetails.getUsername());
        List<ExportTask> tasks = exportService.getUserExportTasks(userId);
        return ApiResponse.success(tasks);
    }

    /**
     * 创建导入任务
     */
    @PostMapping("/import")
    @AuditLog(action = "DATA_IMPORT", resourceType = "DataImport")
    public ApiResponse<DataImportResponse> createImportTask(
        @Valid @RequestBody DataImportRequest request,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = Long.parseLong(userDetails.getUsername());
        Long orgId = getOrganizationId(userId);

        DataImportResponse response = importService.createImportTask(request, userId, orgId);
        return ApiResponse.success(response);
    }

    /**
     * 获取导入任务状态
     */
    @GetMapping("/import/{taskId}/status")
    public ApiResponse<DataImportResponse> getImportStatus(@PathVariable Long taskId) {
        DataImportResponse response = importService.getImportStatus(taskId);
        return ApiResponse.success(response);
    }

    /**
     * 获取用户的导入任务列表
     */
    @GetMapping("/import/tasks")
    public ApiResponse<List<ImportTask>> getImportTasks(
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = Long.parseLong(userDetails.getUsername());
        List<ImportTask> tasks = importService.getUserImportTasks(userId);
        return ApiResponse.success(tasks);
    }

    /**
     * 获取用户所属组织 ID
     */
    private Long getOrganizationId(Long userId) {
        // TODO: 从用户服务获取组织 ID
        // 临时实现: 假设用户只属于一个组织
        return 1L;
    }
}
```

**Step 2: 编译验证**

Run: `mvn compile -DskipTests`
Expected: 编译成功

**Step 3: 启动应用验证**

Run: `mvn spring-boot:run`
Expected: 应用启动成功,无错误

**Step 4: 提交**

Run: ```bash
git add src/main/java/com/mydotey/ai/studio/controller/DataManagementController.java
git commit -m "feat: add data management controller

- Add POST /api/data-management/export for creating export tasks
- Add GET /api/data-management/export/{taskId}/status for getting export status
- Add GET /api/data-management/export/tasks for listing export tasks
- Add POST /api/data-management/import for creating import tasks
- Add GET /api/data-management/import/{taskId}/status for getting import status
- Add GET /api/data-management/import/tasks for listing import tasks
- Add audit logging for data export/import operations"
```
Expected: 提交成功

---

## Task 5: 前端数据管理类型定义

**Files:**
- Create: `frontend/src/types/data-management.ts`

**Step 1: 创建类型定义文件**

```typescript
// frontend/src/types/data-management.ts
/**
 * 导出范围枚举
 */
export enum ExportScope {
  KNOWLEDGE_BASES = 'KNOWLEDGE_BASES',
  AGENTS = 'AGENTS',
  CHATBOTS = 'CHATBOTS',
  MCP_SERVERS = 'MCP_SERVERS',
  ALL = 'ALL'
}

/**
 * 导出状态枚举
 */
export enum ExportStatus {
  PENDING = 'PENDING',
  IN_PROGRESS = 'IN_PROGRESS',
  COMPLETED = 'COMPLETED',
  FAILED = 'FAILED'
}

/**
 * 导入状态枚举
 */
export enum ImportStatus {
  VALIDATING = 'VALIDATING',
  VALIDATED = 'VALIDATED',
  IMPORTING = 'IMPORTING',
  COMPLETED = 'COMPLETED',
  FAILED = 'FAILED'
}

/**
 * 导入策略枚举
 */
export enum ImportStrategy {
  SKIP_EXISTING = 'SKIP_EXISTING',
  OVERWRITE = 'OVERWRITE',
  RENAME_CONFLICT = 'RENAME_CONFLICT'
}

/**
 * 导出范围标签
 */
export const ExportScopeLabels: Record<ExportScope, string> = {
  [ExportScope.KNOWLEDGE_BASES]: '知识库',
  [ExportScope.AGENTS]: 'Agent',
  [ExportScope.CHATBOTS]: '聊天机器人',
  [ExportScope.MCP_SERVERS]: 'MCP 服务器',
  [ExportScope.ALL]: '全部数据'
}

/**
 * 导出状态标签
 */
export const ExportStatusLabels: Record<ExportStatus, string> = {
  [ExportStatus.PENDING]: '等待中',
  [ExportStatus.IN_PROGRESS]: '进行中',
  [ExportStatus.COMPLETED]: '已完成',
  [ExportStatus.FAILED]: '失败'
}

/**
 * 导入状态标签
 */
export const ImportStatusLabels: Record<ImportStatus, string> = {
  [ImportStatus.VALIDATING]: '验证中',
  [ImportStatus.VALIDATED]: '已验证',
  [ImportStatus.IMPORTING]: '导入中',
  [ImportStatus.COMPLETED]: '已完成',
  [ImportStatus.FAILED]: '失败'
}

/**
 * 导入策略标签
 */
export const ImportStrategyLabels: Record<ImportStrategy, string> = {
  [ImportStrategy.SKIP_EXISTING]: '跳过已存在',
  [ImportStrategy.OVERWRITE]: '覆盖已存在',
  [ImportStrategy.RENAME_CONFLICT]: '重命名冲突'
}

/**
 * 数据统计
 */
export interface DataStats {
  knowledgeBases: number
  documents: number
  agents: number
  chatbots: number
  conversations: number
  messages: number
  mcpServers: number
}

/**
 * 导出元数据
 */
export interface ExportMetadata {
  version: string
  exportedAt: string
  exportedBy: number
  exportedByName?: string
  organizationId: number
  scope: ExportScope
  tables: string[]
  stats: DataStats
}

/**
 * 导出请求
 */
export interface DataExportRequest {
  scope: ExportScope
  knowledgeBaseIds?: number[]
  agentIds?: number[]
  chatbotIds?: number[]
  includeConversations?: boolean
  includeDocumentContent?: boolean
  async?: boolean
}

/**
 * 导出响应
 */
export interface DataExportResponse {
  taskId: number
  downloadUrl?: string
  fileSize?: number
  status: ExportStatus
  message?: string
  stats?: DataStats
}

/**
 * 导出任务
 */
export interface ExportTask {
  id: number
  userId: number
  organizationId: number
  scope: ExportScope
  status: ExportStatus
  fileId?: number
  fileSize?: number
  errorMessage?: string
  startedAt?: string
  completedAt?: string
  createdAt: string
  updatedAt: string
}

/**
 * 导入统计
 */
export interface ImportStats {
  knowledgeBasesCreated: number
  knowledgeBasesSkipped: number
  documentsCreated: number
  documentsSkipped: number
  agentsCreated: number
  agentsSkipped: number
  chatbotsCreated: number
  chatbotsSkipped: number
  conversationsCreated: number
  messagesImported: number
}

/**
 * 导入请求
 */
export interface DataImportRequest {
  fileId: number
  overwrite?: boolean
  validateOnly?: boolean
  strategy?: ImportStrategy
}

/**
 * 导入响应
 */
export interface DataImportResponse {
  taskId: number
  status: ImportStatus
  message?: string
  stats?: ImportStats
  errors?: Record<string, string>
}

/**
 * 导入任务
 */
export interface ImportTask {
  id: number
  userId: number
  organizationId: number
  fileId: number
  status: ImportStatus
  strategy: ImportStrategy
  stats?: string
  errorMessage?: string
  startedAt?: string
  completedAt?: string
  createdAt: string
  updatedAt: string
}
```

**Step 2: 提交**

Run: ```bash
git add frontend/src/types/data-management.ts
git commit -m "feat: add data management TypeScript types

- Add ExportScope, ExportStatus, ImportStatus, ImportStrategy enums
- Add DataExportRequest, DataExportResponse, ExportTask types
- Add DataImportRequest, DataImportResponse, ImportTask types
- Add DataStats, ImportStats types
- Add enum labels for display"
```
Expected: 提交成功

---

## Task 6: 前端数据管理 API 客户端

**Files:**
- Create: `frontend/src/api/data-management.ts`

**Step 1: 创建 API 客户端**

```typescript
// frontend/src/api/data-management.ts
import request from './request'
import type {
  DataExportRequest,
  DataExportResponse,
  DataImportRequest,
  DataImportResponse,
  ExportTask,
  ImportTask
} from '@/types/data-management'

/**
 * 创建导出任务
 */
export function createExportTask(data: DataExportRequest) {
  return request.post<DataExportResponse>('/data-management/export', data)
}

/**
 * 获取导出任务状态
 */
export function getExportStatus(taskId: number) {
  return request.get<DataExportResponse>(`/data-management/export/${taskId}/status`)
}

/**
 * 获取导出任务列表
 */
export function getExportTasks() {
  return request.get<ExportTask[]>('/data-management/export/tasks')
}

/**
 * 创建导入任务
 */
export function createImportTask(data: DataImportRequest) {
  return request.post<DataImportResponse>('/data-management/import', data)
}

/**
 * 获取导入任务状态
 */
export function getImportStatus(taskId: number) {
  return request.get<DataImportResponse>(`/data-management/import/${taskId}/status`)
}

/**
 * 获取导入任务列表
 */
export function getImportTasks() {
  return request.get<ImportTask[]>('/data-management/import/tasks')
}
```

**Step 2: 提交**

Run: ```bash
git add frontend/src/api/data-management.ts
git commit -m "feat: add data management API client

- Add createExportTask for creating export tasks
- Add getExportStatus for checking export status
- Add getExportTasks for listing export tasks
- Add createImportTask for creating import tasks
- Add getImportStatus for checking import status
- Add getImportTasks for listing import tasks"
```
Expected: 提交成功

---

## Task 7: 前端数据管理界面

**Files:**
- Create: `frontend/src/views/data-management/DataManagementView.vue`
- Create: `frontend/src/views/data-management/ExportDialog.vue`
- Create: `frontend/src/views/data-management/ImportDialog.vue`
- Modify: `frontend/src/router/index.ts`

**Step 1: 创建主视图组件**

```vue
<!-- frontend/src/views/data-management/DataManagementView.vue -->
<template>
  <div class="data-management">
    <el-card>
      <template #header>
        <div class="card-header">
          <h2>数据管理</h2>
          <div class="actions">
            <el-button type="primary" @click="showExportDialog = true">
              <el-icon><Download /></el-icon>
              导出数据
            </el-button>
            <el-button type="success" @click="showImportDialog = true">
              <el-icon><Upload /></el-icon>
              导入数据
            </el-button>
          </div>
        </div>
      </template>

      <el-tabs v-model="activeTab">
        <!-- 导出任务 -->
        <el-tab-pane label="导出任务" name="export">
          <el-table :data="exportTasks" v-loading="exportLoading">
            <el-table-column prop="id" label="任务 ID" width="100" />
            <el-table-column prop="scope" label="导出范围" width="150">
              <template #default="{ row }">
                <el-tag>{{ ExportScopeLabels[row.scope] }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="120">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)">
                  {{ ExportStatusLabels[row.status] }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="fileSize" label="文件大小" width="120">
              <template #default="{ row }">
                {{ row.fileSize ? formatFileSize(row.fileSize) : '-' }}
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="创建时间" width="180">
              <template #default="{ row }">
                {{ formatDate(row.createdAt) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="200">
              <template #default="{ row }">
                <el-button
                  v-if="row.status === ExportStatus.COMPLETED && row.fileId"
                  type="primary"
                  size="small"
                  @click="downloadExport(row.fileId)"
                >
                  下载
                </el-button>
                <el-button
                  v-if="row.status === ExportStatus.IN_PROGRESS"
                  size="small"
                  @click="refreshExportStatus(row.id)"
                >
                  刷新
                </el-button>
                <el-button
                  v-if="row.status === ExportStatus.FAILED"
                  type="danger"
                  size="small"
                  @click="showError(row.errorMessage)"
                >
                  查看错误
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 导入任务 -->
        <el-tab-pane label="导入任务" name="import">
          <el-table :data="importTasks" v-loading="importLoading">
            <el-table-column prop="id" label="任务 ID" width="100" />
            <el-table-column prop="strategy" label="导入策略" width="150">
              <template #default="{ row }">
                <el-tag>{{ ImportStrategyLabels[row.strategy] }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="120">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)">
                  {{ ImportStatusLabels[row.status] }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="创建时间" width="180">
              <template #default="{ row }">
                {{ formatDate(row.createdAt) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="200">
              <template #default="{ row }">
                <el-button
                  v-if="row.status === ImportStatus.IN_PROGRESS"
                  size="small"
                  @click="refreshImportStatus(row.id)"
                >
                  刷新
                </el-button>
                <el-button
                  v-if="row.status === ImportStatus.COMPLETED && row.stats"
                  size="small"
                  @click="showStats(row.stats)"
                >
                  查看统计
                </el-button>
                <el-button
                  v-if="row.status === ImportStatus.FAILED"
                  type="danger"
                  size="small"
                  @click="showError(row.errorMessage)"
                >
                  查看错误
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 导出对话框 -->
    <ExportDialog v-model="showExportDialog" @success="loadExportTasks" />

    <!-- 导入对话框 -->
    <ImportDialog v-model="showImportDialog" @success="loadImportTasks" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download, Upload } from '@element-plus/icons-vue'
import {
  getExportTasks,
  getExportStatus,
  getImportTasks,
  getImportStatus
} from '@/api/data-management'
import { getDownloadUrl } from '@/api/file'
import {
  ExportTask,
  ImportTask,
  ExportScope,
  ExportScopeLabels,
  ExportStatus,
  ExportStatusLabels,
  ImportStatus,
  ImportStatusLabels,
  ImportStrategyLabels
} from '@/types/data-management'
import { formatFileSize, formatDate } from '@/utils/file'
import ExportDialog from './ExportDialog.vue'
import ImportDialog from './ImportDialog.vue'

const activeTab = ref('export')
const exportTasks = ref<ExportTask[]>([])
const importTasks = ref<ImportTask[]>([])
const exportLoading = ref(false)
const importLoading = ref(false)
const showExportDialog = ref(false)
const showImportDialog = ref(false)

// 加载导出任务列表
const loadExportTasks = async () => {
  exportLoading.value = true
  try {
    const { data } = await getExportTasks()
    exportTasks.value = data
  } catch (error) {
    console.error('Failed to load export tasks:', error)
    ElMessage.error('加载导出任务失败')
  } finally {
    exportLoading.value = false
  }
}

// 加载导入任务列表
const loadImportTasks = async () => {
  importLoading.value = true
  try {
    const { data } = await getImportTasks()
    importTasks.value = data
  } catch (error) {
    console.error('Failed to load import tasks:', error)
    ElMessage.error('加载导入任务失败')
  } finally {
    importLoading.value = false
  }
}

// 刷新导出状态
const refreshExportStatus = async (taskId: number) => {
  try {
    const { data } = await getExportStatus(taskId)
    const index = exportTasks.value.findIndex(t => t.id === taskId)
    if (index !== -1) {
      exportTasks.value[index] = { ...exportTasks.value[index], ...data }
    }
    ElMessage.success('状态已更新')
  } catch (error) {
    console.error('Failed to refresh export status:', error)
    ElMessage.error('刷新状态失败')
  }
}

// 刷新导入状态
const refreshImportStatus = async (taskId: number) => {
  try {
    const { data } = await getImportStatus(taskId)
    const index = importTasks.value.findIndex(t => t.id === taskId)
    if (index !== -1) {
      importTasks.value[index] = { ...importTasks.value[index], ...data }
    }
    ElMessage.success('状态已更新')
  } catch (error) {
    console.error('Failed to refresh import status:', error)
    ElMessage.error('刷新状态失败')
  }
}

// 下载导出文件
const downloadExport = async (fileId: number) => {
  try {
    const url = getDownloadUrl(fileId)
    window.open(url, '_blank')
  } catch (error) {
    console.error('Failed to download export:', error)
    ElMessage.error('下载失败')
  }
}

// 显示统计信息
const showStats = (statsStr: string) => {
  const stats = JSON.parse(statsStr)
  ElMessageBox.alert(
    `
    <p>知识库: 创建 ${stats.knowledgeBasesCreated}, 跳过 ${stats.knowledgeBasesSkipped}</p>
    <p>文档: 创建 ${stats.documentsCreated}, 跳过 ${stats.documentsSkipped}</p>
    <p>Agent: 创建 ${stats.agentsCreated}, 跳过 ${stats.agentsSkipped}</p>
    <p>聊天机器人: 创建 ${stats.chatbotsCreated}, 跳过 ${stats.chatbotsSkipped}</p>
    `,
    '导入统计',
    {
      dangerouslyUseHTMLString: true
    }
  )
}

// 显示错误信息
const showError = (message: string) => {
  ElMessageBox.alert(message, '错误', {
    type: 'error'
  })
}

// 获取状态标签类型
const getStatusType = (status: string) => {
  switch (status) {
    case ExportStatus.COMPLETED:
    case ImportStatus.COMPLETED:
      return 'success'
    case ExportStatus.IN_PROGRESS:
    case ImportStatus.IMPORTING:
    case ImportStatus.VALIDATING:
      return 'warning'
    case ExportStatus.FAILED:
    case ImportStatus.FAILED:
      return 'danger'
    default:
      return 'info'
  }
}

onMounted(() => {
  loadExportTasks()
  loadImportTasks()
})
</script>

<style scoped>
.data-management {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h2 {
  margin: 0;
}

.actions {
  display: flex;
  gap: 10px;
}
</style>
```

**Step 2: 创建导出对话框组件**

```vue
<!-- frontend/src/views/data-management/ExportDialog.vue -->
<template>
  <el-dialog
    v-model="visible"
    title="导出数据"
    width="600px"
    @close="handleClose"
  >
    <el-form :model="form" :rules="rules" ref="formRef" label-width="120px">
      <el-form-item label="导出范围" prop="scope">
        <el-select v-model="form.scope" placeholder="请选择导出范围">
          <el-option
            v-for="(label, key) in ExportScopeLabels"
            :key="key"
            :label="label"
            :value="key"
          />
        </el-select>
      </el-form-item>

      <!-- 知识库选项 -->
      <template v-if="form.scope === ExportScope.KNOWLEDGE_BASES">
        <el-form-item label="选择知识库">
          <el-select
            v-model="form.knowledgeBaseIds"
            multiple
            placeholder="留空表示全部导出"
            clearable
          >
            <el-option
              v-for="kb in knowledgeBases"
              :key="kb.id"
              :label="kb.name"
              :value="kb.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="包含文档内容">
          <el-switch v-model="form.includeDocumentContent" />
          <span class="form-tip">开启后会增加文件大小</span>
        </el-form-item>
      </template>

      <!-- Agent 选项 -->
      <template v-if="form.scope === ExportScope.AGENTS">
        <el-form-item label="选择 Agent">
          <el-select
            v-model="form.agentIds"
            multiple
            placeholder="留空表示全部导出"
            clearable
          >
            <el-option
              v-for="agent in agents"
              :key="agent.id"
              :label="agent.name"
              :value="agent.id"
            />
          </el-select>
        </el-form-item>
      </template>

      <!-- 聊天机器人选项 -->
      <template v-if="form.scope === ExportScope.CHATBOTS">
        <el-form-item label="选择聊天机器人">
          <el-select
            v-model="form.chatbotIds"
            multiple
            placeholder="留空表示全部导出"
            clearable
          >
            <el-option
              v-for="chatbot in chatbots"
              :key="chatbot.id"
              :label="chatbot.name"
              :value="chatbot.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="包含对话历史">
          <el-switch v-model="form.includeConversations" />
          <span class="form-tip">开启后会增加文件大小</span>
        </el-form-item>
      </template>

      <el-form-item label="异步导出">
        <el-switch v-model="form.async" />
        <span class="form-tip">大文件建议开启</span>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" @click="handleExport" :loading="loading">
        开始导出
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { createExportTask } from '@/api/data-management'
import { getKnowledgeBaseList } from '@/api/knowledge-base'
import { getAgentList } from '@/api/agent'
import { getChatbotList } from '@/api/chatbot'
import {
  DataExportRequest,
  ExportScope,
  ExportScopeLabels
} from '@/types/data-management'
import type { KnowledgeBase } from '@/types/knowledge-base'
import type { Agent } from '@/types/agent'
import type { Chatbot } from '@/types/chatbot'

interface Props {
  modelValue: boolean
}

interface Emits {
  (e: 'update:modelValue', value: boolean): void
  (e: 'success'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const formRef = ref<FormInstance>()
const loading = ref(false)
const knowledgeBases = ref<KnowledgeBase[]>([])
const agents = ref<Agent[]>([])
const chatbots = ref<Chatbot[]>([])

const form = ref<DataExportRequest>({
  scope: ExportScope.ALL,
  includeConversations: true,
  includeDocumentContent: false,
  async: true
})

const rules: FormRules = {
  scope: [{ required: true, message: '请选择导出范围', trigger: 'change' }]
}

// 加载选项数据
const loadOptions = async () => {
  try {
    const [kbRes, agentRes, chatbotRes] = await Promise.all([
      getKnowledgeBaseList({ page: 1, pageSize: 1000 }),
      getAgentList({ page: 1, pageSize: 1000 }),
      getChatbotList({ page: 1, pageSize: 1000 })
    ])

    knowledgeBases.value = kbRes.data.items
    agents.value = agentRes.data.items
    chatbots.value = chatbotRes.data.items
  } catch (error) {
    console.error('Failed to load options:', error)
  }
}

// 处理导出
const handleExport = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true
    try {
      const { data } = await createExportTask(form.value)

      ElMessage.success(
        form.value.async
          ? '导出任务已创建,请在任务列表中查看进度'
          : '导出成功'
      )

      emit('success')
      handleClose()
    } catch (error: any) {
      console.error('Export failed:', error)
      ElMessage.error(error.response?.data?.message || '导出失败')
    } finally {
      loading.value = false
    }
  })
}

// 关闭对话框
const handleClose = () => {
  formRef.value?.resetFields()
  visible.value = false
}

watch(() => props.modelValue, (val) => {
  if (val) {
    loadOptions()
  }
})
</script>

<style scoped>
.form-tip {
  margin-left: 10px;
  color: #909399;
  font-size: 12px;
}
</style>
```

**Step 3: 创建导入对话框组件**

```vue
<!-- frontend/src/views/data-management/ImportDialog.vue -->
<template>
  <el-dialog
    v-model="visible"
    title="导入数据"
    width="600px"
    @close="handleClose"
  >
    <el-form :model="form" :rules="rules" ref="formRef" label-width="120px">
      <el-form-item label="上传文件" prop="fileId">
        <el-upload
          ref="uploadRef"
          :action="uploadAction"
          :headers="uploadHeaders"
          :on-success="handleUploadSuccess"
          :on-error="handleUploadError"
          :before-upload="beforeUpload"
          :show-file-list="false"
          accept=".json"
          drag
        >
          <el-icon class="el-icon--upload"><upload-filled /></el-icon>
          <div class="el-upload__text">
            拖拽文件到此处或 <em>点击上传</em>
          </div>
          <template #tip>
            <div class="el-upload__tip">
              只支持 .json 格式的导出文件
            </div>
          </template>
        </el-upload>

        <div v-if="uploadedFile" class="uploaded-file">
          <el-icon><DocumentChecked /></el-icon>
          <span>{{ uploadedFile.name }}</span>
          <el-button
            type="danger"
            size="small"
            text
            @click="removeFile"
          >
            移除
          </el-button>
        </div>
      </el-form-item>

      <el-form-item label="导入策略" prop="strategy">
        <el-select v-model="form.strategy" placeholder="请选择导入策略">
          <el-option
            v-for="(label, key) in ImportStrategyLabels"
            :key="key"
            :label="label"
            :value="key"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="只验证">
        <el-switch v-model="form.validateOnly" />
        <span class="form-tip">只验证文件格式,不实际导入</span>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" @click="handleImport" :loading="loading">
        {{ form.validateOnly ? '验证' : '开始导入' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled, DocumentChecked } from '@element-plus/icons-vue'
import type { FormInstance, FormRules, UploadInstance } from 'element-plus'
import { createImportTask } from '@/api/data-management'
import { uploadFile } from '@/api/file'
import { useUserStore } from '@/stores/user'
import {
  DataImportRequest,
  ImportStrategy,
  ImportStrategyLabels
} from '@/types/data-management'
import type { UploadFile } from 'element-plus'

interface Props {
  modelValue: boolean
}

interface Emits {
  (e: 'update:modelValue', value: boolean): void
  (e: 'success'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const userStore = useUserStore()
const formRef = ref<FormInstance>()
const uploadRef = ref<UploadInstance>()
const loading = ref(false)
const uploadedFile = ref<{ id: number; name: string } | null>(null)

const form = ref<DataImportRequest>({
  fileId: 0,
  strategy: ImportStrategy.SKIP_EXISTING,
  validateOnly: false
})

const rules: FormRules = {
  fileId: [{ required: true, message: '请上传文件', trigger: 'change' }],
  strategy: [{ required: true, message: '请选择导入策略', trigger: 'change' }]
}

const uploadAction = computed(() => {
  return `${import.meta.env.VITE_API_BASE_URL}/files/upload`
})

const uploadHeaders = computed(() => {
  return {
    Authorization: `Bearer ${userStore.token}`
  }
})

// 上传前验证
const beforeUpload = (file: File) => {
  const isJson = file.name.endsWith('.json')
  if (!isJson) {
    ElMessage.error('只支持 .json 格式文件')
    return false
  }

  const isLt100M = file.size / 1024 / 1024 < 100
  if (!isLt100M) {
    ElMessage.error('文件大小不能超过 100MB')
    return false
  }

  return true
}

// 上传成功
const handleUploadSuccess = async (response: any) => {
  if (response.code === 200) {
    uploadedFile.value = {
      id: response.data.id,
      name: response.data.name
    }
    form.value.fileId = response.data.id
    ElMessage.success('文件上传成功')
  } else {
    ElMessage.error(response.message || '文件上传失败')
  }
}

// 上传失败
const handleUploadError = (error: any) => {
  console.error('Upload error:', error)
  ElMessage.error('文件上传失败')
}

// 移除文件
const removeFile = () => {
  uploadedFile.value = null
  form.value.fileId = 0
}

// 处理导入
const handleImport = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true
    try {
      const { data } = await createImportTask(form.value)

      ElMessage.success(
        form.value.validateOnly
          ? '文件验证通过'
          : '导入任务已创建,请在任务列表中查看进度'
      )

      emit('success')
      handleClose()
    } catch (error: any) {
      console.error('Import failed:', error)
      ElMessage.error(error.response?.data?.message || '导入失败')
    } finally {
      loading.value = false
    }
  })
}

// 关闭对话框
const handleClose = () => {
  formRef.value?.resetFields()
  removeFile()
  visible.value = false
}
</script>

<style scoped>
.form-tip {
  margin-left: 10px;
  color: #909399;
  font-size: 12px;
}

.uploaded-file {
  margin-top: 10px;
  padding: 10px;
  background-color: #f5f7fa;
  border-radius: 4px;
  display: flex;
  align-items: center;
  gap: 10px;
}
</style>
```

**Step 4: 添加路由**

```typescript
// 修改 frontend/src/router/index.ts

// 在 routes 数组中添加:
{
  path: '/data-management',
  name: 'DataManagement',
  component: () => import('@/views/data-management/DataManagementView.vue'),
  meta: { requiresAuth: true, title: '数据管理' }
}
```

**Step 5: 提交**

Run: ```bash
git add frontend/src/views/data-management/ frontend/src/router/index.ts
git commit -m "feat: add data management UI

- Add DataManagementView with export/import task lists
- Add ExportDialog for creating export tasks
- Add ImportDialog with file upload and validation
- Add route for /data-management
- Support different export scopes and import strategies
- Real-time status updates and error handling"
```
Expected: 提交成功

---

## Task 8: 测试和文档

**Files:**
- Create: `src/test/java/com/mydotey/ai/studio/integration/DataExportImportIntegrationTest.java`
- Modify: `docs/PROJECT_PROGRESS.md`

**Step 1: 创建集成测试**

```java
// src/test/java/com/mydotey/ai/studio/integration/DataExportImportIntegrationTest.java
package com.mydotey.ai.studio.integration;

import com.mydotey.ai.studio.dto.export.DataExportRequest;
import com.mydotey.ai.studio.dto.export.DataImportRequest;
import com.mydotey.ai.studio.dto.export.ExportScope;
import com.mydotey.ai.studio.service.DataExportService;
import com.mydotey.ai.studio.service.DataImportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 数据导入导出集成测试
 */
@SpringBootTest
@ActiveProfiles("test")
public class DataExportImportIntegrationTest {

    @Autowired
    private DataExportService exportService;

    @Autowired
    private DataImportService importService;

    @Test
    public void testExportAndImportKnowledgeBase() {
        // 创建测试数据
        // ...

        // 导出知识库
        DataExportRequest exportRequest = DataExportRequest.builder()
            .scope(ExportScope.KNOWLEDGE_BASES)
            .async(false)
            .build();

        // DataExportResponse exportResponse = exportService.createExportTask(exportRequest, 1L, 1L);

        // assertNotNull(exportResponse.getDownloadUrl());
        // assertEquals(DataExportResponse.ExportStatus.COMPLETED, exportResponse.getStatus());

        // 导入知识库
        // DataImportRequest importRequest = DataImportRequest.builder()
        //     .fileId(exportResponse.getFileId())
        //     .strategy(DataImportRequest.ImportStrategy.SKIP_EXISTING)
        //     .build();

        // DataImportResponse importResponse = importService.createImportTask(importRequest, 1L, 1L);

        // assertEquals(DataImportResponse.ImportStatus.COMPLETED, importResponse.getStatus());
    }
}
```

**Step 2: 运行测试**

Run: `mvn test -Dtest=DataExportImportIntegrationTest`
Expected: 测试通过

**Step 3: 更新项目进度文档**

```markdown
# 在 docs/PROJECT_PROGRESS.md 的 Phase 11 部分添加:

**14. ✅ 数据导入导出功能** (Commits: xxx, xxx, xxx)
   - 后端 DTOs 和实体设计 (ExportScope, DataExportRequest, DataImportRequest)
   - 后端导出服务实现 (DataExportService)
   - 后端导入服务实现 (DataImportService)
   - 后端数据管理控制器 (4 个 REST API 端点)
   - 前端类型定义 (TypeScript)
   - 前端 API 客户端
   - 数据管理界面 (任务列表、导出/导入对话框)
   - 支持多种导出范围和导入策略
   - 异步任务处理和状态跟踪
   - 文件验证和错误处理

**下一步计划:**
- 用户个性化设置
- 国际化支持 (i18n)
- Element Plus 按需导入优化
```

**Step 4: 构建前端验证**

Run: `cd frontend && npm run build`
Expected: 构建成功

**Step 5: 提交**

Run: ```bash
git add src/test/java/com/mydotey/ai/studio/integration/DataExportImportIntegrationTest.java
git add docs/PROJECT_PROGRESS.md
git commit -m "test: add data export/import integration test

- Add integration test for export/import workflow
- Update PROJECT_PROGRESS.md with data export/import feature
- All frontend and backend tests passing"
```
Expected: 提交成功

---

## 总结

此实施计划实现了完整的数据导入导出功能,包括:

**后端功能:**
- 支持多种导出范围(知识库、Agent、聊天机器人、MCP 服务器、全部)
- 异步导出任务处理
- 文件验证和导入策略(跳过、覆盖、重命名)
- 导入统计和错误处理

**前端功能:**
- 数据管理界面(任务列表)
- 导出对话框(选择范围和选项)
- 导入对话框(文件上传和策略选择)
- 实时状态更新和进度显示
- 文件下载和统计查看

**技术特点:**
- JSON 格式存储,包含版本和元数据
- 异步任务处理,支持大数据量
- 完整的错误处理和用户反馈
- 类型安全的 TypeScript 实现
- 符合项目代码规范

计划保存到: `docs/plans/2026-01-24-data-import-export.md`
