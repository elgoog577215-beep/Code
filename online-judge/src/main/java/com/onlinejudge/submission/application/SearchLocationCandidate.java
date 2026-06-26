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
    private String mistakeType;
    private List<String> knowledgeNodeCodes;
    private List<String> applicableLanguages;
    private List<String> recallSources;
    private String parentKnowledgePath;
    private List<String> siblingMistakePointIds;
    private List<String> extensionCandidateIds;
    private Double textScore;
    private Double vectorScore;
    private Double signalScore;
    private Double finalScore;
    private List<String> matchedSignals;
}
