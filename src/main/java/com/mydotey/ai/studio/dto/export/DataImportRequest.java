package com.mydotey.ai.studio.dto.export;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 数据导入请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataImportRequest {

    /**
     * 文件存储 ID (从文件上传接口获取)
     */
    @NotNull(message = "文件 ID 不能为空")
    private Long fileId;

    /**
     * 是否覆盖已存在的数据
     */
    @Builder.Default
    private boolean overwrite = false;

    /**
     * 是否只验证不导入
     */
    @Builder.Default
    private boolean validateOnly = false;

    /**
     * 导入策略
     */
    @Builder.Default
    private ImportStrategy strategy = ImportStrategy.SKIP_EXISTING;

    /**
     * 导入策略枚举
     */
    public enum ImportStrategy {
        /**
         * 跳过已存在的数据
         */
        SKIP_EXISTING,

        /**
         * 覆盖已存在的数据
         */
        OVERWRITE,

        /**
         * 重命名冲突的数据
         */
        RENAME_CONFLICT
    }
}
