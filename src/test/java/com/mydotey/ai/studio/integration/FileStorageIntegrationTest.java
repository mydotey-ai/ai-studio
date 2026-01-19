package com.mydotey.ai.studio.integration;

import com.mydotey.ai.studio.dto.filestorage.FileUploadRequest;
import com.mydotey.ai.studio.dto.filestorage.FileUploadResponse;
import com.mydotey.ai.studio.service.FileStorageManagerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("文件存储系统集成测试")
class FileStorageIntegrationTest {

    @Autowired
    private FileStorageManagerService fileStorageManagerService;

    @Test
    @DisplayName("完整的文件上传和下载流程测试")
    void testCompleteFileUploadAndDownloadFlow() throws Exception {
        // 1. 上传文件
        MockMultipartFile file = new MockMultipartFile(
                "integration-test.txt",
                "integration-test.txt",
                "text/plain",
                "Integration test content".getBytes()
        );

        FileUploadRequest uploadRequest = new FileUploadRequest();
        uploadRequest.setPath("/integration-test");
        uploadRequest.setRelatedEntityType("DOCUMENT");
        uploadRequest.setRelatedEntityId(1L);

        FileUploadResponse uploadResponse = fileStorageManagerService.uploadFile(file, uploadRequest, 1L);

        assertNotNull(uploadResponse);
        assertNotNull(uploadResponse.getId());
        assertEquals("integration-test.txt", uploadResponse.getOriginalFileName());
        assertNotNull(uploadResponse.getFileUrl());

        // 2. 获取文件元数据
        var metadata = fileStorageManagerService.getFileMetadata(uploadResponse.getId());
        assertEquals(uploadResponse.getId(), metadata.getId());

        // 3. 下载文件
        var inputStream = fileStorageManagerService.downloadFile(uploadResponse.getId(), 1L);
        byte[] content = inputStream.readAllBytes();
        assertEquals("Integration test content", new String(content));

        // 4. 获取文件 URL
        String fileUrl = fileStorageManagerService.getFileUrl(uploadResponse.getId(), 1L, 3600);
        assertNotNull(fileUrl);

        // 5. 获取关联文件
        var relatedFiles = fileStorageManagerService.getRelatedFiles("DOCUMENT", 1L);
        assertFalse(relatedFiles.isEmpty());

        // 6. 删除文件
        fileStorageManagerService.deleteFile(uploadResponse.getId(), 1L);

        // 验证文件已被删除（应该抛出异常）
        assertThrows(Exception.class, () -> fileStorageManagerService.getFileMetadata(uploadResponse.getId()));
    }

    @Test
    @DisplayName("应该能够获取用户文件列表")
    void testGetUserFiles() throws Exception {
        MockMultipartFile file1 = new MockMultipartFile(
                "user-file-1.txt",
                "user-file-1.txt",
                "text/plain",
                "User file 1".getBytes()
        );

        MockMultipartFile file2 = new MockMultipartFile(
                "user-file-2.txt",
                "user-file-2.txt",
                "text/plain",
                "User file 2".getBytes()
        );

        FileUploadRequest request = new FileUploadRequest();
        request.setPath("/user-test");

        fileStorageManagerService.uploadFile(file1, request, 1L);
        fileStorageManagerService.uploadFile(file2, request, 1L);

        var userFiles = fileStorageManagerService.getUserFiles(1L);

        assertTrue(userFiles.size() >= 2);
    }

    @Test
    @DisplayName("应该支持不同路径的文件上传")
    void testUploadToDifferentPaths() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "path-test.txt",
                "path-test.txt",
                "text/plain",
                "Path test".getBytes()
        );

        FileUploadRequest request1 = new FileUploadRequest();
        request1.setPath("/documents/2024");

        FileUploadRequest request2 = new FileUploadRequest();
        request2.setPath("/images");

        var response1 = fileStorageManagerService.uploadFile(file, request1, 1L);
        var response2 = fileStorageManagerService.uploadFile(file, request2, 1L);

        assertNotEquals(response1.getId(), response2.getId());
    }
}
