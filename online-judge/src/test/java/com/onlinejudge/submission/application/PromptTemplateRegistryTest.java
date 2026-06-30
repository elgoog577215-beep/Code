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
                .contains("单诊断 Agent")
                .contains("标准库的作用")
                .contains("不是唯一答案来源")
                .contains("不能限制你对题目、代码和判题结果的整体判断")
                .contains("studentReport")
                .contains("basicLayerText")
                .contains("improvementLayerText")
                .contains("nextActionText")
                .contains("先讲人话")
                .contains("基础层优先")
                .contains("120-220 个中文字符")
                .contains("80-180 个中文字符")
                .contains("OUT_OF_LIBRARY")
                .contains("HIT")
                .contains("PARTIAL")
                .contains("MISS")
                .contains("禁止给完整代码")
                .contains("不要写出替换表达式或精确循环头");
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
