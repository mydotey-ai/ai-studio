package com.mydotey.ai.studio.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mydotey.ai.studio.dto.Message;
import com.mydotey.ai.studio.dto.MessageRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("Prompt 模板服务测试")
@ExtendWith(MockitoExtension.class)
class PromptTemplateServiceTest {

    @InjectMocks
    private PromptTemplateService promptTemplateService;

    @Mock
    private ObjectMapper objectMapper;

    // Use real ObjectMapper for validation in tests
    private final ObjectMapper realMapper = new ObjectMapper();

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

    @Test
    @DisplayName("应该构建正确的消息列表 JSON")
    void testBuildMessages() throws Exception {
        String systemPrompt = "你是一个专业的助手";
        String userQuestion = "什么是人工智能？";

        // Setup mock to use real mapper for serialization
        when(objectMapper.writeValueAsString(any())).thenAnswer(invocation -> {
            List<Message> messages = invocation.getArgument(0);
            return realMapper.writeValueAsString(messages);
        });

        String messagesJson = promptTemplateService.buildMessages(systemPrompt, userQuestion);

        assertNotNull(messagesJson);

        // Verify JSON can be parsed back
        List<Message> messages = realMapper.readValue(messagesJson, new TypeReference<List<Message>>() {});

        assertEquals(2, messages.size());
        assertEquals(MessageRole.SYSTEM, messages.get(0).getRole());
        assertEquals(systemPrompt, messages.get(0).getContent());
        assertEquals(MessageRole.USER, messages.get(1).getRole());
        assertEquals(userQuestion, messages.get(1).getContent());

        verify(objectMapper, times(1)).writeValueAsString(any());
    }

    @Test
    @DisplayName("应该正确处理包含引号的文本")
    void testBuildMessagesWithQuotes() throws Exception {
        String systemPrompt = "请用\"专业\"的方式回答问题";
        String userQuestion = "什么是\"人工智能\"？";

        when(objectMapper.writeValueAsString(any())).thenAnswer(invocation -> {
            List<Message> messages = invocation.getArgument(0);
            return realMapper.writeValueAsString(messages);
        });

        String messagesJson = promptTemplateService.buildMessages(systemPrompt, userQuestion);

        assertNotNull(messagesJson);

        // Verify JSON can be parsed back (ensures proper escaping)
        List<Message> messages = realMapper.readValue(messagesJson, new TypeReference<List<Message>>() {});

        assertEquals(2, messages.size());
        assertEquals(systemPrompt, messages.get(0).getContent());
        assertEquals(userQuestion, messages.get(1).getContent());

        verify(objectMapper, times(1)).writeValueAsString(any());
    }

    @Test
    @DisplayName("应该正确处理包含换行符的文本")
    void testBuildMessagesWithNewlines() throws Exception {
        String systemPrompt = "你是一个助手\n\n请仔细回答";
        String userQuestion = "第一行\n第二行\n第三行";

        when(objectMapper.writeValueAsString(any())).thenAnswer(invocation -> {
            List<Message> messages = invocation.getArgument(0);
            return realMapper.writeValueAsString(messages);
        });

        String messagesJson = promptTemplateService.buildMessages(systemPrompt, userQuestion);

        assertNotNull(messagesJson);

        // Verify JSON can be parsed back
        List<Message> messages = realMapper.readValue(messagesJson, new TypeReference<List<Message>>() {});

        assertEquals(2, messages.size());
        assertEquals(systemPrompt, messages.get(0).getContent());
        assertEquals(userQuestion, messages.get(1).getContent());

        verify(objectMapper, times(1)).writeValueAsString(any());
    }

    @Test
    @DisplayName("应该正确处理包含特殊字符的文本")
    void testBuildMessagesWithSpecialCharacters() throws Exception {
        String systemPrompt = "Test: \\t \\n \\r \\b \\f";
        String userQuestion = "测试: 反斜杠\\ 制表符\t";

        when(objectMapper.writeValueAsString(any())).thenAnswer(invocation -> {
            List<Message> messages = invocation.getArgument(0);
            return realMapper.writeValueAsString(messages);
        });

        String messagesJson = promptTemplateService.buildMessages(systemPrompt, userQuestion);

        assertNotNull(messagesJson);

        // Verify JSON can be parsed back
        List<Message> messages = realMapper.readValue(messagesJson, new TypeReference<List<Message>>() {});

        assertEquals(2, messages.size());
        assertEquals(systemPrompt, messages.get(0).getContent());
        assertEquals(userQuestion, messages.get(1).getContent());

        verify(objectMapper, times(1)).writeValueAsString(any());
    }

    @Test
    @DisplayName("应该正确处理包含 Unicode 字符的文本")
    void testBuildMessagesWithUnicode() throws Exception {
        String systemPrompt = "中文测试：你是一个助手";
        String userQuestion = "什么是 emoji 😊 和特殊符号 ©®™？";

        when(objectMapper.writeValueAsString(any())).thenAnswer(invocation -> {
            List<Message> messages = invocation.getArgument(0);
            return realMapper.writeValueAsString(messages);
        });

        String messagesJson = promptTemplateService.buildMessages(systemPrompt, userQuestion);

        assertNotNull(messagesJson);

        // Verify JSON can be parsed back
        List<Message> messages = realMapper.readValue(messagesJson, new TypeReference<List<Message>>() {});

        assertEquals(2, messages.size());
        assertEquals(systemPrompt, messages.get(0).getContent());
        assertEquals(userQuestion, messages.get(1).getContent());

        verify(objectMapper, times(1)).writeValueAsString(any());
    }

    @Test
    @DisplayName("应该正确处理包含 JSON 结构的文本")
    void testBuildMessagesWithJsonContent() throws Exception {
        String systemPrompt = "Format: {\"key\": \"value\"}";
        String userQuestion = "Parse: [{\"id\": 1}, {\"id\": 2}]";

        when(objectMapper.writeValueAsString(any())).thenAnswer(invocation -> {
            List<Message> messages = invocation.getArgument(0);
            return realMapper.writeValueAsString(messages);
        });

        String messagesJson = promptTemplateService.buildMessages(systemPrompt, userQuestion);

        assertNotNull(messagesJson);

        // Verify JSON can be parsed back (double encoding handled correctly)
        List<Message> messages = realMapper.readValue(messagesJson, new TypeReference<List<Message>>() {});

        assertEquals(2, messages.size());
        assertEquals(systemPrompt, messages.get(0).getContent());
        assertEquals(userQuestion, messages.get(1).getContent());

        verify(objectMapper, times(1)).writeValueAsString(any());
    }

    @Test
    @DisplayName("应该正确处理多行文本")
    void testBuildMessagesWithMultilineText() throws Exception {
        String systemPrompt = """
                你是一个专业的助手。

                ## 任务说明
                请仔细阅读知识库内容，准确回答用户的问题。

                ## 回答要求
                1. 基于知识库回答
                2. 引用来源
                """;
        String userQuestion = "什么是人工智能？\n请详细解释。";

        when(objectMapper.writeValueAsString(any())).thenAnswer(invocation -> {
            List<Message> messages = invocation.getArgument(0);
            return realMapper.writeValueAsString(messages);
        });

        String messagesJson = promptTemplateService.buildMessages(systemPrompt, userQuestion);

        assertNotNull(messagesJson);

        // Verify JSON can be parsed back
        List<Message> messages = realMapper.readValue(messagesJson, new TypeReference<List<Message>>() {});

        assertEquals(2, messages.size());
        assertEquals(systemPrompt, messages.get(0).getContent());
        assertEquals(userQuestion, messages.get(1).getContent());

        verify(objectMapper, times(1)).writeValueAsString(any());
    }

    @Test
    @DisplayName("应该正确处理空文本")
    void testBuildMessagesWithEmptyText() throws Exception {
        String systemPrompt = "";
        String userQuestion = "";

        when(objectMapper.writeValueAsString(any())).thenAnswer(invocation -> {
            List<Message> messages = invocation.getArgument(0);
            return realMapper.writeValueAsString(messages);
        });

        String messagesJson = promptTemplateService.buildMessages(systemPrompt, userQuestion);

        assertNotNull(messagesJson);

        // Verify JSON can be parsed back
        List<Message> messages = realMapper.readValue(messagesJson, new TypeReference<List<Message>>() {});

        assertEquals(2, messages.size());
        assertEquals(MessageRole.SYSTEM, messages.get(0).getRole());
        assertEquals("", messages.get(0).getContent());
        assertEquals(MessageRole.USER, messages.get(1).getRole());
        assertEquals("", messages.get(1).getContent());

        verify(objectMapper, times(1)).writeValueAsString(any());
    }
}
