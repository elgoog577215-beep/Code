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
public class StudentAiFeedbackResponse {
    private Long submissionId;
    private String status;
    private String source;
    private LocalDateTime generatedAt;
    private Long latencyMs;
    private List<FeedbackItem> repairItems;
    private List<FeedbackItem> improvementItems;
    private StudentReport studentReport;
    private String nextQuestion;
    private Safety safety;
    private List<String> evidenceRefs;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeedbackItem {
        private String title;
        private String body;
        private String kind;
        private List<String> knowledgePath;
        private List<EvidenceSnippet> evidenceSnippets;
        private List<String> evidenceRefs;
        private List<String> qualitySignals;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EvidenceSnippet {
        private String evidenceRef;
        private Integer lineNumber;
        private Integer lineEnd;
        private String code;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentReport {
        private String basicLayerText;
        private String improvementLayerText;
        private String nextActionText;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Safety {
        private String answerLeakRisk;
        private List<String> blockedReasons;
    }
}
