package com.mydotey.ai.studio.service.webcrawl;

import com.mydotey.ai.studio.entity.WebCrawlTask;
import com.mydotey.ai.studio.entity.WebPage;
import com.mydotey.ai.studio.mapper.WebCrawlTaskMapper;
import com.mydotey.ai.studio.mapper.WebPageMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("爬取编排器测试")
class CrawlOrchestratorTest {

    @Mock
    private WebScraper webScraper;

    @Mock
    private WebCrawlTaskMapper taskMapper;

    @Mock
    private WebPageMapper pageMapper;

    @InjectMocks
    private CrawlOrchestrator orchestrator;

    @Test
    @DisplayName("应该能够执行 BFS 爬取策略")
    void testExecuteBFSStrategy() {
        // Given - 准备测试数据
        WebCrawlTask task = new WebCrawlTask();
        task.setId(1L);
        task.setStartUrl("https://example.com");
        task.setUrlPattern("https://example.com/.*");
        task.setMaxDepth(2);
        task.setCrawlStrategy("BFS");
        task.setConcurrentLimit(2);
        task.setStatus("PENDING");
        task.setTotalPages(0);
        task.setSuccessPages(0);
        task.setFailedPages(0);

        // Mock webScraper responses
        ScrapedResult result1 = ScrapedResult.builder()
                .url("https://example.com")
                .title("Home")
                .content("Welcome")
                .links(Arrays.asList(
                        "https://example.com/page1",
                        "https://example.com/page2"
                ))
                .build();

        ScrapedResult result2 = ScrapedResult.builder()
                .url("https://example.com/page1")
                .title("Page 1")
                .content("Content 1")
                .links(Arrays.asList(
                        "https://example.com/page1/sub1",
                        "https://example.com/page1/sub2"
                ))
                .build();

        ScrapedResult result3 = ScrapedResult.builder()
                .url("https://example.com/page2")
                .title("Page 2")
                .content("Content 2")
                .links(Collections.emptyList())
                .build();

        try {
            when(webScraper.scrape("https://example.com")).thenReturn(result1);
            when(webScraper.scrape("https://example.com/page1")).thenReturn(result2);
            when(webScraper.scrape("https://example.com/page2")).thenReturn(result3);
        } catch (ScrapingException e) {
            fail("Mock setup failed", e);
        }

        // Mock mapper operations
        when(taskMapper.updateById(any(WebCrawlTask.class))).thenReturn(1);
        when(pageMapper.insert(any(WebPage.class))).thenAnswer(invocation -> {
            WebPage page = invocation.getArgument(0);
            page.setId(1L);
            return 1;
        });

        // When - 执行爬取
        orchestrator.execute(task);

        // Then - 验证结果
        assertEquals("COMPLETED", task.getStatus());
        assertEquals(3, task.getSuccessPages());
        assertEquals(0, task.getFailedPages());
        assertNotNull(task.getCompletedAt());

        // 验证调用了 webScraper.scrape 至少 3 次（首页 + 2个子页面）
        try {
            verify(webScraper, atLeast(3)).scrape(any());
        } catch (ScrapingException e) {
            fail("Verification failed", e);
        }

        // 验证插入了 3 个页面记录
        ArgumentCaptor<WebPage> pageCaptor = ArgumentCaptor.forClass(WebPage.class);
        verify(pageMapper, atLeast(3)).insert(pageCaptor.capture());

        List<WebPage> capturedPages = pageCaptor.getAllValues();
        assertTrue(capturedPages.stream().anyMatch(p -> "https://example.com".equals(p.getUrl())));
        assertTrue(capturedPages.stream().anyMatch(p -> "https://example.com/page1".equals(p.getUrl())));
        assertTrue(capturedPages.stream().anyMatch(p -> "https://example.com/page2".equals(p.getUrl())));

        // 验证任务状态被更新多次（至少开始、进度更新、完成）
        verify(taskMapper, atLeast(2)).updateById(any(WebCrawlTask.class));
    }

    @Test
    @DisplayName("应该能够处理爬取失败的情况")
    void testHandleScrapingFailure() {
        // Given
        WebCrawlTask task = new WebCrawlTask();
        task.setId(1L);
        task.setStartUrl("https://example.com");
        task.setUrlPattern(null);
        task.setMaxDepth(1);
        task.setCrawlStrategy("BFS");
        task.setConcurrentLimit(1);
        task.setStatus("PENDING");
        task.setTotalPages(0);
        task.setSuccessPages(0);
        task.setFailedPages(0);

        try {
            when(webScraper.scrape("https://example.com"))
                    .thenThrow(new ScrapingException("Network error"));
        } catch (ScrapingException e) {
            fail("Mock setup failed", e);
        }
        when(taskMapper.updateById(any(WebCrawlTask.class))).thenReturn(1);
        when(pageMapper.insert(any(WebPage.class))).thenAnswer(invocation -> {
            WebPage page = invocation.getArgument(0);
            page.setId(1L);
            return 1;
        });

        // When
        orchestrator.execute(task);

        // Then
        assertEquals("COMPLETED", task.getStatus());
        assertEquals(0, task.getSuccessPages());
        assertEquals(1, task.getFailedPages());

        // 验证插入了失败的页面记录
        ArgumentCaptor<WebPage> pageCaptor = ArgumentCaptor.forClass(WebPage.class);
        verify(pageMapper).insert(pageCaptor.capture());

        WebPage failedPage = pageCaptor.getValue();
        assertEquals("https://example.com", failedPage.getUrl());
        assertEquals("FAILED", failedPage.getStatus());
        assertNotNull(failedPage.getErrorMessage());
    }

    @Test
    @DisplayName("应该能够执行 DFS 爬取策略")
    void testExecuteDFSStrategy() {
        // Given
        WebCrawlTask task = new WebCrawlTask();
        task.setId(1L);
        task.setStartUrl("https://example.com");
        task.setUrlPattern("https://example.com/.*");
        task.setMaxDepth(2);
        task.setCrawlStrategy("DFS");
        task.setConcurrentLimit(1);
        task.setStatus("PENDING");
        task.setTotalPages(0);
        task.setSuccessPages(0);
        task.setFailedPages(0);

        ScrapedResult result1 = ScrapedResult.builder()
                .url("https://example.com")
                .title("Home")
                .content("Welcome")
                .links(Arrays.asList(
                        "https://example.com/page1",
                        "https://example.com/page2"
                ))
                .build();

        ScrapedResult result2 = ScrapedResult.builder()
                .url("https://example.com/page1")
                .title("Page 1")
                .content("Content 1")
                .links(Collections.emptyList())
                .build();

        try {
            when(webScraper.scrape("https://example.com")).thenReturn(result1);
            when(webScraper.scrape("https://example.com/page1")).thenReturn(result2);
        } catch (ScrapingException e) {
            fail("Mock setup failed", e);
        }

        when(taskMapper.updateById(any(WebCrawlTask.class))).thenReturn(1);
        when(pageMapper.insert(any(WebPage.class))).thenAnswer(invocation -> {
            WebPage page = invocation.getArgument(0);
            page.setId(1L);
            return 1;
        });

        // When
        orchestrator.execute(task);

        // Then
        assertEquals("COMPLETED", task.getStatus());
        assertTrue(task.getSuccessPages() >= 2);
    }

    @Test
    @DisplayName("应该能够限制爬取深度")
    void testRespectMaxDepth() {
        // Given
        WebCrawlTask task = new WebCrawlTask();
        task.setId(1L);
        task.setStartUrl("https://example.com");
        task.setUrlPattern("https://example.com/.*");
        task.setMaxDepth(1); // 只爬取深度 1
        task.setCrawlStrategy("BFS");
        task.setConcurrentLimit(1);
        task.setStatus("PENDING");
        task.setTotalPages(0);
        task.setSuccessPages(0);
        task.setFailedPages(0);

        ScrapedResult result1 = ScrapedResult.builder()
                .url("https://example.com")
                .title("Home")
                .content("Welcome")
                .links(Arrays.asList(
                        "https://example.com/page1",
                        "https://example.com/page2"
                ))
                .build();

        try {
            when(webScraper.scrape(any())).thenReturn(result1);
        } catch (ScrapingException e) {
            fail("Mock setup failed", e);
        }
        when(taskMapper.updateById(any(WebCrawlTask.class))).thenReturn(1);
        when(pageMapper.insert(any(WebPage.class))).thenAnswer(invocation -> {
            WebPage page = invocation.getArgument(0);
            page.setId(1L);
            return 1;
        });

        // When
        orchestrator.execute(task);

        // Then - 应该只爬取首页，因为 maxDepth=1
        assertEquals("COMPLETED", task.getStatus());
        assertEquals(1, task.getSuccessPages());
        try {
            verify(webScraper, times(1)).scrape(any());
        } catch (ScrapingException e) {
            fail("Verification failed", e);
        }
    }

    @Test
    @DisplayName("应该能够使用 URL 过滤器")
    void testUseUrlFilter() {
        // Given
        WebCrawlTask task = new WebCrawlTask();
        task.setId(1L);
        task.setStartUrl("https://example.com");
        task.setUrlPattern("https://example.com/docs/.*"); // 只爬取 docs 路径
        task.setMaxDepth(2);
        task.setCrawlStrategy("BFS");
        task.setConcurrentLimit(1);
        task.setStatus("PENDING");
        task.setTotalPages(0);
        task.setSuccessPages(0);
        task.setFailedPages(0);

        ScrapedResult result1 = ScrapedResult.builder()
                .url("https://example.com")
                .title("Home")
                .content("Welcome")
                .links(Arrays.asList(
                        "https://example.com/docs/page1",  // 应该被爬取
                        "https://example.com/about"        // 应该被过滤
                ))
                .build();

        ScrapedResult result2 = ScrapedResult.builder()
                .url("https://example.com/docs/page1")
                .title("Docs Page 1")
                .content("Docs Content")
                .links(Collections.emptyList())
                .build();

        try {
            when(webScraper.scrape("https://example.com")).thenReturn(result1);
            when(webScraper.scrape("https://example.com/docs/page1")).thenReturn(result2);
        } catch (ScrapingException e) {
            fail("Mock setup failed", e);
        }

        when(taskMapper.updateById(any(WebCrawlTask.class))).thenReturn(1);
        when(pageMapper.insert(any(WebPage.class))).thenAnswer(invocation -> {
            WebPage page = invocation.getArgument(0);
            page.setId(1L);
            return 1;
        });

        // When
        orchestrator.execute(task);

        // Then - 爬取首页和匹配 pattern 的 docs 页面
        assertEquals("COMPLETED", task.getStatus());
        assertEquals(2, task.getSuccessPages());
        try {
            verify(webScraper, times(2)).scrape(any());
        } catch (ScrapingException e) {
            fail("Verification failed", e);
        }
    }

    @Test
    @DisplayName("应该能够更新任务进度")
    void testUpdateProgress() {
        // Given
        WebCrawlTask task = new WebCrawlTask();
        task.setId(1L);
        task.setStartUrl("https://example.com");
        task.setUrlPattern(null);
        task.setMaxDepth(2);
        task.setCrawlStrategy("BFS");
        task.setConcurrentLimit(2);
        task.setStatus("PENDING");
        task.setTotalPages(0);
        task.setSuccessPages(0);
        task.setFailedPages(0);

        ScrapedResult result1 = ScrapedResult.builder()
                .url("https://example.com")
                .title("Home")
                .content("Welcome")
                .links(Arrays.asList(
                        "https://example.com/page1",
                        "https://example.com/page2"
                ))
                .build();

        ScrapedResult result2 = ScrapedResult.builder()
                .url("https://example.com/page1")
                .title("Page 1")
                .content("Content 1")
                .links(Collections.emptyList())
                .build();

        try {
            when(webScraper.scrape(any())).thenReturn(result1, result2);
        } catch (ScrapingException e) {
            fail("Mock setup failed", e);
        }

        when(taskMapper.updateById(any(WebCrawlTask.class))).thenReturn(1);
        when(pageMapper.insert(any(WebPage.class))).thenAnswer(invocation -> {
            WebPage page = invocation.getArgument(0);
            page.setId(1L);
            return 1;
        });

        // When
        orchestrator.execute(task);

        // Then - 验证任务状态被多次更新
        verify(taskMapper, atLeast(2)).updateById(any(WebCrawlTask.class));

        // 验证最终状态是 COMPLETED
        assertEquals("COMPLETED", task.getStatus());
        assertNotNull(task.getStartedAt());
        assertNotNull(task.getCompletedAt());
    }
}
