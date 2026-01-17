package com.mydotey.ai.studio.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mydotey.ai.studio.config.EmbeddingConfig;
import com.mydotey.ai.studio.service.EmbeddingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * OpenAI 兼容的向量化服务实现
 */
@Slf4j
@Service
public class OpenAIEmbeddingService implements EmbeddingService {

    private final RestTemplate restTemplate;
    private final EmbeddingConfig config;
    private final ObjectMapper objectMapper;

    public OpenAIEmbeddingService(RestTemplate restTemplate,
                                   EmbeddingConfig config,
                                   ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.config = config;
        this.objectMapper = objectMapper;
    }

    @Override
    public float[] embed(String text) {
        List<float[]> result = embedBatch(List.of(text));
        return result.get(0);
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        try {
            // 构造请求体
            String requestBody = buildRequestBody(texts);

            // 构造请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(config.getApiKey());

            HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

            // 发送请求
            String url = config.getEndpoint() + "/embeddings";
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            // 解析响应
            return parseEmbeddings(response.getBody());

        } catch (Exception e) {
            log.error("Failed to generate embeddings", e);
            throw new RuntimeException("Failed to generate embeddings: " + e.getMessage(), e);
        }
    }

    @Override
    public int getDimension() {
        return config.getDimension();
    }

    /**
     * 构造请求体
     */
    private String buildRequestBody(List<String> texts) throws Exception {
        return objectMapper.writeValueAsString(new EmbeddingRequest(config.getModel(), texts));
    }

    /**
     * 解析向量响应
     */
    private List<float[]> parseEmbeddings(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode dataArray = root.get("data");

        List<float[]> embeddings = new ArrayList<>();
        for (JsonNode dataNode : dataArray) {
            JsonNode embeddingNode = dataNode.get("embedding");
            float[] embedding = new float[embeddingNode.size()];
            for (int i = 0; i < embeddingNode.size(); i++) {
                embedding[i] = (float) embeddingNode.get(i).asDouble();
            }
            embeddings.add(embedding);
        }
        return embeddings;
    }

    /**
     * Embedding 请求体
     */
    private static class EmbeddingRequest {
        private final String model;
        private final List<String> input;

        public EmbeddingRequest(String model, List<String> input) {
            this.model = model;
            this.input = input;
        }

        public String getModel() {
            return model;
        }

        public List<String> getInput() {
            return input;
        }
    }
}
