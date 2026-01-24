package com.mydotey.ai.studio.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 模型配置类型
 */
public enum ModelConfigType {
    EMBEDDING("embedding", "向量模型"),
    LLM("llm", "大语言模型");

    @EnumValue
    private final String code;
    private final String description;

    ModelConfigType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static ModelConfigType fromCode(String code) {
        for (ModelConfigType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown model config type: " + code);
    }
}
