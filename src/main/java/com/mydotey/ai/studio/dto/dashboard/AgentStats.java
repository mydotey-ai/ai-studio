package com.mydotey.ai.studio.dto.dashboard;

import lombok.Data;

@Data
public class AgentStats {
    private Long totalCount;
    private Long reactCount;
    private Long workflowCount;
    private Long monthlyNewCount;
}
