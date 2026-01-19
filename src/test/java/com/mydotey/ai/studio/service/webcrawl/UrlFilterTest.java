package com.mydotey.ai.studio.service.webcrawl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("URL 过滤器测试")
class UrlFilterTest {

    @Test
    @DisplayName("应该能够根据正则表达式过滤 URL")
    void testFilterByPattern() {
        UrlFilter urlFilter = new UrlFilter("https://example.com/docs/.*");

        List<String> urls = List.of(
                "https://example.com/docs/page1",
                "https://example.com/docs/page2",
                "https://example.com/about",
                "https://other.com/docs/page1"
        );

        List<String> filtered = urlFilter.filter(urls);

        assertEquals(2, filtered.size());
        assertTrue(filtered.contains("https://example.com/docs/page1"));
        assertTrue(filtered.contains("https://example.com/docs/page2"));
    }

    @Test
    @DisplayName("当没有设置 pattern 时应该返回所有 URL")
    void testFilterWithoutPattern() {
        UrlFilter urlFilter = new UrlFilter(null);

        List<String> urls = List.of(
                "https://example.com/page1",
                "https://example.com/page2"
        );

        List<String> filtered = urlFilter.filter(urls);

        assertEquals(2, filtered.size());
    }

    @Test
    @DisplayName("应该能够去除重复的 URL")
    void testRemoveDuplicates() {
        UrlFilter urlFilter = new UrlFilter(null);

        List<String> urls = List.of(
                "https://example.com/page1",
                "https://example.com/page1",
                "https://example.com/page2"
        );

        List<String> filtered = urlFilter.filter(urls);

        assertEquals(2, filtered.size());
    }
}
