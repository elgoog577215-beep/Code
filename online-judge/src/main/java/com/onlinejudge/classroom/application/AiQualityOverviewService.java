package com.onlinejudge.classroom.application;

import com.onlinejudge.classroom.domain.TeacherDiagnosisCorrection;
import com.onlinejudge.classroom.dto.AiQualityOverviewResponse;
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AiQualityOverviewService {

    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final SubmissionAnalysisRepository submissionAnalysisRepository;
    private final TeacherDiagnosisCorrectionRepository teacherDiagnosisCorrectionRepository;
    private final DiagnosisReportReader diagnosisReportReader;
    private final DiagnosisTaxonomy diagnosisTaxonomy;

    public AiQualityOverviewResponse buildOverview(Long assignmentId) {
        if (!assignmentRepository.existsById(assignmentId)) {
            throw new IllegalArgumentException("作业不存在: " + assignmentId);
        }
        List<Submission> submissions = submissionRepository.findByAssignmentIdOrderBySubmittedAtDesc(assignmentId);
        List<Long> submissionIds = submissions.stream()
                .map(Submission::getId)
                .filter(Objects::nonNull)
                .toList();
        List<SubmissionAnalysis> analyses = submissionIds.isEmpty()
                ? List.of()
                : submissionAnalysisRepository.findBySubmissionIdIn(submissionIds);
        List<TeacherDiagnosisCorrection> corrections = teacherDiagnosisCorrectionRepository.findByAssignmentIdOrderByCorrectedAtDesc(assignmentId);
        AiQualityMetrics metrics = AiQualityMetrics.from(analyses, corrections, diagnosisReportReader);

        return AiQualityOverviewResponse.builder()
                .assignmentId(assignmentId)
                .analyzedSubmissionCount(metrics.analyzedSubmissionCount())
                .correctionCount(metrics.correctionCount())
                .evalCandidateCount(metrics.evalCandidateCount())
                .lowConfidenceCount(metrics.lowConfidenceCount())
                .highLeakRiskCount(metrics.highLeakRiskCount())
                .correctionRate(metrics.correctionRate())
                .lowConfidenceRate(metrics.lowConfidenceRate())
                .highLeakRiskRate(metrics.highLeakRiskRate())
                .summary(buildSummary(metrics.analyzedSubmissionCount(), metrics.correctionCount(), metrics.lowConfidenceCount(), metrics.highLeakRiskCount()))
                .correctedTags(buildCorrectedTags(corrections))
                .build();
    }

    private List<AiQualityOverviewResponse.TagCorrectionStat> buildCorrectedTags(List<TeacherDiagnosisCorrection> corrections) {
        Map<String, Long> counts = new LinkedHashMap<>();
        Map<String, TeacherDiagnosisCorrection> sampleByKey = new LinkedHashMap<>();
        for (TeacherDiagnosisCorrection correction : corrections) {
            String original = firstNonBlank(correction.getOriginalFineGrainedTag(), correction.getOriginalIssueTag(), "UNKNOWN");
            String corrected = firstNonBlank(correction.getCorrectedFineGrainedTag(), correction.getCorrectedIssueTag(), "UNKNOWN");
            String key = original + "->" + corrected;
            counts.put(key, counts.getOrDefault(key, 0L) + 1L);
            sampleByKey.putIfAbsent(key, correction);
        }
        return counts.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(entry -> {
                    TeacherDiagnosisCorrection sample = sampleByKey.get(entry.getKey());
                    String original = firstNonBlank(sample.getOriginalFineGrainedTag(), sample.getOriginalIssueTag(), "UNKNOWN");
                    String corrected = firstNonBlank(sample.getCorrectedFineGrainedTag(), sample.getCorrectedIssueTag(), "UNKNOWN");
                    return AiQualityOverviewResponse.TagCorrectionStat.builder()
                            .originalTag(original)
                            .originalLabel(diagnosisTaxonomy.label(original))
                            .correctedTag(corrected)
                            .correctedLabel(diagnosisTaxonomy.label(corrected))
                            .count(entry.getValue())
                            .build();
                })
                .toList();
    }

    private String buildSummary(long analyzedCount, long correctionCount, long lowConfidenceCount, long highLeakRiskCount) {
        if (analyzedCount == 0) {
            return "当前作业还没有 AI 诊断样本。";
        }
        if (highLeakRiskCount > 0) {
            return "存在高泄题风险诊断，建议先复核这些提交再开放给学生。";
        }
        if (correctionCount > 0) {
            return "已有教师校正样例，建议优先沉淀进模型 eval。";
        }
        if (lowConfidenceCount > 0) {
            return "存在低置信度诊断，适合人工抽查补证据。";
        }
        return "当前 AI 诊断暂无明显质量告警，继续观察教师校正和低置信度样本。";
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }
}
