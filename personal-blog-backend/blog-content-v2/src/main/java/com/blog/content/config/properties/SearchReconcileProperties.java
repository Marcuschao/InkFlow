package com.blog.content.config.properties;

import lombok.Data;

@Data
public class SearchReconcileProperties {
    private String cron = "0 30 4 * * ?";
    private int batchSize = 500;
}
