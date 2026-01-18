package com.mydotey.ai.studio.controller;

import com.mydotey.ai.studio.common.ApiResponse;
import com.mydotey.ai.studio.dto.RagQueryRequest;
import com.mydotey.ai.studio.dto.RagQueryResponse;
import com.mydotey.ai.studio.service.RagService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
    @DisplayName("当服务抛出异常时应该传播异常")
    void testQueryWithServiceException() {
        // Given
        RagQueryRequest request = new RagQueryRequest();
        request.setQuestion("测试问题");
        request.setKnowledgeBaseIds(List.of(1L));

        when(ragService.query(any(RagQueryRequest.class), anyLong()))
                .thenThrow(new RuntimeException("Service error"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            ragController.query(request, 1L);
        });

        assertEquals("Service error", exception.getMessage());

        verify(ragService).query(any(RagQueryRequest.class), eq(1L));
    }
}
