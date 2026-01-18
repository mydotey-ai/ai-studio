package com.mydotey.ai.studio.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mydotey.ai.studio.handler.JsonbTypeHandler;
import lombok.Data;

import java.time.Instant;

@Data
@TableName("mcp_tools")
public class McpTool {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long serverId;
    private String toolName;
    private String description;

    @TableField(typeHandler = JsonbTypeHandler.class)
    private String inputSchema;

    @TableField(typeHandler = JsonbTypeHandler.class)
    private String outputSchema;

    @TableField(typeHandler = JsonbTypeHandler.class)
    private String metadata;

    private Instant createdAt;
    private Instant updatedAt;
}
