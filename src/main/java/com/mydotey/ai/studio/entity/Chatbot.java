package com.mydotey.ai.studio.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mydotey.ai.studio.handler.JsonbTypeHandler;
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

    @TableField(typeHandler = JsonbTypeHandler.class)
    private String settings;

    @TableField(typeHandler = JsonbTypeHandler.class)
    private String styleConfig;

    private Boolean isPublished;

    private Long accessCount;

    private Instant createdAt;

    private Instant updatedAt;
}
