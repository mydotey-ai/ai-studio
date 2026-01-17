package com.mydotey.ai.studio.service.parser;

import java.io.InputStream;

/**
 * 文档解析器接口
 */
public interface DocumentParser {
    /**
     * 从输入流中提取文本内容
     * @param inputStream 文件输入流
     * @param fileName 文件名（用于判断文件类型）
     * @return 提取的文本内容
     * @throws Exception 解析失败时抛出异常
     */
    String extractText(InputStream inputStream, String fileName) throws Exception;

    /**
     * 判断是否支持该文件类型
     * @param fileName 文件名
     * @return 是否支持
     */
    boolean supports(String fileName);
}
