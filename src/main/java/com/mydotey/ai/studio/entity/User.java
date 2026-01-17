package com.mydotey.ai.studio.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.Instant;

@Data
@TableName("users")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orgId;

    private String username;

    private String email;

    private String passwordHash;

    private String role; // SUPER_ADMIN, ADMIN, USER

    private String status; // ACTIVE, INACTIVE, LOCKED

    private String avatarUrl;

    private Instant lastLoginAt;

    private Instant createdAt;

    private Instant updatedAt;
}
