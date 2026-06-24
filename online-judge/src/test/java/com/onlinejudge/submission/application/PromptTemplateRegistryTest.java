package com.onlinejudge.submission.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PromptTemplateRegistryTest {

    private final PromptTemplateRegistry registry = new PromptTemplateRegistry();

    @Test
    void registersSearchLocationAdviceAndDiagnosisReportPrompts() {
        PromptTemplateRegistry.PromptTemplate searchLocation =
                registry.get(PromptTemplateRegistry.SEARCH_LOCATION_V1);
        PromptTemplateRegistry.PromptTemplate advice =
                registry.get(PromptTemplateRegistry.DIAGNOSIS_AND_ADVICE_V1);
        PromptTemplateRegistry.PromptTemplate report =
                registry.get(PromptTemplateRegistry.DIAGNOSIS_REPORT_V2);

        assertThat(searchLocation.getStage()).isEqualTo("SEARCH_LOCATION");
        assertThat(searchLocation.getSystemPrompt())
                .contains("search-location stage")
                .contains("candidatePack")
                .contains("basicCandidates")
                .contains("improvementCandidates")
                .contains("knowledgeAnchors")
                .contains("navigation map")
                .contains("HIT")
                .contains("PARTIAL")
                .contains("MISS")
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

        assertThat(report.getStage()).isEqualTo("DIAGNOSIS_REPORT");
        assertThat(report.getSystemPrompt())
                .contains("diagnosis report v2")
                .contains("studentReport")
                .contains("basicLayerText")
                .contains("improvementLayerText")
                .contains("nextActionText")
                .contains("HIT")
                .contains("PARTIAL")
                .contains("MISS")
                .contains("full code")
                .contains("state definition");
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
