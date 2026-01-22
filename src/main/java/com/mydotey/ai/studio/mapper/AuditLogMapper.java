package com.mydotey.ai.studio.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mydotey.ai.studio.dto.audit.AuditLogQueryRequest;
import com.mydotey.ai.studio.dto.audit.AuditLogResponse;
import com.mydotey.ai.studio.entity.AuditLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLog> {

    IPage<AuditLogResponse> queryAuditLogs(Page<AuditLogResponse> page, @Param("query") AuditLogQueryRequest query);
}
