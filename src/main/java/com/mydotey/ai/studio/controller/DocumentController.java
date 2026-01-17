package com.mydotey.ai.studio.controller;

import com.mydotey.ai.studio.common.ApiResponse;
import com.mydotey.ai.studio.dto.DocumentUploadResponse;
import com.mydotey.ai.studio.entity.Document;
import com.mydotey.ai.studio.mapper.DocumentMapper;
import com.mydotey.ai.studio.service.DocumentProcessingService;
import com.mydotey.ai.studio.util.FileUtil;
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
public class DocumentController {

    private final FileUtil fileUtil;
    private final DocumentMapper documentMapper;
    private final DocumentProcessingService processingService;

    /**
     * 上传文档到知识库
     */
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<DocumentUploadResponse>> uploadDocument(
            @RequestParam("file") MultipartFile file,
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
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDocumentStatus(@PathVariable Long id) {
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
