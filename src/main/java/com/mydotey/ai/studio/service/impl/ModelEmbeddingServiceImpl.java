package com.mydotey.ai.studio.service.impl;

import com.mydotey.ai.studio.dto.ModelConfigDto;
import com.mydotey.ai.studio.enums.ModelConfigType;
import com.mydotey.ai.studio.service.EmbeddingService;
import com.mydotey.ai.studio.service.ModelConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

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
