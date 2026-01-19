package com.mydotey.ai.studio.service.webcrawl;

import com.mydotey.ai.studio.entity.WebCrawlTask;
import com.mydotey.ai.studio.entity.WebPage;
import com.mydotey.ai.studio.mapper.WebCrawlTaskMapper;
import com.mydotey.ai.studio.mapper.WebPageMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

/**
 * 爬取编排器
 * 负责协调爬取任务的执行，支持 BFS 和 DFS 两种策略
 */
@Slf4j
@Component
@AllArgsConstructor
public class CrawlOrchestrator {

    private final WebScraper webScraper;
    private final WebCrawlTaskMapper taskMapper;
    private final WebPageMapper pageMapper;

    /**
     * 执行爬取任务
     * @param task 爬取任务
     */
    public void execute(WebCrawlTask task) {
        log.info("Starting crawl task: id={}, url={}, strategy={}",
                task.getId(), task.getStartUrl(), task.getCrawlStrategy());

        // 更新任务状态为运行中
        task.setStatus("RUNNING");
        task.setStartedAt(Instant.now());
        task.setUpdatedAt(Instant.now());
        taskMapper.updateById(task);

        try {
            // 创建 URL 过滤器
            UrlFilter urlFilter = new UrlFilter(task.getUrlPattern());

            // 根据策略执行爬取
            if ("DFS".equalsIgnoreCase(task.getCrawlStrategy())) {
                executeDFS(task, urlFilter);
            } else {
                // 默认使用 BFS
                executeBFS(task, urlFilter);
            }

            // 更新任务状态为完成
            task.setStatus("COMPLETED");
            task.setCompletedAt(Instant.now());
            task.setUpdatedAt(Instant.now());
            taskMapper.updateById(task);

            log.info("Completed crawl task: id={}, success={}, failed={}",
                    task.getId(), task.getSuccessPages(), task.getFailedPages());

        } catch (Exception e) {
            log.error("Error executing crawl task: id=" + task.getId(), e);
            task.setStatus("FAILED");
            task.setErrorMessage(e.getMessage());
            task.setCompletedAt(Instant.now());
            task.setUpdatedAt(Instant.now());
            taskMapper.updateById(task);
        }
    }

    /**
     * 执行 BFS 策略爬取
     */
    private void executeBFS(WebCrawlTask task, UrlFilter urlFilter) {
        int concurrentLimit = task.getConcurrentLimit() != null ? task.getConcurrentLimit() : 1;
        ExecutorService executorService = Executors.newFixedThreadPool(concurrentLimit);

        try {
            // 使用队列实现 BFS
            Queue<UrlTask> queue = new LinkedList<>();
            Set<String> visited = ConcurrentHashMap.newKeySet();

            // 添加起始 URL
            queue.offer(new UrlTask(task.getStartUrl(), 0));
            visited.add(task.getStartUrl());

            List<Future<?>> futures = new ArrayList<>();

            while (!queue.isEmpty() || !futures.isEmpty()) {
                // 提交任务到线程池
                while (!queue.isEmpty() && futures.size() < concurrentLimit * 2) {
                    UrlTask urlTask = queue.poll();
                    if (urlTask != null) {
                        Future<?> future = executorService.submit(() -> {
                            crawlPage(task, urlTask.url, urlTask.depth, urlFilter, visited, queue);
                        });
                        futures.add(future);
                    }
                }

                // 等待至少一个任务完成
                if (!futures.isEmpty()) {
                    futures.removeIf(future -> {
                        if (future.isDone()) {
                            try {
                                future.get();
                            } catch (InterruptedException | ExecutionException e) {
                                log.error("Error in crawl task", e);
                            }
                            return true;
                        }
                        return false;
                    });
                }

                // 更新进度
                if (!queue.isEmpty() || !futures.isEmpty()) {
                    task.setTotalPages(visited.size());
                    task.setUpdatedAt(Instant.now());
                    taskMapper.updateById(task);
                }
            }

        } finally {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 执行 DFS 策略爬取
     */
    private void executeDFS(WebCrawlTask task, UrlFilter urlFilter) {
        int concurrentLimit = task.getConcurrentLimit() != null ? task.getConcurrentLimit() : 1;
        ExecutorService executorService = Executors.newFixedThreadPool(concurrentLimit);

        try {
            Set<String> visited = ConcurrentHashMap.newKeySet();
            Stack<UrlTask> stack = new Stack<>();

            // 添加起始 URL
            stack.push(new UrlTask(task.getStartUrl(), 0));

            while (!stack.isEmpty()) {
                UrlTask urlTask = stack.pop();

                if (visited.contains(urlTask.url)) {
                    continue;
                }

                visited.add(urlTask.url);

                Future<Void> future = executorService.submit(() -> {
                    List<String> links = crawlPage(task, urlTask.url, urlTask.depth, urlFilter, visited, null);

                    // DFS: 将发现的链接压入栈中（后进先出）
                    if (links != null && !links.isEmpty()) {
                        // 反转列表以保持顺序（因为栈是后进先出）
                        Collections.reverse(links);
                        for (String link : links) {
                            if (!visited.contains(link)) {
                                stack.push(new UrlTask(link, urlTask.depth + 1));
                            }
                        }
                    }

                    return null;
                });

                // 等待当前任务完成（DFS 是顺序的）
                try {
                    future.get();
                } catch (InterruptedException | ExecutionException e) {
                    log.error("Error in DFS crawl task", e);
                }

                // 更新进度
                task.setTotalPages(visited.size());
                task.setUpdatedAt(Instant.now());
                taskMapper.updateById(task);
            }

        } finally {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 爬取单个页面
     * @return 提取的链接列表
     */
    private List<String> crawlPage(WebCrawlTask task, String url, int depth,
                                   UrlFilter urlFilter, Set<String> visited, Queue<UrlTask> queue) {
        try {
            log.debug("Crawling: url={}, depth={}", url, depth);

            // 检查深度限制
            if (depth >= task.getMaxDepth()) {
                log.debug("Skipping URL due to depth limit: url={}, depth={}", url, depth);
                return Collections.emptyList();
            }

            // 爬取页面
            ScrapedResult result = webScraper.scrape(url);

            // 保存页面记录
            WebPage page = new WebPage();
            page.setCrawlTaskId(task.getId());
            page.setUrl(result.getUrl());
            page.setTitle(result.getTitle());
            page.setStatus("SUCCESS");
            page.setDepth(depth);
            page.setCreatedAt(Instant.now());
            page.setUpdatedAt(Instant.now());
            pageMapper.insert(page);

            // 更新成功计数
            synchronized (task) {
                task.setSuccessPages(task.getSuccessPages() + 1);
            }

            // 提取并过滤链接
            List<String> links = result.getLinks();
            if (links == null || links.isEmpty()) {
                return Collections.emptyList();
            }

            // 过滤链接
            List<String> filteredLinks = urlFilter.filter(links);

            // 对于 BFS，将符合条件的链接加入队列
            if (queue != null) {
                for (String link : filteredLinks) {
                    if (!visited.contains(link)) {
                        visited.add(link);
                        queue.offer(new UrlTask(link, depth + 1));
                    }
                }
            }

            return filteredLinks;

        } catch (ScrapingException e) {
            log.error("Failed to scrape: url={}", url, e);

            // 保存失败的页面记录
            WebPage page = new WebPage();
            page.setCrawlTaskId(task.getId());
            page.setUrl(url);
            page.setStatus("FAILED");
            page.setErrorMessage(e.getMessage());
            page.setDepth(depth);
            page.setCreatedAt(Instant.now());
            page.setUpdatedAt(Instant.now());
            pageMapper.insert(page);

            // 更新失败计数
            synchronized (task) {
                task.setFailedPages(task.getFailedPages() + 1);
            }

            return Collections.emptyList();

        } catch (Exception e) {
            log.error("Unexpected error crawling: url={}", url, e);

            // 保存失败的页面记录
            WebPage page = new WebPage();
            page.setCrawlTaskId(task.getId());
            page.setUrl(url);
            page.setStatus("FAILED");
            page.setErrorMessage(e.getMessage());
            page.setDepth(depth);
            page.setCreatedAt(Instant.now());
            page.setUpdatedAt(Instant.now());
            pageMapper.insert(page);

            // 更新失败计数
            synchronized (task) {
                task.setFailedPages(task.getFailedPages() + 1);
            }

            return Collections.emptyList();
        }
    }

    /**
     * URL 任务，包含 URL 和深度信息
     */
    @AllArgsConstructor
    private static class UrlTask {
        String url;
        int depth;
    }
}
