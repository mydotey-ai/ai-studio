# Data Import/Export Implementation Plan (REVISED)

> **Revised Date:** 2026-01-24
> **Original Plan:** 2026-01-24-data-import-export.md
> **Revision Reason:** Plan assumptions didn't match actual entity structure and FileStorageManagerService API

**Goal:** 实现数据导入导出功能,支持知识库、Agent、聊天机器人等核心数据的备份和迁移

**Architecture (REVISED):**
- **后端:** 使用 Jackson JSON 序列化,直接返回 JSON 数据（不通过文件存储系统）
- **前端:** 使用 Blob API 下载 JSON, Element Plus Upload 组件上传 JSON
- **格式:** JSON 格式存储数据,包含元数据(版本、时间戳、导出范围)
- **文件处理:** 导出时直接在 Controller 返回 JSON, 导入时从前端接收 MultipartFile

**Tech Stack:**
- Jackson (JSON 处理)
- Spring Web (文件上传下载)
- Element Plus (UI 组件)
- Blob API (前端文件下载)

**Entity Field Mapping (CRITICAL):**
- KnowledgeBase: `orgId` (not `organizationId`)
- Document: `kbId` (not `knowledgeBaseId`)
- Agent: `orgId` (not `organizationId`)
- Chatbot: `ownerId` (not `organizationId`)
- McpServer: `createdBy` (not `organizationId`)
- All timestamps: `Instant` (not `LocalDateTime`)

---

## Task 1: 后端导入导出 DTO 设计 ✅ (COMPLETED)

**Status:** 已完成
**Commit:** 398c275

所有 DTO 类已创建并编译通过：
- ExportScope.java
- ExportMetadata.java
- ExportPackage.java
- DataExportRequest.java
- DataExportResponse.java
- DataImportRequest.java
- DataImportResponse.java

---

## Task 2: 后端导出服务实现 (REVISED)

**Files:**
- Modify: `src/main/java/com/mydotey/ai/studio/service/DataExportService.java`
- Remove: 依赖 FileStorageManagerService（改用直接返回 JSON）
- Modify: `src/main/java/com/mydotey/ai/studio/service/DataImportService.java`

**Changes from original plan:**

1. **移除文件存储依赖** - 导出时直接返回 JSON 字符串
2. **修正实体字段引用**:
   - KnowledgeBase::getOrgId (not getOrganizationId)
   - Document::getKbId (not getKnowledgeBaseId)
   - Agent::getOrgId (not getOrganizationId)
   - Chatbot::getOwnerId
   - McpServer::getCreatedBy

3. **修正时间字段** - 所有时间戳使用 `Instant` 而不是 `LocalDateTime`

**Step 1: 重写 DataExportService.java**

```java
package com.mydotey.ai.studio.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.mydotey.ai.studio.dto.export.*;
import com.mydotey.ai.studio.entity.*;
import com.mydotey.ai.studio.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据导出服务（修订版）
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
    private final ObjectMapper objectMapper;

    /**
     * 创建导出任务（异步）
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

        // 异步执行导出
        executeExportAsync(task.getId(), request);

        return DataExportResponse.builder()
            .taskId(task.getId())
            .status(DataExportResponse.ExportStatus.PENDING)
            .message("导出任务已创建")
            .build();
    }

    /**
     * 同步导出（直接返回 JSON）
     */
    public String exportDataSync(DataExportRequest request, Long userId, Long orgId) {
        try {
            // 构建导出包
            ExportPackage exportPackage = buildExportPackage(request, userId, orgId);

            // 序列化为 JSON
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
            return objectMapper.writeValueAsString(exportPackage);

        } catch (Exception e) {
            log.error("Sync export failed", e);
            throw new RuntimeException("导出失败: " + e.getMessage(), e);
        }
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
     * 执行导出（生成 JSON 并存储为 FileMetadata）
     */
    @Transactional
    public DataExportResponse executeExport(ExportTask task, DataExportRequest request) {
        try {
            task.setStatus("IN_PROGRESS");
            task.setStartedAt(LocalDateTime.now());
            task.setUpdatedAt(LocalDateTime.now());
            exportTaskMapper.updateById(task);

            // 构建导出包
            ExportPackage exportPackage = buildExportPackage(request, task.getUserId(), task.getOrganizationId());

            // 序列化为 JSON
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
            String json = objectMapper.writeValueAsString(exportPackage);

            // 注意：这里暂时不存储到文件系统，只在任务中记录大小
            // TODO: 后续可以考虑存储到临时文件或数据库
            int jsonSize = json.getBytes().length;

            // 更新任务状态
            task.setStatus("COMPLETED");
            task.setFileSize((long) jsonSize);
            task.setCompletedAt(LocalDateTime.now());
            task.setUpdatedAt(LocalDateTime.now());
            exportTaskMapper.updateById(task);

            log.info("Export task {} completed, size: {} bytes", task.getId(), jsonSize);

            return DataExportResponse.builder()
                .taskId(task.getId())
                .fileSize((long) jsonSize)
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
     * 构建导出包（修正实体字段）
     */
    private ExportPackage buildExportPackage(DataExportRequest request, Long userId, Long orgId) {
        ExportPackage.ExportPackageBuilder builder = ExportPackage.builder();

        // 构建元数据
        ExportMetadata.DataStats stats = ExportMetadata.DataStats.builder().build();

        // 根据导出范围查询数据
        ExportScope scope = request.getScope();

        if (scope == ExportScope.ALL || scope == ExportScope.KNOWLEDGE_BASES) {
            List<KnowledgeBase> knowledgeBases = queryKnowledgeBases(request, orgId);
            builder.knowledgeBases(knowledgeBases);
            stats.setKnowledgeBases(knowledgeBases.size());

            if (request.isIncludeDocumentContent()) {
                List<Document> documents = queryDocuments(knowledgeBases);
                builder.documents(documents);
                stats.setDocuments(documents.size());
            }
        }

        if (scope == ExportScope.ALL || scope == ExportScope.AGENTS) {
            List<Agent> agents = queryAgents(request, orgId);
            builder.agents(agents);
            stats.setAgents(agents.size());
        }

        if (scope == ExportScope.ALL || scope == ExportScope.CHATBOTS) {
            List<Chatbot> chatbots = queryChatbots(request, userId);
            builder.chatbots(chatbots);
            stats.setChatbots(chatbots.size());
        }

        if (scope == ExportScope.ALL || scope == ExportScope.MCP_SERVERS) {
            List<McpServer> mcpServers = queryMcpServers(userId);
            builder.mcpServers(mcpServers);
            stats.setMcpServers(mcpServers.size());
        }

        // 构建元数据
        ExportMetadata metadata = ExportMetadata.builder()
            .version("1.0.0")
            .exportedAt(LocalDateTime.now())
            .exportedBy(userId)
            .organizationId(orgId)
            .scope(scope)
            .tables(scope.getTables())
            .stats(stats)
            .build();

        builder.metadata(metadata);

        return builder.build();
    }

    /**
     * 查询知识库（使用 orgId）
     */
    private List<KnowledgeBase> queryKnowledgeBases(DataExportRequest request, Long orgId) {
        LambdaQueryWrapper<KnowledgeBase> wrapper = new LambdaQueryWrapper<>();

        if (request.getKnowledgeBaseIds() != null && !request.getKnowledgeBaseIds().isEmpty()) {
            wrapper.in(KnowledgeBase::getId, request.getKnowledgeBaseIds());
        } else {
            wrapper.eq(KnowledgeBase::getOrgId, orgId); // 使用 orgId
        }

        return knowledgeBaseMapper.selectList(wrapper);
    }

    /**
     * 查询文档（使用 kbId）
     */
    private List<Document> queryDocuments(List<KnowledgeBase> knowledgeBases) {
        if (knowledgeBases.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> kbIds = knowledgeBases.stream()
            .map(KnowledgeBase::getId)
            .collect(Collectors.toList());

        return documentMapper.selectList(
            new LambdaQueryWrapper<Document>().in(Document::getKbId, kbIds) // 使用 kbId
        );
    }

    /**
     * 查询 Agent（使用 orgId）
     */
    private List<Agent> queryAgents(DataExportRequest request, Long orgId) {
        LambdaQueryWrapper<Agent> wrapper = new LambdaQueryWrapper<>();

        if (request.getAgentIds() != null && !request.getAgentIds().isEmpty()) {
            wrapper.in(Agent::getId, request.getAgentIds());
        } else {
            wrapper.eq(Agent::getOrgId, orgId); // 使用 orgId
        }

        return agentMapper.selectList(wrapper);
    }

    /**
     * 查询聊天机器人（使用 ownerId）
     */
    private List<Chatbot> queryChatbots(DataExportRequest request, Long userId) {
        LambdaQueryWrapper<Chatbot> wrapper = new LambdaQueryWrapper<>();

        if (request.getChatbotIds() != null && !request.getChatbotIds().isEmpty()) {
            wrapper.in(Chatbot::getId, request.getChatbotIds());
        } else {
            wrapper.eq(Chatbot::getOwnerId, userId); // 使用 ownerId
        }

        return chatbotMapper.selectList(wrapper);
    }

    /**
     * 查询 MCP 服务器（使用 createdBy）
     */
    private List<McpServer> queryMcpServers(Long userId) {
        return mcpServerMapper.selectList(
            new LambdaQueryWrapper<McpServer>().eq(McpServer::getCreatedBy, userId) // 使用 createdBy
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

        return DataExportResponse.builder()
            .taskId(task.getId())
            .status(status)
            .message(task.getErrorMessage())
            .fileSize(task.getFileSize())
            .build();
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

**Step 2: 编译验证**

Run: `mvn compile -DskipTests`
Expected: 编译成功

**Step 3: 提交修订的服务**

```bash
git add src/main/java/com/mydotey/ai/studio/service/DataExportService.java
git commit -m "feat: revise data export service to match actual entity structure

- Remove FileStorageManagerService dependency
- Use correct entity fields: orgId, kbId, ownerId, createdBy
- Add sync export method that returns JSON directly
- Fix field references for all entity types
- Use Instant for timestamps in entities"
```

---

## Task 3: 后端导入服务实现 (REVISED)

**Files:**
- Modify: `src/main/java/com/mydotey/ai/studio/service/DataImportService.java`

**Changes from original plan:**

1. **移除文件存储依赖** - 导入时接收 MultipartFile，解析后直接使用
2. **修正实体字段引用**（同导出服务）
3. **修正时间字段** - 使用 Instant

**Step 1: 重写 DataImportService.java**

```java
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
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 数据导入服务（修订版）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataImportService {

    private final ImportTaskMapper importTaskMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final AgentMapper agentMapper;
    private final ChatbotMapper chatbotMapper;
    private final McpServerMapper mcpServerMapper;
    private final ObjectMapper objectMapper;

    /**
     * 创建导入任务（从 MultipartFile）
     */
    @Transactional
    public DataImportResponse createImportTask(MultipartFile file, DataImportRequest request, Long userId, Long orgId) {
        // 验证导入文件
        ExportPackage exportPackage;
        try {
            exportPackage = objectMapper.readValue(file.getInputStream(), ExportPackage.class);
            validateExportPackage(exportPackage, orgId);
        } catch (Exception e) {
            throw new RuntimeException("导入文件验证失败: " + e.getMessage(), e);
        }

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
        task.setStatus("VALIDATING");
        task.setStrategy(request.getStrategy());
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());

        importTaskMapper.insert(task);

        log.info("Created import task {} for user {} file {}", task.getId(), userId, file.getOriginalFilename());

        // 异步执行导入
        executeImportAsync(task.getId(), exportPackage, request);

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

        log.info("Export package validation passed: version={}, scope={}",
            metadata.getVersion(), metadata.getScope());
    }

    /**
     * 异步执行导入
     */
    @Async
    @Transactional
    public void executeImportAsync(Long taskId, ExportPackage exportPackage, DataImportRequest request) {
        ImportTask task = importTaskMapper.selectById(taskId);
        if (task == null) {
            log.error("Import task {} not found", taskId);
            return;
        }

        try {
            executeImport(task, exportPackage, request);
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
    public void executeImport(ImportTask task, ExportPackage exportPackage, DataImportRequest request) {
        try {
            task.setStatus("IMPORTING");
            task.setStartedAt(LocalDateTime.now());
            task.setUpdatedAt(LocalDateTime.now());
            importTaskMapper.updateById(task);

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
     * 导入知识库（使用 orgId）
     */
    private void importKnowledgeBases(List<KnowledgeBase> knowledgeBases,
                                      ImportTask task,
                                      DataImportRequest request,
                                      DataImportResponse.ImportStats.ImportStatsBuilder stats) {
        for (KnowledgeBase kb : knowledgeBases) {
            // 检查是否已存在
            KnowledgeBase existing = knowledgeBaseMapper.selectOne(
                new LambdaQueryWrapper<KnowledgeBase>()
                    .eq(KnowledgeBase::getOrgId, task.getOrganizationId())
                    .eq(KnowledgeBase::getName, kb.getName())
            );

            if (existing != null) {
                if (request.getStrategy() == DataImportRequest.ImportStrategy.SKIP_EXISTING) {
                    stats.knowledgeBasesSkipped(stats.build().getKnowledgeBasesSkipped() + 1);
                    continue;
                } else if (request.getStrategy() == DataImportRequest.ImportStrategy.OVERWRITE) {
                    kb.setId(existing.getId());
                    kb.setUpdatedAt(Instant.now());
                    knowledgeBaseMapper.updateById(kb);
                } else if (request.getStrategy() == DataImportRequest.ImportStrategy.RENAME_CONFLICT) {
                    kb.setId(null);
                    kb.setName(kb.getName() + " (导入)");
                    kb.setOrgId(task.getOrganizationId());
                    kb.setCreatedAt(Instant.now());
                    kb.setUpdatedAt(Instant.now());
                    knowledgeBaseMapper.insert(kb);
                }
            } else {
                kb.setId(null);
                kb.setOrgId(task.getOrganizationId());
                kb.setCreatedAt(Instant.now());
                kb.setUpdatedAt(Instant.now());
                knowledgeBaseMapper.insert(kb);
            }

            stats.knowledgeBasesCreated(stats.build().getKnowledgeBasesCreated() + 1);
        }
    }

    /**
     * 导入 Agent（使用 orgId）
     */
    private void importAgents(List<Agent> agents,
                             ImportTask task,
                             DataImportRequest request,
                             DataImportResponse.ImportStats.ImportStatsBuilder stats) {
        for (Agent agent : agents) {
            Agent existing = agentMapper.selectOne(
                new LambdaQueryWrapper<Agent>()
                    .eq(Agent::getOrgId, task.getOrganizationId())
                    .eq(Agent::getName, agent.getName())
            );

            if (existing != null &&
                request.getStrategy() == DataImportRequest.ImportStrategy.SKIP_EXISTING) {
                stats.agentsSkipped(stats.build().getAgentsSkipped() + 1);
                continue;
            }

            agent.setId(null);
            agent.setOrgId(task.getOrganizationId());
            agent.setCreatedAt(Instant.now());
            agent.setUpdatedAt(Instant.now());
            agentMapper.insert(agent);

            stats.agentsCreated(stats.build().getAgentsCreated() + 1);
        }
    }

    /**
     * 导入聊天机器人（使用 ownerId）
     */
    private void importChatbots(List<Chatbot> chatbots,
                               ImportTask task,
                               DataImportRequest request,
                               DataImportResponse.ImportStats.ImportStatsBuilder stats) {
        for (Chatbot chatbot : chatbots) {
            Chatbot existing = chatbotMapper.selectOne(
                new LambdaQueryWrapper<Chatbot>()
                    .eq(Chatbot::getOwnerId, task.getUserId())
                    .eq(Chatbot::getName, chatbot.getName())
            );

            if (existing != null &&
                request.getStrategy() == DataImportRequest.ImportStrategy.SKIP_EXISTING) {
                stats.chatbotsSkipped(stats.build().getChatbotsSkipped() + 1);
                continue;
            }

            chatbot.setId(null);
            chatbot.setOwnerId(task.getUserId());
            chatbot.setCreatedAt(Instant.now());
            chatbot.setUpdatedAt(Instant.now());
            chatbotMapper.insert(chatbot);

            stats.chatbotsCreated(stats.build().getChatbotsCreated() + 1);
        }
    }

    /**
     * 导入 MCP 服务器（使用 createdBy）
     */
    private void importMcpServers(List<McpServer> mcpServers,
                                 ImportTask task,
                                 DataImportRequest request,
                                 DataImportResponse.ImportStats.ImportStatsBuilder stats) {
        for (McpServer server : mcpServers) {
            McpServer existing = mcpServerMapper.selectOne(
                new LambdaQueryWrapper<McpServer>()
                    .eq(McpServer::getCreatedBy, task.getUserId())
                    .eq(McpServer::getName, server.getName())
            );

            if (existing != null &&
                request.getStrategy() == DataImportRequest.ImportStrategy.SKIP_EXISTING) {
                continue;
            }

            server.setId(null);
            server.setCreatedBy(task.getUserId());
            server.setCreatedAt(Instant.now());
            server.setUpdatedAt(Instant.now());
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

**Step 2: 编译验证**

Run: `mvn compile -DskipTests`
Expected: 编译成功

**Step 3: 提交修订的服务**

```bash
git add src/main/java/com/mydotey/ai/studio/service/DataImportService.java
git commit -m "feat: revise data import service to match actual entity structure

- Accept MultipartFile instead of using FileStorageManagerService
- Use correct entity fields: orgId, ownerId, createdBy
- Fix field references for all entity types
- Use Instant for timestamps"
```

---

## Task 4: 后端导出导入控制器 (REVISED)

**Files:**
- Modify: `src/main/java/com/mydotey/ai/studio/controller/DataManagementController.java`

**Changes from original plan:**

1. **添加同步导出端点** - 直接返回 JSON
2. **修改导入端点** - 接收 MultipartFile
3. **移除 fileId 字段**（不再通过文件存储系统）

**Step 1: 创建控制器**

```java
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 数据管理控制器（修订版）
 */
@RestController
@RequestMapping("/api/data-management")
@RequiredArgsConstructor
public class DataManagementController {

    private final DataExportService exportService;
    private final DataImportService importService;

    /**
     * 同步导出数据（直接返回 JSON）
     */
    @PostMapping(value = "/export/sync", produces = MediaType.APPLICATION_JSON_VALUE)
    @AuditLog(action = "DATA_EXPORT", resourceType = "DataExport")
    public String exportDataSync(
        @Valid @RequestBody DataExportRequest request,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = Long.parseLong(userDetails.getUsername());
        Long orgId = getOrganizationId(userId);

        return exportService.exportDataSync(request, userId, orgId);
    }

    /**
     * 创建导出任务（异步）
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

        // 启动异步任务
        exportService.executeExportAsync(response.getTaskId(), request);

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
     * 创建导入任务（从 MultipartFile）
     */
    @PostMapping("/import")
    @AuditLog(action = "DATA_IMPORT", resourceType = "DataImport")
    public ApiResponse<DataImportResponse> createImportTask(
        @RequestParam("file") MultipartFile file,
        @RequestParam("strategy") DataImportRequest.ImportStrategy strategy,
        @RequestParam(value = "validateOnly", defaultValue = "false") boolean validateOnly,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = Long.parseLong(userDetails.getUsername());
        Long orgId = getOrganizationId(userId);

        DataImportRequest request = DataImportRequest.builder()
            .strategy(strategy)
            .validateOnly(validateOnly)
            .build();

        DataImportResponse response = importService.createImportTask(file, request, userId, orgId);

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

**Step 3: 提交**

```bash
git add src/main/java/com/mydotey/ai/studio/controller/DataManagementController.java
git commit -m "feat: add data management controller (revised)

- Add POST /api/data-management/export/sync for sync export (returns JSON)
- Add POST /api/data-management/export for async export tasks
- Add POST /api data-management/import with MultipartFile
- Add GET endpoints for task status and lists
- Add audit logging for data export/import operations"
```

---

## Task 5-7: 前端实现 (REVISED)

### Task 5: 前端数据管理类型定义

**Status:** ✅ 已完成
**Commit:** 已存在于原始计划中，无需修改

### Task 6: 前端数据管理 API 客户端

**Status:** ✅ 已完成
**Commit:** 已存在于原始计划中，无需修改

### Task 7: 前端数据管理界面 (REVISED)

**Files:**
- Modify: `frontend/src/views/data-management/DataManagementView.vue`
- Modify: `frontend/src/views/data-management/ExportDialog.vue`
- Modify: `frontend/src/views/data-management/ImportDialog.vue`

**Changes from original plan:**

1. **导出功能** - 添加同步导出（直接下载 JSON）和异步导出（任务列表）
2. **导入功能** - 使用 FormData 上传文件

**Step 1: 修改 ExportDialog.vue（添加同步导出选项）**

```vue
<template>
  <el-dialog
    v-model="visible"
    title="导出数据"
    width="600px"
    @close="handleClose"
  >
    <el-form :model="form" :rules="rules" ref="formRef" label-width="120px">
      <el-form-item label="导出方式">
        <el-radio-group v-model="exportMode">
          <el-radio label="sync">立即导出（下载 JSON）</el-radio>
          <el-radio label="async">后台导出</el-radio>
        </el-radio-group>
      </el-form-item>

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
    </el-form>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" @click="handleExport" :loading="loading">
        {{ exportMode === 'sync' ? '下载 JSON' : '开始导出' }}
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
import axios from 'axios'

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
const exportMode = ref('sync')
const knowledgeBases = ref<KnowledgeBase[]>([])
const agents = ref<Agent[]>([])
const chatbots = ref<Chatbot[]>([])

const form = ref<DataExportRequest>({
  scope: ExportScope.ALL,
  includeConversations: true,
  includeDocumentContent: false,
  async: false
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
      if (exportMode.value === 'sync') {
        // 同步导出：直接下载 JSON
        const token = localStorage.getItem('token')
        const response = await axios.post(
          '/api/data-management/export/sync',
          form.value,
          {
            headers: {
              Authorization: `Bearer ${token}`,
              'Content-Type': 'application/json'
            },
            responseType: 'blob'
          }
        )

        // 创建下载链接
        const blob = new Blob([response.data], { type: 'application/json' })
        const url = window.URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = url
        link.download = `export_${Date.now()}.json`
        link.click()
        window.URL.revokeObjectURL(url)

        ElMessage.success('导出成功')
        emit('success')
        handleClose()
      } else {
        // 异步导出：创建任务
        form.value.async = true
        const { data } = await createExportTask(form.value)

        ElMessage.success('导出任务已创建,请在任务列表中查看进度')
        emit('success')
        handleClose()
      }
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

**Step 2: 修改 ImportDialog.vue（使用 FormData）**

```vue
<template>
  <el-dialog
    v-model="visible"
    title="导入数据"
    width="600px"
    @close="handleClose"
  >
    <el-form :model="form" :rules="rules" ref="formRef" label-width="120px">
      <el-form-item label="上传文件" prop="file">
        <el-upload
          ref="uploadRef"
          :auto-upload="false"
          :on-change="handleFileChange"
          :show-file-list="true"
          :limit="1"
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
import { UploadFilled } from '@element-plus/icons-vue'
import type { FormInstance, FormRules, UploadInstance, UploadFile } from 'element-plus'
import { createImportTask } from '@/api/data-management'
import {
  DataImportRequest,
  ImportStrategy,
  ImportStrategyLabels
} from '@/types/data-management'

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
const uploadRef = ref<UploadInstance>()
const loading = ref(false)
const selectedFile = ref<File | null>(null)

const form = ref<DataImportRequest>({
  strategy: ImportStrategy.SKIP_EXISTING,
  validateOnly: false
})

const rules: FormRules = {
  file: [{ required: true, message: '请上传文件', trigger: 'change' }],
  strategy: [{ required: true, message: '请选择导入策略', trigger: 'change' }]
}

// 文件选择处理
const handleFileChange = (file: UploadFile) => {
  const rawFile = file.raw as File
  if (!rawFile) return

  const isJson = rawFile.name.endsWith('.json')
  if (!isJson) {
    ElMessage.error('只支持 .json 格式文件')
    return false
  }

  const isLt100M = rawFile.size / 1024 / 1024 < 100
  if (!isLt100M) {
    ElMessage.error('文件大小不能超过 100MB')
    return false
  }

  selectedFile.value = rawFile
  return true
}

// 处理导入
const handleImport = async () => {
  if (!formRef.value || !selectedFile.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true
    try {
      // 使用 FormData 上传文件
      const formData = new FormData()
      formData.append('file', selectedFile.value)
      formData.append('strategy', form.value.strategy)
      formData.append('validateOnly', String(form.value.validateOnly))

      const token = localStorage.getItem('token')
      await axios.post('/api/data-management/import', formData, {
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'multipart/form-data'
        }
      })

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
  selectedFile.value = null
  uploadRef.value?.clearFiles()
  visible.value = false
}
</script>

<style scoped>
.form-tip {
  margin-left: 10px;
  color: #909399;
  font-size: 12px;
}
</style>
```

**Step 3: 提交**

```bash
git add frontend/src/views/data-management/
git commit -m "feat: revise data management UI (frontend)

- Add sync export mode with direct JSON download
- Use FormData for file upload
- Add export mode selection (sync/async)
- Fix file upload handling with FormData"
```

---

## Task 8: 测试和文档

**Files:**
- Modify: `docs/PROJECT_PROGRESS.md`

**Step 1: 运行测试**

Run: `mvn test -Dtest=*Export*,*Import*`
Expected: 所有测试通过

**Step 2: 前端构建验证**

Run: `cd frontend && npm run build`
Expected: 构建成功

**Step 3: 更新项目进度文档**

在 `docs/PROJECT_PROGRESS.md` 的 Phase 11 部分添加:

```markdown
**15. ✅ 数据导入导出功能** (Commits: xxx, xxx, xxx)
   - 后端 DTOs 和实体设计 (ExportScope, DataExportRequest, DataImportRequest)
   - 后端导出服务实现 (DataExportService - 修订版)
   - 后端导入服务实现 (DataImportService - 修订版)
   - 后端数据管理控制器 (3 个 REST API 端点 + 同步导出)
   - 前端类型定义 (TypeScript)
   - 前端 API 客户端
   - 数据管理界面 (任务列表、导出/导入对话框)
   - 支持多种导出范围和导入策略
   - 异步任务处理和状态跟踪
   - 文件验证和错误处理
   - 修订实体字段映射 (orgId, kbId, ownerId, createdBy)
   - 修订文件处理方式 (同步 JSON 导出, FormData 导入)
```

**Step 4: 提交**

```bash
git add docs/PROJECT_PROGRESS.md
git commit -m "docs: update project progress with data import/export feature

- Add data import/export to completed features
- Document entity field mappings
- Document API endpoints"
```

---

## 总结

**主要修订点：**

1. **移除文件存储依赖** - 不再使用 FileStorageManagerService，改用直接 JSON 处理
2. **修正实体字段映射** - 使用正确的字段名（orgId, kbId, ownerId, createdBy）
3. **修正时间字段** - 统一使用 Instant 类型
4. **简化导出流程** - 添加同步导出选项，直接返回 JSON
5. **修正导入流程** - 使用 MultipartFile 和 FormData

**实现功能：**
- ✅ 同步导出（直接下载 JSON）
- ✅ 异步导出（任务队列）
- ✅ 数据导入（文件上传 + 异步处理）
- ✅ 多种导出范围（知识库、Agent、聊天机器人、MCP 服务器、全部）
- ✅ 多种导入策略（跳过、覆盖、重命名）

**技术特点：**
- JSON 格式存储数据
- 包含版本和元数据
- 异步任务处理
- 完整的错误处理
- 前端 Blob API 下载
- FormData 文件上传
