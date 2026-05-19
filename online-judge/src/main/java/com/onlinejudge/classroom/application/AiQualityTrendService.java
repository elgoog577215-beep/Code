package com.onlinejudge.classroom.application;

import com.onlinejudge.classroom.domain.Assignment;
import com.onlinejudge.classroom.domain.TeacherDiagnosisCorrection;
import com.onlinejudge.classroom.dto.AiQualityTrendResponse;
import com.onlinejudge.classroom.persistence.AssignmentRepository;
import com.onlinejudge.classroom.persistence.TeacherDiagnosisCorrectionRepository;
import com.onlinejudge.learning.diagnosis.DiagnosisReportReader;
import com.onlinejudge.learning.diagnosis.DiagnosisTaxonomy;
import com.onlinejudge.submission.domain.Submission;
import com.onlinejudge.submission.domain.SubmissionAnalysis;
import com.onlinejudge.submission.persistence.SubmissionAnalysisRepository;
import com.onlinejudge.submission.persistence.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
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
        List<TeacherDiagnosisCorrection> corrections = teacherDiagnosisCorrectionRepository.findByAssignmentIdIn(assignmentIds);
        Map<Long, List<TeacherDiagnosisCorrection>> correctionsByAssignment = corrections.stream()
                .filter(correction -> correction.getAssignmentId() != null)
                .collect(Collectors.groupingBy(TeacherDiagnosisCorrection::getAssignmentId, LinkedHashMap::new, Collectors.toList()));

        List<AiQualityTrendResponse.AssignmentQualityPoint> points = assignmentIds.stream()
                .map(assignmentId -> buildPoint(
                        assignmentById.get(assignmentId),
                        analysesFor(submissionsByAssignment.get(assignmentId), analysesBySubmissionId),
                        correctionsByAssignment.getOrDefault(assignmentId, List.of())
                ))
                .toList();

        long analyzed = points.stream().mapToLong(AiQualityTrendResponse.AssignmentQualityPoint::getAnalyzedSubmissionCount).sum();
        long correctionCount = points.stream().mapToLong(AiQualityTrendResponse.AssignmentQualityPoint::getCorrectionCount).sum();
        long evalCandidateCount = points.stream().mapToLong(AiQualityTrendResponse.AssignmentQualityPoint::getEvalCandidateCount).sum();
        long lowConfidenceCount = points.stream().mapToLong(AiQualityTrendResponse.AssignmentQualityPoint::getLowConfidenceCount).sum();
        long highLeakRiskCount = points.stream().mapToLong(AiQualityTrendResponse.AssignmentQualityPoint::getHighLeakRiskCount).sum();

        return AiQualityTrendResponse.builder()
                .assignmentCount(points.size())
                .analyzedSubmissionCount(analyzed)
                .correctionCount(correctionCount)
                .evalCandidateCount(evalCandidateCount)
                .lowConfidenceCount(lowConfidenceCount)
                .highLeakRiskCount(highLeakRiskCount)
                .correctionRate(AiQualityMetrics.rate(correctionCount, analyzed))
                .lowConfidenceRate(AiQualityMetrics.rate(lowConfidenceCount, analyzed))
                .highLeakRiskRate(AiQualityMetrics.rate(highLeakRiskCount, analyzed))
                .summary(buildSummary(analyzed, correctionCount, lowConfidenceCount, highLeakRiskCount))
                .assignments(points)
                .correctedTags(buildCorrectedTags(corrections))
                .evalNeededTags(buildEvalNeededTags(corrections))
                .sourceSegments(buildSourceSegments(analysesBySubmissionId, corrections))
                .build();
    }

    private AiQualityTrendResponse.AssignmentQualityPoint buildPoint(Assignment assignment,
                                                                     List<SubmissionAnalysis> analyses,
                                                                     List<TeacherDiagnosisCorrection> corrections) {
        AiQualityMetrics metrics = AiQualityMetrics.from(analyses, corrections, diagnosisReportReader);
        return AiQualityTrendResponse.AssignmentQualityPoint.builder()
                .assignmentId(assignment == null ? null : assignment.getId())
                .assignmentTitle(assignment == null ? "" : assignment.getTitle())
                .analyzedSubmissionCount(metrics.analyzedSubmissionCount())
                .correctionCount(metrics.correctionCount())
                .evalCandidateCount(metrics.evalCandidateCount())
                .lowConfidenceCount(metrics.lowConfidenceCount())
                .highLeakRiskCount(metrics.highLeakRiskCount())
                .correctionRate(metrics.correctionRate())
                .lowConfidenceRate(metrics.lowConfidenceRate())
                .highLeakRiskRate(metrics.highLeakRiskRate())
                .summary(buildSummary(metrics.analyzedSubmissionCount(), metrics.correctionCount(), metrics.lowConfidenceCount(), metrics.highLeakRiskCount()))
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
                                                                                  List<TeacherDiagnosisCorrection> corrections) {
        Map<String, SourceAccumulator> accumulators = new LinkedHashMap<>();
        analysesBySubmissionId.values().stream()
                .filter(Objects::nonNull)
                .forEach(analysis -> {
                    String key = sourceSegmentKey(analysis);
                    SourceAccumulator accumulator = accumulators.computeIfAbsent(key, ignored -> new SourceAccumulator(
                            firstNonBlank(analysis.getAnalysisSource(), "UNKNOWN"),
                            versionLabel(analysis)
                    ));
                    accumulator.analyzedSubmissionCount++;
                    Double confidence = diagnosisReportReader.confidence(analysis);
                    if (confidence == null || confidence < AiQualityMetrics.LOW_CONFIDENCE_THRESHOLD) {
                        accumulator.lowConfidenceCount++;
                    }
                    if ("HIGH".equalsIgnoreCase(diagnosisReportReader.answerLeakRisk(analysis))) {
                        accumulator.highLeakRiskCount++;
                    }
                });

        Map<Long, SubmissionAnalysis> safeAnalyses = analysesBySubmissionId == null ? Map.of() : analysesBySubmissionId;
        for (TeacherDiagnosisCorrection correction : corrections == null ? List.<TeacherDiagnosisCorrection>of() : corrections) {
            SubmissionAnalysis analysis = safeAnalyses.get(correction.getSubmissionId());
            String key = analysis == null ? "UNKNOWN|unknown" : sourceSegmentKey(analysis);
            SourceAccumulator accumulator = accumulators.computeIfAbsent(key, ignored -> new SourceAccumulator(
                    analysis == null ? "UNKNOWN" : firstNonBlank(analysis.getAnalysisSource(), "UNKNOWN"),
                    analysis == null ? "unknown" : versionLabel(analysis)
            ));
            accumulator.correctionCount++;
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
                        .analyzedSubmissionCount(accumulator.analyzedSubmissionCount)
                        .correctionCount(accumulator.correctionCount)
                        .lowConfidenceCount(accumulator.lowConfidenceCount)
                        .highLeakRiskCount(accumulator.highLeakRiskCount)
                        .correctionRate(AiQualityMetrics.rate(accumulator.correctionCount, accumulator.analyzedSubmissionCount))
                        .lowConfidenceRate(AiQualityMetrics.rate(accumulator.lowConfidenceCount, accumulator.analyzedSubmissionCount))
                        .highLeakRiskRate(AiQualityMetrics.rate(accumulator.highLeakRiskCount, accumulator.analyzedSubmissionCount))
                        .build())
                .toList();
    }

    private String sourceSegmentKey(SubmissionAnalysis analysis) {
        return firstNonBlank(analysis.getAnalysisSource(), "UNKNOWN") + "|" + versionLabel(analysis);
    }

    private String versionLabel(SubmissionAnalysis analysis) {
        String schemaVersion = diagnosisReportReader.analysisSchemaVersion(analysis);
        String trace = diagnosisReportReader.diagnosticTrace(analysis);
        String agentVersion = extractTraceToken(trace, "diagnostic-agent:");
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

    private String buildSummary(long analyzedCount, long correctionCount, long lowConfidenceCount, long highLeakRiskCount) {
        if (analyzedCount == 0) {
            return "还没有跨作业 AI 诊断样本。";
        }
        if (highLeakRiskCount > 0) {
            return "跨作业范围内出现高泄题风险样本，建议优先复核。";
        }
        if (correctionCount > 0) {
            return "跨作业已有教师校正样本，优先补充高频校正标签的 eval。";
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
                .lowConfidenceCount(0)
                .highLeakRiskCount(0)
                .correctionRate(0)
                .lowConfidenceRate(0)
                .highLeakRiskRate(0)
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

    private static class SourceAccumulator {
        private final String sourceType;
        private final String versionLabel;
        private long analyzedSubmissionCount;
        private long correctionCount;
        private long lowConfidenceCount;
        private long highLeakRiskCount;

        private SourceAccumulator(String sourceType, String versionLabel) {
            this.sourceType = sourceType;
            this.versionLabel = versionLabel;
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
}
