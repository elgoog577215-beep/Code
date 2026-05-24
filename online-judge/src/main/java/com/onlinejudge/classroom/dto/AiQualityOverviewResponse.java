package com.onlinejudge.classroom.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AiQualityOverviewResponse {
    private Long assignmentId;
    private long analyzedSubmissionCount;
    private long correctionCount;
    private long evalCandidateCount;
    private long lowConfidenceCount;
    private long highLeakRiskCount;
    private long modelFallbackCount;
    private long modelPartialCount;
    private long modelRuntimeFailureCount;
    private long modelCompletedCount;
    private double correctionRate;
    private double lowConfidenceRate;
    private double highLeakRiskRate;
    private double modelFallbackRate;
    private double modelRuntimeFailureRate;
    private String summary;
    private String qualityRiskSummary;
    private List<TagCorrectionStat> correctedTags;

    @Data
    @Builder
    public static class TagCorrectionStat {
        private String originalTag;
        private String originalLabel;
        private String correctedTag;
        private String correctedLabel;
        private long count;
    }
}
