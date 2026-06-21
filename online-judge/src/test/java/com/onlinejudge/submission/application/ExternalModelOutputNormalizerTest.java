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
    void normalizesStudentFeedbackEvidenceAndImprovementCategory() {
        Fixture fixture = fixture();
        SubmissionAnalysisResponse.StudentFeedback feedback = validFeedback(" testing_habit ", " CODE:RANGE_EXCLUDES_N ");

        SubmissionAnalysisResponse.StudentFeedback normalized =
                normalizer.normalizeStudentFeedback(feedback, fixture.runtimePlan());

        assertThat(normalized.getBlockingIssues()).singleElement()
                .satisfies(issue -> assertThat(issue.getEvidenceRefs()).containsExactly("code:range_excludes_n"));
        assertThat(normalized.getImprovementOpportunities()).singleElement()
                .satisfies(item -> {
                    assertThat(item.getCategory()).isEqualTo("TESTING_HABIT");
                    assertThat(item.getEvidenceRefs()).containsExactly("code:range_excludes_n");
                });
        assertThat(normalized.getNextLearningAction().getEvidenceRefs()).containsExactly("code:range_excludes_n");
        assertThat(validator.validateStudentFeedback(normalized, fixture.brief(), fixture.pack()).isValid()).isTrue();
    }

    @Test
    void leavesUnknownCategoryForStrictValidation() {
        Fixture fixture = fixture();
        SubmissionAnalysisResponse.StudentFeedback feedback = validFeedback("made_up", "code:range_excludes_n");

        SubmissionAnalysisResponse.StudentFeedback normalized =
                normalizer.normalizeStudentFeedback(feedback, fixture.runtimePlan());

        assertThat(normalized.getImprovementOpportunities()).singleElement()
                .satisfies(item -> assertThat(item.getCategory()).isEqualTo("made_up"));
        assertThat(validator.validateStudentFeedback(normalized, fixture.brief(), fixture.pack()).isValid()).isFalse();
    }

    @Test
    void calibratesModelReportedHighRiskWhenVisibleTextIsSafe() {
        Fixture fixture = fixture();
        SubmissionAnalysisResponse.StudentFeedback feedback = validFeedback("TESTING_HABIT", "code:range_excludes_n");
        feedback.getNextLearningAction().setAnswerLeakRisk("HIGH");

        SubmissionAnalysisResponse.StudentFeedback normalized =
                normalizer.normalizeStudentFeedback(feedback, fixture.runtimePlan());

        assertThat(normalized.getNextLearningAction().getAnswerLeakRisk()).isEqualTo("MEDIUM");
        assertThat(validator.validateStudentFeedback(normalized, fixture.brief(), fixture.pack()).isValid()).isTrue();
    }

    @Test
    void keepsUnsafeFeedbackVisibleForValidator() {
        Fixture fixture = fixture();
        SubmissionAnalysisResponse.StudentFeedback feedback = validFeedback("TESTING_HABIT", "code:range_excludes_n");
        feedback.getBlockingIssues().get(0).setNextAction("直接改成 range(1, n + 1)。");

        SubmissionAnalysisResponse.StudentFeedback normalized =
                normalizer.normalizeStudentFeedback(feedback, fixture.runtimePlan());

        assertThat(validator.validateStudentFeedback(normalized, fixture.brief(), fixture.pack()).getFailureReason())
                .isEqualTo(ModelStageFailureReason.SAFETY_RISK);
    }

    private SubmissionAnalysisResponse.StudentFeedback validFeedback(String category, String evidenceRef) {
        return SubmissionAnalysisResponse.StudentFeedback.builder()
                .summary("这次主要问题是循环边界没有覆盖题目要求。")
                .blockingIssues(List.of(SubmissionAnalysisResponse.FeedbackIssue.builder()
                        .priority(1)
                        .title("循环边界")
                        .studentMessage("循环范围和题目要求不一致。")
                        .evidence(evidenceRef)
                        .nextAction("列出 range 产生的 i。")
                        .issueTag("SK_LOOP_BOUNDARY")
                        .fineGrainedTag("MP_RANGE_RIGHT_ENDPOINT")
                        .evidenceRefs(List.of(evidenceRef))
                        .build()))
                .secondaryIssues(List.of())
                .improvementOpportunities(List.of(SubmissionAnalysisResponse.ImprovementOpportunity.builder()
                        .category(category)
                        .studentMessage("通过后补一个最小边界自测。")
                        .benefit("能提前发现边界遗漏。")
                        .evidenceRefs(List.of(evidenceRef))
                        .build()))
                .nextLearningAction(SubmissionAnalysisResponse.NextLearningAction.builder()
                        .hintLevel("L2")
                        .action("TRACE_VARIABLES")
                        .task("列出 range 产生的 i。")
                        .checkQuestion("当 n=1 时循环执行几次？")
                        .evidenceRefs(List.of(evidenceRef))
                        .answerLeakRisk("LOW")
                        .build())
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
