package com.mydotey.ai.studio.service;

import com.mydotey.ai.studio.dto.SourceDocument;
import com.mydotey.ai.studio.entity.DocumentChunk;
import com.mydotey.ai.studio.mapper.DocumentChunkMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("向量搜索服务测试")
@ExtendWith(MockitoExtension.class)
class VectorSearchServiceTest {

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private DocumentChunkMapper documentChunkMapper;

    @InjectMocks
    private VectorSearchService vectorSearchService;

    private String question;
    private List<Long> kbIds;
    private int topK;
    private double scoreThreshold;

    @BeforeEach
    void setUp() {
        question = "什么是人工智能？";
        kbIds = List.of(1L, 2L);
        topK = 5;
        scoreThreshold = 0.7;
    }

    @Test
    @DisplayName("应该根据问题检索相关文档")
    void testSearchRelevantDocuments() {
        // Given: 准备测试数据
        float[] testEmbedding = new float[]{0.1f, 0.2f, 0.3f, 0.4f, 0.5f};

        // Mock: EmbeddingService.embed() 返回测试向量
        when(embeddingService.embed(question)).thenReturn(testEmbedding);

        // Mock: DocumentChunkMapper.searchByEmbedding() 返回测试文档分块
        List<DocumentChunk> mockChunks = List.of(
                createMockDocumentChunk(1L, 100L, 0, "人工智能是计算机科学的一个分支", 0.95),
                createMockDocumentChunk(2L, 100L, 1, "AI 模型可以学习和推理", 0.88),
                createMockDocumentChunk(3L, 200L, 0, "机器学习是 AI 的子领域", 0.82)
        );

        when(documentChunkMapper.searchByEmbedding(
                eq(testEmbedding),
                eq(kbIds),
                eq(topK),
                eq(scoreThreshold)
        )).thenReturn(mockChunks);

        // When: 调用搜索方法
        List<SourceDocument> result = vectorSearchService.search(question, kbIds, topK, scoreThreshold);

        // Then: 验证结果
        assertNotNull(result);
        assertEquals(3, result.size());

        // 验证第一个文档
        SourceDocument doc1 = result.get(0);
        assertEquals(100L, doc1.getDocumentId());
        assertEquals("Document_100", doc1.getDocumentName());
        assertEquals(0, doc1.getChunkIndex());
        assertEquals("人工智能是计算机科学的一个分支", doc1.getContent());
        assertEquals(0.95, doc1.getScore(), 0.001);

        // 验证第二个文档
        SourceDocument doc2 = result.get(1);
        assertEquals(100L, doc2.getDocumentId());
        assertEquals("Document_100", doc2.getDocumentName());
        assertEquals(1, doc2.getChunkIndex());
        assertEquals("AI 模型可以学习和推理", doc2.getContent());
        assertEquals(0.88, doc2.getScore(), 0.001);

        // 验证第三个文档
        SourceDocument doc3 = result.get(2);
        assertEquals(200L, doc3.getDocumentId());
        assertEquals("Document_200", doc3.getDocumentName());
        assertEquals(0, doc3.getChunkIndex());
        assertEquals("机器学习是 AI 的子领域", doc3.getContent());
        assertEquals(0.82, doc3.getScore(), 0.001);

        // Then: 验证依赖交互
        verify(embeddingService, times(1)).embed(question);
        verify(documentChunkMapper, times(1)).searchByEmbedding(
                eq(testEmbedding),
                eq(kbIds),
                eq(topK),
                eq(scoreThreshold)
        );
    }

    @Test
    @DisplayName("当没有找到相关文档时应该返回空列表")
    void testSearchReturnsEmptyListWhenNoResults() {
        // Given: 准备测试数据
        float[] testEmbedding = new float[]{0.1f, 0.2f, 0.3f};

        // Mock: EmbeddingService.embed() 返回测试向量
        when(embeddingService.embed(question)).thenReturn(testEmbedding);

        // Mock: DocumentChunkMapper.searchByEmbedding() 返回空列表
        when(documentChunkMapper.searchByEmbedding(
                any(float[].class),
                anyList(),
                anyInt(),
                anyDouble()
        )).thenReturn(List.of());

        // When: 调用搜索方法
        List<SourceDocument> result = vectorSearchService.search(question, kbIds, topK, scoreThreshold);

        // Then: 验证结果
        assertNotNull(result);
        assertTrue(result.isEmpty());

        // Then: 验证依赖交互
        verify(embeddingService, times(1)).embed(question);
        verify(documentChunkMapper, times(1)).searchByEmbedding(
                eq(testEmbedding),
                eq(kbIds),
                eq(topK),
                eq(scoreThreshold)
        );
    }

    @Test
    @DisplayName("应该正确处理单个知识库的搜索")
    void testSearchWithSingleKnowledgeBase() {
        // Given: 准备测试数据
        List<Long> singleKbId = List.of(1L);
        float[] testEmbedding = new float[]{0.1f, 0.2f, 0.3f};

        // Mock: EmbeddingService.embed() 返回测试向量
        when(embeddingService.embed(question)).thenReturn(testEmbedding);

        // Mock: DocumentChunkMapper.searchByEmbedding() 返回测试文档分块
        List<DocumentChunk> mockChunks = List.of(
                createMockDocumentChunk(1L, 100L, 0, "测试内容", 0.9)
        );

        when(documentChunkMapper.searchByEmbedding(
                eq(testEmbedding),
                eq(singleKbId),
                eq(topK),
                eq(scoreThreshold)
        )).thenReturn(mockChunks);

        // When: 调用搜索方法
        List<SourceDocument> result = vectorSearchService.search(question, singleKbId, topK, scoreThreshold);

        // Then: 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(100L, result.get(0).getDocumentId());
        assertEquals("测试内容", result.get(0).getContent());
        assertEquals(0.9, result.get(0).getScore(), 0.001);

        // Then: 验证依赖交互
        verify(embeddingService, times(1)).embed(question);
        verify(documentChunkMapper, times(1)).searchByEmbedding(
                eq(testEmbedding),
                eq(singleKbId),
                eq(topK),
                eq(scoreThreshold)
        );
    }

    @Test
    @DisplayName("应该正确处理不同的 topK 和 scoreThreshold 参数")
    void testSearchWithDifferentParameters() {
        // Given: 准备测试数据
        int customTopK = 10;
        double customThreshold = 0.8;
        float[] testEmbedding = new float[]{0.1f, 0.2f, 0.3f};

        // Mock: EmbeddingService.embed() 返回测试向量
        when(embeddingService.embed(question)).thenReturn(testEmbedding);

        // Mock: DocumentChunkMapper.searchByEmbedding() 返回测试文档分块
        List<DocumentChunk> mockChunks = List.of(
                createMockDocumentChunk(1L, 100L, 0, "高相似度内容", 0.92)
        );

        when(documentChunkMapper.searchByEmbedding(
                eq(testEmbedding),
                eq(kbIds),
                eq(customTopK),
                eq(customThreshold)
        )).thenReturn(mockChunks);

        // When: 调用搜索方法
        List<SourceDocument> result = vectorSearchService.search(question, kbIds, customTopK, customThreshold);

        // Then: 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());

        // Then: 验证使用正确的参数调用依赖
        verify(embeddingService, times(1)).embed(question);
        verify(documentChunkMapper, times(1)).searchByEmbedding(
                eq(testEmbedding),
                eq(kbIds),
                eq(customTopK),
                eq(customThreshold)
        );
    }

    /**
     * 创建测试用的 DocumentChunk 对象
     */
    private DocumentChunk createMockDocumentChunk(Long id, Long documentId, Integer chunkIndex,
                                                 String content, double score) {
        DocumentChunk chunk = new DocumentChunk();
        chunk.setId(id);
        chunk.setDocumentId(documentId);
        chunk.setChunkIndex(chunkIndex);
        chunk.setContent(content);
        chunk.setEmbedding("0.1,0.2,0.3");
        chunk.setMetadata("{}");
        chunk.setCreatedAt(Instant.now());
        chunk.setSimilarityScore(score);
        return chunk;
    }
}
