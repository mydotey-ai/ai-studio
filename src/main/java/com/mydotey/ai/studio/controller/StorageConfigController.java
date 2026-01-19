package com.mydotey.ai.studio.controller;

import com.mydotey.ai.studio.annotation.AuditLog;
import com.mydotey.ai.studio.annotation.RequireRole;
import com.mydotey.ai.studio.common.ApiResponse;
import com.mydotey.ai.studio.dto.filestorage.CreateStorageConfigRequest;
import com.mydotey.ai.studio.dto.filestorage.StorageConfigResponse;
import com.mydotey.ai.studio.dto.filestorage.UpdateStorageConfigRequest;
import com.mydotey.ai.studio.service.StorageConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/storage-configs")
@RequiredArgsConstructor
public class StorageConfigController {

    private final StorageConfigService storageConfigService;

    /**
     * 创建存储配置（仅管理员）
     */
    @PostMapping
    @RequireRole({"ADMIN", "SUPER_ADMIN"})
    @AuditLog(action = "STORAGE_CONFIG_CREATE", resourceType = "StorageConfig")
    public ApiResponse<StorageConfigResponse> createConfig(
            @Valid @RequestBody CreateStorageConfigRequest request,
            @RequestAttribute("userId") Long userId) {
        StorageConfigResponse response = storageConfigService.createConfig(request, userId);
        return ApiResponse.success(response);
    }

    /**
     * 更新存储配置（仅管理员）
     */
    @PutMapping("/{id}")
    @RequireRole({"ADMIN", "SUPER_ADMIN"})
    @AuditLog(action = "STORAGE_CONFIG_UPDATE", resourceType = "StorageConfig", resourceIdParam = "id")
    public ApiResponse<StorageConfigResponse> updateConfig(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStorageConfigRequest request,
            @RequestAttribute("userId") Long userId) {
        StorageConfigResponse response = storageConfigService.updateConfig(id, request, userId);
        return ApiResponse.success(response);
    }

    /**
     * 删除存储配置（仅管理员）
     */
    @DeleteMapping("/{id}")
    @RequireRole({"ADMIN", "SUPER_ADMIN"})
    @AuditLog(action = "STORAGE_CONFIG_DELETE", resourceType = "StorageConfig", resourceIdParam = "id")
    public ApiResponse<String> deleteConfig(@PathVariable Long id) {
        storageConfigService.deleteConfig(id);
        return ApiResponse.success("Storage config deleted");
    }

    /**
     * 获取存储配置详情
     */
    @GetMapping("/{id}")
    public ApiResponse<StorageConfigResponse> getConfig(@PathVariable Long id) {
        StorageConfigResponse response = storageConfigService.getConfig(id);
        return ApiResponse.success(response);
    }

    /**
     * 获取所有存储配置
     */
    @GetMapping
    public ApiResponse<List<StorageConfigResponse>> getAllConfigs() {
        List<StorageConfigResponse> configs = storageConfigService.getAllConfigs();
        return ApiResponse.success(configs);
    }

    /**
     * 获取默认存储配置
     */
    @GetMapping("/default")
    public ApiResponse<StorageConfigResponse> getDefaultConfig() {
        StorageConfigResponse response = storageConfigService.getDefaultConfig();
        return ApiResponse.success(response);
    }
}
