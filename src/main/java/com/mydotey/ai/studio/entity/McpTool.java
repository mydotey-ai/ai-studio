package com.mydotey.ai.studio.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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
    private String inputSchema;
    private String outputSchema;
    private String metadata;
    private Instant createdAt;
    private Instant updatedAt;
}
