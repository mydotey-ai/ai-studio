package com.mydotey.ai.studio.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mydotey.ai.studio.config.LlmConfig;
import com.mydotey.ai.studio.dto.LlmRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

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
            String messages = promptTemplateService.buildMessages(systemPrompt, userQuestion);

            // 构建请求
            LlmRequest request = LlmRequest.builder()
                    .model(config.getModel())
                    .messages(messages)
                    .temperature(temperature != null ? temperature : config.getDefaultTemperature())
                    .maxTokens(maxTokens != null ? maxTokens : config.getDefaultMaxTokens())
                    .stream(true)
                    .build();

            // 构造请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(config.getApiKey());

            // 发送请求
            HttpEntity<String> httpEntity = new HttpEntity<>(
                    objectMapper.writeValueAsString(request),
                    headers
            );

            String url = config.getEndpoint() + "/chat/completions";
            ResponseEntity<String> response = restTemplate.postForEntity(url, httpEntity, String.class);

            // 解析流式响应
            if (response.getStatusCode() == HttpStatus.OK) {
                parseStreamResponse(response.getBody(), streamCallback);
            } else {
                streamCallback.onError(new RuntimeException("Unexpected response status: " + response.getStatusCode()));
            }

        } catch (Exception e) {
            log.error("Failed to stream generate from LLM", e);
            streamCallback.onError(e);
        }
    }

    /**
     * 解析流式响应
     */
    private void parseStreamResponse(String responseBody, StreamCallback streamCallback) {
        try {
            // 解析每一行（SSE 格式：data: {...}）
            String[] lines = responseBody.split("\n");

            for (String line : lines) {
                line = line.trim();

                if (line.startsWith("data: ")) {
                    String data = line.substring(6);

                    // 检查结束标记
                    if ("[DONE]".equals(data)) {
                        streamCallback.onComplete();
                        return;
                    }

                    // 解析 JSON
                    JsonNode json = objectMapper.readTree(data);
                    JsonNode choices = json.get("choices");

                    if (choices != null && choices.size() > 0) {
                        JsonNode delta = choices.get(0).get("delta");
                        if (delta != null && delta.has("content")) {
                            String content = delta.get("content").asText();
                            streamCallback.onContent(content);
                        }
                    }
                }
            }

        } catch (Exception e) {
            log.error("Failed to parse stream response", e);
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
