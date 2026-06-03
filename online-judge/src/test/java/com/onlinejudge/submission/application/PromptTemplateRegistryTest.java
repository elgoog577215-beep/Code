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
                .contains("teachable root cause")
                .contains("distracting signals")
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
        PromptTemplateRegistry.PromptTemplate template = registry.get(PromptTemplateRegistry.DIAGNOSIS_AND_TEACHING_V3);

        assertThat(template.getVersion()).isEqualTo("diagnosis-and-teaching-v3");
        assertThat(template.getStage()).isEqualTo("DIAGNOSIS_AND_TEACHING");
        assertThat(template.getSystemPrompt())
                .contains("low-budget single-call runtime")
                .contains("diagnosisDecision")
                .contains("teachingHint")
                .contains("studentFeedback")
                .contains("primaryReasoning")
                .contains("\"secondaryIssues\": [{")
                .contains("distractorNotes")
                .contains("\"distractorNotes\": [{")
                .contains("teachingPriority")
                .contains("\"teachingPriority\": string")
                .contains("\"nextLearningAction\": {")
                .contains("standardLibrary.educationAgentProtocol")
                .contains("external education AI agent")
                .contains("Omit teachingHint or set it to null")
                .contains("studentFeedback is the primary student-facing output")
                .contains("generator:*")
                .contains("most-specific refs plus a readable judge/problem ref")
                .contains("standardLibrary.judgmentCalibrationExamples")
                .contains("decision-shape calibration")
                .contains("do not copy them when evidence differs")
                .contains("standardLibrary.educationAgentProtocol.rootCauseDecisionChecklist")
                .contains("locate evidence, connect code behavior, compare causes, demote distractors")
                .contains("standardLibrary.educationAgentProtocol.nativeTraceQualityChecklist")
                .contains("self-check diagnosisDecision")
                .contains("selected root cause and concrete evidenceRefs")
                .contains("outranks secondary issues")
                .contains("blockingIssues")
                .contains("improvementOpportunities")
                .contains("then decisionProtocol and studentFeedbackRules")
                .contains("current blocking root cause")
                .contains("secondary issues")
                .contains("distracting signals")
                .contains("under 40 Chinese characters")
                .contains("count required input groups and actual read operations")
                .contains("for _ in range(q)")
                .contains("直接改成")
                .contains("evidence collection and SHOULD be LOW or MEDIUM, not HIGH")
                .contains("Do not provide complete code");
    }

    @Test
    void singleCallPromptSchemaListsEducationJudgmentFieldsAcceptedByDto() {
        String prompt = registry.get(PromptTemplateRegistry.DIAGNOSIS_AND_TEACHING_V3).getSystemPrompt();
        String schema = prompt.substring(prompt.indexOf("\"diagnosisDecision\""), prompt.indexOf("\"studentFeedback\""));

        assertThat(schema)
                .contains("\"primaryReasoning\": string")
                .contains("\"secondaryIssues\": [{")
                .contains("\"distractorNotes\": [{")
                .contains("\"teachingPriority\": string")
                .contains("\"improvementOpportunities\": [{")
                .contains("\"nextLearningAction\": {");
    }

    @Test
    void liteSingleCallPromptKeepsOutputShortAndNativeTraceExplicit() {
        PromptTemplateRegistry.PromptTemplate template =
                registry.get(PromptTemplateRegistry.DIAGNOSIS_AND_TEACHING_V4_LITE);

        assertThat(template.getVersion()).isEqualTo("diagnosis-and-teaching-v4-lite");
        assertThat(template.getStage()).isEqualTo("DIAGNOSIS_AND_TEACHING");
        assertThat(template.getSystemPrompt())
                .contains("low-latency single-call runtime")
                .contains("Return one strict minified JSON object only")
                .contains("Set teachingHint to null")
                .contains("diagnosisDecision.secondaryIssues max 1 item")
                .contains("distractorNotes max 1 item")
                .contains("studentFeedback.blockingIssues exactly 1 item")
                .contains("improvementOpportunities exactly 1 item")
                .contains("MUST reuse values from diagnosisDecision.evidenceRefs")
                .contains("Do not invent new evidenceRefs in nested fields")
                .contains("Keep output under 900 tokens")
                .contains("studentFeedback.secondaryIssues SHOULD be []")
                .contains("Use the same short nextLearningAction object")
                .contains("blockingIssues[0].studentMessage MUST include")
                .contains("primaryReasoning MUST use this short shape")
                .contains("standardLibrary.educationAgentProtocol.rootCauseDecisionChecklist")
                .contains("不能解释当前失败")
                .contains("不是主因")
                .contains("次要信号，不是主因")
                .contains("干扰信号，不能解释当前失败")
                .contains("nextLearningAction.task MUST be one observable action")
                .contains("for _ in range(q)")
                .contains("直接改成")
                .contains("safe evidence collection should be LOW or MEDIUM");
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
