package com.onlinejudge.submission.application;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AdviceGenerationOutputNormalizerTest {

    private final AdviceGenerationOutputNormalizer normalizer = new AdviceGenerationOutputNormalizer();
    private final AdviceGenerationOutputValidator validator = new AdviceGenerationOutputValidator();

    @Test
    void movesLegacyFineTagFromSkillUnitIdToMistakePointId() {
        AdviceGenerationOutput output = validOutput();
        output.getBasicLayerAdvice().get(0).setMistakePointId(null);
        output.getBasicLayerAdvice().get(0).setSkillUnitId("OFF_BY_ONE");

        AdviceGenerationOutput normalized = normalizer.normalize(output, pack());

        assertThat(normalized.getBasicLayerAdvice()).singleElement()
                .satisfies(item -> {
                    assertThat(item.getMistakePointId()).isEqualTo("OFF_BY_ONE");
                    assertThat(item.getSkillUnitId()).isNull();
                });
        assertThat(validator.validate(normalized, brief(), pack()).isValid()).isTrue();
    }

    @Test
    void clearsUnknownImprovementAndSkillIdsWithoutChangingAdviceText() {
        AdviceGenerationOutput output = validOutput();
        output.getImprovementLayerAdvice().get(0).setImprovementPointId("边界意识");
        output.getImprovementLayerAdvice().get(0).setSkillUnitId("OFF_BY_ONE");

        AdviceGenerationOutput normalized = normalizer.normalize(output, pack());

        assertThat(normalized.getImprovementLayerAdvice()).singleElement()
                .satisfies(item -> {
                    assertThat(item.getImprovementPointId()).isNull();
                    assertThat(item.getSkillUnitId()).isNull();
                    assertThat(item.getSuggestion()).contains("最小值");
                });
        assertThat(validator.validate(normalized, brief(), pack()).isValid()).isTrue();
    }

    @Test
    void rewritesDpStudentReportLeakIntoSafePrompt() {
        AdviceGenerationOutput output = validOutput();
        output.setStudentReport(AdviceGenerationOutput.StudentReport.builder()
                .hintLevel("L3")
                .basicLayerText("基础层：你写了 skip_current = dp[i-1] 和 take_current = values[i]。")
                .improvementLayerText("提高层：之后可以做空间压缩。")
                .nextActionText("下一步：检查前驱状态。")
                .build());
        output.setStudentSummary("这次重点是状态定义。");

        AdviceGenerationOutput normalized = normalizer.normalize(output, pack());

        assertThat(normalized.getStudentReport().getBasicLayerText())
                .contains("状态含义")
                .doesNotContain("dp[i", "skip_current", "take_current", "转移来源");
        assertThat(normalized.getStudentReport().getImprovementLayerText())
                .doesNotContain("空间压缩");
        assertThat(normalized.getStudentReport().getNextActionText())
                .contains("可见失败样例");
        assertThat(validator.validate(normalized, brief(), pack()).isValid()).isTrue();
    }

    private AdviceGenerationOutput validOutput() {
        return AdviceGenerationOutput.builder()
                .caseUnderstanding(AdviceGenerationOutput.CaseUnderstanding.builder()
                        .problemGoal("输出 1 到 n 的整数和。")
                        .codeIntent("学生使用循环累加。")
                        .behaviorGap("循环实际没有覆盖题目要求的末端。")
                        .primaryEvidenceRef("code:range_excludes_n")
                        .build())
                .basicLayerAdvice(List.of(AdviceGenerationOutput.BasicLayerAdvice.builder()
                        .mistakePointId("OFF_BY_ONE")
                        .skillUnitId(null)
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
                        .skillUnitId(null)
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

    private ModelDiagnosisBrief brief() {
        return ModelDiagnosisBrief.builder()
                .schemaVersion(ModelDiagnosisBrief.SCHEMA_VERSION)
                .verdict("WRONG_ANSWER")
                .evidenceRefs(List.of("code:range_excludes_n"))
                .build();
    }

    private StandardLibraryPack pack() {
        return StandardLibraryPack.builder()
                .basicCauses(List.of(StandardLibraryPack.BasicCauseOption.builder()
                        .id("OFF_BY_ONE")
                        .name("差一位错误")
                        .build()))
                .improvementPoints(List.of(StandardLibraryPack.ImprovementPointOption.builder()
                        .id("TESTING_HABIT")
                        .category("TESTING_HABIT")
                        .name("自测与反例构造")
                        .build()))
                .improvementTags(List.of(StandardLibraryPack.ImprovementTagOption.builder()
                        .id("TESTING_HABIT")
                        .label("自测习惯")
                        .build()))
                .build();
    }
}
