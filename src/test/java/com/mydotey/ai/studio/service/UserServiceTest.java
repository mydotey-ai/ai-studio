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
        Long currentUserId = 1L;
        User existingUser = new User();
        existingUser.setId(userId);
        existingUser.setEmail("old@example.com");

        UpdateUserRequest request = new UpdateUserRequest();
        request.setEmail("new@example.com");

        when(userMapper.selectById(userId)).thenReturn(existingUser);
        when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        userService.updateUser(userId, request, currentUserId);

        verify(userMapper).updateById(any(User.class));
    }
}
