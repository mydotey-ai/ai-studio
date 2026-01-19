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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class WebCrawlServiceTest {

    private WebCrawlTaskMapper taskMapper = mock(WebCrawlTaskMapper.class);
    private WebPageMapper pageMapper = mock(WebPageMapper.class);
    private KnowledgeBaseMapper kbMapper = mock(KnowledgeBaseMapper.class);
    private CrawlOrchestrator crawlOrchestrator = mock(CrawlOrchestrator.class);

    private WebCrawlService webCrawlService;

    @BeforeEach
    void setUp() {
        webCrawlService = new WebCrawlService(taskMapper, pageMapper, kbMapper, crawlOrchestrator);
    }

    @Test
    void testCreateTask_Success() {
        // Given
        Long userId = 1L;
        Long kbId = 1L;

        KnowledgeBase kb = new KnowledgeBase();
        kb.setId(kbId);
        kb.setOwnerId(userId);

        CreateCrawlTaskRequest request = new CreateCrawlTaskRequest();
        request.setStartUrl("https://example.com");
        request.setKbId(kbId);
        request.setMaxDepth(2);
        request.setCrawlStrategy("BFS");
        request.setConcurrentLimit(3);

        when(kbMapper.selectById(kbId)).thenReturn(kb);
        when(taskMapper.insert(any(WebCrawlTask.class))).thenAnswer(invocation -> {
            WebCrawlTask task = invocation.getArgument(0);
            task.setId(1L);
            return 1;
        });

        // When
        CrawlTaskResponse response = webCrawlService.createTask(request, userId);

        // Then
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(kbId, response.getKbId());
        assertEquals("https://example.com", response.getStartUrl());
        assertEquals(2, response.getMaxDepth());
        assertEquals("BFS", response.getCrawlStrategy());
        assertEquals(3, response.getConcurrentLimit());
        assertEquals("PENDING", response.getStatus());
        assertEquals(userId, response.getCreatedBy());

        ArgumentCaptor<WebCrawlTask> captor = ArgumentCaptor.forClass(WebCrawlTask.class);
        verify(taskMapper).insert(captor.capture());

        WebCrawlTask capturedTask = captor.getValue();
        assertEquals("https://example.com", capturedTask.getStartUrl());
        assertEquals(kbId, capturedTask.getKbId());
        assertEquals(2, capturedTask.getMaxDepth());
        assertEquals("BFS", capturedTask.getCrawlStrategy());
        assertEquals(3, capturedTask.getConcurrentLimit());
        assertEquals("PENDING", capturedTask.getStatus());
        assertEquals(userId, capturedTask.getCreatedBy());
        assertNotNull(capturedTask.getCreatedAt());
        assertNotNull(capturedTask.getUpdatedAt());
    }

    @Test
    void testCreateTask_KnowledgeBaseNotFound() {
        // Given
        Long userId = 1L;
        Long kbId = 999L;

        CreateCrawlTaskRequest request = new CreateCrawlTaskRequest();
        request.setStartUrl("https://example.com");
        request.setKbId(kbId);

        when(kbMapper.selectById(kbId)).thenReturn(null);

        // When & Then
        assertThrows(BusinessException.class, () -> {
            webCrawlService.createTask(request, userId);
        });

        verify(taskMapper, never()).insert(any(WebCrawlTask.class));
    }

    @Test
    void testCreateTask_NoPermission() {
        // Given
        Long userId = 1L;
        Long kbId = 1L;

        KnowledgeBase kb = new KnowledgeBase();
        kb.setId(kbId);
        kb.setOwnerId(2L); // Different owner

        CreateCrawlTaskRequest request = new CreateCrawlTaskRequest();
        request.setStartUrl("https://example.com");
        request.setKbId(kbId);

        when(kbMapper.selectById(kbId)).thenReturn(kb);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            webCrawlService.createTask(request, userId);
        });

        assertTrue(exception.getMessage().contains("permission"));
        verify(taskMapper, never()).insert(any(WebCrawlTask.class));
    }

    @Test
    void testStartCrawl_Success() {
        // Given
        Long taskId = 1L;

        WebCrawlTask task = new WebCrawlTask();
        task.setId(taskId);
        task.setStatus("PENDING");

        when(taskMapper.selectById(taskId)).thenReturn(task);
        doNothing().when(crawlOrchestrator).execute(any(WebCrawlTask.class));

        // When
        webCrawlService.startCrawl(taskId);

        // Then
        ArgumentCaptor<WebCrawlTask> captor = ArgumentCaptor.forClass(WebCrawlTask.class);
        verify(crawlOrchestrator).execute(captor.capture());

        WebCrawlTask executedTask = captor.getValue();
        assertEquals(taskId, executedTask.getId());
    }

    @Test
    void testStartCrawl_TaskNotFound() {
        // Given
        Long taskId = 999L;
        when(taskMapper.selectById(taskId)).thenReturn(null);

        // When & Then
        assertThrows(BusinessException.class, () -> {
            webCrawlService.startCrawl(taskId);
        });

        verify(crawlOrchestrator, never()).execute(any(WebCrawlTask.class));
    }

    @Test
    void testGetTask_Success() {
        // Given
        Long taskId = 1L;
        Long userId = 1L;

        WebCrawlTask task = new WebCrawlTask();
        task.setId(taskId);
        task.setKbId(1L);
        task.setStartUrl("https://example.com");
        task.setMaxDepth(2);
        task.setCrawlStrategy("BFS");
        task.setConcurrentLimit(3);
        task.setStatus("COMPLETED");
        task.setTotalPages(10);
        task.setSuccessPages(8);
        task.setFailedPages(2);
        task.setCreatedBy(userId);
        task.setCreatedAt(Instant.now());
        task.setUpdatedAt(Instant.now());

        KnowledgeBase kb = new KnowledgeBase();
        kb.setId(1L);
        kb.setOwnerId(userId);

        when(taskMapper.selectById(taskId)).thenReturn(task);
        when(kbMapper.selectById(1L)).thenReturn(kb);

        // When
        CrawlTaskResponse response = webCrawlService.getTask(taskId, userId);

        // Then
        assertNotNull(response);
        assertEquals(taskId, response.getId());
        assertEquals(1L, response.getKbId());
        assertEquals("https://example.com", response.getStartUrl());
        assertEquals(2, response.getMaxDepth());
        assertEquals("BFS", response.getCrawlStrategy());
        assertEquals(3, response.getConcurrentLimit());
        assertEquals("COMPLETED", response.getStatus());
        assertEquals(10, response.getTotalPages());
        assertEquals(8, response.getSuccessPages());
        assertEquals(2, response.getFailedPages());
        assertEquals(userId, response.getCreatedBy());
    }

    @Test
    void testGetTask_NotFound() {
        // Given
        Long taskId = 999L;
        Long userId = 1L;

        when(taskMapper.selectById(taskId)).thenReturn(null);

        // When & Then
        assertThrows(BusinessException.class, () -> {
            webCrawlService.getTask(taskId, userId);
        });
    }

    @Test
    void testGetTask_NoPermission() {
        // Given
        Long taskId = 1L;
        Long userId = 1L;

        WebCrawlTask task = new WebCrawlTask();
        task.setId(taskId);
        task.setKbId(1L);
        task.setCreatedBy(2L); // Different creator

        KnowledgeBase kb = new KnowledgeBase();
        kb.setId(1L);
        kb.setOwnerId(2L); // Different owner

        when(taskMapper.selectById(taskId)).thenReturn(task);
        when(kbMapper.selectById(1L)).thenReturn(kb);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            webCrawlService.getTask(taskId, userId);
        });

        assertTrue(exception.getMessage().contains("permission"));
    }

    @Test
    void testGetTaskProgress_Success() {
        // Given
        Long taskId = 1L;
        Long userId = 1L;

        WebCrawlTask task = new WebCrawlTask();
        task.setId(taskId);
        task.setKbId(1L);
        task.setStatus("RUNNING");
        task.setTotalPages(10);
        task.setSuccessPages(5);
        task.setFailedPages(1);
        task.setCreatedBy(userId);

        WebPage page1 = new WebPage();
        page1.setId(1L);
        page1.setUrl("https://example.com/page1");
        page1.setTitle("Page 1");
        page1.setStatus("SUCCESS");
        page1.setDepth(1);

        WebPage page2 = new WebPage();
        page2.setId(2L);
        page2.setUrl("https://example.com/page2");
        page2.setTitle("Page 2");
        page2.setStatus("SUCCESS");
        page2.setDepth(1);

        List<WebPage> pages = List.of(page1, page2);

        KnowledgeBase kb = new KnowledgeBase();
        kb.setId(1L);
        kb.setOwnerId(userId);

        when(taskMapper.selectById(taskId)).thenReturn(task);
        when(kbMapper.selectById(1L)).thenReturn(kb);
        when(pageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(pages);

        // When
        CrawlTaskProgressResponse response = webCrawlService.getTaskProgress(taskId, userId);

        // Then
        assertNotNull(response);
        assertEquals(taskId, response.getTaskId());
        assertEquals("RUNNING", response.getStatus());
        assertEquals(10, response.getTotalPages());
        assertEquals(5, response.getSuccessPages());
        assertEquals(1, response.getFailedPages());
        assertEquals(4, response.getPendingPages()); // 10 - 5 - 1
        assertNotNull(response.getPages());
        assertEquals(2, response.getPages().size());
    }

    @Test
    void testGetTasksByKb_Success() {
        // Given
        Long kbId = 1L;
        Long userId = 1L;

        KnowledgeBase kb = new KnowledgeBase();
        kb.setId(kbId);
        kb.setOwnerId(userId);

        WebCrawlTask task1 = new WebCrawlTask();
        task1.setId(1L);
        task1.setKbId(kbId);
        task1.setStartUrl("https://example.com");
        task1.setStatus("COMPLETED");

        WebCrawlTask task2 = new WebCrawlTask();
        task2.setId(2L);
        task2.setKbId(kbId);
        task2.setStartUrl("https://example.org");
        task1.setStatus("RUNNING");

        List<WebCrawlTask> tasks = List.of(task1, task2);

        when(kbMapper.selectById(kbId)).thenReturn(kb);
        when(taskMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(tasks);

        // When
        List<CrawlTaskResponse> responses = webCrawlService.getTasksByKb(kbId, userId);

        // Then
        assertNotNull(responses);
        assertEquals(2, responses.size());
        assertEquals(1L, responses.get(0).getId());
        assertEquals(2L, responses.get(1).getId());
    }

    @Test
    void testGetTasksByKb_NoPermission() {
        // Given
        Long kbId = 1L;
        Long userId = 1L;

        KnowledgeBase kb = new KnowledgeBase();
        kb.setId(kbId);
        kb.setOwnerId(2L); // Different owner

        when(kbMapper.selectById(kbId)).thenReturn(kb);

        // When & Then
        assertThrows(BusinessException.class, () -> {
            webCrawlService.getTasksByKb(kbId, userId);
        });
    }

    @Test
    void testDeleteTask_Success() {
        // Given
        Long taskId = 1L;
        Long userId = 1L;

        WebCrawlTask task = new WebCrawlTask();
        task.setId(taskId);
        task.setKbId(1L);
        task.setCreatedBy(userId);

        KnowledgeBase kb = new KnowledgeBase();
        kb.setId(1L);
        kb.setOwnerId(userId);

        when(taskMapper.selectById(taskId)).thenReturn(task);
        when(kbMapper.selectById(1L)).thenReturn(kb);
        when(taskMapper.deleteById(taskId)).thenReturn(1);

        // When
        webCrawlService.deleteTask(taskId, userId);

        // Then
        verify(taskMapper).deleteById(taskId);
    }

    @Test
    void testDeleteTask_NotFound() {
        // Given
        Long taskId = 999L;
        Long userId = 1L;

        when(taskMapper.selectById(taskId)).thenReturn(null);

        // When & Then
        assertThrows(BusinessException.class, () -> {
            webCrawlService.deleteTask(taskId, userId);
        });

        verify(taskMapper, never()).deleteById(any(Long.class));
    }

    @Test
    void testDeleteTask_NoPermission() {
        // Given
        Long taskId = 1L;
        Long userId = 1L;

        WebCrawlTask task = new WebCrawlTask();
        task.setId(taskId);
        task.setKbId(1L);
        task.setCreatedBy(2L); // Different creator

        KnowledgeBase kb = new KnowledgeBase();
        kb.setId(1L);
        kb.setOwnerId(2L); // Different owner

        when(taskMapper.selectById(taskId)).thenReturn(task);
        when(kbMapper.selectById(1L)).thenReturn(kb);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            webCrawlService.deleteTask(taskId, userId);
        });

        assertTrue(exception.getMessage().contains("permission"));
        verify(taskMapper, never()).deleteById(any(Long.class));
    }
}
