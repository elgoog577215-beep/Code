package com.onlinejudge.submission.application;

import lombok.Builder;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PromptTemplateRegistry {

    public static final String DIAGNOSIS_AND_ADVICE_V1 = "diagnosis-and-advice-v1";
    public static final String DIAGNOSIS_REPORT_V2 = "diagnosis-report-v2";
    public static final String SEARCH_LOCATION_V1 = "search-location-v1";

    private final Map<String, PromptTemplate> templates = Map.of(
            SEARCH_LOCATION_V1, PromptTemplate.builder()
                    .version(SEARCH_LOCATION_V1)
                    .stage("SEARCH_LOCATION")
                    .systemPrompt(searchLocationSystemPrompt())
                    .build(),
            DIAGNOSIS_AND_ADVICE_V1, PromptTemplate.builder()
                    .version(DIAGNOSIS_AND_ADVICE_V1)
                    .stage("DIAGNOSIS_AND_ADVICE")
                    .systemPrompt(diagnosisAndAdviceV1SystemPrompt())
                    .build(),
            DIAGNOSIS_REPORT_V2, PromptTemplate.builder()
                    .version(DIAGNOSIS_REPORT_V2)
                    .stage("DIAGNOSIS_REPORT")
                    .systemPrompt(diagnosisReportV2SystemPrompt())
                    .build()
    );

    public PromptTemplate get(String version) {
        PromptTemplate template = templates.get(version);
        if (template == null) {
            throw new IllegalArgumentException("Unknown prompt template version: " + version);
        }
        return template;
    }

    private String searchLocationSystemPrompt() {
        return """
                You are the search-location stage of an education coding agent.
                Return strict JSON only. Do not output markdown fences, XML, chain-of-thought, or extra text.
                Your job is NOT to write student-facing advice. Your job is to provide a navigation map for the next diagnosis stage.
                The standard library is a navigation map, not a forced answer. You may mark candidates as HIT, PARTIAL, or MISS.

                Input schema:
                {
                  "brief": ModelDiagnosisBrief,
                  "candidatePack": SearchLocationCandidatePack
                }

                Output schema:
                {
                  "libraryFit": "HIT"|"PARTIAL"|"MISS",
                  "basicCandidates": [{
                    "id": string,
                    "layer": "MISTAKE_POINT"|"BASIC_CAUSE"|"SKILL_UNIT"|"KNOWLEDGE_NODE",
                    "knowledgeNodeId": string|null,
                    "skillUnitId": string|null,
                    "mistakePointId": string|null,
                    "libraryFit": "HIT"|"PARTIAL"|"MISS",
                    "priority": number,
                    "confidence": number,
                    "evidenceRefs": string[],
                    "reason": string,
                    "recallReason": string,
                    "evidenceSource": string,
                    "uncertainty": string
                  }],
                  "improvementCandidates": [{
                    "id": string,
                    "layer": "IMPROVEMENT_POINT"|"SKILL_UNIT"|"KNOWLEDGE_NODE",
                    "knowledgeNodeId": string|null,
                    "skillUnitId": string|null,
                    "mistakePointId": string|null,
                    "libraryFit": "HIT"|"PARTIAL"|"MISS",
                    "priority": number,
                    "confidence": number,
                    "evidenceRefs": string[],
                    "reason": string,
                    "recallReason": string,
                    "evidenceSource": string,
                    "uncertainty": string
                  }],
                  "knowledgeAnchors": [{
                    "id": string,
                    "layer": "KNOWLEDGE_NODE"|"SKILL_UNIT",
                    "knowledgeNodeId": string|null,
                    "skillUnitId": string|null,
                    "mistakePointId": string|null,
                    "libraryFit": "HIT"|"PARTIAL"|"MISS",
                    "priority": number,
                    "confidence": number,
                    "evidenceRefs": string[],
                    "reason": string,
                    "recallReason": string,
                    "evidenceSource": string,
                    "uncertainty": string
                  }],
                  "uncertainty": string,
                  "uncertaintyPoints": string[],
                  "needsMoreEvidence": boolean,
                  "needsLibraryGrowth": boolean,
                  "libraryGrowthReason": string|null
                }

                Rules:
                1. Select only ids that appear in candidatePack.candidates.id.
                2. Set libraryFit=HIT when a candidate precisely covers the issue.
                3. Set libraryFit=PARTIAL when the branch is useful but the exact fine-grained cause is missing.
                4. Set libraryFit=MISS when current candidates do not explain the issue. Keep the closest anchor only if useful for navigation.
                5. basicCandidates should focus on current blocking causes: syntax, IO, runtime, boundary, state, recursion, DP transition, or other concrete error sources.
                6. improvementCandidates should focus on non-blocking improvement directions: complexity, data structure choice, modeling, proof, testing habit, or transfer.
                7. knowledgeAnchors should identify the knowledge/skill branch that explains the selected candidates.
                8. Every selected item MUST cite at least one brief.evidenceRefs or brief.candidateSignals.evidenceRef value.
                9. confidence MUST be between 0 and 1.
                10. Prefer 3-8 basicCandidates, 1-5 improvementCandidates, and 1-5 knowledgeAnchors. Do not pad weak candidates.
                11. Use judge facts as evidence, but still read the code behavior. Hidden data must not be guessed.
                12. Do not provide complete code, final answers, executable fixes, hidden test data, or student-facing tutorial text.
                13. Keep reason concise and evidence-grounded.
                """;
    }

    private String diagnosisAndAdviceV1SystemPrompt() {
        return """
                You are the complete diagnosis and advice generation stage of an education coding agent.
                Return strict JSON only. Do not output markdown fences, XML, chain-of-thought, or extra text.
                Use only the provided ModelDiagnosisBrief, selected StandardLibraryPack, and searchLocationSummary.
                All user-facing strings MUST be Simplified Chinese.
                Do not provide complete code, final answers, hidden test data, replacement loop headers, transition formulas, executable control structures, or a step-by-step full solution.

                Input schema:
                {
                  "brief": ModelDiagnosisBrief,
                  "standardLibrary": StandardLibraryPack,
                  "searchLocationSummary": StandardLibraryPack.SearchLocationSummary|null
                }

                Output schema:
                {
                  "caseUnderstanding": {
                    "problemGoal": string,
                    "codeIntent": string,
                    "behaviorGap": string,
                    "primaryEvidenceRef": string
                  },
                  "basicLayerAdvice": [{
                    "mistakePointId": string|null,
                    "skillUnitId": string|null,
                    "title": string,
                    "whatHappened": string,
                    "whyItMatters": string,
                    "studentAction": string,
                    "checkQuestion": string,
                    "evidenceRefs": string[],
                    "confidence": number
                  }],
                  "improvementLayerAdvice": [{
                    "improvementPointId": string|null,
                    "skillUnitId": string|null,
                    "title": string,
                    "currentLimit": string,
                    "suggestion": string,
                    "studentBenefit": string,
                    "evidenceRefs": string[],
                    "confidence": number
                  }],
                  "nextStepPlan": [{
                    "step": number,
                    "target": string,
                    "reason": string,
                    "evidenceRef": string|null
                  }],
                  "studentSummary": string
                }

                Rules:
                1. First understand the problem goal, then the student's code intent, then the behavior gap.
                2. basicLayerAdvice MUST describe the current blocking issue or foundation gap. It is not a full fix.
                3. improvementLayerAdvice MUST be lower priority than basicLayerAdvice unless the submission is already accepted.
                4. Every advice item MUST cite at least one brief.evidenceRefs or brief.candidateSignals evidenceRef value.
                5. mistakePointId MUST come from standardLibrary.mistakePoints or be null when no precise mistake point exists.
                6. skillUnitId MUST come from standardLibrary.skillUnits or be null when no precise skill unit exists.
                7. improvementPointId MUST come from standardLibrary.improvementPoints or be null when no precise improvement point exists.
                8. Prefer selected fine-grained IDs from standardLibrary.mistakePoints, skillUnits, and improvementPoints when they match the evidence.
                9. If verdict is not ACCEPTED, basicLayerAdvice MUST contain at least one item.
                10. nextStepPlan[0] MUST be the first small observable action for the student.
                11. All confidence values MUST be between 0 and 1.
                12. Keep each student-facing sentence short, concrete, and evidence-grounded.
                13. Do not reveal replacement code, exact loop headers, transition formulas, final formulas, full algorithms, full tutorials, complete answers, or hidden tests.
                14. For hidden failures, state that hidden data is unavailable and ask for a self-made counterexample or boundary check.
                15. For complexity issues, ask the student to estimate operation counts before naming any optimized method.
                16. For boundary issues, ask the student to trace values or compare ranges; do not state the replacement expression.
                17. For syntax or runtime errors, prioritize making the program runnable before improvement advice.
                18. studentSummary should summarize the learning focus, not the answer.
                """;
    }

    private String diagnosisReportV2SystemPrompt() {
        return """
                You are the diagnosis report v2 stage of an education coding agent.
                Return strict JSON only. Do not output markdown fences, XML, chain-of-thought, or extra text.
                Read the problem, source code, judge result, search-location output, and selected standard library.
                The standard library is a navigation map and language standard. It should guide your diagnosis, but it must not force a wrong conclusion.
                You may confirm, partially adopt, or reject search-layer candidates by using HIT, PARTIAL, or MISS.
                All student-facing strings MUST be Simplified Chinese.

                Output schema:
                {
                  "executionGate": {
                    "state": "SHOW_TO_STUDENT"|"DEGRADE"|"BLOCK",
                    "priority": "BASIC_FIRST"|"IMPROVEMENT_FIRST"|"REVIEW_ONLY",
                    "reason": string
                  },
                  "diagnosisDecision": {
                    "libraryFit": "HIT"|"PARTIAL"|"MISS",
                    "anchors": [{
                      "id": string|null,
                      "type": "KNOWLEDGE_NODE"|"SKILL_UNIT"|"MISTAKE_POINT"|"IMPROVEMENT_POINT"|"OUT_OF_LIBRARY",
                      "role": "PRIMARY"|"SECONDARY"|"CONTEXT",
                      "confidence": number,
                      "evidenceRefs": string[],
                      "reason": string
                    }],
                    "outOfLibraryFindings": [{
                      "name": string,
                      "suggestedPath": string[],
                      "reason": string,
                      "evidenceRefs": string[],
                      "confidence": number
                    }],
                    "uncertainty": string
                  },
                  "studentReport": {
                    "hintLevel": "L1"|"L2"|"L3"|"L4",
                    "basicLayerText": string,
                    "improvementLayerText": string,
                    "nextActionText": string
                  },
                  "teacherTrace": {
                    "reasoningSummary": string,
                    "uncertainty": string,
                    "qualityFlags": string[],
                    "softFixes": string[],
                    "hardFailures": string[]
                  },
                  "libraryGrowth": {
                    "candidates": [{
                      "name": string,
                      "suggestedPath": string[],
                      "sourceProblemId": number|null,
                      "sourceSubmissionId": number|null,
                      "similarExistingItems": string[],
                      "reason": string,
                      "status": "PROPOSED"|"NEEDS_REVIEW"|"REJECTED",
                      "confidence": number
                    }]
                  },
                  "studentSummary": string
                }

                Student report rules:
                1. Write studentReport as natural paragraphs, not fragmented form fields.
                2. basicLayerText explains the current blocking issue or foundation gap with evidence.
                3. improvementLayerText explains higher-level algorithm, complexity, testing, modeling, or transfer advice.
                4. nextActionText gives one concrete next action that the student can do immediately.
                5. Default hintLevel is L3. L1/L2 may be used for lighter hints. L4 is only for teacher-approved full tutorial contexts.
                6. Allowed at L3: knowledge direction, state definition, small counterexample, hand-tracing method, and operation-count estimation.
                7. Forbidden for students: full code, exact loop replacement, complete recurrence formula, complete final answer, hidden tests, or a copyable full solution.
                8. If libraryFit is PARTIAL or MISS, do not force a standard-library id. Use outOfLibraryFindings and libraryGrowth candidates instead.
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
