package com.onlinejudge.submission.application;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveModelEvalReport {

    private String model;
    private String promptVersion;
    private Integer totalCount;
    private Integer completedCount;
    private Integer fallbackCount;
    private Integer timeoutCount;
    private Integer issueTagHitCount;
    private Integer fineTagHitCount;
    private Integer safetyPassedCount;
    private List<Entry> entries;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Entry {
        private String caseId;
        private String model;
        private String promptVersion;
        private String stage;
        private Long latencyMs;
        private String status;
        private Boolean fallbackUsed;
        private Boolean jsonValid;
        private Boolean expectedIssueTagHit;
        private Boolean expectedFineTagHit;
        private Boolean evidenceValid;
        private Boolean safetyPassed;
        private String failureReason;
        private String outputSummary;
    }
}
