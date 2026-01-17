package com.mydotey.ai.studio.dto;

import lombok.Data;

/**
 * 文本分块配置
 */
@Data
public class ChunkConfig {
    /**
     * 块大小（字符数），默认 500
     */
    private int chunkSize = 500;

    /**
     * 块重叠大小（字符数），默认 100
     */
    private int chunkOverlap = 100;

    /**
     * 是否按段落分块，默认 true
     */
    private boolean chunkByParagraph = true;
}
