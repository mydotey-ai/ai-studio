package com.mydotey.ai.studio.controller;

import com.mydotey.ai.studio.annotation.AuditLog;
import com.mydotey.ai.studio.common.ApiResponse;
import com.mydotey.ai.studio.dto.RagQueryRequest;
import com.mydotey.ai.studio.dto.RagQueryResponse;
import com.mydotey.ai.studio.service.RagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * RAG 查询控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
public class RagController {

    private final RagService ragService;

    /**
     * RAG 查询接口
     *
     * @param request RAG 查询请求
     * @return RAG 查询响应
     */
    @PostMapping("/query")
    @AuditLog(action = "RAG_QUERY", resourceType = "KnowledgeBase")
    public ApiResponse<RagQueryResponse> query(@Valid @RequestBody RagQueryRequest request) {
        log.info("Received RAG query request, question: {}, kbIds: {}",
                request.getQuestion(), request.getKnowledgeBaseIds());
        RagQueryResponse response = ragService.query(request);
        return ApiResponse.success(response);
    }
}
