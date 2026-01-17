package com.mydotey.ai.studio.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * 文件工具类
 * Phase 2 使用本地存储，后续可扩展到对象存储
 */
@Slf4j
@Component
public class FileUtil {

    @Value("${file.upload-dir:${java.io.tmpdir}/uploads}")
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
     */
    public String saveFile(String originalFilename, byte[] content) throws IOException {
        init();

        // 生成唯一文件名
        String filename = System.currentTimeMillis() + "_" + originalFilename;
        Path filePath = Paths.get(uploadDir, filename);
        Files.write(filePath, content);

        log.info("File saved: {}", filePath);
        return filePath.toString();
    }

    /**
     * 读取文件内容（简化版，仅支持文本）
     */
    public String readFileContent(String filePath) throws IOException {
        return Files.readString(Paths.get(filePath));
    }

    /**
     * 删除文件
     */
    public void deleteFile(String filePath) throws IOException {
        Files.deleteIfExists(Paths.get(filePath));
        log.info("File deleted: {}", filePath);
    }
}
