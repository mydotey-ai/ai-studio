package com.mydotey.ai.studio.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mydotey.ai.studio.config.LlmConfig;
import com.mydotey.ai.studio.dto.LlmRequest;
import com.mydotey.ai.studio.dto.ModelConfigDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * 流式 LLM 服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StreamingLlmService {

    private final RestTemplate restTemplate;
    private final LlmConfig config;
    private final ObjectMapper objectMapper;
    private final PromptTemplateService promptTemplateService;

    /**
     * 流式生成回答
     *
     * @param systemPrompt 系统提示词
     * @param userQuestion 用户问题
     * @param temperature 温度参数
     * @param maxTokens 最大生成长度
     * @param streamCallback 流式响应回调函数
     */
    public void streamGenerate(
            String systemPrompt,
            String userQuestion,
            Double temperature,
            Integer maxTokens,
            StreamCallback streamCallback) {

        try {
            // 构建消息
            var messages = promptTemplateService.buildMessageList(systemPrompt, userQuestion);

            // 构建请求
            LlmRequest request = LlmRequest.builder()
                    .model(config.getModel())
                    .messages(messages)
                    .temperature(temperature != null ? temperature : config.getDefaultTemperature())
                    .maxTokens(maxTokens != null ? maxTokens : config.getDefaultMaxTokens())
                    .stream(true)
                    .build();

            // 构造请求体
            String requestBody = objectMapper.writeValueAsString(request);

            // 构造请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            headers.setBearerAuth(config.getApiKey());

            String url = config.getEndpoint() + "/chat/completions";

            // 使用 execute 方法获取流式响应
            restTemplate.execute(
                    url,
                    HttpMethod.POST,
                    httpRequest -> {
                        httpRequest.getHeaders().putAll(headers);
                        httpRequest.getBody().write(requestBody.getBytes());
                    },
                    response -> {
                        parseStreamResponse(response, streamCallback);
                        return null;
                    }
            );

        } catch (Exception e) {
            log.error("Failed to stream generate from LLM", e);
            streamCallback.onError(e);
        }
    }

    /**
     * 解析流式响应
     */
    private void parseStreamResponse(ClientHttpResponse response, StreamCallback streamCallback) throws IOException {
        InputStream inputStream = response.getBody();
        if (inputStream == null) {
            streamCallback.onError(new RuntimeException("Response body is null"));
            return;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.startsWith("data: ")) {
                    String data = line.substring(6);

                    // 检查结束标记
                    if ("[DONE]".equals(data)) {
                        streamCallback.onComplete();
                        return;
                    }

                    // 解析 JSON
                    try {
                        JsonNode json = objectMapper.readTree(data);
                        JsonNode choices = json.get("choices");

                        if (choices != null && choices.size() > 0) {
                            JsonNode delta = choices.get(0).get("delta");
                            if (delta != null && delta.has("content")) {
                                String content = delta.get("content").asText();
                                streamCallback.onContent(content);
                            }
                        }
                    } catch (Exception e) {
                        log.warn("Failed to parse stream data: {}, error: {}", data, e.getMessage());
                        // 继续处理下一行
                    }
                }
            }
            // 正常结束流
            streamCallback.onComplete();
        } catch (Exception e) {
            log.error("Error reading stream response", e);
            streamCallback.onError(e);
        }
    }

    /**
     * 流式生成回答（使用自定义模型配置）
     *
     * @param systemPrompt 系统提示词
     * @param userQuestion 用户问题
     * @param modelConfig 模型配置
     * @param streamCallback 流式响应回调函数
     */
    public void streamGenerateWithConfig(
            String systemPrompt,
            String userQuestion,
            ModelConfigDto modelConfig,
            StreamCallback streamCallback) {

        try {
            // 构建消息
            var messages = promptTemplateService.buildMessageList(systemPrompt, userQuestion);

            // 使用自定义配置或回退到全局配置
            String model = modelConfig != null && modelConfig.getModel() != null
                    ? modelConfig.getModel()
                    : config.getModel();
            String apiKey = modelConfig != null && modelConfig.getApiKey() != null
                    ? modelConfig.getApiKey()
                    : config.getApiKey();
            String endpoint = modelConfig != null && modelConfig.getEndpoint() != null
                    ? modelConfig.getEndpoint()
                    : config.getEndpoint();
            Double temperature = modelConfig != null && modelConfig.getTemperature() != null
                    ? modelConfig.getTemperature()
                    : config.getDefaultTemperature();
            Integer maxTokens = modelConfig != null && modelConfig.getMaxTokens() != null
                    ? modelConfig.getMaxTokens()
                    : config.getDefaultMaxTokens();

            log.info("LLM stream request - model: {}, endpoint: {}, using custom config: {}",
                    model, endpoint, modelConfig != null);

            // 构建请求
            LlmRequest request = LlmRequest.builder()
                    .model(model)
                    .messages(messages)
                    .temperature(temperature)
                    .maxTokens(maxTokens)
                    .stream(true)
                    .build();

            // 构造请求体
            String requestBody = objectMapper.writeValueAsString(request);

            // 构造请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            String url = endpoint + "/chat/completions";

            // 使用 execute 方法获取流式响应
            restTemplate.execute(
                    url,
                    HttpMethod.POST,
                    httpRequest -> {
                        httpRequest.getHeaders().putAll(headers);
                        httpRequest.getBody().write(requestBody.getBytes());
                    },
                    response -> {
                        parseStreamResponse(response, streamCallback);
                        return null;
                    }
            );

        } catch (Exception e) {
            log.error("Failed to stream generate from LLM with custom config", e);
            streamCallback.onError(e);
        }
    }

    /**
     * 流式响应回调接口
     */
    public interface StreamCallback {
        /**
         * 接收到内容
         */
        void onContent(String content);

        /**
         * 流式传输完成
         */
        void onComplete();

        /**
         * 发生错误
         */
        void onError(Exception e);
    }
}
