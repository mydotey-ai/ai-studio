package com.mydotey.ai.studio.controller;

import com.mydotey.ai.studio.annotation.AuditLog;
import com.mydotey.ai.studio.common.ApiResponse;
import com.mydotey.ai.studio.dto.*;
import com.mydotey.ai.studio.service.*;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * RAG 查询控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
public class RagController {

    private final RagService ragService;
    private final VectorSearchService vectorSearchService;
    private final ContextBuilderService contextBuilderService;
    private final PromptTemplateService promptTemplateService;
    private final StreamingLlmService streamingLlmService;

    /**
     * 执行 RAG 查询（非流式）
     */
    @PostMapping("/query")
    @AuditLog(action = "RAG_QUERY", resourceType = "KnowledgeBase")
    public ApiResponse<RagQueryResponse> query(@Valid @RequestBody RagQueryRequest request,
                                           @RequestAttribute("userId") Long userId) {
        log.info("Received RAG query request: {}", request.getQuestion());

        RagQueryResponse response = ragService.query(request, userId);

        return ApiResponse.success(response);
    }

    /**
     * 执行 RAG 查询（流式 SSE）
     */
    @PostMapping(value = "/query/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @AuditLog(action = "RAG_QUERY_STREAM", resourceType = "KnowledgeBase")
    public void queryStream(
            @Valid @RequestBody RagQueryRequest request,
            HttpServletResponse response) throws IOException {

        log.info("Received RAG stream query request: {}", request.getQuestion());

        // 设置 SSE 响应头
        response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");

        PrintWriter writer = response.getWriter();

        // 1. 向量搜索
        List<SourceDocument> sources = vectorSearchService.search(
                request.getQuestion(),
                request.getKnowledgeBaseIds(),
                request.getTopK() != null ? request.getTopK() : 5,
                request.getScoreThreshold() != null ? request.getScoreThreshold() : 0.7
        );

        // 2. 构建上下文
        String context = contextBuilderService.buildContext(
                request.getQuestion(),
                request.getIncludeSources() ? sources : List.of(),
                request.getConversationHistory()
        );

        // 3. 构建 Prompt
        String systemPrompt = promptTemplateService.buildSystemPrompt(context);

        // 4. 流式生成
        StringBuilder answerBuilder = new StringBuilder();

        streamingLlmService.streamGenerate(
                systemPrompt,
                request.getQuestion(),
                request.getTemperature(),
                request.getMaxTokens(),
                new StreamingLlmService.StreamCallback() {
                    @Override
                    public void onContent(String content) {
                        answerBuilder.append(content);

                        // 发送 SSE 事件
                        writer.write("data: " + escapeSseData(content) + "\n\n");
                        writer.flush();
                    }

                    @Override
                    public void onComplete() {
                        // 发送完成事件
                        RagQueryResponse finalResponse = RagQueryResponse.builder()
                                .answer(answerBuilder.toString())
                                .sources(request.getIncludeSources() ? sources : List.of())
                                .isComplete(true)
                                .build();

                        writer.write("data: [DONE]\n\n");
                        writer.flush();
                    }

                    @Override
                    public void onError(Exception e) {
                        log.error("Stream generation error", e);
                        writer.write("event: error\n");
                        writer.write("data: " + e.getMessage() + "\n\n");
                        writer.flush();
                    }
                }
        );
    }

    /**
     * 转义 SSE 数据
     */
    private String escapeSseData(String data) {
        // 简化版：实际需要更完整的转义
        return data.replace("\n", "\\n").replace("\"", "\\\"");
    }
}
