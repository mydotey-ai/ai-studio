# Model Config Database Management Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 将大模型配置（向量模型、LLM模型）从配置文件迁移到数据库管理，支持多配置和知识库/Agent级别关联

**Architecture:** 创建ModelConfig实体和表，修改KnowledgeBase和Agent添加模型配置关联，提供CRUD API和前端管理页面，修改EmbeddingService和LlmGenerationService从数据库读取配置

**Tech Stack:** Spring Boot 3.5.0, PostgreSQL, MyBatis Plus, Vue 3, Element Plus, Flyway

---

### Task 1: 创建 ModelConfigType 枚举

**Files:**
- Create: `src/main/java/com/mydotey/ai/studio/enums/ModelConfigType.java`

**Step 1: 创建文件**

```java
package com.mydotey.ai.studio.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 模型配置类型
 */
public enum ModelConfigType {
    EMBEDDING("embedding", "向量模型"),
    LLM("llm", "大语言模型");

    @EnumValue
    private final String code;
    private final String description;

    ModelConfigType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static ModelConfigType fromCode(String code) {
        for (ModelConfigType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown model config type: " + code);
    }
}
```

**Step 2: Commit**

```bash
git add src/main/java/com/mydotey/ai/studio/enums/ModelConfigType.java
git commit -m "feat: add ModelConfigType enum"
```

---

### Task 2: 创建 ModelConfig 实体类

**Files:**
- Create: `src/main/java/com/mydotey/ai/studio/entity/ModelConfig.java`

**Step 1: 创建文件**

```java
package com.mydotey.ai.studio.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mydotey.ai.studio.enums.ModelConfigType;
import lombok.Data;

import java.time.Instant;

@Data
@TableName("model_configs")
public class ModelConfig {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orgId;

    private ModelConfigType type;

    private String name;

    private String endpoint;

    private String apiKey;

    private String model;

    private Integer dimension; // 向量模型专用

    private Double temperature; // LLM模型专用

    private Integer maxTokens; // LLM模型专用

    private Integer timeout;

    private Boolean enableStreaming; // LLM模型专用

    private Boolean isDefault;

    private String status; // active, inactive

    private String description;

    private Long createdBy;

    private Instant createdAt;

    private Instant updatedAt;
}
```

**Step 2: Commit**

```bash
git add src/main/java/com/mydotey/ai/studio/entity/ModelConfig.java
git commit -m "feat: add ModelConfig entity"
```

---

### Task 3: 创建数据库迁移脚本

**Files:**
- Create: `src/main/resources/db/migration/V13__model_configs_table.sql`

**Step 1: 创建迁移脚本**

```sql
-- 创建模型配置表
CREATE TABLE IF NOT EXISTS model_configs (
    id BIGSERIAL PRIMARY KEY,
    org_id BIGINT REFERENCES organizations(id) ON DELETE CASCADE,
    type VARCHAR(20) NOT NULL,
    name VARCHAR(100) NOT NULL,
    endpoint VARCHAR(500),
    api_key VARCHAR(500),
    model VARCHAR(100),
    dimension INTEGER,
    temperature DECIMAL(3,2),
    max_tokens INTEGER,
    timeout INTEGER DEFAULT 30000,
    enable_streaming BOOLEAN DEFAULT true,
    is_default BOOLEAN DEFAULT false,
    status VARCHAR(20) DEFAULT 'active',
    description TEXT,
    created_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT check_type CHECK (type IN ('embedding', 'llm')),
    CONSTRAINT check_status CHECK (status IN ('active', 'inactive'))
);

-- 创建索引
CREATE INDEX idx_model_configs_type ON model_configs(type);
CREATE INDEX idx_model_configs_org_id ON model_configs(org_id);
CREATE INDEX idx_model_configs_is_default ON model_configs(is_default) WHERE is_default = true;

-- 添加注释
COMMENT ON TABLE model_configs IS '模型配置表';
COMMENT ON COLUMN model_configs.type IS '配置类型: embedding(向量模型) 或 llm(大语言模型)';
COMMENT ON COLUMN model_configs.is_default IS '是否为默认配置，每个类型只有一个默认配置';
```

**Step 2: Commit**

```bash
git add src/main/resources/db/migration/V13__model_configs_table.sql
git commit -m "feat: add model_configs table migration"
```

---

### Task 4: 创建 ModelConfig DTO 类

**Files:**
- Create: `src/main/java/com/mydotey/ai/studio/dto/ModelConfigDto.java`
- Create: `src/main/java/com/mydotct/ai/studio/dto/ModelConfigRequest.java`
- Create: `src/main/java/com/mydotey/ai/studio/dto/ModelConfigResponse.java`

**Step 1: 创建 ModelConfigDto.java**

```java
package com.mydotey.ai.studio.dto;

import com.mydotey.ai.studio.enums.ModelConfigType;
import lombok.Data;

import java.time.Instant;

@Data
public class ModelConfigDto {
    private Long id;
    private Long orgId;
    private ModelConfigType type;
    private String name;
    private String endpoint;
    private String apiKey; // 部分隐藏
    private String model;
    private Integer dimension;
    private Double temperature;
    private Integer maxTokens;
    private Integer timeout;
    private Boolean enableStreaming;
    private Boolean isDefault;
    private String status;
    private String description;
    private Long createdBy;
    private Instant createdAt;
    private Instant updatedAt;
}
```

**Step 2: 创建 ModelConfigRequest.java**

```java
package com.mydotey.ai.studio.dto;

import com.mydotey.ai.studio.enums.ModelConfigType;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class ModelConfigRequest {
    @NotBlank(message = "名称不能为空")
    private String name;

    @NotNull(message = "类型不能为空")
    private ModelConfigType type;

    @NotBlank(message = "端点不能为空")
    private String endpoint;

    @NotBlank(message = "API Key不能为空")
    private String apiKey;

    @NotBlank(message = "模型不能为空")
    private String model;

    private Integer dimension; // 向量模型必填

    private Double temperature;

    private Integer maxTokens;

    private Integer timeout = 30000;

    private Boolean enableStreaming = true;

    private Boolean isDefault = false;

    private String description;
}
```

**Step 3: 创建 ModelConfigResponse.java**

```java
package com.mydotey.ai.studio.dto;

import com.mydotey.ai.studio.enums.ModelConfigType;
import lombok.Data;

import java.time.Instant;

@Data
public class ModelConfigResponse {
    private Long id;
    private Long orgId;
    private ModelConfigType type;
    private String name;
    private String endpoint;
    private String maskedApiKey; // 部分隐藏的API Key
    private String model;
    private Integer dimension;
    private Double temperature;
    private Integer maxTokens;
    private Integer timeout;
    private Boolean enableStreaming;
    private Boolean isDefault;
    private String status;
    private String description;
    private Long createdBy;
    private Instant createdAt;
    private Instant updatedAt;
}
```

**Step 4: Commit**

```bash
git add src/main/java/com/mydotey/ai/studio/dto/ModelConfigDto.java \
        src/main/java/com/mydotey/ai/studio/dto/ModelConfigRequest.java \
        src/main/java/com/mydotey/ai/studio/dto/ModelConfigResponse.java
git commit -m "feat: add ModelConfig DTO classes"
```

---

### Task 5: 创建 ModelConfigMapper

**Files:**
- Create: `src/main/java/com/mydotey/ai/studio/mapper/ModelConfigMapper.java`

**Step 1: 创建文件**

```java
package com.mydotey.ai.studio.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mydotey.ai.studio.entity.ModelConfig;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ModelConfigMapper extends BaseMapper<ModelConfig> {
}
```

**Step 2: Commit**

```bash
git add src/main/java/com/mydotey/ai/studio/mapper/ModelConfigMapper.java
git commit -m "feat: add ModelConfigMapper"
```

---

### Task 6: 创建 ModelConfigService

**Files:**
- Create: `src/main/java/com/mydotey/ai/studio/service/ModelConfigService.java` (interface)
- Create: `src/main/java/com/mydotey/ai/studio/service/impl/ModelConfigServiceImpl.java`

**Step 1: 创建 ModelConfigService.java**

```java
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
```

**Step 2: 创建 ModelConfigServiceImpl.java**

```java
package com.mydotey.ai.stars.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mydotey.ai.studio.dto.ModelConfigDto;
import com.mydotey.ai.studio.dto.ModelConfigRequest;
import com.mydotey.ai.studio.dto.dto.ModelConfigResponse;
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
);
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
            dto.setModel("gpt-3.5-turbo");
            dto.setTemperature(0.3);
            dto.setMaxTokens(1000);
            dto.setTimeout(60000);
            dto.setEnableStreaming(true);
        }

        return dto;
    }
}
```

**Step 3: Commit**

```bash
git add src/main/java/com/mydotey/ai/studio/service/ModelConfigService.java \
        src/main/java/com/mydotey/ai/studio/service/impl/ModelConfigServiceImpl.java
git commit -m "feat: add ModelConfigService"
```

---

### Task 7: 创建 ModelConfigController

**Files:**
- Create: `src/main/java/com/mydotey/ai/studio/controller/ModelConfigController.java`

**Step 1: 创建文件**

```java
package com.mydotey.ai.studio.controller;

import com.mydotey.ai.studio.dto.ApiResponse;
import com.mydotey.ai.studio.dto.ModelConfigRequest;
import com.mydotey.ai.studio.dto.ModelConfigResponse;
import com.mydotey.ai.studio.enums.ModelConfigType;
import com.mydotey.ai.studio.service.ModelConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "模型配置", description = "模型配置管理接口")
@RestController
@RequestMapping("/api/model-configs")
@RequiredArgsConstructor
public class ModelConfigController {

    private final ModelConfigService modelConfigService;

    @Operation(summary = "创建模型配置")
    @PostMapping
    public ApiResponse<ModelConfigResponse> create(
            @RequestBody ModelConfigRequest request,
            @AuthenticationPrincipal Long userId) {
        // Long orgId = getOrgIdFromUser(userId);
        Long orgId = 1L; // TODO: 从用户获取组织ID
        return ApiResponse.success(modelConfigService.create(request, orgId, userId));
    }

    @Operation(summary = "更新模型配置")
    @PutMapping("/{id}")
    public ApiResponse<ModelConfigResponse> update(
            @PathVariable Long id,
            @RequestBody ModelConfigRequest request,
            @AuthenticationPrincipal Long userId) {
        // Long orgId = getOrgIdFromUser(userId);
        Long orgId = 1L;
        return ApiResponse.success(modelConfigService.update(id, request, orgId));
    }

    @Operation(summary = "删除模型配置")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId) {
        // Long orgId = getOrgIdFromUser(userId);
        Long orgId = 1L;
        modelConfigService.delete(id, orgId);
        return ApiResponse.success(null);
    }

    @Operation(summary = "获取模型配置详情")
    @GetMapping("/{id}")
    public ApiResponse<ModelConfigResponse> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId) {
        // Long orgId = getOrgIdFromUser(userId);
        Long orgId = 1L;
        return ApiResponse.success(modelConfigService.getById(id, orgId));
    }

    @Operation(summary = "获取模型配置列表")
    @GetMapping
    public ApiResponse<List<ModelConfigResponse>> list(
            @Parameter(description = "模型类型") @RequestParam(required = false) ModelConfigType type,
            @AuthenticationPrincipal Long userId) {
        // Long orgId = getOrgIdFromUser(userId);
        Long orgId = 1L;
        return ApiResponse.success(modelConfigService.listByOrg(orgId, type));
    }

    @Operation(summary = "设置默认配置")
    @PutMapping("/{id}/set-default")
    public ApiResponse<Void> setDefault(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId) {
        // Long orgId = getOrgIdFromUser(userId);
        Long orgId = 1L;
        modelConfigService.setDefault(id, orgId);
        return ApiResponse.success(null);
    }

    @Operation(summary = "测试配置")
    @PostMapping("/{id}/test")
    public ApiResponse<Boolean> testConfig(@PathVariable Long id) {
        boolean result = modelConfigService.testConfig(id);
        return ApiResponse.success(result);
    }
}
```

**Step 2: Commit**

```bash
git add src/main/java/com/mydotey/ai/studio/controller/ModelConfigController.java
git commit -m "feat: add ModelConfigController"
```

---

### Task 8: 修改 KnowledgeBase 实体添加模型配置关联

**Files:**
- Modify: `src/main/java/com/mydotey/ai/studio/entity/KnowledgeBase.java`

**Step 1: 添加字段**

在 `KnowledgeBase.java` 中添加两个字段：

```java
private Long embeddingModelId; // 关联的向量模型配置ID

private Long llmModelId; // 关联的LLM模型配置ID
```

**Step 2: 创建迁移脚本添加列**

**Files:**
- Create: `src/main/resources/db/migration/V14__add_model_config_fields_to_knowledge_base.sql`

```sql
-- 为知识库表添加模型配置关联字段
ALTER TABLE knowledge_bases
ADD COLUMN IF NOT EXISTS embedding_model_id BIGINT REFERENCES model_configs(id) ON DELETE SET NULL,
ADD COLUMN IF NOT EXISTS llm_model_id BIGINT REFERENCES model_configs(id) ON DELETE SET NULL;

-- 添加注释
COMMENT ON COLUMN knowledge_bases.embedding_model_id IS '关联的向量模型配置ID';
COMMENT ON COLUMN knowledge_bases.llm_model_id IS '关联的LLM模型配置ID';
```

**Step 3: Commit**

```bash
git add src/main/java/com/mydotey/ai/studio/entity/KnowledgeBase.java \
        src/main/resources/db/migration/V14__add_model_config_fields_to_knowledge_base.sql
git commit -m "feat: add model config fields to KnowledgeBase"
```

---

### Task 9: 修改 Agent 实体添加 LLM 模型配置关联

**Files:**
- Modify: `src/main/java/com/mydotey/ai/studio/entity/Agent.java`

**Step 1: 添加字段**

在 `Agent.java` 中添加字段：

```java
private Long llmModelId; // 关联的LLM模型配置ID
```

**Step 2: 创建迁移脚本添加列**

**Files:**
- Create: `src/main/resources/db/migration/V15__add_llm_model_config_field_to_agent.sql`

```sql
-- 为Agent表添加LLM模型配置关联字段
ALTER TABLE agents
ADD COLUMN IF NOT EXISTS llm_model_id BIGINT REFERENCES model_configs(id) ON DELETE SET NULL;

-- 添加注释
COMMENT ON COLUMN agents.llm_model_id IS '关联的LLM模型配置ID';
```

**Step 3: Commit**

```bash
git add src/main/java/com/mydotey/ai/studio/entity/Agent.java \
        src/main/resources/db/migration/V15__add_llm_model_config_field_to_agent.sql
git commit -m "feat: add llm model config field to Agent"
```

---

### Task 10: 修改 EmbeddingService 支持动态配置

**Files:**
- Create: `src/main/java/com/mydotey/ai/studio/service/impl/ModelEmbeddingServiceImpl.java`

**Step 1: 创建实现类**

```java
package com.mydotey.ai.studio.service.impl;

import com.mydotey.ai.studio.dto.ModelConfigDto;
import com.mydotey.ai.studio.service.EmbeddingService;
import com.mydotey.ai.studio.service.ModelConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Slf4j
@Service("modelEmbeddingService")
@RequiredArgsConstructor
public class ModelEmbeddingServiceImpl implements EmbeddingService {

    private final ModelConfigService modelConfigService;

    @Override
    public float[] embed(String text) {
        ModelConfigDto config = modelConfigService.getDefaultConfig(ModelConfigType.EMBEDDING);
        return embedWithConfig(text, config);
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        ModelConfigDto config = modelConfigService.getDefaultConfig(ModelConfigType.EMBEDDING);
        return embedBatchWithConfig(texts, config);
    }

    @Override
    public int getDimension() {
        ModelConfigDto config = modelConfigService.getDefaultConfig(ModelConfigType.EMBEDDING);
        return config.getDimension();
    }

    public float[] embedWithConfig(String text, ModelConfigDto config) {
        // TODO: 实现实际的向量生成逻辑，使用config中的配置
        log.info("Embedding with config: {}", config.getName());
        return new float[config.getDimension()];
    }

    public List<float[]> embedBatchWithConfig(List<String> texts, ModelConfigDto config) {
        // TODO: 实现批量向量生成
        return texts.stream().map(text -> embedWithConfig(text, config)).toList();
    }
}
```

**Step 2: Commit**

```bash
git add src/main/java/com/mydotey/ai/studio/service/impl/ModelEmbeddingServiceImpl.java
git commit -m "feat: add ModelEmbeddingServiceImpl with dynamic config"
```

---

### Task 11: 前端 - 创建模型配置 API 接口

**Files:**
- Create: `frontend/src/api/modelConfig.ts`

**Step 1: 创建文件**

```typescript
import request from '@/utils/request'
import { ModelConfigType } from '@/enums/modelConfigType'

export interface ModelConfig {
  id: number
  orgId: number
  type: ModelConfigType
  name: string
  endpoint: string
  maskedApiKey: string
  model: string
  dimension?: number
  temperature?: number
  maxTokens?: number
  timeout: number
  enableStreaming?: boolean
  isDefault: boolean
  status: string
  description?: string
  createdBy?: number
  createdAt: string
  updatedAt: string
}

export interface ModelConfigRequest {
  name: string
  type: ModelConfigType
  endpoint: string
  apiKey: string
  model: string
  dimension?: number
  temperature?: number
  maxTokens?: number
  timeout?: number
  enableStreaming?: boolean
  isDefault?: boolean
  description?: string
}

export const modelConfigApi = {
  // 获取模型配置列表
  getList: (type?: ModelConfigType) => {
    return request.get<ModelConfig[]>('/api/model-configs', {
      params: { type }
    })
  },

  // 获取模型配置详情
  getById: (id: number) => {
    return request.get<ModelConfig>(`/api/model-configs/${id}`)
  },

  // 创建模型配置
  create: (data: ModelConfigRequest) => {
    return request.post<ModelConfig>('/api/model-configs', data)
  },

  // 更新模型配置
  update: (id: number, data: ModelConfigRequest) => {
    return request.put<ModelConfig>(`/api/model-configs/${id}`, data)
  },

  // 删除模型配置
  delete: (id: number) => {
    return request.delete(`/api/model-configs/${id}`)
  },

  // 设置默认配置
  setDefault: (id: number) => {
    return request.put(`/api/model-configs/${id}/set-default`)
  },

  // 测试配置
  test: (id: number) => {
    return request.post(`/api/model-configs/${id}/test`)
  }
}
```

**Step 2: Commit**

```bash
git add frontend/src/api/modelConfig.ts
git commit -m "feat: add model config API"
```

---

### Task 12: 前端 - 创建模型配置枚举

**Files:**
- Create: `frontend/src/enums/modelConfigType.ts`

**Step 1: 创建文件**

```typescript
export enum ModelConfigType {
  EMBEDDING = 'embedding',
  LLM = 'llm'
}

export const ModelConfigTypeLabels = {
  [ModelConfigType.EMBEDDING]: '向量模型',
  [ModelConfigType.LLM]: '大语言模型'
}
```

**Step 2: Commit**

```bash
git add frontend/src/enums/modelConfigType.ts
git commit -m "feat: add ModelConfigType enum"
```

---

### Task 13: 前端 - 创建模型配置管理页面

**Files:**
- Create: `frontend/src/views/ModelConfig.vue`

**Step 1: 创建文件**

```vue
<template>
  <div class="model-config-container">
    <el-page-header @back="$router.go(-1)" title="返回">
      <template #content>
        <span class="text-large font-600">模型配置管理</span>
      </template>
    </el-page-header>

    <el-card class="config-card" style="margin-top: 20px">
      <template #header>
        <div class="card-header">
          <el-tabs v-model="activeType" @tab-change="handleTypeChange">
            <el-tab-pane label="向量模型" :name="ModelConfigType.EMBEDDING" />
            <el-tab-pane label="大语言模型" :name="ModelConfigType.LLM" />
          </el-tabs>
          <el-button type="primary" @click="handleCreate">
            <el-icon><Plus /></el-icon>
            新增配置
          </el-button>
        </div>
      </template>

      <el-table :data="configs" v-loading="loading">
        <el-table-column prop="name" label="名称" />
        <el-table-column prop="endpoint" label="端点" />
        <el-table-column prop="model" label="模型" />
        <el-table-column prop="dimension" label="维度" v-if="activeType === ModelConfigType.EMBEDDING" />
        <el-table-column prop="maskedApiKey" label="API Key" />
        <el-table-column label="默认" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.isDefault" type="success" size="small">默认</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleTest(row)">测试</el-button>
            <el-button link type="primary" @click="handleEdit(row)">编辑</</el-button>
            <el-button
              link
              type="success"
              v-if="!row.isDefault"
              @click="handleSetDefault(row)"
            >
              设为默认
            </el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 创建/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑模型配置' : '新增模型配置'"
      width="600px"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="120px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入配置名称" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="form.type" disabled style="width: 100%">
            <el-option
              v-for="(label, value) in ModelConfigTypeLabels"
              :key="value"
              :label="label"
              :value="value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="端点" prop="endpoint">
          <el-input v-model="form.endpoint" placeholder="https://api.openai.com/v1" />
        </el-form-item>
        <el-form-item label="API Key" prop="apiKey">
          <el-input v-model="form.apiKey" placeholder="请输入API Key" show-password />
        </el-form-item>
        <el-form-item label="模型" prop="model">
          <el-input v-model="form.model" placeholder="gpt-3.5-turbo" />
        </el-form-item>
        <el-form-item
          label="维度"
          prop="dimension"
          v-if="form.type === ModelConfigType.EMBEDDING"
        >
          <el-input-number v-model="form.dimension" :min="1" :max="10000" />
        </el-form-item>
        <el-form-item
          label="Temperature"
          prop="temperature"
          v-if="form.type === ModelConfigType.LLM"
        >
          <el-input-number v-model="form.temperature" :min="0" :max="2" :step="0.1" />
        </el-form-item>
        <el-form-item
          label="Max Tokens"
          prop="maxTokens"
          v-if="form.type === ModelConfigType.LLM"
        >
          <el-input-number v-model="form.maxTokens" :min="1" :max="100000" />
        </el-form-item>
        <el-form-item label="超时(ms)" prop="timeout">
          <el-input-number v-model="form.timeout" :min="1000" :max="300000" :step="1000" />
        </el-form-item>
        <el-form-item
          label="流式输出"
          prop="enableStreaming"
          v-if="form.type === ModelConfigType.LLM"
        >
          <el-switch v-model="form.enableStreaming" />
        </el-form-item>
        <el-form-item label="设为默认" prop="isDefault">
          <el-switch v-model="form.isDefault" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { modelConfigApi, type ModelConfig, type ModelConfigRequest } from '@/api/modelConfig'
import { ModelConfigType, ModelConfigTypeLabels } from '@/enums/modelConfigType'

const activeType = ref(ModelConfigType.EMBEDDING)
const configs = ref<ModelConfig[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<Partial<ModelConfigRequest>>({
  name: '',
  type: ModelConfigType.EMBEDDING,
  endpoint: 'https://api.openai.com/v1',
  apiKey: '',
  model: '',
  dimension: undefined,
  temperature: 0.3,
  maxTokens: 1000,
  timeout: 30000,
  enableStreaming: true,
  isDefault: false,
  description: ''
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  endpoint: [{ required: true, message: '请输入端点', trigger: 'blur' }],
  apiKey: [{ required: true, message: '请输入API Key', trigger: 'blur' }],
  model: [{ required: true, message: '请输入模型', trigger: 'blur' }],
  dimension: [
    {
      required: true,
      message: '请输入维度',
      trigger: 'blur',
      validator: (rule, value, callback) => {
        if (form.type === ModelConfigType.EMBEDDING && !value) {
          callback(new Error('请输入维度'))
        } else {
          callback()
        }
      }
    }
  ]
}

const loadConfigs = async () => {
  loading.value = true
  try {
    const res = await modelConfigApi.getList(activeType.value)
    configs.value = res.data
  } catch (error) {
    ElMessage.error('加载配置失败')
  } finally {
    loading.value = false
  }
}

const handleTypeChange = () => {
  loadConfigs()
}

const handleCreate = () => {
  isEdit.value = false
  Object.assign(form, {
    name: '',
    type: activeType.value,
    endpoint: 'https://api.openai.com/v1',
    apiKey: '',
    model: activeType.value === ModelConfigType.EMBEDDING ? 'text-embedding-ada-002' : 'gpt-3.5-turbo',
    dimension: activeType.value === ModelConfigType.EMBEDDING ? 1536 : undefined,
    temperature: 0.3,
    maxTokens: 1000,
    timeout: 30000,
    enableStreaming: true,
    isDefault: false,
    description: ''
  })
  dialogVisible.value = true
}

const handleEdit = (row: ModelConfig) => {
  isEdit.value = true
  Object.assign(form, {
    name: row.name,
    type: row.type,
    endpoint: row.endpoint,
    apiKey: '',
    model: row.model,
    dimension: row.dimension,
    temperature: row.temperature,
    maxTokens: row.maxTokens,
    timeout: row.timeout,
    enableStreaming: row.enableStreaming,
    isDefault: row.isDefault,
    description: row.description
  })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
    submitting.value = true

    if (isEdit.value) {
      await modelConfigApi.update(currentId.value, form as ModelConfigRequest)
      ElMessage.success('更新成功')
    } else {
      await modelConfigApi.create(form as ModelConfigRequest)
      ElMessage.success('创建成功')
    }

    dialogVisible.value = false
    loadConfigs()
  } catch (error) {
    ElMessage.error('操作失败')
  } finally {
    submitting.value = false
  }
}

const handleSetDefault = async (row: ModelConfig) => {
  try {
    await modelConfigApi.setDefault(row.id)
    ElMessage.success('设置成功')
    loadConfigs()
  } catch (error) {
    ElMessage.error('设置失败')
  }
}

const handleTest = async (row: ModelConfig) => {
  try {
    const res = await modelConfigApi.test(row.id)
    if (res.data) {
      ElMessage.success('配置测试成功')
    } else {
      ElMessage.error('配置测试失败')
    }
  } catch (error) {
    ElMessage.error('配置测试失败')
  }
}

const handleDelete = async (row: ModelConfig) => {
  try {
    await ElMessageBox.confirm(`确定要删除配置"${row.name}"吗？`, '确认删除', {
      type: 'warning'
    })
    await modelConfigApi.delete(row.id)
    ElMessage.success('删除成功')
    loadConfigs()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const formatDate = (date: string) => {
  return new Date(date).toLocaleString()
}

const currentId = ref(0)

onMounted(() => {
  loadConfigs()
})
</script>

<style scoped>
.model-config-container {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.config-card {
  margin-top: 20px;
}
</style>
```

**Step 2: Commit**

```bash
git add frontend/src/views/ModelConfig.vue
git commit -m "feat: add model config management page"
```

---

### Task 14: 前端 - 添加路由

**Files:**
- Modify: `frontend/src/router/index.ts`

**Step 1: 添加路由**

在路由配置中添加模型配置页面路由：

```typescript
{
  path: '/model-config',
  name: 'ModelConfig',
  component: () => import('@/views/ModelConfig.vue'),
  meta: { title: '模型配置' }
}
```

**Step 2: Commit**

```bash
git add frontend/src/router/index.ts
git commit -m "feat: add model config route"
```

---

### Task 15: 前端 - 修改知识库页面添加模型配置选择器

**Files:**
- Modify: `frontend/src/views/KnowledgeBase.vue` (或对应的知识库页面)

**Step 1: 添加模型选择组件**

在知识库创建/编辑表单中添加向量模型和LLM模型选择器：

```vue
<el-form-item label="向量模型" prop="embeddingModelId">
  <el-select v-model="form.embeddingModelId" placeholder="请选择向量模型" clearable>
    <el-option
      v-for="model in embeddingModels"
      :key="model.id"
      :label="`${model.name}${model.isDefault ? ' (默认)' : ''}`"
      :value="model.id"
    />
  </el-select>
</el-form-item>

<el-form-item label="LLM模型" prop="llmModelId">
  <el-select v-model="form.llmModelId" placeholder="请选择LLM模型" clearable>
    <el-option
      v-for="model in llmModels"
      :key="model.id"
      :label="`${model.name}${model.isDefault ? ' (默认)' : ''}`"
      :value="model.id"
    />
  </el-select>
</el-form-item>
```

在 script 中添加加载模型的逻辑：

```typescript
import { modelConfigApi } from '@/api/modelConfig'
import { ModelConfigType } from '@/enums/modelConfigType'

const embeddingModels = ref([])
const llmModels = ref([])

const loadModels = async () => {
  try {
    const [embeddingRes, llmRes] = await Promise.all([
      modelConfigApi.getList(ModelConfigType.EMBEDDING),
      modelConfigApi.getList(ModelConfigType.LLM)
    ])
    embeddingModels.value = embeddingRes.data
    llmModels.value = llmRes.data
  } catch (error) {
    console.error('加载模型配置失败', error)
  }
}

// 在组件挂载时调用
onMounted(() => {
  loadModels()
  // 其他现有逻辑...
})
```

**Step 2: Commit**

```bash
git add frontend/src/views/KnowledgeBase.vue
git commit -m "feat: add model config selector to knowledge base page"
```

---

### Task 16: 前端 - 修改 Agent 页面添加 LLM 模型配置选择器

**Files:**
- Modify: `frontend/src/views/Agent.vue` (或对应的Agent页面)

**Step 1: 添加LLM模型选择器**

```vue
<el-form-item label="LLM模型" prop="llmModelId">
  <el-select v-model="form.llmModelId" placeholder="请选择LLM模型" clearable>
    <el-option
      v-for="model in llmModels"
      :key="model.id"
      :label="`${model.name}${model.isDefault ? ' (默认)' : ''}`"
      :value="model.id"
    />
  </el-select>
</el-form-item>
```

在 script 中添加加载模型的逻辑（类似 Task 15）。

**Step 2: Commit**

```bash
git add frontend/src/views/Agent.vue
git commit -m "feat: add llm model config selector to agent page"
```

---

### Task 17: 创建数据迁移脚本导入现有配置

**Files:**
- Create: `src/main/resources/db/migration/V16__migrate_existing_model_config.sql`

**Step 1: 创建迁移脚本**

```sql
-- 将配置文件中的默认配置导入到数据库中
-- 注意：这个脚本需要手动执行或者在应用启动时由代码执行
-- 这里只是示例，实际需要根据当前配置文件的值来设置

-- 插入默认的向量模型配置
INSERT INTO model_configs (org_id, type, name, endpoint, api_key, model, dimension, timeout, is_default, status, created_at, updated_at)
VALUES (
    1,
    'embedding',
    '默认向量模型',
    'https://api.openai.com/v1',
    'your-api-key-here',
    'text-embedding-ada-002',
    1536,
    30000,
    true,
    'active',
    CURRENT_TIMESTAMP,
'   CURRENT_TIMESTAMP
) ON CONFLICT DO NOTHING;

-- 插入默认的LLM模型配置
INSERT INTO model_configs (org_id, type, name, endpoint, api_key, model, temperature, max_tokens, timeout, enable_streaming, is_default, status, created_at, updated_at)
VALUES (
    1,
    'llm',
    '默认LLM模型',
    'https://api.openai.com/v1',
    'your-api-key-here',
    'gpt-3.5-turbo',
    0.3,
    1000,
    60000,
    true,
    'active',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT DO NOTHING;

-- 添加注释
COMMENT ON SCRIPT IS '迁移配置文件中的模型配置到数据库';
```

**Step 2: Commit**

```bash
git add src/main/resources/db/migration/V16__migrate_existing_model_config.sql
git commit` -m "feat: add migration script for existing model config"
```

---

## 总结

这个实现计划包含以下主要部分：

1. **后端实现** (Task 1-10):
   - 创建枚举、实体、DTO、Mapper
   - 创建数据库表和迁移脚本
   - 实现Service和Controller
   - 修改KnowledgeBase和Agent实体添加关联字段
   - 实现动态配置的服务类

2. **前端实现** (Task 11-16):
   - 创建API接口和枚举
   - 创建模型配置管理页面
   - 修改知识库和Agent页面添加模型选择器

3. **数据迁移** (Task 17):
   - 创建迁移脚本将现有配置导入数据库

**配置优先级：**
- 知识库/Agent级别配置
- 系统默认配置（isDefault = true）
- 配置文件中的默认值（兜底）
