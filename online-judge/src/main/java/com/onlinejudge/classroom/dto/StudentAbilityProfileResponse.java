package com.onlinejudge.classroom.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class StudentAbilityProfileResponse {
    private StudentProfileResponse student;
    private List<Long> mergedStudentProfileIds;
    private long totalSubmissions;
    private long problemCount;
    private long assignmentCount;
    private long failedSubmissionCount;
    private String primaryAbilityFocus;
    private String summary;
    private String trendSignal;
    private String recommendationEffectSummary;
    private CoachInteractionSummaryResponse latestCoachInteraction;
    private List<AbilityStat> abilityGaps;
    private List<ProfileStat> knowledgeFocus;
    private List<ProfileStat> commonMistakeFocus;
    private List<ProfileStat> boundaryFocus;

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
    public static class ProfileStat {
        private String label;
        private long count;
        private List<Long> evidenceProblemIds;
    }
}
