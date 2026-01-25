package com.mydotey.ai.studio.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mydotey.ai.studio.common.ApiResponse;
import com.mydotey.ai.studio.dto.webcrawl.CreateCrawlTaskRequest;
import com.mydotey.ai.studio.dto.webcrawl.CrawlTaskProgressResponse;
import com.mydotey.ai.studio.dto.webcrawl.CrawlTaskResponse;
import com.mydotey.ai.studio.service.WebCrawlService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WebCrawlController.class)
@ContextConfiguration(classes = {WebCrawlController.class, com.mydotey.ai.studio.config.TestConfig.class})
public class WebCrawlControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WebCrawlService webCrawlService;

    @Test
    void testCreateTask_Success() throws Exception {
        // Given
        CreateCrawlTaskRequest request = new CreateCrawlTaskRequest();
        request.setStartUrl("https://example.com");
        request.setKbId(1L);
        request.setMaxDepth(2);
        request.setCrawlStrategy("BFS");
        request.setConcurrentLimit(3);

        CrawlTaskResponse response = CrawlTaskResponse.builder()
                .id(1L)
                .kbId(1L)
                .startUrl("https://example.com")
                .maxDepth(2)
                .crawlStrategy("BFS")
                .concurrentLimit(3)
                .status("PENDING")
                .totalPages(0)
                .successPages(0)
                .failedPages(0)
                .createdBy(1L)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(webCrawlService.createTask(any(), eq(1L))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/web-crawl/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.startUrl").value("https://example.com"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void testCreateTask_ValidationError_BlankUrl() throws Exception {
        // Given
        CreateCrawlTaskRequest request = new CreateCrawlTaskRequest();
        request.setStartUrl("");
        request.setKbId(1L);

        // When & Then
        mockMvc.perform(post("/api/web-crawl/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .requestAttr("userId", 1L))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateTask_ValidationError_NullKbId() throws Exception {
        // Given
        CreateCrawlTaskRequest request = new CreateCrawlTaskRequest();
        request.setStartUrl("https://example.com");
        request.setKbId(null);

        // When & Then
        mockMvc.perform(post("/api/web-crawl/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .requestAttr("userId", 1L))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateTask_ValidationError_InvalidDepth() throws Exception {
        // Given
        CreateCrawlTaskRequest request = new CreateCrawlTaskRequest();
        request.setStartUrl("https://example.com");
        request.setKbId(1L);
        request.setMaxDepth(0); // Must be at least 1

        // When & Then
        mockMvc.perform(post("/api/web-crawl/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .requestAttr("userId", 1L))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testStartTask_Success() throws Exception {
        // Given
        doNothing().when(webCrawlService).startCrawl(eq(1L), eq(1L));

        // When & Then
        mockMvc.perform(post("/api/web-crawl/tasks/1/start")
                        .requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Crawl task started successfully"));
    }

    @Test
    void testGetTask_Success() throws Exception {
        // Given
        CrawlTaskResponse response = CrawlTaskResponse.builder()
                .id(1L)
                .kbId(1L)
                .startUrl("https://example.com")
                .maxDepth(2)
                .crawlStrategy("BFS")
                .status("RUNNING")
                .totalPages(10)
                .successPages(5)
                .failedPages(1)
                .createdBy(1L)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(webCrawlService.getTask(eq(1L), eq(1L))).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/web-crawl/tasks/1")
                        .requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.status").value("RUNNING"))
                .andExpect(jsonPath("$.data.totalPages").value(10))
                .andExpect(jsonPath("$.data.successPages").value(5))
                .andExpect(jsonPath("$.data.failedPages").value(1));
    }

    @Test
    void testGetTaskProgress_Success() throws Exception {
        // Given
        CrawlTaskProgressResponse response = CrawlTaskProgressResponse.builder()
                .taskId(1L)
                .status("RUNNING")
                .totalPages(10)
                .successPages(5)
                .failedPages(1)
                .pendingPages(4)
                .pages(List.of())
                .build();

        when(webCrawlService.getTaskProgress(eq(1L), eq(1L))).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/web-crawl/tasks/1/progress")
                        .requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.taskId").value(1))
                .andExpect(jsonPath("$.data.status").value("RUNNING"))
                .andExpect(jsonPath("$.data.totalPages").value(10))
                .andExpect(jsonPath("$.data.successPages").value(5))
                .andExpect(jsonPath("$.data.failedPages").value(1))
                .andExpect(jsonPath("$.data.pendingPages").value(4));
    }

    @Test
    void testGetTasksByKb_Success() throws Exception {
        // Given
        CrawlTaskResponse task1 = CrawlTaskResponse.builder()
                .id(1L)
                .kbId(1L)
                .startUrl("https://example.com")
                .status("COMPLETED")
                .createdBy(1L)
                .createdAt(Instant.now())
                .build();

        CrawlTaskResponse task2 = CrawlTaskResponse.builder()
                .id(2L)
                .kbId(1L)
                .startUrl("https://example.org")
                .status("RUNNING")
                .createdBy(1L)
                .createdAt(Instant.now())
                .build();

        when(webCrawlService.getTasksByKb(eq(1L), eq(1L))).thenReturn(List.of(task1, task2));

        // When & Then
        mockMvc.perform(get("/api/web-crawl/tasks/kb/1")
                        .requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$.data[1].id").value(2))
                .andExpect(jsonPath("$.data[1].status").value("RUNNING"));
    }

    @Test
    void testGetTasksByKb_EmptyList() throws Exception {
        // Given
        when(webCrawlService.getTasksByKb(eq(1L), eq(1L))).thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/api/web-crawl/tasks/kb/1")
                        .requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void testDeleteTask_Success() throws Exception {
        // Given
        doNothing().when(webCrawlService).deleteTask(eq(1L), eq(1L));

        // When & Then
        mockMvc.perform(delete("/api/web-crawl/tasks/1")
                        .requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Crawl task deleted successfully"));
    }
}
