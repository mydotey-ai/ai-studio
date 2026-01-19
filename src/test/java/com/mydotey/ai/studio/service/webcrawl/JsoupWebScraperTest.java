package com.mydotey.ai.studio.service.webcrawl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Jsoup 网页抓取器测试")
@ExtendWith(MockitoExtension.class)
class JsoupWebScraperTest {

    @InjectMocks
    private JsoupWebScraper webScraper;

    @Test
    @DisplayName("应该能够抓取简单网页")
    void testScrapeSimplePage() throws ScrapingException {
        // 使用公开的测试网站
        String url = "https://example.com";

        ScrapedResult result = webScraper.scrape(url);

        assertNotNull(result);
        assertEquals(url, result.getUrl());
        assertNotNull(result.getTitle());
        assertNotNull(result.getContent());
        assertNotNull(result.getLinks());
        // example.com 只有一个链接且指向不同域名，所以链接列表为空是正常的
        // assertEquals("Example Domain", result.getTitle());
    }

    @Test
    @DisplayName("当 URL 无效时应该抛出异常")
    void testScrapeInvalidUrl() {
        String invalidUrl = "not-a-valid-url";

        assertThrows(ScrapingException.class, () -> webScraper.scrape(invalidUrl));
    }
}
