package com.mydotey.ai.studio.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mydotey.ai.studio.entity.RefreshToken;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RefreshTokenMapper extends BaseMapper<RefreshToken> {
    void revokeByUserId(@Param("userId") Long userId);
}
