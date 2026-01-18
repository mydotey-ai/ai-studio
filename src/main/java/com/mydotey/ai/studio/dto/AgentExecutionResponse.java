package com.mydotey.ai.studio.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AgentExecutionResponse {
    private String answer;
    private List<ThoughtStep> thoughtSteps;
    private List<ToolCallResult> toolCalls;

    @Data
    @Builder
    public static class ThoughtStep {
        private Integer step;
        private String thought;
        private String action;
        private String observation;
    }

    @Data
    @Builder
    public static class ToolCallResult {
        private String toolName;
        private String arguments;
        private String result;
        private Boolean success;
    }
}
