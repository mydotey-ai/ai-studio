# Phase 4 RAG System Tests Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 完善 Phase 4 RAG 系统的测试覆盖，为 LlmGenerationService、StreamingLlmService 和 RagController 添加全面的单元测试。

**Architecture:** 使用 Mockito 进行依赖 Mock，采用 TDD 方法，每个测试类独立验证服务逻辑，避免外部 API 依赖。使用 Spring Boot Test 注解进行集成测试。

**Tech Stack:** JUnit 5, Mockito, Spring Boot Test, Java 21

---

## Task 1: LlmGenerationService 单元测试

**Files:**
- Create: `src/test/java/com/mydotey/ai/studio/service/LlmGenerationServiceTest.java`

**Step 1: Write failing test - generate with valid request**

```java
@Test
@DisplayName("应该成功生成回答并解析响应")
void testGenerateSuccess() {
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
```

**Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=LlmGenerationServiceTest#testGenerateSuccess`
Expected: FAIL with class not found

**Step 3: Write test class skeleton**

```java
package com.mydotey.ai.studio.service;

import com.mydotey.ai.studio.config.LlmConfig;
import com.mydotey.ai.studio.dto.LlmResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
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
}
```

**Step 4: Run test to verify it compiles**

Run: `mvn test -Dtest=LlmGenerationServiceTest#testGenerateSuccess`
Expected: FAIL (test exists but config mocks not set up)

**Step 5: Write failing test - generate with null temperature**

```java
@Test
@DisplayName("当 temperature 为 null 时应该使用默认值")
void testGenerateWithNullTemperature() {
    // Given
    when(config.getDefaultTemperature()).thenReturn(0.5);
    when(config.getDefaultMaxTokens()).thenReturn(500);
    when(config.getModel()).thenReturn("gpt-3.5-turbo");
    when(promptTemplateService.buildMessages(anyString(), anyString()))
            .thenReturn("[]");
    when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
            .thenReturn(new ResponseEntity<>("{\"choices\":[{\"message\":{\"content\":\"test\"},\"finish_reason\":\"stop\"}]}", HttpStatus.OK));

    // When
    llmGenerationService.generate("系统提示", "用户问题", null, null);

    // Then
    verify(restTemplate).postForEntity(anyString(), argThat(entity -> {
        String body = entity.getBody();
        return body.contains("\"temperature\":0.5") && body.contains("\"max_tokens\":500");
    }), eq(String.class));
}
```

**Step 6: Run test to verify it fails**

Run: `mvn test -Dtest=LlmGenerationServiceTest#testGenerateWithNullTemperature`
Expected: FAIL (config not mocked properly)

**Step 7: Write failing test - parseLlmResponse without usage**

```java
@Test
@DisplayName("当响应没有 usage 字段时应该正确处理")
void testParseResponseWithoutUsage() {
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

    when(config.getModel()).thenReturn("gpt-3.5-turbo");
    when(config.getDefaultTemperature()).thenReturn(0.3);
    when(config.getDefaultMaxTokens()).thenReturn(1000);
    when(promptTemplateService.buildMessages(anyString(), anyString())).thenReturn("[]");
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
```

**Step 8: Run test to verify it fails**

Run: `mvn test -Dtest=LlmGenerationServiceTest#testParseResponseWithoutUsage`
Expected: FAIL (same as above)

**Step 9: Write failing test - handle API error**

```java
@Test
@DisplayName("当 API 调用失败时应该抛出异常")
void testGenerateApiError() {
    // Given
    when(config.getModel()).thenReturn("gpt-3.5-turbo");
    when(config.getDefaultTemperature()).thenReturn(0.3);
    when(config.getDefaultMaxTokens()).thenReturn(1000);
    when(promptTemplateService.buildMessages(anyString(), anyString())).thenReturn("[]");
    when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
            .thenThrow(new RuntimeException("API error"));

    // When & Then
    RuntimeException exception = assertThrows(RuntimeException.class, () -> {
        llmGenerationService.generate("系统提示", "用户问题", 0.3, 1000);
    });

    assertTrue(exception.getMessage().contains("Failed to generate response"));
}
```

**Step 10: Run test to verify it fails**

Run: `mvn test -Dtest=LlmGenerationServiceTest#testGenerateApiError`
Expected: FAIL (same as above)

**Step 11: Commit**

```bash
git add src/test/java/com/mydotey/ai/studio/service/LlmGenerationServiceTest.java
git commit -m "test: add LlmGenerationService unit tests"
```

---

## Task 2: StreamingLlmService 单元测试

**Files:**
- Create: `src/test/java/com/mydotey/ai/studio/service/StreamingLlmServiceTest.java`

**Step 1: Write failing test - stream generate with valid response**

```java
@Test
@DisplayName("应该成功流式生成并回调")
void testStreamGenerateSuccess() {
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
    when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
            .thenReturn(new ResponseEntity<>(streamResponseBody, HttpStatus.OK));

    // When
    streamingLlmService.streamGenerate(systemPrompt, userQuestion, 0.3, 1000, callback);

    // Then
    assertEquals(2, receivedContent.size());
    assertEquals("人工智能", receivedContent.get(0));
    assertEquals("是", receivedContent.get(1));
}
```

**Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=StreamingLlmServiceTest#testStreamGenerateSuccess`
Expected: FAIL with class not found

**Step 3: Write test class skeleton**

```java
package com.mydotey.ai.studio.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mydotey.ai.studio.config.LlmConfig;
import org.junit.jupiter.api.DisplayName;
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
}
```

**Step 4: Run test to verify it compiles**

Run: `mvn test -Dtest=StreamingLlmServiceTest#testStreamGenerateSuccess`
Expected: FAIL (test exists but config mocks not set up)

**Step 5: Write failing test - stream with [DONE] marker**

```java
@Test
@DisplayName("应该正确处理 [DONE] 标记")
void testStreamWithDoneMarker() {
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

    when(promptTemplateService.buildMessages(anyString(), anyString())).thenReturn("[]");
    when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
            .thenReturn(new ResponseEntity<>(streamResponseBody, HttpStatus.OK));

    // When
    streamingLlmService.streamGenerate("提示", "问题", 0.3, 1000, callback);

    // Then
    assertTrue(completed[0]);
}
```

**Step 6: Run test to verify it fails**

Run: `mvn test -Dtest=StreamingLlmServiceTest#testStreamWithDoneMarker`
Expected: FAIL (same as above)

**Step 7: Write failing test - stream with error**

```java
@Test
@DisplayName("当流式请求失败时应该调用 onError")
void testStreamWithError() {
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

    when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
            .thenThrow(new RuntimeException("Connection failed"));

    // When
    streamingLlmService.streamGenerate("提示", "问题", 0.3, 1000, callback);

    // Then
    assertNotNull(capturedError[0]);
    assertTrue(capturedError[0].getMessage().contains("Connection failed"));
}
```

**Step 8: Run test to verify it fails**

Run: `mvn test -Dtest=StreamingLlmServiceTest#testStreamWithError`
Expected: FAIL (same as above)

**Step 9: Commit**

```bash
git add src/test/java/com/mydotey/ai/studio/service/StreamingLlmServiceTest.java
git commit -m "test: add StreamingLlmService unit tests"
```

---

## Task 3: RagController 集成测试

**Files:**
- Create: `src/test/java/com/mydotey/ai/studio/controller/RagControllerTest.java`

**Step 1: Write failing test - query endpoint returns success**

```java
@Test
@DisplayName("POST /api/rag/query 应该返回成功响应")
void testQuerySuccess() {
    // Given
    RagQueryRequest request = new RagQueryRequest();
    request.setQuestion("什么是人工智能？");
    request.setKnowledgeBaseIds(List.of(1L));
    request.setTopK(5);

    RagQueryResponse mockResponse = RagQueryResponse.builder()
            .answer("人工智能是计算机科学的一个分支")
            .sources(List.of())
            .model("gpt-3.5-turbo")
            .totalTokens(100)
            .isComplete(true)
            .build();

    when(ragService.query(any(RagQueryRequest.class), anyLong()))
            .thenReturn(mockResponse);

    // When
    ApiResponse<RagQueryResponse> response = ragController.query(request);

    // Then
    assertNotNull(response);
    assertTrue(response.getSuccess());
    assertEquals("人工智能是计算机科学的一个分支", response.getData().getAnswer());
    assertEquals(100, response.getData().getTotalTokens());

    verify(ragService).query(any(RagQueryRequest.class), anyLong());
}
```

**Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=RagControllerTest#testQuerySuccess`
Expected: FAIL with class not found

**Step 3: Write test class skeleton**

```java
package com.mydotey.ai.studio.controller;

import com.mydotey.ai.studio.common.ApiResponse;
import com.mydotey.ai.studio.dto.RagQueryRequest;
import com.mydotey.ai.studio.dto.RagQueryResponse;
import com.mydotey.ai.studio.service.RagService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("RAG 控制器测试")
@ExtendWith(MockitoExtension.class)
class RagControllerTest {

    @Mock
    private RagService ragService;

    @InjectMocks
    private RagController ragController;
}
```

**Step 4: Run test to verify it compiles**

Run: `mvn test -Dtest=RagControllerTest#testQuerySuccess`
Expected: FAIL (missing imports)

**Step 5: Write failing test - query with invalid request**

```java
@Test
@DisplayName("当请求参数无效时应该返回错误")
void testQueryWithInvalidRequest() {
    // Given
    RagQueryRequest request = new RagQueryRequest();
    // question is null - should fail validation

    // When
    ApiResponse<RagQueryResponse> response = ragController.query(request);

    // Then
    assertNotNull(response);
    assertFalse(response.getSuccess());

    verify(ragService, never()).query(any(RagQueryRequest.class), anyLong());
}
```

**Step 6: Run test to verify it fails**

Run: `mvn test -Dtest=RagControllerTest#testQueryWithInvalidRequest`
Expected: FAIL (same as above)

**Step 7: Commit**

```bash
git add src/test/java/com/mydotey/ai/studio/controller/RagControllerTest.java
git commit -m "test: add RagController unit tests"
```

---

## Task 4: 运行所有测试并验证

**Step 1: Run all new tests**

Run: `mvn test`
Expected: All 45+ tests pass (42 existing + new tests)

**Step 2: Verify test coverage**

Run: `mvn jacoco:report`
Expected: Report shows coverage for LlmGenerationService, StreamingLlmService, RagController

**Step 3: Update PROJECT_PROGRESS.md**

Modify: `docs/PROJECT_PROGRESS.md`

Add to Phase 4 section:
```markdown
**测试覆盖（完成）：**
- VectorSearchServiceTest - 4 个测试 ✅
- ContextBuilderServiceTest - 2 个测试 ✅
- PromptTemplateServiceTest - 11 个测试 ✅
- RagServiceTest - 1 个测试 ✅
- RagIntegrationTest - 3 个测试 ✅
- LlmGenerationServiceTest - 4 个测试 ✅
- StreamingLlmServiceTest - 3 个测试 ✅
- RagControllerTest - 2 个测试 ✅

**总计：30 个测试**
```

**Step 4: Commit documentation update**

```bash
git add docs/PROJECT_PROGRESS.md
git commit -m "docs: update Phase 4 test coverage in PROJECT_PROGRESS"
```

---

## Summary

完成此计划后，Phase 4 RAG 系统将拥有完整的测试覆盖：

| 服务 | 测试文件 | 测试数量 |
|------|----------|----------|
| VectorSearchService | VectorSearchServiceTest | 4 |
| ContextBuilderService | ContextBuilderServiceTest | 2 |
| PromptTemplateService | PromptTemplateServiceTest | 11 |
| LlmGenerationService | LlmGenerationServiceTest | 4 (新增) |
| StreamingLlmService | StreamingLlmServiceTest | 3 (新增) |
| RagService | RagServiceTest | 1 |
| RagController | RagControllerTest | 2 (新增) |
| RagIntegration | RagIntegrationTest | 3 |
| **总计** | | **30** |

所有测试将遵循 TDD 原则，使用 Mockito 进行依赖隔离，确保测试快速、可靠且易于维护。
