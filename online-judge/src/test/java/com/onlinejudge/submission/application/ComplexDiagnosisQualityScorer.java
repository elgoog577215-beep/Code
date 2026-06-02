package com.onlinejudge.submission.application;

import com.onlinejudge.submission.dto.SubmissionAnalysisResponse;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

class ComplexDiagnosisQualityScorer {

    static final List<String> METRICS = List.of(
            "primaryRootCauseHit",
            "teachingPriorityCorrect",
            "secondaryIssuesNotOverweighted",
            "distractingSignalsIgnored",
            "evidenceGrounded",
            "noFullSolutionLeak"
    );

    static final List<String> INTELLIGENCE_METRICS = List.of(
            "autonomousRootCauseDiscovery",
            "teachingDecisionQuality",
            "complexSignalPrioritization",
            "distractorResistance",
            "evidenceGroundedReasoning",
            "modelSafetyAndBoundary"
    );

    static final List<String> STUDENT_FEEDBACK_METRICS = List.of(
            "blockingPrimaryHit",
            "secondaryIssueBalanced",
            "improvementOpportunityUseful",
            "evidenceGrounded",
            "studentActionable",
            "noSolutionLeak",
            "fallbackHonesty"
    );

    static final Map<String, String> INTELLIGENCE_METRIC_BY_COMPLEX_METRIC = Map.of(
            "primaryRootCauseHit", "autonomousRootCauseDiscovery",
            "secondaryIssuesNotOverweighted", "complexSignalPrioritization",
            "evidenceGrounded", "evidenceGroundedReasoning",
            "teachingPriorityCorrect", "teachingDecisionQuality",
            "distractingSignalsIgnored", "distractorResistance",
            "noFullSolutionLeak", "modelSafetyAndBoundary"
    );

    Result score(ComplexStudentSubmissionEvalFixtureLoader.Fixture fixture,
                 SubmissionAnalysisResponse analysis) {
        if (fixture == null || analysis == null) {
            return Result.empty();
        }
        Map<String, Boolean> metrics = new LinkedHashMap<>();
        String primaryFineTag = fixture.primaryRootCause() == null
                ? ""
                : safe(fixture.primaryRootCause().fineGrainedTag());
        String primaryIssueTag = fixture.primaryRootCause() == null
                ? ""
                : safe(fixture.primaryRootCause().issueTag());
        String combinedText = combinedText(analysis);

        metrics.put("primaryRootCauseHit", containsIgnoreCase(analysis.getFineGrainedTags(), primaryFineTag));
        metrics.put("teachingPriorityCorrect", teachingPriorityCorrect(fixture, analysis, combinedText, primaryFineTag, primaryIssueTag));
        metrics.put("secondaryIssuesNotOverweighted", secondaryIssuesNotOverweighted(analysis, primaryFineTag));
        metrics.put("distractingSignalsIgnored", avoidsSignals(combinedText, fixture.distractingSignals()));
        metrics.put("evidenceGrounded", evidenceGrounded(analysis.getEvidenceRefs(), fixture.requiredEvidenceRefs()));
        metrics.put("noFullSolutionLeak", noFullSolutionLeak(analysis, combinedText, fixture.mustNotMention()));

        List<String> failedMetrics = metrics.entrySet().stream()
                .filter(entry -> !Boolean.TRUE.equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .toList();
        int passedCount = (int) metrics.values().stream().filter(Boolean.TRUE::equals).count();
        return new Result(true, metrics, failedMetrics, passedCount, metrics.size());
    }

    IntelligenceResult intelligence(Result complexQuality, boolean modelCompleted, boolean fallbackUsed) {
        if (complexQuality == null || !complexQuality.complexCase()) {
            return IntelligenceResult.empty(false);
        }
        boolean evaluated = modelCompleted && !fallbackUsed;
        Map<String, Boolean> metrics = mapIntelligenceMetrics(complexQuality.metrics());
        if (!evaluated) {
            return new IntelligenceResult(true, false, metrics, List.of(), 0);
        }
        List<String> failedMetrics = metrics.entrySet().stream()
                .filter(entry -> !Boolean.TRUE.equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .toList();
        int passedCount = (int) metrics.values().stream().filter(Boolean.TRUE::equals).count();
        return new IntelligenceResult(true, true, metrics, failedMetrics, passedCount);
    }

    StudentFeedbackResult studentFeedback(ComplexStudentSubmissionEvalFixtureLoader.Fixture fixture,
                                          SubmissionAnalysisResponse analysis,
                                          boolean modelCompleted,
                                          boolean fallbackUsed) {
        if (fixture == null || analysis == null) {
            return StudentFeedbackResult.empty(false, false);
        }
        boolean evaluated = modelCompleted && !fallbackUsed;
        Map<String, Boolean> metrics = new LinkedHashMap<>();
        SubmissionAnalysisResponse.StudentFeedback feedback = analysis.getStudentFeedback();
        String primaryFineTag = fixture.primaryRootCause() == null
                ? ""
                : safe(fixture.primaryRootCause().fineGrainedTag());
        String primaryIssueTag = fixture.primaryRootCause() == null
                ? ""
                : safe(fixture.primaryRootCause().issueTag());
        String feedbackText = studentFeedbackText(feedback);
        metrics.put("blockingPrimaryHit", feedback != null
                && feedback.getBlockingIssues() != null
                && !feedback.getBlockingIssues().isEmpty()
                && (containsText(feedbackText, primaryFineTag)
                || containsText(feedbackText, primaryIssueTag)
                || fixture.mustMention().stream().anyMatch(token -> containsText(feedbackText, token))));
        metrics.put("secondaryIssueBalanced", secondaryIssueBalanced(feedback));
        metrics.put("improvementOpportunityUseful", feedback != null
                && feedback.getImprovementOpportunities() != null
                && feedback.getImprovementOpportunities().stream().anyMatch(item ->
                item != null && !safe(item.getCategory()).isBlank()
                        && !safe(item.getBenefit()).isBlank()
                        && !containsText(safe(item.getStudentMessage()), primaryFineTag)));
        metrics.put("evidenceGrounded", feedbackEvidenceGrounded(feedback, fixture.requiredEvidenceRefs()));
        metrics.put("studentActionable", feedback != null
                && feedback.getNextLearningAction() != null
                && !safe(feedback.getNextLearningAction().getTask()).isBlank()
                && !safe(feedback.getNextLearningAction().getCheckQuestion()).isBlank());
        metrics.put("noSolutionLeak", noFullSolutionLeak(analysis, feedbackText, fixture.mustNotMention()));
        metrics.put("fallbackHonesty", evaluated || fallbackUsed);
        if (!evaluated) {
            return new StudentFeedbackResult(true, false, metrics, List.of(), 0);
        }
        List<String> failedMetrics = metrics.entrySet().stream()
                .filter(entry -> !Boolean.TRUE.equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .toList();
        int passedCount = (int) metrics.values().stream().filter(Boolean.TRUE::equals).count();
        return new StudentFeedbackResult(true, true, metrics, failedMetrics, passedCount);
    }

    private Map<String, Boolean> mapIntelligenceMetrics(Map<String, Boolean> complexMetrics) {
        Map<String, Boolean> mapped = new LinkedHashMap<>();
        if (complexMetrics == null || complexMetrics.isEmpty()) {
            return mapped;
        }
        METRICS.forEach(metric -> {
            String intelligenceMetric = INTELLIGENCE_METRIC_BY_COMPLEX_METRIC.get(metric);
            if (intelligenceMetric != null) {
                mapped.put(intelligenceMetric, Boolean.TRUE.equals(complexMetrics.get(metric)));
            }
        });
        return mapped;
    }

    private boolean teachingPriorityCorrect(ComplexStudentSubmissionEvalFixtureLoader.Fixture fixture,
                                            SubmissionAnalysisResponse analysis,
                                            String combinedText,
                                            String primaryFineTag,
                                            String primaryIssueTag) {
        boolean primaryMentioned = containsIgnoreCase(analysis.getFineGrainedTags(), primaryFineTag)
                || containsIgnoreCase(analysis.getIssueTags(), primaryIssueTag)
                || containsText(combinedText, primaryFineTag)
                || containsText(combinedText, primaryIssueTag);
        boolean teachingTokenMentioned = fixture.mustMention() != null && fixture.mustMention().stream()
                .filter(value -> value != null && !value.isBlank())
                .anyMatch(value -> containsText(combinedText, value));
        return primaryMentioned && teachingTokenMentioned;
    }

    private boolean secondaryIssuesNotOverweighted(SubmissionAnalysisResponse analysis, String primaryFineTag) {
        List<String> fineTags = analysis.getFineGrainedTags() == null ? List.of() : analysis.getFineGrainedTags();
        if (fineTags.isEmpty() || primaryFineTag.isBlank()) {
            return false;
        }
        return primaryFineTag.equalsIgnoreCase(safe(fineTags.get(0)));
    }

    private boolean secondaryIssueBalanced(SubmissionAnalysisResponse.StudentFeedback feedback) {
        if (feedback == null || feedback.getSecondaryIssues() == null || feedback.getSecondaryIssues().isEmpty()) {
            return true;
        }
        return feedback.getSecondaryIssues().stream()
                .filter(issue -> issue != null)
                .allMatch(issue -> {
                    String why = safe(issue.getWhyNotPrimary());
                    String text = safe(issue.getTitle()) + "\n" + safe(issue.getStudentMessage()) + "\n" + why;
                    boolean hasBalanceReason = !why.isBlank()
                            || containsText(text, "不是主因")
                            || containsText(text, "不应优先")
                            || containsText(text, "先放到后面")
                            || containsText(text, "次要");
                    boolean overweightsSecondary = containsText(text, "必须先改")
                            || containsText(text, "最需要先处理")
                            || containsText(text, "首要问题");
                    return hasBalanceReason && !overweightsSecondary;
                });
    }

    private boolean avoidsSignals(String combinedText, List<String> signals) {
        if (signals == null || signals.isEmpty()) {
            return true;
        }
        return signals.stream()
                .filter(value -> value != null && !value.isBlank())
                .noneMatch(value -> containsText(combinedText, value));
    }

    private boolean evidenceGrounded(List<String> actualRefs, List<String> requiredRefs) {
        if (actualRefs == null || actualRefs.isEmpty() || requiredRefs == null || requiredRefs.isEmpty()) {
            return false;
        }
        return requiredRefs.stream()
                .filter(value -> value != null && !value.isBlank())
                .allMatch(required -> actualRefs.stream().anyMatch(actual -> required.equalsIgnoreCase(safe(actual))));
    }

    private boolean noFullSolutionLeak(SubmissionAnalysisResponse analysis,
                                       String combinedText,
                                       List<String> mustNotMention) {
        if ("HIGH".equalsIgnoreCase(safe(analysis.getAnswerLeakRisk()))) {
            return false;
        }
        List<String> forbidden = mustNotMention == null ? List.of() : mustNotMention;
        return forbidden.stream()
                .filter(value -> value != null && !value.isBlank())
                .noneMatch(value -> containsText(combinedText, value));
    }

    private boolean containsIgnoreCase(List<String> values, String expected) {
        if (values == null || expected == null || expected.isBlank()) {
            return false;
        }
        return values.stream().anyMatch(value -> expected.equalsIgnoreCase(safe(value)));
    }

    private boolean containsText(String text, String expected) {
        if (expected == null || expected.isBlank()) {
            return false;
        }
        return text.toLowerCase(Locale.ROOT).contains(expected.trim().toLowerCase(Locale.ROOT));
    }

    private String combinedText(SubmissionAnalysisResponse analysis) {
        List<String> values = new ArrayList<>();
        values.add(analysis.getHeadline());
        values.add(analysis.getSummary());
        values.add(analysis.getStudentHint());
        values.add(analysis.getTeacherNote());
        values.add(analysis.getReportMarkdown());
        if (analysis.getStudentHintPlan() != null) {
            values.add(analysis.getStudentHintPlan().getProblemType());
            values.add(analysis.getStudentHintPlan().getEvidenceAnchor());
            values.add(analysis.getStudentHintPlan().getNextAction());
            values.add(analysis.getStudentHintPlan().getCoachQuestion());
            values.add(analysis.getStudentHintPlan().getTeachingAction());
        }
        if (analysis.getLearningInterventionPlan() != null) {
            values.add(analysis.getLearningInterventionPlan().getGoal());
            values.add(analysis.getLearningInterventionPlan().getStudentTask());
            values.add(analysis.getLearningInterventionPlan().getCheckQuestion());
            values.add(analysis.getLearningInterventionPlan().getCompletionSignal());
        }
        values.add(studentFeedbackText(analysis.getStudentFeedback()));
        return String.join("\n", values.stream().map(this::safe).toList());
    }

    private String studentFeedbackText(SubmissionAnalysisResponse.StudentFeedback feedback) {
        if (feedback == null) {
            return "";
        }
        List<String> values = new ArrayList<>();
        values.add(feedback.getSummary());
        if (feedback.getBlockingIssues() != null) {
            feedback.getBlockingIssues().forEach(issue -> {
                if (issue == null) {
                    return;
                }
                values.add(issue.getTitle());
                values.add(issue.getStudentMessage());
                values.add(issue.getEvidence());
                values.add(issue.getNextAction());
                values.add(issue.getIssueTag());
                values.add(issue.getFineGrainedTag());
            });
        }
        if (feedback.getSecondaryIssues() != null) {
            feedback.getSecondaryIssues().forEach(issue -> {
                if (issue == null) {
                    return;
                }
                values.add(issue.getTitle());
                values.add(issue.getStudentMessage());
                values.add(issue.getWhyNotPrimary());
                values.add(issue.getIssueTag());
            });
        }
        if (feedback.getImprovementOpportunities() != null) {
            feedback.getImprovementOpportunities().forEach(item -> {
                if (item == null) {
                    return;
                }
                values.add(item.getCategory());
                values.add(item.getStudentMessage());
                values.add(item.getBenefit());
            });
        }
        if (feedback.getNextLearningAction() != null) {
            values.add(feedback.getNextLearningAction().getAction());
            values.add(feedback.getNextLearningAction().getTask());
            values.add(feedback.getNextLearningAction().getCheckQuestion());
        }
        return String.join("\n", values.stream().map(this::safe).toList());
    }

    private boolean feedbackEvidenceGrounded(SubmissionAnalysisResponse.StudentFeedback feedback,
                                             List<String> requiredRefs) {
        if (feedback == null || requiredRefs == null || requiredRefs.isEmpty()) {
            return false;
        }
        List<String> refs = new ArrayList<>();
        if (feedback.getBlockingIssues() != null) {
            feedback.getBlockingIssues().forEach(issue -> {
                if (issue != null && issue.getEvidenceRefs() != null) {
                    refs.addAll(issue.getEvidenceRefs());
                }
            });
        }
        if (feedback.getNextLearningAction() != null && feedback.getNextLearningAction().getEvidenceRefs() != null) {
            refs.addAll(feedback.getNextLearningAction().getEvidenceRefs());
        }
        return requiredRefs.stream()
                .filter(value -> value != null && !value.isBlank())
                .anyMatch(required -> refs.stream().anyMatch(actual -> required.equalsIgnoreCase(safe(actual))));
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    record Result(boolean complexCase,
                  Map<String, Boolean> metrics,
                  List<String> failedMetrics,
                  int passedMetricCount,
                  int totalMetricCount) {

        static Result empty() {
            return new Result(false, Map.of(), List.of(), 0, 0);
        }

        boolean passed() {
            return complexCase && passedMetricCount == totalMetricCount && totalMetricCount > 0;
        }

        double score() {
            return totalMetricCount == 0 ? 0.0 : (double) passedMetricCount / totalMetricCount;
        }

        List<String> passedMetricSignals() {
            return metrics.entrySet().stream()
                    .filter(entry -> Boolean.TRUE.equals(entry.getValue()))
                    .map(entry -> "complexMetric:" + entry.getKey())
                    .toList();
        }
    }

    record IntelligenceResult(boolean complexCase,
                              boolean evaluated,
                              Map<String, Boolean> metrics,
                              List<String> failedMetrics,
                              int passedMetricCount) {

        static IntelligenceResult empty(boolean complexCase) {
            return new IntelligenceResult(complexCase, false, Map.of(), List.of(), 0);
        }

        int totalMetricCount() {
            return evaluated ? metrics.size() : 0;
        }

        boolean passed() {
            return evaluated && passedMetricCount == totalMetricCount() && totalMetricCount() > 0;
        }

        double score() {
            int total = totalMetricCount();
            return total == 0 ? 0.0 : (double) passedMetricCount / total;
        }

        List<String> passedMetricSignals() {
            if (!evaluated) {
                return List.of();
            }
            return metrics.entrySet().stream()
                    .filter(entry -> Boolean.TRUE.equals(entry.getValue()))
                    .map(entry -> "intelligenceMetric:" + entry.getKey())
                    .toList();
        }
    }

    record StudentFeedbackResult(boolean complexCase,
                                 boolean evaluated,
                                 Map<String, Boolean> metrics,
                                 List<String> failedMetrics,
                                 int passedMetricCount) {

        static StudentFeedbackResult empty(boolean complexCase, boolean evaluated) {
            return new StudentFeedbackResult(complexCase, evaluated, Map.of(), List.of(), 0);
        }

        int totalMetricCount() {
            return evaluated ? metrics.size() : 0;
        }

        boolean passed() {
            return evaluated && passedMetricCount == totalMetricCount() && totalMetricCount() > 0;
        }

        double score() {
            int total = totalMetricCount();
            return total == 0 ? 0.0 : (double) passedMetricCount / total;
        }

        List<String> passedMetricSignals() {
            if (!evaluated) {
                return List.of();
            }
            return metrics.entrySet().stream()
                    .filter(entry -> Boolean.TRUE.equals(entry.getValue()))
                    .map(entry -> "studentFeedbackMetric:" + entry.getKey())
                    .toList();
        }
    }
}
