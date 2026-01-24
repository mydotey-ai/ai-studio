package com.mydotey.ai.studio.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {
    @Email(message = "Invalid email format")
    private String email;

    @Size(max = 500, message = "Bio must be at most 500 characters")
    private String bio;

    private String language;

    private String timezone;
}
