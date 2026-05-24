package com.onlinejudge.submission.application;

import com.onlinejudge.submission.dto.SubmissionAnalysisResponse;
import lombok.Builder;
import lombok.Data;
import org.springframework.stereotype.Component;

@Component
public class ExternalModelAgentRuntime {

    private final ModelDiagnosisBriefBuilder briefBuilder;
    private final StandardLibraryPackBuilder standardLibraryPackBuilder;
    private final PromptTemplateRegistry promptTemplateRegistry;
    private final ModelOutputValidator modelOutputValidator;
    private final ExternalModelOutputNormalizer outputNormalizer;

    public ExternalModelAgentRuntime(ModelDiagnosisBriefBuilder briefBuilder,
                                     StandardLibraryPackBuilder standardLibraryPackBuilder,
                                     PromptTemplateRegistry promptTemplateRegistry,
                                     ModelOutputValidator modelOutputValidator) {
        this(briefBuilder, standardLibraryPackBuilder, promptTemplateRegistry, modelOutputValidator,
                new ExternalModelOutputNormalizer());
    }

    public ExternalModelAgentRuntime(ModelDiagnosisBriefBuilder briefBuilder,
                                     StandardLibraryPackBuilder standardLibraryPackBuilder,
                                     PromptTemplateRegistry promptTemplateRegistry,
                                     ModelOutputValidator modelOutputValidator,
                                     ExternalModelOutputNormalizer outputNormalizer) {
        this.briefBuilder = briefBuilder;
        this.standardLibraryPackBuilder = standardLibraryPackBuilder;
        this.promptTemplateRegistry = promptTemplateRegistry;
        this.modelOutputValidator = modelOutputValidator;
        this.outputNormalizer = outputNormalizer == null ? new ExternalModelOutputNormalizer() : outputNormalizer;
    }

    public RuntimePlan prepare(DiagnosisEvidencePackage evidencePackage,
                               RuleSignalAnalyzer.RuleSignalResult ruleSignals,
                               SubmissionAnalysisResponse fallback) {
        ModelDiagnosisBrief brief = briefBuilder.build(evidencePackage, ruleSignals, fallback);
        StandardLibraryPack standardLibraryPack = standardLibraryPackBuilder.build(brief, ruleSignals);
        return RuntimePlan.builder()
                .brief(brief)
                .standardLibraryPack(standardLibraryPack)
                .diagnosisPrompt(promptTemplateRegistry.get(PromptTemplateRegistry.DIAGNOSIS_JUDGE_V2))
                .teachingPrompt(promptTemplateRegistry.get(PromptTemplateRegistry.TEACHING_HINT_V1))
                .singleCallPrompt(promptTemplateRegistry.get(PromptTemplateRegistry.DIAGNOSIS_AND_TEACHING_V2))
                .build();
    }

    public ExternalModelStagePayloads.StageValidationResult validateDiagnosisDecision(
            ExternalModelStagePayloads.DiagnosisJudgeOutput output,
            RuntimePlan runtimePlan) {
        return modelOutputValidator.validateDiagnosisJudgeOutput(
                output,
                runtimePlan == null ? null : runtimePlan.getBrief(),
                runtimePlan == null ? null : runtimePlan.getStandardLibraryPack()
        );
    }

    public ExternalModelStagePayloads.DiagnosisJudgeOutput normalizeDiagnosisDecision(
            ExternalModelStagePayloads.DiagnosisJudgeOutput output,
            RuntimePlan runtimePlan) {
        return outputNormalizer.normalizeDiagnosisDecision(output, runtimePlan);
    }

    public ExternalModelStagePayloads.TeachingHintOutput normalizeTeachingHint(
            ExternalModelStagePayloads.TeachingHintOutput output,
            RuntimePlan runtimePlan) {
        return outputNormalizer.normalizeTeachingHint(output, runtimePlan);
    }

    public ExternalModelStagePayloads.CombinedOutput normalizeCombinedOutput(
            ExternalModelStagePayloads.CombinedOutput output,
            RuntimePlan runtimePlan) {
        return outputNormalizer.normalizeCombinedOutput(output, runtimePlan);
    }

    public ExternalModelStagePayloads.StageValidationResult validateTeachingHint(
            ExternalModelStagePayloads.TeachingHintOutput output,
            ExternalModelStagePayloads.DiagnosisJudgeOutput decision,
            RuntimePlan runtimePlan) {
        return modelOutputValidator.validateTeachingHintOutput(
                output,
                decision,
                runtimePlan == null ? null : runtimePlan.getBrief(),
                runtimePlan == null ? null : runtimePlan.getStandardLibraryPack()
        );
    }

    @Data
    @Builder
    public static class RuntimePlan {
        private ModelDiagnosisBrief brief;
        private StandardLibraryPack standardLibraryPack;
        private PromptTemplateRegistry.PromptTemplate diagnosisPrompt;
        private PromptTemplateRegistry.PromptTemplate teachingPrompt;
        private PromptTemplateRegistry.PromptTemplate singleCallPrompt;
    }
}
