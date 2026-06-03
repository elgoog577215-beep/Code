package com.onlinejudge.submission.application;

import com.onlinejudge.learning.diagnosis.DiagnosisTaxonomy;
import com.onlinejudge.submission.dto.SubmissionAnalysisResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExternalModelOutputNormalizerTest {

    private final ExternalModelOutputNormalizer normalizer = new ExternalModelOutputNormalizer();
    private final ModelOutputValidator validator = new ModelOutputValidator();

    @Test
    void normalizesChineseTagLabelsAndEvidenceRefsBeforeValidation() {
        Fixture fixture = fixture();
        ExternalModelStagePayloads.DiagnosisJudgeOutput output = ExternalModelStagePayloads.DiagnosisJudgeOutput.builder()
                .primaryIssueTag("循环边界")
                .fineGrainedTag("差 一 位 错误")
                .evidenceRefs(List.of(" CODE:RANGE_EXCLUDES_N "))
                .confidence(0.82)
                .uncertainty("已根据可见证据判断。")
                .needsMoreEvidence(false)
                .answerLeakRisk(" low ")
                .build();

        ExternalModelStagePayloads.DiagnosisJudgeOutput normalized =
                normalizer.normalizeDiagnosisDecision(output, fixture.runtimePlan());

        assertThat(normalized.getPrimaryIssueTag()).isEqualTo("LOOP_BOUNDARY");
        assertThat(normalized.getFineGrainedTag()).isEqualTo("OFF_BY_ONE");
        assertThat(normalized.getEvidenceRefs()).containsExactly("code:range_excludes_n");
        assertThat(normalized.getAnswerLeakRisk()).isEqualTo("LOW");
        assertThat(validator.validateDiagnosisJudgeOutput(normalized, fixture.brief(), fixture.pack()).isValid()).isTrue();
    }

    @Test
    void leavesUnknownTagsAndEvidenceRefsForStrictValidation() {
        Fixture fixture = fixture();
        ExternalModelStagePayloads.DiagnosisJudgeOutput output = ExternalModelStagePayloads.DiagnosisJudgeOutput.builder()
                .primaryIssueTag("大概是循环问题")
                .fineGrainedTag("OFF_BY_ONE")
                .evidenceRefs(List.of("invented:evidence"))
                .answerLeakRisk("LOW")
                .build();

        ExternalModelStagePayloads.DiagnosisJudgeOutput normalized =
                normalizer.normalizeDiagnosisDecision(output, fixture.runtimePlan());

        assertThat(normalized.getPrimaryIssueTag()).isEqualTo("大概是循环问题");
        assertThat(normalized.getEvidenceRefs()).containsExactly("invented:evidence");
        assertThat(validator.validateDiagnosisJudgeOutput(normalized, fixture.brief(), fixture.pack()).isValid()).isFalse();
    }

    @Test
    void normalizesEducationAgentJudgmentFields() {
        Fixture fixture = fixture();
        ExternalModelStagePayloads.DiagnosisJudgeOutput output = ExternalModelStagePayloads.DiagnosisJudgeOutput.builder()
                .primaryIssueTag("循环边界")
                .fineGrainedTag("差 一 位 错误")
                .evidenceRefs(List.of(" CODE:RANGE_EXCLUDES_N "))
                .primaryReasoning("先看循环边界。")
                .secondaryIssues(List.of(ExternalModelStagePayloads.EducationIssueNote.builder()
                        .title("次要信号")
                        .message("自测习惯可后续处理。")
                        .issueTag("循环边界")
                        .fineGrainedTag("差 一 位 错误")
                        .evidenceRefs(List.of(" CODE:RANGE_EXCLUDES_N "))
                        .build()))
                .improvementOpportunities(List.of(SubmissionAnalysisResponse.ImprovementOpportunity.builder()
                        .category(" testing_habit ")
                        .studentMessage("补一个最小样例。")
                        .benefit("发现边界遗漏。")
                        .evidenceRefs(List.of(" CODE:RANGE_EXCLUDES_N "))
                        .build()))
                .nextLearningAction(SubmissionAnalysisResponse.NextLearningAction.builder()
                        .task("列出 range 产生的 i。")
                        .checkQuestion("n=1 时循环几次？")
                        .evidenceRefs(List.of(" CODE:RANGE_EXCLUDES_N "))
                        .answerLeakRisk(" low ")
                        .build())
                .answerLeakRisk("low")
                .build();

        ExternalModelStagePayloads.DiagnosisJudgeOutput normalized =
                normalizer.normalizeDiagnosisDecision(output, fixture.runtimePlan());

        assertThat(normalized.getSecondaryIssues()).singleElement()
                .satisfies(note -> {
                    assertThat(note.getIssueTag()).isEqualTo("LOOP_BOUNDARY");
                    assertThat(note.getFineGrainedTag()).isEqualTo("OFF_BY_ONE");
                    assertThat(note.getEvidenceRefs()).containsExactly("code:range_excludes_n");
                });
        assertThat(normalized.getImprovementOpportunities()).singleElement()
                .satisfies(item -> {
                    assertThat(item.getCategory()).isEqualTo("TESTING_HABIT");
                    assertThat(item.getEvidenceRefs()).containsExactly("code:range_excludes_n");
                });
        assertThat(normalized.getNextLearningAction().getEvidenceRefs()).containsExactly("code:range_excludes_n");
        assertThat(normalized.getNextLearningAction().getAnswerLeakRisk()).isEqualTo("LOW");
        assertThat(validator.validateDiagnosisJudgeOutput(normalized, fixture.brief(), fixture.pack()).isValid()).isTrue();
    }

    @Test
    void calibratesModelReportedHighRiskWhenVisibleTextIsSafe() {
        Fixture fixture = fixture();
        ExternalModelStagePayloads.DiagnosisJudgeOutput output = ExternalModelStagePayloads.DiagnosisJudgeOutput.builder()
                .primaryIssueTag("LOOP_BOUNDARY")
                .fineGrainedTag("OFF_BY_ONE")
                .evidenceRefs(List.of("code:range_excludes_n"))
                .primaryReasoning("第一个失败证据显示循环边界需要先手推确认。")
                .teachingPriority("先列出最小样例里的循环取值，再和题目要求对齐。")
                .nextLearningAction(SubmissionAnalysisResponse.NextLearningAction.builder()
                        .task("列出最小样例里循环实际经过的值。")
                        .checkQuestion("第一次和期望不一致的位置在哪里？")
                        .evidenceRefs(List.of("code:range_excludes_n"))
                        .answerLeakRisk("HIGH")
                        .build())
                .answerLeakRisk("HIGH")
                .build();

        ExternalModelStagePayloads.DiagnosisJudgeOutput normalized =
                normalizer.normalizeDiagnosisDecision(output, fixture.runtimePlan());

        assertThat(normalized.getAnswerLeakRisk()).isEqualTo("MEDIUM");
        assertThat(normalized.getNextLearningAction().getAnswerLeakRisk()).isEqualTo("MEDIUM");
        assertThat(validator.validateDiagnosisJudgeOutput(normalized, fixture.brief(), fixture.pack()).isValid()).isTrue();
    }

    @Test
    void normalizesTeachingActionAndEvidenceButDoesNotHideSafetyRisk() {
        Fixture fixture = fixture();
        ExternalModelStagePayloads.TeachingHintOutput output = ExternalModelStagePayloads.TeachingHintOutput.builder()
                .studentHint("完整代码如下：def solve(): pass")
                .studentHintPlan(SubmissionAnalysisResponse.StudentHintPlan.builder()
                        .teachingAction(" trace_variables ")
                        .nextAction("复制完整代码")
                        .coachQuestion("无")
                        .evidenceRefs(List.of(" CODE:RANGE_EXCLUDES_N "))
                        .answerLeakRisk(" high ")
                        .build())
                .learningInterventionPlan(SubmissionAnalysisResponse.LearningInterventionPlan.builder()
                        .studentTask("复制答案")
                        .evidenceRefs(List.of(" CODE:RANGE_EXCLUDES_N "))
                        .answerLeakRisk(" high ")
                        .build())
                .answerLeakRisk(" high ")
                .build();

        ExternalModelStagePayloads.TeachingHintOutput normalized =
                normalizer.normalizeTeachingHint(output, fixture.runtimePlan());

        assertThat(normalized.getStudentHintPlan().getTeachingAction()).isEqualTo("TRACE_VARIABLES");
        assertThat(normalized.getStudentHintPlan().getEvidenceRefs()).containsExactly("code:range_excludes_n");
        assertThat(normalized.getLearningInterventionPlan().getEvidenceRefs()).containsExactly("code:range_excludes_n");
        assertThat(normalized.getAnswerLeakRisk()).isEqualTo("HIGH");
        assertThat(validator.validateTeachingHintOutput(
                normalized,
                validDecision(),
                fixture.brief(),
                fixture.pack()
        ).getFailureReason()).isEqualTo(ModelStageFailureReason.SAFETY_RISK);
    }

    private ExternalModelStagePayloads.DiagnosisJudgeOutput validDecision() {
        return ExternalModelStagePayloads.DiagnosisJudgeOutput.builder()
                .primaryIssueTag("LOOP_BOUNDARY")
                .fineGrainedTag("OFF_BY_ONE")
                .evidenceRefs(List.of("code:range_excludes_n"))
                .answerLeakRisk("LOW")
                .build();
    }

    private Fixture fixture() {
        DiagnosisEvidencePackage evidencePackage = DiagnosisEvidencePackage.builder()
                .problem(DiagnosisEvidencePackage.ProblemEvidence.builder()
                        .title("Sum 1 to n")
                        .description("Input n and output the sum from 1 to n.")
                        .build())
                .submission(DiagnosisEvidencePackage.SubmissionEvidence.builder()
                        .language("Python 3")
                        .verdict("WRONG_ANSWER")
                        .sourceCodeWithLineNumbers("1: for i in range(1, n):")
                        .sourceCodeLineCount(1)
                        .build())
                .build();
        RuleSignalAnalyzer.RuleSignalResult ruleSignals = RuleSignalAnalyzer.RuleSignalResult.builder()
                .candidateIssueTags(List.of("LOOP_BOUNDARY"))
                .candidateFineGrainedTags(List.of("OFF_BY_ONE"))
                .evidenceRefs(List.of("code:range_excludes_n"))
                .signals(List.of(RuleSignalAnalyzer.Signal.builder()
                        .evidenceRef("code:range_excludes_n")
                        .coarseTag("LOOP_BOUNDARY")
                        .fineTag("OFF_BY_ONE")
                        .confidence(0.9)
                        .message("range excludes n")
                        .build()))
                .build();
        ModelDiagnosisBrief brief = new ModelDiagnosisBriefBuilder().build(evidencePackage, ruleSignals, null);
        StandardLibraryPack pack = new StandardLibraryPackBuilder(new DiagnosisTaxonomy()).build(brief, ruleSignals);
        return new Fixture(ExternalModelAgentRuntime.RuntimePlan.builder()
                .brief(brief)
                .standardLibraryPack(pack)
                .build(), brief, pack);
    }

    private record Fixture(ExternalModelAgentRuntime.RuntimePlan runtimePlan,
                           ModelDiagnosisBrief brief,
                           StandardLibraryPack pack) {
    }
}
