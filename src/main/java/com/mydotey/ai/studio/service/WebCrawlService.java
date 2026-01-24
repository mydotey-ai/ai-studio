package com.mydotey.ai.studio.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mydotey.ai.studio.common.exception.BusinessException;
import com.mydotey.ai.studio.dto.webcrawl.CreateCrawlTaskRequest;
import com.mydotey.ai.studio.dto.webcrawl.CrawlTaskProgressResponse;
import com.mydotey.ai.studio.dto.webcrawl.CrawlTaskResponse;
import com.mydotey.ai.studio.dto.webcrawl.WebPageResponse;
import com.mydotey.ai.studio.entity.KnowledgeBase;
import com.mydotey.ai.studio.entity.WebCrawlTask;
import com.mydotey.ai.studio.entity.WebPage;
import com.mydotey.ai.studio.mapper.KnowledgeBaseMapper;
import com.mydotey.ai.studio.mapper.WebCrawlTaskMapper;
import com.mydotey.ai.studio.mapper.WebPageMapper;
import com.mydotey.ai.studio.service.webcrawl.CrawlOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Web Crawl Service
 * Provides business logic for managing web crawl tasks
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebCrawlService {

    private final WebCrawlTaskMapper taskMapper;
    private final WebPageMapper pageMapper;
    private final KnowledgeBaseMapper kbMapper;
    private final CrawlOrchestrator crawlOrchestrator;

    /**
     * Create a new crawl task
     *
     * @param request The crawl task creation request
     * @param userId The ID of the user creating the task
     * @return The created task response
     */
    public CrawlTaskResponse createTask(CreateCrawlTaskRequest request, Long userId) {
        // Verify knowledge base ownership
        KnowledgeBase kb = kbMapper.selectById(request.getKbId());
        if (kb == null) {
            throw new BusinessException("Knowledge base not found");
        }

        if (!kb.getOwnerId().equals(userId)) {
            throw new BusinessException("You don't have permission to create crawl tasks for this knowledge base");
        }

        // Create task
        WebCrawlTask task = new WebCrawlTask();
        task.setKbId(request.getKbId());
        task.setStartUrl(request.getStartUrl());
        task.setUrlPattern(request.getUrlPattern());
        task.setMaxDepth(request.getMaxDepth());
        task.setCrawlStrategy(request.getCrawlStrategy());
        task.setConcurrentLimit(request.getConcurrentLimit());
        task.setStatus("PENDING");
        task.setTotalPages(0);
        task.setSuccessPages(0);
        task.setFailedPages(0);
        task.setCreatedBy(userId);
        task.setCreatedAt(Instant.now());
        task.setUpdatedAt(Instant.now());

        taskMapper.insert(task);

        log.info("Created crawl task: id={}, kbId={}, url={}, userId={}",
                task.getId(), task.getKbId(), task.getStartUrl(), userId);

        return toResponse(task);
    }

    /**
     * Start async crawl execution
     *
     * @param taskId The ID of the task to start
     */
    @Async
    public void startCrawl(Long taskId, Long userId) {
        log.info("Starting crawl task: taskId={}", taskId);

        WebCrawlTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException("Crawl task not found");
        }

        // Execute crawl asynchronously
        crawlOrchestrator.execute(task);

        log.info("Crawl task execution started: taskId={}", taskId);
    }

    /**
     * Get task details
     *
     * @param taskId The ID of the task
     * @param userId The ID of the user requesting the task
     * @return The task response
     */
    public CrawlTaskResponse getTask(Long taskId, Long userId) {
        WebCrawlTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException("Crawl task not found");
        }

        // Verify permission (user must own the knowledge base)
        verifyTaskPermission(task, userId);

        return toResponse(task);
    }

    /**
     * Get task progress with pages
     *
     * @param taskId The ID of the task
     * @param userId The ID of the user requesting the progress
     * @return The task progress response
     */
    public CrawlTaskProgressResponse getTaskProgress(Long taskId, Long userId) {
        WebCrawlTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException("Crawl task not found");
        }

        // Verify permission
        verifyTaskPermission(task, userId);

        // Get pages
        List<WebPage> pages = pageMapper.selectList(
                new LambdaQueryWrapper<WebPage>()
                        .eq(WebPage::getCrawlTaskId, taskId)
                        .orderByAsc(WebPage::getDepth)
                        .orderByAsc(WebPage::getCreatedAt)
        );

        List<WebPageResponse> pageResponses = pages.stream()
                .map(this::toPageResponse)
                .collect(Collectors.toList());

        // Calculate pending pages
        int pendingPages = 0;
        if (task.getTotalPages() != null && task.getSuccessPages() != null && task.getFailedPages() != null) {
            pendingPages = task.getTotalPages() - task.getSuccessPages() - task.getFailedPages();
        }

        return CrawlTaskProgressResponse.builder()
                .taskId(task.getId())
                .status(task.getStatus())
                .totalPages(task.getTotalPages())
                .successPages(task.getSuccessPages())
                .failedPages(task.getFailedPages())
                .pendingPages(pendingPages)
                .errorMessage(task.getErrorMessage())
                .pages(pageResponses)
                .build();
    }

    /**
     * Get all tasks for a knowledge base
     *
     * @param kbId The knowledge base ID
     * @param userId The ID of the user requesting the tasks
     * @return List of task responses
     */
    public List<CrawlTaskResponse> getTasksByKb(Long kbId, Long userId) {
        // Verify knowledge base ownership
        KnowledgeBase kb = kbMapper.selectById(kbId);
        if (kb == null) {
            throw new BusinessException("Knowledge base not found");
        }

        if (!kb.getOwnerId().equals(userId)) {
            throw new BusinessException("You don't have permission to access crawl tasks for this knowledge base");
        }

        List<WebCrawlTask> tasks = taskMapper.selectList(
                new LambdaQueryWrapper<WebCrawlTask>()
                        .eq(WebCrawlTask::getKbId, kbId)
                        .orderByDesc(WebCrawlTask::getCreatedAt)
        );

        return tasks.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Delete a crawl task
     *
     * @param taskId The ID of the task to delete
     * @param userId The ID of the user deleting the task
     */
    public void deleteTask(Long taskId, Long userId) {
        WebCrawlTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException("Crawl task not found");
        }

        // Verify permission
        verifyTaskPermission(task, userId);

        // Delete related pages first
        pageMapper.delete(
                new LambdaQueryWrapper<WebPage>()
                        .eq(WebPage::getCrawlTaskId, taskId)
        );

        // Delete task
        taskMapper.deleteById(taskId);

        log.info("Deleted crawl task: id={}, userId={}", taskId, userId);
    }

    /**
     * Verify that the user has permission to access the task
     *
     * @param task The crawl task
     * @param userId The user ID
     */
    private void verifyTaskPermission(WebCrawlTask task, Long userId) {
        KnowledgeBase kb = kbMapper.selectById(task.getKbId());
        if (kb == null) {
            throw new BusinessException("Knowledge base not found");
        }

        // User must own the knowledge base to access the task
        if (!kb.getOwnerId().equals(userId)) {
            throw new BusinessException("You don't have permission to access this crawl task");
        }
    }

    /**
     * Convert WebCrawlTask entity to CrawlTaskResponse DTO
     *
     * @param task The task entity
     * @return The task response DTO
     */
    private CrawlTaskResponse toResponse(WebCrawlTask task) {
        return CrawlTaskResponse.builder()
                .id(task.getId())
                .kbId(task.getKbId())
                .startUrl(task.getStartUrl())
                .urlPattern(task.getUrlPattern())
                .maxDepth(task.getMaxDepth())
                .crawlStrategy(task.getCrawlStrategy())
                .concurrentLimit(task.getConcurrentLimit())
                .status(task.getStatus())
                .totalPages(task.getTotalPages())
                .successPages(task.getSuccessPages())
                .failedPages(task.getFailedPages())
                .errorMessage(task.getErrorMessage())
                .createdBy(task.getCreatedBy())
                .startedAt(task.getStartedAt())
                .completedAt(task.getCompletedAt())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }

    /**
     * Convert WebPage entity to WebPageResponse DTO
     *
     * @param page The page entity
     * @return The page response DTO
     */
    private WebPageResponse toPageResponse(WebPage page) {
        return WebPageResponse.builder()
                .id(page.getId())
                .crawlTaskId(page.getCrawlTaskId())
                .documentId(page.getDocumentId())
                .url(page.getUrl())
                .title(page.getTitle())
                .status(page.getStatus())
                .errorMessage(page.getErrorMessage())
                .depth(page.getDepth())
                .createdAt(page.getCreatedAt())
                .updatedAt(page.getUpdatedAt())
                .build();
    }
}
