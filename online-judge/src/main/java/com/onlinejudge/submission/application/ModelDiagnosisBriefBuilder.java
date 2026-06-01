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

        List<ModelDiagnosisBrief.CandidateSignal> candidateSignals = new ArrayList<>(toCandidateSignals(ruleSignals));
        candidateSignals.addAll(toMemoryCandidateSignals(evidencePackage));
        List<String> evidenceRefs = collectEvidenceRefs(ruleSignals, candidateSignals, judgeFacts, evidencePackage, baseline);

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
                .allowedIssueTags(allowedIssueTags(ruleSignals, evidencePackage, baseline))
                .allowedFineGrainedTags(allowedFineTags(ruleSignals, evidencePackage, baseline))
                .learningTrajectorySummary(learningTrajectorySummary(evidencePackage, baseline))
                .learningMemorySummary(learningMemorySummary(evidencePackage))
                .teacherCalibrationSummary(teacherCalibrationSummary(evidencePackage))
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

    private List<ModelDiagnosisBrief.CandidateSignal> toMemoryCandidateSignals(DiagnosisEvidencePackage evidencePackage) {
        DiagnosisEvidencePackage.StudentLearningMemorySnapshot memory =
                evidencePackage == null ? null : evidencePackage.getLearningMemory();
        if (memory == null) {
            return List.of();
        }
        List<ModelDiagnosisBrief.CandidateSignal> signals = new ArrayList<>();
        if (memory.getRecurringFineGrainedTags() != null) {
            memory.getRecurringFineGrainedTags().stream()
                    .limit(3)
                    .forEach(stat -> signals.add(ModelDiagnosisBrief.CandidateSignal.builder()
                            .evidenceRef("memory:recurring_fine:" + stat.getTag())
                            .issueTag(null)
                            .fineGrainedTag(stat.getTag())
                            .confidence(memoryConfidence(stat.getCount()))
                            .reason("Student memory shows repeated fine-grained issue. Use only as auxiliary evidence after current submission facts.")
                            .build()));
        }
        if (memory.getRecurringIssueTags() != null) {
            memory.getRecurringIssueTags().stream()
                    .limit(3)
                    .forEach(stat -> signals.add(ModelDiagnosisBrief.CandidateSignal.builder()
                            .evidenceRef("memory:recurring_issue:" + stat.getTag())
                            .issueTag(stat.getTag())
                            .fineGrainedTag(null)
                            .confidence(memoryConfidence(stat.getCount()))
                            .reason("Student memory shows repeated issue tag. Use only as auxiliary evidence; current submission evidence has higher priority.")
                            .build()));
        }
        if (memory.getTeacherCalibrationPatterns() != null) {
            memory.getTeacherCalibrationPatterns().stream()
                    .limit(3)
                    .forEach(pattern -> signals.add(ModelDiagnosisBrief.CandidateSignal.builder()
                            .evidenceRef(firstEvidenceRef(pattern))
                            .issueTag(blankToNull(pattern.getCorrectedIssueTag()))
                            .fineGrainedTag(blankToNull(pattern.getCorrectedFineGrainedTag()))
                            .confidence(teacherCalibrationConfidence(pattern))
                            .reason("Teacher calibration pattern: previous AI diagnosis was corrected by teacher. Use as auxiliary constraint; current submission facts still have priority.")
                            .build()));
        }
        return signals;
    }

    private double memoryConfidence(Long count) {
        long safeCount = count == null ? 0L : count;
        if (safeCount >= 4) {
            return 0.58;
        }
        if (safeCount >= 3) {
            return 0.52;
        }
        return 0.46;
    }

    private List<String> collectEvidenceRefs(RuleSignalAnalyzer.RuleSignalResult ruleSignals,
                                             List<ModelDiagnosisBrief.CandidateSignal> candidateSignals,
                                             DiagnosisEvidencePackage.JudgeFacts judgeFacts,
                                             DiagnosisEvidencePackage evidencePackage,
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
        DiagnosisEvidencePackage.StudentLearningMemorySnapshot memory =
                evidencePackage == null ? null : evidencePackage.getLearningMemory();
        if (memory != null && memory.getEvidenceRefs() != null) {
            refs.addAll(memory.getEvidenceRefs());
        }
        return List.copyOf(refs);
    }

    private List<String> allowedIssueTags(RuleSignalAnalyzer.RuleSignalResult ruleSignals,
                                          DiagnosisEvidencePackage evidencePackage,
                                          SubmissionAnalysisResponse baseline) {
        Set<String> tags = new LinkedHashSet<>();
        if (ruleSignals != null && ruleSignals.getCandidateIssueTags() != null) {
            tags.addAll(ruleSignals.getCandidateIssueTags());
        }
        if (baseline != null && baseline.getIssueTags() != null) {
            tags.addAll(baseline.getIssueTags());
        }
        DiagnosisEvidencePackage.StudentLearningMemorySnapshot memory =
                evidencePackage == null ? null : evidencePackage.getLearningMemory();
        if (memory != null && memory.getRecurringIssueTags() != null) {
            memory.getRecurringIssueTags().stream()
                    .map(DiagnosisEvidencePackage.MemoryTagStat::getTag)
                    .filter(tag -> tag != null && !tag.isBlank())
                    .forEach(tags::add);
        }
        addTeacherCalibrationIssueTags(tags, memory);
        tags.add("NEEDS_MORE_EVIDENCE");
        return List.copyOf(tags);
    }

    private List<String> allowedFineTags(RuleSignalAnalyzer.RuleSignalResult ruleSignals,
                                         DiagnosisEvidencePackage evidencePackage,
                                         SubmissionAnalysisResponse baseline) {
        Set<String> tags = new LinkedHashSet<>();
        if (ruleSignals != null && ruleSignals.getCandidateFineGrainedTags() != null) {
            tags.addAll(ruleSignals.getCandidateFineGrainedTags());
        }
        if (baseline != null && baseline.getFineGrainedTags() != null) {
            tags.addAll(baseline.getFineGrainedTags());
        }
        DiagnosisEvidencePackage.StudentLearningMemorySnapshot memory =
                evidencePackage == null ? null : evidencePackage.getLearningMemory();
        if (memory != null && memory.getRecurringFineGrainedTags() != null) {
            memory.getRecurringFineGrainedTags().stream()
                    .map(DiagnosisEvidencePackage.MemoryTagStat::getTag)
                    .filter(tag -> tag != null && !tag.isBlank())
                    .forEach(tags::add);
        }
        addTeacherCalibrationFineTags(tags, memory);
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
                "transition=" + defaultIfBlank(history.getTransitionSignal(), ""),
                learningActionFeedbackSummary(history));
    }

    private String learningActionFeedbackSummary(DiagnosisEvidencePackage.HistoryEvidence history) {
        if (history == null || isBlank(history.getPreviousInterventionType())) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        parts.add("previousIntervention=" + defaultIfBlank(history.getPreviousInterventionType(), ""));
        if (!isBlank(history.getPreviousLearningActionStatus())) {
            parts.add("actionStatus=" + history.getPreviousLearningActionStatus());
        }
        if (history.getPreviousLearningActionConfidence() != null) {
            parts.add("actionConfidence=" + String.format("%.2f", history.getPreviousLearningActionConfidence()));
        }
        if (!isBlank(history.getPreviousLearningActionSummary())) {
            parts.add("actionEvidence=" + truncate(history.getPreviousLearningActionSummary(), 220));
        }
        if (!isBlank(history.getPreviousLearningActionNextAdjustment())) {
            parts.add("nextAdjustment=" + truncate(history.getPreviousLearningActionNextAdjustment(), 180));
        }
        return String.join(" | ", parts);
    }

    private String learningMemorySummary(DiagnosisEvidencePackage evidencePackage) {
        DiagnosisEvidencePackage.StudentLearningMemorySnapshot memory =
                evidencePackage == null ? null : evidencePackage.getLearningMemory();
        if (memory == null) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        if (memory.getObservedSubmissionCount() != null) {
            parts.add("observedSubmissions=" + memory.getObservedSubmissionCount());
        }
        if (memory.getObservedProblemCount() != null) {
            parts.add("observedProblems=" + memory.getObservedProblemCount());
        }
        addMemoryStats(parts, "recurringIssueTags", memory.getRecurringIssueTags());
        addMemoryStats(parts, "recurringFineTags", memory.getRecurringFineGrainedTags());
        addAbilityFocus(parts, memory.getAbilityFocus());
        if (memory.getRecentTrend() != null && !memory.getRecentTrend().isBlank()) {
            parts.add("recentTrend=" + memory.getRecentTrend());
        }
        if (memory.getInterventionEffect() != null && !memory.getInterventionEffect().isBlank()) {
            parts.add("interventionEffect=" + memory.getInterventionEffect());
        }
        if (memory.getTeacherCorrectionSummary() != null && !memory.getTeacherCorrectionSummary().isBlank()) {
            parts.add("teacherCorrection=" + memory.getTeacherCorrectionSummary());
        }
        String teacherCalibration = teacherCalibrationSummary(evidencePackage);
        if (!teacherCalibration.isBlank()) {
            parts.add("teacherCalibration=" + teacherCalibration);
        }
        if (memory.getEvidenceRefs() != null && !memory.getEvidenceRefs().isEmpty()) {
            parts.add("memoryEvidenceRefs=" + String.join(",", memory.getEvidenceRefs()));
        }
        return truncate(String.join(" | ", parts), 900);
    }

    private String teacherCalibrationSummary(DiagnosisEvidencePackage evidencePackage) {
        DiagnosisEvidencePackage.StudentLearningMemorySnapshot memory =
                evidencePackage == null ? null : evidencePackage.getLearningMemory();
        if (memory == null || memory.getTeacherCalibrationPatterns() == null
                || memory.getTeacherCalibrationPatterns().isEmpty()) {
            return "";
        }
        return truncate(memory.getTeacherCalibrationPatterns().stream()
                .limit(3)
                .map(pattern -> "original="
                        + defaultIfBlank(pattern.getOriginalFineGrainedTag(), pattern.getOriginalIssueTag())
                        + " corrected="
                        + defaultIfBlank(pattern.getCorrectedFineGrainedTag(), pattern.getCorrectedIssueTag())
                        + " count=" + (pattern.getCorrectionCount() == null ? 0L : pattern.getCorrectionCount())
                        + " refs=" + String.join(",", pattern.getEvidenceRefs() == null ? List.of() : pattern.getEvidenceRefs())
                        + (isBlank(pattern.getLatestTeacherNote()) ? "" : " note=" + truncate(pattern.getLatestTeacherNote(), 120)))
                .toList()
                .toString(), 600);
    }

    private void addTeacherCalibrationIssueTags(Set<String> tags,
                                                DiagnosisEvidencePackage.StudentLearningMemorySnapshot memory) {
        if (memory == null || memory.getTeacherCalibrationPatterns() == null) {
            return;
        }
        memory.getTeacherCalibrationPatterns().stream()
                .map(DiagnosisEvidencePackage.TeacherCalibrationPattern::getCorrectedIssueTag)
                .filter(tag -> tag != null && !tag.isBlank())
                .forEach(tags::add);
    }

    private void addTeacherCalibrationFineTags(Set<String> tags,
                                               DiagnosisEvidencePackage.StudentLearningMemorySnapshot memory) {
        if (memory == null || memory.getTeacherCalibrationPatterns() == null) {
            return;
        }
        memory.getTeacherCalibrationPatterns().stream()
                .map(DiagnosisEvidencePackage.TeacherCalibrationPattern::getCorrectedFineGrainedTag)
                .filter(tag -> tag != null && !tag.isBlank())
                .forEach(tags::add);
    }

    private String firstEvidenceRef(DiagnosisEvidencePackage.TeacherCalibrationPattern pattern) {
        if (pattern == null || pattern.getEvidenceRefs() == null || pattern.getEvidenceRefs().isEmpty()) {
            return "memory:teacher_calibration";
        }
        return pattern.getEvidenceRefs().get(0);
    }

    private Double teacherCalibrationConfidence(DiagnosisEvidencePackage.TeacherCalibrationPattern pattern) {
        long count = pattern == null || pattern.getCorrectionCount() == null ? 0L : pattern.getCorrectionCount();
        if (count >= 3) {
            return 0.64;
        }
        if (count >= 2) {
            return 0.58;
        }
        return 0.5;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private void addMemoryStats(List<String> parts,
                                String name,
                                List<DiagnosisEvidencePackage.MemoryTagStat> stats) {
        if (stats == null || stats.isEmpty()) {
            return;
        }
        parts.add(name + "=" + stats.stream()
                .limit(3)
                .map(stat -> defaultIfBlank(stat.getTag(), "") + ":" + (stat.getCount() == null ? 0L : stat.getCount()))
                .toList());
    }

    private void addAbilityFocus(List<String> parts, List<DiagnosisEvidencePackage.AbilityFocus> abilityFocus) {
        if (abilityFocus == null || abilityFocus.isEmpty()) {
            return;
        }
        parts.add("abilityFocus=" + abilityFocus.stream()
                .limit(3)
                .map(focus -> defaultIfBlank(focus.getAbilityPoint(), "")
                        + ":submissions=" + (focus.getSubmissionCount() == null ? 0L : focus.getSubmissionCount())
                        + ",problems=" + (focus.getProblemCount() == null ? 0L : focus.getProblemCount()))
                .toList());
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

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value == null ? "" : value;
        }
        return value.substring(0, Math.max(0, maxLength - 24)) + "\n...[truncated for model]";
    }
}
