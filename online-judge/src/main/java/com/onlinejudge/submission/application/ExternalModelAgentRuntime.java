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

    public ExternalModelAgentRuntime(ModelDiagnosisBriefBuilder briefBuilder,
                                     StandardLibraryPackBuilder standardLibraryPackBuilder,
                                     PromptTemplateRegistry promptTemplateRegistry,
                                     ModelOutputValidator modelOutputValidator) {
        this.briefBuilder = briefBuilder;
        this.standardLibraryPackBuilder = standardLibraryPackBuilder;
        this.promptTemplateRegistry = promptTemplateRegistry;
        this.modelOutputValidator = modelOutputValidator;
    }

    public RuntimePlan prepare(DiagnosisEvidencePackage evidencePackage,
                               RuleSignalAnalyzer.RuleSignalResult ruleSignals,
                               SubmissionAnalysisResponse fallback) {
        ModelDiagnosisBrief brief = briefBuilder.build(evidencePackage, ruleSignals, fallback);
        StandardLibraryPack standardLibraryPack = standardLibraryPackBuilder.build(brief, ruleSignals);
        return RuntimePlan.builder()
                .brief(brief)
                .standardLibraryPack(standardLibraryPack)
                .diagnosisPrompt(promptTemplateRegistry.get(PromptTemplateRegistry.DIAGNOSIS_JUDGE_V1))
                .teachingPrompt(promptTemplateRegistry.get(PromptTemplateRegistry.TEACHING_HINT_V1))
                .singleCallPrompt(promptTemplateRegistry.get(PromptTemplateRegistry.DIAGNOSIS_AND_TEACHING_V1))
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
