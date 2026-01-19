package com.mydotey.ai.studio.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mydotey.ai.studio.service.FileStorageManagerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FileStorageController.class)
@DisplayName("文件存储控制器测试")
class FileStorageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FileStorageManagerService fileStorageManagerService;

    @Test
    @DisplayName("应该能够上传文件")
    void testUploadFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "Hello, World!".getBytes()
        );

        when(fileStorageManagerService.uploadFile(any(), any(), eq(1L)))
                .thenReturn(new com.mydotey.ai.studio.dto.filestorage.FileUploadResponse());

        mockMvc.perform(multipart("/api/files/upload")
                        .file(file)
                        .param("path", "/test")
                        .requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("应该能够获取文件元数据")
    void testGetFileMetadata() throws Exception {
        when(fileStorageManagerService.getFileMetadata(1L))
                .thenReturn(new com.mydotey.ai.studio.dto.filestorage.FileMetadataResponse());

        mockMvc.perform(get("/api/files/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
