package com.onlinejudge.submission.application;

import com.onlinejudge.learning.diagnosis.DiagnosisTaxonomy;
import com.onlinejudge.submission.dto.SubmissionAnalysisResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ModelOutputValidatorTest {

    private final ModelOutputValidator validator = new ModelOutputValidator();

    @Test
    void acceptsValidDiagnosisJudgeOutput() {
        Fixture fixture = fixture();

        ExternalModelStagePayloads.StageValidationResult result = validator.validateDiagnosisJudgeOutput(
                ExternalModelStagePayloads.DiagnosisJudgeOutput.builder()
                        .primaryIssueTag("LOOP_BOUNDARY")
                        .fineGrainedTag("OFF_BY_ONE")
                        .evidenceRefs(List.of("code:range_excludes_n"))
                        .confidence(0.88)
                        .uncertainty("Evidence points to loop boundary.")
                        .needsMoreEvidence(false)
                        .answerLeakRisk("LOW")
                        .build(),
                fixture.brief(),
                fixture.pack()
        );

        assertThat(result.isValid()).isTrue();
        assertThat(result.getFailureReason()).isEqualTo(ModelStageFailureReason.NONE);
    }

    @Test
    void rejectsInvalidDiagnosisTag() {
        Fixture fixture = fixture();

        ExternalModelStagePayloads.StageValidationResult result = validator.validateDiagnosisJudgeOutput(
                ExternalModelStagePayloads.DiagnosisJudgeOutput.builder()
                        .primaryIssueTag("MADE_UP_TAG")
                        .fineGrainedTag("OFF_BY_ONE")
                        .evidenceRefs(List.of("code:range_excludes_n"))
                        .answerLeakRisk("LOW")
                        .build(),
                fixture.brief(),
                fixture.pack()
        );

        assertThat(result.isValid()).isFalse();
        assertThat(result.getFailureReason()).isEqualTo(ModelStageFailureReason.INVALID_TAG);
    }

    @Test
    void rejectsInvalidEvidenceRef() {
        Fixture fixture = fixture();

        ExternalModelStagePayloads.StageValidationResult result = validator.validateDiagnosisJudgeOutput(
                ExternalModelStagePayloads.DiagnosisJudgeOutput.builder()
                        .primaryIssueTag("LOOP_BOUNDARY")
                        .fineGrainedTag("OFF_BY_ONE")
                        .evidenceRefs(List.of("invented:evidence"))
                        .answerLeakRisk("LOW")
                        .build(),
                fixture.brief(),
                fixture.pack()
        );

        assertThat(result.isValid()).isFalse();
        assertThat(result.getFailureReason()).isEqualTo(ModelStageFailureReason.INVALID_EVIDENCE_REF);
    }

    @Test
    void acceptsEducationAgentJudgmentWhenEvidenceAndSafetyAreValid() {
        Fixture fixture = fixture();

        ExternalModelStagePayloads.StageValidationResult result = validator.validateDiagnosisJudgeOutput(
                validDecision().toBuilder()
                        .primaryReasoning("第一个失败证据显示循环没有覆盖题目要求的末端。")
                        .secondaryIssues(List.of(ExternalModelStagePayloads.EducationIssueNote.builder()
                                .title("自测习惯")
                                .message("可以补最小样例，但它不是当前第一失败的主因。")
                                .issueTag("LOOP_BOUNDARY")
                                .fineGrainedTag("OFF_BY_ONE")
                                .evidenceRefs(List.of("code:range_excludes_n"))
                                .build()))
                        .teachingPriority("先验证循环边界是否覆盖闭区间。")
                        .improvementOpportunities(List.of(SubmissionAnalysisResponse.ImprovementOpportunity.builder()
                                .category("TESTING_HABIT")
                                .studentMessage("通过后补一个最小边界自测。")
                                .benefit("能提前发现边界遗漏。")
                                .evidenceRefs(List.of("code:range_excludes_n"))
                                .build()))
                        .nextLearningAction(SubmissionAnalysisResponse.NextLearningAction.builder()
                                .hintLevel("L2")
                                .action("TRACE_VARIABLES")
                                .task("列出 range 产生的 i。")
                                .checkQuestion("当 n=1 时循环执行几次？")
                                .evidenceRefs(List.of("code:range_excludes_n"))
                                .answerLeakRisk("LOW")
                                .build())
                        .build(),
                fixture.brief(),
                fixture.pack()
        );

        assertThat(result.isValid()).isTrue();
        assertThat(result.getFailureReason()).isEqualTo(ModelStageFailureReason.NONE);
    }

    @Test
    void rejectsUnsafeEducationAgentJudgment() {
        Fixture fixture = fixture();

        ExternalModelStagePayloads.StageValidationResult result = validator.validateDiagnosisJudgeOutput(
                validDecision().toBuilder()
                        .primaryReasoning("直接改成 range(1, n + 1) 就能过。")
                        .nextLearningAction(SubmissionAnalysisResponse.NextLearningAction.builder()
                                .task("复制完整代码")
                                .evidenceRefs(List.of("code:range_excludes_n"))
                                .answerLeakRisk("LOW")
                                .build())
                        .build(),
                fixture.brief(),
                fixture.pack()
        );

        assertThat(result.isValid()).isFalse();
        assertThat(result.getFailureReason()).isEqualTo(ModelStageFailureReason.SAFETY_RISK);
    }

    @Test
    void reportsInvalidEducationEvidenceRefWithFieldPath() {
        Fixture fixture = fixture();

        ExternalModelStagePayloads.StageValidationResult result = validator.validateDiagnosisJudgeOutput(
                validDecision().toBuilder()
                        .secondaryIssues(List.of(ExternalModelStagePayloads.EducationIssueNote.builder()
                                .title("次要信号")
                                .message("这是次要问题，不是主因。")
                                .issueTag("LOOP_BOUNDARY")
                                .fineGrainedTag("OFF_BY_ONE")
                                .evidenceRefs(List.of("invented:secondary_ref"))
                                .build()))
                        .build(),
                fixture.brief(),
                fixture.pack()
        );

        assertThat(result.isValid()).isFalse();
        assertThat(result.getFailureReason()).isEqualTo(ModelStageFailureReason.INVALID_EVIDENCE_REF);
        assertThat(result.getMessage())
                .contains("diagnosisDecision.secondaryIssues[0].evidenceRefs")
                .contains("invented:secondary_ref");
    }

    @Test
    void rejectsUnsafeTeachingHintOutput() {
        Fixture fixture = fixture();

        ExternalModelStagePayloads.StageValidationResult result = validator.validateTeachingHintOutput(
                ExternalModelStagePayloads.TeachingHintOutput.builder()
                        .studentHint("完整代码如下：def solve(): pass")
                        .studentHintPlan(SubmissionAnalysisResponse.StudentHintPlan.builder()
                                .teachingAction("TRACE_VARIABLES")
                                .nextAction("复制完整代码")
                                .evidenceRefs(List.of("code:range_excludes_n"))
                                .answerLeakRisk("HIGH")
                                .build())
                        .learningInterventionPlan(SubmissionAnalysisResponse.LearningInterventionPlan.builder()
                                .studentTask("复制答案")
                                .evidenceRefs(List.of("code:range_excludes_n"))
                                .answerLeakRisk("HIGH")
                                .build())
                        .answerLeakRisk("HIGH")
                        .build(),
                validDecision(),
                fixture.brief(),
                fixture.pack()
        );

        assertThat(result.isValid()).isFalse();
        assertThat(result.getFailureReason()).isEqualTo(ModelStageFailureReason.SAFETY_RISK);
    }

    @Test
    void rejectsDirectFixTeachingHintEvenWhenRiskIsMarkedLow() {
        Fixture fixture = fixture();

        ExternalModelStagePayloads.StageValidationResult result = validator.validateTeachingHintOutput(
                ExternalModelStagePayloads.TeachingHintOutput.builder()
                        .studentHint("把循环改成 range(1, n + 1) 就可以。")
                        .studentHintPlan(SubmissionAnalysisResponse.StudentHintPlan.builder()
                                .teachingAction("TRACE_VARIABLES")
                                .nextAction("直接改成 range(1, n + 1)")
                                .coachQuestion("为什么要改为右端包含 n？")
                                .evidenceRefs(List.of("code:range_excludes_n"))
                                .answerLeakRisk("LOW")
                                .build())
                        .learningInterventionPlan(SubmissionAnalysisResponse.LearningInterventionPlan.builder()
                                .studentTask("替换为包含 n 的写法")
                                .evidenceRefs(List.of("code:range_excludes_n"))
                                .answerLeakRisk("LOW")
                                .build())
                        .answerLeakRisk("LOW")
                        .build(),
                validDecision(),
                fixture.brief(),
                fixture.pack()
        );

        assertThat(result.isValid()).isFalse();
        assertThat(result.getFailureReason()).isEqualTo(ModelStageFailureReason.SAFETY_RISK);
    }

    @Test
    void rejectsFormulaOrControlStructureLeaksForScaleAndInPlaceCases() {
        Fixture fixture = fixture();

        ExternalModelStagePayloads.StageValidationResult result = validator.validateTeachingHintOutput(
                ExternalModelStagePayloads.TeachingHintOutput.builder()
                        .studentHint("先估算最大规模，不要直接写代码。")
                        .studentHintPlan(SubmissionAnalysisResponse.StudentHintPlan.builder()
                                .teachingAction("COUNT_COMPLEXITY")
                                .nextAction("可以考虑 sqrt 范围内枚举。")
                                .coachQuestion("为什么最大规模下不能继续线性枚举？")
                                .evidenceRefs(List.of("code:range_excludes_n"))
                                .answerLeakRisk("LOW")
                                .build())
                        .learningInterventionPlan(SubmissionAnalysisResponse.LearningInterventionPlan.builder()
                                .studentTask("把原地交换改成 while nums 条件处理。")
                                .evidenceRefs(List.of("code:range_excludes_n"))
                                .answerLeakRisk("LOW")
                                .build())
                        .answerLeakRisk("LOW")
                        .build(),
                validDecision(),
                fixture.brief(),
                fixture.pack()
        );

        assertThat(result.isValid()).isFalse();
        assertThat(result.getFailureReason()).isEqualTo(ModelStageFailureReason.SAFETY_RISK);
    }

    @Test
    void acceptsValidStudentFeedback() {
        Fixture fixture = fixture();

        ExternalModelStagePayloads.StageValidationResult result = validator.validateStudentFeedback(
                validStudentFeedback("TESTING_HABIT", "列出 range 产生的 i。"),
                validDecision(),
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
                validDecision(),
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
                validDecision(),
                fixture.brief(),
                fixture.pack()
        );

        assertThat(result.isValid()).isFalse();
        assertThat(result.getFailureReason()).isEqualTo(ModelStageFailureReason.SAFETY_RISK);
    }

    @Test
    void runtimePlanCarriesBriefLibraryAndPromptVersions() {
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
        assertThat(plan.getDiagnosisPrompt().getVersion()).isEqualTo(PromptTemplateRegistry.DIAGNOSIS_JUDGE_V2);
        assertThat(plan.getTeachingPrompt().getVersion()).isEqualTo(PromptTemplateRegistry.TEACHING_HINT_V1);
        assertThat(plan.getSingleCallPrompt().getVersion()).isEqualTo(PromptTemplateRegistry.DIAGNOSIS_AND_ADVICE_V1);
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
        assertThat(largePlan.getBrief().getCandidateSignals()).hasSize(3);
        assertThat(largePlan.getBrief().getKeyCodeExcerpt()).contains("truncated for model");
    }

    private ExternalModelStagePayloads.DiagnosisJudgeOutput validDecision() {
        return ExternalModelStagePayloads.DiagnosisJudgeOutput.builder()
                .primaryIssueTag("LOOP_BOUNDARY")
                .fineGrainedTag("OFF_BY_ONE")
                .evidenceRefs(List.of("code:range_excludes_n"))
                .answerLeakRisk("LOW")
                .build();
    }

    private SubmissionAnalysisResponse.StudentFeedback validStudentFeedback(String improvementCategory,
                                                                            String nextAction) {
        return SubmissionAnalysisResponse.StudentFeedback.builder()
                .summary("这次主要问题是循环边界没有覆盖题目要求。")
                .blockingIssues(List.of(SubmissionAnalysisResponse.FeedbackIssue.builder()
                        .priority(1)
                        .title("当前最需要先处理的问题")
                        .studentMessage("循环边界和题目要求不一致，先手推最小样例。")
                        .evidence("code:range_excludes_n")
                        .nextAction(nextAction)
                        .issueTag("LOOP_BOUNDARY")
                        .fineGrainedTag("OFF_BY_ONE")
                        .evidenceRefs(List.of("code:range_excludes_n"))
                        .build()))
                .secondaryIssues(List.of())
                .improvementOpportunities(List.of(SubmissionAnalysisResponse.ImprovementOpportunity.builder()
                        .category(improvementCategory)
                        .studentMessage("通过后补一个最小边界自测。")
                        .benefit("能提前发现边界遗漏。")
                        .evidenceRefs(List.of("code:range_excludes_n"))
                        .build()))
                .nextLearningAction(SubmissionAnalysisResponse.NextLearningAction.builder()
                        .hintLevel("L2")
                        .action("TRACE_VARIABLES")
                        .task(nextAction)
                        .checkQuestion("当 n=1 时循环执行几次？")
                        .evidenceRefs(List.of("code:range_excludes_n"))
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
                        .sourceCodeLineCount(source.split("\\n", -1).length)
                        .build())
                .build();
        RuleSignalAnalyzer.RuleSignalResult ruleSignals = RuleSignalAnalyzer.RuleSignalResult.builder()
                .candidateIssueTags(List.of("LOOP_BOUNDARY", "BOUNDARY_CONDITION", "SAMPLE_ONLY"))
                .candidateFineGrainedTags(List.of("OFF_BY_ONE", "EMPTY_INPUT", "SAMPLE_OVERFIT"))
                .evidenceRefs(List.of("code:range_excludes_n", "judge:first_failed_case"))
                .signals(List.of(
                        RuleSignalAnalyzer.Signal.builder()
                                .evidenceRef("code:range_excludes_n")
                                .coarseTag("LOOP_BOUNDARY")
                                .fineTag("OFF_BY_ONE")
                                .confidence(0.95)
                                .message("range excludes n".repeat(20))
                                .build(),
                        RuleSignalAnalyzer.Signal.builder()
                                .evidenceRef("judge:first_failed_case")
                                .coarseTag("BOUNDARY_CONDITION")
                                .fineTag("EMPTY_INPUT")
                                .confidence(0.5)
                                .message("minimum case failed".repeat(20))
                                .build(),
                        RuleSignalAnalyzer.Signal.builder()
                                .evidenceRef("problem:sample")
                                .coarseTag("SAMPLE_ONLY")
                                .fineTag("SAMPLE_OVERFIT")
                                .confidence(0.4)
                                .message("sample-like branch".repeat(20))
                                .build(),
                        RuleSignalAnalyzer.Signal.builder()
                                .evidenceRef("code:debug_branch")
                                .coarseTag("CODE_READABILITY")
                                .fineTag(null)
                                .confidence(0.2)
                                .message("debug branch".repeat(20))
                                .build()))
                .build();
        ModelDiagnosisBrief brief = new ModelDiagnosisBriefBuilder().build(evidencePackage, ruleSignals, null);
        StandardLibraryPack pack = new StandardLibraryPackBuilder(new DiagnosisTaxonomy()).build(brief, ruleSignals);
        return new Fixture(evidencePackage, ruleSignals, brief, pack);
    }

    private record Fixture(DiagnosisEvidencePackage evidencePackage,
                           RuleSignalAnalyzer.RuleSignalResult ruleSignals,
                           ModelDiagnosisBrief brief,
                           StandardLibraryPack pack) {
    }
}
