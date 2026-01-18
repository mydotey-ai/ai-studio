package com.mydotey.ai.studio.controller;

import com.mydotey.ai.studio.common.ApiResponse;
import com.mydotey.ai.studio.dto.RagQueryRequest;
import com.mydotey.ai.studio.dto.RagQueryResponse;
import com.mydotey.ai.studio.service.RagService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RAG 控制器测试")
class RagControllerTest {

    @Mock
    private RagService ragService;

    @InjectMocks
    private RagController ragController;

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
        ApiResponse<RagQueryResponse> response = ragController.query(request, 1L);

        // Then
        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertEquals("Success", response.getMessage());
        assertNotNull(response.getData());
        assertEquals("人工智能是计算机科学的一个分支", response.getData().getAnswer());
        assertEquals("gpt-3.5-turbo", response.getData().getModel());
        assertEquals(100, response.getData().getTotalTokens());
        assertTrue(response.getData().getIsComplete());

        verify(ragService).query(any(RagQueryRequest.class), eq(1L));
    }

    @Test
    @DisplayName("当请求参数无效时应该返回错误")
    void testQueryWithInvalidRequest() {
        // Given
        RagQueryRequest request = new RagQueryRequest();
        // question is null - should fail validation
        request.setKnowledgeBaseIds(List.of(1L));
        request.setTopK(5);

        // When - manually validate
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();
        Set<ConstraintViolation<RagQueryRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("Question") ||
                                v.getPropertyPath().toString().equals("question")));

        // Verify service would never be called due to validation
        verify(ragService, never()).query(any(RagQueryRequest.class), anyLong());
    }
}
