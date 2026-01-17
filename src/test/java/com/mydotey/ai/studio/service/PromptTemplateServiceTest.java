package com.mydotey.ai.studio.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Prompt 模板服务测试")
@ExtendWith(MockitoExtension.class)
class PromptTemplateServiceTest {

    @InjectMocks
    private PromptTemplateService promptTemplateService;

    @Test
    @DisplayName("应该构建完整的系统提示词")
    void testBuildSystemPrompt() {
        String context = "知识库内容：人工智能是计算机科学的一个分支";
        String systemPrompt = promptTemplateService.buildSystemPrompt(context);

        assertNotNull(systemPrompt);
        assertTrue(systemPrompt.contains("你是一个专业的助手"));
        assertTrue(systemPrompt.contains(context));
    }

    @Test
    @DisplayName("当没有相关文档时应该提示用户")
    void testBuildSystemPromptWithNoSources() {
        String context = "（未找到相关资料）";
        String systemPrompt = promptTemplateService.buildSystemPrompt(context);

        assertNotNull(systemPrompt);
        assertTrue(systemPrompt.contains("根据提供的知识库，没有找到与用户问题相关的信息"));
    }
}
