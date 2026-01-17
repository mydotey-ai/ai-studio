package com.mydotey.ai.studio.service;

import com.mydotey.ai.studio.dto.ChunkConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("文本分块服务测试")
class TextChunkingServiceTest {

    private final TextChunkingService service = new TextChunkingService();
    private final ChunkConfig config = new ChunkConfig();

    @Test
    @DisplayName("应该将文本分成多个块")
    void testChunkText() {
        String text = "这是第一段。\n\n这是第二段。\n\n这是第三段。";
        List<String> chunks = service.chunkText(text, config);
        assertTrue(chunks.size() > 0);
    }

    @Test
    @DisplayName("块之间应该有重叠")
    void testChunksHaveOverlap() {
        config.setChunkSize(100);
        config.setChunkOverlap(50);
        String longText = generateLongText(300);
        List<String> chunks = service.chunkText(longText, config);
        if (chunks.size() > 1) {
            String firstChunk = chunks.get(0);
            String secondChunk = chunks.get(1);
            // 检查第二块包含第一块末尾的内容
            assertTrue(secondChunk.contains(firstChunk.substring(
                Math.max(0, firstChunk.length() - 20)
            )));
        }
    }

    @Test
    @DisplayName("空文本应该返回空列表")
    void testEmptyTextReturnsEmptyList() {
        List<String> chunks = service.chunkText("", config);
        assertTrue(chunks.isEmpty());
    }

    private String generateLongText(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append("测试");
        }
        return sb.toString();
    }
}
