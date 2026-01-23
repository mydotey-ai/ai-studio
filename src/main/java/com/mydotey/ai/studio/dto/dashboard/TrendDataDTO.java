package com.mydotey.ai.studio.dto.dashboard;

import lombok.Data;

@Data
public class TrendDataDTO {
    private String date; // YYYY-MM-DD
    private Long apiCalls;
    private Integer activeUsers;
}
