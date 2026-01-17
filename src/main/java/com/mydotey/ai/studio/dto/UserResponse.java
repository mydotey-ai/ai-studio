package com.mydotey.ai.studio.dto;

import lombok.Data;
import java.time.Instant;

@Data
public class UserResponse {
    private Long id;
    private Long orgId;
    private String username;
    private String email;
    private String role;
    private String status;
    private String avatarUrl;
    private Instant lastLoginAt;
    private Instant createdAt;
    private Instant updatedAt;
}
