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
