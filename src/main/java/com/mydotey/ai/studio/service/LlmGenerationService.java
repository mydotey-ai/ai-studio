package com.mydotey.ai.studio.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mydotey.ai.studio.config.LlmConfig;
import com.mydotey.ai.studio.dto.LlmRequest;
import com.mydotey.ai.studio.dto.LlmResponse;
import com.mydotey.ai.studio.service.PromptTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * LLM 生成服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LlmGenerationService {

    private final RestTemplate restTemplate;
    private final LlmConfig config;
    private final ObjectMapper objectMapper;
    private final PromptTemplateService promptTemplateService;

    /**
     * 生成回答（非流式）
     *
     * @param systemPrompt 系统提示词
     * @param userQuestion 用户问题
     * @param temperature 温度参数
     * @param maxTokens 最大生成长度
     * @return LLM 响应
     */
    public LlmResponse generate(
            String systemPrompt,
            String userQuestion,
            Double temperature,
            Integer maxTokens) {

        try {
            // 构建消息
            String messages = promptTemplateService.buildMessages(systemPrompt, userQuestion);

            // 构建请求
            LlmRequest request = LlmRequest.builder()
                    .model(config.getModel())
                    .messages(messages)
                    .temperature(temperature != null ? temperature : config.getDefaultTemperature())
                    .maxTokens(maxTokens != null ? maxTokens : config.getDefaultMaxTokens())
                    .stream(false)
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

            // 解析响应
            return parseLlmResponse(response.getBody());

        } catch (Exception e) {
            log.error("Failed to generate response from LLM", e);
            throw new RuntimeException("Failed to generate response: " + e.getMessage(), e);
        }
    }

    /**
     * 解析 LLM 响应
     */
    private LlmResponse parseLlmResponse(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);

        JsonNode choices = root.get("choices");
        if (choices == null || choices.isEmpty()) {
            throw new RuntimeException("No choices in LLM response");
        }

        JsonNode choice = choices.get(0);
        JsonNode message = choice.get("message");
        String content = message.get("content").asText();
        String finishReason = choice.get("finish_reason").asText();

        // 解析 token 使用情况
        Integer totalTokens = null;
        Integer promptTokens = null;
        Integer completionTokens = null;

        JsonNode usage = root.get("usage");
        if (usage != null) {
            totalTokens = usage.get("total_tokens").asInt();
            promptTokens = usage.get("prompt_tokens").asInt();
            completionTokens = usage.get("completion_tokens").asInt();
        }

        return LlmResponse.builder()
                .content(content)
                .finishReason(finishReason)
                .promptTokens(promptTokens)
                .completionTokens(completionTokens)
                .totalTokens(totalTokens)
                .build();
    }
}
