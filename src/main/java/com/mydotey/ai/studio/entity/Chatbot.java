package com.mydotey.ai.studio.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.Instant;

@Data
@TableName("chatbots")
public class Chatbot {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long agentId;

    private String name;

    private String description;

    private String welcomeMessage;

    private String avatarUrl;

    private Long ownerId;

    private String settings;

    private String styleConfig;

    private Boolean isPublished;

    private Long accessCount;

    private Instant createdAt;

    private Instant updatedAt;
}
