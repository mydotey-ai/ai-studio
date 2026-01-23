package com.mydotey.ai.studio.dto.dashboard;

import lombok.Data;

@Data
public class UserStats {
    private Long totalCount;
    private Long activeCount; // 7天内有登录
    private Long adminCount;
    private Long regularCount;
    private Long weeklyNewCount;
}
