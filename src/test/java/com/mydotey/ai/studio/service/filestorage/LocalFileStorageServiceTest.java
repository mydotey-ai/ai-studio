package com.mydotey.ai.studio.service.filestorage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("本地文件存储服务测试")
class LocalFileStorageServiceTest {

    @Autowired
    private LocalFileStorageService fileStorageService;

    @Test
    @DisplayName("应该能够上传文件")
    void testUploadFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "test.txt",
                "test.txt",
                "text/plain",
                "Hello, World!".getBytes()
        );

        FileInfo fileInfo = fileStorageService.uploadFile(file, "/test");

        assertNotNull(fileInfo);
        assertEquals("test.txt", fileInfo.getOriginalFileName());
        assertEquals("text/plain", fileInfo.getContentType());
        assertTrue(fileInfo.getFileSize() > 0);
        assertEquals("LOCAL", fileInfo.getStorageType());
    }

    @Test
    @DisplayName("应该能够下载文件")
    void testDownloadFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "download.txt",
                "download.txt",
                "text/plain",
                "Download test content".getBytes()
        );

        FileInfo uploadedInfo = fileStorageService.uploadFile(file, "/test");

        var inputStream = fileStorageService.downloadFile(uploadedInfo.getFileKey());
        assertNotNull(inputStream);

        byte[] content = inputStream.readAllBytes();
        assertEquals("Download test content", new String(content));
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

        FileInfo uploadedInfo = fileStorageService.uploadFile(file, "/test");

        fileStorageService.deleteFile(uploadedInfo.getFileKey());

        assertFalse(fileStorageService.fileExists(uploadedInfo.getFileKey()));
    }

    @Test
    @DisplayName("应该能够检查文件是否存在")
    void testFileExists() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "exists.txt",
                "exists.txt",
                "text/plain",
                "I exist".getBytes()
        );

        FileInfo uploadedInfo = fileStorageService.uploadFile(file, "/test");

        assertTrue(fileStorageService.fileExists(uploadedInfo.getFileKey()));
        assertFalse(fileStorageService.fileExists("non-existent-file.txt"));
    }
}
