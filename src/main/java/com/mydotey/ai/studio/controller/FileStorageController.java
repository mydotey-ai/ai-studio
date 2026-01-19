package com.mydotey.ai.studio.controller;

import com.mydotey.ai.studio.annotation.AuditLog;
import com.mydotey.ai.studio.common.ApiResponse;
import com.mydotey.ai.studio.dto.filestorage.FileMetadataResponse;
import com.mydotey.ai.studio.dto.filestorage.FileUploadRequest;
import com.mydotey.ai.studio.dto.filestorage.FileUploadResponse;
import com.mydotey.ai.studio.service.FileStorageManagerService;
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
public class FileStorageController {

    private final FileStorageManagerService fileStorageManagerService;

    /**
     * 上传文件
     */
    @PostMapping("/upload")
    @AuditLog(action = "FILE_UPLOAD", resourceType = "File")
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
    public ApiResponse<FileMetadataResponse> getFileMetadata(@PathVariable Long id) {
        FileMetadataResponse response = fileStorageManagerService.getFileMetadata(id);
        return ApiResponse.success(response);
    }

    /**
     * 获取用户的文件列表
     */
    @GetMapping("/my")
    public ApiResponse<List<FileMetadataResponse>> getMyFiles(@RequestAttribute("userId") Long userId) {
        List<FileMetadataResponse> files = fileStorageManagerService.getUserFiles(userId);
        return ApiResponse.success(files);
    }

    /**
     * 获取关联实体的文件列表
     */
    @GetMapping("/related/{entityType}/{entityId}")
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
    public ApiResponse<String> deleteFile(
            @PathVariable Long id,
            @RequestAttribute("userId") Long userId) throws Exception {
        fileStorageManagerService.deleteFile(id, userId);
        return ApiResponse.success("File deleted successfully");
    }
}
