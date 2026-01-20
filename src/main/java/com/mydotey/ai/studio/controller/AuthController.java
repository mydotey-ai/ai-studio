package com.mydotey.ai.studio.controller;

import com.mydotey.ai.studio.annotation.AuditLog;
import com.mydotey.ai.studio.common.ApiResponse;
import com.mydotey.ai.studio.dto.LoginRequest;
import com.mydotey.ai.studio.dto.LoginResponse;
import com.mydotey.ai.studio.dto.RefreshTokenRequest;
import com.mydotey.ai.studio.dto.RegisterRequest;
import com.mydotey.ai.studio.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "认证", description = "用户认证和授权相关接口")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @AuditLog(action = "USER_LOGIN", resourceType = "User")
    @Operation(summary = "用户登录", description = "使用用户名和密码登录系统")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "登录成功")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "用户名或密码错误")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ApiResponse.success(response);
    }

    @PostMapping("/register")
    @AuditLog(action = "USER_REGISTER", resourceType = "User")
    @Operation(summary = "用户注册", description = "创建新用户账号")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "注册成功")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数错误")
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ApiResponse.success("Registration successful", null);
    }

    @PostMapping("/refresh")
    @AuditLog(action = "TOKEN_REFRESH", resourceType = "User")
    @Operation(summary = "刷新访问令牌", description = "使用刷新令牌获取新的访问令牌")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "刷新成功")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "刷新令牌无效或已过期")
    public ApiResponse<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        LoginResponse response = authService.refreshAccessToken(request.getRefreshToken());
        return ApiResponse.success(response);
    }

    @PostMapping("/logout")
    @AuditLog(action = "USER_LOGOUT", resourceType = "User")
    @Operation(summary = "用户登出", description = "退出登录并使刷新令牌失效")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "登出成功")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数错误")
    public ApiResponse<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.getRefreshToken());
        return ApiResponse.success("Logout successful", null);
    }
}
