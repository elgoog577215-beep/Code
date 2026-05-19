package com.onlinejudge.classroom.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class StudentRecommendationResponse {
    private StudentProfileResponse student;
    private String summary;
    private List<RecommendationItem> recommendations;

    @Data
    @Builder
    public static class RecommendationItem {
        private String recommendationToken;
        private String type;
        private String title;
        private String reason;
        private String actionLabel;
        private Long problemId;
        private String problemTitle;
        private String focusAbility;
        private List<String> focusTags;
        private List<Long> evidenceProblemIds;
        private int priority;
    }
}
