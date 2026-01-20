package com.mydotey.ai.studio.integration;

import com.mydotey.ai.studio.dto.CreateAgentRequest;
import com.mydotey.ai.studio.dto.UpdateAgentRequest;
import com.mydotey.ai.studio.dto.WorkflowType;
import com.mydotey.ai.studio.entity.Agent;
import com.mydotey.ai.studio.entity.KnowledgeBase;
import com.mydotey.ai.studio.entity.McpServer;
import com.mydotey.ai.studio.entity.McpTool;
import com.mydotey.ai.studio.entity.Organization;
import com.mydotey.ai.studio.entity.User;
import com.mydotey.ai.studio.mapper.KnowledgeBaseMapper;
import com.mydotey.ai.studio.mapper.McpServerMapper;
import com.mydotey.ai.studio.mapper.McpToolMapper;
import com.mydotey.ai.studio.mapper.OrganizationMapper;
import com.mydotey.ai.studio.mapper.UserMapper;
import com.mydotey.ai.studio.service.AgentExecutionService;
import com.mydotey.ai.studio.service.AgentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class AgentSystemIntegrationTest {

    @Autowired
    private AgentService agentService;

    @Autowired
    private AgentExecutionService agentExecutionService;

    @Autowired
    private OrganizationMapper organizationMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private KnowledgeBaseMapper knowledgeBaseMapper;

    @Autowired
    private McpServerMapper mcpServerMapper;

    @Autowired
    private McpToolMapper mcpToolMapper;

    private Long orgId;
    private Long userId;
    private List<Long> knowledgeBaseIds;
    private List<Long> toolIds;

    @BeforeEach
    void setUp() {
        // Create test organization
        Organization org = new Organization();
        org.setName("Test Organization");
        org.setDescription("Test organization for integration tests");
        org.setCreatedAt(Instant.now());
        org.setUpdatedAt(Instant.now());
        organizationMapper.insert(org);
        orgId = org.getId();

        // Create test user with unique username
        String uniqueId = java.util.UUID.randomUUID().toString().substring(0, 8);
        User user = new User();
        user.setOrgId(orgId);
        user.setUsername("testuser_" + uniqueId);
        user.setEmail("test_" + uniqueId + "@example.com");
        user.setPasswordHash("test-hash");
        user.setRole("ADMIN");
        user.setStatus("ACTIVE");
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        userMapper.insert(user);
        userId = user.getId();

        // Create test knowledge bases
        knowledgeBaseIds = new java.util.ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            KnowledgeBase kb = new KnowledgeBase();
            kb.setOrgId(orgId);
            kb.setName("Test KB " + i);
            kb.setDescription("Test knowledge base " + i);
            kb.setOwnerId(userId);
            kb.setIsPublic(false);
            kb.setEmbeddingModel("text-embedding-3-small");
            kb.setCreatedAt(Instant.now());
            kb.setUpdatedAt(Instant.now());
            knowledgeBaseMapper.insert(kb);
            knowledgeBaseIds.add(kb.getId());
        }

        // Create test MCP server
        McpServer server = new McpServer();
        server.setName("Test MCP Server");
        server.setDescription("Test MCP server for integration tests");
        server.setConnectionType("STDIO");
        server.setCommand("npx -y @modelcontextprotocol/server-filesystem /tmp");
        server.setStatus("ACTIVE");
        server.setCreatedBy(userId);
        server.setCreatedAt(Instant.now());
        server.setUpdatedAt(Instant.now());
        mcpServerMapper.insert(server);

        // Create test MCP tools
        toolIds = new java.util.ArrayList<>();
        for (int i = 1; i <= 2; i++) {
            McpTool tool = new McpTool();
            tool.setServerId(server.getId());
            tool.setToolName("test_tool_" + i);
            tool.setDescription("Test tool " + i);
            tool.setInputSchema("{}");
            tool.setCreatedAt(Instant.now());
            tool.setUpdatedAt(Instant.now());
            mcpToolMapper.insert(tool);
            toolIds.add(tool.getId());
        }
    }

    @Test
    void testCreateAndExecuteAgent_Success() {
        // 1. 创建 Agent
        CreateAgentRequest agentRequest = new CreateAgentRequest();
        agentRequest.setName("Test Agent");
        agentRequest.setSystemPrompt("You are a helpful assistant");
        agentRequest.setModelConfig("{\"model\":\"gpt-3.5-turbo\"}");
        agentRequest.setWorkflowType(WorkflowType.REACT);
        agentRequest.setMaxIterations(5);
        agentRequest.setKnowledgeBaseIds(List.of(knowledgeBaseIds.get(0)));

        Agent agent = agentService.createAgent(agentRequest, orgId, userId);

        assertNotNull(agent);
        assertNotNull(agent.getId());
        assertEquals("Test Agent", agent.getName());
        assertEquals("REACT", agent.getWorkflowType());
    }

    @Test
    void testUpdateAgent_Success() {
        // 1. 创建 Agent
        CreateAgentRequest createRequest = new CreateAgentRequest();
        createRequest.setName("Original Name");
        createRequest.setSystemPrompt("Original prompt");
        createRequest.setModelConfig("{\"model\":\"gpt-3.5-turbo\"}");
        createRequest.setWorkflowType(WorkflowType.REACT);
        createRequest.setKnowledgeBaseIds(List.of(knowledgeBaseIds.get(0)));

        Agent agent = agentService.createAgent(createRequest, orgId, userId);

        // 2. 更新 Agent
        UpdateAgentRequest updateRequest = new UpdateAgentRequest();
        updateRequest.setName("Updated Name");
        updateRequest.setSystemPrompt("Updated prompt");
        updateRequest.setWorkflowType(WorkflowType.CUSTOM);
        updateRequest.setKnowledgeBaseIds(List.of(knowledgeBaseIds.get(1)));

        agentService.updateAgent(agent.getId(), updateRequest, userId);

        // 3. 验证更新
        Agent updatedAgent = agentService.getAgent(agent.getId());
        assertEquals("Updated Name", updatedAgent.getName());
        assertEquals("Updated prompt", updatedAgent.getSystemPrompt());
        assertEquals("CUSTOM", updatedAgent.getWorkflowType());
    }

    @Test
    void testDeleteAgent_Success() {
        // 1. 创建 Agent
        CreateAgentRequest createRequest = new CreateAgentRequest();
        createRequest.setName("To Delete");
        createRequest.setSystemPrompt("Will be deleted");
        createRequest.setModelConfig("{\"model\":\"gpt-3.5-turbo\"}");
        createRequest.setWorkflowType(WorkflowType.REACT);
        createRequest.setKnowledgeBaseIds(List.of(knowledgeBaseIds.get(0)));

        Agent agent = agentService.createAgent(createRequest, orgId, userId);
        Long agentId = agent.getId();

        // 2. 删除 Agent
        agentService.deleteAgent(agentId, userId);

        // 3. 验证删除
        assertThrows(Exception.class, () -> {
            agentService.getAgent(agentId);
        });
    }

    @Test
    void testGetAgent_Success() {
        // 1. 创建 Agent
        CreateAgentRequest createRequest = new CreateAgentRequest();
        createRequest.setName("Get Test Agent");
        createRequest.setSystemPrompt("This is a test agent");
        createRequest.setModelConfig("{\"model\":\"gpt-3.5-turbo\"}");
        createRequest.setWorkflowType(WorkflowType.REACT);
        createRequest.setKnowledgeBaseIds(List.of(knowledgeBaseIds.get(0)));

        Agent createdAgent = agentService.createAgent(createRequest, orgId, userId);

        // 2. 获取 Agent
        Agent retrievedAgent = agentService.getAgent(createdAgent.getId());

        // 3. 验证
        assertNotNull(retrievedAgent);
        assertEquals(createdAgent.getId(), retrievedAgent.getId());
        assertEquals("Get Test Agent", retrievedAgent.getName());
        assertEquals("This is a test agent", retrievedAgent.getSystemPrompt());
    }

    @Test
    void testGetAgentKnowledgeBaseIds_Success() {
        // 1. 创建 Agent with multiple knowledge bases
        CreateAgentRequest createRequest = new CreateAgentRequest();
        createRequest.setName("KB Test Agent");
        createRequest.setSystemPrompt("Test KB associations");
        createRequest.setModelConfig("{\"model\":\"gpt-3.5-turbo\"}");
        createRequest.setWorkflowType(WorkflowType.REACT);
        createRequest.setKnowledgeBaseIds(knowledgeBaseIds);

        Agent agent = agentService.createAgent(createRequest, orgId, userId);

        // 2. 获取知识库 IDs
        List<Long> kbIds = agentService.getAgentKnowledgeBaseIds(agent.getId());

        // 3. 验证
        assertNotNull(kbIds);
        assertEquals(3, kbIds.size());
        assertTrue(kbIds.contains(knowledgeBaseIds.get(0)));
        assertTrue(kbIds.contains(knowledgeBaseIds.get(1)));
        assertTrue(kbIds.contains(knowledgeBaseIds.get(2)));
    }

    @Test
    void testGetAgentToolIds_Success() {
        // 1. 创建 Agent with tools
        CreateAgentRequest createRequest = new CreateAgentRequest();
        createRequest.setName("Tool Test Agent");
        createRequest.setSystemPrompt("Test tool associations");
        createRequest.setModelConfig("{\"model\":\"gpt-3.5-turbo\"}");
        createRequest.setWorkflowType(WorkflowType.REACT);
        createRequest.setKnowledgeBaseIds(List.of(knowledgeBaseIds.get(0)));
        createRequest.setToolIds(toolIds);

        Agent agent = agentService.createAgent(createRequest, orgId, userId);

        // 2. 获取工具 IDs
        List<Long> toolIds = agentService.getAgentToolIds(agent.getId());

        // 3. 验证
        assertNotNull(toolIds);
        assertEquals(2, toolIds.size());
        assertTrue(toolIds.contains(this.toolIds.get(0)));
        assertTrue(toolIds.contains(this.toolIds.get(1)));
    }
}
