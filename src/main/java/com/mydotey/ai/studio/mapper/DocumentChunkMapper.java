package com.mydotey.ai.studio.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mydotey.ai.studio.entity.DocumentChunk;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DocumentChunkMapper extends BaseMapper<DocumentChunk> {

    /**
     * 向量相似度搜索（余弦相似度）
     *
     * @param queryEmbedding 查询向量
     * @param knowledgeBaseIds 知识库 ID 列表
     * @param topK 返回结果数量
     * @param scoreThreshold 相似度阈值
     * @return 相关的分块列表
     */
    List<DocumentChunk> searchByEmbedding(
            @Param("queryEmbedding") float[] queryEmbedding,
            @Param("knowledgeBaseIds") List<Long> knowledgeBaseIds,
            @Param("topK") int topK,
            @Param("scoreThreshold") double scoreThreshold
    );
}
