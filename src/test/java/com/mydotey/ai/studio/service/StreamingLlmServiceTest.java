package com.mydotey.ai.studio.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mydotey.ai.studio.config.LlmConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("流式 LLM 服务测试")
@ExtendWith(MockitoExtension.class)
class StreamingLlmServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private LlmConfig config;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private PromptTemplateService promptTemplateService;

    @InjectMocks
    private StreamingLlmService streamingLlmService;

    // Use real ObjectMapper for JSON parsing
    private final ObjectMapper realMapper = new ObjectMapper();

    @Test
    @DisplayName("应该成功流式生成并回调")
    void testStreamGenerateSuccess() throws Exception {
        // Given
        String systemPrompt = "你是一个专业的助手";
        String userQuestion = "什么是人工智能？";
        String streamResponseBody = """
            data: {"choices":[{"delta":{"content":"人工智能"},"finish_reason":null}]}

            data: {"choices":[{"delta":{"content":"是"},"finish_reason":null}]}

            data: {"choices":[{"delta":{},"finish_reason":"stop"}]}

            data: [DONE]
            """;

        List<String> receivedContent = new ArrayList<>();
        StreamingLlmService.StreamCallback callback = new StreamingLlmService.StreamCallback() {
            @Override
            public void onContent(String content) {
                receivedContent.add(content);
            }

            @Override
            public void onComplete() {
                // Mark complete
            }

            @Override
            public void onError(Exception e) {
                throw new RuntimeException(e);
            }
        };

        when(promptTemplateService.buildMessages(systemPrompt, userQuestion))
                .thenReturn("[]");
        when(config.getEndpoint()).thenReturn("https://api.openai.com/v1");
        when(config.getApiKey()).thenReturn("test-api-key");
        when(config.getModel()).thenReturn("gpt-3.5-turbo");
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // Use real mapper for JSON parsing
        when(objectMapper.readTree(anyString())).thenAnswer(invocation -> {
            String json = invocation.getArgument(0);
            return realMapper.readTree(json);
        });

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(streamResponseBody, HttpStatus.OK));

        // When
        streamingLlmService.streamGenerate(systemPrompt, userQuestion, 0.3, 1000, callback);

        // Then
        assertEquals(2, receivedContent.size());
        assertEquals("人工智能", receivedContent.get(0));
        assertEquals("是", receivedContent.get(1));
    }

    @Test
    @DisplayName("应该正确处理 [DONE] 标记")
    void testStreamWithDoneMarker() throws Exception {
        // Given
        String streamResponseBody = """
            data: {"choices":[{"delta":{"content":"内容"}}]}

            data: [DONE]
            """;

        boolean[] completed = {false};
        StreamingLlmService.StreamCallback callback = new StreamingLlmService.StreamCallback() {
            @Override
            public void onContent(String content) {}

            @Override
            public void onComplete() {
                completed[0] = true;
            }

            @Override
            public void onError(Exception e) {}
        };

        when(config.getEndpoint()).thenReturn("https://api.openai.com/v1");
        when(config.getApiKey()).thenReturn("test-api-key");
        when(config.getModel()).thenReturn("gpt-3.5-turbo");
        when(promptTemplateService.buildMessages(anyString(), anyString())).thenReturn("[]");
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // Use real mapper for JSON parsing
        when(objectMapper.readTree(anyString())).thenAnswer(invocation -> {
            String json = invocation.getArgument(0);
            return realMapper.readTree(json);
        });

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(streamResponseBody, HttpStatus.OK));

        // When
        streamingLlmService.streamGenerate("提示", "问题", 0.3, 1000, callback);

        // Then
        assertTrue(completed[0]);
    }

    @Test
    @DisplayName("当流式请求失败时应该调用 onError")
    void testStreamWithError() throws Exception {
        // Given
        Exception[] capturedError = new Exception[1];
        StreamingLlmService.StreamCallback callback = new StreamingLlmService.StreamCallback() {
            @Override
            public void onContent(String content) {}

            @Override
            public void onComplete() {}

            @Override
            public void onError(Exception e) {
                capturedError[0] = e;
            }
        };

        when(config.getEndpoint()).thenReturn("https://api.openai.com/v1");
        when(config.getApiKey()).thenReturn("test-api-key");
        when(config.getModel()).thenReturn("gpt-3.5-turbo");
        when(promptTemplateService.buildMessages(anyString(), anyString())).thenReturn("[]");
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RuntimeException("Connection failed"));

        // When
        streamingLlmService.streamGenerate("提示", "问题", 0.3, 1000, callback);

        // Then
        assertNotNull(capturedError[0]);
        assertTrue(capturedError[0].getMessage().contains("Connection failed"));
    }
}
