package com.mydotey.ai.studio.service.webcrawl;

import java.util.List;

public interface WebScraper {
    /**
     * 抓取单个网页
     * @param url 网页 URL
     * @return 抓取结果
     */
    ScrapedResult scrape(String url) throws ScrapingException;

    /**
     * 从 HTML 中提取链接
     * @param html HTML 内容
     * @param baseUrl 基础 URL
     * @return 链接列表
     */
    List<String> extractLinks(String html, String baseUrl);
}
