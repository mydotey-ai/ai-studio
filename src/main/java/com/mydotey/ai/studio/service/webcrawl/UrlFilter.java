package com.mydotey.ai.studio.service.webcrawl;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

@Slf4j
public class UrlFilter {

    private final Pattern pattern;

    public UrlFilter(String urlPattern) {
        if (urlPattern != null && !urlPattern.isBlank()) {
            try {
                this.pattern = Pattern.compile(urlPattern);
            } catch (PatternSyntaxException e) {
                log.error("Invalid URL pattern: {}", urlPattern, e);
                throw new IllegalArgumentException("Invalid URL pattern: " + urlPattern, e);
            }
        } else {
            this.pattern = null;
        }
    }

    /**
     * 过滤 URL 列表
     * 1. 根据正则表达式过滤
     * 2. 去重
     */
    public List<String> filter(List<String> urls) {
        List<String> result = urls;

        // 应用正则表达式过滤
        if (pattern != null) {
            result = result.stream()
                    .filter(url -> pattern.matcher(url).matches())
                    .collect(Collectors.toList());
        }

        // 去重
        return result.stream()
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 检查单个 URL 是否匹配
     */
    public boolean matches(String url) {
        if (pattern == null) {
            return true;
        }
        return pattern.matcher(url).matches();
    }
}
