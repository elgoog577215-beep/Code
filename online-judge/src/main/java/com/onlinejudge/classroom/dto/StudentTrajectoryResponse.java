package com.onlinejudge.classroom.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class StudentTrajectoryResponse {
    private AssignmentResponse assignment;
    private StudentProfileResponse student;
    private int totalTasks;
    private int completedTasks;
    private int totalAttempts;
    private String stageTransition;
    private String repeatedIssueTag;
    private String repeatedFineGrainedTag;
    private long repeatedIssueCount;
    private String nextStep;
    private String attentionReason;
    private String improvementSignal;
    private String primaryAbilityFocus;
    private String crossProblemSummary;
    private CoachInteractionSummaryResponse latestCoachInteraction;
    private List<IssueStat> recentIssueDistribution;
    private List<IssueStat> recentFineGrainedIssueDistribution;
    private List<AbilityStat> abilitySummary;
    private List<TaskTrajectory> tasks;

    @Data
    @Builder
    public static class IssueStat {
        private String label;
        private long count;
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
    public static class TaskTrajectory {
        private Long problemId;
        private String title;
        private String difficulty;
        private int attemptCount;
        private boolean passed;
        private String latestVerdict;
        private String latestProgressSignal;
        private String latestHint;
        private String latestImprovementSignal;
        private CoachInteractionSummaryResponse latestCoachInteraction;
        private List<SubmissionPoint> submissions;
    }

    @Data
    @Builder
    public static class SubmissionPoint {
        private Long submissionId;
        private String verdict;
        private LocalDateTime submittedAt;
        private List<String> issueTags;
        private List<String> fineGrainedTags;
        private String progressSignal;
        private String improvementSignal;
        private CoachInteractionSummaryResponse coachInteraction;
    }
}
