package com.onlinejudge.submission.application;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SearchLocationCandidate {
    private Long itemId;
    private String id;
    private String layer;
    private String category;
    private String name;
    private String description;
    private String skillUnitCode;
    private String parentSkillUnitId;
    private String mistakeType;
    private String primaryKnowledgeNodeCode;
    private List<String> knowledgeNodeCodes;
    private List<String> structurePath;
    private List<String> applicableLanguages;
    private List<String> recallSources;
    private String parentKnowledgePath;
    private List<String> childMistakePointIds;
    private List<String> siblingMistakePointIds;
    private List<String> relatedImprovementPointIds;
    private List<String> extensionCandidateIds;
    private Double textScore;
    private Double vectorScore;
    private Double finalScore;
}
