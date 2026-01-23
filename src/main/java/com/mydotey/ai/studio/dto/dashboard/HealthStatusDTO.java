package com.mydotey.ai.studio.dto.dashboard;

import lombok.Data;

@Data
public class HealthStatusDTO {
    private Double apiSuccessRate;
    private Long avgResponseTime; // 毫秒
    private Double errorRate;
}
