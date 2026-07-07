package com.onlinejudge.submission.application;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SearchLocationOutputValidatorTest {

    private final SearchLocationOutputValidator validator = new SearchLocationOutputValidator();

    @Test
    void acceptsSelectedCandidateFromCandidatePackWithEvidence() {
        SearchLocationCandidatePack pack = SearchLocationCandidatePack.builder()
                .candidates(List.of(SearchLocationCandidate.builder()
                        .id("MP_RANGE_RIGHT_ENDPOINT_MISSING")
                        .layer("MISTAKE_POINT")
                        .build()))
                .build();
        SearchLocationOutput output = SearchLocationOutput.builder()
                .basicCandidates(List.of(SearchLocationOutput.SelectedCandidate.builder()
                        .id("MP_RANGE_RIGHT_ENDPOINT_MISSING")
                        .layer("MISTAKE_POINT")
                        .confidence(0.9)
                        .evidenceRefs(List.of("code:range_excludes_n"))
                        .build()))
                .build();

        ExternalModelStagePayloads.StageValidationResult result = validator.validate(output, pack, brief());

        assertThat(result.isValid()).isTrue();
    }

    @Test
    void acceptsHitPartialAndMissLibraryFitStates() {
        SearchLocationCandidatePack pack = SearchLocationCandidatePack.builder()
                .candidates(List.of(SearchLocationCandidate.builder()
                        .id("SK_RANGE_BOUNDARY")
                        .layer("SKILL_UNIT")
                        .build()))
                .build();

        for (String fit : List.of("HIT", "PARTIAL", "MISS")) {
            SearchLocationOutput output = SearchLocationOutput.builder()
                    .libraryFit(fit)
                    .needsLibraryGrowth("MISS".equals(fit))
                    .basicCandidates(List.of(SearchLocationOutput.SelectedCandidate.builder()
                            .id("SK_RANGE_BOUNDARY")
                            .layer("SKILL_UNIT")
                            .libraryFit(fit)
                            .recallReason("代码中出现 range 边界，候选来自循环边界分支。")
                            .evidenceSource("sourceCode")
                            .uncertainty("MISS".equals(fit) ? "现有库没有精确错因，只能保留上级技能。": "")
                            .confidence(0.7)
                            .evidenceRefs(List.of("code:range_excludes_n"))
                            .build()))
                    .build();

            ExternalModelStagePayloads.StageValidationResult result = validator.validate(output, pack, brief());

            assertThat(result.isValid()).as(fit).isTrue();
        }
    }

    @Test
    void rejectsUnknownCandidateId() {
        SearchLocationCandidatePack pack = SearchLocationCandidatePack.builder()
                .candidates(List.of(SearchLocationCandidate.builder()
                        .id("KNOWN")
                        .layer("MISTAKE_POINT")
                        .build()))
                .build();
        SearchLocationOutput output = SearchLocationOutput.builder()
                .basicCandidates(List.of(SearchLocationOutput.SelectedCandidate.builder()
                        .id("UNKNOWN")
                        .layer("MISTAKE_POINT")
                        .confidence(0.9)
                        .evidenceRefs(List.of("code:range_excludes_n"))
                        .build()))
                .build();

        ExternalModelStagePayloads.StageValidationResult result = validator.validate(output, pack, brief());

        assertThat(result.isValid()).isFalse();
        assertThat(result.getMessage()).contains("not in candidate pack");
    }

    @Test
    void acceptsMissWithoutSelectedCandidates() {
        SearchLocationCandidatePack pack = SearchLocationCandidatePack.builder()
                .candidates(List.of(SearchLocationCandidate.builder()
                        .id("SK_RANGE_BOUNDARY")
                        .layer("SKILL_UNIT")
                        .build()))
                .build();
        SearchLocationOutput output = SearchLocationOutput.builder()
                .libraryFit("MISS")
                .needsLibraryGrowth(true)
                .libraryGrowthReason("当前候选没有覆盖真实错因，需要后续扩库。")
                .basicCandidates(List.of())
                .improvementCandidates(List.of())
                .knowledgeAnchors(List.of())
                .build();

        ExternalModelStagePayloads.StageValidationResult result = validator.validate(output, pack, brief());

        assertThat(result.isValid()).isTrue();
    }

    @Test
    void rejectsHitWithoutSelectedCandidates() {
        SearchLocationCandidatePack pack = SearchLocationCandidatePack.builder()
                .candidates(List.of(SearchLocationCandidate.builder()
                        .id("SK_RANGE_BOUNDARY")
                        .layer("SKILL_UNIT")
                        .build()))
                .build();
        SearchLocationOutput output = SearchLocationOutput.builder()
                .libraryFit("HIT")
                .basicCandidates(List.of())
                .improvementCandidates(List.of())
                .knowledgeAnchors(List.of())
                .build();

        ExternalModelStagePayloads.StageValidationResult result = validator.validate(output, pack, brief());

        assertThat(result.isValid()).isFalse();
        assertThat(result.getMessage()).contains("selected no candidates");
    }

    @Test
    void rejectsMissingEvidenceRefs() {
        SearchLocationCandidatePack pack = SearchLocationCandidatePack.builder()
                .candidates(List.of(SearchLocationCandidate.builder()
                        .id("KNOWN")
                        .layer("MISTAKE_POINT")
                        .build()))
                .build();
        SearchLocationOutput output = SearchLocationOutput.builder()
                .basicCandidates(List.of(SearchLocationOutput.SelectedCandidate.builder()
                        .id("KNOWN")
                        .layer("MISTAKE_POINT")
                        .confidence(0.9)
                        .build()))
                .build();

        ExternalModelStagePayloads.StageValidationResult result = validator.validate(output, pack, brief());

        assertThat(result.isValid()).isFalse();
        assertThat(result.getMessage()).contains("evidenceRefs");
    }

    private ModelDiagnosisBrief brief() {
        return ModelDiagnosisBrief.builder()
                .evidenceRefs(List.of("code:range_excludes_n"))
                .build();
    }
}
