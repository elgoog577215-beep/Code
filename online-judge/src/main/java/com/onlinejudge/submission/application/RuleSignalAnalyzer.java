package com.onlinejudge.submission.application;

import com.onlinejudge.submission.domain.Submission;
import com.onlinejudge.submission.domain.SubmissionCaseResult;
import lombok.Builder;
import lombok.Data;

import java.util.List;

public class RuleSignalAnalyzer {

    public RuleSignalResult analyze(DiagnosisEvidencePackage evidencePackage) {
        return empty();
    }

    public RuleSignalResult analyze(Submission submission, List<SubmissionCaseResult> caseResults) {
        return empty();
    }

    public static RuleSignalResult empty() {
        return RuleSignalResult.builder()
                .signals(List.of())
                .candidateIssueTags(List.of())
                .candidateFineGrainedTags(List.of())
                .evidenceRefs(List.of())
                .build();
    }

    @Data
    @Builder
    public static class RuleSignalResult {
        private List<Signal> signals;
        private List<String> candidateIssueTags;
        private List<String> candidateFineGrainedTags;
        private List<String> evidenceRefs;
    }

    @Data
    @Builder
    public static class Signal {
        private String evidenceRef;
        private String coarseTag;
        private String fineTag;
        private Double confidence;
        private String message;
    }
}
