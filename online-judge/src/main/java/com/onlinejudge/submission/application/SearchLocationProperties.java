package com.onlinejudge.submission.application;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ai.search-location")
public class SearchLocationProperties {
    private boolean enabled = false;
    private String mode = "hybrid";
    private int candidateLimit = 120;
    private int selectedLimit = 15;
    private boolean requireVector = false;
    private double textWeight = 0.45;
    private double vectorWeight = 0.35;
}
