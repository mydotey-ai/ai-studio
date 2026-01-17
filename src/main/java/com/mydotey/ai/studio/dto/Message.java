package com.mydotey.ai.studio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    /**
     * 消息角色：user, assistant, system
     */
    private MessageRole role;

    /**
     * 消息内容
     */
    private String content;
}
