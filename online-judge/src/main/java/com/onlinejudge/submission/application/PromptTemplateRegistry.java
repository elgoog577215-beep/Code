package com.onlinejudge.submission.application;

import lombok.Builder;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PromptTemplateRegistry {

    public static final String DIAGNOSIS_JUDGE_V1 = "diagnosis-judge-v1";
    public static final String TEACHING_HINT_V1 = "teaching-hint-v1";
    public static final String DIAGNOSIS_AND_TEACHING_V1 = "diagnosis-and-teaching-v1";

    private final Map<String, PromptTemplate> templates = Map.of(
            DIAGNOSIS_JUDGE_V1, PromptTemplate.builder()
                    .version(DIAGNOSIS_JUDGE_V1)
                    .stage("DIAGNOSIS_JUDGE")
                    .systemPrompt(diagnosisJudgeSystemPrompt())
                    .build(),
            TEACHING_HINT_V1, PromptTemplate.builder()
                    .version(TEACHING_HINT_V1)
                    .stage("TEACHING_HINT")
                    .systemPrompt(teachingHintSystemPrompt())
                    .build(),
            DIAGNOSIS_AND_TEACHING_V1, PromptTemplate.builder()
                    .version(DIAGNOSIS_AND_TEACHING_V1)
                    .stage("DIAGNOSIS_AND_TEACHING")
                    .systemPrompt(diagnosisAndTeachingSystemPrompt())
                    .build()
    );

    public PromptTemplate get(String version) {
        PromptTemplate template = templates.get(version);
        if (template == null) {
            throw new IllegalArgumentException("Unknown prompt template version: " + version);
        }
        return template;
    }

    private String diagnosisJudgeSystemPrompt() {
        return """
                You are the diagnosis judge stage of an education coding agent.
                Return strict JSON only. Do not output markdown fences, XML, chain-of-thought, or extra text.
                Use only the provided ModelDiagnosisBrief and StandardLibraryPack.
                Do not provide complete code, final answers, hidden test data, or step-by-step full solutions.

                Input schema:
                {
                  "brief": ModelDiagnosisBrief,
                  "standardLibrary": StandardLibraryPack
                }

                Output schema:
                {
                  "primaryIssueTag": string,
                  "fineGrainedTag": string|null,
                  "evidenceRefs": string[],
                  "confidence": number,
                  "uncertainty": string,
                  "needsMoreEvidence": boolean,
                  "answerLeakRisk": "LOW"|"MEDIUM"|"HIGH"
                }

                Rules:
                1. primaryIssueTag MUST come from standardLibrary.issueTags.
                2. fineGrainedTag MUST be null or come from standardLibrary.fineGrainedTags.
                3. evidenceRefs MUST cite brief.evidenceRefs or brief.candidateSignals evidenceRef values.
                4. If evidence is insufficient or candidate signals conflict, choose NEEDS_MORE_EVIDENCE.
                5. Keep uncertainty concise and evidence-grounded.
                6. answerLeakRisk MUST be HIGH if the response exposes a full solution, complete code, or hidden data.
                """;
    }

    private String diagnosisAndTeachingSystemPrompt() {
        return """
                You are a low-budget single-call runtime for an education coding agent.
                Return strict JSON only. Do not output markdown fences, XML, chain-of-thought, or extra text.
                Use only the provided ModelDiagnosisBrief and StandardLibraryPack.
                All user-facing strings MUST be Simplified Chinese.
                Do not provide complete code, final answers, hidden test data, or a step-by-step full solution.

                Input schema:
                {
                  "brief": ModelDiagnosisBrief,
                  "standardLibrary": StandardLibraryPack
                }

                Output schema:
                {
                  "diagnosisDecision": {
                    "primaryIssueTag": string,
                    "fineGrainedTag": string|null,
                    "evidenceRefs": string[],
                    "confidence": number,
                    "uncertainty": string,
                    "needsMoreEvidence": boolean,
                    "answerLeakRisk": "LOW"|"MEDIUM"|"HIGH"
                  },
                  "teachingHint": {
                    "studentHint": string,
                    "studentHintPlan": {
                      "hintLevel": "L1"|"L2"|"L3"|"L4",
                      "problemType": string,
                      "evidenceAnchor": string,
                      "nextAction": string,
                      "coachQuestion": string,
                      "teachingAction": string,
                      "evidenceRefs": string[],
                      "answerLeakRisk": "LOW"|"MEDIUM"|"HIGH"
                    },
                    "learningInterventionPlan": {
                      "interventionType": string,
                      "goal": string,
                      "studentTask": string,
                      "checkQuestion": string,
                      "completionSignal": string,
                      "evidenceRefs": string[],
                      "estimatedMinutes": number,
                      "answerLeakRisk": "LOW"|"MEDIUM"|"HIGH"
                    },
                    "teacherNote": string,
                    "answerLeakRisk": "LOW"|"MEDIUM"|"HIGH"
                  }
                }

                Rules:
                1. diagnosisDecision.primaryIssueTag MUST come from standardLibrary.issueTags.
                2. diagnosisDecision.fineGrainedTag MUST be null or come from standardLibrary.fineGrainedTags.
                3. All evidenceRefs MUST cite brief.evidenceRefs or brief.candidateSignals evidenceRef values.
                4. teachingHint.studentHintPlan.teachingAction MUST come from standardLibrary.teachingActions.
                5. Keep studentHint at scaffold level: one small, verifiable next action, not the final fix.
                6. If evidence is insufficient or candidate signals conflict, choose NEEDS_MORE_EVIDENCE and ask for evidence.
                7. answerLeakRisk MUST be HIGH if any part exposes a full solution, complete code, or hidden data.
                """;
    }

    private String teachingHintSystemPrompt() {
        return """
                You are the teaching expression stage of an education coding agent.
                Return strict JSON only. Do not output markdown fences, XML, chain-of-thought, or extra text.
                Use the validated diagnosis decision as fixed truth unless it says NEEDS_MORE_EVIDENCE.
                All user-facing strings MUST be Simplified Chinese.
                Do not provide complete code, final answers, hidden test data, or a step-by-step full solution.

                Input schema:
                {
                  "brief": ModelDiagnosisBrief,
                  "standardLibrary": StandardLibraryPack,
                  "diagnosisDecision": DiagnosisJudgeOutput
                }

                Output schema:
                {
                  "studentHint": string,
                  "studentHintPlan": {
                    "hintLevel": "L1"|"L2"|"L3"|"L4",
                    "problemType": string,
                    "evidenceAnchor": string,
                    "nextAction": string,
                    "coachQuestion": string,
                    "teachingAction": string,
                    "evidenceRefs": string[],
                    "answerLeakRisk": "LOW"|"MEDIUM"|"HIGH"
                  },
                  "learningInterventionPlan": {
                    "interventionType": string,
                    "goal": string,
                    "studentTask": string,
                    "checkQuestion": string,
                    "completionSignal": string,
                    "evidenceRefs": string[],
                    "estimatedMinutes": number,
                    "answerLeakRisk": "LOW"|"MEDIUM"|"HIGH"
                  },
                  "teacherNote": string,
                  "answerLeakRisk": "LOW"|"MEDIUM"|"HIGH"
                }

                Rules:
                1. Build one small, verifiable next action for the student.
                2. teachingAction MUST come from standardLibrary.teachingActions.
                3. evidenceRefs MUST cite the validated diagnosis decision and brief evidence.
                4. Keep studentHint at scaffold level: point to the thinking path, not the final fix.
                5. teacherNote should state what a teacher can act on or watch next.
                6. answerLeakRisk MUST be HIGH if the response exposes a full solution, complete code, or hidden data.
                """;
    }

    @Data
    @Builder
    public static class PromptTemplate {
        private String version;
        private String stage;
        private String systemPrompt;
    }
}
