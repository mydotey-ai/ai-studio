package com.mydotey.ai.studio.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mydotey.ai.studio.config.LlmConfig;
import com.mydotey.ai.studio.dto.LlmResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("LLM 生成服务测试")
@ExtendWith(MockitoExtension.class)
class LlmGenerationServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private LlmConfig config;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private PromptTemplateService promptTemplateService;

    @InjectMocks
    private LlmGenerationService llmGenerationService;

    // Use real ObjectMapper for response parsing
    private final ObjectMapper realMapper = new ObjectMapper();

    @Test
    @DisplayName("应该成功生成回答并解析响应")
    void testGenerateSuccess() throws Exception {
        // Given
        String systemPrompt = "你是一个专业的助手";
        String userQuestion = "什么是人工智能？";
        String expectedResponseBody = """
            {
                "choices": [{
                    "message": {
                        "content": "人工智能是计算机科学的一个分支",
                        "role": "assistant"
                    },
                    "finish_reason": "stop",
                    "index": 0
                }],
                "usage": {
                    "prompt_tokens": 100,
                    "completion_tokens": 50,
                    "total_tokens": 150
                }
            }
            """;

        when(promptTemplateService.buildMessages(systemPrompt, userQuestion))
                .thenReturn("[{\"role\":\"system\",\"content\":\"你是一个专业的助手\"},{\"role\":\"user\",\"content\":\"什么是人工智能？\"}]");
        when(config.getEndpoint()).thenReturn("https://api.openai.com/v1");
        when(config.getApiKey()).thenReturn("test-api-key");
        when(config.getModel()).thenReturn("gpt-3.5-turbo");
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // Use real mapper for response parsing
        when(objectMapper.readTree(anyString())).thenAnswer(invocation -> {
            String json = invocation.getArgument(0);
            return realMapper.readTree(json);
        });

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(expectedResponseBody, HttpStatus.OK));

        // When
        LlmResponse response = llmGenerationService.generate(systemPrompt, userQuestion, 0.3, 1000);

        // Then
        assertNotNull(response);
        assertEquals("人工智能是计算机科学的一个分支", response.getContent());
        assertEquals("stop", response.getFinishReason());
        assertEquals(150, response.getTotalTokens());
        assertEquals(100, response.getPromptTokens());
        assertEquals(50, response.getCompletionTokens());

        verify(restTemplate).postForEntity(
                eq("https://api.openai.com/v1/chat/completions"),
                any(HttpEntity.class),
                eq(String.class)
        );
    }

    @Test
    @DisplayName("当 temperature 为 null 时应该使用默认值")
    void testGenerateWithNullTemperature() throws Exception {
        // Given
        when(config.getEndpoint()).thenReturn("https://api.openai.com/v1");
        when(config.getApiKey()).thenReturn("test-api-key");
        when(config.getModel()).thenReturn("gpt-3.5-turbo");
        when(config.getDefaultTemperature()).thenReturn(0.5);
        when(config.getDefaultMaxTokens()).thenReturn(500);
        when(promptTemplateService.buildMessages(anyString(), anyString()))
                .thenReturn("[]");
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"temperature\":0.5,\"max_tokens\":500}");

        // Use real mapper for response parsing
        when(objectMapper.readTree(anyString())).thenAnswer(invocation -> {
            String json = invocation.getArgument(0);
            return realMapper.readTree(json);
        });

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>("{\"choices\":[{\"message\":{\"content\":\"test\"},\"finish_reason\":\"stop\"}]}", HttpStatus.OK));

        // When
        llmGenerationService.generate("系统提示", "用户问题", null, null);

        // Then
        @SuppressWarnings("unchecked")
        ArgumentCaptor<HttpEntity<String>> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(anyString(), captor.capture(), eq(String.class));

        String body = captor.getValue().getBody();
        assertNotNull(body);
        assertTrue(body.contains("\"temperature\":0.5"));
        assertTrue(body.contains("\"max_tokens\":500"));
    }

    @Test
    @DisplayName("当响应没有 usage 字段时应该正确处理")
    void testParseResponseWithoutUsage() throws Exception {
        // Given
        String responseBody = """
            {
                "choices": [{
                    "message": {"content": "测试内容", "role": "assistant"},
                    "finish_reason": "stop",
                    "index": 0
                }]
            }
            """;

        when(config.getEndpoint()).thenReturn("https://api.openai.com/v1");
        when(config.getApiKey()).thenReturn("test-api-key");
        when(config.getModel()).thenReturn("gpt-3.5-turbo");
        when(promptTemplateService.buildMessages(anyString(), anyString())).thenReturn("[]");
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // Use real mapper for response parsing
        when(objectMapper.readTree(anyString())).thenAnswer(invocation -> {
            String json = invocation.getArgument(0);
            return realMapper.readTree(json);
        });

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(responseBody, HttpStatus.OK));

        // When
        LlmResponse response = llmGenerationService.generate("系统提示", "用户问题", 0.3, 1000);

        // Then
        assertNotNull(response);
        assertEquals("测试内容", response.getContent());
        assertEquals("stop", response.getFinishReason());
        assertNull(response.getTotalTokens());
        assertNull(response.getPromptTokens());
        assertNull(response.getCompletionTokens());
    }

    @Test
    @DisplayName("当 API 调用失败时应该抛出异常")
    void testGenerateApiError() throws Exception {
        // Given
        when(promptTemplateService.buildMessages(anyString(), anyString())).thenReturn("[]");
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RuntimeException("API error"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            llmGenerationService.generate("系统提示", "用户问题", 0.3, 1000);
        });

        assertTrue(exception.getMessage().contains("Failed to generate response"));
    }
}
