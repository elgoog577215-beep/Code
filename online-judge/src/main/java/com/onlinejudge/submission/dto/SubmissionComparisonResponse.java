package com.onlinejudge.submission.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionComparisonResponse {
    private Long problemId;
    private String problemTitle;
    private SubmissionSnapshot baseline;
    private SubmissionSnapshot target;
    private String progressSummary;
    private List<String> causeChanges;
    private String comparisonMode;
    private Comparability comparability;
    private JudgeDelta judgeDelta;
    private SubmissionGrowthSummaryResponse issueDelta;
    private DiffStats diffStats;
    private List<DiffLine> diffLines;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubmissionSnapshot {
        private Long submissionId;
        private String languageName;
        private String verdict;
        private LocalDateTime submittedAt;
        private String analysisSummary;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiffStats {
        private Integer addedLines;
        private Integer removedLines;
        private Integer unchangedLines;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiffLine {
        private String type;
        private Integer leftLineNumber;
        private Integer rightLineNumber;
        private String content;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Comparability {
        private boolean comparable;
        private String dataCompletenessStatus;
        private String reason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JudgeDelta {
        private String baselineVerdict;
        private String targetVerdict;
        private Integer baselinePassedTestCases;
        private Integer targetPassedTestCases;
        private Integer totalTestCases;
        private Integer passedTestCaseDelta;
    }
}

