package com.mydotey.ai.studio.dto;

import lombok.Data;

/**
 * 文档上传响应
 */
@Data
public class DocumentUploadResponse {

    /**
     * 文档 ID
     */
    private Long documentId;

    /**
     * 文件名
     */
    private String filename;

    /**
     * 文件大小
     */
    private Long fileSize;

    /**
     * 处理状态
     */
    private String status;

    /**
     * 消息
     */
    private String message;
}
