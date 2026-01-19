package com.mydotey.ai.studio.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.Instant;

@Data
@TableName("web_crawl_tasks")
public class WebCrawlTask {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long kbId;

    private String startUrl;

    private String urlPattern;

    private Integer maxDepth;

    private String crawlStrategy;

    private Integer concurrentLimit;

    private String status;

    private Integer totalPages;

    private Integer successPages;

    private Integer failedPages;

    private String errorMessage;

    private Long createdBy;

    private Instant startedAt;

    private Instant completedAt;

    private Instant createdAt;

    private Instant updatedAt;
}
