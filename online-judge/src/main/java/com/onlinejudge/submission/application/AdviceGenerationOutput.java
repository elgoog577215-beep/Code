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
public class AdviceGenerationOutput {
    private ExecutionGate executionGate;
    private DiagnosisDecision diagnosisDecision;
    private StudentReport studentReport;
    private TeacherTrace teacherTrace;
    private LibraryGrowth libraryGrowth;
    private List<DiagnosisCandidate> diagnosisCandidates;
    private CaseUnderstanding caseUnderstanding;
    private List<BasicLayerAdvice> basicLayerAdvice;
    private List<ImprovementLayerAdvice> improvementLayerAdvice;
    private List<NextStepAdvice> nextStepPlan;
    private String studentSummary;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ExecutionGate {
        private String state;
        private String priority;
        private String reason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DiagnosisDecision {
        private String libraryFit;
        private List<DiagnosisAnchor> anchors;
        private List<OutOfLibraryFinding> outOfLibraryFindings;
        private String uncertainty;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DiagnosisAnchor {
        private String id;
        private String type;
        private String role;
        private Double confidence;
        private List<String> evidenceRefs;
        private String reason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class OutOfLibraryFinding {
        private String name;
        private List<String> suggestedPath;
        private String reason;
        private List<String> evidenceRefs;
        private Double confidence;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DiagnosisCandidate {
        private String name;
        private String layer;
        private String libraryFit;
        private String anchorId;
        private String anchorType;
        private List<String> libraryPath;
        private String role;
        private List<String> evidenceRefs;
        private String reason;
        private Double confidence;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class StudentReport {
        private String hintLevel;
        private String basicLayerText;
        private String improvementLayerText;
        private String nextActionText;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TeacherTrace {
        private String reasoningSummary;
        private String uncertainty;
        private List<String> qualityFlags;
        private List<String> softFixes;
        private List<String> hardFailures;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class LibraryGrowth {
        private List<LibraryGrowthCandidate> candidates;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class LibraryGrowthCandidate {
        private String name;
        private List<String> suggestedPath;
        private Long sourceProblemId;
        private Long sourceSubmissionId;
        private List<String> similarExistingItems;
        private List<String> evidenceRefs;
        private String evidenceStatus;
        private String errorSymptom;
        private String typicalCodePattern;
        private String studentExplanation;
        private String reason;
        private String status;
        private Double confidence;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CaseUnderstanding {
        private String problemGoal;
        private String codeIntent;
        private String behaviorGap;
        private String primaryEvidenceRef;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class BasicLayerAdvice {
        private String mistakePointId;
        private String skillUnitId;
        private String title;
        private String whatHappened;
        private String whyItMatters;
        private String studentAction;
        private String checkQuestion;
        private List<String> evidenceRefs;
        private Double confidence;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ImprovementLayerAdvice {
        private String improvementPointId;
        private String skillUnitId;
        private String title;
        private String currentLimit;
        private String suggestion;
        private String studentBenefit;
        private List<String> evidenceRefs;
        private Double confidence;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class NextStepAdvice {
        private Integer step;
        private String target;
        private String reason;
        private String evidenceRef;
    }
}
