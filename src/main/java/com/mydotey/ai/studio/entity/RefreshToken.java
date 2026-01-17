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
