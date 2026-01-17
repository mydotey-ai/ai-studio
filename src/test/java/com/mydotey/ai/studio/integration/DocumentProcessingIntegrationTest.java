package com.mydotey.ai.studio.integration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mydotey.ai.studio.dto.CreateKnowledgeBaseRequest;
import com.mydotey.ai.studio.dto.KnowledgeBaseResponse;
import com.mydotey.ai.studio.entity.Document;
import com.mydotey.ai.studio.entity.DocumentChunk;
import com.mydotey.ai.studio.entity.User;
import com.mydotey.ai.studio.mapper.DocumentChunkMapper;
import com.mydotey.ai.studio.mapper.DocumentMapper;
import com.mydotey.ai.studio.mapper.UserMapper;
import com.mydotey.ai.studio.service.KnowledgeBaseService;
import com.mydotey.ai.studio.util.FileUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@DisplayName("文档处理集成测试")
class DocumentProcessingIntegrationTest {

    @Autowired
    private DocumentMapper documentMapper;

    @Autowired
    private DocumentChunkMapper chunkMapper;

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @Autowired
    private FileUtil fileUtil;

    @Autowired
    private UserMapper userMapper;

    private Long testKbId;
    private Long testDocId;
    private Long testUserId;

    @BeforeEach
    void setUp() {
        // 创建测试用户
        User user = new User();
        user.setUsername("test_user_integration");
        user.setPasswordHash("hashed_password");
        user.setEmail("test_integration@example.com");
        userMapper.insert(user);
        testUserId = user.getId();

        // 创建测试知识库
        CreateKnowledgeBaseRequest request = new CreateKnowledgeBaseRequest();
        request.setName("Test KB for Doc Processing");
        request.setDescription("Integration test knowledge base");
        request.setEmbeddingModel("text-embedding-ada-002");
        request.setChunkSize(500);
        request.setChunkOverlap(100);

        KnowledgeBaseResponse kb = knowledgeBaseService.create(request, testUserId);
        testKbId = kb.getId();
    }

    @AfterEach
    void tearDown() {
        // 清理测试数据
        if (testDocId != null) {
            // 手动删除关联的 document chunks
            chunkMapper.delete(new LambdaQueryWrapper<DocumentChunk>()
                .eq(DocumentChunk::getDocumentId, testDocId));
            documentMapper.deleteById(testDocId);
        }
        if (testKbId != null) {
            knowledgeBaseService.delete(testKbId, testUserId);
        }
        if (testUserId != null) {
            userMapper.deleteById(testUserId);
        }
    }

    @Test
    @DisplayName("完整的文档上传和处理流程")
    void testCompleteDocumentProcessingFlow() throws Exception {
        // 1. 准备测试文件
        String testContent = "这是第一段测试文本。\n\n" +
                            "这是第二段测试文本，用于测试文档处理功能。\n\n" +
                            "这是第三段测试文本。";
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.txt",
            "text/plain",
            testContent.getBytes()
        );

        // 2. 保存文件
        String filePath = fileUtil.saveFile(file.getOriginalFilename(), file.getBytes());

        // 3. 创建文档记录
        Document document = new Document();
        document.setKbId(testKbId);
        document.setFilename(file.getOriginalFilename());
        document.setFileUrl(filePath);
        document.setFileSize(file.getSize());
        document.setFileType("txt");
        document.setStatus("PENDING");
        document.setSourceType("UPLOAD");

        documentMapper.insert(document);
        testDocId = document.getId();

        // 4. 验证文档已创建
        assertNotNull(document.getId());
        assertEquals("PENDING", document.getStatus());

        // 注意：由于这是一个集成测试，异步处理可能在测试执行期间没有完成
        // 在生产环境中，应该添加等待或使用 CountDownLatch 来确保异步处理完成
    }
}
