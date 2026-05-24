package com.onlinejudge.submission.application;

import lombok.Builder;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PromptTemplateRegistry {

    public static final String DIAGNOSIS_JUDGE_V1 = "diagnosis-judge-v1";
    public static final String DIAGNOSIS_JUDGE_V2 = "diagnosis-judge-v2";
    public static final String TEACHING_HINT_V1 = "teaching-hint-v1";
    public static final String DIAGNOSIS_AND_TEACHING_V1 = "diagnosis-and-teaching-v1";
    public static final String DIAGNOSIS_AND_TEACHING_V2 = "diagnosis-and-teaching-v2";

    private final Map<String, PromptTemplate> templates = Map.of(
            DIAGNOSIS_JUDGE_V1, PromptTemplate.builder()
                    .version(DIAGNOSIS_JUDGE_V1)
                    .stage("DIAGNOSIS_JUDGE")
                    .systemPrompt(diagnosisJudgeSystemPrompt())
                    .build(),
            DIAGNOSIS_JUDGE_V2, PromptTemplate.builder()
                    .version(DIAGNOSIS_JUDGE_V2)
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
                    .build(),
            DIAGNOSIS_AND_TEACHING_V2, PromptTemplate.builder()
                    .version(DIAGNOSIS_AND_TEACHING_V2)
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
                4. Follow standardLibrary.decisionProtocol before choosing tags.
                5. Select the most evidence-supported tag, not the most common or most severe tag.
                6. Use fineGrainedTag only when direct evidence distinguishes it from the parent issue.
                7. Prefer the highest-confidence candidateSignals that directly explain firstFailedCase or concrete problem/source evidence.
                8. Do not choose NEEDS_MORE_EVIDENCE merely because hidden data is unavailable when visible evidence supports a narrower tag.
                9. For medium-length code, ignore helper-method distractions and diagnose the behavior that changes output or verdict.
                10. If evidence is insufficient or candidate signals conflict without a stronger concrete signal, choose NEEDS_MORE_EVIDENCE.
                11. Keep uncertainty concise and evidence-grounded.
                12. answerLeakRisk MUST be HIGH only if the response exposes a full solution, complete code, hidden data, replacement loop header, transition formula, or executable control structure; otherwise use LOW or MEDIUM.
                13. Do not include replacement loop headers, transition formulas, or executable control structures.
                14. If brief.learningTrajectorySummary includes previousIntervention/actionStatus, use it only as learning-process evidence: OBSERVED means review or transfer can be appropriate; PARTIALLY_OBSERVED means keep direction but shrink the next observable task; CONTRADICTED means lower hint granularity or suggest teacher attention; NOT_OBSERVED means do not assume the student executed the action.
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
                4. Follow standardLibrary.decisionProtocol before choosing diagnosisDecision tags.
                5. Select the most evidence-supported tag, not the most common or most severe tag.
                6. Use diagnosisDecision.fineGrainedTag only when direct evidence distinguishes it from the parent issue.
                7. Prefer the highest-confidence candidateSignals that directly explain firstFailedCase or concrete problem/source evidence.
                8. Do not choose NEEDS_MORE_EVIDENCE merely because hidden data is unavailable when visible evidence supports a narrower tag.
                9. For medium-length code, ignore helper-method distractions and diagnose the behavior that changes output or verdict.
                10. teachingHint.studentHintPlan.teachingAction MUST come from standardLibrary.teachingActions.
                11. Keep studentHint at scaffold level: one small, verifiable next action, not the final fix.
                12. studentHintPlan.evidenceAnchor MUST name the concrete evidence used for the diagnosis.
                13. If evidence is insufficient or candidate signals conflict without a stronger concrete signal, choose NEEDS_MORE_EVIDENCE and ask for evidence.
                14. answerLeakRisk MUST be HIGH only if any part exposes a full solution, complete code, hidden data, replacement loop header, transition formula, or executable control structure; otherwise use LOW or MEDIUM.
                15. Do not include replacement loop headers, transition formulas, or executable control structures.
                16. For input-format issues, ask the student to compare required input lines with actual read operations instead of naming the exact loop to add.
                17. If actual and expected output differ only by whitespace or casing, prioritize OUTPUT_FORMAT_DETAIL and ask for character-level comparison before algorithm changes.
                18. For large-bound TLE or over-simulation evidence, ask the student to estimate operation count at the maximum input; do not name the replacement algorithm, data structure, formula, sqrt bound, or math trick.
                19. For hidden failures after public samples pass, do not guess hidden data; ask the student to construct a small counterexample that differs from the sample structure.
                20. For in-place state progress, ask the student to trace the current position after one mutation and check the invariant; do not provide a replacement while loop.
                21. For empty or minimum input evidence, ask the student to trace values through the existing functions on the minimum case; do not provide the guard condition.
                22. If brief.learningTrajectorySummary includes previousIntervention/actionStatus, adapt teachingHint to that status: OBSERVED -> review/generalize; PARTIALLY_OBSERVED -> preserve direction and make the task more checkable; CONTRADICTED -> ask for a smaller observable artifact and consider teacher attention; NOT_OBSERVED -> ask for observable evidence rather than judging execution.
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
                5. studentHintPlan.evidenceAnchor MUST name one concrete source, state, input-output, or counterexample anchor.
                6. teacherNote should state what a teacher can act on or watch next.
                7. answerLeakRisk MUST be HIGH only if the response exposes a full solution, complete code, hidden data, replacement loop header, transition formula, or executable control structure; otherwise use LOW or MEDIUM.
                8. Do not include replacement loop headers, transition formulas, or executable control structures.
                9. For input-format issues, ask the student to compare required input lines with actual read operations instead of naming the exact loop to add.
                10. For output-format issues, ask for character-level output comparison before discussing algorithms.
                11. For large-bound complexity issues, ask for maximum-scale operation counting without naming the optimized method or formula.
                12. For hidden failures, state that hidden data is unavailable and ask for a self-made counterexample instead of guessing the hidden case.
                13. For in-place state progress, ask whether the value newly moved to the current position has been processed to a stable invariant.
                14. For empty/minimum input, ask what each existing function receives and returns on that minimum input.
                15. Use previousIntervention/actionStatus from brief.learningTrajectorySummary as learning-process evidence only. Never claim the student completed an action unless actionStatus is OBSERVED or PARTIALLY_OBSERVED.
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
