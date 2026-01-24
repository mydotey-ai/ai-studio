package com.mydotey.ai.studio.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Model configuration type enum
 */
@Getter
@AllArgsConstructor
public enum ModelConfigType {

    /**
     * OpenAI configuration type
     */
    OPENAI("openai", "OpenAI"),

    /**
     * Azure OpenAI configuration type
     */
    AZURE_OPENAI("azure_openai", "Azure OpenAI"),

    /**
     * Anthropic configuration type
     */
    ANTHROPIC("anthropic", "Anthropic"),

    /**
     * Hugging Face configuration type
     */
    HUGGING_FACE("hugging_face", "Hugging Face"),

    /**
     * Ollama configuration type
     */
    OLLAMA("ollama", "Ollama"),

    /**
     * Local AI configuration type
     */
    LOCAL_AI("local_ai", "Local AI"),

    /**
     * Custom configuration type
     */
    CUSTOM("custom", "Custom");

    /**
     * Code value
     */
    private final String code;

    /**
     * Display name
     */
    private final String displayName;

    /**
     * Get enum by code
     *
     * @param code code value
     * @return ModelConfigType enum, return null if not found
     */
    public static ModelConfigType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (ModelConfigType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}
