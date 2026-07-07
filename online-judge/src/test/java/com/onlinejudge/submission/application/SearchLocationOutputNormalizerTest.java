package com.onlinejudge.submission.application;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SearchLocationOutputNormalizerTest {

    private final SearchLocationOutputNormalizer normalizer = new SearchLocationOutputNormalizer();

    @Test
    void filtersInvalidEvidenceRefsWithoutSignalFallback() {
        SearchLocationCandidatePack pack = SearchLocationCandidatePack.builder()
                .candidates(List.of(SearchLocationCandidate.builder()
                        .id("MP_RANGE_RIGHT_ENDPOINT_MISSING")
                        .build()))
                .build();
        SearchLocationOutput output = SearchLocationOutput.builder()
                .basicCandidates(List.of(SearchLocationOutput.SelectedCandidate.builder()
                        .id("MP_RANGE_RIGHT_ENDPOINT_MISSING")
                        .confidence(0.9)
                        .evidenceRefs(List.of("learningTrajectorySummary"))
                        .build()))
                .build();

        SearchLocationOutput normalized = normalizer.normalize(output, pack, brief());

        assertThat(normalized.getBasicCandidates().get(0).getEvidenceRefs())
                .isEmpty();
    }

    @Test
    void keepsValidEvidenceRefsAndDropsInvalidOnes() {
        SearchLocationOutput output = SearchLocationOutput.builder()
                .basicCandidates(List.of(SearchLocationOutput.SelectedCandidate.builder()
                        .id("MP_RANGE_RIGHT_ENDPOINT_MISSING")
                        .confidence(0.9)
                        .evidenceRefs(List.of("code:range_excludes_n", "learningTrajectorySummary"))
                        .build()))
                .build();

        SearchLocationOutput normalized = normalizer.normalize(output, SearchLocationCandidatePack.builder().build(), brief());

        assertThat(normalized.getBasicCandidates().get(0).getEvidenceRefs())
                .containsExactly("code:range_excludes_n");
    }

    private ModelDiagnosisBrief brief() {
        return ModelDiagnosisBrief.builder()
                .evidenceRefs(List.of("code:range_excludes_n"))
                .build();
    }
}
