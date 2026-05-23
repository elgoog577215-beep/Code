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
        assertThat(plan.getDiagnosisPrompt().getVersion()).isEqualTo(PromptTemplateRegistry.DIAGNOSIS_JUDGE_V1);
        assertThat(plan.getTeachingPrompt().getVersion()).isEqualTo(PromptTemplateRegistry.TEACHING_HINT_V1);
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
        return new Fixture(evidencePackage, ruleSignals, brief, pack);
    }

    private record Fixture(DiagnosisEvidencePackage evidencePackage,
                           RuleSignalAnalyzer.RuleSignalResult ruleSignals,
                           ModelDiagnosisBrief brief,
                           StandardLibraryPack pack) {
    }
}
