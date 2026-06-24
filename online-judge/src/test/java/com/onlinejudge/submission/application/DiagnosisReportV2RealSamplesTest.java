package com.onlinejudge.submission.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DiagnosisReportV2RealSamplesTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void realSamplesCoverEightTargetProblemsWithQualityExpectations() throws Exception {
        List<RealSample> samples = loadSamples();

        assertThat(samples).extracting(RealSample::id)
                .containsExactly(
                        "sum-right-endpoint",
                        "matrix-difference-prefix-restore",
                        "monotonic-stack-duplicates",
                        "stone-merge-dp-interval",
                        "dynamic-connectivity-missing-union",
                        "string-window-boundary",
                        "sliding-window-balance-count",
                        "layered-shortest-path-state"
                );
        assertThat(samples).extracting(RealSample::expectedLibraryFit)
                .allMatch(fit -> Set.of("HIT", "PARTIAL", "MISS").contains(fit));
        assertThat(samples).anySatisfy(sample -> assertThat(sample.expectedLibraryFit()).isEqualTo("HIT"));
        assertThat(samples).anySatisfy(sample -> assertThat(sample.expectedLibraryFit()).isEqualTo("PARTIAL"));
        assertThat(samples).filteredOn(RealSample::shouldGenerateGrowthCandidate)
                .allSatisfy(sample -> assertThat(sample.expectedLibraryFit()).isIn("PARTIAL", "MISS"));

        samples.forEach(sample -> {
            assertThat(sample.problemSummary()).isNotBlank();
            assertThat(sample.buggyCode()).isNotBlank();
            assertThat(sample.buggyCode().lines().count()).isGreaterThanOrEqualTo(4);
            assertThat(sample.judgeResult()).isNotBlank();
            assertThat(sample.expectedAnchors()).isNotEmpty();
            assertThat(sample.expectedStudentFeedbackQuality()).hasSizeGreaterThanOrEqualTo(3);
            assertThat(sample.quickFeedbackKnownLimit()).isNotBlank();
            assertThat(sample.formalMustImproveAtLeastOneOf()).isNotEmpty();
        });
    }

    @Test
    void formalChainComparisonMetadataRequiresAtLeastOneConcreteImprovementOverQuickFeedback() throws Exception {
        List<RealSample> samples = loadSamples();

        samples.forEach(sample -> {
            assertThat(sample.quickFeedbackKnownLimit())
                    .as(sample.id() + " quick feedback limitation")
                    .containsAnyOf("快反馈", "通常", "可能", "常说");
            assertThat(sample.formalMustImproveAtLeastOneOf())
                    .as(sample.id() + " formal chain improvement")
                    .allSatisfy(requirement -> assertThat(requirement).isNotBlank());
            assertThat(String.join("\n", sample.expectedStudentFeedbackQuality()))
                    .as(sample.id() + " student visible quality")
                    .containsAnyOf("基础层", "下一步", "提高层");
        });
    }

    private List<RealSample> loadSamples() throws Exception {
        try (InputStream input = getClass().getResourceAsStream("/diagnosis-eval-fixtures/diagnosis-report-v2-real-samples.json")) {
            assertThat(input).isNotNull();
            return objectMapper.readValue(input, new TypeReference<>() {
            });
        }
    }

    private record RealSample(
            String id,
            String title,
            String problemSummary,
            String language,
            String buggyCode,
            String judgeResult,
            String expectedLibraryFit,
            List<String> expectedAnchors,
            List<String> expectedStudentFeedbackQuality,
            boolean shouldGenerateGrowthCandidate,
            String quickFeedbackKnownLimit,
            List<String> formalMustImproveAtLeastOneOf
    ) {
    }
}
