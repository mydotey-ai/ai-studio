package com.mydotey.ai.studio.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.Instant;

@Data
@TableName("web_pages")
public class WebPage {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long crawlTaskId;

    private Long documentId;

    private String url;

    private String title;

    private String status;

    private String errorMessage;

    private Integer depth;

    private Instant createdAt;

    private Instant updatedAt;
}
