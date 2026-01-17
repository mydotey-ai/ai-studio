package com.mydotey.ai.studio.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mydotey.ai.studio.dto.Message;
import com.mydotey.ai.studio.dto.MessageRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Prompt 模板服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PromptTemplateService {

    private final ObjectMapper objectMapper;

    // Using the same constant as ContextBuilderService to avoid circular dependency
    private static final String NO_SOURCES_MESSAGE = "（未找到相关资料）";

    private static final String DEFAULT_SYSTEM_PROMPT =
            """
            你是一个专业的助手，负责根据知识库内容回答用户问题。

            ## 任务说明
            请仔细阅读知识库内容，准确回答用户的问题。

            ## 回答要求
            1. **基于知识库回答**：优先使用知识库中的信息，确保准确性
            2. **引用来源**：在回答中明确标注信息来源（文档名称）
            3. **诚实回答**：如果知识库中没有相关信息，请明确说明"根据提供的知识库，没有找到相关信息"
            4. **不要编造**：绝不要凭空编造知识库中没有的信息
            5. **语言自然**：使用自然流畅的语言，避免机械生硬

            ## 上下文信息
            %s

            ## 用户的后续问题
            请根据以上上下文信息回答用户的问题。
            """;

    private static final String NO_SOURCES_SYSTEM_PROMPT =
            """
            你是一个专业的助手。

            注意：根据提供的知识库，没有找到与用户问题相关的信息。
            请明确告诉用户这一点，不要编造任何信息。

            如果用户的问题比较通用，你可以基于你的通用知识回答，但必须声明这不是来自知识库的信息。
            """;

    /**
     * 构建系统提示词
     *
     * @param context 上下文信息
     * @return 完整的系统提示词
     */
    public String buildSystemPrompt(String context) {
        // 检查是否有找到相关文档
        boolean hasSources = context != null && !context.contains(NO_SOURCES_MESSAGE);

        if (hasSources) {
            return String.format(DEFAULT_SYSTEM_PROMPT, context);
        } else {
            return NO_SOURCES_SYSTEM_PROMPT;
        }
    }

    /**
     * 构建完整的消息列表（用于 API 调用）
     *
     * @param systemPrompt 系统提示词
     * @param userQuestion 用户问题
     * @return 消息列表的 JSON 字符串
     */
    public String buildMessages(String systemPrompt, String userQuestion) {
        try {
            List<Message> messages = List.of(
                    Message.builder()
                            .role(MessageRole.SYSTEM)
                            .content(systemPrompt)
                            .build(),
                    Message.builder()
                            .role(MessageRole.USER)
                            .content(userQuestion)
                            .build()
            );
            return objectMapper.writeValueAsString(messages);
        } catch (Exception e) {
            log.error("Failed to build messages JSON", e);
            throw new RuntimeException("Failed to build messages JSON", e);
        }
    }
}
