package com.blog.ai.config.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.blog.ai.config.properties.RagProperties;
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
@ConditionalOnProperty(name = "blog.rag.enabled", havingValue = "true")
public class RagElasticsearchConfig {

    @Bean(destroyMethod = "close")
    public RestClient ragRestClient(RagProperties properties) {
        RagProperties.Es es = properties.getEs();
        String raw = es.getHost().replace("http://", "").replace("https://", "");
        String scheme = es.getHost().startsWith("https") ? "https" : "http";
        String[] parts = raw.split(":");
        String hostname = parts[0];
        int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 9200;
        RestClientBuilder builder = RestClient.builder(new HttpHost(hostname, port, scheme));
        if (StringUtils.hasText(es.getUsername())) {
            BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY,
                    new UsernamePasswordCredentials(es.getUsername(), es.getPassword()));
            builder.setHttpClientConfigCallback(httpClientBuilder ->
                    httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));
        }
        return builder.build();
    }

    @Bean
    public ObjectMapper ragObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    @Bean
    public ElasticsearchClient ragElasticsearchClient(RestClient ragRestClient, ObjectMapper ragObjectMapper) {
        ElasticsearchTransport transport = new RestClientTransport(ragRestClient, new JacksonJsonpMapper(ragObjectMapper));
        return new ElasticsearchClient(transport);
    }
}
