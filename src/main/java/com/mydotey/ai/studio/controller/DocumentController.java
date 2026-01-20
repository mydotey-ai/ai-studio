package com.mydotey.ai.studio.controller;

import com.mydotey.ai.studio.common.ApiResponse;
import com.mydotey.ai.studio.dto.DocumentUploadResponse;
import com.mydotey.ai.studio.entity.Document;
import com.mydotey.ai.studio.mapper.DocumentMapper;
import com.mydotey.ai.studio.service.DocumentProcessingService;
import com.mydotey.ai.studio.util.FileUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * 文档管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Tag(name = "文档", description = "文档上传和处理相关接口")
@SecurityRequirement(name = "bearerAuth")
public class DocumentController {

    private final FileUtil fileUtil;
    private final DocumentMapper documentMapper;
    private final DocumentProcessingService processingService;

    /**
     * 上传文档到知识库
     */
    @PostMapping("/upload")
    @Operation(summary = "上传文档", description = "上传文档到指定知识库并开始处理")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "文档上传成功")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数错误或文件为空")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "服务器内部错误")
    public ResponseEntity<ApiResponse<DocumentUploadResponse>> uploadDocument(
            @Parameter(description = "要上传的文档文件", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "知识库ID", required = true)
            @RequestParam("kbId") Long kbId) {

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
            document.setKbId(kbId);
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
            log.error("Failed to upload document", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Failed to upload document: " + e.getMessage()));
        }
    }

    /**
     * 获取文档状态
     */
    @GetMapping("/{id}/status")
    @Operation(summary = "获取文档状态", description = "查询文档的处理状态和详细信息")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "文档不存在")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDocumentStatus(
            @Parameter(description = "文档ID", required = true)
            @PathVariable Long id) {
        Document document = documentMapper.selectById(id);

        if (document == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> status = new HashMap<>();
        status.put("documentId", document.getId());
        status.put("status", document.getStatus());
        status.put("chunkCount", document.getChunkCount());
        status.put("errorMessage", document.getErrorMessage());

        return ResponseEntity.ok(ApiResponse.success(status));
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
