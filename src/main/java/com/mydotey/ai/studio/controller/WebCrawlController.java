package com.mydotey.ai.studio.controller;

import com.mydotey.ai.studio.annotation.AuditLog;
import com.mydotey.ai.studio.common.ApiResponse;
import com.mydotey.ai.studio.dto.webcrawl.CreateCrawlTaskRequest;
import com.mydotey.ai.studio.dto.webcrawl.CrawlTaskProgressResponse;
import com.mydotey.ai.studio.dto.webcrawl.CrawlTaskResponse;
import com.mydotey.ai.studio.service.WebCrawlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Web Crawl Controller
 * REST API for managing web crawl tasks
 */
@Slf4j
@RestController
@RequestMapping("/api/web-crawl/tasks")
@RequiredArgsConstructor
@Tag(name = "网页抓取", description = "网页抓取任务管理相关接口")
@SecurityRequirement(name = "bearerAuth")
public class WebCrawlController {

    private final WebCrawlService webCrawlService;

    /**
     * Create a new crawl task
     *
     * @param request The crawl task creation request
     * @param userId The ID of the user creating the task
     * @return The created task response
     */
    @PostMapping
    @AuditLog(action = "WEB_CRAWL_CREATE", resourceType = "WebCrawlTask")
    @Operation(summary = "创建网页抓取任务", description = "创建新的网页抓取任务")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "创建成功")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数错误")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权")
    public ApiResponse<CrawlTaskResponse> createTask(
            @Valid @RequestBody CreateCrawlTaskRequest request,
            @RequestAttribute("userId") Long userId) {
        log.info("Creating crawl task: kbId={}, url={}", request.getKbId(), request.getStartUrl());
        CrawlTaskResponse response = webCrawlService.createTask(request, userId);
        return ApiResponse.success(response);
    }

    /**
     * Start a crawl task
     *
     * @param id The ID of the task to start
     * @param userId The ID of the user starting the task
     * @return Success response
     */
    @PostMapping("/{id}/start")
    @AuditLog(action = "WEB_CRAWL_START", resourceType = "WebCrawlTask", resourceIdParam = "id")
    @Operation(summary = "启动网页抓取任务", description = "启动指定的网页抓取任务")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "启动成功")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "任务不存在")
    public ApiResponse<Void> startTask(
            @PathVariable Long id,
            @RequestAttribute("userId") Long userId) {
        log.info("Starting crawl task: taskId={}, userId={}", id, userId);
        webCrawlService.startCrawl(id);
        return ApiResponse.<Void>success("Crawl task started successfully", null);
    }

    /**
     * Get task details
     *
     * @param id The ID of the task
     * @param userId The ID of the user requesting the task
     * @return The task response
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取网页抓取任务详情", description = "获取指定网页抓取任务的详细信息")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "任务不存在")
    public ApiResponse<CrawlTaskResponse> getTask(
            @PathVariable Long id,
            @RequestAttribute("userId") Long userId) {
        CrawlTaskResponse response = webCrawlService.getTask(id, userId);
        return ApiResponse.success(response);
    }

    /**
     * Get task progress with pages
     *
     * @param id The ID of the task
     * @param userId The ID of the user requesting the progress
     * @return The task progress response
     */
    @GetMapping("/{id}/progress")
    @Operation(summary = "获取网页抓取任务进度", description = "获取指定网页抓取任务的执行进度")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "任务不存在")
    public ApiResponse<CrawlTaskProgressResponse> getTaskProgress(
            @PathVariable Long id,
            @RequestAttribute("userId") Long userId) {
        CrawlTaskProgressResponse response = webCrawlService.getTaskProgress(id, userId);
        return ApiResponse.success(response);
    }

    /**
     * Get all tasks for a knowledge base
     *
     * @param kbId The knowledge base ID
     * @param userId The ID of the user requesting the tasks
     * @return List of task responses
     */
    @GetMapping("/kb/{kbId}")
    @Operation(summary = "获取知识库的抓取任务列表", description = "获取指定知识库的所有网页抓取任务")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权")
    public ApiResponse<List<CrawlTaskResponse>> getTasksByKb(
            @PathVariable Long kbId,
            @RequestAttribute("userId") Long userId) {
        List<CrawlTaskResponse> response = webCrawlService.getTasksByKb(kbId, userId);
        return ApiResponse.success(response);
    }

    /**
     * Delete a crawl task
     *
     * @param id The ID of the task to delete
     * @param userId The ID of the user deleting the task
     * @return Success response
     */
    @DeleteMapping("/{id}")
    @AuditLog(action = "WEB_CRAWL_DELETE", resourceType = "WebCrawlTask", resourceIdParam = "id")
    @Operation(summary = "删除网页抓取任务", description = "删除指定的网页抓取任务")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "删除成功")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "任务不存在")
    public ApiResponse<Void> deleteTask(
            @PathVariable Long id,
            @RequestAttribute("userId") Long userId) {
        log.info("Deleting crawl task: taskId={}, userId={}", id, userId);
        webCrawlService.deleteTask(id, userId);
        return ApiResponse.<Void>success("Crawl task deleted successfully", null);
    }
}
