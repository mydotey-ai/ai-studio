package com.mydotey.ai.studio.service;

import com.mydotey.ai.studio.dto.ChunkConfig;
import com.mydotey.ai.studio.entity.Document;
import com.mydotey.ai.studio.entity.DocumentChunk;
import com.mydotey.ai.studio.entity.KnowledgeBase;
import com.mydotey.ai.studio.mapper.DocumentChunkMapper;
import com.mydotey.ai.studio.mapper.DocumentMapper;
import com.mydotey.ai.studio.service.parser.DocumentParser;
import com.mydotey.ai.studio.util.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * 文档处理服务
 * 负责文档解析、分块、向量化和存储
 */
@Slf4j
@Service
public class DocumentProcessingService {

    private final List<DocumentParser> parsers;
    private final TextChunkingService chunkingService;
    private final EmbeddingService embeddingService;
    private final DocumentMapper documentMapper;
    private final DocumentChunkMapper chunkMapper;
    private final KnowledgeBaseService knowledgeBaseService;
    private final FileUtil fileUtil;

    public DocumentProcessingService(List<DocumentParser> parsers,
                                     TextChunkingService chunkingService,
                                     EmbeddingService embeddingService,
                                     DocumentMapper documentMapper,
                                     DocumentChunkMapper chunkMapper,
                                     KnowledgeBaseService knowledgeBaseService,
                                     FileUtil fileUtil) {
        this.parsers = parsers;
        this.chunkingService = chunkingService;
        this.embeddingService = embeddingService;
        this.documentMapper = documentMapper;
        this.chunkMapper = chunkMapper;
        this.knowledgeBaseService = knowledgeBaseService;
        this.fileUtil = fileUtil;
    }

    /**
     * 异步处理文档
     */
    @Async("documentProcessingExecutor")
    @Transactional
    public void processDocument(Long documentId) {
        log.info("Starting to process document: {}", documentId);

        try {
            // 1. 获取文档记录
            Document document = documentMapper.selectById(documentId);
            if (document == null) {
                log.error("Document not found: {}", documentId);
                return;
            }

            // 2. 读取文件内容
            byte[] fileBytes = Files.readAllBytes(Paths.get(document.getFileUrl()));

            // 3. 提取文本
            String extractedText = extractText(document.getFilename(), fileBytes);

            if (extractedText == null || extractedText.trim().isEmpty()) {
                throw new RuntimeException("Failed to extract text from document");
            }

            // 4. 获取知识库配置
            KnowledgeBase kb = knowledgeBaseService.getById(document.getKbId());
            ChunkConfig chunkConfig = createChunkConfig(kb);

            // 5. 分块
            List<String> chunks = chunkingService.chunkText(extractedText, chunkConfig);
            log.info("Document {} split into {} chunks", documentId, chunks.size());

            // 6. 向量化并存储
            processChunks(document, chunks);

            // 7. 更新文档状态
            document.setStatus("COMPLETED");
            document.setChunkCount(chunks.size());
            documentMapper.updateById(document);

            log.info("Document {} processed successfully", documentId);

        } catch (Exception e) {
            log.error("Failed to process document: {}", documentId, e);

            // 更新文档状态为失败
            Document document = new Document();
            document.setId(documentId);
            document.setStatus("FAILED");
            document.setErrorMessage(e.getMessage());
            documentMapper.updateById(document);
        }
    }

    /**
     * 从文件内容提取文本
     */
    private String extractText(String filename, byte[] fileBytes) throws Exception {
        // 查找支持的解析器
        for (DocumentParser parser : parsers) {
            if (parser.supports(filename)) {
                try (InputStream inputStream = new java.io.ByteArrayInputStream(fileBytes)) {
                    return parser.extractText(inputStream, filename);
                }
            }
        }

        // 如果没有找到支持的解析器，尝试作为纯文本读取
        return new String(fileBytes);
    }

    /**
     * 处理分块
     */
    private void processChunks(Document document, List<String> chunks) {
        for (int i = 0; i < chunks.size(); i++) {
            String chunkText = chunks.get(i);

            // 生成向量
            float[] embedding = embeddingService.embed(chunkText);

            // 创建分块记录
            DocumentChunk chunk = new DocumentChunk();
            chunk.setDocumentId(document.getId());
            chunk.setChunkIndex(i);
            chunk.setContent(chunkText);
            chunk.setEmbedding(floatArrayToJson(embedding));
            chunk.setMetadata("{}");

            // 保存到数据库
            chunkMapper.insert(chunk);
        }
    }

    /**
     * 创建分块配置
     */
    private ChunkConfig createChunkConfig(KnowledgeBase kb) {
        ChunkConfig config = new ChunkConfig();
        if (kb != null) {
            config.setChunkSize(kb.getChunkSize());
            config.setChunkOverlap(kb.getChunkOverlap());
        }
        return config;
    }

    /**
     * 将 float 数组转换为 JSON 字符串
     */
    private String floatArrayToJson(float[] array) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < array.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(array[i]);
        }
        sb.append("]");
        return sb.toString();
    }
}
