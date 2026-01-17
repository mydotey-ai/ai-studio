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
