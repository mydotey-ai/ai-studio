package com.mydotey.ai.studio.service.mcp;

public interface McpTransport {
    String sendRequest(String jsonRequest) throws Exception;
    void close();
}
