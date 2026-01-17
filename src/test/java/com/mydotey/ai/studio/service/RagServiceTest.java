package com.mydotey.ai.studio.service;

import com.mydotey.ai.studio.config.LlmConfig;
import com.mydotey.ai.studio.dto.LlmResponse;
import com.mydotey.ai.studio.dto.RagQueryRequest;
import com.mydotey.ai.studio.dto.RagQueryResponse;
import com.mydotey.ai.studio.dto.SourceDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("RAG 服务测试")
@ExtendWith(MockitoExtension.class)
class RagServiceTest {

    @Mock
    private VectorSearchService vectorSearchService;

    @Mock
    private ContextBuilderService contextBuilderService;

    @Mock
    private PromptTemplateService promptTemplateService;

    @Mock
    private LlmGenerationService llmGenerationService;

    @Mock
    private LlmConfig llmConfig;

    @Mock
    private KnowledgeBaseService knowledgeBaseService;

    @InjectMocks
    private RagService ragService;

    @Test
    @DisplayName("应该完整执行 RAG 流程")
    void testRagQuery() {
        // 1. 准备测试数据
        String question = "什么是人工智能？";
        List<Long> kbIds = List.of(1L);
        Long userId = 1L;

        // 2. 准备 mock 的相关文档
        List<SourceDocument> mockSources = List.of(
                SourceDocument.builder()
                        .documentId(100L)
                        .documentName("AI简介.pdf")
                        .chunkIndex(0)
                        .content("人工智能是计算机科学的一个分支")
                        .score(0.95)
                        .build()
        );

        // 3. Mock knowledgeBaseService.validateAccess() - 验证权限（无异常）
        doNothing().when(knowledgeBaseService).validateAccess(eq(kbIds), eq(userId));

        // 4. Mock vectorSearchService.search() to return mockSources
        when(vectorSearchService.search(eq(question), eq(kbIds), anyInt(), anyDouble()))
                .thenReturn(mockSources);

        // 5. Mock contextBuilderService.buildContext()
        when(contextBuilderService.buildContext(eq(question), eq(mockSources), isNull()))
                .thenReturn("知识库内容：人工智能是计算机科学的一个分支");

        // 6. Mock promptTemplateService.buildSystemPrompt()
        when(promptTemplateService.buildSystemPrompt(anyString()))
                .thenReturn("你是一个专业的助手");

        // 7. Mock llmGenerationService.generate()
        LlmResponse mockLlmResponse = LlmResponse.builder()
                .content("根据知识库，人工智能是计算机科学的一个分支")
                .totalTokens(100)
                .build();
        when(llmGenerationService.generate(anyString(), anyString(), anyDouble(), anyInt()))
                .thenReturn(mockLlmResponse);

        // Mock llmConfig.getModel()
        when(llmConfig.getModel()).thenReturn("gpt-4");

        // 8. 准备请求并调用
        RagQueryRequest request = new RagQueryRequest();
        request.setQuestion(question);
        request.setKnowledgeBaseIds(kbIds);
        request.setTopK(5);
        request.setScoreThreshold(0.7);
        request.setTemperature(0.3);
        request.setMaxTokens(1000);
        request.setIncludeSources(true);

        // 9. 执行并验证
        RagQueryResponse response = ragService.query(request, userId);

        // 10. 验证结果
        assertNotNull(response);
        assertNotNull(response.getAnswer());
        assertEquals("根据知识库，人工智能是计算机科学的一个分支", response.getAnswer());
        assertTrue(response.getIsComplete());
        assertEquals(1, response.getSources().size());
        assertEquals("AI简介.pdf", response.getSources().get(0).getDocumentName());
        assertEquals(100, response.getTotalTokens().intValue());
        assertEquals("gpt-4", response.getModel());

        // 11. 验证所有服务被正确调用
        verify(knowledgeBaseService).validateAccess(eq(kbIds), eq(userId));
        verify(vectorSearchService).search(eq(question), eq(kbIds), eq(5), eq(0.7));
        verify(contextBuilderService).buildContext(eq(question), eq(mockSources), isNull());
        verify(promptTemplateService).buildSystemPrompt(anyString());
        verify(llmGenerationService).generate(anyString(), anyString(), eq(0.3), eq(1000));
    }
}
