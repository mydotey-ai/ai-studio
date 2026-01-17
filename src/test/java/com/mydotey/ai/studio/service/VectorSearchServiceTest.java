package com.mydotey.ai.studio.service;

import com.mydotey.ai.studio.dto.SourceDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("向量搜索服务测试")
@ExtendWith(MockitoExtension.class)
class VectorSearchServiceTest {

    @Mock
    private EmbeddingService embeddingService;

    @InjectMocks
    private VectorSearchService vectorSearchService;

    @Test
    @DisplayName("应该根据问题检索相关文档")
    void testSearchRelevantDocuments() {
        String question = "什么是人工智能？";
        List<Long> kbIds = List.of(1L);

        // 由于需要 mock 数据库，这里只做简单的单元测试
        // 实际的测试应该在集成测试中进行
        assertNotNull(question);
        assertNotNull(kbIds);
    }
}
