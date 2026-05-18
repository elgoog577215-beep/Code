package com.onlinejudge.classroom.application;

import com.onlinejudge.classroom.dto.StudentAbilityProfileResponse;
import com.onlinejudge.classroom.dto.StudentRecommendationResponse;
import com.onlinejudge.problem.domain.Problem;
import com.onlinejudge.problem.persistence.ProblemRepository;
import com.onlinejudge.submission.domain.Submission;
import com.onlinejudge.submission.persistence.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class StudentRecommendationService {

    private final StudentAbilityProfileService abilityProfileService;
    private final ProblemRepository problemRepository;
    private final SubmissionRepository submissionRepository;

    public StudentRecommendationResponse recommend(Long studentProfileId) {
        StudentAbilityProfileResponse profile = abilityProfileService.buildProfile(studentProfileId);
        List<Submission> submissions = profile.getMergedStudentProfileIds() == null || profile.getMergedStudentProfileIds().isEmpty()
                ? List.of()
                : submissionRepository.findByStudentProfileIdInOrderBySubmittedAtDesc(profile.getMergedStudentProfileIds());
        Set<Long> attemptedProblemIds = new LinkedHashSet<>();
        Set<Long> failedProblemIds = new LinkedHashSet<>();
        submissions.stream()
                .filter(Objects::nonNull)
                .forEach(submission -> {
                    if (submission.getProblemId() != null) {
                        attemptedProblemIds.add(submission.getProblemId());
                        if (submission.getVerdict() != Submission.Verdict.ACCEPTED) {
                            failedProblemIds.add(submission.getProblemId());
                        }
                    }
                });
        List<Problem> problems = problemRepository.findAllByOrderByIdAsc();
        List<StudentRecommendationResponse.RecommendationItem> items = new ArrayList<>();

        addRedoRecommendation(profile, problems, failedProblemIds, items);
        addNewPracticeRecommendation(profile, problems, attemptedProblemIds, items);
        addReviewRecommendation(profile, items);

        return StudentRecommendationResponse.builder()
                .student(profile.getStudent())
                .summary(buildSummary(profile, items))
                .recommendations(items.stream()
                        .sorted(Comparator.comparing(StudentRecommendationResponse.RecommendationItem::getPriority))
                        .limit(3)
                        .toList())
                .build();
    }

    private void addRedoRecommendation(StudentAbilityProfileResponse profile,
                                       List<Problem> problems,
                                       Set<Long> failedProblemIds,
                                       List<StudentRecommendationResponse.RecommendationItem> items) {
        if (failedProblemIds.isEmpty()) {
            return;
        }
        Problem problem = problems.stream()
                .filter(item -> failedProblemIds.contains(item.getId()))
                .filter(item -> matchesProfileFocus(item, profile))
                .findFirst()
                .orElseGet(() -> problems.stream()
                        .filter(item -> failedProblemIds.contains(item.getId()))
                        .findFirst()
                        .orElse(null));
        if (problem == null) {
            return;
        }
        items.add(StudentRecommendationResponse.RecommendationItem.builder()
                .type("REDO")
                .title("先重做最近卡住的题")
                .reason("这道题和当前能力画像最接近，适合用来验证错因是否真正被解决。")
                .actionLabel("去重做")
                .problemId(problem.getId())
                .problemTitle(problem.getTitle())
                .focusAbility(profile.getPrimaryAbilityFocus())
                .focusTags(focusTags(problem, profile))
                .evidenceProblemIds(List.of(problem.getId()))
                .priority(10)
                .build());
    }

    private void addNewPracticeRecommendation(StudentAbilityProfileResponse profile,
                                              List<Problem> problems,
                                              Set<Long> attemptedProblemIds,
                                              List<StudentRecommendationResponse.RecommendationItem> items) {
        Problem problem = problems.stream()
                .filter(item -> !attemptedProblemIds.contains(item.getId()))
                .filter(item -> matchesProfileFocus(item, profile))
                .findFirst()
                .orElseGet(() -> problems.stream()
                        .filter(item -> !attemptedProblemIds.contains(item.getId()))
                        .findFirst()
                        .orElse(null));
        if (problem == null) {
            return;
        }
        items.add(StudentRecommendationResponse.RecommendationItem.builder()
                .type("NEXT_PROBLEM")
                .title("做一题同类新题")
                .reason("换一道题可以检查你是不是只修好了原题，而不是掌握了这类能力点。")
                .actionLabel("去练习")
                .problemId(problem.getId())
                .problemTitle(problem.getTitle())
                .focusAbility(profile.getPrimaryAbilityFocus())
                .focusTags(focusTags(problem, profile))
                .evidenceProblemIds(collectEvidenceProblemIds(profile))
                .priority(20)
                .build());
    }

    private void addReviewRecommendation(StudentAbilityProfileResponse profile,
                                         List<StudentRecommendationResponse.RecommendationItem> items) {
        List<String> tags = new ArrayList<>();
        if (profile.getPrimaryAbilityFocus() != null && !profile.getPrimaryAbilityFocus().isBlank()) {
            tags.add(profile.getPrimaryAbilityFocus());
        }
        safeStats(profile.getKnowledgeFocus()).stream()
                .limit(2)
                .map(StudentAbilityProfileResponse.ProfileStat::getLabel)
                .forEach(tags::add);
        if (tags.isEmpty()) {
            tags.add("最小样例");
            tags.add("边界与复杂度");
        }
        items.add(StudentRecommendationResponse.RecommendationItem.builder()
                .type("REVIEW")
                .title("做一次短复盘")
                .reason(buildReviewReason(profile))
                .actionLabel("按提示复盘")
                .focusAbility(profile.getPrimaryAbilityFocus())
                .focusTags(tags.stream().distinct().toList())
                .evidenceProblemIds(collectEvidenceProblemIds(profile))
                .priority(30)
                .build());
    }

    private boolean matchesProfileFocus(Problem problem, StudentAbilityProfileResponse profile) {
        if (problem == null || profile == null) {
            return false;
        }
        List<String> problemTags = problemTags(problem);
        if (problemTags.isEmpty()) {
            return false;
        }
        if (containsAny(problemTags, profile.getKnowledgeFocus())) {
            return true;
        }
        if (containsAny(problemTags, profile.getCommonMistakeFocus())) {
            return true;
        }
        if (containsAny(problemTags, profile.getBoundaryFocus())) {
            return true;
        }
        String ability = profile.getPrimaryAbilityFocus();
        return ability != null && problemTags.stream().anyMatch(tag -> ability.contains(tag) || tag.contains(ability));
    }

    private boolean containsAny(List<String> problemTags, List<StudentAbilityProfileResponse.ProfileStat> profileStats) {
        if (profileStats == null || profileStats.isEmpty()) {
            return false;
        }
        return profileStats.stream()
                .map(StudentAbilityProfileResponse.ProfileStat::getLabel)
                .filter(Objects::nonNull)
                .anyMatch(label -> problemTags.stream().anyMatch(tag -> tag.equalsIgnoreCase(label)));
    }

    private List<String> focusTags(Problem problem, StudentAbilityProfileResponse profile) {
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        if (profile.getPrimaryAbilityFocus() != null && !profile.getPrimaryAbilityFocus().isBlank()) {
            tags.add(profile.getPrimaryAbilityFocus());
        }
        problemTags(problem).stream().limit(3).forEach(tags::add);
        return List.copyOf(tags);
    }

    private List<String> problemTags(Problem problem) {
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        addAll(tags, problem.getKnowledgePoints());
        addAll(tags, problem.getCommonMistakes());
        addAll(tags, problem.getBoundaryTypes());
        return List.copyOf(tags);
    }

    private void addAll(Set<String> target, List<String> values) {
        if (values == null) {
            return;
        }
        values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .forEach(target::add);
    }

    private List<Long> collectEvidenceProblemIds(StudentAbilityProfileResponse profile) {
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        collectEvidenceIds(ids, profile.getKnowledgeFocus());
        collectEvidenceIds(ids, profile.getCommonMistakeFocus());
        collectEvidenceIds(ids, profile.getBoundaryFocus());
        return ids.stream().limit(5).toList();
    }

    private void collectEvidenceIds(Set<Long> ids, List<StudentAbilityProfileResponse.ProfileStat> stats) {
        safeStats(stats).stream()
                .flatMap(stat -> stat.getEvidenceProblemIds() == null ? List.<Long>of().stream() : stat.getEvidenceProblemIds().stream())
                .filter(Objects::nonNull)
                .forEach(ids::add);
    }

    private List<StudentAbilityProfileResponse.ProfileStat> safeStats(List<StudentAbilityProfileResponse.ProfileStat> stats) {
        return stats == null ? List.of() : stats;
    }

    private String buildReviewReason(StudentAbilityProfileResponse profile) {
        if (profile.getLatestCoachInteraction() != null && profile.getLatestCoachInteraction().isPrompted()
                && !profile.getLatestCoachInteraction().isAnswered()) {
            return "你还有 AI 教练追问没有回答，先把自己的样例或判断依据补完整。";
        }
        if (profile.getTrendSignal() != null && !profile.getTrendSignal().isBlank()) {
            return profile.getTrendSignal();
        }
        return "把最近一次错误写成一个最小样例，再说明输入、预期输出和当前代码为什么不一致。";
    }

    private String buildSummary(StudentAbilityProfileResponse profile,
                                List<StudentRecommendationResponse.RecommendationItem> items) {
        if (items == null || items.isEmpty()) {
            return "暂时没有足够证据生成推荐，先完成一次提交。";
        }
        if (profile.getPrimaryAbilityFocus() != null && !profile.getPrimaryAbilityFocus().isBlank()) {
            return "推荐围绕“" + profile.getPrimaryAbilityFocus() + "”安排，先验证旧问题，再迁移到新题。";
        }
        return "推荐先用复盘任务补齐证据，再进入下一题。";
    }
}
