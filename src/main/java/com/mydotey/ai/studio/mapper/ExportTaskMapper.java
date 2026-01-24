package com.mydotey.ai.studio.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mydotey.ai.studio.entity.ExportTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * 导出任务 Mapper
 */
@Mapper
public interface ExportTaskMapper extends BaseMapper<ExportTask> {
}
