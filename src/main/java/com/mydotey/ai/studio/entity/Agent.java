package com.mydotey.ai.studio.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mydotey.ai.studio.handler.JsonbTypeHandler;
import lombok.Data;

import java.time.Instant;

@Data
@TableName("agents")
public class Agent {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orgId;
    private String name;
    private String description;
    private String systemPrompt;
    private Long ownerId;
    private Boolean isPublic;

    @TableField(typeHandler = JsonbTypeHandler.class)
    private String modelConfig;

    private String workflowType;

    @TableField(typeHandler = JsonbTypeHandler.class)
    private String workflowConfig;

    private Integer maxIterations;
    private Instant createdAt;
    private Instant updatedAt;
}
