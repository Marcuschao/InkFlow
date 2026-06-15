package com.blog.content.config.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.blog.content.config.properties.SearchProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
@ConditionalOnProperty(name = "blog.search.enabled", havingValue = "true")
public class ElasticsearchConfig {

    @Bean(destroyMethod = "close")
    public RestClient elasticsearchRestClient(SearchProperties properties) {
        String raw = properties.getHost().replace("http://", "").replace("https://", "");
        String scheme = properties.getHost().startsWith("https") ? "https" : "http";
        String[] parts = raw.split(":");
        String hostname = parts[0];
        int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 9200;
        RestClientBuilder builder = RestClient.builder(new HttpHost(hostname, port, scheme));
        if (StringUtils.hasText(properties.getUsername())) {
            BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(
                    AuthScope.ANY,
                    new UsernamePasswordCredentials(properties.getUsername(), properties.getPassword()));
            builder.setHttpClientConfigCallback(httpClientBuilder ->
                    httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));
        }
        return builder.build();
    }

    @Bean
    public ObjectMapper elasticsearchObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    @Bean
    public ElasticsearchClient elasticsearchClient(RestClient restClient, ObjectMapper elasticsearchObjectMapper) {
        ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper(elasticsearchObjectMapper));
        return new ElasticsearchClient(transport);
    }
}
