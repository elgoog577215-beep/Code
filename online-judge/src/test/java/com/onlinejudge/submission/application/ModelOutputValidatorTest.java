package com.onlinejudge.submission.application;

import com.onlinejudge.learning.diagnosis.DiagnosisTaxonomy;
import com.onlinejudge.submission.dto.SubmissionAnalysisResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ModelOutputValidatorTest {

    private final ModelOutputValidator validator = new ModelOutputValidator();

    @Test
    void acceptsValidStudentFeedback() {
        Fixture fixture = fixture();

        ExternalModelStagePayloads.StageValidationResult result = validator.validateStudentFeedback(
                validStudentFeedback("TESTING_HABIT", "列出 range 产生的 i。"),
                fixture.brief(),
                fixture.pack()
        );

        assertThat(result.isValid()).isTrue();
        assertThat(result.getFailureReason()).isEqualTo(ModelStageFailureReason.NONE);
    }

    @Test
    void rejectsInvalidStudentFeedbackImprovementCategory() {
        Fixture fixture = fixture();

        ExternalModelStagePayloads.StageValidationResult result = validator.validateStudentFeedback(
                validStudentFeedback("MADE_UP_IMPROVEMENT", "列出 range 产生的 i。"),
                fixture.brief(),
                fixture.pack()
        );

        assertThat(result.isValid()).isFalse();
        assertThat(result.getFailureReason()).isEqualTo(ModelStageFailureReason.INVALID_TAG);
    }

    @Test
    void rejectsUnsafeStudentFeedbackEvenWhenRiskIsMarkedLow() {
        Fixture fixture = fixture();

        ExternalModelStagePayloads.StageValidationResult result = validator.validateStudentFeedback(
                validStudentFeedback("TESTING_HABIT", "直接改成 range(1, n + 1)。"),
                fixture.brief(),
                fixture.pack()
        );

        assertThat(result.isValid()).isFalse();
        assertThat(result.getFailureReason()).isEqualTo(ModelStageFailureReason.SAFETY_RISK);
    }

    @Test
    void runtimePlanCarriesBriefLibraryAndNewPromptVersions() {
        Fixture fixture = fixture();
        ExternalModelAgentRuntime runtime = new ExternalModelAgentRuntime(
                new ModelDiagnosisBriefBuilder(),
                new StandardLibraryPackBuilder(new DiagnosisTaxonomy()),
                new PromptTemplateRegistry(),
                validator
        );

        ExternalModelAgentRuntime.RuntimePlan plan = runtime.prepare(
                fixture.evidencePackage(),
                fixture.ruleSignals(),
                null
        );

        assertThat(plan.getBrief().getSchemaVersion()).isEqualTo(ModelDiagnosisBrief.SCHEMA_VERSION);
        assertThat(plan.getStandardLibraryPack().getSchemaVersion()).isEqualTo(StandardLibraryPack.SCHEMA_VERSION);
        assertThat(plan.getSearchLocationPrompt().getVersion()).isEqualTo(PromptTemplateRegistry.SEARCH_LOCATION_V1);
        assertThat(plan.getAdvicePrompt().getVersion()).isEqualTo(PromptTemplateRegistry.DIAGNOSIS_REPORT_V2);
        assertThat(plan.getRuntimeProfile()).isEqualTo("standard");
        assertThat(plan.isRequestCompact()).isFalse();
    }

    @Test
    void autoRuntimeProfileCompactsLargeSubmissionsOnly() {
        ExternalModelAgentRuntime runtime = new ExternalModelAgentRuntime(
                new ModelDiagnosisBriefBuilder(),
                new StandardLibraryPackBuilder(new DiagnosisTaxonomy()),
                new PromptTemplateRegistry(),
                validator
        );

        ExternalModelAgentRuntime.RuntimePlan smallPlan = runtime.prepare(
                fixture().evidencePackage(),
                fixture().ruleSignals(),
                null,
                ExternalModelAgentRuntime.RUNTIME_PROFILE_AUTO
        );
        ExternalModelAgentRuntime.RuntimePlan largePlan = runtime.prepare(
                largeFixture().evidencePackage(),
                largeFixture().ruleSignals(),
                null,
                ExternalModelAgentRuntime.RUNTIME_PROFILE_AUTO
        );

        assertThat(smallPlan.getRuntimeProfile()).isEqualTo("auto");
        assertThat(smallPlan.isRequestCompact()).isFalse();
        assertThat(largePlan.getRuntimeProfile()).isEqualTo("auto");
        assertThat(largePlan.isRequestCompact()).isTrue();
        assertThat(largePlan.getBrief().getCandidateSignals()).isEmpty();
        assertThat(largePlan.getBrief().getKeyCodeExcerpt()).contains("truncated for model");
    }

    @Test
    void lowLatencyCompactsSelectedPackAfterLocalRecall() {
        ExternalModelAgentRuntime runtime = new ExternalModelAgentRuntime(
                new ModelDiagnosisBriefBuilder(),
                new StandardLibraryPackBuilder(new DiagnosisTaxonomy()),
                new PromptTemplateRegistry(),
                validator
        );
        ExternalModelAgentRuntime.RuntimePlan plan = ExternalModelAgentRuntime.RuntimePlan.builder()
                .requestCompact(true)
                .build();
        StandardLibraryPack selectedPack = StandardLibraryPack.builder()
                .schemaVersion(StandardLibraryPack.SCHEMA_VERSION)
                .taxonomyVersion(DiagnosisTaxonomy.TAXONOMY_VERSION)
                .basicCauses(List.of(StandardLibraryPack.BasicCauseOption.builder()
                        .id("MP_LONG_CONTEXT")
                        .category("循环")
                        .name("长上下文测试")
                        .description("这是一段会被压缩的描述。".repeat(20))
                        .studentExplanation("学生解释。".repeat(20))
                        .teacherExplanation("教师解释不应进入低延迟模型上下文。")
                        .evidenceSignals(List.of("a", "b", "c", "d"))
                        .build()))
                .build();

        StandardLibraryPack compacted = runtime.compactSelectedPack(selectedPack, plan);

        StandardLibraryPack.BasicCauseOption cause = compacted.getBasicCauses().get(0);
        assertThat(cause.getDescription()).contains("truncated for model");
        assertThat(cause.getTeacherExplanation()).isNull();
        assertThat(cause.getEvidenceSignals()).containsExactly("a", "b", "c");
    }

    private SubmissionAnalysisResponse.StudentFeedback validStudentFeedback(String improvementCategory,
                                                                            String nextAction) {
        return SubmissionAnalysisResponse.StudentFeedback.builder()
                .summary("这次主要问题是循环边界没有覆盖题目要求。")
                .blockingIssues(List.of(SubmissionAnalysisResponse.FeedbackIssue.builder()
                        .priority(1)
                        .title("当前最需要先处理的问题")
                        .studentMessage("循环边界和题目要求不一致，先手推最小样例。")
                        .evidence("judge:first_failed_case")
                        .nextAction(nextAction)
                        .issueTag("SK_LOOP_BOUNDARY")
                        .fineGrainedTag("MP_RANGE_RIGHT_ENDPOINT")
                        .evidenceRefs(List.of("judge:first_failed_case"))
                        .build()))
                .secondaryIssues(List.of())
                .improvementOpportunities(List.of(SubmissionAnalysisResponse.ImprovementOpportunity.builder()
                        .category(improvementCategory)
                        .studentMessage("通过后补一个最小边界自测。")
                        .benefit("能提前发现边界遗漏。")
                        .evidenceRefs(List.of("judge:first_failed_case"))
                        .build()))
                .nextLearningAction(SubmissionAnalysisResponse.NextLearningAction.builder()
                        .hintLevel("L2")
                        .action("TRACE_VARIABLES")
                        .task(nextAction)
                        .checkQuestion("当 n=1 时循环执行几次？")
                        .evidenceRefs(List.of("judge:first_failed_case"))
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
                .judgeFacts(DiagnosisEvidencePackage.JudgeFacts.builder()
                        .firstFailedCase(SubmissionAnalysisResponse.FailedCaseSnapshot.builder()
                                .testCaseNumber(1)
                                .hidden(false)
                                .input("3")
                                .expectedOutput("6")
                                .actualOutput("3")
                                .build())
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
        return new Fixture(evidencePackage, ruleSignals, brief, pack);
    }

    private Fixture largeFixture() {
        String source = """
                n = int(input())
                total = 0
                for i in range(1, n):
                    total += i
                print(total)
                """.repeat(80);
        DiagnosisEvidencePackage evidencePackage = DiagnosisEvidencePackage.builder()
                .problem(DiagnosisEvidencePackage.ProblemEvidence.builder()
                        .title("Sum 1 to n")
                        .description("Input n and output the sum from 1 to n.".repeat(50))
                        .build())
                .submission(DiagnosisEvidencePackage.SubmissionEvidence.builder()
                        .language("Python 3")
                        .verdict("WRONG_ANSWER")
                        .sourceCode(source)
                        .sourceCodeWithLineNumbers(source)
                        .sourceCodeLineCount(80 * 5)
                        .build())
                .judgeFacts(DiagnosisEvidencePackage.JudgeFacts.builder()
                        .firstFailedCase(SubmissionAnalysisResponse.FailedCaseSnapshot.builder()
                                .testCaseNumber(1)
                                .hidden(false)
                                .input("3")
                                .expectedOutput("6")
                                .actualOutput("3")
                                .build())
                        .build())
                .build();
        RuleSignalAnalyzer.RuleSignalResult ruleSignals = RuleSignalAnalyzer.RuleSignalResult.builder()
                .candidateIssueTags(List.of("LOOP_BOUNDARY"))
                .candidateFineGrainedTags(List.of("OFF_BY_ONE"))
                .evidenceRefs(List.of("code:range_excludes_n"))
                .signals(List.of(
                        signal("code:range_excludes_n", 0.9),
                        signal("case:first_failed", 0.8),
                        signal("code:loop_header", 0.7),
                        signal("code:sum_delta", 0.6),
                        signal("memory:repeat", 0.5)
                ))
                .build();
        ModelDiagnosisBrief brief = new ModelDiagnosisBriefBuilder().build(evidencePackage, ruleSignals, null);
        StandardLibraryPack pack = new StandardLibraryPackBuilder(new DiagnosisTaxonomy()).build(brief, ruleSignals);
        return new Fixture(evidencePackage, ruleSignals, brief, pack);
    }

    private RuleSignalAnalyzer.Signal signal(String ref, double confidence) {
        return RuleSignalAnalyzer.Signal.builder()
                .evidenceRef(ref)
                .coarseTag("LOOP_BOUNDARY")
                .fineTag("OFF_BY_ONE")
                .confidence(confidence)
                .message(ref)
                .build();
    }

    private record Fixture(DiagnosisEvidencePackage evidencePackage,
                           RuleSignalAnalyzer.RuleSignalResult ruleSignals,
                           ModelDiagnosisBrief brief,
                           StandardLibraryPack pack) {
    }
}
