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
