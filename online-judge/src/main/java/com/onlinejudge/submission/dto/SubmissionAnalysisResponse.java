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
public class SubmissionAnalysisResponse {
    private String analysisSchemaVersion;
    private String evidenceSchemaVersion;
    private String taxonomyVersion;
    private Long submissionId;
    private String sourceType;
    private String scenario;
    private String headline;
    private String summary;
    private List<String> issueTags;
    private List<String> fineGrainedTags;
    private List<String> abilityPoints;
    private List<String> focusPoints;
    private List<String> fixDirections;
    private List<String> evidenceRefs;
    private String studentHint;
    private String teacherNote;
    private String progressSignal;
    private Double confidence;
    private String uncertainty;
    private String diagnosticTrace;
    private AiInvocation aiInvocation;
    private String answerLeakRisk;
    private String wrongSolution;
    private String correctSolution;
    private List<LineIssue> lineIssues;
    private FailedCaseSnapshot firstFailedCase;
    private String reportMarkdown;
    private LocalDateTime generatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FailedCaseSnapshot {
        private Integer testCaseNumber;
        private boolean hidden;
        private String input;
        private String expectedOutput;
        private String actualOutput;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LineIssue {
        private Integer lineNumber;
        private String error;
        private String suggestion;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AiInvocation {
        private String provider;
        private String model;
        private String modelVersion;
        private String promptVersion;
        private String agentVersion;
        private String analysisSchemaVersion;
        private String evidenceSchemaVersion;
        private String taxonomyVersion;
        private String status;
        private boolean fallbackUsed;
    }
}
