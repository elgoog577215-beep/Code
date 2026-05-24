package com.onlinejudge.submission.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PromptTemplateRegistryTest {

    private final PromptTemplateRegistry registry = new PromptTemplateRegistry();

    @Test
    void diagnosisJudgePromptDefinesTightJsonContract() {
        PromptTemplateRegistry.PromptTemplate template = registry.get(PromptTemplateRegistry.DIAGNOSIS_JUDGE_V2);

        assertThat(template.getVersion()).isEqualTo("diagnosis-judge-v2");
        assertThat(template.getStage()).isEqualTo("DIAGNOSIS_JUDGE");
        assertThat(template.getSystemPrompt())
                .contains("Return strict JSON only")
                .contains("ModelDiagnosisBrief")
                .contains("StandardLibraryPack")
                .contains("primaryIssueTag")
                .contains("fineGrainedTag")
                .contains("evidenceRefs")
                .contains("NEEDS_MORE_EVIDENCE")
                .contains("standardLibrary.decisionProtocol")
                .contains("most evidence-supported tag")
                .contains("direct evidence distinguishes")
                .contains("Do not provide complete code")
                .contains("hidden test data");
    }

    @Test
    void teachingHintPromptUsesValidatedDecisionAndSafetyBoundary() {
        PromptTemplateRegistry.PromptTemplate template = registry.get(PromptTemplateRegistry.TEACHING_HINT_V1);

        assertThat(template.getVersion()).isEqualTo("teaching-hint-v1");
        assertThat(template.getStage()).isEqualTo("TEACHING_HINT");
        assertThat(template.getSystemPrompt())
                .contains("validated diagnosis decision")
                .contains("studentHintPlan")
                .contains("learningInterventionPlan")
                .contains("teacherNote")
                .contains("teachingAction MUST come from standardLibrary.teachingActions")
                .contains("All user-facing strings MUST be Simplified Chinese")
                .contains("Do not provide complete code");
    }

    @Test
    void singleCallPromptReturnsDiagnosisAndTeachingInOneContract() {
        PromptTemplateRegistry.PromptTemplate template = registry.get(PromptTemplateRegistry.DIAGNOSIS_AND_TEACHING_V2);

        assertThat(template.getVersion()).isEqualTo("diagnosis-and-teaching-v2");
        assertThat(template.getStage()).isEqualTo("DIAGNOSIS_AND_TEACHING");
        assertThat(template.getSystemPrompt())
                .contains("low-budget single-call runtime")
                .contains("diagnosisDecision")
                .contains("teachingHint")
                .contains("studentHintPlan")
                .contains("learningInterventionPlan")
                .contains("standardLibrary.decisionProtocol")
                .contains("most evidence-supported tag")
                .contains("direct evidence distinguishes")
                .contains("teachingHint.studentHintPlan.teachingAction MUST come from standardLibrary.teachingActions")
                .contains("Do not provide complete code");
    }

    @Test
    void unknownPromptVersionFailsFast() {
        assertThatThrownBy(() -> registry.get("unknown"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown prompt template version");
    }

    @Test
    void keepsLegacyPromptVersionsResolvableForHistoricalCompatibility() {
        assertThat(registry.get(PromptTemplateRegistry.DIAGNOSIS_JUDGE_V1).getVersion())
                .isEqualTo("diagnosis-judge-v1");
        assertThat(registry.get(PromptTemplateRegistry.DIAGNOSIS_AND_TEACHING_V1).getVersion())
                .isEqualTo("diagnosis-and-teaching-v1");
    }
}
