package com.blog.content.model.dto.freshness;

import lombok.Data;

@Data
public class FreshnessSummaryDto {
    private long healthyCount;
    private long warnCount;
    private long severeCount;
    private String lastFullScanAt;
}
