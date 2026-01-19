package com.mydotey.ai.studio.dto.filestorage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for file upload
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadRequest {

    /**
     * Target path for the file (e.g., "documents", "avatars", "exports")
     */
    @NotBlank(message = "Path is required")
    @Size(max = 500, message = "Path must not exceed 500 characters")
    private String path;

    /**
     * Related entity type (e.g., "KNOWLEDGE_BASE", "CONVERSATION", "USER")
     */
    private String relatedEntityType;

    /**
     * Related entity ID
     */
    private Long relatedEntityId;

    /**
     * Storage configuration ID (if null, uses default storage)
     */
    private Long storageConfigId;
}
