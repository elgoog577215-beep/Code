package com.onlinejudge.submission.application;

import com.onlinejudge.learning.diagnosis.DiagnosisTaxonomy;
import com.onlinejudge.submission.dto.SubmissionAnalysisResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AdviceGenerationOutputValidatorTest {

    private final AdviceGenerationOutputValidator validator = new AdviceGenerationOutputValidator();

    @Test
    void acceptsValidAdviceOutput() {
        ExternalModelStagePayloads.StageValidationResult result = validator.validate(
                validOutput(),
                brief("WRONG_ANSWER"),
                pack()
        );

        assertThat(result.isValid()).isTrue();
        assertThat(result.getFailureReason()).isEqualTo(ModelStageFailureReason.NONE);
    }

    @Test
    void acceptsDiagnosisReportV2WithStateDefinitionAtL3() {
        AdviceGenerationOutput output = AdviceGenerationOutput.builder()
                .executionGate(AdviceGenerationOutput.ExecutionGate.builder()
                        .state("SHOW_TO_STUDENT")
                        .priority("BASIC_FIRST")
                        .reason("存在基础层阻塞问题。")
                        .build())
                .diagnosisDecision(AdviceGenerationOutput.DiagnosisDecision.builder()
                        .libraryFit("HIT")
                        .anchors(List.of(AdviceGenerationOutput.DiagnosisAnchor.builder()
                                .id("MP_RANGE_RIGHT_ENDPOINT_MISSING")
                                .type("MISTAKE_POINT")
                                .role("PRIMARY")
                                .confidence(0.9)
                                .evidenceRefs(List.of("code:range_excludes_n"))
                                .reason("循环取值范围和题目端点要求不一致。")
                                .build()))
                        .build())
                .studentReport(AdviceGenerationOutput.StudentReport.builder()
                        .hintLevel("L3")
                        .basicLayerText("基础层：先把 f[i][j] 这类状态含义想清楚，再检查循环端点是否覆盖题目要求。")
                        .improvementLayerText("提高层：修好后用最小值和端点值做边界自测。")
                        .nextActionText("下一步：手推 n=1、n=2 时循环变量实际出现过哪些值。")
                        .build())
                .studentSummary("这次重点是边界和状态含义。")
                .build();

        ExternalModelStagePayloads.StageValidationResult result = validator.validate(
                output,
                brief("WRONG_ANSWER"),
                pack()
        );

        assertThat(result.isValid()).isTrue();
    }

    @Test
    void softRepairsEvidenceAliasesAndUnknownAnchorIdsForDiagnosisReportV2() {
        AdviceGenerationOutput output = AdviceGenerationOutput.builder()
                .diagnosisDecision(AdviceGenerationOutput.DiagnosisDecision.builder()
                        .libraryFit("PARTIAL")
                        .anchors(List.of(AdviceGenerationOutput.DiagnosisAnchor.builder()
                                .id("MP_LIBRARY_GAP_NEW_BOUNDARY_CASE")
                                .type("MISTAKE_POINT")
                                .role("PRIMARY")
                                .confidence(0.82)
                                .evidenceRefs(List.of("sourceCode", "problemConstraints"))
                                .reason("模型发现了库里没有精确覆盖的边界错因。")
                                .build()))
                        .build())
                .studentReport(AdviceGenerationOutput.StudentReport.builder()
                        .hintLevel("L3")
                        .basicLayerText("基础层：代码的循环范围和题目边界条件没有完全对齐。")
                        .improvementLayerText("提高层：修好后补充最小值和端点值自测。")
                        .nextActionText("下一步：先手推一个最小样例，确认循环变量是否覆盖端点。")
                        .build())
                .studentSummary("这次重点是边界条件。")
                .build();

        ExternalModelStagePayloads.StageValidationResult result = validator.validate(
                output,
                brief("WRONG_ANSWER"),
                pack()
        );

        assertThat(result.isValid()).isTrue();
        assertThat(result.getSoftFixes())
                .contains("evidenceRef alias sourceCode -> code:range_excludes_n")
                .contains("evidenceRef alias problemConstraints -> judge:first_failed_case")
                .anySatisfy(item -> assertThat(item).contains("unknown anchor id"));
        assertThat(output.getDiagnosisDecision().getAnchors()).singleElement()
                .satisfies(anchor -> {
                    assertThat(anchor.getType()).isEqualTo("OUT_OF_LIBRARY");
                    assertThat(anchor.getId()).isNull();
                    assertThat(anchor.getEvidenceRefs()).containsExactly("code:range_excludes_n", "judge:first_failed_case");
                });
    }

    @Test
    void softRepairsEvidenceRefsWithExtraDetailForDiagnosisReportV2() {
        AdviceGenerationOutput output = AdviceGenerationOutput.builder()
                .diagnosisDecision(AdviceGenerationOutput.DiagnosisDecision.builder()
                        .libraryFit("HIT")
                        .anchors(List.of(AdviceGenerationOutput.DiagnosisAnchor.builder()
                                .id("MP_RANGE_RIGHT_ENDPOINT_MISSING")
                                .type("MISTAKE_POINT")
                                .role("PRIMARY")
                                .confidence(0.9)
                                .evidenceRefs(List.of("judge:first_failed_case:1", "code:range_excludes_n:line3"))
                                .reason("模型补充了证据细节，但前缀仍对应合法证据。")
                                .build()))
                        .build())
                .studentReport(AdviceGenerationOutput.StudentReport.builder()
                        .hintLevel("L3")
                        .basicLayerText("基础层：循环取值范围和题目要求的端点没有对齐。")
                        .improvementLayerText("提高层：修好后补充最小值和端点值自测。")
                        .nextActionText("下一步：手推 n=1、n=2 时循环变量实际出现过哪些值。")
                        .build())
                .studentSummary("这次重点是循环边界。")
                .build();

        ExternalModelStagePayloads.StageValidationResult result = validator.validate(
                output,
                brief("WRONG_ANSWER"),
                pack()
        );

        assertThat(result.isValid()).isTrue();
        assertThat(result.getSoftFixes())
                .contains("evidenceRef alias judge:first_failed_case:1 -> judge:first_failed_case")
                .contains("evidenceRef alias code:range_excludes_n:line3 -> code:range_excludes_n");
        assertThat(output.getDiagnosisDecision().getAnchors()).singleElement()
                .satisfies(anchor -> assertThat(anchor.getEvidenceRefs())
                        .containsExactly("judge:first_failed_case", "code:range_excludes_n"));
    }

    @Test
    void softRepairsVerdictEvidenceAliasForDiagnosisReportV2() {
        AdviceGenerationOutput output = AdviceGenerationOutput.builder()
                .diagnosisDecision(AdviceGenerationOutput.DiagnosisDecision.builder()
                        .libraryFit("HIT")
                        .anchors(List.of(AdviceGenerationOutput.DiagnosisAnchor.builder()
                                .id("MP_RANGE_RIGHT_ENDPOINT_MISSING")
                                .type("MISTAKE_POINT")
                                .role("PRIMARY")
                                .confidence(0.9)
                                .evidenceRefs(List.of("verdict:wrong-answer"))
                                .reason("判题结果显示当前行为与期望不一致。")
                                .build()))
                        .build())
                .studentReport(AdviceGenerationOutput.StudentReport.builder()
                        .hintLevel("L3")
                        .basicLayerText("基础层：循环取值范围和题目要求的端点没有对齐。")
                        .improvementLayerText("提高层：修好后补充最小值和端点值自测。")
                        .nextActionText("下一步：手推一个最小样例，确认循环变量是否覆盖端点。")
                        .build())
                .studentSummary("这次重点是循环边界。")
                .build();

        ExternalModelStagePayloads.StageValidationResult result = validator.validate(
                output,
                brief("WRONG_ANSWER"),
                pack()
        );

        assertThat(result.isValid()).isTrue();
        assertThat(result.getSoftFixes()).contains("evidenceRef alias verdict:wrong-answer -> judge:first_failed_case");
        assertThat(output.getDiagnosisDecision().getAnchors()).singleElement()
                .satisfies(anchor -> assertThat(anchor.getEvidenceRefs()).containsExactly("judge:first_failed_case"));
    }

    @Test
    void rejectsUnknownStandardLibraryId() {
        AdviceGenerationOutput output = validOutput();
        output.getBasicLayerAdvice().get(0).setMistakePointId("MP_UNKNOWN");

        ExternalModelStagePayloads.StageValidationResult result = validator.validate(
                output,
                brief("WRONG_ANSWER"),
                pack()
        );

        assertThat(result.isValid()).isFalse();
        assertThat(result.getFailureReason()).isEqualTo(ModelStageFailureReason.INVALID_TAG);
    }

    @Test
    void acceptsLegacyBasicCauseIdWhenSearchLocationFallsBack() {
        AdviceGenerationOutput output = validOutput();
        output.getBasicLayerAdvice().get(0).setMistakePointId("LOOP_BOUNDARY");
        output.getBasicLayerAdvice().get(0).setSkillUnitId(null);
        output.getImprovementLayerAdvice().get(0).setSkillUnitId(null);

        StandardLibraryPack legacyPack = StandardLibraryPack.builder()
                .schemaVersion(StandardLibraryPack.SCHEMA_VERSION)
                .taxonomyVersion(DiagnosisTaxonomy.TAXONOMY_VERSION)
                .basicCauses(List.of(StandardLibraryPack.BasicCauseOption.builder()
                        .id("LOOP_BOUNDARY")
                        .name("循环边界")
                        .build()))
                .improvementPoints(List.of(StandardLibraryPack.ImprovementPointOption.builder()
                        .id("TESTING_HABIT")
                        .name("自测与反例构造")
                        .build()))
                .build();

        ExternalModelStagePayloads.StageValidationResult result = validator.validate(
                output,
                brief("WRONG_ANSWER"),
                legacyPack
        );

        assertThat(result.isValid()).isTrue();
    }

    @Test
    void rejectsInvalidEvidenceRef() {
        AdviceGenerationOutput output = validOutput();
        output.getBasicLayerAdvice().get(0).setEvidenceRefs(List.of("invented:evidence"));

        ExternalModelStagePayloads.StageValidationResult result = validator.validate(
                output,
                brief("WRONG_ANSWER"),
                pack()
        );

        assertThat(result.isValid()).isFalse();
        assertThat(result.getFailureReason()).isEqualTo(ModelStageFailureReason.INVALID_EVIDENCE_REF);
    }

    @Test
    void rejectsUnsafeDirectAnswerText() {
        AdviceGenerationOutput output = validOutput();
        output.getBasicLayerAdvice().get(0).setStudentAction("直接改成 range(1, n + 1)。");

        ExternalModelStagePayloads.StageValidationResult result = validator.validate(
                output,
                brief("WRONG_ANSWER"),
                pack()
        );

        assertThat(result.isValid()).isFalse();
        assertThat(result.getFailureReason()).isEqualTo(ModelStageFailureReason.SAFETY_RISK);
    }

    @Test
    void rejectsEmptyBasicAdviceForNonAcceptedSubmission() {
        AdviceGenerationOutput output = validOutput();
        output.setBasicLayerAdvice(List.of());

        ExternalModelStagePayloads.StageValidationResult result = validator.validate(
                output,
                brief("WRONG_ANSWER"),
                pack()
        );

        assertThat(result.isValid()).isFalse();
        assertThat(result.getMessage()).contains("basicLayerAdvice");
    }

    @Test
    void allowsEmptyBasicAdviceForAcceptedSubmission() {
        AdviceGenerationOutput output = validOutput();
        output.setBasicLayerAdvice(List.of());

        ExternalModelStagePayloads.StageValidationResult result = validator.validate(
                output,
                brief("ACCEPTED"),
                pack()
        );

        assertThat(result.isValid()).isTrue();
    }

    @Test
    void softTrimsOverlongDiagnosisReportV2StudentText() {
        AdviceGenerationOutput output = AdviceGenerationOutput.builder()
                .studentReport(AdviceGenerationOutput.StudentReport.builder()
                        .hintLevel("L3")
                        .basicLayerText("基础层：".repeat(220))
                        .improvementLayerText("提高层：修好后做边界测试。")
                        .nextActionText("下一步：手推一个最小样例。")
                        .build())
                .studentSummary("报告过长。")
                .build();

        ExternalModelStagePayloads.StageValidationResult result = validator.validate(
                output,
                brief("WRONG_ANSWER"),
                pack()
        );

        assertThat(result.isValid()).isTrue();
        assertThat(output.getStudentReport().getBasicLayerText()).hasSize(360);
        assertThat(output.getStudentReport().getBasicLayerText()).endsWith("…");
        assertThat(result.getSoftFixes()).contains("studentReport.basicLayerText trimmed to 360 chars");
    }

    private AdviceGenerationOutput validOutput() {
        return AdviceGenerationOutput.builder()
                .caseUnderstanding(AdviceGenerationOutput.CaseUnderstanding.builder()
                        .problemGoal("输出 1 到 n 的整数和。")
                        .codeIntent("学生想用循环累加。")
                        .behaviorGap("循环实际没有覆盖题目要求的末端。")
                        .primaryEvidenceRef("code:range_excludes_n")
                        .build())
                .basicLayerAdvice(List.of(AdviceGenerationOutput.BasicLayerAdvice.builder()
                        .mistakePointId("MP_RANGE_RIGHT_ENDPOINT_MISSING")
                        .skillUnitId("SK_RANGE_BOUNDARY")
                        .title("循环右边界漏取")
                        .whatHappened("当前循环范围没有覆盖题目要求的最后一个数。")
                        .whyItMatters("少处理端点会让求和结果偏小。")
                        .studentAction("先手推 n=1 和 n=2 时循环变量实际出现过哪些值。")
                        .checkQuestion("最后一个应该被处理的数有没有进入循环？")
                        .evidenceRefs(List.of("code:range_excludes_n"))
                        .confidence(0.92)
                        .build()))
                .improvementLayerAdvice(List.of(AdviceGenerationOutput.ImprovementLayerAdvice.builder()
                        .improvementPointId("TESTING_HABIT")
                        .skillUnitId("SK_RANGE_BOUNDARY")
                        .title("补充边界样例意识")
                        .currentLimit("这类问题不是算法方向错，而是边界验证不足。")
                        .suggestion("修复后补测最小值、端点值和最大值附近样例。")
                        .studentBenefit("能更早发现开闭区间和下标边界问题。")
                        .evidenceRefs(List.of("code:range_excludes_n"))
                        .confidence(0.8)
                        .build()))
                .nextStepPlan(List.of(AdviceGenerationOutput.NextStepAdvice.builder()
                        .step(1)
                        .target("手推循环变量取值。")
                        .reason("这是当前阻塞通过的主要问题。")
                        .evidenceRef("code:range_excludes_n")
                        .build()))
                .studentSummary("这次主要卡在循环边界和题目要求范围没有对齐。")
                .build();
    }

    private ModelDiagnosisBrief brief(String verdict) {
        return ModelDiagnosisBrief.builder()
                .schemaVersion(ModelDiagnosisBrief.SCHEMA_VERSION)
                .verdict(verdict)
                .evidenceRefs(List.of("code:range_excludes_n", "judge:first_failed_case"))
                .candidateSignals(List.of(ModelDiagnosisBrief.CandidateSignal.builder()
                        .evidenceRef("code:range_excludes_n")
                        .issueTag("LOOP_BOUNDARY")
                        .fineGrainedTag("OFF_BY_ONE")
                        .confidence(0.9)
                        .reason("range excludes n")
                        .build()))
                .build();
    }

    private StandardLibraryPack pack() {
        return StandardLibraryPack.builder()
                .schemaVersion(StandardLibraryPack.SCHEMA_VERSION)
                .taxonomyVersion(DiagnosisTaxonomy.TAXONOMY_VERSION)
                .mistakePoints(List.of(StandardLibraryPack.MistakePointOption.builder()
                        .id("MP_RANGE_RIGHT_ENDPOINT_MISSING")
                        .name("循环右边界漏取")
                        .build()))
                .skillUnits(List.of(StandardLibraryPack.SkillUnitOption.builder()
                        .id("SK_RANGE_BOUNDARY")
                        .name("循环边界")
                        .build()))
                .improvementPoints(List.of(StandardLibraryPack.ImprovementPointOption.builder()
                        .id("TESTING_HABIT")
                        .name("自测与反例构造")
                        .build()))
                .build();
    }
}
