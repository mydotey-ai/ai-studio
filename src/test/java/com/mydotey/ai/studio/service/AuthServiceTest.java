package com.mydotey.ai.studio.service;

import com.mydotey.ai.studio.TestBase;
import com.mydotey.ai.studio.dto.LoginRequest;
import com.mydotey.ai.studio.dto.LoginResponse;
import com.mydotey.ai.studio.dto.RegisterRequest;
import com.mydotey.ai.studio.entity.User;
import com.mydotey.ai.studio.mapper.UserMapper;
import com.mydotey.ai.studio.util.PasswordUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AuthServiceTest extends TestBase {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordUtil passwordUtil;

    private final List<String> createdUsernames = new ArrayList<>();

    @AfterEach
    void cleanup() {
        // 清理本次测试创建的所有用户数据
        for (String username : createdUsernames) {
            userMapper.delete(new LambdaQueryWrapper<User>()
                    .eq(User::getUsername, username));
        }
        createdUsernames.clear();
    }

    private String generateUniqueUsername(String prefix) {
        String username = prefix + "_" + System.currentTimeMillis();
        createdUsernames.add(username);
        return username;
    }

    @Test
    void testRegisterUser() {
        String username = generateUniqueUsername("testuser");
        RegisterRequest request = new RegisterRequest();
        request.setUsername(username);
        request.setEmail(username + "@example.com");
        request.setPassword("password123");

        authService.register(request);

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        assertNotNull(user);
        assertEquals(username, user.getUsername());
        assertEquals(username + "@example.com", user.getEmail());
        assertTrue(passwordUtil.matches("password123", user.getPasswordHash()));
        assertEquals("USER", user.getRole());
    }

    @Test
    void testRegisterDuplicateUsername() {
        String username = generateUniqueUsername("duplicate_test");
        RegisterRequest request = new RegisterRequest();
        request.setUsername(username);
        request.setEmail(username + "@example.com");
        request.setPassword("password123");

        authService.register(request);

        RegisterRequest duplicateRequest = new RegisterRequest();
        duplicateRequest.setUsername(username);
        duplicateRequest.setEmail("different@example.com");
        duplicateRequest.setPassword("password456");

        assertThrows(com.mydotey.ai.studio.common.exception.BusinessException.class,
                () -> authService.register(duplicateRequest));
    }

    @Test
    void testLoginSuccess() {
        String username = generateUniqueUsername("login_test");
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername(username);
        registerRequest.setEmail(username + "@example.com");
        registerRequest.setPassword("password123");
        authService.register(registerRequest);

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(username);
        loginRequest.setPassword("password123");

        LoginResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertNotNull(response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        assertEquals(username, user.getUsername());
    }

    @Test
    void testLoginInvalidCredentials() {
        String username = generateUniqueUsername("invalid_test");
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername(username);
        registerRequest.setEmail(username + "@example.com");
        registerRequest.setPassword("password123");
        authService.register(registerRequest);

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(username);
        loginRequest.setPassword("wrongpassword");

        assertThrows(com.mydotey.ai.studio.common.exception.BusinessException.class,
                () -> authService.login(loginRequest));
    }
}
