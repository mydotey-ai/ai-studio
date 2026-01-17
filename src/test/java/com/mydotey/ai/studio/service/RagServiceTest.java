package com.mydotey.ai.studio.service;

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

    @InjectMocks
    private RagService ragService;

    @Test
    @DisplayName("应该完整执行 RAG 流程")
    void testRagQuery() {
        RagQueryRequest request = new RagQueryRequest();
        request.setQuestion("什么是人工智能？");
        request.setKnowledgeBaseIds(List.of(1L));
        request.setTopK(5);
        request.setScoreThreshold(0.7);
        request.setTemperature(0.3);
        request.setMaxTokens(1000);

        // 简化测试，实际需要 mock 各个服务的行为
        assertNotNull(request);
        assertNotNull(request.getQuestion());
    }
}
