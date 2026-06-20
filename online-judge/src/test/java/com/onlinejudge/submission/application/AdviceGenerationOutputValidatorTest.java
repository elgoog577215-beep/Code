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
