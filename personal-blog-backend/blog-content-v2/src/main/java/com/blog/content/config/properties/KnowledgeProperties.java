package com.blog.content.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "blog.knowledge")
public class KnowledgeProperties {
    private String cooccurrenceCron = "0 0 2 * * ?";
    private String hotnessCron = "0 0 * * * ?";
    private double decayLambda = 0.01;
    private int maxEdges = 500;
    private int graphCacheTtlHours = 25;
    private int topArticleNodes = 50;
    private int topAuthorNodes = 20;
    private int subgraphMaxNodes = 20;
    private boolean autoTagEnabled = false;
}
