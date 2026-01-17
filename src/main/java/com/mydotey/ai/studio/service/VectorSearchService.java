package com.mydotey.ai.studio.service;

import com.mydotey.ai.studio.dto.SourceDocument;
import com.mydotey.ai.studio.entity.DocumentChunk;
import com.mydotey.ai.studio.mapper.DocumentChunkMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 向量搜索服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VectorSearchService {

    private final DocumentChunkMapper documentChunkMapper;
    private final EmbeddingService embeddingService;

    /**
     * 根据问题搜索相关的文档分块
     *
     * @param question 用户问题
     * @param knowledgeBaseIds 知识库 ID 列表
     * @param topK 返回结果数量
     * @param scoreThreshold 相似度阈值
     * @return 相关文档列表
     */
    public List<SourceDocument> search(
            String question,
            List<Long> knowledgeBaseIds,
            int topK,
            double scoreThreshold) {

        log.info("Searching for relevant documents, question: {}, kbIds: {}, topK: {}",
                question, knowledgeBaseIds, topK);

        // 1. 生成问题的向量
        float[] queryEmbedding = embeddingService.embed(question);

        // 2. 向量相似度搜索
        List<DocumentChunk> chunks = documentChunkMapper.searchByEmbedding(
                queryEmbedding,
                knowledgeBaseIds,
                topK,
                scoreThreshold
        );

        log.info("Found {} relevant chunks", chunks.size());

        // 3. 转换为 SourceDocument
        return chunks.stream()
                .map(this::toSourceDocument)
                .collect(Collectors.toList());
    }

    /**
     * 转换为 SourceDocument
     */
    private SourceDocument toSourceDocument(DocumentChunk chunk) {
        return SourceDocument.builder()
                .documentId(chunk.getDocumentId())
                .documentName(extractDocumentName(chunk))
                .chunkIndex(chunk.getChunkIndex())
                .content(chunk.getContent())
                .score(chunk.getSimilarityScore())
                .build();
    }

    /**
     * 提取文档名称（从 metadata 或其他地方）
     */
    private String extractDocumentName(DocumentChunk chunk) {
        // 简化版：实际需要从文档表查询
        return "Document_" + chunk.getDocumentId();
    }
}
