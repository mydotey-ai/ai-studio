package com.mydotey.ai.studio.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mydotey.ai.studio.common.ApiResponse;
import com.mydotey.ai.studio.dto.CreateKnowledgeBaseRequest;
import com.mydotey.ai.studio.dto.DocumentUploadResponse;
import com.mydotey.ai.studio.dto.KnowledgeBaseResponse;
import com.mydotey.ai.studio.dto.UpdateKnowledgeBaseRequest;
import com.mydotey.ai.studio.entity.Document;
import com.mydotey.ai.studio.mapper.DocumentMapper;
import com.mydotey.ai.studio.service.DocumentProcessingService;
import com.mydotey.ai.studio.service.KnowledgeBaseService;
import com.mydotey.ai.studio.util.FileUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/knowledge-bases")
@RequiredArgsConstructor
@Tag(name = "知识库", description = "知识库管理相关接口")
@SecurityRequirement(name = "bearerAuth")
public class KnowledgeBaseController {

    private final KnowledgeBaseService kbService;
    private final DocumentMapper documentMapper;
    private final DocumentProcessingService processingService;
    private final FileUtil fileUtil;

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

    /**
     * 上传文档到知识库
     */
    @PostMapping("/{id}/documents")
    @Operation(summary = "上传文档到知识库", description = "上传文档到指定知识库并开始处理")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "文档上传成功")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数错误或文件为空")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "服务器内部错误")
    public ResponseEntity<ApiResponse<DocumentUploadResponse>> uploadDocument(
            @PathVariable Long id,
            @Parameter(description = "要上传的文档文件", required = true)
            @RequestParam("file") MultipartFile file) {

        try {
            // 1. 验证文件
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("File is empty"));
            }

            // 2. 保存文件
            String filePath = fileUtil.saveFile(
                file.getOriginalFilename(),
                file.getBytes()
            );

            // 3. 创建文档记录
            Document document = new Document();
            document.setKbId(id);
            document.setFilename(file.getOriginalFilename());
            document.setFileUrl(filePath);
            document.setFileSize(file.getSize());
            document.setFileType(getFileExtension(file.getOriginalFilename()));
            document.setStatus("PENDING");
            document.setSourceType("UPLOAD");
            document.setChunkCount(0);

            documentMapper.insert(document);

            // 4. 异步处理文档
            processingService.processDocument(document.getId());

            // 5. 构造响应
            DocumentUploadResponse response = new DocumentUploadResponse();
            response.setDocumentId(document.getId());
            response.setFilename(file.getOriginalFilename());
            response.setFileSize(file.getSize());
            response.setStatus("PENDING");
            response.setMessage("Document uploaded successfully, processing started");

            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Failed to upload document: " + e.getMessage()));
        }
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }
}
