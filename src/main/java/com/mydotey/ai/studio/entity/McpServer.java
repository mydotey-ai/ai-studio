package com.mydotey.ai.studio.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.Instant;

@Data
@TableName("mcp_servers")
public class McpServer {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String description;
    private String connectionType;
    private String command;
    private String workingDir;
    private String endpointUrl;
    private String headers;
    private String authType;
    private String authConfig;
    private String status;
    private Long createdBy;
    private Instant createdAt;
    private Instant updatedAt;
}
