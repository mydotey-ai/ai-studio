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
            DataImportResponse.ImportStats.ImportStatsBuilder stats = DataImportResponse.ImportStats.builder()
                .knowledgeBasesCreated(0)
                .knowledgeBasesSkipped(0)
                .agentsCreated(0)
                .agentsSkipped(0)
                .chatbotsCreated(0)
                .chatbotsSkipped(0);

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

            DataImportResponse.ImportStats currentStats = stats.build();
            int skipped = currentStats.getKnowledgeBasesSkipped();
            int created = currentStats.getKnowledgeBasesCreated();

            if (existing != null) {
                if (request.getStrategy() == DataImportRequest.ImportStrategy.SKIP_EXISTING) {
                    stats.knowledgeBasesSkipped(skipped + 1);
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

            stats.knowledgeBasesCreated(created + 1);
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

            DataImportResponse.ImportStats currentStats = stats.build();
            int skipped = currentStats.getAgentsSkipped();
            int created = currentStats.getAgentsCreated();

            if (existing != null &&
                request.getStrategy() == DataImportRequest.ImportStrategy.SKIP_EXISTING) {
                stats.agentsSkipped(skipped + 1);
                continue;
            }

            agent.setId(null);
            agent.setOrgId(task.getOrganizationId());
            agent.setCreatedAt(Instant.now());
            agent.setUpdatedAt(Instant.now());
            agentMapper.insert(agent);

            stats.agentsCreated(created + 1);
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

            DataImportResponse.ImportStats currentStats = stats.build();
            int skipped = currentStats.getChatbotsSkipped();
            int created = currentStats.getChatbotsCreated();

            if (existing != null &&
                request.getStrategy() == DataImportRequest.ImportStrategy.SKIP_EXISTING) {
                stats.chatbotsSkipped(skipped + 1);
                continue;
            }

            chatbot.setId(null);
            chatbot.setOwnerId(task.getUserId());
            chatbot.setCreatedAt(Instant.now());
            chatbot.setUpdatedAt(Instant.now());
            chatbotMapper.insert(chatbot);

            stats.chatbotsCreated(created + 1);
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
