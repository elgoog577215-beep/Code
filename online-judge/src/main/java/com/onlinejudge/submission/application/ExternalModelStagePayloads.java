package com.onlinejudge.submission.application;

import com.onlinejudge.submission.dto.SubmissionAnalysisResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public final class ExternalModelStagePayloads {

    private ExternalModelStagePayloads() {
    }

    @Data
    @Builder(toBuilder = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiagnosisJudgeOutput {
        private String primaryIssueTag;
        private String fineGrainedTag;
        private List<String> evidenceRefs;
        private String primaryReasoning;
        private List<EducationIssueNote> secondaryIssues;
        private List<EducationIssueNote> distractorNotes;
        private String teachingPriority;
        private List<SubmissionAnalysisResponse.ImprovementOpportunity> improvementOpportunities;
        private SubmissionAnalysisResponse.NextLearningAction nextLearningAction;
        private Double confidence;
        private String uncertainty;
        private Boolean needsMoreEvidence;
        private String answerLeakRisk;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EducationIssueNote {
        private String title;
        private String message;
        private String issueTag;
        private String fineGrainedTag;
        private List<String> evidenceRefs;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeachingHintOutput {
        private String studentHint;
        private SubmissionAnalysisResponse.StudentHintPlan studentHintPlan;
        private SubmissionAnalysisResponse.LearningInterventionPlan learningInterventionPlan;
        private String teacherNote;
        private String answerLeakRisk;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CombinedOutput {
        private DiagnosisJudgeOutput diagnosisDecision;
        private TeachingHintOutput teachingHint;
        private SubmissionAnalysisResponse.StudentFeedback studentFeedback;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StageValidationResult {
        private boolean valid;
        private String stage;
        private ModelStageFailureReason failureReason;
        private String message;
    }
}
