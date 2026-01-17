package com.mydotey.ai.studio.dto;

import lombok.Data;
import java.time.Instant;

@Data
public class OrganizationResponse {
    private Long id;
    private String name;
    private String description;
    private String settings;
    private Instant createdAt;
    private Instant updatedAt;
}
