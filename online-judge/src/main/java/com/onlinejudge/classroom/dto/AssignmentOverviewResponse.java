package com.onlinejudge.classroom.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class AssignmentOverviewResponse {
    private AssignmentResponse assignment;
    private long participantCount;
    private long attemptCount;
    private long passedAttemptCount;
    private long strugglingStudentCount;
    private List<IssueStat> topIssues;
    private List<AbilityStat> classAbilityWeaknesses;
    private List<ClassReviewSuggestion> classReviewSuggestions;
    private List<StudentProgressSummary> students;

    @Data
    @Builder
    public static class IssueStat {
        private String label;
        private long count;
        private String explanation;
        private String abilityPoint;
        private String recommendedHintPolicy;
        private String interventionSuggestion;
    }

    @Data
    @Builder
    public static class StudentProgressSummary {
        private Long studentProfileId;
        private String displayName;
        private String studentNo;
        private long attemptCount;
        private long passedCount;
        private Long latestSubmissionId;
        private String latestVerdict;
        private String latestIssue;
        private String latestIssueTag;
        private String latestFineGrainedIssue;
        private String latestProgressSignal;
        private Double latestConfidence;
        private String latestUncertainty;
        private String latestAnswerLeakRisk;
        private TeacherDiagnosisCorrectionResponse latestCorrection;
        private CoachInteractionSummaryResponse latestCoachInteraction;
        private String primaryAbilityFocus;
        private String crossProblemSummary;
        private List<AbilityStat> abilitySummary;
        private String repeatedIssueTag;
        private String repeatedFineGrainedTag;
        private long repeatedIssueCount;
        private String attentionReason;
        private List<AttentionEvidence> attentionEvidence;
        private boolean needsAttention;
    }

    @Data
    @Builder
    public static class AbilityStat {
        private String abilityPoint;
        private long taskCount;
        private long submissionCount;
        private List<String> evidenceTags;
    }

    @Data
    @Builder
    public static class ClassReviewSuggestion {
        private String suggestionKey;
        private String title;
        private String targetAbility;
        private Long exampleProblemId;
        private String exampleProblemTitle;
        private List<String> evidenceTags;
        private List<Long> evidenceSubmissionIds;
        private String guidingQuestion;
        private String action;
        private String evidenceSummary;
        private ClassReviewFeedbackSummary latestFeedback;
    }

    @Data
    @Builder
    public static class ClassReviewFeedbackSummary {
        private String actionType;
        private String teacherNote;
        private String createdBy;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    public static class AttentionEvidence {
        private Long submissionId;
        private Long problemId;
        private String verdict;
        private LocalDateTime submittedAt;
        private String issueTag;
        private String fineGrainedTag;
        private String abilityPoint;
        private String headline;
        private String reason;
    }
}
