package com.onlinejudge.learning.standardlibrary.dto;

import com.onlinejudge.learning.standardlibrary.domain.AiStandardImprovementPoint;
import com.onlinejudge.learning.standardlibrary.domain.AiStandardMistakePoint;
import com.onlinejudge.learning.standardlibrary.domain.AiStandardSkillUnit;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AiStandardLibraryDiagnosticLayerResponse {
    private AiStandardLibraryNavigationNodeResponse knowledgePoint;
    private List<SkillUnit> skillUnits;
    private List<ImprovementPoint> directImprovementPoints;

    @Data
    @Builder
    public static class SkillUnit {
        private String code;
        private String category;
        private String name;
        private String description;
        private String learningGoal;
        private String primaryKnowledgeNodeCode;
        private List<String> knowledgeNodeCodes;
        private List<MistakePoint> mistakePoints;
        private List<ImprovementPoint> improvementPoints;

        public static SkillUnit from(AiStandardSkillUnit item,
                                     List<MistakePoint> mistakePoints,
                                     List<ImprovementPoint> improvementPoints) {
            return SkillUnit.builder()
                    .code(item.getCode())
                    .category(item.getCategory())
                    .name(item.getName())
                    .description(item.getDescription())
                    .learningGoal(item.getLearningGoal())
                    .primaryKnowledgeNodeCode(item.getPrimaryKnowledgeNodeCode())
                    .knowledgeNodeCodes(lines(item.getKnowledgeNodeCodes()))
                    .mistakePoints(mistakePoints)
                    .improvementPoints(improvementPoints)
                    .build();
        }
    }

    @Data
    @Builder
    public static class MistakePoint {
        private String code;
        private String category;
        private String name;
        private String description;
        private String skillUnitCode;
        private String mistakeType;
        private String misconception;
        private String symptom;
        private String repairStrategy;
        private String severity;
        private String primaryKnowledgeNodeCode;
        private List<String> knowledgeNodeCodes;

        public static MistakePoint from(AiStandardMistakePoint item) {
            return MistakePoint.builder()
                    .code(item.getCode())
                    .category(item.getCategory())
                    .name(item.getName())
                    .description(item.getDescription())
                    .skillUnitCode(item.getSkillUnitCode())
                    .mistakeType(item.getMistakeType())
                    .misconception(item.getMisconception())
                    .symptom(item.getSymptom())
                    .repairStrategy(item.getRepairStrategy())
                    .severity(item.getSeverity())
                    .primaryKnowledgeNodeCode(item.getPrimaryKnowledgeNodeCode())
                    .knowledgeNodeCodes(lines(item.getKnowledgeNodeCodes()))
                    .build();
        }
    }

    @Data
    @Builder
    public static class ImprovementPoint {
        private String code;
        private String category;
        private String name;
        private String description;
        private String skillUnitCode;
        private String primaryKnowledgeNodeCode;
        private List<String> knowledgeNodeCodes;
        private String improvementGoal;
        private String practiceStrategy;
        private String studentBenefit;
        private String teacherExplanation;
        private List<String> relatedMistakeCodes;

        public static ImprovementPoint from(AiStandardImprovementPoint item) {
            return ImprovementPoint.builder()
                    .code(item.getCode())
                    .category(item.getCategory())
                    .name(item.getName())
                    .description(item.getDescription())
                    .skillUnitCode(item.getSkillUnitCode())
                    .primaryKnowledgeNodeCode(item.getPrimaryKnowledgeNodeCode())
                    .knowledgeNodeCodes(lines(item.getKnowledgeNodeCodes()))
                    .improvementGoal(item.getImprovementGoal())
                    .practiceStrategy(item.getPracticeStrategy())
                    .studentBenefit(item.getStudentBenefit())
                    .teacherExplanation(item.getTeacherExplanation())
                    .relatedMistakeCodes(lines(item.getRelatedMistakeCodes()))
                    .build();
        }
    }

    private static List<String> lines(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return value.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toList();
    }
}
