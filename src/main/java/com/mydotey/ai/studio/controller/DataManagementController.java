package com.mydotey.ai.studio.controller;

import com.mydotey.ai.studio.annotation.AuditLog;
import com.mydotey.ai.studio.common.ApiResponse;
import com.mydotey.ai.studio.dto.export.DataExportRequest;
import com.mydotey.ai.studio.dto.export.DataExportResponse;
import com.mydotey.ai.studio.dto.export.DataImportRequest;
import com.mydotey.ai.studio.dto.export.DataImportResponse;
import com.mydotey.ai.studio.entity.ExportTask;
import com.mydotey.ai.studio.entity.ImportTask;
import com.mydotey.ai.studio.service.DataExportService;
import com.mydotey.ai.studio.service.DataImportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 数据管理控制器（修订版）
 */
@RestController
@RequestMapping("/api/data-management")
@RequiredArgsConstructor
public class DataManagementController {

    private final DataExportService exportService;
    private final DataImportService importService;

    /**
     * 同步导出数据（直接返回 JSON）
     */
    @PostMapping(value = "/export/sync", produces = MediaType.APPLICATION_JSON_VALUE)
    @AuditLog(action = "DATA_EXPORT", resourceType = "DataExport")
    public String exportDataSync(
        @Valid @RequestBody DataExportRequest request,
        @RequestAttribute("userId") Long userId
    ) {
        Long orgId = getOrganizationId(userId);
        return exportService.exportDataSync(request, userId, orgId);
    }

    /**
     * 创建导出任务（异步）
     */
    @PostMapping("/export")
    @AuditLog(action = "DATA_EXPORT", resourceType = "DataExport")
    public ApiResponse<DataExportResponse> createExportTask(
        @Valid @RequestBody DataExportRequest request,
        @RequestAttribute("userId") Long userId
    ) {
        Long orgId = getOrganizationId(userId);
        DataExportResponse response = exportService.createExportTask(request, userId, orgId);
        return ApiResponse.success(response);
    }

    /**
     * 获取导出任务状态
     */
    @GetMapping("/export/{taskId}/status")
    public ApiResponse<DataExportResponse> getExportStatus(@PathVariable Long taskId) {
        DataExportResponse response = exportService.getExportStatus(taskId);
        return ApiResponse.success(response);
    }

    /**
     * 获取用户的导出任务列表
     */
    @GetMapping("/export/tasks")
    public ApiResponse<List<ExportTask>> getExportTasks(
        @RequestAttribute("userId") Long userId
    ) {
        List<ExportTask> tasks = exportService.getUserExportTasks(userId);
        return ApiResponse.success(tasks);
    }

    /**
     * 创建导入任务（从 MultipartFile）
     */
    @PostMapping("/import")
    @AuditLog(action = "DATA_IMPORT", resourceType = "DataImport")
    public ApiResponse<DataImportResponse> createImportTask(
        @RequestParam("file") MultipartFile file,
        @RequestParam("strategy") DataImportRequest.ImportStrategy strategy,
        @RequestParam(value = "validateOnly", defaultValue = "false") boolean validateOnly,
        @RequestAttribute("userId") Long userId
    ) {
        Long orgId = getOrganizationId(userId);

        DataImportRequest request = DataImportRequest.builder()
            .strategy(strategy)
            .validateOnly(validateOnly)
            .build();

        DataImportResponse response = importService.createImportTask(file, request, userId, orgId);
        return ApiResponse.success(response);
    }

    /**
     * 获取导入任务状态
     */
    @GetMapping("/import/{taskId}/status")
    public ApiResponse<DataImportResponse> getImportStatus(@PathVariable Long taskId) {
        DataImportResponse response = importService.getImportStatus(taskId);
        return ApiResponse.success(response);
    }

    /**
     * 获取用户的导入任务列表
     */
    @GetMapping("/import/tasks")
    public ApiResponse<List<ImportTask>> getImportTasks(
        @RequestAttribute("userId") Long userId
    ) {
        List<ImportTask> tasks = importService.getUserImportTasks(userId);
        return ApiResponse.success(tasks);
    }

    /**
     * 获取用户所属组织 ID
     */
    private Long getOrganizationId(Long userId) {
        // TODO: 从用户服务获取组织 ID
        // 临时实现: 假设用户只属于一个组织
        return 1L;
    }
}
