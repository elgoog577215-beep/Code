package com.onlinejudge.submission.application;

import com.onlinejudge.submission.dto.SubmissionAnalysisResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AdviceGenerationFeedbackMapperTest {

    private final AdviceGenerationFeedbackMapper mapper = new AdviceGenerationFeedbackMapper();
    private final ModelOutputValidator validator = new ModelOutputValidator();

    @Test
    void mapsFineGrainedImprovementPointToLegacyImprovementTagCategory() {
        AdviceGenerationOutput output = AdviceGenerationOutput.builder()
                .caseUnderstanding(AdviceGenerationOutput.CaseUnderstanding.builder()
                        .problemGoal("输出 1 到 n 的整数和。")
                        .codeIntent("学生使用循环累加。")
                        .behaviorGap("循环没有覆盖题目要求的端点。")
                        .primaryEvidenceRef("code:range_excludes_n")
                        .build())
                .basicLayerAdvice(List.of(AdviceGenerationOutput.BasicLayerAdvice.builder()
                        .mistakePointId("MP_RANGE_RIGHT_ENDPOINT_MISSING")
                        .skillUnitId("SK_RANGE_BOUNDARY")
                        .title("循环右边界漏取")
                        .whatHappened("循环范围和题目要求不一致。")
                        .whyItMatters("少处理端点会影响求和结果。")
                        .studentAction("手推 n=1 时循环变量实际出现过哪些值。")
                        .checkQuestion("最后一个需要处理的数有没有进入循环？")
                        .evidenceRefs(List.of("code:range_excludes_n"))
                        .confidence(0.9)
                        .build()))
                .improvementLayerAdvice(List.of(AdviceGenerationOutput.ImprovementLayerAdvice.builder()
                        .improvementPointId("IP_BOUNDARY_MIN_CASE_TEST")
                        .skillUnitId("SK_RANGE_BOUNDARY")
                        .title("补充最小边界自测")
                        .currentLimit("这次暴露的是边界验证不足。")
                        .suggestion("修复后先补测最小输入和端点附近输入。")
                        .studentBenefit("能更早发现开闭区间问题。")
                        .evidenceRefs(List.of("code:range_excludes_n"))
                        .confidence(0.82)
                        .build()))
                .nextStepPlan(List.of(AdviceGenerationOutput.NextStepAdvice.builder()
                        .step(1)
                        .target("手推循环变量取值。")
                        .reason("这是当前阻塞通过的主要问题。")
                        .evidenceRef("code:range_excludes_n")
                        .build()))
                .studentSummary("这次主要卡在循环边界和最小样例验证。")
                .build();

        StandardLibraryPack pack = StandardLibraryPack.builder()
                .improvementPoints(List.of(StandardLibraryPack.ImprovementPointOption.builder()
                        .id("IP_BOUNDARY_MIN_CASE_TEST")
                        .category("BOUNDARY_AWARENESS")
                        .name("最小边界自测")
                        .build()))
                .improvementTags(List.of(
                        StandardLibraryPack.ImprovementTagOption.builder()
                                .id("TESTING_HABIT")
                                .label("自测习惯")
                                .build(),
                        StandardLibraryPack.ImprovementTagOption.builder()
                                .id("BOUNDARY_AWARENESS")
                                .label("边界意识")
                                .build()
                ))
                .teachingActions(List.of(StandardLibraryPack.TeachingActionOption.builder()
                        .id("TRACE_VARIABLES")
                        .label("手推变量")
                        .build()))
                .build();
        ModelDiagnosisBrief brief = ModelDiagnosisBrief.builder()
                .verdict("WRONG_ANSWER")
                .evidenceRefs(List.of("code:range_excludes_n"))
                .build();

        SubmissionAnalysisResponse.StudentFeedback feedback = mapper.toStudentFeedback(output, pack);

        assertThat(feedback.getImprovementOpportunities()).singleElement()
                .satisfies(item -> assertThat(item.getCategory()).isEqualTo("BOUNDARY_AWARENESS"));
        assertThat(validator.validateStudentFeedback(feedback, brief, pack).isValid()).isTrue();
    }
}
