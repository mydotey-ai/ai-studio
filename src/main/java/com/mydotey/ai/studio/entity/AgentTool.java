package com.mydotey.ai.studio.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.Instant;

@Data
@TableName("agent_tools")
public class AgentTool {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long agentId;
    private Long toolId;
    private Instant createdAt;
}
