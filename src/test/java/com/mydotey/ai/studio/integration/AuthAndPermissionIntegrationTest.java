package com.mydotey.ai.studio.integration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mydotey.ai.studio.dto.LoginRequest;
import com.mydotey.ai.studio.dto.LoginResponse;
import com.mydotey.ai.studio.dto.RegisterRequest;
import com.mydotey.ai.studio.dto.RefreshTokenRequest;
import com.mydotey.ai.studio.entity.User;
import com.mydotey.ai.studio.mapper.UserMapper;
import com.mydotey.ai.studio.service.AuthService;
import com.mydotey.ai.studio.service.UserService;
import com.mydotey.ai.studio.util.JwtUtil;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("认证和权限集成测试")
class AuthAndPermissionIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserMapper userMapper;

    private LoginResponse adminLoginResponse;
    private LoginResponse userLoginResponse;

    private static final String TEST_USERNAME = "testuser123";

    @BeforeAll
    void cleanUpTestData() {
        // 删除测试用户（如果存在）
        User existingUser = userMapper.selectOne(
            new LambdaQueryWrapper<User>().eq(User::getUsername, TEST_USERNAME)
        );
        if (existingUser != null) {
            userMapper.deleteById(existingUser.getId());
        }
    }

    @Test
    @DisplayName("用户注册和登录流程")
    void testRegistrationAndLoginFlow() {
        // 使用 UUID 确保用户名唯一
        String username = TEST_USERNAME + "_" + System.currentTimeMillis();

        // 1. 注册新用户
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername(username);
        registerRequest.setEmail(username + "@example.com");
        registerRequest.setPassword("password123");

        assertDoesNotThrow(() -> authService.register(registerRequest));

        // 2. 登录
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(username);
        loginRequest.setPassword("password123");

        LoginResponse loginResponse = authService.login(loginRequest);

        assertNotNull(loginResponse);
        assertNotNull(loginResponse.getAccessToken());
        assertNotNull(loginResponse.getRefreshToken());
        assertEquals(username, loginResponse.getUser().getUsername());
    }

    @Test
    @DisplayName("Token 刷新流程")
    void testTokenRefreshFlow() {
        // 注册并登录用户
        String username = TEST_USERNAME + "_refresh_" + System.currentTimeMillis();

        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername(username);
        registerRequest.setEmail(username + "@example.com");
        registerRequest.setPassword("password123");

        assertDoesNotThrow(() -> authService.register(registerRequest));

        // 登录获取 token
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(username);
        loginRequest.setPassword("password123");

        LoginResponse loginResponse = authService.login(loginRequest);
        String refreshToken = loginResponse.getRefreshToken();

        assertNotNull(refreshToken);

        // 2. 使用 refresh token 刷新 access token
        LoginResponse refreshResponse = authService.refreshAccessToken(refreshToken);

        assertNotNull(refreshResponse);
        assertNotNull(refreshResponse.getAccessToken());
        assertNotNull(refreshResponse.getRefreshToken());
        // 新的 refresh token 应该与旧的不同
        assertNotEquals(refreshToken, refreshResponse.getRefreshToken());
    }

    @Test
    @DisplayName("用户名不存在时应抛出异常")
    void testLoginWithNonExistentUser() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("nonexistentuser");
        loginRequest.setPassword("password123");

        assertThrows(Exception.class, () -> authService.login(loginRequest));
    }

    @Test
    @DisplayName("密码错误时应抛出异常")
    void testLoginWithWrongPassword() {
        String username = TEST_USERNAME + "_wrongpwd_" + System.currentTimeMillis();

        // 先注册用户
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername(username);
        registerRequest.setEmail(username + "@example.com");
        registerRequest.setPassword("password123");

        assertDoesNotThrow(() -> authService.register(registerRequest));

        // 使用错误密码登录
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(username);
        loginRequest.setPassword("wrongpassword");

        assertThrows(Exception.class, () -> authService.login(loginRequest));
    }

    @Test
    @DisplayName("无效的 refresh token 应该被拒绝")
    void testInvalidRefreshToken() {
        String invalidToken = "invalid-refresh-token";

        assertThrows(Exception.class, () -> authService.refreshAccessToken(invalidToken));
    }

    @Test
    @DisplayName("JWT token 应该包含正确的用户信息")
    void testJwtTokenContainsUserInfo() {
        String username = TEST_USERNAME + "_jwt_" + System.currentTimeMillis();

        // 注册并登录用户
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername(username);
        registerRequest.setEmail(username + "@example.com");
        registerRequest.setPassword("password123");

        assertDoesNotThrow(() -> authService.register(registerRequest));

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(username);
        loginRequest.setPassword("password123");

        LoginResponse loginResponse = authService.login(loginRequest);
        String accessToken = loginResponse.getAccessToken();

        assertNotNull(accessToken);

        // 解析 token 验证内容
        String tokenUsername = jwtUtil.getUsernameFromToken(accessToken);
        Long userId = jwtUtil.getUserIdFromToken(accessToken);
        String role = jwtUtil.getRoleFromToken(accessToken);

        assertEquals(username, tokenUsername);
        assertNotNull(userId);
        assertEquals("USER", role);
    }
}
