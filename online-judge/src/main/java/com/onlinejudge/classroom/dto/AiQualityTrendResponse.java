package com.onlinejudge.classroom.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AiQualityTrendResponse {
    private long assignmentCount;
    private long analyzedSubmissionCount;
    private long correctionCount;
    private long evalCandidateCount;
    private long lowConfidenceCount;
    private long highLeakRiskCount;
    private double correctionRate;
    private double lowConfidenceRate;
    private double highLeakRiskRate;
    private String summary;
    private List<AssignmentQualityPoint> assignments;
    private List<TagTrendStat> correctedTags;
    private List<TagTrendStat> evalNeededTags;
    private List<SourceQualitySegment> sourceSegments;

    @Data
    @Builder
    public static class AssignmentQualityPoint {
        private Long assignmentId;
        private String assignmentTitle;
        private long analyzedSubmissionCount;
        private long correctionCount;
        private long evalCandidateCount;
        private long lowConfidenceCount;
        private long highLeakRiskCount;
        private double correctionRate;
        private double lowConfidenceRate;
        private double highLeakRiskRate;
        private String summary;
    }

    @Data
    @Builder
    public static class TagTrendStat {
        private String tag;
        private String label;
        private long count;
        private long evalCandidateCount;
    }

    @Data
    @Builder
    public static class SourceQualitySegment {
        private String sourceType;
        private String versionLabel;
        private long analyzedSubmissionCount;
        private long correctionCount;
        private long lowConfidenceCount;
        private long highLeakRiskCount;
        private double correctionRate;
        private double lowConfidenceRate;
        private double highLeakRiskRate;
    }
}
