package com.mydotey.ai.studio.dto.dashboard;

import lombok.Data;

import java.time.Instant;

@Data
public class ActivityDTO {
    private Long id;
    private String action;
    private String resourceType;
    private String username;
    private Instant createdAt;
}
