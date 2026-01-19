package com.mydotey.ai.studio.service;

import com.mydotey.ai.studio.dto.filestorage.FileUploadRequest;
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
@DisplayName("文件存储管理服务测试")
class FileStorageManagerServiceTest {

    @Autowired
    private FileStorageManagerService fileStorageManagerService;

    @Test
    @DisplayName("应该能够上传文件")
    void testUploadFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "test.txt",
                "test.txt",
                "text/plain",
                "Hello, World!".getBytes()
        );

        FileUploadRequest request = new FileUploadRequest();
        request.setPath("/test");

        var response = fileStorageManagerService.uploadFile(file, request, 1L);

        assertNotNull(response);
        assertNotNull(response.getId());
        assertEquals("test.txt", response.getOriginalFileName());
        assertEquals("text/plain", response.getContentType());
    }

    @Test
    @DisplayName("应该能够获取文件元数据")
    void testGetFileMetadata() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "metadata.txt",
                "metadata.txt",
                "text/plain",
                "Metadata test".getBytes()
        );

        FileUploadRequest request = new FileUploadRequest();
        request.setPath("/test");

        var uploaded = fileStorageManagerService.uploadFile(file, request, 1L);
        var metadata = fileStorageManagerService.getFileMetadata(uploaded.getId());

        assertEquals(uploaded.getId(), metadata.getId());
        assertEquals("metadata.txt", metadata.getOriginalFileName());
    }

    @Test
    @DisplayName("应该能够删除文件")
    void testDeleteFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "delete.txt",
                "delete.txt",
                "text/plain",
                "Delete me".getBytes()
        );

        FileUploadRequest request = new FileUploadRequest();
        request.setPath("/test");

        var uploaded = fileStorageManagerService.uploadFile(file, request, 1L);

        assertDoesNotThrow(() -> fileStorageManagerService.deleteFile(uploaded.getId(), 1L));

        // 验证文件已被删除（应该抛出异常）
        assertThrows(Exception.class, () -> fileStorageManagerService.getFileMetadata(uploaded.getId()));
    }
}
