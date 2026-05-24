package com.onlinejudge.submission.application;

import com.onlinejudge.submission.dto.SubmissionAnalysisResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiagnosisEvidencePackage {

    public static final String SCHEMA_VERSION = "evidence-v1";

    private String schemaVersion;
    private SubmissionEvidence submission;
    private ProblemEvidence problem;
    private JudgeFacts judgeFacts;
    private HistoryEvidence history;
    private PolicyEvidence policy;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubmissionEvidence {
        private Long id;
        private String language;
        private String verdict;
        private String sourceCode;
        private String sourceCodeWithLineNumbers;
        private Integer sourceCodeLineCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProblemEvidence {
        private Long id;
        private String title;
        private String description;
        private String difficulty;
        private Integer timeLimit;
        private Integer memoryLimit;
        private String aiPromptDirection;
        private List<String> knowledgePoints;
        private List<String> algorithmStrategies;
        private List<String> commonMistakes;
        private List<String> boundaryTypes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JudgeFacts {
        private String compileOutput;
        private String runtimeErrorMessage;
        private Integer passedCount;
        private Integer totalCount;
        private Boolean hiddenFailureObserved;
        private SubmissionAnalysisResponse.FailedCaseSnapshot firstFailedCase;
        private List<CaseSummary> caseResultsSummary;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CaseSummary {
        private Integer testCaseNumber;
        private Boolean passed;
        private Boolean hidden;
        private Double executionTime;
        private Integer memoryUsed;
        private String actualOutputPreview;
        private String expectedOutputPreview;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HistoryEvidence {
        private String previousVerdict;
        private List<String> previousIssueTags;
        private List<String> previousFineGrainedTags;
        private List<String> recentIssueTags;
        private List<String> recentFineGrainedTags;
        private String repeatedIssueTag;
        private String repeatedFineGrainedTag;
        private Long repeatedIssueCount;
        private Long repeatedFineGrainedIssueCount;
        private String transitionSignal;
        private String previousInterventionType;
        private String previousInterventionTask;
        private String previousInterventionCompletionSignal;
        private String previousLearningActionStatus;
        private Double previousLearningActionConfidence;
        private List<String> previousLearningActionEvidenceRefs;
        private String previousLearningActionSummary;
        private String previousLearningActionNextAdjustment;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PolicyEvidence {
        private String hintPolicy;
        private List<String> allowedHintLevels;
    }
}
