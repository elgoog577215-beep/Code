package com.onlinejudge.submission.application;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ai.embedding")
public class EmbeddingProperties {
    private boolean enabled = true;
    private String baseUrl = "https://api-inference.modelscope.cn/v1";
    private String apiKey = "";
    private String model = "Qwen/Qwen3-Embedding-0.6B";
    private long timeoutSeconds = 20;
}
