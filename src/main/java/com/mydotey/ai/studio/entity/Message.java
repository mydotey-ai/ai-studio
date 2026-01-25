package com.mydotey.ai.studio.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mydotey.ai.studio.handler.JsonbTypeHandler;
import lombok.Data;

import java.time.Instant;

@Data
@TableName("messages")
public class Message {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long conversationId;

    private String role;

    private String content;

    @TableField(typeHandler = JsonbTypeHandler.class)
    private String sources;

    @TableField(typeHandler = JsonbTypeHandler.class)
    private String toolCalls;

    @TableField(typeHandler = JsonbTypeHandler.class)
    private String metadata;

    private Instant createdAt;
}
