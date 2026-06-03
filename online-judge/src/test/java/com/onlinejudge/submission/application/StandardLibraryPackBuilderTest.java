package com.onlinejudge.submission.application;

import com.onlinejudge.learning.diagnosis.DiagnosisTaxonomy;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StandardLibraryPackBuilderTest {

    private final StandardLibraryPackBuilder builder =
            new StandardLibraryPackBuilder(new DiagnosisTaxonomy());

    @Test
    void includesTeachingActionsFromFineGrainedTags() {
        ModelDiagnosisBrief brief = ModelDiagnosisBrief.builder()
                .allowedIssueTags(List.of("ALGORITHM_STRATEGY"))
                .allowedFineGrainedTags(List.of("DP_STATE_DESIGN"))
                .build();

        StandardLibraryPack pack = builder.build(brief, null);

        assertThat(pack.getIssueTags())
                .extracting(StandardLibraryPack.TagOption::getId)
                .contains("ALGORITHM_STRATEGY", "NEEDS_MORE_EVIDENCE");
        assertThat(pack.getFineGrainedTags())
                .extracting(StandardLibraryPack.TagOption::getId)
                .contains("DP_STATE_DESIGN");
        assertThat(pack.getTeachingActions())
                .extracting(StandardLibraryPack.TeachingActionOption::getId)
                .contains("CHECK_INVARIANT", "DEFINE_STATE", "COLLECT_EVIDENCE");
        assertThat(pack.getImprovementTags())
                .extracting(StandardLibraryPack.ImprovementTagOption::getId)
                .contains("COMPLEXITY", "TESTING_HABIT", "CODE_CLARITY", "BOUNDARY_AWARENESS", "ROBUSTNESS", "DEBUG_CLEANUP");
        assertThat(pack.getStudentFeedbackRules()).isNotNull();
        assertThat(pack.getStudentFeedbackRules().getBlockingIssueRules())
                .anySatisfy(rule -> assertThat(rule).contains("blockingIssues"));
        assertThat(pack.getDecisionProtocol()).isNotNull();
        assertThat(pack.getDecisionProtocol().getGlobalRules())
                .anySatisfy(rule -> assertThat(rule).contains("most evidence-supported diagnosis"));
        assertThat(pack.getDecisionProtocol().getEvidencePriorityRules())
                .anySatisfy(rule -> assertThat(rule).contains("Visible failed case"));
        assertThat(pack.getDecisionProtocol().getTagSelectionRules())
                .anySatisfy(rule -> assertThat(rule).contains("NEEDS_MORE_EVIDENCE"));
        assertThat(pack.getDecisionProtocol().getConflictRules())
                .anySatisfy(rule -> assertThat(rule).contains("candidate signals conflict"));
        assertThat(pack.getDecisionProtocol().getTeachingActionRules())
                .anySatisfy(rule -> assertThat(rule).contains("COLLECT_EVIDENCE"));
        assertThat(pack.getEducationAgentProtocol()).isNotNull();
        assertThat(pack.getEducationAgentProtocol().getRoleRules())
                .anySatisfy(rule -> assertThat(rule).contains("external education AI agent"));
        assertThat(pack.getEducationAgentProtocol().getRootCauseDecisionChecklist())
                .containsExactly(
                        "1. Locate the earliest concrete failure evidence: first failed case, compiler/runtime message, visible output mismatch, or strongest candidate signal.",
                        "2. Connect that evidence to the student's code behavior: what the code read, computed, skipped, repeated, printed, or failed to guard.",
                        "3. Compare candidate root causes and choose the one that most directly explains the failed behavior, not the most severe or familiar label.",
                        "4. Demote distractors such as helper names, debug branches, sample-specific branches, style, or broad complexity unless they directly explain the same evidence.",
                        "5. Turn the chosen root cause into one observable next action that lets the student verify the diagnosis without seeing the full fix."
                );
        assertThat(pack.getEducationAgentProtocol().getPrimaryRootCauseRules())
                .anySatisfy(rule -> assertThat(rule).contains("blocking root cause"));
        assertThat(pack.getEducationAgentProtocol().getEvidenceGroundingRules())
                .anySatisfy(rule -> assertThat(rule).contains("generator:*").contains("most specific"));
        assertThat(pack.getEducationAgentProtocol().getStudentActionRules())
                .anySatisfy(rule -> assertThat(rule).contains("observable next action"));
        assertThat(pack.getEducationAgentProtocol().getSafetyBoundaryRules())
                .anySatisfy(rule -> assertThat(rule)
                        .contains("for _ in range(q)")
                        .contains("read operations")
                        .contains("directly add a loop"));
        assertThat(pack.getEducationAgentProtocol().getSafetyBoundaryRules())
                .anySatisfy(rule -> assertThat(rule).contains("HIGH risk").contains("solution leakage"));
        assertThat(pack.getEducationAgentProtocol().getNativeTraceQualityChecklist())
                .containsExactly(
                        "nativePrimaryReasoningGrounded: primaryReasoning must name the selected root cause, cite the strongest evidenceRefs, and mention the concrete failed behavior.",
                        "nativeTeachingPriorityClear: teachingPriority must say why this is the first learning focus before secondary improvements.",
                        "nativeSecondarySignalsBalanced: secondaryIssues and distractorNotes must explain why non-primary signals do not outrank the current failure.",
                        "nativeNextActionObservable: nextLearningAction must be one observable comparison, trace, estimate, counterexample, or checklist task with evidenceRefs.",
                        "nativeSafetyBoundary: no complete code, direct replacement structure, hidden test guess, transition formula, or phrase such as directly change to / 直接改成."
                );
        assertThat(pack.getJudgmentCalibrationExamples())
                .extracting(StandardLibraryPack.JudgmentCalibrationExample::getId)
                .contains("multi-query-input-primary", "sample-overfit-hidden-primary", "large-bound-scale-primary");
        assertThat(pack.getJudgmentCalibrationExamples())
                .anySatisfy(example -> {
                    assertThat(example.getWhen()).contains("fewer lines");
                    assertThat(example.getChoosePrimary()).contains("INPUT_PARSING");
                    assertThat(example.getDoNotChoosePrimary()).contains("DEBUG_CLEANUP");
                    assertThat(example.getNextActionPattern()).contains("读取").contains("输出");
                    assertThat(example.getSafeImprovementCategories()).contains("TESTING_HABIT");
                });
        assertThat(pack.getSafetyRules())
                .anySatisfy(rule -> assertThat(rule).contains("for _ in range(q)").contains("directly add a loop"));
        assertThat(pack.getSafetyRules())
                .anySatisfy(rule -> assertThat(rule).contains("answerLeakRisk=HIGH").contains("complete code"));
    }

    @Test
    void includesFineGrainedActionsFromRuleSignalsEvenWhenBriefIsBroad() {
        ModelDiagnosisBrief brief = ModelDiagnosisBrief.builder()
                .allowedIssueTags(List.of("BOUNDARY_CONDITION"))
                .build();
        RuleSignalAnalyzer.RuleSignalResult signals = RuleSignalAnalyzer.RuleSignalResult.builder()
                .candidateFineGrainedTags(List.of("OUTPUT_FORMAT_DETAIL"))
                .build();

        StandardLibraryPack pack = builder.build(brief, signals);

        assertThat(pack.getFineGrainedTags())
                .extracting(StandardLibraryPack.TagOption::getId)
                .contains("OUTPUT_FORMAT_DETAIL");
        assertThat(pack.getTeachingActions())
                .extracting(StandardLibraryPack.TeachingActionOption::getId)
                .contains("ASK_MIN_CASE", "COMPARE_OUTPUT");
    }

    @Test
    void compactRuntimeKeepsCalibrationExamplesForAutoProfile() {
        ExternalModelAgentRuntime runtime = new ExternalModelAgentRuntime(
                new ModelDiagnosisBriefBuilder(),
                builder,
                new PromptTemplateRegistry(),
                new ModelOutputValidator()
        );
        StandardLibraryPack fullPack = builder.build(ModelDiagnosisBrief.builder()
                .allowedIssueTags(List.of("IO_FORMAT", "TIME_COMPLEXITY"))
                .allowedFineGrainedTags(List.of("INPUT_PARSING", "OVER_SIMULATION"))
                .build(), null);

        StandardLibraryPack compactPack = ReflectionTestUtils.invokeMethod(runtime, "compactStandardLibraryPack", fullPack);

        assertThat(compactPack.getJudgmentCalibrationExamples())
                .extracting(StandardLibraryPack.JudgmentCalibrationExample::getId)
                .contains("multi-query-input-primary", "large-bound-scale-primary");
        assertThat(compactPack.getJudgmentCalibrationExamples())
                .allSatisfy(example -> assertThat(example.getReasoningPattern()).hasSizeLessThanOrEqualTo(90));
        assertThat(compactPack.getEducationAgentProtocol().getRootCauseDecisionChecklist())
                .contains(
                        "1. Locate earliest concrete failed evidence.",
                        "3. Compare candidate root causes and choose the direct explainer.",
                        "5. Convert the chosen root cause into one observable next action."
                );
        assertThat(compactPack.getEducationAgentProtocol().getNativeTraceQualityChecklist())
                .contains(
                        "nativePrimaryReasoningGrounded: root cause + strongest evidenceRefs + concrete failed behavior.",
                        "nativeNextActionObservable: one observable compare/trace/estimate/counterexample/checklist task with evidenceRefs.",
                        "nativeSafetyBoundary: no code, hidden data, replacement structures, formulas, or 直接改成."
                );
    }

    @Test
    void runtimePlanSupportsSingleCallPromptVersionOverrideForEvalExperiments() {
        ExternalModelAgentRuntime runtime = new ExternalModelAgentRuntime(
                new ModelDiagnosisBriefBuilder(),
                builder,
                new PromptTemplateRegistry(),
                new ModelOutputValidator()
        );

        ExternalModelAgentRuntime.RuntimePlan v2Plan = runtime.prepare(
                DiagnosisEvidencePackage.builder().build(),
                RuleSignalAnalyzer.RuleSignalResult.builder().build(),
                null,
                "auto",
                PromptTemplateRegistry.DIAGNOSIS_AND_TEACHING_V2
        );
        ExternalModelAgentRuntime.RuntimePlan v4LitePlan = runtime.prepare(
                DiagnosisEvidencePackage.builder().build(),
                RuleSignalAnalyzer.RuleSignalResult.builder().build(),
                null,
                "auto",
                PromptTemplateRegistry.DIAGNOSIS_AND_TEACHING_V4_LITE
        );
        ExternalModelAgentRuntime.RuntimePlan fallbackPlan = runtime.prepare(
                DiagnosisEvidencePackage.builder().build(),
                RuleSignalAnalyzer.RuleSignalResult.builder().build(),
                null,
                "auto",
                "missing-prompt-candidate"
        );

        assertThat(v2Plan.getSingleCallPrompt().getVersion())
                .isEqualTo(PromptTemplateRegistry.DIAGNOSIS_AND_TEACHING_V2);
        assertThat(v4LitePlan.getSingleCallPrompt().getVersion())
                .isEqualTo(PromptTemplateRegistry.DIAGNOSIS_AND_TEACHING_V4_LITE);
        assertThat(v4LitePlan.getSingleCallPrompt().getSystemPrompt())
                .contains("low-latency single-call runtime")
                .contains("teachingHint to null");
        assertThat(fallbackPlan.getSingleCallPrompt().getVersion())
                .isEqualTo(PromptTemplateRegistry.DIAGNOSIS_AND_TEACHING_V3);
    }
}
