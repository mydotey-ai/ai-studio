package com.mydotey.ai.studio.service;

import java.util.List;

/**
 * 向量化服务接口
 */
public interface EmbeddingService {

    /**
     * 将文本转换为向量
     * @param text 文本内容
     * @return 向量数组
     */
    float[] embed(String text);

    /**
     * 批量将文本转换为向量
     * @param texts 文本列表
     * @return 向量列表
     */
    List<float[]> embedBatch(List<String> texts);

    /**
     * 获取向量维度
     * @return 向量维度
     */
    int getDimension();
}
