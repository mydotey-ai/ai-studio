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
