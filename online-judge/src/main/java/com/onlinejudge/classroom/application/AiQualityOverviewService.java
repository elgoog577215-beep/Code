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
                .modelFallbackCount(metrics.modelFallbackCount())
                .modelPartialCount(metrics.modelPartialCount())
                .modelRuntimeFailureCount(metrics.modelRuntimeFailureCount())
                .modelCompletedCount(metrics.modelCompletedCount())
                .correctionRate(metrics.correctionRate())
                .lowConfidenceRate(metrics.lowConfidenceRate())
                .highLeakRiskRate(metrics.highLeakRiskRate())
                .modelFallbackRate(metrics.modelFallbackRate())
                .modelRuntimeFailureRate(metrics.modelRuntimeFailureRate())
                .summary(buildSummary(metrics))
                .qualityRiskSummary(buildQualityRiskSummary(metrics))
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

    private String buildSummary(AiQualityMetrics metrics) {
        if (metrics.analyzedSubmissionCount() == 0) {
            return "当前作业还没有 AI 诊断样本。";
        }
        if (metrics.modelRuntimeFailureCount() > 0) {
            return "存在外部模型兜底或运行失败，建议先检查模型调用与 prompt 输出稳定性。";
        }
        if (metrics.modelPartialCount() > 0) {
            return "存在外部模型部分完成样本，建议复核第二阶段教学提示是否稳定。";
        }
        if (metrics.highLeakRiskCount() > 0) {
            return "存在高泄题风险诊断，建议先复核这些提交再开放给学生。";
        }
        if (metrics.correctionCount() > 0) {
            return "已有教师校正样例，建议优先沉淀进模型 eval。";
        }
        if (metrics.lowConfidenceCount() > 0) {
            return "存在低置信度诊断，适合人工抽查补证据。";
        }
        return "当前 AI 诊断暂无明显质量告警，继续观察教师校正和低置信度样本。";
    }

    private String buildQualityRiskSummary(AiQualityMetrics metrics) {
        if (metrics.analyzedSubmissionCount() == 0) {
            return "暂无可分析的模型质量风险。";
        }
        if (metrics.modelRuntimeFailureCount() > 0) {
            return "模型调用质量风险最高：存在兜底或运行失败，在线效果可能没有真正使用外部大模型。";
        }
        if (metrics.modelPartialCount() > 0) {
            return "模型阶段质量风险较高：诊断可能完成，但教学提示阶段存在部分失败。";
        }
        if (metrics.highLeakRiskCount() > 0) {
            return "教学安全风险较高：存在高泄题风险输出，需要优先人工复核。";
        }
        if (metrics.correctionCount() > 0) {
            return "教学准确性风险可沉淀：教师校正样例应进入评测集和标准库迭代。";
        }
        if (metrics.lowConfidenceCount() > 0) {
            return "证据充分性风险存在：低置信度样本适合作为人工抽检队列。";
        }
        return "模型调用和教学输出暂未出现明显质量风险。";
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
