package com.mydotey.ai.studio.service;

import com.mydotey.ai.studio.dto.ModelConfigDto;
import com.mydotey.ai.studio.dto.ModelConfigRequest;
import com.mydotey.ai.studio.dto.ModelConfigResponse;
import com.mydotey.ai.studio.enums.ModelConfigType;

import java.util.List;

public interface ModelConfigService {

    /**
     * 创建模型配置
     */
    ModelConfigResponse create(ModelConfigRequest request, Long orgId, Long userId);

    /**
     * 更新模型配置
     */
    ModelConfigResponse update(Long id, ModelConfigRequest request, Long orgId);

    /**
     * 删除模型配置
     */
    void delete(Long id, Long orgId);

    /**
     * 获取模型配置详情
     */
    ModelConfigResponse getById(Long id, Long orgId);

    /**
     * 获取组织的所有模型配置
     */
    List<ModelConfigResponse> listByOrg(Long orgId, ModelConfigType type);

    /**
     * 获取默认模型配置
     */
    ModelConfigDto getDefaultConfig(ModelConfigType type);

    /**
     * 设置默认配置
     */
    void setDefault(Long id, Long orgId);

    /**
     * 根据ID获取配置
     */
    ModelConfigDto getConfigById(Long id);

    /**
     * 测试配置是否有效
     */
    boolean testConfig(Long id);
}
