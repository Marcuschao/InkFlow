package com.blog.ai.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Set;

@Data
@Component
@ConfigurationProperties(prefix = "blog.rag")
public class RagProperties {
    private boolean enabled = false;
    private Es es = new Es();
    private Embedding embedding = new Embedding();
    private Rerank rerank = new Rerank();
    private Chunk chunk = new Chunk();
    private Retrieve retrieve = new Retrieve();
    private Rabbit rabbit = new Rabbit();
    private String knowledgeBucket = "blog-knowledge";
    private int historySummaryThreshold = 6;

    /** 单文件最大字节数，默认 20MB */
    public long getMaxFileSize() {
        return 20L * 1024 * 1024;
    }

    /** 允许上传的扩展名（小写，不含点） */
    public Set<String> getAllowFileType() {
        return Set.of(
                "pdf", "doc", "docx", "txt", "md", "html", "htm", "ppt", "pptx", "xls", "xlsx"
        );
    }

    @Data
    public static class Es {
        private String host = "http://127.0.0.1:9200";
        private String username = "";
        private String password = "";
        private String chunksIndex = "knowledge_chunks";
        private int vectorDims = 1024;
    }

    @Data
    public static class Embedding {
        private String apiKey = "";
        private String baseUrl = "https://api.openai.com";
        private String model = "text-embedding-ada-002";
        private int maxBatchSize = 16;
    }

    @Data
    public static class Rerank {
        private boolean enabled = true;
        private String apiKey = "";
        private String baseUrl = "";
        private String model = "bge-reranker-base";
        private int topN = 5;
    }

    @Data
    public static class Chunk {
        private int size = 512;
        private int overlap = 64;
    }

    @Data
    public static class Retrieve {
        private int keywordTopK = 20;
        private int vectorTopK = 20;
        private int rrfTopK = 10;
        private int rrfK = 60;
        private int finalTopK = 5;
        /** OBSERVE records low evidence without blocking; ENFORCE applies the calibrated threshold. */
        private String evidenceGateMode = "OBSERVE";
        /** Disabled until an evaluation run calibrates a provider/model-specific value. */
        private double calibratedRerankThreshold = -1.0;
    }

    @Data
    public static class Rabbit {
        private String exchange = "blog.rag";
        private String queue = "rag.doc.parse.queue";
        private String routingKey = "rag.doc.parse";
        private boolean deadLetterEnabled = false;
    }
}
