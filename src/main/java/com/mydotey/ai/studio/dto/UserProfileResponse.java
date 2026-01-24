package com.mydotey.ai.studio.dto;

import lombok.Data;
import java.time.Instant;

@Data
public class UserProfileResponse {
    private Long id;
    private String username;
    private String email;
    private String role;
    private String bio;
    private String language;
    private String timezone;
    private Instant createdAt;
    private Instant updatedAt;
}
