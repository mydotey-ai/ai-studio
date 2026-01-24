package com.mydotey.ai.studio.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mydotey.ai.studio.common.exception.BusinessException;
import com.mydotey.ai.studio.dto.ChangePasswordRequest;
import com.mydotey.ai.studio.dto.UpdateProfileRequest;
import com.mydotey.ai.studio.dto.UpdateUserRequest;
import com.mydotey.ai.studio.dto.UserProfileResponse;
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

        // 更新密码 - 需要验证当前密码
        if (request.getNewPassword() != null && !request.getNewPassword().isEmpty()) {
            // 验证当前密码
            if (request.getCurrentPassword() == null || request.getCurrentPassword().isEmpty()) {
                throw new BusinessException("Current password is required to change password");
            }
            if (!passwordUtil.matches(request.getCurrentPassword(), user.getPasswordHash())) {
                throw new BusinessException("Current password is incorrect");
            }
            user.setPasswordHash(passwordUtil.encode(request.getNewPassword()));
        }

        // 更新角色 - 需要管理员权限
        if (request.getRole() != null && !request.getRole().isEmpty()) {
            // 验证角色值
            if (!"USER".equals(request.getRole()) &&
                !"ADMIN".equals(request.getRole()) &&
                !"SUPER_ADMIN".equals(request.getRole())) {
                throw new BusinessException("Invalid role value");
            }

            // 只有超级管理员可以分配/修改超级管理员角色
            if ("SUPER_ADMIN".equals(request.getRole())) {
                String operatorRole = getUserRole(currentUserId);
                if (!"SUPER_ADMIN".equals(operatorRole)) {
                    throw new BusinessException("Only super admin can assign super admin role");
                }
            }

            // 管理员不能将自己的角色降级
            if (userId.equals(currentUserId) && !"SUPER_ADMIN".equals(getUserRole(currentUserId))) {
                throw new BusinessException("Cannot modify your own role");
            }

            user.setRole(request.getRole());
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

    /**
     * 获取当前登录用户的详细信息
     */
    public UserProfileResponse getCurrentUserProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("User not found");
        }
        return toProfileResponse(user);
    }

    /**
     * 更新当前登录用户的信息
     */
    @Transactional
    public void updateUserProfile(Long userId, UpdateProfileRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("User not found");
        }

        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
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

        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }

        if (request.getLanguage() != null) {
            user.setLanguage(request.getLanguage());
        }

        if (request.getTimezone() != null) {
            user.setTimezone(request.getTimezone());
        }

        userMapper.updateById(user);
    }

    /**
     * 修改密码
     */
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("New password and confirm password do not match");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("User not found");
        }

        if (!passwordUtil.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new BusinessException("Old password is incorrect");
        }

        user.setPasswordHash(passwordUtil.encode(request.getNewPassword()));
        userMapper.updateById(user);
    }

    /**
     * 转换为个人资料响应对象
     */
    private UserProfileResponse toProfileResponse(User user) {
        UserProfileResponse response = new UserProfileResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole());
        response.setBio(user.getBio());
        response.setLanguage(user.getLanguage());
        response.setTimezone(user.getTimezone());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        return response;
    }
}
