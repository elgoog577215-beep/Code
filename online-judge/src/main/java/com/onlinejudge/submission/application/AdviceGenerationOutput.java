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
