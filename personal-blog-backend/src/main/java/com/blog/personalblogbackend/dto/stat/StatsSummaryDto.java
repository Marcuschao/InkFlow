package com.blog.personalblogbackend.dto.stat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatsSummaryDto {
    private long articleCount;
    private long pvTotal;
    private long uvEstimate;
    private long aiCallTotal;
}
