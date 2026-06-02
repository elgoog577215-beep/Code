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
public class StandardLibraryPack {

    public static final String SCHEMA_VERSION = "standard-library-pack-v1";

    private String schemaVersion;
    private String taxonomyVersion;
    private List<TagOption> issueTags;
    private List<TagOption> fineGrainedTags;
    private List<ImprovementTagOption> improvementTags;
    private List<TeachingActionOption> teachingActions;
    private DecisionProtocol decisionProtocol;
    private StudentFeedbackRules studentFeedbackRules;
    private List<String> safetyRules;
    private List<String> uncertaintyOptions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TagOption {
        private String id;
        private String label;
        private String studentExplanation;
        private String teacherExplanation;
        private String abilityPoint;
        private String parentTag;
        private String teachingAction;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TeachingActionOption {
        private String id;
        private String label;
        private String whenToUse;
        private String studentTaskTemplate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ImprovementTagOption {
        private String id;
        private String label;
        private String whenToUse;
        private String studentBenefit;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DecisionProtocol {
        private List<String> globalRules;
        private List<String> evidencePriorityRules;
        private List<String> tagSelectionRules;
        private List<String> conflictRules;
        private List<String> teachingActionRules;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class StudentFeedbackRules {
        private List<String> blockingIssueRules;
        private List<String> secondaryIssueRules;
        private List<String> improvementRules;
        private List<String> nextActionRules;
    }
}
