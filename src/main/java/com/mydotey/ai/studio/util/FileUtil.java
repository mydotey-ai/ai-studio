package com.mydotey.ai.studio.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 文件工具类（兼容 Phase 2 文档处理）
 *
 * @deprecated 使用 {@link com.mydotey.ai.studio.service.FileStorageManagerService} 代替
 */
@Slf4j
@Component
@Deprecated
public class FileUtil {

    @Value("${file.storage.local.upload-dir:${java.io.tmpdir}/ai-studio-uploads}")
    private String uploadDir;

    /**
     * 初始化上传目录
     */
    public void init() {
        try {
            Path path = Paths.get(uploadDir);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                log.info("Created upload directory: {}", uploadDir);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to create upload directory", e);
        }
    }

    /**
     * 保存上传的文件
     *
     * @deprecated 使用 FileStorageManagerService.uploadFile 代替
     */
    public String saveFile(String originalFilename, byte[] content) throws IOException {
        init();

        String filename = System.currentTimeMillis() + "_" + originalFilename;
        Path filePath = Paths.get(uploadDir, filename);
        Files.write(filePath, content);

        log.info("File saved: {}", filePath);
        return filePath.toString();
    }

    /**
     * 读取文件内容
     *
     * @deprecated 使用 FileStorageManagerService.downloadFile 代替
     */
    public String readFileContent(String filePath) throws IOException {
        return Files.readString(Paths.get(filePath));
    }

    /**
     * 删除文件
     *
     * @deprecated 使用 FileStorageManagerService.deleteFile 代替
     */
    public void deleteFile(String filePath) throws IOException {
        Files.deleteIfExists(Paths.get(filePath));
        log.info("File deleted: {}", filePath);
    }
}
