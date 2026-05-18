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
    private double correctionRate;
    private double lowConfidenceRate;
    private double highLeakRiskRate;
    private String summary;
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
