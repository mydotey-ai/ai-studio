package com.mydotey.ai.studio.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.Instant;

@Data
@TableName("agent_knowledge_bases")
public class AgentKnowledgeBase {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long agentId;
    private Long kbId;
    private Instant createdAt;
}
