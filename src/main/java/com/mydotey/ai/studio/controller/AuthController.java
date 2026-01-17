package com.mydotey.ai.studio.controller;

import com.mydotey.ai.studio.annotation.AuditLog;
import com.mydotey.ai.studio.common.ApiResponse;
import com.mydotey.ai.studio.dto.LoginRequest;
import com.mydotey.ai.studio.dto.LoginResponse;
import com.mydotey.ai.studio.dto.RefreshTokenRequest;
import com.mydotey.ai.studio.dto.RegisterRequest;
import com.mydotey.ai.studio.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @AuditLog(action = "USER_LOGIN", resourceType = "User")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ApiResponse.success(response);
    }

    @PostMapping("/register")
    @AuditLog(action = "USER_REGISTER", resourceType = "User")
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ApiResponse.success("Registration successful", null);
    }

    @PostMapping("/refresh")
    @AuditLog(action = "TOKEN_REFRESH", resourceType = "User")
    public ApiResponse<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        LoginResponse response = authService.refreshAccessToken(request.getRefreshToken());
        return ApiResponse.success(response);
    }

    @PostMapping("/logout")
    @AuditLog(action = "USER_LOGOUT", resourceType = "User")
    public ApiResponse<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.getRefreshToken());
        return ApiResponse.success("Logout successful", null);
    }
}
