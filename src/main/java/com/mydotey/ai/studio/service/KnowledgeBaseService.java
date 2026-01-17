package com.mydotey.ai.studio.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mydotey.ai.studio.common.exception.BusinessException;
import com.mydotey.ai.studio.dto.CreateKnowledgeBaseRequest;
import com.mydotey.ai.studio.dto.KnowledgeBaseResponse;
import com.mydotey.ai.studio.dto.UpdateKnowledgeBaseRequest;
import com.mydotey.ai.studio.entity.Document;
import com.mydotey.ai.studio.entity.KnowledgeBase;
import com.mydotey.ai.studio.entity.User;
import com.mydotey.ai.studio.mapper.DocumentMapper;
import com.mydotey.ai.studio.mapper.KnowledgeBaseMapper;
import com.mydotey.ai.studio.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KnowledgeBaseService {

    private final KnowledgeBaseMapper kbMapper;
    private final UserMapper userMapper;
    private final DocumentMapper documentMapper;

    public KnowledgeBaseResponse create(CreateKnowledgeBaseRequest request, Long userId) {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setName(request.getName());
        kb.setDescription(request.getDescription());
        kb.setOwnerId(userId);
        kb.setIsPublic(request.getIsPublic());
        kb.setEmbeddingModel(request.getEmbeddingModel());
        kb.setChunkSize(request.getChunkSize());
        kb.setChunkOverlap(request.getChunkOverlap());
        kb.setCreatedAt(Instant.now());
        kb.setUpdatedAt(Instant.now());

        kbMapper.insert(kb);
        return toResponse(kb);
    }

    public KnowledgeBaseResponse update(Long id, UpdateKnowledgeBaseRequest request, Long userId) {
        KnowledgeBase kb = kbMapper.selectById(id);
        if (kb == null) {
            throw new BusinessException("Knowledge base not found");
        }

        if (!kb.getOwnerId().equals(userId)) {
            throw new BusinessException("You don't have permission to update this knowledge base");
        }

        kb.setName(request.getName());
        kb.setDescription(request.getDescription());
        if (request.getIsPublic() != null) {
            kb.setIsPublic(request.getIsPublic());
        }
        if (request.getEmbeddingModel() != null) {
            kb.setEmbeddingModel(request.getEmbeddingModel());
        }
        if (request.getChunkSize() != null) {
            kb.setChunkSize(request.getChunkSize());
        }
        if (request.getChunkOverlap() != null) {
            kb.setChunkOverlap(request.getChunkOverlap());
        }
        kb.setUpdatedAt(Instant.now());

        kbMapper.updateById(kb);
        return toResponse(kb);
    }

    public void delete(Long id, Long userId) {
        KnowledgeBase kb = kbMapper.selectById(id);
        if (kb == null) {
            throw new BusinessException("Knowledge base not found");
        }

        if (!kb.getOwnerId().equals(userId)) {
            throw new BusinessException("You don't have permission to delete this knowledge base");
        }

        kbMapper.deleteById(id);
    }

    public KnowledgeBaseResponse get(Long id, Long userId) {
        KnowledgeBase kb = kbMapper.selectById(id);
        if (kb == null) {
            throw new BusinessException("Knowledge base not found");
        }

        if (!kb.getOwnerId().equals(userId) && !kb.getIsPublic()) {
            throw new BusinessException("You don't have permission to access this knowledge base");
        }

        return toResponse(kb);
    }

    public IPage<KnowledgeBaseResponse> list(Long userId, int page, int size) {
        Page<KnowledgeBase> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<KnowledgeBase> wrapper = new LambdaQueryWrapper<KnowledgeBase>()
                .and(w -> w.eq(KnowledgeBase::getOwnerId, userId)
                        .or().eq(KnowledgeBase::getIsPublic, true))
                .orderByDesc(KnowledgeBase::getCreatedAt);

        IPage<KnowledgeBase> result = kbMapper.selectPage(pageParam, wrapper);
        return result.convert(this::toResponse);
    }

    /**
     * 验证用户是否有权限访问指定的知识库
     *
     * @param kbIds 知识库 ID 列表
     * @param userId 用户 ID
     * @throws BusinessException 如果用户没有权限访问任何请求的知识库
     */
    public void validateAccess(List<Long> kbIds, Long userId) {
        if (kbIds == null || kbIds.isEmpty()) {
            throw new BusinessException("Knowledge base IDs cannot be empty");
        }

        // 检查每个知识库的访问权限
        for (Long kbId : kbIds) {
            KnowledgeBase kb = kbMapper.selectById(kbId);
            if (kb == null) {
                throw new BusinessException("Knowledge base not found: " + kbId);
            }

            if (!kb.getOwnerId().equals(userId) && !kb.getIsPublic()) {
                throw new BusinessException("You don't have permission to access knowledge base: " + kbId);
            }
        }
    }

    private KnowledgeBaseResponse toResponse(KnowledgeBase kb) {
        User owner = userMapper.selectById(kb.getOwnerId());
        Long documentCount = documentMapper.selectCount(
                new LambdaQueryWrapper<Document>().eq(Document::getKbId, kb.getId()));

        return new KnowledgeBaseResponse(
                kb.getId(),
                kb.getName(),
                kb.getDescription(),
                kb.getOwnerId(),
                owner != null ? owner.getUsername() : null,
                kb.getIsPublic(),
                kb.getEmbeddingModel(),
                kb.getChunkSize(),
                kb.getChunkOverlap(),
                documentCount.intValue(),
                kb.getCreatedAt(),
                kb.getUpdatedAt()
        );
    }
}
