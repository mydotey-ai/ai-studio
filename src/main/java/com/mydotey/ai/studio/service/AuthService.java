package com.mydotey.ai.studio.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mydotey.ai.studio.common.exception.BusinessException;
import com.mydotey.ai.studio.dto.LoginRequest;
import com.mydotey.ai.studio.dto.LoginResponse;
import com.mydotey.ai.studio.dto.RegisterRequest;
import com.mydotey.ai.studio.entity.RefreshToken;
import com.mydotey.ai.studio.entity.User;
import com.mydotey.ai.studio.mapper.UserMapper;
import com.mydotey.ai.studio.util.JwtUtil;
import com.mydotey.ai.studio.util.PasswordUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordUtil passwordUtil;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final LoginAttemptService loginAttemptService;

    @Transactional
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

    @Transactional
    public LoginResponse refreshAccessToken(String refreshToken) {
        RefreshToken token = refreshTokenService.validateRefreshToken(refreshToken);

        if (token == null) {
            throw new BusinessException("Invalid or expired refresh token");
        }

        // Revoke old token before creating new one
        refreshTokenService.revokeToken(refreshToken);

        User user = userMapper.selectById(token.getUserId());
        if (user == null || !"ACTIVE".equals(user.getStatus())) {
            throw new BusinessException("User not found or inactive");
        }

        String newAccessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), user.getRole());
        String newRefreshToken = refreshTokenService.createRefreshToken(user.getId());

        return new LoginResponse(newAccessToken, newRefreshToken,
                user.getId(), user.getUsername(), user.getEmail(), user.getRole());
    }

    @Transactional
    public void logout(String refreshToken) {
        int rowsUpdated = refreshTokenService.revokeToken(refreshToken);
        if (rowsUpdated == 0) {
            throw new BusinessException("Invalid or already revoked refresh token");
        }
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
