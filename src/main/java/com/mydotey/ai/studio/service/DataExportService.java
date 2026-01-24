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
     * 执行导出（生成 JSON 并记录大小）
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
