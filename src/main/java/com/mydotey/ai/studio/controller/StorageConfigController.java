package com.mydotey.ai.studio.controller;

import com.mydotey.ai.studio.annotation.AuditLog;
import com.mydotey.ai.studio.annotation.RequireRole;
import com.mydotey.ai.studio.common.ApiResponse;
import com.mydotey.ai.studio.dto.filestorage.CreateStorageConfigRequest;
import com.mydotey.ai.studio.dto.filestorage.StorageConfigResponse;
import com.mydotey.ai.studio.dto.filestorage.UpdateStorageConfigRequest;
import com.mydotey.ai.studio.service.StorageConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/storage-configs")
@RequiredArgsConstructor
@Tag(name = "存储配置", description = "文件存储配置管理相关接口")
@SecurityRequirement(name = "bearerAuth")
public class StorageConfigController {

    private final StorageConfigService storageConfigService;

    /**
     * 创建存储配置（仅管理员）
     */
    @PostMapping
    @RequireRole({"ADMIN", "SUPER_ADMIN"})
    @AuditLog(action = "STORAGE_CONFIG_CREATE", resourceType = "StorageConfig")
    @Operation(summary = "创建存储配置", description = "创建新的文件存储配置（仅管理员）")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "创建成功")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数错误")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足")
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
    @Operation(summary = "更新存储配置", description = "更新文件存储配置（仅管理员）")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "更新成功")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数错误")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "配置不存在")
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
    @Operation(summary = "删除存储配置", description = "删除文件存储配置（仅管理员）")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "删除成功")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "配置不存在")
    public ApiResponse<String> deleteConfig(@PathVariable Long id) {
        storageConfigService.deleteConfig(id);
        return ApiResponse.success("Storage config deleted");
    }

    /**
     * 获取存储配置详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取存储配置详情", description = "获取指定存储配置的详细信息")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "配置不存在")
    public ApiResponse<StorageConfigResponse> getConfig(@PathVariable Long id) {
        StorageConfigResponse response = storageConfigService.getConfig(id);
        return ApiResponse.success(response);
    }

    /**
     * 获取所有存储配置
     */
    @GetMapping
    @Operation(summary = "获取所有存储配置", description = "获取所有文件存储配置列表")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权")
    public ApiResponse<List<StorageConfigResponse>> getAllConfigs() {
        List<StorageConfigResponse> configs = storageConfigService.getAllConfigs();
        return ApiResponse.success(configs);
    }

    /**
     * 获取默认存储配置
     */
    @GetMapping("/default")
    @Operation(summary = "获取默认存储配置", description = "获取默认的文件存储配置")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "默认配置不存在")
    public ApiResponse<StorageConfigResponse> getDefaultConfig() {
        StorageConfigResponse response = storageConfigService.getDefaultConfig();
        return ApiResponse.success(response);
    }
}
