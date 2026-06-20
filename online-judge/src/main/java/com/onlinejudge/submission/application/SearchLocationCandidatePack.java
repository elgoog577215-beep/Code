package com.onlinejudge.submission.application;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SearchLocationCandidatePack {
    public static final String SCHEMA_VERSION = "search-location-candidate-pack-v1";

    private String schemaVersion;
    private String mode;
    private String embeddingStatus;
    private String fallbackReason;
    private int totalAvailableCount;
    private int candidateCount;
    private List<SearchLocationCandidate> candidates;
}
