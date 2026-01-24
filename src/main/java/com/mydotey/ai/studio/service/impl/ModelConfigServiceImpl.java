package com.mydotey.ai.studio.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mydotey.ai.studio.dto.ModelConfigDto;
import com.mydotey.ai.studio.dto.ModelConfigRequest;
import com.mydotey.ai.studio.dto.ModelConfigResponse;
import com.mydotey.ai.studio.entity.ModelConfig;
import com.mydotey.ai.studio.enums.ModelConfigType;
import com.mydotey.ai.studio.exception.BusinessException;
import com.mydotey.ai.studio.mapper.ModelConfigMapper;
import com.mydotey.ai.studio.service.ModelConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModelConfigServiceImpl implements ModelConfigService {

    private final ModelConfigMapper modelConfigMapper;

    @Override
    @Transactional
    public ModelConfigResponse create(ModelConfigRequest request, Long orgId, Long userId) {
        // 验证必填字段
        validateRequest(request);

        // 如果设置为默认，取消其他默认配置
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            removePreviousDefault(request.getType(), orgId);
        }

        ModelConfig config = new ModelConfig();
        BeanUtils.copyProperties(request, config);
        config.setOrgId(orgId);
        config.setCreatedBy(userId);
        config.setCreatedAt(Instant.now());
        config.setUpdatedAt(Instant.now());
        config.setStatus("active");

        modelConfigMapper.insert(config);

        return toResponse(config);
    }

    @Override
    @Transactional
    public ModelConfigResponse update(Long id, ModelConfigRequest request, Long orgId) {
        ModelConfig config = modelConfigMapper.selectById(id);
        if (config == null || !config.getOrgId().equals(orgId)) {
            throw new BusinessException("配置不存在或无权访问");
        }

        validateRequest(request);

        // 如果设置为默认，取消其他默认配置
        if (Boolean.TRUE.equals(request.getIsDefault()) && !Boolean.TRUE.equals(config.getIsDefault())) {
            removePreviousDefault(request.getType(), orgId);
        }

        BeanUtils.copyProperties(request, config, "id", "orgId", "createdAt", "createdBy");
        config.setUpdatedAt(Instant.now());

        modelConfigMapper.updateById(config);

        return toResponse(config);
    }

    @Override
    @Transactional
    public void delete(Long id, Long orgId) {
        ModelConfig config = modelConfigMapper.selectById(id);
        if (config == null || !config.getOrgId().equals(orgId)) {
            throw new BusinessException("配置不存在或无权访问");
        }

        if (Boolean.TRUE.equals(config.getIsDefault())) {
            throw new BusinessException("默认配置不能删除");
        }

        modelConfigMapper.deleteById(id);
    }

    @Override
    public ModelConfigResponse getById(Long id, Long orgId) {
        ModelConfig config = modelConfigMapper.selectById(id);
        if (config == null || !config.getOrgId().equals(orgId)) {
            throw new BusinessException("配置不存在或无权访问");
        }
        return toResponse(config);
    }

    @Override
    public List<ModelConfigResponse> listByOrg(Long orgId, ModelConfigType type) {
        LambdaQueryWrapper<ModelConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ModelConfig::getOrgId, orgId);
        if (type != null) {
            wrapper.eq(ModelConfig::getType, type);
        }
        wrapper.eq(ModelConfig::getStatus, "active");
        wrapper.orderByDesc(ModelConfig::getIsDefault);
        wrapper.orderByDesc(ModelConfig::getCreatedAt);

        return modelConfigMapper.selectList(wrapper).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ModelConfigDto getDefaultConfig(ModelConfigType type) {
        LambdaQueryWrapper<ModelConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ModelConfig::getType, type);
        wrapper.eq(ModelConfig::getIsDefault, true);
        wrapper.eq(ModelConfig::getStatus, "active");

        ModelConfig config = modelConfigMapper.selectOne(wrapper);
        if (config != null) {
            return toDto(config);
        }

        // 返回配置文件中的默认值
        return getFallbackConfig(type);
    }

    @Override
    @Transactional
    public void setDefault(Long id, Long orgId) {
        ModelConfig config = modelConfigMapper.selectById(id);
        if (config == null || !config.getOrgId().equals(orgId)) {
            throw new BusinessException("配置不存在或无权访问");
        }

        // 取消其他默认配置
        removePreviousDefault(config.getType(), orgId);

        // 设置为默认
        config.setIsDefault(true);
        config.setUpdatedAt(Instant.now());
        modelConfigMapper.updateById(config);
    }

    @Override
    public ModelConfigDto getConfigById(Long id) {
        ModelConfig config = modelConfigMapper.selectById(id);
        if (config == null) {
            throw new BusinessException("配置不存在");
        }
        return toDto(config);
    }

    @Override
    public boolean testConfig(Long id) {
        ModelConfig config = modelConfigMapper.selectById(id);
        if (config == null) {
            throw new BusinessException("配置不存在");
        }

        // TODO: 实现实际测试逻辑
        // 根据配置类型调用对应的API进行测试
        log.info("Testing config: {}", config.getName());
        return true;
    }

    private void validateRequest(ModelConfigRequest request) {
        if (request.getType() == ModelConfigType.EMBEDDING && request.getDimension() == null) {
            throw new BusinessException("向量模型必须设置维度");
        }
    }

    private void removePreviousDefault(ModelConfigType type, Long orgId) {
        LambdaQueryWrapper<ModelConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ModelConfig::getType, type);
        wrapper.eq(ModelConfig::getOrgId, orgId);
        wrapper.eq(ModelConfig::getIsDefault, true);

        ModelConfig previousDefault = modelConfigMapper.selectOne(wrapper);
        if (previousDefault != null) {
            previousDefault.setIsDefault(false);
            previousDefault.setUpdatedAt(Instant.now());
            modelConfigMapper.updateById(previousDefault);
        }
    }

    private ModelConfigDto toDto(ModelConfig config) {
        ModelConfigDto dto = new ModelConfigDto();
        BeanUtils.copyProperties(config, dto);
        return dto;
    }

    private ModelConfigResponse toResponse(ModelConfig config) {
        ModelConfigResponse response = new ModelConfigResponse();
        BeanUtils.copyProperties(config, response);
        // 部分隐藏API Key
        response.setMaskedApiKey(maskApiKey(config.getApiKey()));
        return response;
    }

    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() <= 8) {
            return "****";
        }
        return apiKey.substring(0, 8) + "..." + apiKey.substring(apiKey.length() - 4);
    }

    private ModelConfigDto getFallbackConfig(ModelConfigType type) {
        // 从配置文件读取默认值
        ModelConfigDto dto = new ModelConfigDto();
        dto.setType(type);

        if (type == ModelConfigType.EMBEDDING) {
            dto.setEndpoint("https://api.openai.com/v1");
            dto.setModel("text-embedding-ada-002");
            dto.setDimension(1536);
            dto.setTimeout(30000);
        } else {
            dto.setEndpoint("https://api.openai.com/v1");
            dto.setModel("ModelEmbeddingServiceImpl");
            dto.setTemperature(0.3);
            dto.setMaxTokens(1000);
            dto.setTimeout(60000);
            dto.setEnableStreaming(true);
        }

        return dto;
    }
}
