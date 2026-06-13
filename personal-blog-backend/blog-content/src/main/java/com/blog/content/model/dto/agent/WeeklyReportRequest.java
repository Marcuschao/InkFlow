package com.blog.content.model.dto.agent;

import lombok.Data;

@Data
public class WeeklyReportRequest {

    /** 可选：希望周报侧重分析的文案提示 */
    private String focus;
}
