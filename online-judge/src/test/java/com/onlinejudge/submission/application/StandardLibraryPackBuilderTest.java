package com.onlinejudge.submission.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlinejudge.learning.diagnosis.DiagnosisTaxonomy;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StandardLibraryPackBuilderTest {

    private final StandardLibraryPackBuilder builder =
            new StandardLibraryPackBuilder(new DiagnosisTaxonomy());

    @Test
    void buildsBasicAndImprovementKnowledgeLayersWithoutRuntimeProtocols() throws Exception {
        ModelDiagnosisBrief brief = ModelDiagnosisBrief.builder()
                .allowedIssueTags(List.of("IO_FORMAT", "TIME_COMPLEXITY", "ALGORITHM_STRATEGY"))
                .allowedFineGrainedTags(List.of("INPUT_PARSING", "BRUTE_FORCE_LIMIT", "GREEDY_ASSUMPTION"))
                .build();

        StandardLibraryPack pack = builder.build(brief, null);

        assertThat(pack.getBasicCauses())
                .extracting(StandardLibraryPack.BasicCauseOption::getId)
                .contains(
                        "IO_FORMAT",
                        "INPUT_PARSING",
                        "TIME_COMPLEXITY",
                        "BRUTE_FORCE_LIMIT",
                        "ALGORITHM_STRATEGY",
                        "GREEDY_ASSUMPTION",
                        "NEEDS_MORE_EVIDENCE"
                );
        assertThat(pack.getBasicCauses())
                .filteredOn(cause -> "IO_FORMAT".equals(cause.getId()))
                .singleElement()
                .satisfies(cause -> {
                    assertThat(cause.getCategory()).isEqualTo("输入输出");
                    assertThat(cause.getEvidenceSignals()).contains("wrong output shape", "input format");
                    assertThat(cause.getHintL1()).contains("输入输出格式");
                    assertThat(cause.getHintL2()).contains("读几组");
                    assertThat(cause.getHintL3()).contains("最小样例");
                });
        assertThat(pack.getImprovementPoints())
                .extracting(StandardLibraryPack.ImprovementPointOption::getId)
                .contains("COMPLEXITY", "ALGORITHM_MODELING", "GREEDY_PROOF", "TESTING_HABIT");
        assertThat(pack.getImprovementPoints())
                .filteredOn(point -> "COMPLEXITY".equals(point.getId()))
                .singleElement()
                .satisfies(point -> {
                    assertThat(point.getCategory()).isEqualTo("复杂度");
                    assertThat(point.getHintL1()).contains("核心循环");
                    assertThat(point.getRelatedBasicCauses()).contains("TIME_COMPLEXITY", "BRUTE_FORCE_LIMIT");
                });
        assertThat(pack.getIssueTags())
                .extracting(StandardLibraryPack.TagOption::getId)
                .contains("IO_FORMAT", "TIME_COMPLEXITY", "ALGORITHM_STRATEGY", "NEEDS_MORE_EVIDENCE");
        assertThat(pack.getFineGrainedTags())
                .extracting(StandardLibraryPack.TagOption::getId)
                .contains("INPUT_PARSING", "BRUTE_FORCE_LIMIT", "GREEDY_ASSUMPTION");
        assertThat(pack.getTeachingActions())
                .extracting(StandardLibraryPack.TeachingActionOption::getId)
                .contains("COMPARE_INPUT_SPEC", "COUNT_COMPLEXITY", "CHECK_INVARIANT", "COLLECT_EVIDENCE");

        String json = new ObjectMapper().writeValueAsString(pack);
        assertThat(json)
                .contains("\"basicCauses\"")
                .contains("\"improvementPoints\"")
                .doesNotContain("decisionProtocol")
                .doesNotContain("educationAgentProtocol")
                .doesNotContain("studentFeedbackRules")
                .doesNotContain("judgmentCalibrationExamples")
                .doesNotContain("safetyRules")
                .doesNotContain("uncertaintyOptions");
    }

    @Test
    void ruleSignalsAddFineGrainedBasicCausesAndTeachingActions() {
        ModelDiagnosisBrief brief = ModelDiagnosisBrief.builder()
                .allowedIssueTags(List.of("BOUNDARY_CONDITION"))
                .build();
        RuleSignalAnalyzer.RuleSignalResult signals = RuleSignalAnalyzer.RuleSignalResult.builder()
                .candidateFineGrainedTags(List.of("OUTPUT_FORMAT_DETAIL"))
                .build();

        StandardLibraryPack pack = builder.build(brief, signals);

        assertThat(pack.getBasicCauses())
                .extracting(StandardLibraryPack.BasicCauseOption::getId)
                .contains("BOUNDARY_CONDITION", "OUTPUT_FORMAT_DETAIL", "NEEDS_MORE_EVIDENCE");
        assertThat(pack.getTeachingActions())
                .extracting(StandardLibraryPack.TeachingActionOption::getId)
                .contains("ASK_MIN_CASE", "COMPARE_OUTPUT");
        assertThat(pack.getImprovementPoints())
                .extracting(StandardLibraryPack.ImprovementPointOption::getId)
                .contains("TESTING_HABIT", "BOUNDARY_AWARENESS", "CODE_ORGANIZATION");
    }

    @Test
    void defaultSliceStillCarriesEvidenceCollectionAndReviewKnowledge() {
        StandardLibraryPack pack = builder.build(ModelDiagnosisBrief.builder().build(), null);

        assertThat(pack.getBasicCauses())
                .extracting(StandardLibraryPack.BasicCauseOption::getId)
                .containsExactly("NEEDS_MORE_EVIDENCE");
        assertThat(pack.getImprovementPoints())
                .extracting(StandardLibraryPack.ImprovementPointOption::getId)
                .contains("TESTING_HABIT", "TRANSFER_REVIEW");
        assertThat(pack.getImprovementTags())
                .extracting(StandardLibraryPack.ImprovementTagOption::getId)
                .contains("TESTING_HABIT", "TRANSFER_REVIEW");
    }

    @Test
    void compactRuntimeKeepsLayeredKnowledgeButTrimsLongFields() {
        ExternalModelAgentRuntime runtime = new ExternalModelAgentRuntime(
                new ModelDiagnosisBriefBuilder(),
                builder,
                new PromptTemplateRegistry(),
                new ModelOutputValidator()
        );
        StandardLibraryPack fullPack = builder.build(ModelDiagnosisBrief.builder()
                .allowedIssueTags(List.of("TIME_COMPLEXITY", "STATE_TRANSITION"))
                .allowedFineGrainedTags(List.of("BRUTE_FORCE_LIMIT", "DP_STATE_DESIGN"))
                .build(), null);

        StandardLibraryPack compactPack = ReflectionTestUtils.invokeMethod(runtime, "compactStandardLibraryPack", fullPack);

        assertThat(compactPack.getBasicCauses())
                .extracting(StandardLibraryPack.BasicCauseOption::getId)
                .contains("TIME_COMPLEXITY", "BRUTE_FORCE_LIMIT", "STATE_TRANSITION", "DP_STATE_DESIGN");
        assertThat(compactPack.getImprovementPoints())
                .extracting(StandardLibraryPack.ImprovementPointOption::getId)
                .contains("COMPLEXITY", "DP_STATE_DESIGN");
        assertThat(compactPack.getBasicCauses())
                .allSatisfy(cause -> assertThat(cause.getHintL1()).hasSizeLessThanOrEqualTo(70));
        assertThat(compactPack.getImprovementPoints())
                .allSatisfy(point -> assertThat(point.getHintL2()).hasSizeLessThanOrEqualTo(70));
    }

    @Test
    void runtimePlanAlwaysUsesFormalAdvicePrompt() {
        ExternalModelAgentRuntime runtime = new ExternalModelAgentRuntime(
                new ModelDiagnosisBriefBuilder(),
                builder,
                new PromptTemplateRegistry(),
                new ModelOutputValidator()
        );

        ExternalModelAgentRuntime.RuntimePlan standardPlan = runtime.prepare(
                DiagnosisEvidencePackage.builder().build(),
                RuleSignalAnalyzer.RuleSignalResult.builder().build(),
                null,
                ExternalModelAgentRuntime.RUNTIME_PROFILE_STANDARD
        );
        ExternalModelAgentRuntime.RuntimePlan lowLatencyPlan = runtime.prepare(
                DiagnosisEvidencePackage.builder().build(),
                RuleSignalAnalyzer.RuleSignalResult.builder().build(),
                null,
                ExternalModelAgentRuntime.RUNTIME_PROFILE_LOW_LATENCY
        );

        assertThat(standardPlan.getAdvicePrompt().getVersion()).isEqualTo(PromptTemplateRegistry.DIAGNOSIS_AND_ADVICE_V1);
        assertThat(lowLatencyPlan.getAdvicePrompt().getVersion()).isEqualTo(PromptTemplateRegistry.DIAGNOSIS_AND_ADVICE_V1);
        assertThat(lowLatencyPlan.getAdvicePrompt().getSystemPrompt())
                .contains("complete diagnosis and advice generation stage")
                .contains("basicLayerAdvice")
                .contains("improvementLayerAdvice")
                .doesNotContain("diagnosisDecision")
                .doesNotContain("teachingHint");
    }
}
