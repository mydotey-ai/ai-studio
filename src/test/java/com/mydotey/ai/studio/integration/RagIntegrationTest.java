package com.mydotey.ai.studio.integration;

import com.mydotey.ai.studio.dto.*;
import com.mydotey.ai.studio.entity.Document;
import com.mydotey.ai.studio.mapper.DocumentMapper;
import com.mydotey.ai.studio.mapper.UserMapper;
import com.mydotey.ai.studio.service.KnowledgeBaseService;
import com.mydotey.ai.studio.service.RagService;
import com.mydotey.ai.studio.service.TextChunkingService;
import com.mydotey.ai.studio.util.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

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
    private TextChunkingService textChunkingService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private FileUtil fileUtil;

    private Long testKbId;
    private Long testDocId;
    private Long testUserId;

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

        // 2. 上传测试文档并处理
        String testContent = "人工智能（AI）是计算机科学的一个分支，" +
                "旨在创建能够执行通常需要人类智能的任务的系统。" +
                "这些任务包括学习、推理、解决问题、理解语言、感知等。";

        // 这里简化处理 - 直接创建文档记录并模拟分块
        String filePath = fileUtil.saveFile("AI简介.txt", testContent.getBytes());

        Document doc = new Document();
        doc.setKbId(testKbId);
        doc.setFilename("AI简介.txt");
        doc.setFileUrl(filePath);
        doc.setFileSize(testContent.getBytes().length);
        doc.setFileType("txt");
        doc.setStatus("COMPLETED");
        doc.setSourceType("UPLOAD");

        documentMapper.insert(doc);
        testDocId = doc.getId();

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
    }

    @Test
    @DisplayName("完整的 RAG 查询流程")
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
        assertTrue(response.getComplete());
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
        // 应该明确说明没有找到相关信息
        assertTrue(response.getAnswer().contains("没有") ||
                    response.getAnswer().contains("未找到") ||
                    response.getAnswer().contains("没有相关信息"));
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
        // 第二轮回答应该参考第一轮的答案
        assertTrue(response2.getAnswer().contains("应用") ||
                    response2.getAnswer().contains("应用场景"));
    }
}
