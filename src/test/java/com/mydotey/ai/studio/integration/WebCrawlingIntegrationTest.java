package com.mydotey.ai.studio.integration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mydotey.ai.studio.common.exception.BusinessException;
import com.mydotey.ai.studio.dto.CreateKnowledgeBaseRequest;
import com.mydotey.ai.studio.dto.KnowledgeBaseResponse;
import com.mydotey.ai.studio.dto.webcrawl.CreateCrawlTaskRequest;
import com.mydotey.ai.studio.dto.webcrawl.CrawlTaskProgressResponse;
import com.mydotey.ai.studio.dto.webcrawl.CrawlTaskResponse;
import com.mydotey.ai.studio.entity.User;
import com.mydotey.ai.studio.entity.WebPage;
import com.mydotey.ai.studio.mapper.KnowledgeBaseMapper;
import com.mydotey.ai.studio.mapper.UserMapper;
import com.mydotey.ai.studio.mapper.WebCrawlTaskMapper;
import com.mydotey.ai.studio.mapper.WebPageMapper;
import com.mydotey.ai.studio.service.KnowledgeBaseService;
import com.mydotey.ai.studio.service.WebCrawlService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Web Crawling Integration Test
 * Tests the complete web crawling system end-to-end
 */
@SpringBootTest
@Transactional
@DisplayName("Web Crawling Integration Test")
class WebCrawlingIntegrationTest {

    @Autowired
    private WebCrawlService webCrawlService;

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @Autowired
    private WebCrawlTaskMapper taskMapper;

    @Autowired
    private WebPageMapper pageMapper;

    @Autowired
    private KnowledgeBaseMapper kbMapper;

    @Autowired
    private UserMapper userMapper;

    private Long testKbId;
    private Long testUserId;
    private Long testTaskId;

    @BeforeEach
    void setUp() {
        // Create test user
        User user = new User();
        user.setUsername("test_user_webcrawl");
        user.setPasswordHash("hashed_password");
        user.setEmail("test_webcrawl@example.com");
        userMapper.insert(user);
        testUserId = user.getId();

        // Create test knowledge base
        CreateKnowledgeBaseRequest request = new CreateKnowledgeBaseRequest();
        request.setName("Test KB for Web Crawling");
        request.setDescription("Integration test knowledge base for web crawling");
        request.setEmbeddingModel("text-embedding-ada-002");
        request.setChunkSize(500);
        request.setChunkOverlap(100);

        KnowledgeBaseResponse kb = knowledgeBaseService.create(request, testUserId);
        testKbId = kb.getId();
    }

    @AfterEach
    void tearDown() {
        // Clean up test data (will be rolled back by @Transactional)
        // Explicit cleanup not needed due to @Transactional, but kept for clarity
        if (testTaskId != null) {
            pageMapper.delete(new LambdaQueryWrapper<WebPage>()
                .eq(WebPage::getCrawlTaskId, testTaskId));
            taskMapper.deleteById(testTaskId);
        }
        if (testKbId != null) {
            knowledgeBaseService.delete(testKbId, testUserId);
        }
        if (testUserId != null) {
            userMapper.deleteById(testUserId);
        }
    }

    @Test
    @DisplayName("Test complete crawl flow - end to end")
    void testCompleteCrawlFlow() {
        // 1. Create crawl task
        CreateCrawlTaskRequest request = new CreateCrawlTaskRequest();
        request.setKbId(testKbId);
        request.setStartUrl("https://example.com");
        request.setMaxDepth(1);
        request.setConcurrentLimit(1);
        request.setCrawlStrategy("BFS");

        CrawlTaskResponse task = webCrawlService.createTask(request, testUserId);
        testTaskId = task.getId();

        // Verify task created successfully
        assertNotNull(task);
        assertNotNull(task.getId());
        assertEquals(testKbId, task.getKbId());
        assertEquals("https://example.com", task.getStartUrl());
        assertEquals(1, task.getMaxDepth());
        assertEquals(1, task.getConcurrentLimit());
        assertEquals("BFS", task.getCrawlStrategy());
        assertEquals("PENDING", task.getStatus());
        assertEquals(0, task.getTotalPages());
        assertEquals(0, task.getSuccessPages());
        assertEquals(0, task.getFailedPages());

        // 2. Start crawl (async operation)
        webCrawlService.startCrawl(task.getId(), testUserId);

        // 3. Wait for crawl to progress (async operation may take time)
        // Note: Due to @Transactional, the async operation runs in a different transaction
        // and may not be visible to the test. We're primarily testing that the task
        // is created and can be retrieved successfully.
        try {
            TimeUnit.SECONDS.sleep(10);
        } catch (InterruptedException e) {
            fail("Test interrupted while waiting for crawl to complete");
        }

        // 4. Verify task can still be retrieved
        CrawlTaskResponse updatedTask = webCrawlService.getTask(task.getId(), testUserId);
        assertNotNull(updatedTask);
        assertEquals(task.getId(), updatedTask.getId());

        // 5. Check task progress endpoint works
        CrawlTaskProgressResponse progress = webCrawlService.getTaskProgress(task.getId(), testUserId);
        assertNotNull(progress);
        assertEquals(task.getId(), progress.getTaskId());
        assertNotNull(progress.getStatus());

        // Note: In a real @Transactional test, the async updates may not be visible
        // The primary integration test goal here is to verify the service layer
        // correctly creates tasks, handles permissions, and manages the workflow
        // A full end-to-end test with actual crawling would require @DirtiesContext
        // or a separate test without @Transactional
    }

    @Test
    @DisplayName("Test get tasks by knowledge base")
    void testGetTasksByKb() {
        // Create multiple tasks
        CreateCrawlTaskRequest request1 = new CreateCrawlTaskRequest();
        request1.setKbId(testKbId);
        request1.setStartUrl("https://example.com/page1");
        request1.setMaxDepth(1);
        request1.setConcurrentLimit(1);

        CrawlTaskResponse task1 = webCrawlService.createTask(request1, testUserId);

        CreateCrawlTaskRequest request2 = new CreateCrawlTaskRequest();
        request2.setKbId(testKbId);
        request2.setStartUrl("https://example.com/page2");
        request2.setMaxDepth(1);
        request2.setConcurrentLimit(1);

        CrawlTaskResponse task2 = webCrawlService.createTask(request2, testUserId);

        // Get all tasks for KB
        List<CrawlTaskResponse> tasks = webCrawlService.getTasksByKb(testKbId, testUserId);

        assertNotNull(tasks);
        assertTrue(tasks.size() >= 2, "Should have at least 2 tasks");

        // Verify tasks are ordered by creation time (most recent first)
        assertTrue(tasks.get(0).getCreatedAt().isAfter(tasks.get(1).getCreatedAt()) ||
                   tasks.get(0).getCreatedAt().equals(tasks.get(1).getCreatedAt()));

        // Verify our tasks are in the list
        boolean foundTask1 = tasks.stream().anyMatch(t -> t.getId().equals(task1.getId()));
        boolean foundTask2 = tasks.stream().anyMatch(t -> t.getId().equals(task2.getId()));

        assertTrue(foundTask1, "Task 1 should be in the list");
        assertTrue(foundTask2, "Task 2 should be in the list");
    }

    @Test
    @DisplayName("Test delete task")
    void testDeleteTask() {
        // Create task
        CreateCrawlTaskRequest request = new CreateCrawlTaskRequest();
        request.setKbId(testKbId);
        request.setStartUrl("https://example.com/delete-test");
        request.setMaxDepth(1);
        request.setConcurrentLimit(1);

        CrawlTaskResponse task = webCrawlService.createTask(request, testUserId);
        Long taskId = task.getId();

        // Verify task exists
        CrawlTaskResponse fetched = webCrawlService.getTask(taskId, testUserId);
        assertNotNull(fetched);
        assertEquals(taskId, fetched.getId());

        // Delete task
        webCrawlService.deleteTask(taskId, testUserId);

        // Verify task is deleted
        BusinessException exception = assertThrows(BusinessException.class,
            () -> webCrawlService.getTask(taskId, testUserId));
        assertTrue(exception.getMessage().contains("not found") ||
                   exception.getMessage().contains("Crawl task"));

        // Verify pages are also deleted
        List<WebPage> pages = pageMapper.selectList(
            new LambdaQueryWrapper<WebPage>()
                .eq(WebPage::getCrawlTaskId, taskId)
        );
        assertTrue(pages.isEmpty(), "Pages should be deleted when task is deleted");

        testTaskId = null; // Don't try to delete again in tearDown
    }

    @Test
    @DisplayName("Test create task with non-existent knowledge base")
    void testCreateTaskWithNonExistentKb() {
        // Try to create task with non-existent KB
        CreateCrawlTaskRequest request = new CreateCrawlTaskRequest();
        request.setKbId(999999L); // Non-existent KB
        request.setStartUrl("https://example.com");
        request.setMaxDepth(1);
        request.setConcurrentLimit(1);

        // Should throw exception
        BusinessException exception = assertThrows(BusinessException.class,
            () -> webCrawlService.createTask(request, testUserId));

        assertTrue(exception.getMessage().contains("not found") ||
                   exception.getMessage().contains("Knowledge base"));
    }

    @Test
    @DisplayName("Test get tasks with different user - permission check")
    void testGetTasksPermissionCheck() {
        // Create another user
        User anotherUser = new User();
        anotherUser.setUsername("another_user");
        anotherUser.setPasswordHash("hashed_password");
        anotherUser.setEmail("another@example.com");
        userMapper.insert(anotherUser);
        Long anotherUserId = anotherUser.getId();

        // Create task with testUserId
        CreateCrawlTaskRequest request = new CreateCrawlTaskRequest();
        request.setKbId(testKbId);
        request.setStartUrl("https://example.com/private");
        request.setMaxDepth(1);
        request.setConcurrentLimit(1);

        CrawlTaskResponse task = webCrawlService.createTask(request, testUserId);

        // Try to get task with different user - should fail
        BusinessException exception = assertThrows(BusinessException.class,
            () -> webCrawlService.getTask(task.getId(), anotherUserId));

        assertTrue(exception.getMessage().contains("permission") ||
                   exception.getMessage().contains("don't have permission"));

        // Cleanup
        userMapper.deleteById(anotherUserId);
    }

    @Test
    @DisplayName("Test task progress tracking")
    void testTaskProgressTracking() {
        // Create and start task
        CreateCrawlTaskRequest request = new CreateCrawlTaskRequest();
        request.setKbId(testKbId);
        request.setStartUrl("https://example.com");
        request.setMaxDepth(1);
        request.setConcurrentLimit(1);

        CrawlTaskResponse task = webCrawlService.createTask(request, testUserId);
        testTaskId = task.getId();

        // Get progress before starting
        CrawlTaskProgressResponse progressBefore = webCrawlService.getTaskProgress(task.getId(), testUserId);

        assertNotNull(progressBefore);
        assertEquals(task.getId(), progressBefore.getTaskId());
        assertEquals("PENDING", progressBefore.getStatus());

        // Progress should have valid counts (even if zero)
        assertNotNull(progressBefore.getTotalPages());
        assertNotNull(progressBefore.getSuccessPages());
        assertNotNull(progressBefore.getFailedPages());
        assertNotNull(progressBefore.getPendingPages());

        // Verify non-negative counts
        assertTrue(progressBefore.getTotalPages() >= 0);
        assertTrue(progressBefore.getSuccessPages() >= 0);
        assertTrue(progressBefore.getFailedPages() >= 0);
        assertTrue(progressBefore.getPendingPages() >= 0);

        // Verify pages list exists (even if empty)
        assertNotNull(progressBefore.getPages());

        // Start crawl
        webCrawlService.startCrawl(task.getId(), testUserId);

        // Wait a bit for crawl to start
        try {
            TimeUnit.SECONDS.sleep(3);
        } catch (InterruptedException e) {
            fail("Test interrupted while waiting for crawl to start");
        }

        // Get progress after starting
        CrawlTaskProgressResponse progressAfter = webCrawlService.getTaskProgress(task.getId(), testUserId);

        assertNotNull(progressAfter);
        assertEquals(task.getId(), progressAfter.getTaskId());
        assertNotNull(progressAfter.getStatus());
    }

    @Test
    @DisplayName("Test delete task permission check")
    void testDeleteTaskPermissionCheck() {
        // Create another user
        User anotherUser = new User();
        anotherUser.setUsername("malicious_user");
        anotherUser.setPasswordHash("hashed_password");
        anotherUser.setEmail("malicious@example.com");
        userMapper.insert(anotherUser);
        Long anotherUserId = anotherUser.getId();

        // Create task with testUserId
        CreateCrawlTaskRequest request = new CreateCrawlTaskRequest();
        request.setKbId(testKbId);
        request.setStartUrl("https://example.com/protected");
        request.setMaxDepth(1);
        request.setConcurrentLimit(1);

        CrawlTaskResponse task = webCrawlService.createTask(request, testUserId);

        // Try to delete task with different user - should fail
        BusinessException exception = assertThrows(BusinessException.class,
            () -> webCrawlService.deleteTask(task.getId(), anotherUserId));

        assertTrue(exception.getMessage().contains("permission") ||
                   exception.getMessage().contains("don't have permission"));

        // Verify task still exists
        CrawlTaskResponse stillExists = webCrawlService.getTask(task.getId(), testUserId);
        assertNotNull(stillExists);
        assertEquals(task.getId(), stillExists.getId());

        // Cleanup
        userMapper.deleteById(anotherUserId);
    }
}
