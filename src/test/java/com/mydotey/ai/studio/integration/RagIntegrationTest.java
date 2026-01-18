package com.mydotey.ai.studio.integration;

import com.mydotey.ai.studio.dto.*;
import com.mydotey.ai.studio.entity.Document;
import com.mydotey.ai.studio.mapper.DocumentMapper;
import com.mydotey.ai.studio.mapper.UserMapper;
import com.mydotey.ai.studio.service.KnowledgeBaseService;
import com.mydotey.ai.studio.service.LlmGenerationService;
import com.mydotey.ai.studio.service.RagService;
import com.mydotey.ai.studio.service.VectorSearchService;
import com.mydotey.ai.studio.util.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@Transactional
@Slf4j
@DisplayName("RAG 系统集成测试")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RagIntegrationTest {

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @Autowired
    private DocumentMapper documentMapper;

    @Autowired
    private RagService ragService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private FileUtil fileUtil;

    @MockBean
    private VectorSearchService vectorSearchService;

    @MockBean
    private LlmGenerationService llmGenerationService;

    private Long testKbId;
    private Long testDocId;
    private Long testUserId;
    private String testFilePath;

    @BeforeEach
    void setUp() {
        // 创建测试用户
        com.mydotey.ai.studio.entity.User user = new com.mydotey.ai.studio.entity.User();
        user.setUsername("test_user_rag");
        user.setPasswordHash("hashed_password");
        user.setEmail("test_rag@example.com");
        userMapper.insert(user);
        testUserId = user.getId();

        // 1. 创建测试知识库
        CreateKnowledgeBaseRequest kbRequest = new CreateKnowledgeBaseRequest();
        kbRequest.setName("Test KB for RAG");
        kbRequest.setDescription("Integration test knowledge base for RAG");
        kbRequest.setEmbeddingModel("text-embedding-ada-002");
        kbRequest.setChunkSize(500);
        kbRequest.setChunkOverlap(100);
        kbRequest.setIsPublic(true);

        KnowledgeBaseResponse kb = knowledgeBaseService.create(kbRequest, testUserId);
        testKbId = kb.getId();

        // 2. 创建测试文档
        String testContent = "人工智能（AI）是计算机科学的一个分支，" +
                "旨在创建能够执行通常需要人类智能的任务的系统。" +
                "这些任务包括学习、推理、解决问题、理解语言、感知等。";

        // 保存测试文件（记录路径以便清理）
        try {
            testFilePath = fileUtil.saveFile("AI简介.txt", testContent.getBytes());
        } catch (Exception e) {
            throw new RuntimeException("Failed to save test file", e);
        }

        Document doc = new Document();
        doc.setKbId(testKbId);
        doc.setFilename("AI简介.txt");
        doc.setFileUrl(testFilePath);
        doc.setFileSize((long) testContent.getBytes().length);
        doc.setFileType("txt");
        doc.setStatus("COMPLETED");
        doc.setSourceType("UPLOAD");

        documentMapper.insert(doc);
        testDocId = doc.getId();

        // 3. Mock 向量搜索服务返回预定义的文档块
        // 避免依赖真实的嵌入服务和向量数据库查询
        mockVectorSearchService();

        // 4. Mock LLM 服务返回预定义响应
        mockLlmGenerationService();

        log.info("Test setup complete - KB: {}, Document: {}", testKbId, testDocId);
    }

    @AfterEach
    void tearDown() {
        // 清理测试数据
        if (testKbId != null) {
            try {
                knowledgeBaseService.delete(testKbId, testUserId);
                log.info("Test KB deleted: {}", testKbId);
            } catch (Exception e) {
                log.error("Failed to delete test KB", e);
            }
        }
        if (testUserId != null) {
            try {
                userMapper.deleteById(testUserId);
            } catch (Exception e) {
                log.error("Failed to delete test user", e);
            }
        }

        // 清理测试文件
        if (testFilePath != null) {
            try {
                fileUtil.deleteFile(testFilePath);
                log.info("Test file deleted: {}", testFilePath);
            } catch (Exception e) {
                log.error("Failed to delete test file: {}", testFilePath, e);
            }
        }
    }

    /**
     * Mock 向量搜索服务，返回预定义的文档块
     * 避免依赖真实的嵌入服务和向量数据库查询
     */
    private void mockVectorSearchService() {
        // 准备测试用的源文档
        SourceDocument doc1 = SourceDocument.builder()
                .documentId(testDocId)
                .documentName("AI简介.txt")
                .chunkIndex(0)
                .content("人工智能（AI）是计算机科学的一个分支，" +
                        "旨在创建能够执行通常需要人类智能的任务的系统。")
                .score(0.95)
                .build();

        SourceDocument doc2 = SourceDocument.builder()
                .documentId(testDocId)
                .documentName("AI简介.txt")
                .chunkIndex(1)
                .content("这些任务包括学习、推理、解决问题、理解语言、感知等。")
                .score(0.88)
                .build();

        List<SourceDocument> relevantDocs = List.of(doc1, doc2);

        // Mock vectorSearchService.search() 返回相关文档
        when(vectorSearchService.search(
                anyString(),
                eq(List.of(testKbId)),
                anyInt(),
                anyDouble()
        )).thenReturn(relevantDocs);
    }

    /**
     * Mock LLM 生成服务，返回预定义的响应
     * 避免真实的 API 调用，使测试快速且可靠
     */
    private void mockLlmGenerationService() {
        // 场景1: 有相关信息的查询 - 匹配用户问题"什么是人工智能？"
        LlmResponse responseWithInfo = LlmResponse.builder()
                .content("根据提供的知识库，人工智能（AI）是计算机科学的一个分支，" +
                        "旨在创建能够执行通常需要人类智能的任务的系统。" +
                        "这些任务包括学习、推理、解决问题、理解语言、感知等。")
                .finishReason("stop")
                .promptTokens(100)
                .completionTokens(50)
                .totalTokens(150)
                .build();

        // 场景2: 没有相关信息的查询 - 匹配用户问题"什么是量子计算？"
        LlmResponse responseWithoutInfo = LlmResponse.builder()
                .content("根据提供的知识库，没有找到与'量子计算'相关的信息。")
                .finishReason("stop")
                .promptTokens(100)
                .completionTokens(30)
                .totalTokens(130)
                .build();

        // 场景3: 多轮对话场景 - 匹配用户问题"它有哪些应用？"
        LlmResponse responseMultiTurn = LlmResponse.builder()
                .content("人工智能的应用包括图像识别、自然语言处理、语音识别、推荐系统、" +
                        "自动驾驶、医疗诊断、金融分析等多个领域。")
                .finishReason("stop")
                .promptTokens(150)
                .completionTokens(60)
                .totalTokens(210)
                .build();

        // 根据用户问题返回不同的响应
        when(llmGenerationService.generate(
                anyString(),
                eq("什么是人工智能？"),
                anyDouble(),
                anyInt()
        )).thenReturn(responseWithInfo);

        when(llmGenerationService.generate(
                anyString(),
                eq("什么是量子计算？"),
                anyDouble(),
                anyInt()
        )).thenReturn(responseWithoutInfo);

        when(llmGenerationService.generate(
                anyString(),
                eq("它有哪些应用？"),
                anyDouble(),
                anyInt()
        )).thenReturn(responseMultiTurn);
    }

    @Test
    @DisplayName("完整的 RAG 查询流程 - 应该返回基于知识库的回答")
    void testCompleteRagQuery() {
        RagQueryRequest request = new RagQueryRequest();
        request.setQuestion("什么是人工智能？");
        request.setKnowledgeBaseIds(List.of(testKbId));
        request.setTopK(5);
        request.setScoreThreshold(0.7);
        request.setTemperature(0.3);
        request.setMaxTokens(500);

        RagQueryResponse response = ragService.query(request, testUserId);

        assertNotNull(response);
        assertNotNull(response.getAnswer());
        assertFalse(response.getAnswer().isEmpty());
        assertTrue(response.getIsComplete());

        // 验证回答包含知识库中的关键信息
        String answer = response.getAnswer();
        assertTrue(answer.contains("人工智能") || answer.contains("AI"),
                "回答应该包含人工智能相关的内容");
        assertTrue(answer.contains("计算机科学") || answer.contains("系统"),
                "回答应该包含知识库中的核心概念");
    }

    @Test
    @DisplayName("当知识库中没有相关信息时应该返回明确的回答")
    void testRagQueryWithNoRelevantInfo() {
        RagQueryRequest request = new RagQueryRequest();
        request.setQuestion("什么是量子计算？"); // 测试文档中没有的内容
        request.setKnowledgeBaseIds(List.of(testKbId));
        request.setTopK(5);
        request.setScoreThreshold(0.7);

        RagQueryResponse response = ragService.query(request, testUserId);

        assertNotNull(response);
        assertNotNull(response.getAnswer());
        assertFalse(response.getAnswer().isEmpty());

        // 验证回答明确说明没有找到相关信息
        // 使用更健壮的断言，检查特定的完整短语
        String answer = response.getAnswer();
        assertTrue(
            answer.contains("根据提供的知识库，没有找到") ||
            answer.contains("知识库中没有相关信息") ||
            answer.contains("未找到相关信息") ||
            answer.contains("没有与"),
            "回答应该明确说明没有在知识库中找到相关信息。实际回答: " + answer
        );
    }

    @Test
    @DisplayName("多轮对话 - 应该包含历史上下文")
    void testRagQueryWithConversationHistory() {
        String question1 = "什么是人工智能？";

        // 创建第一轮对话
        RagQueryRequest request1 = new RagQueryRequest();
        request1.setQuestion(question1);
        request1.setKnowledgeBaseIds(List.of(testKbId));

        RagQueryResponse response1 = ragService.query(request1, testUserId);
        String answer1 = response1.getAnswer();

        // 第二轮问题
        String question2 = "它有哪些应用？";
        List<Message> history = List.of(
                Message.builder()
                        .role(MessageRole.USER)
                        .content(question1)
                        .build(),
                Message.builder()
                        .role(MessageRole.ASSISTANT)
                        .content(answer1)
                        .build()
        );

        RagQueryRequest request2 = new RagQueryRequest();
        request2.setQuestion(question2);
        request2.setKnowledgeBaseIds(List.of(testKbId));
        request2.setConversationHistory(history);

        RagQueryResponse response2 = ragService.query(request2, testUserId);

        assertNotNull(response2);
        assertNotNull(response2.getAnswer());
        assertFalse(response2.getAnswer().isEmpty());

        // 第二轮回答应该包含应用相关的信息
        // 验证回答提到了应用场景或具体的应用领域
        String answer2 = response2.getAnswer();
        assertTrue(
            answer2.contains("应用") || answer2.contains("图像识别") ||
            answer2.contains("自然语言处理") || answer2.contains("语音识别"),
            "第二轮回答应该包含人工智能应用相关信息。实际回答: " + answer2
        );
    }
}
