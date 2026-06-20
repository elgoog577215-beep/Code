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
                .candidateSignals(List.of(ModelDiagnosisBrief.CandidateSignal.builder()
                        .evidenceRef("code:range_excludes_n")
                        .issueTag("LOOP_BOUNDARY")
                        .fineGrainedTag("OFF_BY_ONE")
                        .confidence(0.9)
                        .build()))
                .build();
    }
}
