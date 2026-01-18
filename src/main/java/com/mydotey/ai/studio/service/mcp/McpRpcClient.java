package com.mydotey.ai.studio.service.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mydotey.ai.studio.entity.McpServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class McpRpcClient {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<ToolDefinition> listTools(McpServer server) throws Exception {
        log.info("Listing tools for MCP server: {}", server.getName());

        McpTransport transport = createTransport(server);
        try {
            String request = buildJsonRpcRequest("tools/list", null);
            String response = transport.sendRequest(request);

            return parseToolsResponse(response);
        } finally {
            transport.close();
        }
    }

    public JsonNode callTool(McpServer server, String toolName, JsonNode arguments) throws Exception {
        log.info("Calling tool: {} on server: {}", toolName, server.getName());

        McpTransport transport = createTransport(server);
        try {
            String request = buildJsonRpcRequest("tools/call",
                objectMapper.createObjectNode()
                    .put("name", toolName)
                    .set("arguments", arguments));

            String response = transport.sendRequest(request);
            return parseToolCallResponse(response);
        } finally {
            transport.close();
        }
    }

    private McpTransport createTransport(McpServer server) throws Exception {
        if ("STDIO".equals(server.getConnectionType())) {
            return new StdioMcpTransport(server.getCommand(), server.getWorkingDir());
        } else if ("HTTP".equals(server.getConnectionType())) {
            return new HttpMcpTransport(server.getEndpointUrl(), server.getHeaders());
        } else {
            throw new Exception("Unsupported connection type: " + server.getConnectionType());
        }
    }

    private String buildJsonRpcRequest(String method, Object params) throws Exception {
        var request = objectMapper.createObjectNode();
        request.put("jsonrpc", "2.0");
        request.put("id", System.currentTimeMillis());
        request.put("method", method);
        if (params != null) {
            request.set("params", objectMapper.valueToTree(params));
        }
        return objectMapper.writeValueAsString(request);
    }

    private List<ToolDefinition> parseToolsResponse(String response) throws Exception {
        JsonNode root = objectMapper.readTree(response);
        if (root == null) {
            throw new Exception("Invalid JSON-RPC response: root is null");
        }

        // Check for error response
        if (root.has("error")) {
            JsonNode error = root.get("error");
            String message = error.has("message") ? error.get("message").asText() : "Unknown error";
            int code = error.has("code") ? error.get("code").asInt() : -1;
            throw new Exception("JSON-RPC error: " + code + " - " + message);
        }

        // Check for result
        if (!root.has("result")) {
            throw new Exception("Invalid JSON-RPC response: missing 'result' field");
        }
        JsonNode result = root.get("result");

        // Check for tools array
        if (!result.has("tools")) {
            throw new Exception("Invalid JSON-RPC response: missing 'tools' field in result");
        }
        JsonNode tools = result.get("tools");

        List<ToolDefinition> toolDefinitions = new ArrayList<>();
        for (JsonNode tool : tools) {
            ToolDefinition def = new ToolDefinition();
            if (!tool.has("name")) {
                throw new Exception("Invalid tool definition: missing 'name' field");
            }
            def.name = tool.get("name").asText();
            def.description = tool.has("description") ? tool.get("description").asText() : "";

            if (!tool.has("inputSchema")) {
                throw new Exception("Invalid tool definition: missing 'inputSchema' field for tool: " + def.name);
            }
            def.inputSchema = tool.get("inputSchema").toString();
            toolDefinitions.add(def);
        }

        return toolDefinitions;
    }

    private JsonNode parseToolCallResponse(String response) throws Exception {
        JsonNode root = objectMapper.readTree(response);
        if (root == null) {
            throw new Exception("Invalid JSON-RPC response: root is null");
        }

        // Check for error response
        if (root.has("error")) {
            JsonNode error = root.get("error");
            String message = error.has("message") ? error.get("message").asText() : "Unknown error";
            int code = error.has("code") ? error.get("code").asInt() : -1;
            throw new Exception("JSON-RPC error: " + code + " - " + message);
        }

        // Check for result
        if (!root.has("result")) {
            throw new Exception("Invalid JSON-RPC response: missing 'result' field");
        }
        JsonNode result = root.get("result");

        // Check for content
        if (!result.has("content")) {
            throw new Exception("Invalid JSON-RPC response: missing 'content' field in result");
        }
        JsonNode content = result.get("content");
        return content;
    }

    public static class ToolDefinition {
        public String name;
        public String description;
        public String inputSchema;
    }
}
