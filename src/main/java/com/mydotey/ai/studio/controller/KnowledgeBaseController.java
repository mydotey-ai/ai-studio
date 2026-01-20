package com.mydotey.ai.studio.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mydotey.ai.studio.common.ApiResponse;
import com.mydotey.ai.studio.dto.CreateKnowledgeBaseRequest;
import com.mydotey.ai.studio.dto.KnowledgeBaseResponse;
import com.mydotey.ai.studio.dto.UpdateKnowledgeBaseRequest;
import com.mydotey.ai.studio.service.KnowledgeBaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/knowledge-bases")
@RequiredArgsConstructor
@Tag(name = "知识库", description = "知识库管理相关接口")
@SecurityRequirement(name = "bearerAuth")
public class KnowledgeBaseController {

    private final KnowledgeBaseService kbService;

    @PostMapping
    @Operation(summary = "创建知识库", description = "创建新的知识库")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "创建成功")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数错误")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权")
    public ApiResponse<KnowledgeBaseResponse> create(
            @Valid @RequestBody CreateKnowledgeBaseRequest request,
            @RequestAttribute("userId") Long userId) {
        KnowledgeBaseResponse response = kbService.create(request, userId);
        return ApiResponse.success(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新知识库", description = "更新知识库信息")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "更新成功")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数错误")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "知识库不存在")
    public ApiResponse<KnowledgeBaseResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateKnowledgeBaseRequest request,
            @RequestAttribute("userId") Long userId) {
        KnowledgeBaseResponse response = kbService.update(id, request, userId);
        return ApiResponse.success(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除知识库", description = "删除指定的知识库")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "删除成功")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "知识库不存在")
    public ApiResponse<Void> delete(
            @PathVariable Long id,
            @RequestAttribute("userId") Long userId) {
        kbService.delete(id, userId);
        return ApiResponse.success("Knowledge base deleted", null);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取知识库详情", description = "获取指定知识库的详细信息")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "知识库不存在")
    public ApiResponse<KnowledgeBaseResponse> get(
            @PathVariable Long id,
            @RequestAttribute("userId") Long userId) {
        KnowledgeBaseResponse response = kbService.get(id, userId);
        return ApiResponse.success(response);
    }

    @GetMapping
    @Operation(summary = "获取知识库列表", description = "分页获取用户的知识库列表")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权")
    public ApiResponse<IPage<KnowledgeBaseResponse>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestAttribute("userId") Long userId) {
        IPage<KnowledgeBaseResponse> response = kbService.list(userId, page, size);
        return ApiResponse.success(response);
    }
}
