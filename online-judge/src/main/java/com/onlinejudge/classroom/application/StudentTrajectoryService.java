package com.onlinejudge.classroom.application;

import com.onlinejudge.classroom.dto.AssignmentResponse;
import com.onlinejudge.classroom.dto.StudentProfileResponse;
import com.onlinejudge.classroom.dto.StudentTrajectoryResponse;
import com.onlinejudge.classroom.persistence.StudentProfileRepository;
import com.onlinejudge.learning.diagnosis.DiagnosisReportReader;
import com.onlinejudge.learning.diagnosis.DiagnosisTaxonomy;
import com.onlinejudge.problem.domain.Problem;
import com.onlinejudge.problem.persistence.ProblemRepository;
import com.onlinejudge.submission.domain.Submission;
import com.onlinejudge.submission.domain.SubmissionAnalysis;
import com.onlinejudge.submission.persistence.SubmissionAnalysisRepository;
import com.onlinejudge.submission.persistence.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentTrajectoryService {

    private final ClassroomService classroomService;
    private final StudentProfileRepository studentProfileRepository;
    private final ProblemRepository problemRepository;
    private final SubmissionRepository submissionRepository;
    private final SubmissionAnalysisRepository submissionAnalysisRepository;
    private final DiagnosisTaxonomy diagnosisTaxonomy;
    private final DiagnosisReportReader diagnosisReportReader;
    private final TrajectorySignalAnalyzer trajectorySignalAnalyzer;
    private final AbilitySignalAnalyzer abilitySignalAnalyzer;
    private final CoachInteractionAnalyzer coachInteractionAnalyzer;

    public StudentTrajectoryResponse buildTrajectory(Long assignmentId, Long studentProfileId) {
        AssignmentResponse assignment = classroomService.getAssignment(assignmentId);
        var student = studentProfileRepository.findById(studentProfileId)
                .orElseThrow(() -> new IllegalArgumentException("学生画像不存在: " + studentProfileId));
        List<Submission> submissions = submissionRepository
                .findByAssignmentIdAndStudentProfileIdOrderBySubmittedAtDesc(assignmentId, studentProfileId);
        List<Long> submissionIds = submissions.stream().map(Submission::getId).toList();
        Map<Long, SubmissionAnalysis> analyses = submissionIds.isEmpty()
                ? Map.of()
                : submissionAnalysisRepository.findBySubmissionIdIn(submissionIds)
                .stream()
                .collect(Collectors.toMap(SubmissionAnalysis::getSubmissionId, Function.identity()));
        Map<Long, com.onlinejudge.classroom.dto.CoachInteractionSummaryResponse> coachInteractions =
                coachInteractionAnalyzer.summarize(submissionIds);

        Map<Long, Problem> problems = problemRepository.findAllById(assignment.getTasks().stream()
                        .map(AssignmentResponse.TaskSummary::getProblemId)
                        .toList())
                .stream()
                .collect(Collectors.toMap(Problem::getId, Function.identity()));

        List<StudentTrajectoryResponse.TaskTrajectory> taskTrajectories = assignment.getTasks().stream()
                .map(task -> buildTaskTrajectory(task, problems.get(task.getProblemId()), submissions, analyses, coachInteractions))
                .toList();

        List<String> recentTags = submissions.stream()
                .limit(10)
                .map(submission -> analyses.get(submission.getId()))
                .filter(Objects::nonNull)
                .flatMap(analysis -> diagnosisReportReader.issueTags(analysis).stream())
                .toList();
        Map<String, Long> tagCounts = new LinkedHashMap<>();
        recentTags.forEach(tag -> tagCounts.put(tag, tagCounts.getOrDefault(tag, 0L) + 1));

        List<String> recentFineTags = submissions.stream()
                .limit(10)
                .map(submission -> analyses.get(submission.getId()))
                .filter(Objects::nonNull)
                .flatMap(analysis -> diagnosisReportReader.fineGrainedTags(analysis).stream())
                .toList();
        Map<String, Long> fineTagCounts = new LinkedHashMap<>();
        recentFineTags.forEach(tag -> fineTagCounts.put(tag, fineTagCounts.getOrDefault(tag, 0L) + 1));

        Map.Entry<String, Long> repeated = tagCounts.entrySet().stream()
                .filter(entry -> !List.of("CODE_QUALITY", "GENERALIZATION_CHECK").contains(entry.getKey()))
                .max(Map.Entry.comparingByValue())
                .orElse(null);
        Map.Entry<String, Long> repeatedFine = fineTagCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElse(null);
        String stageTransition = buildStageTransition(submissions);
        String nextStep = buildNextStep(repeatedFine, repeated, submissions);
        String attentionReason = buildAttentionReason(repeatedFine, repeated, submissions);
        String improvementSignal = buildLatestImprovementSignal(submissions, analyses);
        List<AbilitySignalAnalyzer.AbilitySignal> abilitySignals = abilitySignalAnalyzer.summarize(submissions, analyses);
        String primaryAbilityFocus = abilitySignals.stream()
                .findFirst()
                .map(AbilitySignalAnalyzer.AbilitySignal::getAbilityPoint)
                .orElse(null);
        String crossProblemSummary = abilitySignalAnalyzer.buildCrossProblemSummary(submissions, analyses);
        String largeChangeSameIssue = trajectorySignalAnalyzer.detectLargeChangeSameIssue(submissions, analyses);
        if (!largeChangeSameIssue.isBlank()) {
            nextStep = "先暂停整体重写。请用一个最小样例验证单个假设。";
            attentionReason = largeChangeSameIssue;
            improvementSignal = largeChangeSameIssue;
        }

        return StudentTrajectoryResponse.builder()
                .assignment(assignment)
                .student(StudentProfileResponse.from(student))
                .totalTasks(assignment.getTasks().size())
                .completedTasks((int) taskTrajectories.stream().filter(StudentTrajectoryResponse.TaskTrajectory::isPassed).count())
                .totalAttempts(submissions.size())
                .stageTransition(stageTransition)
                .repeatedIssueTag(repeated == null ? null : repeated.getKey())
                .repeatedFineGrainedTag(repeatedFine == null ? null : repeatedFine.getKey())
                .repeatedIssueCount(repeated == null ? 0 : repeated.getValue())
                .nextStep(nextStep)
                .attentionReason(attentionReason)
                .improvementSignal(improvementSignal)
                .primaryAbilityFocus(primaryAbilityFocus)
                .crossProblemSummary(crossProblemSummary)
                .latestCoachInteraction(coachInteractionAnalyzer.latestForOrderedSubmissions(submissionIds, coachInteractions))
                .recentIssueDistribution(tagCounts.entrySet().stream()
                        .map(entry -> StudentTrajectoryResponse.IssueStat.builder()
                                .label(entry.getKey())
                                .count(entry.getValue())
                                .build())
                        .toList())
                .recentFineGrainedIssueDistribution(fineTagCounts.entrySet().stream()
                        .map(entry -> StudentTrajectoryResponse.IssueStat.builder()
                                .label(entry.getKey())
                                .count(entry.getValue())
                                .build())
                        .toList())
                .abilitySummary(abilitySignals.stream()
                        .map(signal -> StudentTrajectoryResponse.AbilityStat.builder()
                                .abilityPoint(signal.getAbilityPoint())
                                .taskCount(signal.getTaskCount())
                                .submissionCount(signal.getSubmissionCount())
                                .evidenceTags(signal.getEvidenceTags())
                                .build())
                        .toList())
                .tasks(taskTrajectories)
                .build();
    }

    private StudentTrajectoryResponse.TaskTrajectory buildTaskTrajectory(AssignmentResponse.TaskSummary task,
                                                                         Problem problem,
                                                                         List<Submission> allSubmissions,
                                                                         Map<Long, SubmissionAnalysis> analyses) {
        return buildTaskTrajectory(task, problem, allSubmissions, analyses, Map.of());
    }

    private StudentTrajectoryResponse.TaskTrajectory buildTaskTrajectory(AssignmentResponse.TaskSummary task,
                                                                         Problem problem,
                                                                         List<Submission> allSubmissions,
                                                                         Map<Long, SubmissionAnalysis> analyses,
                                                                         Map<Long, com.onlinejudge.classroom.dto.CoachInteractionSummaryResponse> coachInteractions) {
        List<Submission> submissions = allSubmissions.stream()
                .filter(submission -> Objects.equals(submission.getProblemId(), task.getProblemId()))
                .sorted(Comparator.comparing(Submission::getSubmittedAt, Comparator.nullsLast(LocalDateTime::compareTo)).reversed())
                .toList();
        Submission latest = submissions.isEmpty() ? null : submissions.get(0);
        SubmissionAnalysis latestAnalysis = latest == null ? null : analyses.get(latest.getId());
        String latestImprovementSignal = buildLatestImprovementSignal(submissions, analyses);

        return StudentTrajectoryResponse.TaskTrajectory.builder()
                .problemId(task.getProblemId())
                .title(problem == null ? task.getTitle() : problem.getTitle())
                .difficulty(problem == null || problem.getDifficulty() == null ? task.getDifficulty() : problem.getDifficulty().name())
                .attemptCount(submissions.size())
                .passed(submissions.stream().anyMatch(submission -> submission.getVerdict() == Submission.Verdict.ACCEPTED))
                .latestVerdict(latest == null || latest.getVerdict() == null ? "暂无提交" : latest.getVerdict().name())
                .latestProgressSignal(resolveProgressSignal(latestAnalysis, latest))
                .latestHint(extractStudentHint(latestAnalysis))
                .latestImprovementSignal(latestImprovementSignal)
                .latestCoachInteraction(latest == null ? null : coachInteractions.get(latest.getId()))
                .submissions(submissions.stream()
                        .limit(8)
                        .map(submission -> StudentTrajectoryResponse.SubmissionPoint.builder()
                                .submissionId(submission.getId())
                                .verdict(submission.getVerdict() == null ? "UNKNOWN" : submission.getVerdict().name())
                                .submittedAt(submission.getSubmittedAt())
                                .issueTags(diagnosisReportReader.issueTags(analyses.get(submission.getId())))
                                .fineGrainedTags(diagnosisReportReader.fineGrainedTags(analyses.get(submission.getId())))
                                .progressSignal(resolveProgressSignal(analyses.get(submission.getId()), submission))
                                .improvementSignal(buildPointImprovementSignal(submission, submissions))
                                .coachInteraction(coachInteractions.get(submission.getId()))
                                .build())
                        .toList())
                .build();
    }

    private String buildStageTransition(List<Submission> submissions) {
        if (submissions.isEmpty()) {
            return "尚未开始提交";
        }
        List<String> stages = submissions.stream()
                .sorted(Comparator.comparing(Submission::getSubmittedAt, Comparator.nullsLast(LocalDateTime::compareTo)))
                .map(submission -> submission.getVerdict() == null ? "UNKNOWN" : submission.getVerdict().name())
                .distinct()
                .toList();
        if (stages.contains("ACCEPTED")) {
            if (stages.contains("WRONG_ANSWER") || stages.contains("TIME_LIMIT_EXCEEDED")) {
                return "已经从调试阶段推进到通过阶段";
            }
            return "已经完成至少一个任务，可进入复盘优化";
        }
        if (stages.contains("TIME_LIMIT_EXCEEDED")) {
            return "正确性可能接近，复杂度成为主要阻碍";
        }
        if (stages.contains("WRONG_ANSWER")) {
            return "正在正确性调试阶段，重点看边界和条件分支";
        }
        if (stages.contains("COMPILATION_ERROR")) {
            return "仍处在语法或编译环境修正阶段";
        }
        return "需要更多提交证据判断学习阶段";
    }

    private String buildNextStep(Map.Entry<String, Long> repeatedFine,
                                 Map.Entry<String, Long> repeated,
                                 List<Submission> submissions) {
        if (submissions.isEmpty()) {
            return "先选择一个任务完成第一次提交，让平台开始记录你的学习轨迹。";
        }
        if (repeatedFine != null && repeatedFine.getValue() >= 2) {
            return "下一次只优先处理：" + diagnosisTaxonomy.label(repeatedFine.getKey()) + "。先构造一个最小样例验证，再改代码。";
        }
        if (repeated != null && repeated.getValue() >= 2) {
            return "下一次只优先处理：" + diagnosisTaxonomy.label(repeated.getKey()) + "。先自造一个最小样例手推，再改代码。";
        }
        Submission latest = submissions.get(0);
        if (latest.getVerdict() == Submission.Verdict.ACCEPTED) {
            return "本轮已通过。下一步请补一个边界样例，并用自己的话说明复杂度。";
        }
        return "下一次不要整体重写。先根据最新 AI 提示定位一个最小可疑点，再提交。";
    }

    private String buildAttentionReason(Map.Entry<String, Long> repeatedFine,
                                        Map.Entry<String, Long> repeated,
                                        List<Submission> submissions) {
        if (submissions.isEmpty()) {
            return "还没有提交证据，暂不判断学习状态。";
        }
        if (repeatedFine != null && repeatedFine.getValue() >= 2) {
            return "最近多次出现细分卡点：" + diagnosisTaxonomy.label(repeatedFine.getKey()) + "。";
        }
        if (repeated != null && repeated.getValue() >= 2) {
            return "最近多次出现同类问题：" + diagnosisTaxonomy.label(repeated.getKey()) + "。";
        }
        long failedAttempts = submissions.stream()
                .filter(submission -> submission.getVerdict() != Submission.Verdict.ACCEPTED)
                .count();
        if (failedAttempts >= 3) {
            return "已经有多次未通过提交，建议先缩小调试范围。";
        }
        return "当前轨迹没有明显重复卡点，继续观察下一次提交。";
    }

    private String buildLatestImprovementSignal(List<Submission> submissions,
                                                Map<Long, SubmissionAnalysis> analyses) {
        if (submissions == null || submissions.isEmpty()) {
            return "还没有提交，暂时无法判断改善。";
        }
        if (submissions.size() == 1) {
            return "已完成第一次提交，下一次会开始对比变化。";
        }
        Submission latest = submissions.get(0);
        Submission previous = submissions.get(1);
        return compareSubmissionProgress(previous, latest, analyses);
    }

    private String buildPointImprovementSignal(Submission submission, List<Submission> sortedSubmissions) {
        if (submission == null || sortedSubmissions == null) {
            return "";
        }
        int index = sortedSubmissions.indexOf(submission);
        if (index < 0 || index + 1 >= sortedSubmissions.size()) {
            return index == sortedSubmissions.size() - 1 ? "第一次提交" : "";
        }
        return compareVerdictTransition(sortedSubmissions.get(index + 1), submission);
    }

    private String compareSubmissionProgress(Submission previous,
                                             Submission latest,
                                             Map<Long, SubmissionAnalysis> analyses) {
        String transition = compareVerdictTransition(previous, latest);
        if (!transition.isBlank()) {
            return transition;
        }
        List<String> previousFine = diagnosisReportReader.fineGrainedTags(analyses.get(previous.getId()));
        List<String> latestFine = diagnosisReportReader.fineGrainedTags(analyses.get(latest.getId()));
        if (!previousFine.isEmpty() && latestFine.stream().noneMatch(previousFine::contains)) {
            return "细分卡点发生变化，说明修改已经推动问题进入新阶段。";
        }
        if (!latestFine.isEmpty() && latestFine.stream().anyMatch(previousFine::contains)) {
            return "仍在同一细分卡点上，需要先缩小调试范围。";
        }
        return "评测结果暂未明显变化，请结合最新提示继续观察。";
    }

    private String compareVerdictTransition(Submission previous, Submission latest) {
        String previousVerdict = previous == null || previous.getVerdict() == null ? "UNKNOWN" : previous.getVerdict().name();
        String latestVerdict = latest == null || latest.getVerdict() == null ? "UNKNOWN" : latest.getVerdict().name();
        if (previousVerdict.equals(latestVerdict)) {
            return "";
        }
        if ("ACCEPTED".equals(latestVerdict)) {
            return "这次已经从“" + readableVerdict(previousVerdict) + "”推进到“已通过”。";
        }
        if ("COMPILATION_ERROR".equals(previousVerdict) && !"COMPILATION_ERROR".equals(latestVerdict)) {
            return "这次已经越过语法/编译阶段，进入运行或逻辑调试。";
        }
        if ("WRONG_ANSWER".equals(previousVerdict) && "TIME_LIMIT_EXCEEDED".equals(latestVerdict)) {
            return "正确性可能接近了一些，但复杂度成为新的阻碍。";
        }
        if (!"RUNTIME_ERROR".equals(previousVerdict) && "RUNTIME_ERROR".equals(latestVerdict)) {
            return "本次修改引入了运行稳定性问题，建议回看最近改动。";
        }
        return "评测阶段从“" + readableVerdict(previousVerdict) + "”变化为“" + readableVerdict(latestVerdict) + "”。";
    }

    private String readableVerdict(String verdict) {
        return switch (verdict) {
            case "ACCEPTED" -> "已通过";
            case "WRONG_ANSWER" -> "答案需修正";
            case "TIME_LIMIT_EXCEEDED" -> "时间超限";
            case "MEMORY_LIMIT_EXCEEDED" -> "内存超限";
            case "RUNTIME_ERROR" -> "运行错误";
            case "COMPILATION_ERROR" -> "编译错误";
            default -> "待观察";
        };
    }

    private String extractStudentHint(SubmissionAnalysis analysis) {
        return diagnosisReportReader.studentHint(analysis);
    }

    private String resolveProgressSignal(SubmissionAnalysis analysis, Submission submission) {
        String signal = diagnosisReportReader.progressSignal(analysis);
        if (!signal.isBlank()) {
            return signal;
        }
        if (submission == null || submission.getVerdict() == null) {
            return "等待第一次提交";
        }
        return switch (submission.getVerdict()) {
            case ACCEPTED -> "已通过，进入复盘优化阶段";
            case WRONG_ANSWER -> "正确性调试中，重点观察边界和条件分支";
            case TIME_LIMIT_EXCEEDED -> "复杂度是当前阻碍";
            case COMPILATION_ERROR -> "语法或编译环境需要先过关";
            case RUNTIME_ERROR -> "程序稳定性需要优先修复";
            default -> "需要继续观察下一次提交";
        };
    }
}
