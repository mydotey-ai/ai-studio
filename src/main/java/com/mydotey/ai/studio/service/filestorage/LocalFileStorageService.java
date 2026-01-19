package com.mydotey.ai.studio.service.filestorage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Service
public class LocalFileStorageService implements FileStorageService {

    @Value("${file.storage.local.upload-dir:${java.io.tmpdir}/ai-studio-uploads}")
    private String uploadDir;

    private Path basePath;

    @PostConstruct
    public void init() throws IOException {
        basePath = Paths.get(uploadDir);
        if (!Files.exists(basePath)) {
            Files.createDirectories(basePath);
            log.info("Created base upload directory: {}", uploadDir);
        }
    }

    @Override
    public FileInfo uploadFile(MultipartFile file, String path) {
        String originalFileName = file.getOriginalFilename();
        String contentType = file.getContentType();
        String extension = getFileExtension(originalFileName);
        String uniqueFileName = UUID.randomUUID() + (extension.isEmpty() ? "" : "." + extension);

        // 构建完整路径
        Path fullPath = basePath.resolve(path.replaceFirst("^/", ""));
        Path filePath = fullPath.resolve(uniqueFileName);

        try {
            // 创建目录
            Files.createDirectories(fullPath);

            // 保存文件
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            log.info("File uploaded: {}", filePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file", e);
        }

        return FileInfo.builder()
                .fileName(uniqueFileName)
                .originalFileName(originalFileName)
                .fileKey(filePath.toString())
                .filePath(filePath.toString())
                .fileSize(file.getSize())
                .contentType(contentType)
                .storageType(getStorageType())
                .build();
    }

    @Override
    public FileInfo uploadFile(InputStream inputStream, String fileName, String contentType, String path) {
        String extension = getFileExtension(fileName);
        String uniqueFileName = UUID.randomUUID() + (extension.isEmpty() ? "" : "." + extension);

        Path fullPath = basePath.resolve(path.replaceFirst("^/", ""));
        Path filePath = fullPath.resolve(uniqueFileName);

        try {
            Files.createDirectories(fullPath);
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file", e);
        }

        File file = filePath.toFile();

        log.info("File uploaded: {}", filePath);

        return FileInfo.builder()
                .fileName(uniqueFileName)
                .originalFileName(fileName)
                .fileKey(filePath.toString())
                .filePath(filePath.toString())
                .fileSize(file.length())
                .contentType(contentType)
                .storageType(getStorageType())
                .build();
    }

    @Override
    public InputStream downloadFile(String fileKey) {
        try {
            Path filePath = Paths.get(fileKey);
            if (!Files.exists(filePath)) {
                throw new RuntimeException("File not found: " + fileKey);
            }
            return new FileInputStream(filePath.toFile());
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Failed to download file", e);
        }
    }

    @Override
    public void deleteFile(String fileKey) {
        try {
            Path filePath = Paths.get(fileKey);
            Files.deleteIfExists(filePath);
            log.info("File deleted: {}", fileKey);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete file", e);
        }
    }

    @Override
    public String getPresignedUrl(String fileKey, long expirationInSeconds) {
        // 本地存储不支持签名 URL，返回文件路径
        // 在实际应用中，应该通过一个下载端点来提供文件访问
        return "/api/files/download?key=" + fileKey;
    }

    @Override
    public boolean fileExists(String fileKey) {
        return Files.exists(Paths.get(fileKey));
    }

    @Override
    public String getStorageType() {
        return "LOCAL";
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        int lastDotIndex = fileName.lastIndexOf('.');
        return lastDotIndex > 0 ? fileName.substring(lastDotIndex + 1) : "";
    }
}
