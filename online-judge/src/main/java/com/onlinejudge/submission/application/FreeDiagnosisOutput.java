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
public class FreeDiagnosisOutput {
    private String problemUnderstanding;
    private String codeIntent;
    private String behaviorGap;
    private List<Hypothesis> hypotheses;
    private NavigationIntent navigationIntent;
    private String uncertainty;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Hypothesis {
        private String name;
        private String reason;
        private List<String> evidenceRefs;
        private Double confidence;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class NavigationIntent {
        private List<String> preferredDirections;
        private String reason;
        private List<String> avoidDirections;
    }
}
