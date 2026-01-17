package com.mydotey.ai.studio.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mydotey.ai.studio.config.EmbeddingConfig;
import com.mydotey.ai.studio.service.impl.OpenAIEmbeddingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("向量化服务测试")
@ExtendWith(MockitoExtension.class)
class EmbeddingServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private EmbeddingConfig config;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OpenAIEmbeddingService embeddingService;

    @Test
    @DisplayName("应该返回正确维度的向量")
    void testEmbeddingDimension() throws Exception {
        // Use real ObjectMapper for node creation
        ObjectMapper realMapper = new ObjectMapper();

        // Setup mocks
        when(config.getModel()).thenReturn("text-embedding-ada-002");
        when(config.getApiKey()).thenReturn("test-key");
        when(config.getEndpoint()).thenReturn("https://api.openai.com/v1");

        ResponseEntity<String> response = new ResponseEntity<>(
            "{\"data\":[{\"embedding\":[0.1,0.2,0.3]}]}",
            HttpStatus.OK
        );
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
            .thenReturn(response);

        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // Create mock JsonNode for response using real mapper
        ArrayNode dataArray = realMapper.createArrayNode();
        ObjectNode dataNode = realMapper.createObjectNode();
        ArrayNode embeddingArray = realMapper.createArrayNode();
        embeddingArray.add(0.1);
        embeddingArray.add(0.2);
        embeddingArray.add(0.3);
        dataNode.set("embedding", embeddingArray);
        dataArray.add(dataNode);

        ObjectNode rootNode = realMapper.createObjectNode();
        rootNode.set("data", dataArray);

        when(objectMapper.readTree(anyString())).thenReturn(rootNode);

        // Test
        String text = "测试文本";
        float[] embedding = embeddingService.embed(text);

        // Verify
        assertNotNull(embedding);
        assertEquals(3, embedding.length); // Mock returns 3 dimensions
    }
}
