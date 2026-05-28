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
    void rejectsPrimaryDiagnosisSupportedOnlyByMemoryEvidence() {
        Fixture fixture = memoryConflictFixture();

        ExternalModelStagePayloads.StageValidationResult result = validator.validateDiagnosisJudgeOutput(
                ExternalModelStagePayloads.DiagnosisJudgeOutput.builder()
                        .primaryIssueTag("IO_FORMAT")
                        .fineGrainedTag("INPUT_PARSING")
                        .evidenceRefs(List.of("memory:recurring_issue:IO_FORMAT"))
                        .confidence(0.82)
                        .uncertainty("Student often has input parsing issues.")
                        .needsMoreEvidence(false)
                        .answerLeakRisk("LOW")
                        .build(),
                fixture.brief(),
                fixture.pack()
        );

        assertThat(result.isValid()).isFalse();
        assertThat(result.getFailureReason()).isEqualTo(ModelStageFailureReason.INVALID_EVIDENCE_REF);
        assertThat(result.getMessage()).contains("student memory evidence");
    }

    @Test
    void acceptsMemoryEvidenceWhenCurrentEvidenceAlsoSupportsDiagnosis() {
        Fixture fixture = memoryAlignedFixture();

        ExternalModelStagePayloads.StageValidationResult result = validator.validateDiagnosisJudgeOutput(
                ExternalModelStagePayloads.DiagnosisJudgeOutput.builder()
                        .primaryIssueTag("LOOP_BOUNDARY")
                        .fineGrainedTag("OFF_BY_ONE")
                        .evidenceRefs(List.of("code:range_excludes_n", "memory:recurring_issue:LOOP_BOUNDARY"))
                        .confidence(0.88)
                        .uncertainty("Current loop evidence and memory are aligned.")
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
    void rejectsTeachingOnlyMemoryTagEvenWhenGenericCurrentRefIsCited() {
        Fixture fixture = memoryConflictFixture();

        ExternalModelStagePayloads.StageValidationResult result = validator.validateDiagnosisJudgeOutput(
                ExternalModelStagePayloads.DiagnosisJudgeOutput.builder()
                        .primaryIssueTag("IO_FORMAT")
                        .fineGrainedTag("INPUT_PARSING")
                        .evidenceRefs(List.of("code:range_excludes_n", "memory:recurring_issue:IO_FORMAT"))
                        .confidence(0.82)
                        .uncertainty("Student often has input parsing issues.")
                        .needsMoreEvidence(false)
                        .answerLeakRisk("LOW")
                        .build(),
                fixture.brief(),
                fixture.pack()
        );

        assertThat(result.isValid()).isFalse();
        assertThat(result.getFailureReason()).isEqualTo(ModelStageFailureReason.INVALID_EVIDENCE_REF);
        assertThat(result.getMessage()).contains("without current evidence support");
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
    void acceptsSafeVisibleTeachingHintEvenWhenTopLevelRiskIsHigh() {
        Fixture fixture = fixture();

        ExternalModelStagePayloads.StageValidationResult result = validator.validateTeachingHintOutput(
                ExternalModelStagePayloads.TeachingHintOutput.builder()
                        .studentHint("先用 n=1 手推循环执行次数。")
                        .studentHintPlan(SubmissionAnalysisResponse.StudentHintPlan.builder()
                                .teachingAction("TRACE_VARIABLES")
                                .nextAction("列出 range 产生的 i 值。")
                                .coachQuestion("最后一次循环有没有包含 n？")
                                .evidenceRefs(List.of("code:range_excludes_n"))
                                .answerLeakRisk("LOW")
                                .build())
                        .learningInterventionPlan(SubmissionAnalysisResponse.LearningInterventionPlan.builder()
                                .studentTask("手推 n=1 和 n=2 两个最小样例。")
                                .evidenceRefs(List.of("code:range_excludes_n"))
                                .answerLeakRisk("LOW")
                                .build())
                        .answerLeakRisk("HIGH")
                        .build(),
                validDecision(),
                fixture.brief(),
                fixture.pack()
        );

        assertThat(result.isValid()).isTrue();
        assertThat(result.getFailureReason()).isEqualTo(ModelStageFailureReason.NONE);
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
        assertThat(plan.getSingleCallPrompt().getVersion()).isEqualTo(PromptTemplateRegistry.DIAGNOSIS_AND_TEACHING_V2);
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
        DiagnosisEvidencePackage evidencePackage = baseEvidencePackage(null);
        RuleSignalAnalyzer.RuleSignalResult ruleSignals = ruleSignals();
        ModelDiagnosisBrief brief = new ModelDiagnosisBriefBuilder().build(evidencePackage, ruleSignals, null);
        StandardLibraryPack pack = new StandardLibraryPackBuilder(new DiagnosisTaxonomy()).build(brief, ruleSignals);
        return new Fixture(evidencePackage, ruleSignals, brief, pack);
    }

    private Fixture memoryConflictFixture() {
        DiagnosisEvidencePackage evidencePackage = baseEvidencePackage(
                DiagnosisEvidencePackage.StudentLearningMemorySnapshot.builder()
                        .studentProfileId(9L)
                        .recurringIssueTags(List.of(DiagnosisEvidencePackage.MemoryTagStat.builder()
                                .tag("IO_FORMAT")
                                .count(4L)
                                .build()))
                        .recurringFineGrainedTags(List.of(DiagnosisEvidencePackage.MemoryTagStat.builder()
                                .tag("INPUT_PARSING")
                                .count(4L)
                                .build()))
                        .evidenceRefs(List.of("memory:student:9", "memory:recurring_issue:IO_FORMAT"))
                        .build()
        );
        RuleSignalAnalyzer.RuleSignalResult ruleSignals = ruleSignals();
        ModelDiagnosisBrief brief = new ModelDiagnosisBriefBuilder().build(evidencePackage, ruleSignals, null);
        StandardLibraryPack pack = new StandardLibraryPackBuilder(new DiagnosisTaxonomy()).build(brief, ruleSignals);
        return new Fixture(evidencePackage, ruleSignals, brief, pack);
    }

    private Fixture memoryAlignedFixture() {
        DiagnosisEvidencePackage evidencePackage = baseEvidencePackage(
                DiagnosisEvidencePackage.StudentLearningMemorySnapshot.builder()
                        .studentProfileId(9L)
                        .recurringIssueTags(List.of(DiagnosisEvidencePackage.MemoryTagStat.builder()
                                .tag("LOOP_BOUNDARY")
                                .count(4L)
                                .build()))
                        .evidenceRefs(List.of("memory:student:9", "memory:recurring_issue:LOOP_BOUNDARY"))
                        .build()
        );
        RuleSignalAnalyzer.RuleSignalResult ruleSignals = ruleSignals();
        ModelDiagnosisBrief brief = new ModelDiagnosisBriefBuilder().build(evidencePackage, ruleSignals, null);
        StandardLibraryPack pack = new StandardLibraryPackBuilder(new DiagnosisTaxonomy()).build(brief, ruleSignals);
        return new Fixture(evidencePackage, ruleSignals, brief, pack);
    }

    private DiagnosisEvidencePackage baseEvidencePackage(DiagnosisEvidencePackage.StudentLearningMemorySnapshot memory) {
        return DiagnosisEvidencePackage.builder()
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
                .learningMemory(memory)
                .build();
    }

    private RuleSignalAnalyzer.RuleSignalResult ruleSignals() {
        return RuleSignalAnalyzer.RuleSignalResult.builder()
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
    }

    private record Fixture(DiagnosisEvidencePackage evidencePackage,
                           RuleSignalAnalyzer.RuleSignalResult ruleSignals,
                           ModelDiagnosisBrief brief,
                           StandardLibraryPack pack) {
    }
}
