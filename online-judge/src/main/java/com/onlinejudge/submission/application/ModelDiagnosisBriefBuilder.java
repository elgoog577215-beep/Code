package com.onlinejudge.submission.application;

import com.onlinejudge.submission.dto.SubmissionAnalysisResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class ModelDiagnosisBriefBuilder {

    private static final int MAX_PROBLEM_BRIEF_LENGTH = 600;
    private static final int MAX_CODE_EXCERPT_LENGTH = 4000;
    private static final int MAX_CASE_FACTS = 5;

    public ModelDiagnosisBrief build(DiagnosisEvidencePackage evidencePackage,
                                     RuleSignalAnalyzer.RuleSignalResult ruleSignals,
                                     SubmissionAnalysisResponse baseline) {
        DiagnosisEvidencePackage.ProblemEvidence problem = evidencePackage == null ? null : evidencePackage.getProblem();
        DiagnosisEvidencePackage.SubmissionEvidence submission = evidencePackage == null ? null : evidencePackage.getSubmission();
        DiagnosisEvidencePackage.JudgeFacts judgeFacts = evidencePackage == null ? null : evidencePackage.getJudgeFacts();

        List<ModelDiagnosisBrief.CandidateSignal> candidateSignals = toCandidateSignals(ruleSignals);
        List<String> evidenceRefs = collectEvidenceRefs(ruleSignals, candidateSignals, judgeFacts, baseline);

        return ModelDiagnosisBrief.builder()
                .schemaVersion(ModelDiagnosisBrief.SCHEMA_VERSION)
                .problemBrief(problemBrief(problem))
                .problemConstraints(problemConstraints(problem))
                .verdict(submission == null ? "UNKNOWN" : defaultIfBlank(submission.getVerdict(), "UNKNOWN"))
                .language(submission == null ? "" : defaultIfBlank(submission.getLanguage(), ""))
                .keyCodeExcerpt(codeExcerpt(submission))
                .sourceCodeLineCount(submission == null ? null : submission.getSourceCodeLineCount())
                .firstFailedCase(sanitizedFirstFailedCase(judgeFacts))
                .visibleCaseFacts(visibleCaseFacts(judgeFacts))
                .candidateSignals(candidateSignals)
                .evidenceRefs(evidenceRefs)
                .allowedIssueTags(allowedIssueTags(ruleSignals, baseline))
                .allowedFineGrainedTags(allowedFineTags(ruleSignals, baseline))
                .learningTrajectorySummary(learningTrajectorySummary(evidencePackage, baseline))
                .hiddenDataBoundary(hiddenDataBoundary(judgeFacts))
                .uncertainty(uncertainty(judgeFacts, ruleSignals))
                .build();
    }

    private String problemBrief(DiagnosisEvidencePackage.ProblemEvidence problem) {
        if (problem == null) {
            return "";
        }
        return truncate(String.join("\n",
                defaultIfBlank(problem.getTitle(), ""),
                defaultIfBlank(problem.getAiPromptDirection(), ""),
                defaultIfBlank(problem.getDescription(), "")), MAX_PROBLEM_BRIEF_LENGTH);
    }

    private String problemConstraints(DiagnosisEvidencePackage.ProblemEvidence problem) {
        if (problem == null) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        if (problem.getDifficulty() != null && !problem.getDifficulty().isBlank()) {
            parts.add("difficulty=" + problem.getDifficulty());
        }
        if (problem.getTimeLimit() != null) {
            parts.add("timeLimitMs=" + problem.getTimeLimit());
        }
        if (problem.getMemoryLimit() != null) {
            parts.add("memoryLimitKb=" + problem.getMemoryLimit());
        }
        addNamedList(parts, "knowledgePoints", problem.getKnowledgePoints());
        addNamedList(parts, "algorithmStrategies", problem.getAlgorithmStrategies());
        addNamedList(parts, "commonMistakes", problem.getCommonMistakes());
        addNamedList(parts, "boundaryTypes", problem.getBoundaryTypes());
        return String.join("; ", parts);
    }

    private String codeExcerpt(DiagnosisEvidencePackage.SubmissionEvidence submission) {
        if (submission == null) {
            return "";
        }
        String numbered = defaultIfBlank(submission.getSourceCodeWithLineNumbers(), "");
        String source = defaultIfBlank(submission.getSourceCode(), "");
        return truncate(numbered.isBlank() ? source : numbered, MAX_CODE_EXCERPT_LENGTH);
    }

    private SubmissionAnalysisResponse.FailedCaseSnapshot sanitizedFirstFailedCase(DiagnosisEvidencePackage.JudgeFacts judgeFacts) {
        if (judgeFacts == null || judgeFacts.getFirstFailedCase() == null) {
            return null;
        }
        SubmissionAnalysisResponse.FailedCaseSnapshot source = judgeFacts.getFirstFailedCase();
        if (source.isHidden()) {
            return SubmissionAnalysisResponse.FailedCaseSnapshot.builder()
                    .testCaseNumber(source.getTestCaseNumber())
                    .hidden(true)
                    .input(null)
                    .expectedOutput(null)
                    .actualOutput(null)
                    .build();
        }
        return source;
    }

    private List<ModelDiagnosisBrief.VisibleCaseFact> visibleCaseFacts(DiagnosisEvidencePackage.JudgeFacts judgeFacts) {
        if (judgeFacts == null || judgeFacts.getCaseResultsSummary() == null) {
            return List.of();
        }
        return judgeFacts.getCaseResultsSummary().stream()
                .limit(MAX_CASE_FACTS)
                .map(item -> ModelDiagnosisBrief.VisibleCaseFact.builder()
                        .testCaseNumber(item.getTestCaseNumber())
                        .passed(item.getPassed())
                        .hidden(item.getHidden())
                        .executionTime(item.getExecutionTime())
                        .memoryUsed(item.getMemoryUsed())
                        .actualOutputPreview(Boolean.TRUE.equals(item.getHidden()) ? null : item.getActualOutputPreview())
                        .expectedOutputPreview(Boolean.TRUE.equals(item.getHidden()) ? null : item.getExpectedOutputPreview())
                        .build())
                .toList();
    }

    private List<ModelDiagnosisBrief.CandidateSignal> toCandidateSignals(RuleSignalAnalyzer.RuleSignalResult ruleSignals) {
        if (ruleSignals == null || ruleSignals.getSignals() == null) {
            return List.of();
        }
        return ruleSignals.getSignals().stream()
                .map(signal -> ModelDiagnosisBrief.CandidateSignal.builder()
                        .evidenceRef(signal.getEvidenceRef())
                        .issueTag(signal.getCoarseTag())
                        .fineGrainedTag(signal.getFineTag())
                        .confidence(signal.getConfidence())
                        .reason(signal.getMessage())
                        .build())
                .toList();
    }

    private List<String> collectEvidenceRefs(RuleSignalAnalyzer.RuleSignalResult ruleSignals,
                                             List<ModelDiagnosisBrief.CandidateSignal> candidateSignals,
                                             DiagnosisEvidencePackage.JudgeFacts judgeFacts,
                                             SubmissionAnalysisResponse baseline) {
        Set<String> refs = new LinkedHashSet<>();
        if (ruleSignals != null && ruleSignals.getEvidenceRefs() != null) {
            refs.addAll(ruleSignals.getEvidenceRefs());
        }
        candidateSignals.stream()
                .map(ModelDiagnosisBrief.CandidateSignal::getEvidenceRef)
                .filter(ref -> ref != null && !ref.isBlank())
                .forEach(refs::add);
        if (judgeFacts != null && judgeFacts.getFirstFailedCase() != null) {
            refs.add("judge:first_failed_case");
        }
        if (judgeFacts != null && Boolean.TRUE.equals(judgeFacts.getHiddenFailureObserved())) {
            refs.add("judge:hidden_failure");
        }
        if (baseline != null && baseline.getEvidenceRefs() != null) {
            refs.addAll(baseline.getEvidenceRefs());
        }
        return List.copyOf(refs);
    }

    private List<String> allowedIssueTags(RuleSignalAnalyzer.RuleSignalResult ruleSignals,
                                          SubmissionAnalysisResponse baseline) {
        Set<String> tags = new LinkedHashSet<>();
        if (ruleSignals != null && ruleSignals.getCandidateIssueTags() != null) {
            tags.addAll(ruleSignals.getCandidateIssueTags());
        }
        if (baseline != null && baseline.getIssueTags() != null) {
            tags.addAll(baseline.getIssueTags());
        }
        tags.add("NEEDS_MORE_EVIDENCE");
        return List.copyOf(tags);
    }

    private List<String> allowedFineTags(RuleSignalAnalyzer.RuleSignalResult ruleSignals,
                                         SubmissionAnalysisResponse baseline) {
        Set<String> tags = new LinkedHashSet<>();
        if (ruleSignals != null && ruleSignals.getCandidateFineGrainedTags() != null) {
            tags.addAll(ruleSignals.getCandidateFineGrainedTags());
        }
        if (baseline != null && baseline.getFineGrainedTags() != null) {
            tags.addAll(baseline.getFineGrainedTags());
        }
        return List.copyOf(tags);
    }

    private String learningTrajectorySummary(DiagnosisEvidencePackage evidencePackage,
                                             SubmissionAnalysisResponse baseline) {
        if (baseline != null && baseline.getLearningTrajectorySignal() != null) {
            SubmissionAnalysisResponse.LearningTrajectorySignal signal = baseline.getLearningTrajectorySignal();
            return String.join(" | ",
                    defaultIfBlank(signal.getPhase(), ""),
                    defaultIfBlank(signal.getSummary(), ""),
                    defaultIfBlank(signal.getNextFocus(), ""));
        }
        DiagnosisEvidencePackage.HistoryEvidence history = evidencePackage == null ? null : evidencePackage.getHistory();
        if (history == null) {
            return "";
        }
        return String.join(" | ",
                "previousVerdict=" + defaultIfBlank(history.getPreviousVerdict(), "UNKNOWN"),
                "repeatedIssueTag=" + defaultIfBlank(history.getRepeatedIssueTag(), ""),
                "transition=" + defaultIfBlank(history.getTransitionSignal(), ""));
    }

    private ModelDiagnosisBrief.HiddenDataBoundary hiddenDataBoundary(DiagnosisEvidencePackage.JudgeFacts judgeFacts) {
        boolean hiddenFailureObserved = judgeFacts != null && Boolean.TRUE.equals(judgeFacts.getHiddenFailureObserved());
        return ModelDiagnosisBrief.HiddenDataBoundary.builder()
                .hiddenFailureObserved(hiddenFailureObserved)
                .hiddenInputVisible(false)
                .policy(hiddenFailureObserved
                        ? "Hidden judge data is not visible. Do not infer exact hidden inputs or outputs."
                        : "Use only visible judge facts and provided code evidence.")
                .build();
    }

    private String uncertainty(DiagnosisEvidencePackage.JudgeFacts judgeFacts,
                               RuleSignalAnalyzer.RuleSignalResult ruleSignals) {
        if (ruleSignals == null || ruleSignals.getSignals() == null || ruleSignals.getSignals().isEmpty()) {
            return "No strong local rule signal is available; model must stay within evidence and can choose NEEDS_MORE_EVIDENCE.";
        }
        if (judgeFacts != null && Boolean.TRUE.equals(judgeFacts.getHiddenFailureObserved())) {
            return "Hidden failure is observed, but hidden test data is unavailable; do not guess hidden data.";
        }
        return "Use candidate signals as bounded evidence; cite evidenceRefs instead of inventing facts.";
    }

    private void addNamedList(List<String> parts, String name, List<String> values) {
        if (values != null && !values.isEmpty()) {
            parts.add(name + "=" + String.join(",", values));
        }
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value == null ? "" : value;
        }
        return value.substring(0, Math.max(0, maxLength - 24)) + "\n...[truncated for model]";
    }
}
