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
    void compactRuntimeKeepsStructuredKnowledgeGroupsWithinBudget() {
        ExternalModelAgentRuntime runtime = new ExternalModelAgentRuntime(
                new ModelDiagnosisBriefBuilder(),
                builder,
                new PromptTemplateRegistry(),
                new ModelOutputValidator()
        );
        StandardLibraryPack source = StandardLibraryPack.builder()
                .schemaVersion(StandardLibraryPack.SCHEMA_VERSION)
                .taxonomyVersion(DiagnosisTaxonomy.TAXONOMY_VERSION)
                .structureVersion(StandardLibraryPack.STRUCTURE_VERSION)
                .knowledgeGroups(List.of(StandardLibraryPack.KnowledgeGroupOption.builder()
                        .id("BASIC.LOOP.BOUNDARY")
                        .name("循环边界")
                        .path("BASIC > LOOP > BOUNDARY")
                        .description("这是一个用于验证 compact 是否保留结构视图的长描述。".repeat(8))
                        .skillUnits(List.of(StandardLibraryPack.SkillUnitGroupOption.builder()
                                .skillUnit(StandardLibraryPack.SkillUnitOption.builder()
                                        .id("SK_RANGE_BOUNDARY")
                                        .category("循环边界")
                                        .name("能判断循环区间是否包含答案")
                                        .description("这是一个很长的能力点定义，用来确认 compact 后仍保留能力点但会限制描述长度。".repeat(8))
                                        .knowledgeNodeCodes(List.of("BASIC.LOOP.BOUNDARY"))
                                        .applicableLanguages(List.of("PYTHON", "CPP17", "JAVA", "GO"))
                                        .build())
                                .mistakePoints(List.of(
                                        mistake("MP_BOUNDARY_1"),
                                        mistake("MP_BOUNDARY_2"),
                                        mistake("MP_BOUNDARY_3"),
                                        mistake("MP_BOUNDARY_4"),
                                        mistake("MP_BOUNDARY_5"),
                                        mistake("MP_BOUNDARY_6")
                                ))
                                .improvementPoints(List.of(
                                        improvement("IP_BOUNDARY_1"),
                                        improvement("IP_BOUNDARY_2"),
                                        improvement("IP_BOUNDARY_3"),
                                        improvement("IP_BOUNDARY_4")
                                ))
                                .candidateIds(List.of(
                                        "SK_RANGE_BOUNDARY",
                                        "MP_BOUNDARY_1",
                                        "MP_BOUNDARY_2",
                                        "MP_BOUNDARY_3",
                                        "MP_BOUNDARY_4",
                                        "MP_BOUNDARY_5",
                                        "MP_BOUNDARY_6",
                                        "IP_BOUNDARY_1",
                                        "IP_BOUNDARY_2",
                                        "IP_BOUNDARY_3"
                                ))
                                .build()))
                        .build()))
                .build();

        StandardLibraryPack compactPack = ReflectionTestUtils.invokeMethod(runtime, "compactStandardLibraryPack", source);

        assertThat(compactPack.getStructureVersion()).isEqualTo(StandardLibraryPack.STRUCTURE_VERSION);
        assertThat(compactPack.getKnowledgeGroups()).singleElement().satisfies(group -> {
            assertThat(group.getDescription()).hasSizeLessThanOrEqualTo(104);
            assertThat(group.getSkillUnits()).singleElement().satisfies(skillGroup -> {
                assertThat(skillGroup.getSkillUnit().getId()).isEqualTo("SK_RANGE_BOUNDARY");
                assertThat(skillGroup.getSkillUnit().getDescription()).hasSizeLessThanOrEqualTo(114);
                assertThat(skillGroup.getMistakePoints()).hasSize(5);
                assertThat(skillGroup.getImprovementPoints()).hasSize(3);
                assertThat(skillGroup.getCandidateIds()).hasSize(9);
            });
        });
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

        assertThat(standardPlan.getAdvicePrompt().getVersion()).isEqualTo(PromptTemplateRegistry.DIAGNOSIS_REPORT_V2);
        assertThat(lowLatencyPlan.getAdvicePrompt().getVersion()).isEqualTo(PromptTemplateRegistry.DIAGNOSIS_REPORT_V2);
        assertThat(lowLatencyPlan.getAdvicePrompt().getSystemPrompt())
                .contains("diagnosis report v2")
                .contains("单诊断 Agent")
                .contains("标准库的作用")
                .contains("studentReport")
                .contains("basicLayerText")
                .contains("improvementLayerText")
                .contains("diagnosisDecision")
                .doesNotContain("teachingHint");
    }

    private StandardLibraryPack.MistakePointOption mistake(String id) {
        return StandardLibraryPack.MistakePointOption.builder()
                .id(id)
                .category("循环边界")
                .name(id)
                .description("边界易错点描述".repeat(10))
                .skillUnitCode("SK_RANGE_BOUNDARY")
                .mistakeType("OFF_BY_ONE")
                .commonMisconception("误解循环边界。".repeat(10))
                .knowledgeNodeCodes(List.of("BASIC.LOOP.BOUNDARY"))
                .applicableLanguages(List.of("PYTHON", "CPP17", "JAVA", "GO"))
                .build();
    }

    private StandardLibraryPack.ImprovementPointOption improvement(String id) {
        return StandardLibraryPack.ImprovementPointOption.builder()
                .id(id)
                .category("边界验证")
                .name(id)
                .description("提升点描述".repeat(10))
                .whenToUse("修复基础问题后，用多类边界样例验证模板。".repeat(8))
                .studentBenefit("提升迁移能力。".repeat(8))
                .abilityPoint("SK_RANGE_BOUNDARY")
                .relatedBasicCauses(List.of("MP_BOUNDARY_1"))
                .build();
    }
}
