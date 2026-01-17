package com.mydotey.ai.studio.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private UserInfo user;

    public LoginResponse(String accessToken, String refreshToken, Long id, String username, String email, String role) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.user = new UserInfo(id, username, email, role);
    }

    @Data
    @AllArgsConstructor
    public static class UserInfo {
        private Long id;
        private String username;
        private String email;
        private String role;
    }
}
