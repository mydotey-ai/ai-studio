# Phase 3: User Authentication and Permission Management Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 完善用户认证和权限管理系统，实现登录失败锁定、Token 刷新、用户管理、组织架构、权限控制和审计日志

**Architecture:** 基于已有的 JWT 认证框架，增加登录失败计数器、刷新 Token 机制、基于角色的权限控制（RBAC）和多租户组织隔离

**Tech Stack:** Spring Boot 3.5、JWT、BCrypt、MyBatis Plus、Redis（可选用于缓存）

---

## Prerequisites

- Phase 1 backend infrastructure is complete
- Phase 2 document processing is complete
- Database is initialized with users table

---

## Task 1: Add JWT Configuration and Update Auth Controller

**Files:**
- Modify: `src/main/resources/application-dev.yml`

**Step 1: Add JWT configuration to application.yml**

```yaml
jwt:
  secret: ${JWT_SECRET:your-super-secret-key-change-this-in-production-minimum-256-bits}
  access-token-expiration: 7200000  # 2 hours in milliseconds
  refresh-token-expiration: 604800000  # 7 days in milliseconds
  issuer: ai-studio

auth:
  max-login-attempts: 5
  lock-duration: 900000  # 15 minutes in milliseconds
```

**Step 2: Commit**

```bash
git add src/main/resources/application-dev.yml
git commit -m "feat: add JWT and auth configuration"
```

---

## Task 2: Implement Refresh Token Mechanism

**Files:**
- Create: `src/main/java/com/mydotey/ai/studio/entity/RefreshToken.java`
- Modify: `src/main/java/com/mydotey/ai/studio/service/AuthService.java`
- Modify: `src/main/java/com/mydotey/ai/studio/controller/AuthController.java`

**Step 1: Write the RefreshToken entity**

```java
package com.mydotey.ai.studio.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.Instant;

@Data
@TableName("refresh_tokens")
public class RefreshToken {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String token;

    private Instant expiresAt;

    private Boolean isRevoked;

    private Instant createdAt;

    private Instant updatedAt;
}
```

**Step 2: Write the RefreshToken mapper**

Create: `src/main/java/com/mydotey/ai/studio/mapper/RefreshTokenMapper.java`

```java
package com.mydotey.ai.studio.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mydotey.ai.studio.entity.RefreshToken;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RefreshTokenMapper extends BaseMapper<RefreshToken> {
    void revokeByUserId(@Param("userId") Long userId);
}
```

**Step 3: Create RefreshTokenService**

Create: `src/main/java/com/mydotey/ai/studio/service/RefreshTokenService.java`

```java
package com.mydotey.ai.studio.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mydotey.ai.studio.entity.RefreshToken;
import com.mydotey.ai.studio.mapper.RefreshTokenMapper;
import com.mydotey.ai.studio.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenMapper refreshTokenMapper;
    private final JwtUtil jwtUtil;

    /**
     * 创建并保存 refresh token
     */
    public String createRefreshToken(Long userId) {
        // 撤销该用户之前的所有 refresh tokens
        revokeUserTokens(userId);

        String tokenValue = UUID.randomUUID().toString();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(userId);
        refreshToken.setToken(tokenValue);
        refreshToken.setExpiresAt(Instant.now().plusMillis(jwtUtil.getRefreshTokenExpiration()));
        refreshToken.setIsRevoked(false);
        refreshToken.setCreatedAt(Instant.now());
        refreshToken.setUpdatedAt(Instant.now());

        refreshTokenMapper.insert(refreshToken);

        return tokenValue;
    }

    /**
     * 验证 refresh token
     */
    public RefreshToken validateRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenMapper.selectOne(
                new LambdaQueryWrapper<RefreshToken>()
                        .eq(RefreshToken::getToken, token)
                        .eq(RefreshToken::getIsRevoked, false)
        );

        if (refreshToken == null) {
            return null;
        }

        if (refreshToken.getExpiresAt().isBefore(Instant.now())) {
            revokeToken(token);
            return null;
        }

        return refreshToken;
    }

    /**
     * 撤销指定的 refresh token
     */
    @Transactional
    public void revokeToken(String token) {
        refreshTokenMapper.update(null,
                new LambdaQueryWrapper<RefreshToken>()
                        .eq(RefreshToken::getToken, token)
                        .set(RefreshToken::getIsRevoked, true)
                        .set(RefreshToken::getUpdatedAt, Instant.now())
        );
    }

    /**
     * 撤销用户的所有 refresh tokens
     */
    @Transactional
    public void revokeUserTokens(Long userId) {
        refreshTokenMapper.revokeByUserId(userId);
    }

    /**
     * 删除过期的 refresh tokens
     */
    @Transactional
    public void deleteExpiredTokens() {
        refreshTokenMapper.delete(
                new LambdaQueryWrapper<RefreshToken>()
                        .lt(RefreshToken::getExpiresAt, Instant.now())
        );
    }
}
```

**Step 4: Add getRefreshTokenExpiration method to JwtUtil**

Modify: `src/main/java/com/mydotey/ai/studio/util/JwtUtil.java`

```java
public long getRefreshTokenExpiration() {
    return refreshTokenExpiration;
}
```

**Step 5: Update AuthService to use RefreshToken**

Modify: `src/main/java/com/mydotey/ai/studio/service/AuthService.java`

```java
package com.mydotey.ai.studio.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mydotey.ai.studio.common.exception.BusinessException;
import com.mydotey.ai.studio.dto.LoginRequest;
import com.mydotey.ai.studio.dto.LoginResponse;
import com.mydotey.ai.studio.dto.RegisterRequest;
import com.mydotey.ai.studio.entity.RefreshToken;
import com.mydotey.ai.studio.entity.User;
import com.mydotey.ai.studio.mapper.UserMapper;
import com.mydotey.ai.studio.service.RefreshTokenService;
import com.mydotey.ai.studio.util.JwtUtil;
import com.mydotey.ai.studio.util.PasswordUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordUtil passwordUtil;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    public LoginResponse login(LoginRequest request) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername()));

        if (user == null) {
            throw new BusinessException("Invalid username or password");
        }

        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BusinessException("Account is inactive or locked");
        }

        if (!passwordUtil.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException("Invalid username or password");
        }

        // Update last login time
        user.setLastLoginAt(Instant.now());
        userMapper.updateById(user);

        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), user.getRole());
        String refreshToken = refreshTokenService.createRefreshToken(user.getId());

        return new LoginResponse(accessToken, refreshToken,
                user.getId(), user.getUsername(), user.getEmail(), user.getRole());
    }

    public LoginResponse refreshAccessToken(String refreshToken) {
        RefreshToken token = refreshTokenService.validateRefreshToken(refreshToken);

        if (token == null) {
            throw new BusinessException("Invalid or expired refresh token");
        }

        User user = userMapper.selectById(token.getUserId());
        if (user == null || !"ACTIVE".equals(user.getStatus())) {
            throw new BusinessException("User not found or inactive");
        }

        String newAccessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), user.getRole());
        String newRefreshToken = refreshTokenService.createRefreshToken(user.getId());

        return new LoginResponse(newAccessToken, newRefreshToken,
                user.getId(), user.getUsername(), user.getEmail(), user.getRole());
    }

    public void logout(String refreshToken) {
        refreshTokenService.revokeToken(refreshToken);
    }

    public void register(RegisterRequest request) {
        // Check if username exists
        Long usernameCount = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername()));
        if (usernameCount > 0) {
            throw new BusinessException("Username already exists");
        }

        // Check if email exists
        Long emailCount = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, request.getEmail()));
        if (emailCount > 0) {
            throw new BusinessException("Email already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordUtil.encode(request.getPassword()));
        user.setRole("USER");
        user.setStatus("ACTIVE");
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());

        userMapper.insert(user);
    }
}
```

**Step 6: Add refresh and logout endpoints to AuthController**

Modify: `src/main/java/com/mydotey/ai/studio/controller/AuthController.java`

```java
package com.mydotey.ai.studio.controller;

import com.mydotey.ai.studio.common.ApiResponse;
import com.mydotey.ai.studio.dto.LoginRequest;
import com.mydotey.ai.studio.dto.LoginResponse;
import com.mydotey.ai.studio.dto.RegisterRequest;
import com.mydotey.ai.studio.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ApiResponse.success(response);
    }

    @PostMapping("/register")
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ApiResponse.success("Registration successful", null);
    }

    @PostMapping("/refresh")
    public ApiResponse<LoginResponse> refresh(@RequestBody RefreshTokenRequest request) {
        LoginResponse response = authService.refreshAccessToken(request.getRefreshToken());
        return ApiResponse.success(response);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestBody RefreshTokenRequest request) {
        authService.logout(request.getRefreshToken());
        return ApiResponse.success("Logout successful", null);
    }
}
```

**Step 7: Create RefreshTokenRequest DTO**

Create: `src/main/java/com/mydotey/ai/studio/dto/RefreshTokenRequest.java`

```java
package com.mydotey.ai.studio.dto;

import lombok.Data;

@Data
public class RefreshTokenRequest {
    private String refreshToken;
}
```

**Step 8: Commit**

```bash
git add src/main/java/com/mydotey/ai/studio/entity/RefreshToken.java
git add src/main/java/com/mydotey/ai/studio/mapper/RefreshTokenMapper.java
git add src/main/java/com/mydotey/ai/studio/service/RefreshTokenService.java
git add src/main/java/com/mydotey/ai/studio/service/AuthService.java
git add src/main/java/com/mydotey/ai/studio/controller/AuthController.java
git add src/main/java/com/mydotey/ai/studio/dto/RefreshTokenRequest.java
git add src/main/java/com/mydotey/ai/studio/util/JwtUtil.java
git commit -m "feat: implement refresh token mechanism"
```

---

## Task 3: Implement Login Failure Locking

**Files:**
- Create: `src/main/java/com/mydotey/ai/studio/entity/LoginAttempt.java`
- Create: `src/main/java/com/mydotey/ai/studio/service/LoginAttemptService.java`
- Modify: `src/main/java/com/mydotey/ai/studio/service/AuthService.java`

**Step 1: Write the LoginAttempt entity**

```java
package com.mydotey.ai.studio.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.Instant;

@Data
@TableName("login_attempts")
public class LoginAttempt {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String identifier; // username or IP address

    private Long userId;

    private Integer attemptCount;

    private Instant lastAttemptAt;

    private Instant lockedUntil;

    private Instant createdAt;

    private Instant updatedAt;
}
```

**Step 2: Write the LoginAttempt mapper**

Create: `src/main/java/com/mydotey/ai/studio/mapper/LoginAttemptMapper.java`

```java
package com.mydotey.ai.studio.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mydotey.ai.studio.entity.LoginAttempt;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LoginAttemptMapper extends BaseMapper<LoginAttempt> {
}
```

**Step 3: Write the LoginAttemptService**

Create: `src/main/java/com/mydotey/ai/studio/service/LoginAttemptService.java`

```java
package com.mydotey.ai.studio.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mydotey.ai.studio.entity.LoginAttempt;
import com.mydotey.ai.studio.mapper.LoginAttemptMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private final LoginAttemptMapper loginAttemptMapper;

    @Value("${auth.max-login-attempts:5}")
    private int maxLoginAttempts;

    @Value("${auth.lock-duration:900000}") // 15 minutes default
    private long lockDuration;

    /**
     * 检查是否被锁定
     */
    public boolean isLocked(String identifier) {
        LoginAttempt attempt = loginAttemptMapper.selectOne(
                new LambdaQueryWrapper<LoginAttempt>()
                        .eq(LoginAttempt::getIdentifier, identifier)
        );

        if (attempt == null) {
            return false;
        }

        if (attempt.getLockedUntil() != null) {
            if (attempt.getLockedUntil().isAfter(Instant.now())) {
                return true;
            } else {
                // 锁定已过期，重置
                resetAttempt(identifier);
                return false;
            }
        }

        return false;
    }

    /**
     * 记录失败的登录尝试
     */
    @Transactional
    public void recordFailedAttempt(String identifier, Long userId) {
        LoginAttempt attempt = loginAttemptMapper.selectOne(
                new LambdaQueryWrapper<LoginAttempt>()
                        .eq(LoginAttempt::getIdentifier, identifier)
        );

        if (attempt == null) {
            attempt = new LoginAttempt();
            attempt.setIdentifier(identifier);
            attempt.setUserId(userId);
            attempt.setAttemptCount(1);
            attempt.setLastAttemptAt(Instant.now());
            attempt.setCreatedAt(Instant.now());
            attempt.setUpdatedAt(Instant.now());
            loginAttemptMapper.insert(attempt);
        } else {
            attempt.setAttemptCount(attempt.getAttemptCount() + 1);
            attempt.setLastAttemptAt(Instant.now());
            attempt.setUpdatedAt(Instant.now());

            // 检查是否需要锁定
            if (attempt.getAttemptCount() >= maxLoginAttempts) {
                attempt.setLockedUntil(Instant.now().plusMillis(lockDuration));
            }

            loginAttemptMapper.updateById(attempt);
        }
    }

    /**
     * 记录成功的登录，重置尝试次数
     */
    @Transactional
    public void recordSuccessfulAttempt(String identifier) {
        resetAttempt(identifier);
    }

    /**
     * 重置登录尝试
     */
    @Transactional
    public void resetAttempt(String identifier) {
        LoginAttempt attempt = loginAttemptMapper.selectOne(
                new LambdaQueryWrapper<LoginAttempt>()
                        .eq(LoginAttempt::getIdentifier, identifier)
        );

        if (attempt != null) {
            attempt.setAttemptCount(0);
            attempt.setLockedUntil(null);
            attempt.setUpdatedAt(Instant.now());
            loginAttemptMapper.updateById(attempt);
        }
    }

    /**
     * 获取剩余锁定时间（秒）
     */
    public long getRemainingLockTime(String identifier) {
        LoginAttempt attempt = loginAttemptMapper.selectOne(
                new LambdaQueryWrapper<LoginAttempt>()
                        .eq(LoginAttempt::getIdentifier, identifier)
        );

        if (attempt == null || attempt.getLockedUntil() == null) {
            return 0;
        }

        long remaining = attempt.getLockedUntil().toEpochMilli() - Instant.now().toEpochMilli();
        return Math.max(0, remaining / 1000);
    }
}
```

**Step 4: Update AuthService to use LoginAttemptService**

Modify: `src/main/java/com/mydotey/ai/studio/service/AuthService.java`

```java
package com.mydotey.ai.studio.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mydotey.ai.studio.common.exception.BusinessException;
import com.mydotey.ai.studio.dto.LoginRequest;
import com.mydotey.ai.studio.dto.LoginResponse;
import com.mydotey.ai.studio.dto.RegisterRequest;
import com.mydotey.ai.studio.entity.RefreshToken;
import com.mydotey.ai.studio.entity.User;
import com.mydotey.ai.studio.mapper.UserMapper;
import com.mydotey.ai.studio.service.RefreshTokenService;
import com.mydotey.ai.studio.util.JwtUtil;
import com.mydotey.ai.studio.util.PasswordUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordUtil passwordUtil;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final LoginAttemptService loginAttemptService;

    public LoginResponse login(LoginRequest request) {
        String username = request.getUsername();

        // 检查是否被锁定
        if (loginAttemptService.isLocked(username)) {
            long remainingSeconds = loginAttemptService.getRemainingLockTime(username);
            throw new BusinessException(
                    String.format("Account is locked. Try again in %d minutes",
                            remainingSeconds / 60 + 1)
            );
        }

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));

        if (user == null || !"ACTIVE".equals(user.getStatus())) {
            loginAttemptService.recordFailedAttempt(username, user != null ? user.getId() : null);
            throw new BusinessException("Invalid username or password");
        }

        if (!passwordUtil.matches(request.getPassword(), user.getPasswordHash())) {
            loginAttemptService.recordFailedAttempt(username, user.getId());
            throw new BusinessException("Invalid username or password");
        }

        // 登录成功，重置尝试次数
        loginAttemptService.recordSuccessfulAttempt(username);

        // Update last login time
        user.setLastLoginAt(Instant.now());
        userMapper.updateById(user);

        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), user.getRole());
        String refreshToken = refreshTokenService.createRefreshToken(user.getId());

        return new LoginResponse(accessToken, refreshToken,
                user.getId(), user.getUsername(), user.getEmail(), user.getRole());
    }

    public LoginResponse refreshAccessToken(String refreshToken) {
        RefreshToken token = refreshTokenService.validateRefreshToken(refreshToken);

        if (token == null) {
            throw new BusinessException("Invalid or expired refresh token");
        }

        User user = userMapper.selectById(token.getUserId());
        if (user == null || !"ACTIVE".equals(user.getStatus())) {
            throw new BusinessException("User not found or inactive");
        }

        String newAccessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), user.getRole());
        String newRefreshToken = refreshTokenService.createRefreshToken(user.getId());

        return new LoginResponse(newAccessToken, newRefreshToken,
                user.getId(), user.getUsername(), user.getEmail(), user.getRole());
    }

    public void logout(String refreshToken) {
        refreshTokenService.revokeToken(refreshToken);
    }

    public void register(RegisterRequest request) {
        // Check if username exists
        Long usernameCount = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername()));
        if (usernameCount > 0) {
            throw new BusinessException("Username already exists");
        }

        // Check if email exists
        Long emailCount = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, request.getEmail()));
        if (emailCount > 0) {
            throw new BusinessException("Email already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordUtil.encode(request.getPassword()));
        user.setRole("USER");
        user.setStatus("ACTIVE");
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());

        userMapper.insert(user);
    }
}
```

**Step 5: Commit**

```bash
git add src/main/java/com/mydotey/ai/studio/entity/LoginAttempt.java
git add src/main/java/com/mydotey/ai/studio/mapper/LoginAttemptMapper.java
git add src/main/java/com/mydotey/ai/studio/service/LoginAttemptService.java
git add src/main/java/com/mydotey/ai/studio/service/AuthService.java
git commit -m "feat: implement login failure locking mechanism"
```

---

## Task 4: Implement User Management Service

**Files:**
- Create: `src/main/java/com/mydotey/ai/studio/service/UserService.java`
- Create: `src/main/java/com/mydotey/ai/studio/dto/UserResponse.java`
- Create: `src/main/java/com/mydotey/ai/studio/dto/UpdateUserRequest.java`
- Create: `src/main/java/com/mydotey/ai/studio/controller/UserController.java`
- Test: `src/test/java/com/mydotey/ai/studio/service/UserServiceTest.java`

**Step 1: Write the UserResponse DTO**

```java
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
```

**Step 2: Write the UpdateUserRequest DTO**

```java
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
}
```

**Step 3: Write the test class**

```java
package com.mydotey.ai.studio.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mydotey.ai.studio.dto.UpdateUserRequest;
import com.mydotey.ai.studio.entity.User;
import com.mydotey.ai.studio.mapper.UserMapper;
import com.mydotey.ai.studio.util.PasswordUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("用户服务测试")
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordUtil passwordUtil;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("应该能够获取用户信息")
    void testGetUserById() {
        Long userId = 1L;
        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        user.setEmail("test@example.com");

        when(userMapper.selectById(userId)).thenReturn(user);

        var response = userService.getUserById(userId);

        assertNotNull(response);
        assertEquals(userId, response.getId());
        assertEquals("testuser", response.getUsername());
    }

    @Test
    @DisplayName("用户不存在时应该抛出异常")
    void testGetUserByIdNotFound() {
        when(userMapper.selectById(any())).thenReturn(null);

        assertThrows(Exception.class, () -> userService.getUserById(1L));
    }

    @Test
    @DisplayName("应该能够更新用户邮箱")
    void testUpdateUserEmail() {
        Long userId = 1L;
        User existingUser = new User();
        existingUser.setId(userId);
        existingUser.setEmail("old@example.com");

        UpdateUserRequest request = new UpdateUserRequest();
        request.setEmail("new@example.com");

        when(userMapper.selectById(userId)).thenReturn(existingUser);
        when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        userService.updateUser(userId, request);

        verify(userMapper).updateById(any(User.class));
    }
}
```

**Step 4: Run test to verify it fails**

Run: `mvn test -Dtest=UserServiceTest`
Expected: FAIL with class not found

**Step 5: Write the UserService implementation**

```java
package com.mydotey.ai.studio.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mydotey.ai.studio.common.exception.BusinessException;
import com.mydotey.ai.studio.dto.UpdateUserRequest;
import com.mydotey.ai.studio.dto.UserResponse;
import com.mydotey.ai.studio.entity.User;
import com.mydotey.ai.studio.mapper.UserMapper;
import com.mydotey.ai.studio.util.PasswordUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final PasswordUtil passwordUtil;

    /**
     * 根据用户 ID 获取用户信息
     */
    public UserResponse getUserById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("User not found");
        }
        return toResponse(user);
    }

    /**
     * 根据用户名获取用户信息
     */
    public UserResponse getUserByUsername(String username) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, username)
        );
        if (user == null) {
            throw new BusinessException("User not found");
        }
        return toResponse(user);
    }

    /**
     * 获取所有用户（仅管理员）
     */
    public List<UserResponse> getAllUsers() {
        List<User> users = userMapper.selectList(null);
        return users.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 更新用户信息
     */
    @Transactional
    public void updateUser(Long userId, UpdateUserRequest request, Long currentUserId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("User not found");
        }

        // 检查权限：用户只能修改自己的信息，管理员可以修改任何人
        if (!userId.equals(currentUserId) && !"ADMIN".equals(getUserRole(currentUserId))
                && !"SUPER_ADMIN".equals(getUserRole(currentUserId))) {
            throw new BusinessException("Permission denied");
        }

        // 更新邮箱
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            // 检查邮箱是否被其他用户使用
            Long emailCount = userMapper.selectCount(
                    new LambdaQueryWrapper<User>()
                            .eq(User::getEmail, request.getEmail())
                            .ne(User::getId, userId)
            );
            if (emailCount > 0) {
                throw new BusinessException("Email already in use");
            }
            user.setEmail(request.getEmail());
        }

        // 更新头像
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }

        // 更新密码
        if (request.getNewPassword() != null && !request.getNewPassword().isEmpty()) {
            user.setPasswordHash(passwordUtil.encode(request.getNewPassword()));
        }

        userMapper.updateById(user);
    }

    /**
     * 更新用户状态（仅管理员）
     */
    @Transactional
    public void updateUserStatus(Long userId, String status, Long operatorId) {
        if (!"ADMIN".equals(getUserRole(operatorId)) && !"SUPER_ADMIN".equals(getUserRole(operatorId))) {
            throw new BusinessException("Permission denied");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("User not found");
        }

        user.setStatus(status);
        userMapper.updateById(user);
    }

    /**
     * 删除用户（仅管理员，且不能删除自己）
     */
    @Transactional
    public void deleteUser(Long userId, Long operatorId) {
        if (userId.equals(operatorId)) {
            throw new BusinessException("Cannot delete yourself");
        }

        if (!"ADMIN".equals(getUserRole(operatorId)) && !"SUPER_ADMIN".equals(getUserRole(operatorId))) {
            throw new BusinessException("Permission denied");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("User not found");
        }

        // 防止删除超级管理员
        if ("SUPER_ADMIN".equals(user.getRole()) && !"SUPER_ADMIN".equals(getUserRole(operatorId))) {
            throw new BusinessException("Cannot delete super admin");
        }

        userMapper.deleteById(userId);
    }

    /**
     * 获取用户角色
     */
    private String getUserRole(Long userId) {
        User user = userMapper.selectById(userId);
        return user != null ? user.getRole() : null;
    }

    /**
     * 转换为响应对象
     */
    private UserResponse toResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setOrgId(user.getOrgId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole());
        response.setStatus(user.getStatus());
        response.setAvatarUrl(user.getAvatarUrl());
        response.setLastLoginAt(user.getLastLoginAt());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        return response;
    }
}
```

**Step 6: Run test to verify it passes**

Run: `mvn test -Dtest=UserServiceTest`
Expected: PASS

**Step 7: Write UserController**

Create: `src/main/java/com/mydotey/ai/studio/controller/UserController.java`

```java
package com.mydotey.ai.studio.controller;

import com.mydotey.ai.studio.common.ApiResponse;
import com.mydotey.ai.studio.dto.UpdateUserRequest;
import com.mydotey.ai.studio.dto.UserResponse;
import com.mydotey.ai.studio.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}")
    public ApiResponse<UserResponse> getUser(@PathVariable Long id) {
        UserResponse user = userService.getUserById(id);
        return ApiResponse.success(user);
    }

    @GetMapping
    public ApiResponse<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = userService.getAllUsers();
        return ApiResponse.success(users);
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request,
            @RequestAttribute("userId") Long currentUserId) {
        userService.updateUser(id, request, currentUserId);
        return ApiResponse.success("User updated successfully");
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<Void> updateUserStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestAttribute("userId") Long operatorId) {
        userService.updateUserStatus(id, status, operatorId);
        return ApiResponse.success("User status updated successfully");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteUser(
            @PathVariable Long id,
            @RequestAttribute("userId") Long operatorId) {
        userService.deleteUser(id, operatorId);
        return ApiResponse.success("User deleted successfully");
    }
}
```

**Step 8: Commit**

```bash
git add src/main/java/com/mydotey/ai/studio/service/UserService.java
git add src/main/java/com/mydotey/ai/studio/dto/UserResponse.java
git add src/main/java/com/mydotey/ai/studio/dto/UpdateUserRequest.java
git add src/main/java/com/mydotey/ai/studio/controller/UserController.java
git add src/test/java/com/mydotey/ai/studio/service/UserServiceTest.java
git commit -m "feat: implement user management service"
```

---

## Task 5: Implement Organization Management

**Files:**
- Create: `src/main/java/com/mydotey/ai/studio/entity/Organization.java`
- Create: `src/main/java/com/mydotey/ai/studio/mapper/OrganizationMapper.java`
- Create: `src/main/java/com/mydotey/ai/studio/dto/OrganizationResponse.java`
- Create: `src/main/java/com/mydotey/ai/studio/dto/CreateOrganizationRequest.java`
- Create: `src/main/java/com/mydotey/ai/studio/service/OrganizationService.java`
- Create: `src/main/java/com/mydotey/ai/studio/controller/OrganizationController.java`

**Step 1: Write the Organization entity**

```java
package com.mydotey.ai.studio.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.Instant;

@Data
@TableName("organizations")
public class Organization {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String description;

    private String settings;

    private Instant createdAt;

    private Instant updatedAt;
}
```

**Step 2: Write the OrganizationMapper**

```java
package com.mydotey.ai.studio.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mydotey.ai.studio.entity.Organization;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrganizationMapper extends BaseMapper<Organization> {
}
```

**Step 3: Write the DTOs**

Create: `src/main/java/com/mydotey/ai/studio/dto/OrganizationResponse.java`

```java
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
```

Create: `src/main/java/com/mydotey/ai/studio/dto/CreateOrganizationRequest.java`

```java
package com.mydotey.ai.studio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateOrganizationRequest {
    @NotBlank(message = "Organization name is required")
    @Size(max = 255, message = "Organization name must not exceed 255 characters")
    private String name;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;
}
```

**Step 4: Write the OrganizationService**

Create: `src/main/java/com/mydotey/ai/studio/service/OrganizationService.java`

```java
package com.mydotey.ai.studio.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mydotey.ai.studio.common.exception.BusinessException;
import com.mydotey.ai.studio.dto.CreateOrganizationRequest;
import com.mydotey.ai.studio.dto.OrganizationResponse;
import com.mydotey.ai.studio.entity.Organization;
import com.mydotey.ai.studio.entity.User;
import com.mydotey.ai.studio.mapper.OrganizationMapper;
import com.mydotey.ai.studio.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationMapper organizationMapper;
    private final UserMapper userMapper;

    /**
     * 创建组织
     */
    @Transactional
    public OrganizationResponse create(CreateOrganizationRequest request, Long userId) {
        // 检查用户是否已有组织（简化版：每个用户只能创建一个组织）
        User user = userMapper.selectById(userId);
        if (user != null && user.getOrgId() != null) {
            throw new BusinessException("User already belongs to an organization");
        }

        Organization org = new Organization();
        org.setName(request.getName());
        org.setDescription(request.getDescription());
        org.setSettings("{}");

        organizationMapper.insert(org);

        // 将创建者关联到组织
        if (user != null) {
            user.setOrgId(org.getId());
            userMapper.updateById(user);
        }

        return toResponse(org);
    }

    /**
     * 获取组织信息
     */
    public OrganizationResponse getById(Long orgId) {
        Organization org = organizationMapper.selectById(orgId);
        if (org == null) {
            throw new BusinessException("Organization not found");
        }
        return toResponse(org);
    }

    /**
     * 获取用户的组织信息
     */
    public OrganizationResponse getUserOrganization(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getOrgId() == null) {
            throw new BusinessException("User organization not found");
        }
        return getById(user.getOrgId());
    }

    /**
     * 更新组织信息
     */
    @Transactional
    public void update(Long orgId, CreateOrganizationRequest request) {
        Organization org = organizationMapper.selectById(orgId);
        if (org == null) {
            throw new BusinessException("Organization not found");
        }

        org.setName(request.getName());
        org.setDescription(request.getDescription());

        organizationMapper.updateById(org);
    }

    /**
     * 转换为响应对象
     */
    private OrganizationResponse toResponse(Organization org) {
        OrganizationResponse response = new OrganizationResponse();
        response.setId(org.getId());
        response.setName(org.getName());
        response.setDescription(org.getDescription());
        response.setSettings(org.getSettings());
        response.setCreatedAt(org.getCreatedAt());
        response.setUpdatedAt(org.getUpdatedAt());
        return response;
    }
}
```

**Step 5: Write the OrganizationController**

Create: `src/main/java/com/mydotey/ai/studio/controller/OrganizationController.java`

```java
package com.mydotey.ai.studio.controller;

import com.mydotey.ai.studio.common.ApiResponse;
import com.mydotey.ai.studio.dto.CreateOrganizationRequest;
import com.mydotey.ai.studio.dto.OrganizationResponse;
import com.mydotey.ai.studio.service.OrganizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;

    @PostMapping
    public ApiResponse<OrganizationResponse> create(
            @Valid @RequestBody CreateOrganizationRequest request,
            @RequestAttribute("userId") Long userId) {
        OrganizationResponse org = organizationService.create(request, userId);
        return ApiResponse.success(org);
    }

    @GetMapping("/{id}")
    public ApiResponse<OrganizationResponse> getById(@PathVariable Long id) {
        OrganizationResponse org = organizationService.getById(id);
        return ApiResponse.success(org);
    }

    @GetMapping("/my")
    public ApiResponse<OrganizationResponse> getMyOrganization(@RequestAttribute("userId") Long userId) {
        OrganizationResponse org = organizationService.getUserOrganization(userId);
        return ApiResponse.success(org);
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(
            @PathVariable Long id,
            @Valid @RequestBody CreateOrganizationRequest request) {
        organizationService.update(id, request);
        return ApiResponse.success("Organization updated successfully");
    }
}
```

**Step 6: Commit**

```bash
git add src/main/java/com/mydotey/ai/studio/entity/Organization.java
git add src/main/java/com/mydotey/ai/studio/mapper/OrganizationMapper.java
git add src/main/java/com/mydotey/ai/studio/dto/OrganizationResponse.java
git add src/main/java/com/mydotey/ai/studio/dto/CreateOrganizationRequest.java
git add src/main/java/com/mydotey/ai/studio/service/OrganizationService.java
git add src/main/java/com/mydotey/ai/studio/controller/OrganizationController.java
git commit -m "feat: implement organization management"
```

---

## Task 6: Implement Permission Annotation and Aspect

**Files:**
- Create: `src/main/java/com/mydotey/ai/studio/annotation/RequireRole.java`
- Create: `src/main/java/com/mydotey/ai/studio/aspect/PermissionAspect.java`

**Step 1: Write the RequireRole annotation**

```java
package com.mydotey.ai.studio.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireRole {
    /**
     * 需要的角色列表
     */
    String[] value();

    /**
     * 是否需要匹配所有角色（AND），默认匹配任一角色（OR）
     */
    boolean requireAll() default false;
}
```

**Step 2: Write the PermissionAspect**

Create: `src/main/java/com/mydotey/ai/studio/aspect/PermissionAspect.java`

```java
package com.mydotey.ai.studio.aspect;

import com.mydotey.ai.studio.annotation.RequireRole;
import com.mydotey.ai.studio.common.exception.AuthException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;

@Slf4j
@Aspect
@Component
public class PermissionAspect {

    @Before("@annotation(requireRole)")
    public void checkPermission(JoinPoint joinPoint, RequireRole requireRole) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new AuthException("No request context");
        }

        HttpServletRequest request = attributes.getRequest();
        Object userRoleObj = request.getAttribute("userRole");

        if (userRoleObj == null) {
            throw new AuthException("Authentication required");
        }

        String userRole = (String) userRoleObj;
        String[] requiredRoles = requireRole.value();

        boolean hasPermission = requireRole.requireAll()
                ? Arrays.stream(requiredRoles).allMatch(role -> role.equals(userRole))
                : Arrays.stream(requiredRoles).anyMatch(role -> role.equals(userRole));

        if (!hasPermission) {
            log.warn("Permission denied: user role={}, required roles={}", userRole, Arrays.toString(requiredRoles));
            throw new AuthException("Permission denied");
        }
    }
}
```

**Step 3: Update AuthException**

Create: `src/main/java/com/mydotey/ai/studio/common/exception/AuthException.java`

```java
package com.mydotey.ai.studio.common.exception;

public class AuthException extends RuntimeException {
    public AuthException(String message) {
        super(message);
    }
}
```

**Step 4: Update GlobalExceptionHandler to handle AuthException**

Modify: `src/main/java/com/mydotey/ai/studio/common/exception/GlobalExceptionHandler.java`

```java
@ExceptionHandler(AuthException.class)
public ResponseEntity<ApiResponse<Void>> handleAuthException(AuthException e) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.error(e.getMessage()));
}
```

**Step 5: Commit**

```bash
git add src/main/java/com/mydotey/ai/studio/annotation/RequireRole.java
git add src/main/java/com/mydotey/ai/studio/aspect/PermissionAspect.java
git add src/main/java/com/mydotey/ai/studio/common/exception/AuthException.java
git add src/main/java/com/mydotey/ai/studio/common/exception/GlobalExceptionHandler.java
git commit -m "feat: implement permission annotation and aspect"
```

---

## Task 7: Apply Permission Annotations to Controllers

**Files:**
- Modify: `src/main/java/com/mydotey/ai/studio/controller/UserController.java`
- Modify: `src/main/java/com/mydotey/ai/studio/controller/OrganizationController.java`
- Modify: `src/main/java/com/mydotey/ai/studio/controller/KnowledgeBaseController.java`

**Step 1: Update UserController with permission annotations**

```java
package com.mydotey.ai.studio.controller;

import com.mydotey.ai.studio.annotation.RequireRole;
import com.mydotey.ai.studio.common.ApiResponse;
import com.mydotey.ai.studio.dto.UpdateUserRequest;
import com.mydotey.ai.studio.dto.UserResponse;
import com.mydotey.ai.studio.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}")
    public ApiResponse<UserResponse> getUser(@PathVariable Long id) {
        UserResponse user = userService.getUserById(id);
        return ApiResponse.success(user);
    }

    @GetMapping
    @RequireRole({"ADMIN", "SUPER_ADMIN"})
    public ApiResponse<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = userService.getAllUsers();
        return ApiResponse.success(users);
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request,
            @RequestAttribute("userId") Long currentUserId) {
        userService.updateUser(id, request, currentUserId);
        return ApiResponse.success("User updated successfully");
    }

    @PatchMapping("/{id}/status")
    @RequireRole({"ADMIN", "SUPER_ADMIN"})
    public ApiResponse<Void> updateUserStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestAttribute("userId") Long operatorId) {
        userService.updateUserStatus(id, status, operatorId);
        return ApiResponse.success("User status updated successfully");
    }

    @DeleteMapping("/{id}")
    @RequireRole({"ADMIN", "SUPER_ADMIN"})
    public ApiResponse<Void> deleteUser(
            @PathVariable Long id,
            @RequestAttribute("userId") Long operatorId) {
        userService.deleteUser(id, operatorId);
        return ApiResponse.success("User deleted successfully");
    }
}
```

**Step 2: Update OrganizationController with permission annotations**

```java
package com.mydotey.ai.studio.controller;

import com.mydotey.ai.studio.annotation.RequireRole;
import com.mydotey.ai.studio.common.ApiResponse;
import com.mydotey.ai.studio.dto.CreateOrganizationRequest;
import com.mydotey.ai.studio.dto.OrganizationResponse;
import com.mydotey.ai.studio.service.OrganizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;

    @PostMapping
    public ApiResponse<OrganizationResponse> create(
            @Valid @RequestBody CreateOrganizationRequest request,
            @RequestAttribute("userId") Long userId) {
        OrganizationResponse org = organizationService.create(request, userId);
        return ApiResponse.success(org);
    }

    @GetMapping("/{id}")
    public ApiResponse<OrganizationResponse> getById(@PathVariable Long id) {
        OrganizationResponse org = organizationService.getById(id);
        return ApiResponse.success(org);
    }

    @GetMapping("/my")
    public ApiResponse<OrganizationResponse> getMyOrganization(@RequestAttribute("userId") Long userId) {
        OrganizationResponse org = organizationService.getUserOrganization(userId);
        return ApiResponse.success(org);
    }

    @PutMapping("/{id}")
    @RequireRole({"ADMIN", "SUPER_ADMIN"})
    public ApiResponse<Void> update(
            @PathVariable Long id,
            @Valid @RequestBody CreateOrganizationRequest request) {
        organizationService.update(id, request);
        return ApiResponse.success("Organization updated successfully");
    }
}
```

**Step 3: Commit**

```bash
git add src/main/java/com/mydotey/ai/studio/controller/UserController.java
git add src/main/java/com/mydotey/ai/studio/controller/OrganizationController.java
git commit -m "feat: apply permission annotations to controllers"
```

---

## Task 8: Implement Audit Logging

**Files:**
- Create: `src/main/java/com/mydotey/ai/studio/entity/AuditLog.java`
- Create: `src/main/java/com/mydotey/ai/studio/mapper/AuditLogMapper.java`
- Create: `src/main/java/com/mydotey/ai/studio/service/AuditLogService.java`
- Create: `src/main/java/com/mydotey/ai/studio/annotation/AuditLog.java`
- Create: `src/main/java/com/mydotey/ai/studio/aspect/AuditLogAspect.java`

**Step 1: Write the AuditLog entity**

```java
package com.mydotey.ai.studio.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.Instant;

@Data
@TableName("audit_logs")
public class AuditLog {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String action;

    private String resourceType;

    private Long resourceId;

    private String details;

    private String ipAddress;

    private String userAgent;

    private Instant createdAt;
}
```

**Step 2: Write the AuditLogMapper**

```java
package com.mydotey.ai.studio.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mydotey.ai.studio.entity.AuditLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLog> {
}
```

**Step 3: Write the AuditLog annotation**

```java
package com.mydotey.ai.studio.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditLog {
    String action();

    String resourceType() default "";

    String resourceIdParam() default "";
}
```

**Step 4: Write the AuditLogService**

Create: `src/main/java/com/mydotey/ai/studio/service/AuditLogService.java`

```java
package com.mydotey.ai.studio.service;

import com.mydotey.ai.studio.entity.AuditLog;
import com.mydotey.ai.studio.mapper.AuditLogMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper;

    public void logAudit(Long userId, String action, String resourceType, Long resourceId, Object details) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setUserId(userId);
            auditLog.setAction(action);
            auditLog.setResourceType(resourceType);
            auditLog.setResourceId(resourceId);

            if (details != null) {
                auditLog.setDetails(objectMapper.writeValueAsString(details));
            } else {
                auditLog.setDetails("{}");
            }

            // 获取请求信息
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                auditLog.setIpAddress(getClientIpAddress(request));
                auditLog.setUserAgent(request.getHeader("User-Agent"));
            }

            auditLog.setCreatedAt(Instant.now());

            auditLogMapper.insert(auditLog);

        } catch (Exception e) {
            log.error("Failed to create audit log", e);
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
```

**Step 5: Write the AuditLogAspect**

Create: `src/main/java/com/mydotey/ai/studio/aspect/AuditLogAspect.java`

```java
package com.mydotey.ai.studio.aspect;

import com.mydotey.ai.studio.annotation.AuditLog;
import com.mydotey.ai.studio.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private final AuditLogService auditLogService;

    @Around("@annotation(auditLog)")
    public Object logAudit(ProceedingJoinPoint joinPoint, AuditLog auditLog) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        Long userId = null;

        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            Object userIdObj = request.getAttribute("userId");
            if (userIdObj != null) {
                userId = (Long) userIdObj;
            }
        }

        // 获取资源 ID
        Long resourceId = null;
        if (!auditLog.resourceIdParam().isEmpty()) {
            Object[] args = joinPoint.getArgs();
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String[] paramNames = signature.getParameterNames();

            for (int i = 0; i < paramNames.length; i++) {
                if (paramNames[i].equals(auditLog.resourceIdParam())) {
                    if (args[i] instanceof Long) {
                        resourceId = (Long) args[i];
                    }
                    break;
                }
            }
        }

        Object result = joinPoint.proceed();

        // 记录审计日志
        auditLogService.logAudit(userId, auditLog.action(),
                auditLog.resourceType(), resourceId, null);

        return result;
    }
}
```

**Step 6: Commit**

```bash
git add src/main/java/com/mydotey/ai/studio/entity/AuditLog.java
git add src/main/java/com/mydotey/ai/studio/mapper/AuditLogMapper.java
git add src/main/java/com/mydotey/ai/studio/service/AuditLogService.java
git add src/main/java/com/mydotey/ai/studio/annotation/AuditLog.java
git add src/main/java/com/mydotey/ai/studio/aspect/AuditLogAspect.java
git commit -m "feat: implement audit logging"
```

---

## Task 9: Apply Audit Log to Key Operations

**Files:**
- Modify: `src/main/java/com/mydotey/ai/studio/controller/AuthController.java`
- Modify: `src/main/java/com/mydotey/ai/studio/controller/UserController.java`
- Modify: `src/main/java/com/mydotey/ai/studio/controller/OrganizationController.java`

**Step 1: Update AuthController with audit logs**

```java
package com.mydotey.ai.studio.controller;

import com.mydotey.ai.studio.annotation.AuditLog;
import com.mydotey.ai.studio.common.ApiResponse;
import com.mydotey.ai.studio.dto.LoginRequest;
import com.mydotey.ai.studio.dto.LoginResponse;
import com.mydotey.ai.studio.dto.RegisterRequest;
import com.mydotey.ai.studio.dto.RefreshTokenRequest;
import com.mydotey.ai.studio.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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
    public ApiResponse<LoginResponse> refresh(@RequestBody RefreshTokenRequest request) {
        LoginResponse response = authService.refreshAccessToken(request.getRefreshToken());
        return ApiResponse.success(response);
    }

    @PostMapping("/logout")
    @AuditLog(action = "USER_LOGOUT", resourceType = "User")
    public ApiResponse<Void> logout(@RequestBody RefreshTokenRequest request) {
        authService.logout(request.getRefreshToken());
        return ApiResponse.success("Logout successful", null);
    }
}
```

**Step 2: Update UserController with audit logs**

```java
package com.mydotey.ai.studio.controller;

import com.mydotey.ai.studio.annotation.AuditLog;
import com.mydotey.ai.studio.annotation.RequireRole;
import com.mydotey.ai.studio.common.ApiResponse;
import com.mydotey.ai.studio.dto.UpdateUserRequest;
import com.mydotey.ai.studio.dto.UserResponse;
import com.mydotey.ai.studio.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}")
    public ApiResponse<UserResponse> getUser(@PathVariable Long id) {
        UserResponse user = userService.getUserById(id);
        return ApiResponse.success(user);
    }

    @GetMapping
    @RequireRole({"ADMIN", "SUPER_ADMIN"})
    public ApiResponse<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = userService.getAllUsers();
        return ApiResponse.success(users);
    }

    @PutMapping("/{id}")
    @AuditLog(action = "USER_UPDATE", resourceType = "User", resourceIdParam = "id")
    public ApiResponse<Void> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request,
            @RequestAttribute("userId") Long currentUserId) {
        userService.updateUser(id, request, currentUserId);
        return ApiResponse.success("User updated successfully");
    }

    @PatchMapping("/{id}/status")
    @RequireRole({"ADMIN", "SUPER_ADMIN"})
    @AuditLog(action = "USER_STATUS_UPDATE", resourceType = "User", resourceIdParam = "id")
    public ApiResponse<Void> updateUserStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestAttribute("userId") Long operatorId) {
        userService.updateUserStatus(id, status, operatorId);
        return ApiResponse.success("User status updated successfully");
    }

    @DeleteMapping("/{id}")
    @RequireRole({"ADMIN", "SUPER_ADMIN"})
    @AuditLog(action = "USER_DELETE", resourceType = "User", resourceIdParam = "id")
    public ApiResponse<Void> deleteUser(
            @PathVariable Long id,
            @RequestAttribute("userId") Long operatorId) {
        userService.deleteUser(id, operatorId);
        return ApiResponse.success("User deleted successfully");
    }
}
```

**Step 3: Update OrganizationController with audit logs**

```java
package com.mydotey.ai.studio.controller;

import com.mydotey.ai.studio.annotation.AuditLog;
import com.mydotey.ai.studio.annotation.RequireRole;
import com.mydotey.ai.studio.common.ApiResponse;
import com.mydotey.ai.studio.dto.CreateOrganizationRequest;
import com.mydotey.ai.studio.dto.OrganizationResponse;
import com.mydotey.ai.studio.service.OrganizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;

    @PostMapping
    @AuditLog(action = "ORGANIZATION_CREATE", resourceType = "Organization")
    public ApiResponse<OrganizationResponse> create(
            @Valid @RequestBody CreateOrganizationRequest request,
            @RequestAttribute("userId") Long userId) {
        OrganizationResponse org = organizationService.create(request, userId);
        return ApiResponse.success(org);
    }

    @GetMapping("/{id}")
    public ApiResponse<OrganizationResponse> getById(@PathVariable Long id) {
        OrganizationResponse org = organizationService.getById(id);
        return ApiResponse.success(org);
    }

    @GetMapping("/my")
    public ApiResponse<OrganizationResponse> getMyOrganization(@RequestAttribute("userId") Long userId) {
        OrganizationResponse org = organizationService.getUserOrganization(userId);
        return ApiResponse.success(org);
    }

    @PutMapping("/{id}")
    @RequireRole({"ADMIN", "SUPER_ADMIN"})
    @AuditLog(action = "ORGANIZATION_UPDATE", resourceType = "Organization", resourceIdParam = "id")
    public ApiResponse<Void> update(
            @PathVariable Long id,
            @Valid @RequestBody CreateOrganizationRequest request) {
        organizationService.update(id, request);
        return ApiResponse.success("Organization updated successfully");
    }
}
```

**Step 4: Commit**

```bash
git add src/main/java/com/mydotey/ai/studio/controller/AuthController.java
git add src/main/java/com/mydotey/ai/studio/controller/UserController.java
git add src/main/java/com/mydotey/ai/studio/controller/OrganizationController.java
git commit -m "feat: apply audit logging to key operations"
```

---

## Task 10: Create Database Migration Scripts

**Files:**
- Create: `src/main/resources/db/migration/V3__auth_permission_tables.sql`

**Step 1: Write the migration script**

```sql
-- Refresh Tokens Table
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    is_revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);

-- Login Attempts Table
CREATE TABLE IF NOT EXISTS login_attempts (
    id BIGSERIAL PRIMARY KEY,
    identifier VARCHAR(255) NOT NULL,
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    last_attempt_at TIMESTAMP NOT NULL,
    locked_until TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_login_attempts_identifier ON login_attempts(identifier);

-- Audit Logs Table
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    action VARCHAR(50) NOT NULL,
    resource_type VARCHAR(50),
    resource_id BIGINT,
    details JSONB DEFAULT '{}',
    ip_address VARCHAR(50),
    user_agent TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_resource_type ON audit_logs(resource_type);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);

-- Organizations Table (if not exists)
CREATE TABLE IF NOT EXISTS organizations (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    settings JSONB DEFAULT '{}',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Add org_id foreign key to users table if not exists
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_users_org_id'
    ) THEN
        ALTER TABLE users ADD CONSTRAINT fk_users_org_id
        FOREIGN KEY (org_id) REFERENCES organizations(id) ON DELETE SET NULL;
    END IF;
END $$;
```

**Step 2: Commit**

```bash
git add src/main/resources/db/migration/V3__auth_permission_tables.sql
git commit -m "feat: add database migration for auth and permission tables"
```

---

## Task 11: Integration Test - Auth and Permission Flow

**Files:**
- Create: `src/test/java/com/mydotey/ai/studio/integration/AuthAndPermissionIntegrationTest.java`

**Step 1: Write the integration test**

```java
package com.mydotey.ai.studio.integration;

import com.mydotey.ai.studio.dto.*;
import com.mydotey.ai.studio.service.AuthService;
import com.mydotey.ai.studio.service.UserService;
import com.mydotey.ai.studio.util.JwtUtil;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("认证和权限集成测试")
class AuthAndPermissionIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    private String baseUrl;
    private LoginResponse adminLoginResponse;
    private LoginResponse userLoginResponse;

    @BeforeAll
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api";
    }

    @Test
    @DisplayName("用户注册和登录流程")
    void testRegistrationAndLoginFlow() {
        // 1. 注册新用户
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("testuser123");
        registerRequest.setEmail("testuser123@example.com");
        registerRequest.setPassword("password123");

        ResponseEntity<ApiResponse> registerResponse = restTemplate.postForEntity(
                baseUrl + "/auth/register",
                registerRequest,
                ApiResponse.class
        );
        assertEquals(HttpStatus.OK, registerResponse.getStatusCode());

        // 2. 登录
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser123");
        loginRequest.setPassword("password123");

        ResponseEntity<ApiResponse<LoginResponse>> loginResponse = restTemplate.exchange(
                baseUrl + "/auth/login",
                HttpMethod.POST,
                new HttpEntity<>(loginRequest),
                new ParameterizedTypeReference<ApiResponse<LoginResponse>>() {}
        );
        assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
        assertNotNull(loginResponse.getBody().getData().getAccessToken());
    }

    @Test
    @DisplayName("Token 刷新流程")
    void testTokenRefreshFlow() {
        // 1. 登录获取 token
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser123");
        loginRequest.setPassword("password123");

        ResponseEntity<ApiResponse<LoginResponse>> loginResponse = restTemplate.exchange(
                baseUrl + "/auth/login",
                HttpMethod.POST,
                new HttpEntity<>(loginRequest),
                new ParameterizedTypeReference<ApiResponse<LoginResponse>>() {}
        );
        String refreshToken = loginResponse.getBody().getData().getRefreshToken();

        // 2. 使用 refresh token 刷新 access token
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest();
        refreshRequest.setRefreshToken(refreshToken);

        ResponseEntity<ApiResponse<LoginResponse>> refreshResponse = restTemplate.exchange(
                baseUrl + "/auth/refresh",
                HttpMethod.POST,
                new HttpEntity<>(refreshRequest),
                new ParameterizedTypeReference<ApiResponse<LoginResponse>>() {}
        );
        assertEquals(HttpStatus.OK, refreshResponse.getStatusCode());
        assertNotNull(refreshResponse.getBody().getData().getAccessToken());
    }

    @Test
    @DisplayName("需要认证的端点应该拒绝未授权请求")
    void testUnauthorizedAccessShouldBeRejected() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<ApiResponse> response = restTemplate.exchange(
                baseUrl + "/users",
                HttpMethod.GET,
                entity,
                ApiResponse.class
        );
        assertTrue(response.getStatusCode() == HttpStatus.FORBIDDEN || response.getStatusCode() == HttpStatus.UNAUTHORIZED);
    }
}
```

**Step 2: Run test to verify it passes**

Run: `mvn test -Dtest=AuthAndPermissionIntegrationTest`
Expected: PASS

**Step 3: Commit**

```bash
git add src/test/java/com/mydotey/ai/studio/integration/AuthAndPermissionIntegrationTest.java
git commit -m "test: add integration test for auth and permission flow"
```

---

## Summary

Phase 3 完成后，系统将具备以下能力：

1. **JWT Token 刷新机制**：支持 access token 过期后使用 refresh token 获取新 token
2. **登录失败锁定**：5 次失败后锁定账户 15 分钟
3. **用户管理**：CRUD 操作、状态管理、密码修改
4. **组织管理**：组织创建、查询、更新
5. **基于角色的权限控制**：@RequireRole 注解实现方法级权限控制
6. **审计日志**：记录关键操作（登录、注册、用户操作等）
7. **完整的认证流程**：注册、登录、刷新、登出

### 测试覆盖

- 用户服务单元测试
- 认证和权限集成测试

### API 端点

| 方法 | 端点 | 权限 | 描述 |
|------|------|--------|------|
| POST | /api/auth/login | 公开 | 用户登录 |
| POST | /api/auth/register | 公开 | 用户注册 |
| POST | /api/auth/refresh | 公开 | 刷新 token |
| POST | /api/auth/logout | 公开 | 用户登出 |
| GET | /api/users/{id} | 所有用户 | 获取用户信息 |
| GET | /api/users | ADMIN/SUPER_ADMIN | 获取所有用户 |
| PUT | /api/users/{id} | 本人或管理员 | 更新用户信息 |
| PATCH | /api/users/{id}/status | ADMIN/SUPER_ADMIN | 更新用户状态 |
| DELETE | /api/users/{id} | ADMIN/SUPER_ADMIN | 删除用户 |
| POST | /api/organizations | 所有用户 | 创建组织 |
| GET | /api/organizations/{id} | 所有用户 | 获取组织信息 |
| GET | /api/organizations/my | 所有用户 | 获取我的组织 |
| PUT | /api/organizations/{id} | ADMIN/SUPER_ADMIN | 更新组织信息 |

### 数据库表

1. `refresh_tokens` - 存储刷新 token
2. `login_attempts` - 记录登录失败尝试
3. `audit_logs` - 审计日志表
4. `organizations` - 组织表

### 后续改进方向（Phase 4+）

- 知识库权限管理（kb_members 表）
- API Key 管理和认证
- 密码重置功能（邮件验证）
- 二次认证（2FA）
- 用户组管理
- 更细粒度的权限控制（基于资源的权限）
