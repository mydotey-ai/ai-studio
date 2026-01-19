package com.mydotey.ai.studio.service.webcrawl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class JsoupWebScraper implements WebScraper {

    private static final int TIMEOUT_MS = 10000;
    private static final String USER_AGENT = "Mozilla/5.0 (compatible; AI-Studio-Bot/1.0)";

    @Override
    public ScrapedResult scrape(String url) throws ScrapingException {
        try {
            log.info("Scraping URL: {}", url);

            // 使用 Jsoup 连接并获取文档
            Document doc = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT_MS)
                    .followRedirects(true)
                    .get();

            // 提取标题
            String title = doc.title();

            // 提取主要内容
            String content = extractMainContent(doc);

            // 提取链接
            List<String> links = extractLinks(doc.html(), url);

            log.info("Successfully scraped URL: {}, title: {}, links count: {}",
                    url, title, links.size());

            return ScrapedResult.builder()
                    .url(url)
                    .title(title)
                    .content(content)
                    .links(links)
                    .build();

        } catch (Exception e) {
            log.error("Failed to scrape URL: {}", url, e);
            throw new ScrapingException("Failed to scrape URL: " + url, e);
        }
    }

    @Override
    public List<String> extractLinks(String html, String baseUrl) {
        List<String> links = new ArrayList<>();

        try {
            Document doc = Jsoup.parse(html, baseUrl);
            Elements linkElements = doc.select("a[href]");

            URL base = new URL(baseUrl);

            for (Element link : linkElements) {
                String href = link.attr("abs:href");
                if (href != null && !href.isEmpty() && isValidUrl(href)) {
                    // 只收集同域名下的链接
                    if (isSameDomain(href, base)) {
                        links.add(href);
                    }
                }
            }

            log.debug("Extracted {} links from {}", links.size(), baseUrl);

        } catch (Exception e) {
            log.error("Failed to extract links from: {}", baseUrl, e);
        }

        return links;
    }

    /**
     * 提取主要内容
     */
    private String extractMainContent(Document doc) {
        // 移除不需要的标签
        doc.select("script, style, nav, footer, header, aside").remove();

        // 尝试找到主要内容区域
        Element mainContent = doc.selectFirst("main, article, #content, .content");

        if (mainContent != null) {
            return mainContent.text();
        }

        // 如果没有找到主要内容区域，使用 body
        return doc.body() != null ? doc.body().text() : "";
    }

    /**
     * 验证 URL 是否有效
     */
    private boolean isValidUrl(String url) {
        try {
            new URL(url);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 检查是否为同一域名
     */
    private boolean isSameDomain(String url, URL base) {
        try {
            URL target = new URL(url);
            return target.getHost().equals(base.getHost());
        } catch (Exception e) {
            return false;
        }
    }
}
