package com.onlinejudge.submission.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlinejudge.submission.dto.SubmissionAnalysisResponse;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ComplexDiagnosisQualityScorerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ComplexDiagnosisQualityScorer scorer = new ComplexDiagnosisQualityScorer();

    @Test
    void scoresAllComplexMetricsWhenAnalysisMatchesFixtureTruth() throws IOException {
        ComplexStudentSubmissionEvalFixtureLoader.Fixture fixture = firstComplexFixture();
        SubmissionAnalysisResponse analysis = alignedAnalysis(fixture);

        ComplexDiagnosisQualityScorer.Result result = scorer.score(fixture, analysis);

        assertThat(result.complexCase()).isTrue();
        assertThat(result.passed()).isTrue();
        assertThat(result.passedMetricCount()).isEqualTo(6);
        assertThat(result.totalMetricCount()).isEqualTo(6);
        assertThat(result.failedMetrics()).isEmpty();
        assertThat(result.passedMetricSignals()).containsExactly(
                "complexMetric:primaryRootCauseHit",
                "complexMetric:teachingPriorityCorrect",
                "complexMetric:secondaryIssuesNotOverweighted",
                "complexMetric:distractingSignalsIgnored",
                "complexMetric:evidenceGrounded",
                "complexMetric:noFullSolutionLeak"
        );

        ComplexDiagnosisQualityScorer.IntelligenceResult intelligence = scorer.intelligence(result, true, false);
        ComplexDiagnosisQualityScorer.StudentFeedbackResult studentFeedback = scorer.studentFeedback(fixture, analysis, true, false);

        assertThat(intelligence.evaluated()).isTrue();
        assertThat(intelligence.passed()).isTrue();
        assertThat(intelligence.passedMetricSignals()).containsExactly(
                "intelligenceMetric:autonomousRootCauseDiscovery",
                "intelligenceMetric:teachingDecisionQuality",
                "intelligenceMetric:complexSignalPrioritization",
                "intelligenceMetric:distractorResistance",
                "intelligenceMetric:evidenceGroundedReasoning",
                "intelligenceMetric:modelSafetyAndBoundary"
        );
        assertThat(studentFeedback.evaluated()).isTrue();
        assertThat(studentFeedback.passed()).isTrue();
        assertThat(studentFeedback.passedMetricSignals()).contains("studentFeedbackMetric:blockingPrimaryHit",
                "studentFeedbackMetric:improvementOpportunityUseful",
                "studentFeedbackMetric:studentActionable");
    }

    @Test
    void intelligenceScoreExcludesFallbackEvenWhenLocalComplexMetricsWouldPass() throws Exception {
        var fixture = firstComplexFixture();
        SubmissionAnalysisResponse analysis = alignedAnalysis(fixture);
        ComplexDiagnosisQualityScorer.Result complex = scorer.score(fixture, analysis);

        ComplexDiagnosisQualityScorer.IntelligenceResult intelligence = scorer.intelligence(complex, false, true);

        assertThat(complex.passed()).isTrue();
        assertThat(intelligence.complexCase()).isTrue();
        assertThat(intelligence.evaluated()).isFalse();
        assertThat(intelligence.passed()).isFalse();
        assertThat(intelligence.passedMetricCount()).isZero();
        assertThat(intelligence.totalMetricCount()).isZero();
        assertThat(intelligence.passedMetricSignals()).isEmpty();
    }

    @Test
    void reportsFailedMetricsWhenAnalysisOverweightsSecondarySignalsAndLeaksAnswer() throws IOException {
        ComplexStudentSubmissionEvalFixtureLoader.Fixture fixture = firstComplexFixture();
        String secondaryFineTag = "OFF_BY_ONE".equals(fixture.primaryRootCause().fineGrainedTag())
                ? "INPUT_PARSING"
                : "OFF_BY_ONE";
        SubmissionAnalysisResponse analysis = SubmissionAnalysisResponse.builder()
                .issueTags(List.of("BOUNDARY_CONDITION"))
                .fineGrainedTags(List.of(secondaryFineTag, fixture.primaryRootCause().fineGrainedTag()))
                .evidenceRefs(List.of("invented:evidence"))
                .headline("把次要边界问题放在最前面")
                .summary(fixture.distractingSignals().get(0))
                .studentHint("完整代码如下：def solve(): pass")
                .answerLeakRisk("HIGH")
                .build();

        ComplexDiagnosisQualityScorer.Result result = scorer.score(fixture, analysis);

        assertThat(result.passed()).isFalse();
        assertThat(result.failedMetrics()).contains(
                "teachingPriorityCorrect",
                "secondaryIssuesNotOverweighted",
                "distractingSignalsIgnored",
                "evidenceGrounded",
                "noFullSolutionLeak"
        );
        assertThat(result.metrics()).containsEntry("primaryRootCauseHit", true);
        assertThat(result.score()).isBetween(0.0, 1.0);
    }

    private ComplexStudentSubmissionEvalFixtureLoader.Fixture firstComplexFixture() throws IOException {
        return new ComplexStudentSubmissionEvalFixtureLoader(objectMapper).loadDefault().get(0);
    }

    private SubmissionAnalysisResponse alignedAnalysis(ComplexStudentSubmissionEvalFixtureLoader.Fixture fixture) {
        return SubmissionAnalysisResponse.builder()
                .issueTags(List.of(fixture.primaryRootCause().issueTag()))
                .fineGrainedTags(List.of(fixture.primaryRootCause().fineGrainedTag()))
                .evidenceRefs(fixture.requiredEvidenceRefs())
                .headline(fixture.primaryRootCause().fineGrainedTag() + " 是首要问题")
                .summary("先围绕 " + fixture.mustMention().get(0) + " 做最小证据核对。")
                .studentHint("先检查 " + fixture.mustMention().get(1) + "，先不要急着改程序。")
                .studentFeedback(SubmissionAnalysisResponse.StudentFeedback.builder()
                        .summary("这次主要问题是 " + fixture.primaryRootCause().fineGrainedTag())
                        .blockingIssues(List.of(SubmissionAnalysisResponse.FeedbackIssue.builder()
                                .priority(1)
                                .title("当前最需要先处理的问题")
                                .studentMessage("先核对 " + fixture.mustMention().get(0) + " 和 " + fixture.mustMention().get(1))
                                .evidence("first failed case")
                                .nextAction("数一数代码实际处理了几组输入。")
                                .issueTag(fixture.primaryRootCause().issueTag())
                                .fineGrainedTag(fixture.primaryRootCause().fineGrainedTag())
                                .evidenceRefs(fixture.requiredEvidenceRefs())
                                .build()))
                        .secondaryIssues(List.of(SubmissionAnalysisResponse.SecondaryIssue.builder()
                                .title("可能的次要问题")
                                .studentMessage("次要信号先放到后面观察。")
                                .whyNotPrimary("它不是 first failed case 的主因。")
                                .issueTag("BOUNDARY_CONDITION")
                                .evidenceRefs(fixture.requiredEvidenceRefs())
                                .build()))
                        .improvementOpportunities(List.of(SubmissionAnalysisResponse.ImprovementOpportunity.builder()
                                .category("TESTING_HABIT")
                                .studentMessage("通过后补一个不同于样例的小自测。")
                                .benefit("能确认修复不只覆盖眼前数据。")
                                .evidenceRefs(fixture.requiredEvidenceRefs())
                                .build()))
                        .nextLearningAction(SubmissionAnalysisResponse.NextLearningAction.builder()
                                .hintLevel("L2")
                                .action("COMPARE_INPUT_SPEC")
                                .task("数一数代码实际处理了几组输入。")
                                .checkQuestion("题面要求几组，代码处理几组？")
                                .evidenceRefs(fixture.requiredEvidenceRefs())
                                .answerLeakRisk("LOW")
                                .build())
                        .build())
                .answerLeakRisk("LOW")
                .build();
    }
}
