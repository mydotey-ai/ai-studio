package com.mydotey.ai.studio.service;

import com.mydotey.ai.studio.dto.ChunkConfig;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 文本分块服务
 */
@Service
public class TextChunkingService {

    /**
     * 将文本分成多个块
     * @param text 原始文本
     * @param config 分块配置
     * @return 分块后的文本列表
     */
    public List<String> chunkText(String text, ChunkConfig config) {
        List<String> chunks = new ArrayList<>();

        if (text == null || text.trim().isEmpty()) {
            return chunks;
        }

        if (config.isChunkByParagraph()) {
            chunks.addAll(chunkByParagraph(text, config));
        } else {
            chunks.addAll(chunkBySize(text, config));
        }

        return chunks;
    }

    /**
     * 按段落分块（智能分块）
     */
    private List<String> chunkByParagraph(String text, ChunkConfig config) {
        List<String> chunks = new ArrayList<>();
        String[] paragraphs = text.split("\\n\\s*\\n|\\r\\n\\s*\\r\\n");

        StringBuilder currentChunk = new StringBuilder();

        for (String paragraph : paragraphs) {
            paragraph = paragraph.trim();
            if (paragraph.isEmpty()) {
                continue;
            }

            // 如果当前块加上新段落超过限制
            if (currentChunk.length() + paragraph.length() > config.getChunkSize()
                    && currentChunk.length() > 0) {
                chunks.add(currentChunk.toString());
                // 保留重叠部分
                String overlapText = extractOverlapText(currentChunk.toString(), config.getChunkOverlap());
                currentChunk = new StringBuilder(overlapText);
            }

            if (currentChunk.length() > 0) {
                currentChunk.append("\n\n");
            }
            currentChunk.append(paragraph);
        }

        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString());
        }

        return chunks;
    }

    /**
     * 按固定大小分块
     */
    private List<String> chunkBySize(String text, ChunkConfig config) {
        List<String> chunks = new ArrayList<>();
        int chunkSize = config.getChunkSize();
        int overlap = config.getChunkOverlap();

        if (text.length() <= chunkSize) {
            chunks.add(text);
            return chunks;
        }

        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            chunks.add(text.substring(start, end));
            start = end - overlap;
            if (start >= text.length() - overlap) {
                break;
            }
        }

        return chunks;
    }

    /**
     * 提取重叠文本
     */
    private String extractOverlapText(String text, int overlapSize) {
        if (text.length() <= overlapSize) {
            return text;
        }
        return text.substring(text.length() - overlapSize);
    }
}
