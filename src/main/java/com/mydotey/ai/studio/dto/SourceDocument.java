package com.mydotey.ai.studio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SourceDocument {
    /**
     * 文档 ID
     */
    private Long documentId;

    /**
     * 文档名称
     */
    private String documentName;

    /**
     * 分块索引
     */
    private Integer chunkIndex;

    /**
     * 相关内容
     */
    private String content;

    /**
     * 相似度分数
     */
    private Double score;
}
