package com.onlinejudge.submission.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PromptTemplateRegistryTest {

    private final PromptTemplateRegistry registry = new PromptTemplateRegistry();

    @Test
    void registersOnlySearchLocationAndAdvicePrompts() {
        PromptTemplateRegistry.PromptTemplate searchLocation =
                registry.get(PromptTemplateRegistry.SEARCH_LOCATION_V1);
        PromptTemplateRegistry.PromptTemplate advice =
                registry.get(PromptTemplateRegistry.DIAGNOSIS_AND_ADVICE_V1);

        assertThat(searchLocation.getStage()).isEqualTo("SEARCH_LOCATION");
        assertThat(searchLocation.getSystemPrompt())
                .contains("search-location stage")
                .contains("candidatePack")
                .contains("basicCandidates")
                .contains("improvementCandidates")
                .contains("knowledgeAnchors")
                .contains("Do not provide complete code");

        assertThat(advice.getStage()).isEqualTo("DIAGNOSIS_AND_ADVICE");
        assertThat(advice.getSystemPrompt())
                .contains("complete diagnosis and advice generation stage")
                .contains("caseUnderstanding")
                .contains("basicLayerAdvice")
                .contains("improvementLayerAdvice")
                .contains("nextStepPlan")
                .contains("studentSummary")
                .contains("Do not provide complete code");
    }

    @Test
    void oldPromptVersionsAreNotResolvable() {
        assertThatThrownBy(() -> registry.get("diagnosis-and-" + "teaching-v3"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown prompt template version");
        assertThatThrownBy(() -> registry.get("diagnosis-" + "judge-v2"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown prompt template version");
        assertThatThrownBy(() -> registry.get("teaching-" + "hint-v1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown prompt template version");
    }

    @Test
    void advicePromptDoesNotExposeLegacyContracts() {
        String prompt = registry.get(PromptTemplateRegistry.DIAGNOSIS_AND_ADVICE_V1).getSystemPrompt();

        assertThat(prompt)
                .doesNotContain("diagnosisDecision")
                .doesNotContain("teachingHint")
                .doesNotContain("Combined" + "Output")
                .doesNotContain("AiAnalysis" + "Payload");
    }
}
