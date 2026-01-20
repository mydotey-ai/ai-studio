package com.mydotey.ai.studio.service;

import com.mydotey.ai.studio.annotation.PerformanceMonitor;
import com.mydotey.ai.studio.common.exception.BusinessException;
import com.mydotey.ai.studio.config.LlmConfig;
import com.mydotey.ai.studio.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * RAG 服务（检索增强生成）
 * 协调向量搜索、上下文构建和 LLM 生成
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final VectorSearchService vectorSearchService;
    private final ContextBuilderService contextBuilderService;
    private final PromptTemplateService promptTemplateService;
    private final LlmGenerationService llmGenerationService;
    private final LlmConfig llmConfig;
    private final KnowledgeBaseService knowledgeBaseService;

    /**
     * 执行 RAG 查询
     *
     * @param request RAG 查询请求
     * @param userId 当前用户 ID（用于权限验证）
     * @return RAG 查询响应
     */
    @PerformanceMonitor(value = "RAG Query", slowThreshold = 2000, logParams = true)
    public RagQueryResponse query(RagQueryRequest request, Long userId) {
        log.info("Executing RAG query, question: {}, kbIds: {}, userId: {}",
                request.getQuestion(), request.getKnowledgeBaseIds(), userId);

        try {
            // 1. 验证用户是否有权限访问请求的知识库
            try {
                knowledgeBaseService.validateAccess(request.getKnowledgeBaseIds(), userId);
            } catch (BusinessException e) {
                log.warn("User {} does not have permission to access knowledge bases: {}",
                        userId, request.getKnowledgeBaseIds());
                throw new BusinessException("You don't have permission to access one or more knowledge bases");
            }

            // 2. 向量搜索 - 检索相关文档
            List<SourceDocument> sources = vectorSearchService.search(
                    request.getQuestion(),
                    request.getKnowledgeBaseIds(),
                    request.getTopK() != null ? request.getTopK() : 5,
                    request.getScoreThreshold() != null ? request.getScoreThreshold() : 0.7
            );

            log.info("Found {} relevant documents", sources.size());

            // 3. 构建上下文
            String context = contextBuilderService.buildContext(
                    request.getQuestion(),
                    request.getIncludeSources() ? sources : List.of(),
                    request.getConversationHistory()
            );

            // 4. 构建 Prompt
            String systemPrompt = promptTemplateService.buildSystemPrompt(context);

            // 5. LLM 生成回答
            LlmResponse llmResponse = llmGenerationService.generate(
                    systemPrompt,
                    request.getQuestion(),
                    request.getTemperature(),
                    request.getMaxTokens()
            );

            log.info("Generated response, total tokens: {}", llmResponse.getTotalTokens());

            // 6. 构建响应
            return RagQueryResponse.builder()
                    .answer(llmResponse.getContent())
                    .sources(request.getIncludeSources() ? sources : List.of())
                    .model(llmConfig.getModel())
                    .totalTokens(llmResponse.getTotalTokens())
                    .isComplete(true)
                    .build();

        } catch (Exception e) {
            log.error("RAG query failed", e);
            throw new RuntimeException("RAG query failed: " + e.getMessage(), e);
        }
    }
}
