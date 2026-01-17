package com.mydotey.ai.studio.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserRequest {
    @Email(message = "Invalid email format")
    private String email;

    private String avatarUrl;

    @Size(min = 6, max = 50, message = "Password must be between 6 and 50 characters")
    private String newPassword;

    private String currentPassword;
}
