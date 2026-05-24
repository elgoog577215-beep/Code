package com.onlinejudge.submission.application;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StandardLibraryPack {

    public static final String SCHEMA_VERSION = "standard-library-pack-v1";

    private String schemaVersion;
    private String taxonomyVersion;
    private List<TagOption> issueTags;
    private List<TagOption> fineGrainedTags;
    private List<TeachingActionOption> teachingActions;
    private DecisionProtocol decisionProtocol;
    private List<String> safetyRules;
    private List<String> uncertaintyOptions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
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
    public static class DecisionProtocol {
        private List<String> globalRules;
        private List<String> evidencePriorityRules;
        private List<String> tagSelectionRules;
        private List<String> conflictRules;
        private List<String> teachingActionRules;
    }
}
