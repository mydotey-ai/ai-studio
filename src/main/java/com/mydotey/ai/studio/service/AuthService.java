package com.mydotey.ai.studio.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mydotey.ai.studio.common.exception.BusinessException;
import com.mydotey.ai.studio.dto.LoginRequest;
import com.mydotey.ai.studio.dto.LoginResponse;
import com.mydotey.ai.studio.dto.RegisterRequest;
import com.mydotey.ai.studio.entity.User;
import com.mydotey.ai.studio.mapper.UserMapper;
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
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername(), user.getRole());

        return new LoginResponse(accessToken, refreshToken,
                user.getId(), user.getUsername(), user.getEmail(), user.getRole());
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
