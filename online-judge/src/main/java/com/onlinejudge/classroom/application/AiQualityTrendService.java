package com.onlinejudge.classroom.application;

import com.onlinejudge.classroom.domain.Assignment;
import com.onlinejudge.classroom.domain.ClassReviewFeedback;
import com.onlinejudge.classroom.domain.HintSafetyCheck;
import com.onlinejudge.classroom.domain.TeacherDiagnosisCorrection;
import com.onlinejudge.classroom.dto.AiQualityTrendResponse;
import com.onlinejudge.classroom.persistence.AssignmentRepository;
import com.onlinejudge.classroom.persistence.ClassReviewFeedbackRepository;
import com.onlinejudge.classroom.persistence.HintSafetyCheckRepository;
import com.onlinejudge.classroom.persistence.TeacherDiagnosisCorrectionRepository;
import com.onlinejudge.learning.diagnosis.DiagnosisReportReader;
import com.onlinejudge.learning.diagnosis.DiagnosisTaxonomy;
import com.onlinejudge.submission.domain.Submission;
import com.onlinejudge.submission.domain.SubmissionAnalysis;
import com.onlinejudge.submission.persistence.SubmissionAnalysisRepository;
import com.onlinejudge.submission.persistence.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiQualityTrendService {

    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final SubmissionAnalysisRepository submissionAnalysisRepository;
    private final TeacherDiagnosisCorrectionRepository teacherDiagnosisCorrectionRepository;
    private final ClassReviewFeedbackRepository classReviewFeedbackRepository;
    private final HintSafetyCheckRepository hintSafetyCheckRepository;
    private final DiagnosisReportReader diagnosisReportReader;
    private final DiagnosisTaxonomy diagnosisTaxonomy;

    public AiQualityTrendResponse buildTrend() {
        List<Assignment> assignments = assignmentRepository.findAllByOrderByCreatedAtDesc();
        List<Long> assignmentIds = assignments.stream()
                .map(Assignment::getId)
                .filter(Objects::nonNull)
                .toList();
        if (assignmentIds.isEmpty()) {
            return emptyTrend();
        }

        Map<Long, Assignment> assignmentById = assignments.stream()
                .filter(assignment -> assignment.getId() != null)
                .collect(Collectors.toMap(Assignment::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        List<Submission> submissions = submissionRepository.findByAssignmentIdIn(assignmentIds);
        Map<Long, List<Submission>> submissionsByAssignment = submissions.stream()
                .filter(submission -> submission.getAssignmentId() != null)
                .collect(Collectors.groupingBy(Submission::getAssignmentId, LinkedHashMap::new, Collectors.toList()));
        List<Long> submissionIds = submissions.stream()
                .map(Submission::getId)
                .filter(Objects::nonNull)
                .toList();
        Map<Long, SubmissionAnalysis> analysesBySubmissionId = submissionIds.isEmpty()
                ? Map.of()
                : submissionAnalysisRepository.findBySubmissionIdIn(submissionIds)
                .stream()
                .collect(Collectors.toMap(SubmissionAnalysis::getSubmissionId, Function.identity(), (left, right) -> left));
        List<HintSafetyCheck> safetyChecks = submissionIds.isEmpty()
                ? List.of()
                : hintSafetyCheckRepository.findBySubmissionIdIn(submissionIds);
        Map<Long, List<HintSafetyCheck>> safetyChecksBySubmissionId = safetyChecks.stream()
                .filter(check -> check.getSubmissionId() != null)
                .collect(Collectors.groupingBy(HintSafetyCheck::getSubmissionId, LinkedHashMap::new, Collectors.toList()));
        List<TeacherDiagnosisCorrection> corrections = teacherDiagnosisCorrectionRepository.findByAssignmentIdIn(assignmentIds);
        Map<Long, List<TeacherDiagnosisCorrection>> correctionsByAssignment = corrections.stream()
                .filter(correction -> correction.getAssignmentId() != null)
                .collect(Collectors.groupingBy(TeacherDiagnosisCorrection::getAssignmentId, LinkedHashMap::new, Collectors.toList()));
        List<ClassReviewFeedback> feedbacks = classReviewFeedbackRepository.findByAssignmentIdIn(assignmentIds);
        Map<Long, List<ClassReviewFeedback>> feedbacksByAssignment = feedbacks.stream()
                .filter(feedback -> feedback.getAssignmentId() != null)
                .collect(Collectors.groupingBy(ClassReviewFeedback::getAssignmentId, LinkedHashMap::new, Collectors.toList()));

        List<AiQualityTrendResponse.AssignmentQualityPoint> points = assignmentIds.stream()
                .map(assignmentId -> buildPoint(
                        assignmentById.get(assignmentId),
                        submissionsByAssignment.getOrDefault(assignmentId, List.of()),
                        analysesFor(submissionsByAssignment.get(assignmentId), analysesBySubmissionId),
                        safetyChecksFor(submissionsByAssignment.get(assignmentId), safetyChecksBySubmissionId),
                        analysesBySubmissionId,
                        correctionsByAssignment.getOrDefault(assignmentId, List.of()),
                        feedbacksByAssignment.getOrDefault(assignmentId, List.of())
                ))
                .toList();

        long analyzed = points.stream().mapToLong(AiQualityTrendResponse.AssignmentQualityPoint::getAnalyzedSubmissionCount).sum();
        long correctionCount = points.stream().mapToLong(AiQualityTrendResponse.AssignmentQualityPoint::getCorrectionCount).sum();
        long evalCandidateCount = points.stream().mapToLong(AiQualityTrendResponse.AssignmentQualityPoint::getEvalCandidateCount).sum();
        long interventionEvalCandidateCount = points.stream().mapToLong(AiQualityTrendResponse.AssignmentQualityPoint::getInterventionEvalCandidateCount).sum();
        long interventionWaitingFollowupCount = points.stream().mapToLong(AiQualityTrendResponse.AssignmentQualityPoint::getInterventionWaitingFollowupCount).sum();
        long interventionImprovedCount = points.stream().mapToLong(AiQualityTrendResponse.AssignmentQualityPoint::getInterventionImprovedCount).sum();
        long interventionShiftedCount = points.stream().mapToLong(AiQualityTrendResponse.AssignmentQualityPoint::getInterventionShiftedCount).sum();
        long interventionStillStuckCount = points.stream().mapToLong(AiQualityTrendResponse.AssignmentQualityPoint::getInterventionStillStuckCount).sum();
        long lowConfidenceCount = points.stream().mapToLong(AiQualityTrendResponse.AssignmentQualityPoint::getLowConfidenceCount).sum();
        long highLeakRiskCount = points.stream().mapToLong(AiQualityTrendResponse.AssignmentQualityPoint::getHighLeakRiskCount).sum();
        long promptSafetyIncidentCount = points.stream().mapToLong(AiQualityTrendResponse.AssignmentQualityPoint::getPromptSafetyIncidentCount).sum();
        long promptSafetyDowngradeCount = points.stream().mapToLong(AiQualityTrendResponse.AssignmentQualityPoint::getPromptSafetyDowngradeCount).sum();
        long promptSafetyHighRiskDowngradeCount = points.stream().mapToLong(AiQualityTrendResponse.AssignmentQualityPoint::getPromptSafetyHighRiskDowngradeCount).sum();

        return AiQualityTrendResponse.builder()
                .assignmentCount(points.size())
                .analyzedSubmissionCount(analyzed)
                .correctionCount(correctionCount)
                .evalCandidateCount(evalCandidateCount)
                .interventionEvalCandidateCount(interventionEvalCandidateCount)
                .interventionWaitingFollowupCount(interventionWaitingFollowupCount)
                .interventionImprovedCount(interventionImprovedCount)
                .interventionShiftedCount(interventionShiftedCount)
                .interventionStillStuckCount(interventionStillStuckCount)
                .lowConfidenceCount(lowConfidenceCount)
                .highLeakRiskCount(highLeakRiskCount)
                .promptSafetyIncidentCount(promptSafetyIncidentCount)
                .promptSafetyDowngradeCount(promptSafetyDowngradeCount)
                .promptSafetyHighRiskDowngradeCount(promptSafetyHighRiskDowngradeCount)
                .correctionRate(AiQualityMetrics.rate(correctionCount, analyzed))
                .lowConfidenceRate(AiQualityMetrics.rate(lowConfidenceCount, analyzed))
                .highLeakRiskRate(AiQualityMetrics.rate(highLeakRiskCount, analyzed))
                .promptSafetyIncidentRate(AiQualityMetrics.rate(promptSafetyIncidentCount, analyzed))
                .summary(buildSummary(analyzed, correctionCount, lowConfidenceCount, highLeakRiskCount,
                        promptSafetyDowngradeCount, interventionEvalCandidateCount, interventionStillStuckCount, interventionWaitingFollowupCount))
                .assignments(points)
                .correctedTags(buildCorrectedTags(corrections))
                .evalNeededTags(buildEvalNeededTags(corrections))
                .sourceSegments(buildSourceSegments(analysesBySubmissionId, corrections, safetyChecks))
                .build();
    }

    private AiQualityTrendResponse.AssignmentQualityPoint buildPoint(Assignment assignment,
                                                                     List<Submission> submissions,
                                                                     List<SubmissionAnalysis> analyses,
                                                                     List<HintSafetyCheck> safetyChecks,
                                                                     Map<Long, SubmissionAnalysis> analysesBySubmissionId,
                                                                     List<TeacherDiagnosisCorrection> corrections,
                                                                     List<ClassReviewFeedback> feedbacks) {
        AiQualityMetrics metrics = AiQualityMetrics.from(analyses, corrections, diagnosisReportReader);
        InterventionTrendCounts interventionCounts = interventionTrendCounts(submissions, analysesBySubmissionId, feedbacks);
        PromptSafetyTrendCounts promptSafetyCounts = promptSafetyTrendCounts(metrics.highLeakRiskCount(), safetyChecks);
        return AiQualityTrendResponse.AssignmentQualityPoint.builder()
                .assignmentId(assignment == null ? null : assignment.getId())
                .assignmentTitle(assignment == null ? "" : assignment.getTitle())
                .analyzedSubmissionCount(metrics.analyzedSubmissionCount())
                .correctionCount(metrics.correctionCount())
                .evalCandidateCount(metrics.evalCandidateCount())
                .interventionEvalCandidateCount(interventionCounts.candidateCount())
                .interventionWaitingFollowupCount(interventionCounts.waitingFollowupCount())
                .interventionImprovedCount(interventionCounts.improvedCount())
                .interventionShiftedCount(interventionCounts.shiftedCount())
                .interventionStillStuckCount(interventionCounts.stillStuckCount())
                .lowConfidenceCount(metrics.lowConfidenceCount())
                .highLeakRiskCount(metrics.highLeakRiskCount())
                .promptSafetyIncidentCount(promptSafetyCounts.incidentCount())
                .promptSafetyDowngradeCount(promptSafetyCounts.downgradeCount())
                .promptSafetyHighRiskDowngradeCount(promptSafetyCounts.highRiskDowngradeCount())
                .correctionRate(metrics.correctionRate())
                .lowConfidenceRate(metrics.lowConfidenceRate())
                .highLeakRiskRate(metrics.highLeakRiskRate())
                .promptSafetyIncidentRate(AiQualityMetrics.rate(promptSafetyCounts.incidentCount(), metrics.analyzedSubmissionCount()))
                .summary(buildSummary(metrics.analyzedSubmissionCount(), metrics.correctionCount(), metrics.lowConfidenceCount(), metrics.highLeakRiskCount(),
                        promptSafetyCounts.downgradeCount(), interventionCounts.candidateCount(), interventionCounts.stillStuckCount(), interventionCounts.waitingFollowupCount()))
                .build();
    }

    private List<SubmissionAnalysis> analysesFor(List<Submission> submissions,
                                                 Map<Long, SubmissionAnalysis> analysesBySubmissionId) {
        if (submissions == null || submissions.isEmpty()) {
            return List.of();
        }
        return submissions.stream()
                .map(Submission::getId)
                .map(analysesBySubmissionId::get)
                .filter(Objects::nonNull)
                .toList();
    }

    private List<HintSafetyCheck> safetyChecksFor(List<Submission> submissions,
                                                  Map<Long, List<HintSafetyCheck>> safetyChecksBySubmissionId) {
        if (submissions == null || submissions.isEmpty()) {
            return List.of();
        }
        Map<Long, List<HintSafetyCheck>> safeChecks = safetyChecksBySubmissionId == null ? Map.of() : safetyChecksBySubmissionId;
        return submissions.stream()
                .map(Submission::getId)
                .map(safeChecks::get)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .toList();
    }

    private InterventionTrendCounts interventionTrendCounts(List<Submission> submissions,
                                                            Map<Long, SubmissionAnalysis> analysesBySubmissionId,
                                                            List<ClassReviewFeedback> feedbacks) {
        long waiting = 0;
        long improved = 0;
        long shifted = 0;
        long stillStuck = 0;
        List<Submission> safeSubmissions = submissions == null ? List.of() : submissions;
        Map<Long, SubmissionAnalysis> safeAnalyses = analysesBySubmissionId == null ? Map.of() : analysesBySubmissionId;
        for (ClassReviewFeedback feedback : feedbacks == null ? List.<ClassReviewFeedback>of() : feedbacks) {
            if (!ClassReviewFeedbackService.ACTION_ACCEPTED.equals(feedback.getActionType())
                    && !ClassReviewFeedbackService.ACTION_MODIFIED.equals(feedback.getActionType())) {
                continue;
            }
            String status = interventionStatus(feedback, safeSubmissions, safeAnalyses);
            switch (status) {
                case TeacherInterventionImpactAnalyzer.STATUS_WAITING_FOLLOWUP -> waiting++;
                case TeacherInterventionImpactAnalyzer.STATUS_IMPROVED -> improved++;
                case TeacherInterventionImpactAnalyzer.STATUS_SHIFTED -> shifted++;
                case TeacherInterventionImpactAnalyzer.STATUS_STILL_STUCK -> stillStuck++;
                default -> {
                }
            }
        }
        return new InterventionTrendCounts(improved + shifted + stillStuck, waiting, improved, shifted, stillStuck);
    }

    private PromptSafetyTrendCounts promptSafetyTrendCounts(long highLeakRiskCount,
                                                            List<HintSafetyCheck> safetyChecks) {
        long downgradeCount = 0;
        long highRiskDowngradeCount = 0;
        for (HintSafetyCheck check : safetyChecks == null ? List.<HintSafetyCheck>of() : safetyChecks) {
            int weight = riskWeight(check == null ? null : check.getRiskLevel());
            if (weight >= 2) {
                downgradeCount++;
            }
            if (weight >= 3) {
                highRiskDowngradeCount++;
            }
        }
        return new PromptSafetyTrendCounts(highLeakRiskCount + downgradeCount, downgradeCount, highRiskDowngradeCount);
    }

    private String interventionStatus(ClassReviewFeedback feedback,
                                      List<Submission> submissions,
                                      Map<Long, SubmissionAnalysis> analysesBySubmissionId) {
        List<Submission> followups = submissions.stream()
                .filter(Objects::nonNull)
                .filter(submission -> feedback.getExampleProblemId() == null
                        || Objects.equals(submission.getProblemId(), feedback.getExampleProblemId()))
                .filter(submission -> feedback.getCreatedAt() == null || isAfter(submission.getSubmittedAt(), feedback.getCreatedAt()))
                .sorted(Comparator
                        .comparing(Submission::getSubmittedAt, Comparator.nullsLast(LocalDateTime::compareTo))
                        .thenComparing(Submission::getId, Comparator.nullsLast(Long::compareTo))
                        .reversed())
                .toList();
        if (followups.isEmpty()) {
            return TeacherInterventionImpactAnalyzer.STATUS_WAITING_FOLLOWUP;
        }
        Submission latest = followups.get(0);
        if (latest.getVerdict() == Submission.Verdict.ACCEPTED) {
            return TeacherInterventionImpactAnalyzer.STATUS_IMPROVED;
        }
        SubmissionAnalysis analysis = latest.getId() == null ? null : analysesBySubmissionId.get(latest.getId());
        if (analysis == null) {
            return TeacherInterventionImpactAnalyzer.STATUS_WAITING_FOLLOWUP;
        }
        List<String> expectedTags = parseEvidenceTags(feedback.getEvidenceTags());
        if (!expectedTags.isEmpty() && mergedTags(analysis).stream().anyMatch(expectedTags::contains)) {
            return TeacherInterventionImpactAnalyzer.STATUS_STILL_STUCK;
        }
        return TeacherInterventionImpactAnalyzer.STATUS_SHIFTED;
    }

    private boolean isAfter(LocalDateTime value, LocalDateTime anchor) {
        return value != null && value.isAfter(anchor);
    }

    private List<String> parseEvidenceTags(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        String trimmed = json.trim();
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
            return List.of(trimmed);
        }
        String body = trimmed.substring(1, trimmed.length() - 1).trim();
        if (body.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(body.split(","))
                .map(value -> value.replace("\"", "").trim())
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    private List<String> mergedTags(SubmissionAnalysis analysis) {
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        diagnosisReportReader.fineGrainedTags(analysis).forEach(tags::add);
        diagnosisReportReader.issueTags(analysis).forEach(tags::add);
        return List.copyOf(tags);
    }

    private List<AiQualityTrendResponse.TagTrendStat> buildCorrectedTags(List<TeacherDiagnosisCorrection> corrections) {
        Map<String, TagAccumulator> accumulators = new LinkedHashMap<>();
        for (TeacherDiagnosisCorrection correction : corrections) {
            String tag = firstNonBlank(correction.getCorrectedFineGrainedTag(), correction.getCorrectedIssueTag(), "UNKNOWN");
            TagAccumulator accumulator = accumulators.computeIfAbsent(tag, TagAccumulator::new);
            accumulator.count++;
            if (correction.isEvalCandidate()) {
                accumulator.evalCandidateCount++;
            }
        }
        return toTagStats(accumulators.values());
    }

    private List<AiQualityTrendResponse.TagTrendStat> buildEvalNeededTags(List<TeacherDiagnosisCorrection> corrections) {
        Map<String, TagAccumulator> accumulators = new LinkedHashMap<>();
        for (TeacherDiagnosisCorrection correction : corrections) {
            if (correction.isEvalCandidate()) {
                continue;
            }
            String tag = firstNonBlank(correction.getCorrectedFineGrainedTag(), correction.getCorrectedIssueTag(), "UNKNOWN");
            TagAccumulator accumulator = accumulators.computeIfAbsent(tag, TagAccumulator::new);
            accumulator.count++;
        }
        return toTagStats(accumulators.values());
    }

    private List<AiQualityTrendResponse.TagTrendStat> toTagStats(Collection<TagAccumulator> values) {
        return values.stream()
                .sorted(Comparator.comparing(TagAccumulator::getCount).reversed().thenComparing(TagAccumulator::getTag))
                .limit(8)
                .map(accumulator -> AiQualityTrendResponse.TagTrendStat.builder()
                        .tag(accumulator.tag)
                        .label(diagnosisTaxonomy.label(accumulator.tag))
                        .count(accumulator.count)
                        .evalCandidateCount(accumulator.evalCandidateCount)
                        .build())
                .toList();
    }

    private List<AiQualityTrendResponse.SourceQualitySegment> buildSourceSegments(Map<Long, SubmissionAnalysis> analysesBySubmissionId,
                                                                                  List<TeacherDiagnosisCorrection> corrections,
                                                                                  List<HintSafetyCheck> safetyChecks) {
        Map<String, SourceAccumulator> accumulators = new LinkedHashMap<>();
        analysesBySubmissionId.values().stream()
                .filter(Objects::nonNull)
                .forEach(analysis -> {
                    String key = sourceSegmentKey(analysis);
                    SourceAccumulator accumulator = accumulators.computeIfAbsent(
                            key,
                            ignored -> new SourceAccumulator(analysis, diagnosisReportReader)
                    );
                    accumulator.recordInvocation(analysis, diagnosisReportReader);
                    accumulator.analyzedSubmissionCount++;
                    Double confidence = diagnosisReportReader.confidence(analysis);
                    if (confidence == null || confidence < AiQualityMetrics.LOW_CONFIDENCE_THRESHOLD) {
                        accumulator.lowConfidenceCount++;
                    }
                    if ("HIGH".equalsIgnoreCase(diagnosisReportReader.answerLeakRisk(analysis))) {
                        accumulator.highLeakRiskCount++;
                        accumulator.promptSafetyIncidentCount++;
                    }
                });

        Map<Long, SubmissionAnalysis> safeAnalyses = analysesBySubmissionId == null ? Map.of() : analysesBySubmissionId;
        for (TeacherDiagnosisCorrection correction : corrections == null ? List.<TeacherDiagnosisCorrection>of() : corrections) {
            SubmissionAnalysis analysis = safeAnalyses.get(correction.getSubmissionId());
            String key = analysis == null ? "UNKNOWN|unknown" : sourceSegmentKey(analysis);
            SourceAccumulator accumulator = accumulators.computeIfAbsent(key, ignored -> analysis == null
                    ? new SourceAccumulator("UNKNOWN", "unknown")
                    : new SourceAccumulator(analysis, diagnosisReportReader));
            accumulator.correctionCount++;
        }

        for (HintSafetyCheck safetyCheck : safetyChecks == null ? List.<HintSafetyCheck>of() : safetyChecks) {
            int riskWeight = riskWeight(safetyCheck == null ? null : safetyCheck.getRiskLevel());
            if (riskWeight < 2) {
                continue;
            }
            SubmissionAnalysis analysis = safetyCheck.getSubmissionId() == null ? null : safeAnalyses.get(safetyCheck.getSubmissionId());
            String key = analysis == null ? "UNKNOWN|unknown" : sourceSegmentKey(analysis);
            SourceAccumulator accumulator = accumulators.computeIfAbsent(key, ignored -> analysis == null
                    ? new SourceAccumulator("UNKNOWN", "unknown")
                    : new SourceAccumulator(analysis, diagnosisReportReader));
            accumulator.promptSafetyIncidentCount++;
            accumulator.promptSafetyDowngradeCount++;
            if (riskWeight >= 3) {
                accumulator.promptSafetyHighRiskDowngradeCount++;
            }
        }

        return accumulators.values().stream()
                .sorted(Comparator
                        .comparing(SourceAccumulator::getAnalyzedSubmissionCount)
                        .thenComparing(SourceAccumulator::getCorrectionCount)
                        .reversed()
                        .thenComparing(SourceAccumulator::getSourceType)
                        .thenComparing(SourceAccumulator::getVersionLabel))
                .limit(8)
                .map(accumulator -> AiQualityTrendResponse.SourceQualitySegment.builder()
                        .sourceType(accumulator.sourceType)
                        .versionLabel(accumulator.versionLabel)
                        .provider(accumulator.provider)
                        .model(accumulator.model)
                        .modelVersion(accumulator.modelVersion)
                        .promptVersion(accumulator.promptVersion)
                        .agentVersion(accumulator.agentVersion)
                        .status(accumulator.status)
                        .fallbackCount(accumulator.fallbackCount)
                        .analyzedSubmissionCount(accumulator.analyzedSubmissionCount)
                        .correctionCount(accumulator.correctionCount)
                        .lowConfidenceCount(accumulator.lowConfidenceCount)
                        .highLeakRiskCount(accumulator.highLeakRiskCount)
                        .promptSafetyIncidentCount(accumulator.promptSafetyIncidentCount)
                        .promptSafetyDowngradeCount(accumulator.promptSafetyDowngradeCount)
                        .promptSafetyHighRiskDowngradeCount(accumulator.promptSafetyHighRiskDowngradeCount)
                        .correctionRate(AiQualityMetrics.rate(accumulator.correctionCount, accumulator.analyzedSubmissionCount))
                        .lowConfidenceRate(AiQualityMetrics.rate(accumulator.lowConfidenceCount, accumulator.analyzedSubmissionCount))
                        .highLeakRiskRate(AiQualityMetrics.rate(accumulator.highLeakRiskCount, accumulator.analyzedSubmissionCount))
                        .promptSafetyIncidentRate(AiQualityMetrics.rate(accumulator.promptSafetyIncidentCount, accumulator.analyzedSubmissionCount))
                        .build())
                .toList();
    }

    private String sourceSegmentKey(SubmissionAnalysis analysis) {
        DiagnosisReportReader.AiInvocationSnapshot invocation = diagnosisReportReader.aiInvocation(analysis);
        if (invocation != null) {
            return String.join("|",
                    firstNonBlank(analysis.getAnalysisSource(), "UNKNOWN"),
                    firstNonBlank(invocation.provider(), "UNKNOWN"),
                    firstNonBlank(invocation.modelVersion(), invocation.model(), "unknown-model"),
                    firstNonBlank(invocation.promptVersion(), "unknown-prompt"),
                    firstNonBlank(invocation.agentVersion(), "unknown-agent"),
                    firstNonBlank(invocation.status(), "unknown-status")
            );
        }
        return firstNonBlank(analysis.getAnalysisSource(), "UNKNOWN") + "|" + versionLabel(analysis);
    }

    private String versionLabel(SubmissionAnalysis analysis) {
        DiagnosisReportReader.AiInvocationSnapshot invocation = diagnosisReportReader.aiInvocation(analysis);
        if (invocation != null) {
            return String.join(" / ",
                    firstNonBlank(invocation.analysisSchemaVersion(), diagnosisReportReader.analysisSchemaVersion(analysis)),
                    firstNonBlank(invocation.agentVersion(), "unknown-agent"),
                    firstNonBlank(invocation.promptVersion(), "unknown-prompt"),
                    firstNonBlank(invocation.modelVersion(), invocation.model(), "unknown-model")
            );
        }
        String schemaVersion = diagnosisReportReader.analysisSchemaVersion(analysis);
        String trace = diagnosisReportReader.diagnosticTrace(analysis);
        String agentVersion = extractTraceToken(trace, "diagnostic-agent:");
        if (agentVersion.isBlank()) {
            agentVersion = extractTraceToken(trace, "diagnostic-agent-");
        }
        if (!agentVersion.isBlank()) {
            return schemaVersion + " / agent-" + agentVersion;
        }
        return schemaVersion;
    }

    private String extractTraceToken(String trace, String prefix) {
        if (trace == null || trace.isBlank()) {
            return "";
        }
        int start = trace.indexOf(prefix);
        if (start < 0) {
            return "";
        }
        int valueStart = start + prefix.length();
        int end = trace.indexOf(' ', valueStart);
        if (end < 0) {
            end = trace.length();
        }
        return trace.substring(valueStart, end).trim();
    }

    private String buildSummary(long analyzedCount,
                                long correctionCount,
                                long lowConfidenceCount,
                                long highLeakRiskCount,
                                long promptSafetyDowngradeCount,
                                long interventionEvalCandidateCount,
                                long interventionStillStuckCount,
                                long interventionWaitingFollowupCount) {
        if (analyzedCount == 0) {
            return "还没有跨作业 AI 诊断样本。";
        }
        if (interventionStillStuckCount > 0) {
            return "跨作业存在教师介入后仍卡同类问题，优先沉淀课堂介入 eval 并复盘策略颗粒度。";
        }
        if (highLeakRiskCount > 0) {
            return "跨作业范围内出现高泄题风险样本，建议优先复核。";
        }
        if (promptSafetyDowngradeCount > 0) {
            return "跨作业已有提示安全降级事件，建议按模型来源和提示版本复核安全网触发原因。";
        }
        if (correctionCount > 0) {
            return "跨作业已有教师校正样本，优先补充高频校正标签的 eval。";
        }
        if (interventionEvalCandidateCount > 0) {
            return "跨作业已有课堂介入成效候选，建议沉淀教学建议和班级策略 eval fixture。";
        }
        if (interventionWaitingFollowupCount > 0) {
            return "跨作业已有课堂介入反馈，但仍在等待后续提交证据。";
        }
        if (lowConfidenceCount > 0) {
            return "跨作业存在低置信度诊断，建议抽查证据包和提示词。";
        }
        return "跨作业 AI 质量暂无明显告警，继续观察校正率和低置信度趋势。";
    }

    private AiQualityTrendResponse emptyTrend() {
        return AiQualityTrendResponse.builder()
                .assignmentCount(0)
                .analyzedSubmissionCount(0)
                .correctionCount(0)
                .evalCandidateCount(0)
                .interventionEvalCandidateCount(0)
                .interventionWaitingFollowupCount(0)
                .interventionImprovedCount(0)
                .interventionShiftedCount(0)
                .interventionStillStuckCount(0)
                .lowConfidenceCount(0)
                .highLeakRiskCount(0)
                .promptSafetyIncidentCount(0)
                .promptSafetyDowngradeCount(0)
                .promptSafetyHighRiskDowngradeCount(0)
                .correctionRate(0)
                .lowConfidenceRate(0)
                .highLeakRiskRate(0)
                .promptSafetyIncidentRate(0)
                .summary("还没有作业，暂时无法形成跨作业 AI 质量趋势。")
                .assignments(List.of())
                .correctedTags(List.of())
                .evalNeededTags(List.of())
                .sourceSegments(List.of())
                .build();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private static class TagAccumulator {
        private final String tag;
        private long count;
        private long evalCandidateCount;

        private TagAccumulator(String tag) {
            this.tag = tag;
        }

        private long getCount() {
            return count;
        }

        private String getTag() {
            return tag;
        }
    }

    private record InterventionTrendCounts(long candidateCount,
                                           long waitingFollowupCount,
                                           long improvedCount,
                                           long shiftedCount,
                                           long stillStuckCount) {
    }

    private record PromptSafetyTrendCounts(long incidentCount,
                                           long downgradeCount,
                                           long highRiskDowngradeCount) {
    }

    private int riskWeight(String risk) {
        return switch (risk == null ? "" : risk.trim().toUpperCase(Locale.ROOT)) {
            case "HIGH" -> 3;
            case "MEDIUM" -> 2;
            case "LOW" -> 1;
            default -> 0;
        };
    }

    private static class SourceAccumulator {
        private final String sourceType;
        private final String versionLabel;
        private final String provider;
        private final String model;
        private final String modelVersion;
        private final String promptVersion;
        private final String agentVersion;
        private final String status;
        private long analyzedSubmissionCount;
        private long correctionCount;
        private long lowConfidenceCount;
        private long highLeakRiskCount;
        private long promptSafetyIncidentCount;
        private long promptSafetyDowngradeCount;
        private long promptSafetyHighRiskDowngradeCount;
        private long fallbackCount;

        private SourceAccumulator(String sourceType, String versionLabel) {
            this.sourceType = sourceType;
            this.versionLabel = versionLabel;
            this.provider = "";
            this.model = "";
            this.modelVersion = "";
            this.promptVersion = "";
            this.agentVersion = "";
            this.status = "";
        }

        private SourceAccumulator(SubmissionAnalysis analysis,
                                  DiagnosisReportReader diagnosisReportReader) {
            DiagnosisReportReader.AiInvocationSnapshot invocation = diagnosisReportReader.aiInvocation(analysis);
            this.sourceType = analysis == null ? "UNKNOWN" : staticFirstNonBlank(analysis.getAnalysisSource(), "UNKNOWN");
            this.versionLabel = analysis == null ? "unknown" : buildVersionLabel(analysis, invocation, diagnosisReportReader);
            this.provider = invocation == null ? "" : invocation.provider();
            this.model = invocation == null ? "" : invocation.model();
            this.modelVersion = invocation == null ? "" : invocation.modelVersion();
            this.promptVersion = invocation == null ? "" : invocation.promptVersion();
            this.agentVersion = invocation == null ? "" : invocation.agentVersion();
            this.status = invocation == null ? "" : invocation.status();
            this.fallbackCount = 0;
        }

        private void recordInvocation(SubmissionAnalysis analysis,
                                      DiagnosisReportReader diagnosisReportReader) {
            DiagnosisReportReader.AiInvocationSnapshot invocation = diagnosisReportReader.aiInvocation(analysis);
            if (invocation != null && invocation.fallbackUsed()) {
                fallbackCount++;
            }
        }

        private long getAnalyzedSubmissionCount() {
            return analyzedSubmissionCount;
        }

        private long getCorrectionCount() {
            return correctionCount;
        }

        private String getSourceType() {
            return sourceType;
        }

        private String getVersionLabel() {
            return versionLabel;
        }
    }

    private static String buildVersionLabel(SubmissionAnalysis analysis,
                                            DiagnosisReportReader.AiInvocationSnapshot invocation,
                                            DiagnosisReportReader diagnosisReportReader) {
        if (invocation != null) {
            return String.join(" / ",
                    staticFirstNonBlank(invocation.analysisSchemaVersion(), diagnosisReportReader.analysisSchemaVersion(analysis)),
                    staticFirstNonBlank(invocation.agentVersion(), "unknown-agent"),
                    staticFirstNonBlank(invocation.promptVersion(), "unknown-prompt"),
                    staticFirstNonBlank(invocation.modelVersion(), invocation.model(), "unknown-model")
            );
        }
        String schemaVersion = diagnosisReportReader.analysisSchemaVersion(analysis);
        String trace = diagnosisReportReader.diagnosticTrace(analysis);
        String agentVersion = staticExtractTraceToken(trace, "diagnostic-agent:");
        if (agentVersion.isBlank()) {
            agentVersion = staticExtractTraceToken(trace, "diagnostic-agent-");
        }
        return agentVersion.isBlank() ? schemaVersion : schemaVersion + " / agent-" + agentVersion;
    }

    private static String staticFirstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private static String staticExtractTraceToken(String trace, String prefix) {
        if (trace == null || trace.isBlank()) {
            return "";
        }
        int start = trace.indexOf(prefix);
        if (start < 0) {
            return "";
        }
        int valueStart = start + prefix.length();
        int end = trace.indexOf(' ', valueStart);
        if (end < 0) {
            end = trace.length();
        }
        return trace.substring(valueStart, end).trim();
    }
}
