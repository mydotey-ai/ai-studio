package com.mydotey.ai.studio.controller;

import com.mydotey.ai.studio.annotation.AuditLog;
import com.mydotey.ai.studio.common.ApiResponse;
import com.mydotey.ai.studio.dto.filestorage.FileMetadataResponse;
import com.mydotey.ai.studio.dto.filestorage.FileUploadRequest;
import com.mydotey.ai.studio.dto.filestorage.FileUploadResponse;
import com.mydotey.ai.studio.service.FileStorageManagerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Tag(name = "文件存储", description = "文件上传下载和存储配置相关接口")
@SecurityRequirement(name = "bearerAuth")
public class FileStorageController {

    private final FileStorageManagerService fileStorageManagerService;

    /**
     * 上传文件
     */
    @PostMapping("/upload")
    @AuditLog(action = "FILE_UPLOAD", resourceType = "File")
    @Operation(summary = "上传文件", description = "上传文件到存储系统")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "上传成功")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数错误")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权")
    public ApiResponse<FileUploadResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            @Valid @ModelAttribute FileUploadRequest request,
            @RequestAttribute("userId") Long userId) throws Exception {
        FileUploadResponse response = fileStorageManagerService.uploadFile(file, request, userId);
        return ApiResponse.success(response);
    }

    /**
     * 下载文件
     */
    @GetMapping("/download/{id}")
    @Operation(summary = "下载文件", description = "从存储系统下载文件")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "下载成功")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "文件不存在")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable Long id,
            @RequestAttribute("userId") Long userId) throws Exception {
        var inputStream = fileStorageManagerService.downloadFile(id, userId);
        var metadata = fileStorageManagerService.getFileMetadata(id);

        // 对于本地存储，直接返回文件
        if ("LOCAL".equals(metadata.getStorageType())) {
            Path filePath = Paths.get(metadata.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(metadata.getContentType()))
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"" + metadata.getOriginalFileName() + "\"")
                        .body(resource);
            }
        }

        // 对于云存储，返回流
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(metadata.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + metadata.getOriginalFileName() + "\"")
                .body(new InputStreamResource(inputStream));
    }

    /**
     * 获取文件访问 URL
     */
    @GetMapping("/{id}/url")
    @Operation(summary = "获取文件访问 URL", description = "获取文件的临时访问 URL，支持设置过期时间")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "文件不存在")
    public ApiResponse<String> getFileUrl(
            @PathVariable Long id,
            @RequestParam(defaultValue = "3600") long expirationSeconds,
            @RequestAttribute("userId") Long userId) throws Exception {
        String url = fileStorageManagerService.getFileUrl(id, userId, expirationSeconds);
        return ApiResponse.success(url);
    }

    /**
     * 获取文件元数据
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取文件元数据", description = "获取指定文件的元数据信息")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "文件不存在")
    public ApiResponse<FileMetadataResponse> getFileMetadata(@PathVariable Long id) {
        FileMetadataResponse response = fileStorageManagerService.getFileMetadata(id);
        return ApiResponse.success(response);
    }

    /**
     * 获取用户的文件列表
     */
    @GetMapping("/my")
    @Operation(summary = "获取用户的文件列表", description = "获取当前用户上传的所有文件")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权")
    public ApiResponse<List<FileMetadataResponse>> getMyFiles(@RequestAttribute("userId") Long userId) {
        List<FileMetadataResponse> files = fileStorageManagerService.getUserFiles(userId);
        return ApiResponse.success(files);
    }

    /**
     * 获取关联实体的文件列表
     */
    @GetMapping("/related/{entityType}/{entityId}")
    @Operation(summary = "获取关联实体的文件列表", description = "获取与指定实体关联的所有文件")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权")
    public ApiResponse<List<FileMetadataResponse>> getRelatedFiles(
            @PathVariable String entityType,
            @PathVariable Long entityId) {
        List<FileMetadataResponse> files = fileStorageManagerService.getRelatedFiles(entityType, entityId);
        return ApiResponse.success(files);
    }

    /**
     * 删除文件
     */
    @DeleteMapping("/{id}")
    @AuditLog(action = "FILE_DELETE", resourceType = "File", resourceIdParam = "id")
    @Operation(summary = "删除文件", description = "删除指定的文件")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "删除成功")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "文件不存在")
    public ApiResponse<String> deleteFile(
            @PathVariable Long id,
            @RequestAttribute("userId") Long userId) throws Exception {
        fileStorageManagerService.deleteFile(id, userId);
        return ApiResponse.success("File deleted successfully");
    }
}
