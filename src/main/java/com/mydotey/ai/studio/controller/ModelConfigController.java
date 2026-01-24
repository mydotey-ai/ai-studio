package com.mydotey.ai.studio.controller;

import com.mydotey.ai.studio.common.ApiResponse;
import com.mydotey.ai.studio.dto.ModelConfigDto;
import com.mydotey.ai.studio.dto.ModelConfigRequest;
import com.mydotey.ai.studio.dto.ModelConfigResponse;
import com.mydotey.ai.studio.enums.ModelConfigType;
import com.mydotey.ai.studio.service.ModelConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/model-configs")
@RequiredArgsConstructor
@Tag(name = "模型配置", description = "模型配置管理相关接口")
@SecurityRequirement(name = "bearerAuth")
public class ModelConfigController {

    private final ModelConfigService modelConfigService;

    @PostMapping
    @Operation(summary = "创建模型配置", description = "创建新的模型配置")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "创建成功")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数错误")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权")
    public ApiResponse<ModelConfigResponse> create(
            @Valid @RequestBody ModelConfigRequest request,
            @RequestAttribute("orgId") Long orgId,
            @RequestAttribute("userId") Long userId) {
        ModelConfigResponse response = modelConfigService.create(request, orgId, userId);
        return ApiResponse.success(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新模型配置", description = "更新模型配置信息")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "更新成功")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数错误")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "模型配置不存在")
    public ApiResponse<ModelConfigResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ModelConfigRequest request,
            @RequestAttribute("orgId") Long orgId) {
        ModelConfigResponse response = modelConfigService.update(id, request, orgId);
        return ApiResponse.success(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除模型配置", description = "删除指定的模型配置")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "删除成功")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "模型配置不存在")
    public ApiResponse<Void> delete(
            @PathVariable Long id,
            @RequestAttribute("orgId") Long orgId) {
        modelConfigService.delete(id, orgId);
        return ApiResponse.success("Model config deleted", null);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取模型配置详情", description = "获取指定模型配置的详细信息")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "模型配置不存在")
    public ApiResponse<ModelConfigResponse> getById(
            @PathVariable Long id,
            @RequestAttribute("orgId") Long orgId) {
        ModelConfigResponse response = modelConfigService.getById(id, orgId);
        return ApiResponse.success(response);
    }

    @GetMapping
    @Operation(summary = "获取组织的模型配置列表", description = "获取组织的所有模型配置")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权")
    public ApiResponse<List<ModelConfigResponse>> listByOrg(
            @Parameter(description = "模型配置类型") @RequestParam(required = false) ModelConfigType type,
            @RequestAttribute("orgId") Long orgId) {
        List<ModelConfigResponse> response = modelConfigService.listByOrg(orgId, type);
        return ApiResponse.success(response);
    }

    @GetMapping("/default")
    @Operation(summary = "获取默认模型配置", description = "获取指定类型的默认模型配置")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "默认配置不存在")
    public ApiResponse<ModelConfigDto> getDefaultConfig(
            @Parameter(description = "模型配置类型") @RequestParam ModelConfigType type) {
        ModelConfigDto response = modelConfigService.getDefaultConfig(type);
        return ApiResponse.success(response);
    }

    @PutMapping("/{id}/default")
    @Operation(summary = "设置默认配置", description = "将指定配置设置为默认配置")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "设置成功")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "模型配置不存在")
    public ApiResponse<Void> setDefault(
            @PathVariable Long id,
            @RequestAttribute("orgId") Long orgId) {
        modelConfigService.setDefault(id, orgId);
        return ApiResponse.success("Default config set", null);
    }

    @PostMapping("/{id}/test")
    @Operation(summary = "测试配置是否有效", description = "测试指定的模型配置是否有效")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "测试成功")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "模型配置不存在")
    public ApiResponse<Boolean> testConfig(@PathVariable Long id) {
        boolean isValid = modelConfigService.testConfig(id);
        return ApiResponse.success(isValid);
    }
}
