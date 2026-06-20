package com.onlinejudge.submission.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class EmbeddingClient {

    private final ObjectMapper objectMapper;
    private final EmbeddingProperties properties;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public EmbeddingResponse embed(String text) {
        if (!properties.isEnabled()) {
            return EmbeddingResponse.disabled();
        }
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            return EmbeddingResponse.failed("EMBEDDING_API_KEY_MISSING", List.of());
        }
        try {
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", properties.getModel());
            requestBody.put("input", text == null ? "" : text);
            String endpoint = properties.getBaseUrl().endsWith("/")
                    ? properties.getBaseUrl() + "embeddings"
                    : properties.getBaseUrl() + "/embeddings";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(Math.max(5, properties.getTimeoutSeconds())))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return EmbeddingResponse.failed("EMBEDDING_HTTP_" + response.statusCode(), List.of());
            }
            JsonNode embedding = objectMapper.readTree(response.body()).path("data").path(0).path("embedding");
            if (!embedding.isArray() || embedding.isEmpty()) {
                return EmbeddingResponse.failed("EMBEDDING_EMPTY", List.of());
            }
            List<Double> vector = new java.util.ArrayList<>();
            embedding.forEach(node -> vector.add(node.asDouble()));
            return EmbeddingResponse.ready(vector);
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return EmbeddingResponse.failed("EMBEDDING_ERROR:" + exception.getClass().getSimpleName(), List.of());
        }
    }

    public record EmbeddingResponse(String status, String failureReason, List<Double> vector) {
        static EmbeddingResponse ready(List<Double> vector) {
            return new EmbeddingResponse("READY", "", vector == null ? List.of() : vector);
        }

        static EmbeddingResponse failed(String reason, List<Double> vector) {
            return new EmbeddingResponse("DEGRADED", reason == null ? "" : reason, vector == null ? List.of() : vector);
        }

        static EmbeddingResponse disabled() {
            return new EmbeddingResponse("DISABLED", "EMBEDDING_DISABLED", List.of());
        }
    }
}
