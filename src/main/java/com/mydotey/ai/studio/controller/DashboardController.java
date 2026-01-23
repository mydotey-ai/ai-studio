package com.mydotey.ai.studio.controller;

import com.mydotey.ai.studio.common.ApiResponse;
import com.mydotey.ai.studio.dto.dashboard.*;
import com.mydotey.ai.studio.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "仪表盘", description = "数据可视化仪表盘相关接口")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/statistics")
    @Operation(summary = "获取统计数据汇总", description = "获取知识库、Agent、聊天机器人、文档、用户、存储的统计数据")
    public ApiResponse<DashboardStatisticsDTO> getStatistics() {
        DashboardStatisticsDTO stats = dashboardService.getStatistics();
        return ApiResponse.success(stats);
    }

    @GetMapping("/trends")
    @Operation(summary = "获取趋势数据", description = "获取指定天数内的 API 调用和活跃用户趋势")
    public ApiResponse<List<TrendDataDTO>> getTrends(
            @RequestParam(defaultValue = "7") int days
    ) {
        List<TrendDataDTO> trends = dashboardService.getTrends(days);
        return ApiResponse.success(trends);
    }

    @GetMapping("/activities")
    @Operation(summary = "获取最近活动", description = "获取最近的审计日志记录")
    public ApiResponse<List<ActivityDTO>> getRecentActivities(
            @RequestParam(defaultValue = "10") int limit
    ) {
        List<ActivityDTO> activities = dashboardService.getRecentActivities(limit);
        return ApiResponse.success(activities);
    }

    @GetMapping("/health")
    @Operation(summary = "获取系统健康状态", description = "获取 API 成功率、响应时间等健康指标")
    public ApiResponse<HealthStatusDTO> getHealthStatus() {
        HealthStatusDTO health = dashboardService.getHealthStatus();
        return ApiResponse.success(health);
    }
}
