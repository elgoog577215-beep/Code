package com.onlinejudge.classroom.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RecommendationEffectivenessResponse {
    private long recentEventCount;
    private long uniqueRecommendationCount;
    private long exposureCount;
    private long clickCount;
    private long enteredProblemCount;
    private long followupSubmissionCount;
    private long acceptedFollowupCount;
    private long sameFocusIssueCount;
    private long clickedWithoutSubmissionCount;
    private double clickThroughRate;
    private double followupSubmissionRate;
    private double acceptedFollowupRate;
    private double sameFocusIssueRate;
    private String summary;
    private List<SegmentStat> byType;
    private List<SegmentStat> focusTags;

    @Data
    @Builder
    public static class SegmentStat {
        private String key;
        private String label;
        private long exposureCount;
        private long clickCount;
        private long enteredProblemCount;
        private long followupSubmissionCount;
        private long acceptedFollowupCount;
        private long sameFocusIssueCount;
        private double clickThroughRate;
        private double followupSubmissionRate;
        private double acceptedFollowupRate;
        private double sameFocusIssueRate;
    }
}
