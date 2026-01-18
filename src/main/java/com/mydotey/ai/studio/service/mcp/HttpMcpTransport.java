package com.mydotey.ai.studio.service.mcp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

@Slf4j
public class HttpMcpTransport implements McpTransport {

    private final String endpointUrl;
    private final HttpHeaders headers;
    private final RestTemplate restTemplate;

    public HttpMcpTransport(String endpointUrl, String headersJson) {
        this.endpointUrl = endpointUrl;
        this.headers = new HttpHeaders();
        this.headers.setContentType(MediaType.APPLICATION_JSON);

        // Parse headers from JSON format if provided
        if (headersJson != null && !headersJson.isEmpty()) {
            try {
                String[] pairs = headersJson.split(",");
                for (String pair : pairs) {
                    String[] keyValue = pair.split(":", 2);
                    if (keyValue.length == 2) {
                        headers.add(keyValue[0].trim(), keyValue[1].trim());
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to parse headers JSON: {}", headersJson, e);
            }
        }

        this.restTemplate = new RestTemplate();
    }

    @Override
    public String sendRequest(String jsonRequest) throws Exception {
        log.debug("Sending HTTP MCP request to: {}", endpointUrl);

        HttpEntity<String> request = new HttpEntity<>(jsonRequest, headers);
        String response = restTemplate.postForObject(endpointUrl, request, String.class);

        log.debug("Received HTTP MCP response: {}", response);
        return response;
    }

    @Override
    public void close() {
        // HTTP connections are stateless, no cleanup needed
    }
}
