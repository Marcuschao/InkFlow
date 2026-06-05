package com.blog.personalblogbackend.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "blog.search")
public class SearchProperties {
    private boolean enabled = false;
    private String host = "http://127.0.0.1:7700";
    private String apiKey = "";
    private String index = "articles";
    private String exchange = "blog.search";
    private String queue = "search.sync.queue";
    private String routingKey = "search.sync";
    private SearchOutboxProperties outbox = new SearchOutboxProperties();
    private SearchReconcileProperties reconcile = new SearchReconcileProperties();
}
