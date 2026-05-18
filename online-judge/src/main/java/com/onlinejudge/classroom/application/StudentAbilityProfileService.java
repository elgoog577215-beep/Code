package com.onlinejudge.classroom.application;

import com.onlinejudge.classroom.domain.StudentProfile;
import com.onlinejudge.classroom.dto.CoachInteractionSummaryResponse;
import com.onlinejudge.classroom.dto.StudentAbilityProfileResponse;
import com.onlinejudge.classroom.dto.StudentProfileResponse;
import com.onlinejudge.classroom.persistence.StudentProfileRepository;
import com.onlinejudge.problem.domain.Problem;
import com.onlinejudge.problem.persistence.ProblemRepository;
import com.onlinejudge.submission.domain.Submission;
import com.onlinejudge.submission.domain.SubmissionAnalysis;
import com.onlinejudge.submission.persistence.SubmissionAnalysisRepository;
import com.onlinejudge.submission.persistence.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentAbilityProfileService {

    private static final int RECENT_WINDOW = 40;

    private final StudentProfileRepository studentProfileRepository;
    private final SubmissionRepository submissionRepository;
    private final SubmissionAnalysisRepository submissionAnalysisRepository;
    private final ProblemRepository problemRepository;
    private final AbilitySignalAnalyzer abilitySignalAnalyzer;
    private final CoachInteractionAnalyzer coachInteractionAnalyzer;
    private final StudentIdentityService studentIdentityService;

    public StudentAbilityProfileResponse buildProfile(Long studentProfileId) {
        StudentProfile student = studentProfileRepository.findById(studentProfileId)
                .orElseThrow(() -> new IllegalArgumentException("学生画像不存在: " + studentProfileId));
        List<StudentProfile> mergedProfiles = findMergedProfiles(student);
        List<Long> profileIds = mergedProfiles.stream()
                .map(StudentProfile::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<Submission> submissions = profileIds.isEmpty()
                ? List.of()
                : submissionRepository.findByStudentProfileIdInOrderBySubmittedAtDesc(profileIds)
                .stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(Submission::getSubmittedAt, Comparator.nullsLast(LocalDateTime::compareTo)).reversed())
                .limit(RECENT_WINDOW)
                .toList();
        List<Long> submissionIds = submissions.stream()
                .map(Submission::getId)
                .filter(Objects::nonNull)
                .toList();
        Map<Long, SubmissionAnalysis> analyses = submissionIds.isEmpty()
                ? Map.of()
                : submissionAnalysisRepository.findBySubmissionIdIn(submissionIds)
                .stream()
                .collect(Collectors.toMap(SubmissionAnalysis::getSubmissionId, Function.identity(), (left, right) -> left));
        Map<Long, Problem> problems = problemRepository.findAllById(submissions.stream()
                        .map(Submission::getProblemId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList())
                .stream()
                .collect(Collectors.toMap(Problem::getId, Function.identity()));
        Map<Long, CoachInteractionSummaryResponse> coachInteractions = coachInteractionAnalyzer.summarize(submissionIds);
        List<AbilitySignalAnalyzer.AbilitySignal> abilitySignals = abilitySignalAnalyzer.summarize(submissions, analyses);
        List<StudentAbilityProfileResponse.AbilityStat> abilityGaps = abilitySignals.stream()
                .map(signal -> StudentAbilityProfileResponse.AbilityStat.builder()
                        .abilityPoint(signal.getAbilityPoint())
                        .taskCount(signal.getTaskCount())
                        .submissionCount(signal.getSubmissionCount())
                        .evidenceTags(signal.getEvidenceTags())
                        .build())
                .toList();

        return StudentAbilityProfileResponse.builder()
                .student(StudentProfileResponse.from(student))
                .mergedStudentProfileIds(profileIds)
                .totalSubmissions(submissions.size())
                .failedSubmissionCount(submissions.stream().filter(submission -> submission.getVerdict() != Submission.Verdict.ACCEPTED).count())
                .problemCount(submissions.stream().map(Submission::getProblemId).filter(Objects::nonNull).distinct().count())
                .assignmentCount(submissions.stream().map(Submission::getAssignmentId).filter(Objects::nonNull).distinct().count())
                .primaryAbilityFocus(abilitySignals.stream()
                        .findFirst()
                        .map(AbilitySignalAnalyzer.AbilitySignal::getAbilityPoint)
                        .orElse(null))
                .summary(buildSummary(submissions, abilitySignals))
                .trendSignal(buildTrendSignal(submissions, abilitySignals))
                .latestCoachInteraction(coachInteractionAnalyzer.latestForOrderedSubmissions(submissionIds, coachInteractions))
                .abilityGaps(abilityGaps)
                .knowledgeFocus(summarizeProblemTags(submissions, problems, Problem::getKnowledgePoints))
                .commonMistakeFocus(summarizeProblemTags(submissions, problems, Problem::getCommonMistakes))
                .boundaryFocus(summarizeProblemTags(submissions, problems, Problem::getBoundaryTypes))
                .build();
    }

    private List<StudentProfile> findMergedProfiles(StudentProfile student) {
        LinkedHashMap<Long, StudentProfile> merged = new LinkedHashMap<>();
        addProfile(merged, student);
        if (student.getClassGroupId() == null) {
            return List.copyOf(merged.values());
        }
        String stableIdentityKey = studentIdentityService.buildStableIdentityKey(
                student.getClassGroupId(),
                null,
                student.getDisplayName(),
                student.getStudentNo()
        );
        if (!stableIdentityKey.isBlank()) {
            addProfiles(merged, studentProfileRepository.findByIdentityKeyOrderByCreatedAtDesc(stableIdentityKey));
        }
        if (studentIdentityService.isStableIdentityKey(student.getIdentityKey())) {
            addProfiles(merged, studentProfileRepository.findByIdentityKeyOrderByCreatedAtDesc(student.getIdentityKey()));
        }
        String studentNo = normalize(student.getStudentNo());
        if (!studentNo.isBlank()) {
            addProfiles(merged, studentProfileRepository
                    .findByClassGroupIdAndStudentNoIgnoreCaseOrderByCreatedAtDesc(student.getClassGroupId(), studentNo));
            return List.copyOf(merged.values());
        }
        String displayName = normalize(student.getDisplayName());
        if (!displayName.isBlank()) {
            addProfiles(merged, studentProfileRepository
                    .findByClassGroupIdAndDisplayNameIgnoreCaseOrderByCreatedAtDesc(student.getClassGroupId(), displayName));
        }
        return List.copyOf(merged.values());
    }

    private void addProfiles(Map<Long, StudentProfile> merged, List<StudentProfile> candidates) {
        if (candidates == null) {
            return;
        }
        candidates.forEach(candidate -> addProfile(merged, candidate));
    }

    private void addProfile(Map<Long, StudentProfile> merged, StudentProfile profile) {
        if (profile != null && profile.getId() != null) {
            merged.putIfAbsent(profile.getId(), profile);
        }
    }

    private List<StudentAbilityProfileResponse.ProfileStat> summarizeProblemTags(
            List<Submission> submissions,
            Map<Long, Problem> problems,
            Function<Problem, List<String>> extractor) {
        if (submissions == null || submissions.isEmpty() || problems == null || problems.isEmpty()) {
            return List.of();
        }
        boolean hasFailed = submissions.stream().anyMatch(submission -> submission.getVerdict() != Submission.Verdict.ACCEPTED);
        Map<String, TagAccumulator> accumulators = new LinkedHashMap<>();
        submissions.stream()
                .filter(submission -> !hasFailed || submission.getVerdict() != Submission.Verdict.ACCEPTED)
                .limit(RECENT_WINDOW)
                .forEach(submission -> {
                    Problem problem = problems.get(submission.getProblemId());
                    if (problem == null) {
                        return;
                    }
                    List<String> tags = extractor.apply(problem);
                    if (tags == null) {
                        return;
                    }
                    tags.stream()
                            .map(this::normalize)
                            .filter(value -> !value.isBlank())
                            .distinct()
                            .forEach(value -> {
                                TagAccumulator accumulator = accumulators.computeIfAbsent(value, TagAccumulator::new);
                                accumulator.count++;
                                if (submission.getProblemId() != null) {
                                    accumulator.problemIds.add(submission.getProblemId());
                                }
                            });
                });
        return accumulators.values()
                .stream()
                .sorted(Comparator.comparing(TagAccumulator::getCount).reversed()
                        .thenComparing(TagAccumulator::getLabel))
                .limit(5)
                .map(TagAccumulator::toStat)
                .toList();
    }

    private String buildSummary(List<Submission> submissions, List<AbilitySignalAnalyzer.AbilitySignal> abilitySignals) {
        if (submissions == null || submissions.isEmpty()) {
            return "还没有足够的提交证据，完成一次提交后会开始形成长期能力画像。";
        }
        if (abilitySignals != null && !abilitySignals.isEmpty()) {
            AbilitySignalAnalyzer.AbilitySignal top = abilitySignals.get(0);
            if (top.getTaskCount() >= 2) {
                return "跨作业主要集中在：" + top.getAbilityPoint() + "。建议先对比这些题的共同判断步骤。";
            }
            return "近期主要关注：" + top.getAbilityPoint() + "。继续观察下一次提交是否改变这个信号。";
        }
        long accepted = submissions.stream().filter(submission -> submission.getVerdict() == Submission.Verdict.ACCEPTED).count();
        if (accepted > 0) {
            return "近期已有通过记录，可以进入复盘：讲清边界、复杂度和可迁移思路。";
        }
        return "提交证据还比较分散，建议先围绕最近一次失败样例做最小化验证。";
    }

    private String buildTrendSignal(List<Submission> submissions, List<AbilitySignalAnalyzer.AbilitySignal> abilitySignals) {
        if (submissions == null || submissions.isEmpty()) {
            return "暂无趋势";
        }
        Submission latest = submissions.get(0);
        if (latest.getVerdict() == Submission.Verdict.ACCEPTED) {
            return "最近一次提交已通过，可以观察是否能解释边界与复杂度。";
        }
        if (submissions.size() >= 3 && submissions.stream().limit(3).noneMatch(submission -> submission.getVerdict() == Submission.Verdict.ACCEPTED)) {
            return "最近 3 次仍未通过，建议教师或 AI 教练先收窄到一个最小失败样例。";
        }
        if (abilitySignals != null && !abilitySignals.isEmpty() && abilitySignals.get(0).getSubmissionCount() >= 2) {
            return "同一能力点多次出现，下一轮应验证是否真正理解，而不是只改代码。";
        }
        return "趋势还在形成中，继续用下一次提交补充证据。";
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static class TagAccumulator {
        private final String label;
        private long count;
        private final Set<Long> problemIds = new LinkedHashSet<>();

        private TagAccumulator(String label) {
            this.label = label;
        }

        private long getCount() {
            return count;
        }

        private String getLabel() {
            return label;
        }

        private StudentAbilityProfileResponse.ProfileStat toStat() {
            return StudentAbilityProfileResponse.ProfileStat.builder()
                    .label(label)
                    .count(count)
                    .evidenceProblemIds(problemIds.stream().limit(5).toList())
                    .build();
        }
    }
}
