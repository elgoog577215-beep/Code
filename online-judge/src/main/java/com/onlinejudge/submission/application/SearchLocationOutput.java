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
public class SearchLocationOutput {
    private List<SelectedCandidate> basicCandidates;
    private List<SelectedCandidate> improvementCandidates;
    private List<SelectedCandidate> knowledgeAnchors;
    private String uncertainty;
    private Boolean needsMoreEvidence;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SelectedCandidate {
        private String id;
        private String layer;
        private String knowledgeNodeId;
        private String skillUnitId;
        private String mistakePointId;
        private Integer priority;
        private Double confidence;
        private List<String> evidenceRefs;
        private String reason;
    }
}
