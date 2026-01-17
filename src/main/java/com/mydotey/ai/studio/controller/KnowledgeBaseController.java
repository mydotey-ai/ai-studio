package com.mydotey.ai.studio.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mydotey.ai.studio.common.ApiResponse;
import com.mydotey.ai.studio.dto.CreateKnowledgeBaseRequest;
import com.mydotey.ai.studio.dto.KnowledgeBaseResponse;
import com.mydotey.ai.studio.dto.UpdateKnowledgeBaseRequest;
import com.mydotey.ai.studio.service.KnowledgeBaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/knowledge-bases")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseService kbService;

    @PostMapping
    public ApiResponse<KnowledgeBaseResponse> create(
            @Valid @RequestBody CreateKnowledgeBaseRequest request,
            @RequestAttribute("userId") Long userId) {
        KnowledgeBaseResponse response = kbService.create(request, userId);
        return ApiResponse.success(response);
    }

    @PutMapping("/{id}")
    public ApiResponse<KnowledgeBaseResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateKnowledgeBaseRequest request,
            @RequestAttribute("userId") Long userId) {
        KnowledgeBaseResponse response = kbService.update(id, request, userId);
        return ApiResponse.success(response);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            @PathVariable Long id,
            @RequestAttribute("userId") Long userId) {
        kbService.delete(id, userId);
        return ApiResponse.success("Knowledge base deleted", null);
    }

    @GetMapping("/{id}")
    public ApiResponse<KnowledgeBaseResponse> get(
            @PathVariable Long id,
            @RequestAttribute("userId") Long userId) {
        KnowledgeBaseResponse response = kbService.get(id, userId);
        return ApiResponse.success(response);
    }

    @GetMapping
    public ApiResponse<IPage<KnowledgeBaseResponse>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestAttribute("userId") Long userId) {
        IPage<KnowledgeBaseResponse> response = kbService.list(userId, page, size);
        return ApiResponse.success(response);
    }
}
