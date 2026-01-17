package com.mydotey.ai.studio.service;

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

    /**
     * 执行 RAG 查询
     *
     * @param request RAG 查询请求
     * @return RAG 查询响应
     */
    public RagQueryResponse query(RagQueryRequest request) {
        log.info("Executing RAG query, question: {}, kbIds: {}",
                request.getQuestion(), request.getKnowledgeBaseIds());

        try {
            // 1. 向量搜索 - 检索相关文档
            List<SourceDocument> sources = vectorSearchService.search(
                    request.getQuestion(),
                    request.getKnowledgeBaseIds(),
                    request.getTopK() != null ? request.getTopK() : 5,
                    request.getScoreThreshold() != null ? request.getScoreThreshold() : 0.7
            );

            log.info("Found {} relevant documents", sources.size());

            // 2. 构建上下文
            String context = contextBuilderService.buildContext(
                    request.getQuestion(),
                    request.getIncludeSources() ? sources : List.of(),
                    request.getConversationHistory()
            );

            // 3. 构建 Prompt
            String systemPrompt = promptTemplateService.buildSystemPrompt(context);

            // 4. LLM 生成回答
            LlmResponse llmResponse = llmGenerationService.generate(
                    systemPrompt,
                    request.getQuestion(),
                    request.getTemperature(),
                    request.getMaxTokens()
            );

            log.info("Generated response, total tokens: {}", llmResponse.getTotalTokens());

            // 5. 构建响应
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
