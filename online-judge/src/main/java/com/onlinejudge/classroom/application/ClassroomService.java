package com.onlinejudge.classroom.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlinejudge.classroom.domain.*;
import com.onlinejudge.classroom.dto.*;
import com.onlinejudge.classroom.persistence.*;
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
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClassroomService {

    private static final String INVITE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ClassGroupRepository classGroupRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final AssignmentRepository assignmentRepository;
    private final AssignmentInviteRepository assignmentInviteRepository;
    private final AssignmentTaskRepository assignmentTaskRepository;
    private final ProblemRepository problemRepository;
    private final SubmissionRepository submissionRepository;
    private final SubmissionAnalysisRepository submissionAnalysisRepository;
    private final TeacherDiagnosisCorrectionRepository teacherDiagnosisCorrectionRepository;
    private final ObjectMapper objectMapper;
    private final DiagnosisTaxonomy diagnosisTaxonomy;
    private final DiagnosisReportReader diagnosisReportReader;
    private final AbilitySignalAnalyzer abilitySignalAnalyzer;
    private final CoachInteractionAnalyzer coachInteractionAnalyzer;
    private final StudentIdentityService studentIdentityService;
    private final ClassReviewFeedbackService classReviewFeedbackService;
    private final CoachImpactAnalyzer coachImpactAnalyzer;
    private final LearningInterventionImpactAnalyzer learningInterventionImpactAnalyzer;
    private final LearningActionEvidenceAnalyzer learningActionEvidenceAnalyzer;
    private final TeacherActionPriorityAnalyzer teacherActionPriorityAnalyzer;

    public List<ClassGroupResponse> getClassGroups() {
        return classGroupRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(ClassGroupResponse::from)
                .toList();
    }

    @Transactional
    public ClassGroupResponse createClassGroup(CreateClassGroupRequest request) {
        ClassGroup classGroup = classGroupRepository.save(ClassGroup.builder()
                .name(normalizeRequired(request.getName(), "班级名称不能为空"))
                .grade(normalizeNullable(request.getGrade()))
                .teacherName(normalizeNullable(request.getTeacherName()))
                .build());
        return ClassGroupResponse.from(classGroup);
    }

    public List<AssignmentResponse> getAssignments() {
        List<Assignment> assignments = assignmentRepository.findAllByOrderByCreatedAtDesc();
        return assignments.stream()
                .map(this::toAssignmentResponse)
                .toList();
    }

    public AssignmentResponse getAssignment(Long assignmentId) {
        Assignment assignment = findAssignment(assignmentId);
        return toAssignmentResponse(assignment);
    }

    @Transactional
    public AssignmentResponse createAssignment(CreateAssignmentRequest request) {
        if (request.getProblemIds() == null || request.getProblemIds().isEmpty()) {
            throw new IllegalArgumentException("作业至少需要绑定一个学习任务");
        }

        validateProblemsExist(request.getProblemIds());
        if (request.getClassGroupId() != null && !classGroupRepository.existsById(request.getClassGroupId())) {
            throw new IllegalArgumentException("班级不存在: " + request.getClassGroupId());
        }

        Assignment assignment = assignmentRepository.save(Assignment.builder()
                .title(normalizeRequired(request.getTitle(), "作业标题不能为空"))
                .description(normalizeNullable(request.getDescription()))
                .classGroupId(request.getClassGroupId())
                .hintPolicy(request.getHintPolicy() == null ? Assignment.HintPolicy.L2 : request.getHintPolicy())
                .status(request.getStatus() == null ? Assignment.AssignmentStatus.ACTIVE : request.getStatus())
                .startsAt(request.getStartsAt())
                .endsAt(request.getEndsAt())
                .build());

        saveAssignmentTasks(assignment.getId(), request.getProblemIds());
        ensureInviteForAssignment(assignment);
        return toAssignmentResponse(assignment);
    }

    @Transactional
    public AssignmentResponse updateAssignment(Long assignmentId, CreateAssignmentRequest request) {
        Assignment assignment = findAssignment(assignmentId);
        validateProblemsExist(request.getProblemIds());

        assignment.setTitle(normalizeRequired(request.getTitle(), "作业标题不能为空"));
        assignment.setDescription(normalizeNullable(request.getDescription()));
        assignment.setClassGroupId(request.getClassGroupId());
        assignment.setHintPolicy(request.getHintPolicy() == null ? Assignment.HintPolicy.L2 : request.getHintPolicy());
        assignment.setStatus(request.getStatus() == null ? Assignment.AssignmentStatus.ACTIVE : request.getStatus());
        assignment.setStartsAt(request.getStartsAt());
        assignment.setEndsAt(request.getEndsAt());
        Assignment saved = assignmentRepository.save(assignment);

        assignmentTaskRepository.deleteByAssignmentId(saved.getId());
        saveAssignmentTasks(saved.getId(), request.getProblemIds());
        ensureInviteForAssignment(saved);
        return toAssignmentResponse(saved);
    }

    @Transactional
    public AssignmentResponse rotateInvite(Long assignmentId) {
        Assignment assignment = findAssignment(assignmentId);
        AssignmentInvite invite = assignmentInviteRepository.findAll()
                .stream()
                .filter(item -> Objects.equals(item.getAssignmentId(), assignmentId))
                .findFirst()
                .orElse(null);
        if (invite != null) {
            invite.setEnabled(false);
            assignmentInviteRepository.save(invite);
        }
        assignmentInviteRepository.save(AssignmentInvite.builder()
                .assignmentId(assignment.getId())
                .code(generateInviteCode())
                .enabled(true)
                .build());
        return toAssignmentResponse(assignment);
    }

    public AssignmentResponse resolveInvite(String code) {
        AssignmentInvite invite = findEnabledInvite(code);
        Assignment assignment = findAssignment(invite.getAssignmentId());
        ensureAssignmentOpen(assignment, invite);
        return toAssignmentResponse(assignment);
    }

    @Transactional
    public StudentProfileResponse bindStudentIdentity(StudentIdentityRequest request) {
        Assignment assignment = findAssignment(request.getAssignmentId());
        Long classGroupId = request.getClassGroupId() != null ? request.getClassGroupId() : assignment.getClassGroupId();
        String identityKey = studentIdentityService.buildPreferredIdentityKey(
                assignment.getId(),
                classGroupId,
                request.getClassName(),
                request.getDisplayName(),
                request.getStudentNo()
        );
        String legacyIdentityKey = studentIdentityService.buildLegacyAssignmentIdentityKey(
                assignment.getId(),
                classGroupId,
                request.getClassName(),
                request.getDisplayName(),
                request.getStudentNo()
        );

        StudentProfile student = studentProfileRepository.findByIdentityKeyOrderByCreatedAtDesc(identityKey)
                .stream()
                .findFirst()
                .or(() -> identityKey.equals(legacyIdentityKey)
                        ? Optional.empty()
                        : studentProfileRepository.findByIdentityKeyOrderByCreatedAtDesc(legacyIdentityKey).stream().findFirst())
                .orElse(StudentProfile.builder()
                        .identityKey(identityKey)
                        .build());
        student.setIdentityKey(identityKey);
        student.setClassGroupId(classGroupId);
        student.setDisplayName(normalizeRequired(request.getDisplayName(), "姓名不能为空"));
        student.setStudentNo(normalizeNullable(request.getStudentNo()));
        student.setNote(normalizeNullable(request.getNote()));

        return StudentProfileResponse.from(studentProfileRepository.save(student));
    }

    public AssignmentOverviewResponse getAssignmentOverview(Long assignmentId) {
        Assignment assignment = findAssignment(assignmentId);
        AssignmentResponse assignmentResponse = toAssignmentResponse(assignment);
        List<Submission> submissions = submissionRepository.findByAssignmentIdOrderBySubmittedAtDesc(assignmentId);
        List<Long> submissionIds = submissions.stream().map(Submission::getId).toList();
        Map<Long, SubmissionAnalysis> analyses = submissionIds.isEmpty()
                ? Map.of()
                : submissionAnalysisRepository.findBySubmissionIdIn(submissionIds)
                .stream()
                .collect(Collectors.toMap(SubmissionAnalysis::getSubmissionId, Function.identity()));
        Map<Long, TeacherDiagnosisCorrection> corrections = submissionIds.isEmpty()
                ? Map.of()
                : teacherDiagnosisCorrectionRepository.findBySubmissionIdIn(submissionIds)
                .stream()
                .collect(Collectors.toMap(
                        TeacherDiagnosisCorrection::getSubmissionId,
                        Function.identity(),
                        (left, right) -> {
                            if (left.getCorrectedAt() == null) {
                                return right;
                            }
                            if (right.getCorrectedAt() == null) {
                                return left;
                            }
                            return right.getCorrectedAt().isAfter(left.getCorrectedAt()) ? right : left;
                        }
                ));
        Map<Long, CoachInteractionSummaryResponse> coachInteractions = coachInteractionAnalyzer.summarize(submissionIds);
        Map<Long, CoachImpactResponse> coachImpacts = coachImpactAnalyzer.summarizeByCoachedSubmission(
                submissions,
                analyses,
                coachInteractionAnalyzer.findPrompts(submissionIds)
        );
        Map<Long, StudentTrajectoryResponse.LearningInterventionImpact> interventionImpacts =
                learningInterventionImpactAnalyzer.summarizeByInterventionSubmission(submissions, analyses);
        Map<Long, StudentTrajectoryResponse.LearningActionEvidence> actionEvidence =
                learningActionEvidenceAnalyzer.summarizeByInterventionSubmission(submissions, analyses, interventionImpacts);
        Map<String, TeacherActionPriorityAnalyzer.PrioritySignal> actionPrioritySignals =
                teacherActionPriorityAnalyzer.summarize(submissions, analyses, interventionImpacts, actionEvidence);
        Map<Long, Problem> submittedProblems = problemRepository.findAllById(submissions.stream()
                        .map(Submission::getProblemId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList())
                .stream()
                .collect(Collectors.toMap(Problem::getId, Function.identity()));

        Map<Long, List<Submission>> byStudent = submissions.stream()
                .filter(submission -> submission.getStudentProfileId() != null)
                .collect(Collectors.groupingBy(Submission::getStudentProfileId));
        Map<Long, StudentProfile> students = studentProfileRepository.findAllById(byStudent.keySet())
                .stream()
                .collect(Collectors.toMap(StudentProfile::getId, Function.identity()));

        List<AssignmentOverviewResponse.StudentProgressSummary> studentSummaries = byStudent.entrySet()
                .stream()
                .map(entry -> toStudentSummary(
                        entry.getKey(),
                        students.get(entry.getKey()),
                        entry.getValue(),
                        analyses,
                        corrections,
                        coachInteractions,
                        coachImpacts,
                        actionEvidence
                ))
                .sorted(Comparator.comparing(AssignmentOverviewResponse.StudentProgressSummary::isNeedsAttention).reversed()
                        .thenComparing(AssignmentOverviewResponse.StudentProgressSummary::getDisplayName, Comparator.nullsLast(String::compareTo)))
                .toList();

        Map<String, Long> issueCounts = new LinkedHashMap<>();
        analyses.values().forEach(analysis -> diagnosisReportReader.issueTags(analysis).forEach(tag -> {
            issueCounts.put(tag, issueCounts.getOrDefault(tag, 0L) + 1L);
        }));
        Map<String, Long> fineIssueCounts = new LinkedHashMap<>();
        analyses.values().forEach(analysis -> diagnosisReportReader.fineGrainedTags(analysis).forEach(tag -> {
            fineIssueCounts.put(tag, fineIssueCounts.getOrDefault(tag, 0L) + 1L);
        }));

        List<AssignmentOverviewResponse.IssueStat> topIssues = mergeIssueCounts(fineIssueCounts, issueCounts).entrySet()
                .stream()
                .map(entry -> AssignmentOverviewResponse.IssueStat.builder()
                        .label(entry.getKey())
                        .count(entry.getValue())
                        .explanation(resolveTeacherExplanation(entry.getKey()))
                        .abilityPoint(resolveAbilityPoint(entry.getKey()))
                        .recommendedHintPolicy(resolveRecommendedHintPolicy(entry.getKey()))
                        .interventionSuggestion(resolveInterventionSuggestion(entry.getKey()))
                        .build())
                .map(issue -> teacherActionPriorityAnalyzer.enrich(issue, actionPrioritySignals.get(issue.getLabel())))
                .sorted(Comparator
                        .comparing((AssignmentOverviewResponse.IssueStat issue) -> issue.getActionPriorityScore() == null
                                ? 0.0
                        : issue.getActionPriorityScore())
                        .reversed()
                        .thenComparing(AssignmentOverviewResponse.IssueStat::getCount, Comparator.reverseOrder()))
                .limit(5)
                .toList();
        List<AssignmentOverviewResponse.AbilityStat> classAbilityWeaknesses = abilitySignalAnalyzer.summarize(submissions, analyses)
                .stream()
                .map(signal -> AssignmentOverviewResponse.AbilityStat.builder()
                        .abilityPoint(signal.getAbilityPoint())
                        .taskCount(signal.getTaskCount())
                        .submissionCount(signal.getSubmissionCount())
                        .evidenceTags(signal.getEvidenceTags())
                        .build())
                .toList();
        List<AssignmentOverviewResponse.ClassReviewSuggestion> classReviewSuggestions = buildClassReviewSuggestions(
                assignmentId,
                submissions,
                analyses,
                submittedProblems,
                topIssues,
                classAbilityWeaknesses,
                latestClassReviewFeedbackByKey(assignmentId)
        );

        return AssignmentOverviewResponse.builder()
                .assignment(assignmentResponse)
                .participantCount(byStudent.keySet().size())
                .attemptCount(submissions.size())
                .passedAttemptCount(submissions.stream().filter(submission -> submission.getVerdict() == Submission.Verdict.ACCEPTED).count())
                .strugglingStudentCount(studentSummaries.stream().filter(AssignmentOverviewResponse.StudentProgressSummary::isNeedsAttention).count())
                .topIssues(topIssues)
                .classAbilityWeaknesses(classAbilityWeaknesses)
                .classReviewSuggestions(classReviewSuggestions)
                .students(studentSummaries)
                .build();
    }

    private String resolveTeacherExplanation(String tagId) {
        DiagnosisTaxonomy.DiagnosisTag tag = diagnosisTaxonomy.get(tagId);
        if (tag == null || normalizeNullable(tag.getTeacherExplanation()).isBlank()) {
            return diagnosisTaxonomy.label(tagId);
        }
        return tag.getTeacherExplanation();
    }

    private String resolveAbilityPoint(String tagId) {
        DiagnosisTaxonomy.DiagnosisTag tag = diagnosisTaxonomy.get(tagId);
        return tag == null ? null : tag.getAbilityPoint();
    }

    private String resolveRecommendedHintPolicy(String tagId) {
        DiagnosisTaxonomy.DiagnosisTag tag = diagnosisTaxonomy.get(tagId);
        return tag == null || tag.getRecommendedHintPolicy() == null ? null : tag.getRecommendedHintPolicy().name();
    }

    private String resolveInterventionSuggestion(String tagId) {
        DiagnosisTaxonomy.DiagnosisTag tag = diagnosisTaxonomy.get(tagId);
        if (tag == null) {
            return "先补充一次提交证据，再判断是否需要教师介入。";
        }
        return switch (tag.getId()) {
            case "OFF_BY_ONE", "LOOP_BOUNDARY" -> "让学生列出循环变量表，手推 1 个和 2 个元素的最小样例。";
            case "EMPTY_INPUT", "BOUNDARY_CONDITION" -> "要求学生先写出极小、极大、特殊输入三类样例，再解释每类预期输出。";
            case "OUTPUT_FORMAT_DETAIL", "IO_FORMAT" -> "让学生逐字符对比题面输出格式和实际输出，先修正换行、空格、大小写。";
            case "INPUT_PARSING" -> "请学生圈出题面输入结构，说明每一行读入的变量含义。";
            case "BRUTE_FORCE_LIMIT", "TIME_COMPLEXITY", "MAX_BOUNDARY" -> "先估算循环次数，再比较最大规模下当前算法是否可运行。";
            case "OVER_SIMULATION", "ALGORITHM_STRATEGY" -> "引导学生说明当前策略为什么成立，并尝试找一个能推翻策略的小反例。";
            case "GREEDY_ASSUMPTION" -> "要求学生写出贪心选择依据，再用反例检验这个依据是否总成立。";
            case "DP_STATE_DESIGN", "STATE_TRANSITION" -> "先用自然语言定义状态含义，再检查转移是否覆盖题目全部信息。";
            case "INITIAL_STATE", "STATE_RESET", "VARIABLE_INITIALIZATION" -> "让学生标出每个变量第一次赋值和每轮重置的位置。";
            case "SAMPLE_OVERFIT", "SAMPLE_ONLY" -> "请学生构造一个不同于样例的最小反例，验证代码是否只记住样例路径。";
            case "PARTIAL_FIX_REGRESSION" -> "对比前后两次提交，只保留一个最小修改点重新验证。";
            case "RUNTIME_STABILITY" -> "从异常位置回看数组越界、空值、除零和未初始化访问。";
            case "SYNTAX_ERROR" -> "先让学生读编译信息，定位第一处语法错误，不要同时改多处。";
            case "NEEDS_MORE_EVIDENCE" -> "先补充可见样例或下一次提交，不急着给出确定错因。";
            case "CODE_QUALITY", "CODE_READABILITY", "GENERALIZATION_CHECK" -> "让学生用自己的话复述思路、复杂度和一个边界样例。";
            default -> "让学生先用一个最小样例说明当前思路，再决定是否改代码。";
        };
    }

    private List<AssignmentOverviewResponse.ClassReviewSuggestion> buildClassReviewSuggestions(
            Long assignmentId,
            List<Submission> submissions,
            Map<Long, SubmissionAnalysis> analyses,
            Map<Long, Problem> problems,
            List<AssignmentOverviewResponse.IssueStat> topIssues,
            List<AssignmentOverviewResponse.AbilityStat> classAbilityWeaknesses,
            Map<String, ClassReviewFeedback> feedbackByKey) {
        if (submissions == null || submissions.isEmpty()) {
            return List.of();
        }
        LinkedHashMap<String, AssignmentOverviewResponse.ClassReviewSuggestion> suggestions = new LinkedHashMap<>();
        for (AssignmentOverviewResponse.AbilityStat ability : safeList(classAbilityWeaknesses)) {
            if (ability.getAbilityPoint() == null || ability.getAbilityPoint().isBlank()) {
                continue;
            }
            ClassReviewEvidence evidence = findClassReviewEvidence(submissions, analyses, problems, ability.getAbilityPoint(), ability.getEvidenceTags());
            if (evidence == null) {
                continue;
            }
            String suggestionKey = classReviewSuggestionKey(assignmentId, "ability", ability.getAbilityPoint(), evidence.problemId(), evidence.primaryTag());
            suggestions.putIfAbsent(suggestionKey, AssignmentOverviewResponse.ClassReviewSuggestion.builder()
                    .suggestionKey(suggestionKey)
                    .title("复盘：" + ability.getAbilityPoint())
                    .targetAbility(ability.getAbilityPoint())
                    .exampleProblemId(evidence.problemId())
                    .exampleProblemTitle(evidence.problemTitle())
                    .evidenceTags(evidence.evidenceTags())
                    .evidenceSubmissionIds(evidence.submissionIds())
                    .guidingQuestion(buildGuidingQuestion(evidence.primaryTag(), ability.getAbilityPoint()))
                    .action(resolveInterventionSuggestion(evidence.primaryTag()))
                    .evidenceSummary("涉及 " + ability.getTaskCount() + " 题、" + ability.getSubmissionCount() + " 次提交。")
                    .latestFeedback(toClassReviewFeedbackSummary(feedbackByKey.get(suggestionKey)))
                    .build());
            if (suggestions.size() >= 3) {
                return List.copyOf(suggestions.values());
            }
        }
        for (AssignmentOverviewResponse.IssueStat issue : safeList(topIssues)) {
            String abilityPoint = issue.getAbilityPoint() == null ? diagnosisTaxonomy.label(issue.getLabel()) : issue.getAbilityPoint();
            ClassReviewEvidence evidence = findClassReviewEvidence(submissions, analyses, problems, abilityPoint, List.of(issue.getLabel()));
            if (evidence == null) {
                continue;
            }
            String suggestionKey = classReviewSuggestionKey(assignmentId, "issue", issue.getLabel(), evidence.problemId(), evidence.primaryTag());
            suggestions.putIfAbsent(suggestionKey, AssignmentOverviewResponse.ClassReviewSuggestion.builder()
                    .suggestionKey(suggestionKey)
                    .title("复盘：" + diagnosisTaxonomy.label(issue.getLabel()))
                    .targetAbility(abilityPoint)
                    .exampleProblemId(evidence.problemId())
                    .exampleProblemTitle(evidence.problemTitle())
                    .evidenceTags(evidence.evidenceTags())
                    .evidenceSubmissionIds(evidence.submissionIds())
                    .guidingQuestion(buildGuidingQuestion(issue.getLabel(), abilityPoint))
                    .action(resolveInterventionSuggestion(issue.getLabel()))
                    .evidenceSummary("该问题出现 " + issue.getCount() + " 次。")
                    .latestFeedback(toClassReviewFeedbackSummary(feedbackByKey.get(suggestionKey)))
                    .build());
            if (suggestions.size() >= 3) {
                break;
            }
        }
        return List.copyOf(suggestions.values());
    }

    private Map<String, ClassReviewFeedback> latestClassReviewFeedbackByKey(Long assignmentId) {
        return classReviewFeedbackService.latestByAssignment(assignmentId)
                .stream()
                .filter(feedback -> feedback.getSuggestionKey() != null && !feedback.getSuggestionKey().isBlank())
                .collect(Collectors.toMap(
                        ClassReviewFeedback::getSuggestionKey,
                        Function.identity(),
                        (first, ignored) -> first,
                        LinkedHashMap::new
                ));
    }

    private AssignmentOverviewResponse.ClassReviewFeedbackSummary toClassReviewFeedbackSummary(ClassReviewFeedback feedback) {
        if (feedback == null) {
            return null;
        }
        return AssignmentOverviewResponse.ClassReviewFeedbackSummary.builder()
                .actionType(feedback.getActionType())
                .teacherNote(feedback.getTeacherNote())
                .createdBy(feedback.getCreatedBy())
                .createdAt(feedback.getCreatedAt())
                .build();
    }

    private String classReviewSuggestionKey(Long assignmentId, String kind, String focus, Long problemId, String primaryTag) {
        return String.join(":",
                "review",
                String.valueOf(assignmentId == null ? 0 : assignmentId),
                normalizeKeyPart(kind),
                normalizeKeyPart(focus),
                String.valueOf(problemId == null ? 0 : problemId),
                normalizeKeyPart(primaryTag)
        );
    }

    private String normalizeKeyPart(String value) {
        String normalized = normalizeNullable(value).toLowerCase(Locale.ROOT);
        return normalized.replaceAll("[^a-z0-9\\u4e00-\\u9fa5_-]+", "-");
    }

    private ClassReviewEvidence findClassReviewEvidence(List<Submission> submissions,
                                                        Map<Long, SubmissionAnalysis> analyses,
                                                        Map<Long, Problem> problems,
                                                        String abilityPoint,
                                                        List<String> preferredTags) {
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        if (preferredTags != null) {
            preferredTags.stream()
                    .filter(tag -> tag != null && !tag.isBlank())
                    .forEach(tags::add);
        }
        List<String> normalizedPreferredTags = List.copyOf(tags);
        return submissions.stream()
                .filter(Objects::nonNull)
                .filter(submission -> submission.getVerdict() != Submission.Verdict.ACCEPTED)
                .filter(submission -> {
                    SubmissionAnalysis analysis = analyses.get(submission.getId());
                    if (analysis == null) {
                        return false;
                    }
                    List<String> submissionTags = mergedDiagnosisTags(analysis);
                    if (!normalizedPreferredTags.isEmpty() && submissionTags.stream().noneMatch(normalizedPreferredTags::contains)) {
                        return false;
                    }
                    return abilityPoint == null || abilityPoint.isBlank()
                            || submissionTags.stream().map(this::resolveAbilityPoint).anyMatch(abilityPoint::equals);
                })
                .findFirst()
                .map(submission -> toClassReviewEvidence(submission, analyses.get(submission.getId()), problems.get(submission.getProblemId()), normalizedPreferredTags))
                .orElse(null);
    }

    private ClassReviewEvidence toClassReviewEvidence(Submission submission,
                                                      SubmissionAnalysis analysis,
                                                      Problem problem,
                                                      List<String> preferredTags) {
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        mergedDiagnosisTags(analysis).stream()
                .filter(tag -> preferredTags == null || preferredTags.isEmpty() || preferredTags.contains(tag))
                .forEach(tags::add);
        if (tags.isEmpty()) {
            tags.addAll(mergedDiagnosisTags(analysis));
        }
        String primaryTag = tags.stream().findFirst().orElse("NEEDS_MORE_EVIDENCE");
        return new ClassReviewEvidence(
                submission.getProblemId(),
                problem == null ? "题目 #" + submission.getProblemId() : problem.getTitle(),
                tags.stream().limit(4).toList(),
                List.of(submission.getId()),
                primaryTag
        );
    }

    private List<String> mergedDiagnosisTags(SubmissionAnalysis analysis) {
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        diagnosisReportReader.fineGrainedTags(analysis).forEach(tags::add);
        diagnosisReportReader.issueTags(analysis).forEach(tags::add);
        return List.copyOf(tags);
    }

    private String buildGuidingQuestion(String tagId, String abilityPoint) {
        return switch (tagId == null ? "" : tagId) {
            case "OFF_BY_ONE", "LOOP_BOUNDARY" -> "这道题里循环第一次、最后一次分别处理了哪个位置？少算或多算一个位置会怎样？";
            case "INPUT_PARSING", "IO_FORMAT", "OUTPUT_FORMAT_DETAIL" -> "题面每一行输入/输出分别对应代码里的哪个变量或打印位置？";
            case "BRUTE_FORCE_LIMIT", "TIME_COMPLEXITY", "MAX_BOUNDARY" -> "如果输入达到最大规模，这段循环大约会执行多少次？";
            case "INITIAL_STATE", "STATE_RESET", "VARIABLE_INITIALIZATION" -> "每个变量第一次赋值在哪里？多组数据或多轮循环前有没有被重置？";
            case "SAMPLE_OVERFIT", "SAMPLE_ONLY" -> "能不能构造一个不同于样例的最小反例，让当前代码暴露问题？";
            default -> "这道题最小失败样例是什么？它暴露的关键判断步骤和 " + (abilityPoint == null ? "当前能力点" : abilityPoint) + " 有什么关系？";
        };
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private record ClassReviewEvidence(
            Long problemId,
            String problemTitle,
            List<String> evidenceTags,
            List<Long> submissionIds,
            String primaryTag) {
    }

    @Transactional
    public TeacherDiagnosisCorrectionResponse correctDiagnosis(Long assignmentId, TeacherDiagnosisCorrectionRequest request) {
        findAssignment(assignmentId);
        Submission submission = submissionRepository.findById(request.getSubmissionId())
                .orElseThrow(() -> new IllegalArgumentException("提交记录不存在: " + request.getSubmissionId()));
        if (!Objects.equals(submission.getAssignmentId(), assignmentId)) {
            throw new IllegalArgumentException("提交记录不属于当前作业");
        }

        SubmissionAnalysis analysis = submissionAnalysisRepository.findBySubmissionId(submission.getId()).orElse(null);
        String originalIssueTag = diagnosisReportReader.issueTags(analysis).stream().findFirst().orElse(null);
        String originalFineGrainedTag = diagnosisReportReader.fineGrainedTags(analysis).stream().findFirst().orElse(null);
        String correctedIssueTag = normalizeDiagnosisTag(request.getCorrectedIssueTag(), false, "修正后的粗粒度错因不存在");
        String correctedFineGrainedTag = normalizeNullable(request.getCorrectedFineGrainedTag()).isBlank()
                ? null
                : normalizeDiagnosisTag(request.getCorrectedFineGrainedTag(), true, "修正后的细粒度错因不存在");

        TeacherDiagnosisCorrection correction = teacherDiagnosisCorrectionRepository.save(TeacherDiagnosisCorrection.builder()
                .assignmentId(assignmentId)
                .submissionId(submission.getId())
                .studentProfileId(submission.getStudentProfileId())
                .originalIssueTag(originalIssueTag)
                .originalFineGrainedTag(originalFineGrainedTag)
                .correctedIssueTag(correctedIssueTag)
                .correctedFineGrainedTag(correctedFineGrainedTag)
                .teacherNote(normalizeNullable(request.getTeacherNote()))
                .evalCandidate(request.getEvalCandidate() == null || request.getEvalCandidate())
                .correctedBy(normalizeNullable(request.getCorrectedBy()))
                .build());
        return TeacherDiagnosisCorrectionResponse.from(correction);
    }

    public DiagnosisEvalCandidateResponse getDiagnosisEvalCandidates(Long assignmentId) {
        findAssignment(assignmentId);
        List<TeacherDiagnosisCorrection> corrections = teacherDiagnosisCorrectionRepository
                .findByAssignmentIdAndEvalCandidateTrueOrderByCorrectedAtDesc(assignmentId);
        List<Long> submissionIds = corrections.stream()
                .map(TeacherDiagnosisCorrection::getSubmissionId)
                .filter(Objects::nonNull)
                .toList();
        Map<Long, Submission> submissions = submissionIds.isEmpty()
                ? Map.of()
                : submissionRepository.findAllById(submissionIds)
                .stream()
                .collect(Collectors.toMap(Submission::getId, Function.identity()));
        Map<Long, SubmissionAnalysis> analyses = submissionIds.isEmpty()
                ? Map.of()
                : submissionAnalysisRepository.findBySubmissionIdIn(submissionIds)
                .stream()
                .collect(Collectors.toMap(SubmissionAnalysis::getSubmissionId, Function.identity()));
        Map<Long, Problem> problems = submissions.values().stream()
                .map(Submission::getProblemId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.collectingAndThen(Collectors.toList(), ids -> ids.isEmpty()
                        ? Map.of()
                        : problemRepository.findAllById(ids)
                        .stream()
                        .collect(Collectors.toMap(Problem::getId, Function.identity()))));

        List<DiagnosisEvalCandidateResponse.Candidate> candidates = corrections.stream()
                .map(correction -> {
                    Submission submission = submissions.get(correction.getSubmissionId());
                    SubmissionAnalysis analysis = analyses.get(correction.getSubmissionId());
                    Problem problem = submission == null ? null : problems.get(submission.getProblemId());
                    return DiagnosisEvalCandidateResponse.Candidate.builder()
                            .correctionId(correction.getId())
                            .submissionId(correction.getSubmissionId())
                            .studentProfileId(correction.getStudentProfileId())
                            .problemId(submission == null ? null : submission.getProblemId())
                            .problemTitle(problem == null ? "" : problem.getTitle())
                            .problemDescription(problem == null ? "" : problem.getDescription())
                            .problemDifficulty(problem == null || problem.getDifficulty() == null ? "" : problem.getDifficulty().name())
                            .problemTimeLimit(problem == null ? null : problem.getTimeLimit())
                            .problemMemoryLimit(problem == null ? null : problem.getMemoryLimit())
                            .verdict(submission == null || submission.getVerdict() == null ? "UNKNOWN" : submission.getVerdict().name())
                            .languageName(submission == null ? "" : submission.getLanguageName())
                            .sourceCode(submission == null ? "" : trimToLength(submission.getSourceCode(), 8000))
                            .scenario(analysis == null ? "" : analysis.getScenario())
                            .originalIssueTag(correction.getOriginalIssueTag())
                            .originalFineGrainedTag(correction.getOriginalFineGrainedTag())
                            .correctedIssueTag(correction.getCorrectedIssueTag())
                            .correctedFineGrainedTag(correction.getCorrectedFineGrainedTag())
                            .teacherNote(correction.getTeacherNote())
                            .analysisHeadline(analysis == null ? "" : analysis.getHeadline())
                            .analysisSource(analysis == null ? "" : analysis.getAnalysisSource())
                            .sourceCodePreview(buildSourceCodePreview(submission))
                            .correctedAt(correction.getCorrectedAt())
                            .build();
                })
                .toList();

        return DiagnosisEvalCandidateResponse.builder()
                .assignmentId(assignmentId)
                .candidateCount(candidates.size())
                .candidates(candidates)
                .build();
    }

    @Transactional
    public AssignmentResponse ensureDemoAssignment() {
        List<Assignment> existing = assignmentRepository.findAllByOrderByCreatedAtDesc();
        if (!existing.isEmpty()) {
            return toAssignmentResponse(existing.get(0));
        }

        ClassGroup classGroup = classGroupRepository.save(ClassGroup.builder()
                .name("温中信息技术试点班")
                .grade("高一")
                .teacherName("信息技术组")
                .build());

        List<Long> problemIds = problemRepository.findAllByOrderByIdAsc()
                .stream()
                .limit(3)
                .map(Problem::getId)
                .toList();
        if (problemIds.isEmpty()) {
            return null;
        }

        Assignment assignment = assignmentRepository.save(Assignment.builder()
                .title("课堂编程作业")
                .description("学生通过邀请码进入作业，提交 Python/C++ 代码并查看提交结果。")
                .classGroupId(classGroup.getId())
                .hintPolicy(Assignment.HintPolicy.L2)
                .status(Assignment.AssignmentStatus.ACTIVE)
                .build());
        saveAssignmentTasks(assignment.getId(), problemIds);
        assignmentInviteRepository.save(AssignmentInvite.builder()
                .assignmentId(assignment.getId())
                .code("WZAI01")
                .enabled(true)
                .build());
        return toAssignmentResponse(assignment);
    }

    private AssignmentOverviewResponse.StudentProgressSummary toStudentSummary(Long studentId,
                                                                               StudentProfile student,
                                                                               List<Submission> submissions,
                                                                               Map<Long, SubmissionAnalysis> analyses,
                                                                               Map<Long, TeacherDiagnosisCorrection> corrections,
                                                                               Map<Long, CoachInteractionSummaryResponse> coachInteractions,
                                                                               Map<Long, CoachImpactResponse> coachImpacts,
                                                                               Map<Long, StudentTrajectoryResponse.LearningActionEvidence> actionEvidence) {
        List<Submission> sorted = submissions.stream()
                .sorted(Comparator.comparing(Submission::getSubmittedAt, Comparator.nullsLast(LocalDateTime::compareTo)).reversed())
                .toList();
        Submission latest = sorted.isEmpty() ? null : sorted.get(0);
        SubmissionAnalysis latestAnalysis = latest == null ? null : analyses.get(latest.getId());
        CoachInteractionSummaryResponse latestCoachInteraction = coachInteractionAnalyzer.latestForOrderedSubmissions(
                sorted.stream().map(Submission::getId).toList(),
                coachInteractions
        );
        if (latestCoachInteraction != null) {
            latestCoachInteraction.setImpact(coachImpacts.get(latestCoachInteraction.getSubmissionId()));
        }
        CoachImpactResponse latestCoachImpact = coachImpactAnalyzer.latestForOrderedSubmissions(
                sorted.stream().map(Submission::getId).toList(),
                coachImpacts
        );
        long passedCount = sorted.stream().filter(submission -> submission.getVerdict() == Submission.Verdict.ACCEPTED).count();
        Map.Entry<String, Long> repeatedIssue = resolveRepeatedIssue(sorted, analyses);
        Map.Entry<String, Long> repeatedFineIssue = resolveRepeatedFineIssue(sorted, analyses);
        boolean repeatedDifficulty = (repeatedFineIssue != null && repeatedFineIssue.getValue() >= 2)
                || (repeatedIssue != null && repeatedIssue.getValue() >= 3);
        List<AbilitySignalAnalyzer.AbilitySignal> abilitySignals = abilitySignalAnalyzer.summarize(sorted, analyses);
        boolean crossProblemGap = abilitySignalAnalyzer.hasCrossProblemGap(sorted, analyses);
        boolean needsAttention = (sorted.size() >= 3 && passedCount == 0) || repeatedDifficulty || crossProblemGap;
        String attentionReason = buildAttentionReason(sorted, passedCount, repeatedIssue, repeatedFineIssue);
        if (crossProblemGap && "当前无需重点干预。".equals(attentionReason)) {
            attentionReason = "多题集中在能力点：" + abilitySignals.get(0).getAbilityPoint() + "。";
        }

        return AssignmentOverviewResponse.StudentProgressSummary.builder()
                .studentProfileId(studentId)
                .displayName(resolveStudentDisplayName(student, studentId))
                .studentNo(student == null ? "" : student.getStudentNo())
                .attemptCount(sorted.size())
                .passedCount(passedCount)
                .latestSubmissionId(latest == null ? null : latest.getId())
                .latestVerdict(latest == null || latest.getVerdict() == null ? "暂无" : latest.getVerdict().name())
                .latestIssue(latestAnalysis == null ? "等待诊断" : latestAnalysis.getHeadline())
                .latestIssueTag(resolveLatestIssueTag(latestAnalysis))
                .latestFineGrainedIssue(resolveLatestFineGrainedIssue(latestAnalysis))
                .latestProgressSignal(resolveProgressSignal(latestAnalysis, repeatedIssue, repeatedFineIssue))
                .latestConfidence(diagnosisReportReader.confidence(latestAnalysis))
                .latestUncertainty(normalizeNullable(diagnosisReportReader.uncertainty(latestAnalysis)))
                .latestAnswerLeakRisk(diagnosisReportReader.answerLeakRisk(latestAnalysis))
                .latestCorrection(TeacherDiagnosisCorrectionResponse.from(latest == null ? null : corrections.get(latest.getId())))
                .latestCoachInteraction(latestCoachInteraction)
                .latestCoachImpact(latestCoachImpact)
                .latestLearningActionEvidence(learningActionEvidenceAnalyzer.latestForOrderedSubmissions(
                        sorted.stream().map(Submission::getId).toList(),
                        actionEvidence
                ))
                .primaryAbilityFocus(abilitySignals.stream()
                        .findFirst()
                        .map(AbilitySignalAnalyzer.AbilitySignal::getAbilityPoint)
                        .orElse(null))
                .crossProblemSummary(abilitySignalAnalyzer.buildCrossProblemSummary(sorted, analyses))
                .abilitySummary(abilitySignals.stream()
                        .map(signal -> AssignmentOverviewResponse.AbilityStat.builder()
                                .abilityPoint(signal.getAbilityPoint())
                                .taskCount(signal.getTaskCount())
                                .submissionCount(signal.getSubmissionCount())
                                .evidenceTags(signal.getEvidenceTags())
                                .build())
                        .toList())
                .repeatedIssueTag(repeatedIssue == null ? null : repeatedIssue.getKey())
                .repeatedFineGrainedTag(repeatedFineIssue == null ? null : repeatedFineIssue.getKey())
                .repeatedIssueCount(repeatedIssue == null ? 0 : repeatedIssue.getValue())
                .attentionReason(attentionReason)
                .attentionEvidence(buildAttentionEvidence(sorted, analyses, repeatedIssue, repeatedFineIssue, abilitySignals))
                .needsAttention(needsAttention)
                .build();
    }

    private Map<String, Long> mergeIssueCounts(Map<String, Long> primary, Map<String, Long> fallback) {
        Map<String, Long> merged = new LinkedHashMap<>();
        primary.forEach(merged::put);
        fallback.forEach((key, value) -> merged.putIfAbsent(key, value));
        return merged;
    }

    private List<AssignmentOverviewResponse.AttentionEvidence> buildAttentionEvidence(
            List<Submission> submissions,
            Map<Long, SubmissionAnalysis> analyses,
            Map.Entry<String, Long> repeatedIssue,
            Map.Entry<String, Long> repeatedFineIssue,
            List<AbilitySignalAnalyzer.AbilitySignal> abilitySignals) {
        String topAbility = abilitySignals == null || abilitySignals.isEmpty() ? null : abilitySignals.get(0).getAbilityPoint();
        return submissions.stream()
                .filter(Objects::nonNull)
                .filter(submission -> submission.getVerdict() != Submission.Verdict.ACCEPTED || matchesRepeatedEvidence(submission, analyses, repeatedIssue, repeatedFineIssue, topAbility))
                .limit(3)
                .map(submission -> {
                    SubmissionAnalysis analysis = analyses.get(submission.getId());
                    String fineTag = resolveLatestFineGrainedIssue(analysis);
                    String issueTag = resolveLatestIssueTag(analysis);
                    String abilityPoint = resolveAbilityPoint(fineTag != null ? fineTag : issueTag);
                    return AssignmentOverviewResponse.AttentionEvidence.builder()
                            .submissionId(submission.getId())
                            .problemId(submission.getProblemId())
                            .verdict(submission.getVerdict() == null ? "UNKNOWN" : submission.getVerdict().name())
                            .submittedAt(submission.getSubmittedAt())
                            .issueTag(issueTag)
                            .fineGrainedTag(fineTag)
                            .abilityPoint(abilityPoint)
                            .headline(analysis == null ? "" : analysis.getHeadline())
                            .reason(buildEvidenceReason(submission, fineTag, issueTag, abilityPoint, repeatedIssue, repeatedFineIssue, topAbility))
                            .build();
                })
                .toList();
    }

    private String buildSourceCodePreview(Submission submission) {
        if (submission == null || submission.getSourceCode() == null) {
            return "";
        }
        return trimToLength(submission.getSourceCode(), 800);
    }

    private String trimToLength(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String normalized = value.strip();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "\n...";
    }

    private boolean matchesRepeatedEvidence(Submission submission,
                                            Map<Long, SubmissionAnalysis> analyses,
                                            Map.Entry<String, Long> repeatedIssue,
                                            Map.Entry<String, Long> repeatedFineIssue,
                                            String topAbility) {
        SubmissionAnalysis analysis = analyses.get(submission.getId());
        String fineTag = resolveLatestFineGrainedIssue(analysis);
        String issueTag = resolveLatestIssueTag(analysis);
        String abilityPoint = resolveAbilityPoint(fineTag != null ? fineTag : issueTag);
        return (repeatedFineIssue != null && Objects.equals(repeatedFineIssue.getKey(), fineTag))
                || (repeatedIssue != null && Objects.equals(repeatedIssue.getKey(), issueTag))
                || (topAbility != null && Objects.equals(topAbility, abilityPoint));
    }

    private String buildEvidenceReason(Submission submission,
                                       String fineTag,
                                       String issueTag,
                                       String abilityPoint,
                                       Map.Entry<String, Long> repeatedIssue,
                                       Map.Entry<String, Long> repeatedFineIssue,
                                       String topAbility) {
        if (fineTag != null && repeatedFineIssue != null && Objects.equals(fineTag, repeatedFineIssue.getKey())) {
            return "重复细分错因：" + diagnosisTaxonomy.label(fineTag);
        }
        if (issueTag != null && repeatedIssue != null && Objects.equals(issueTag, repeatedIssue.getKey())) {
            return "重复主要错因：" + diagnosisTaxonomy.label(issueTag);
        }
        if (abilityPoint != null && Objects.equals(abilityPoint, topAbility)) {
            return "关联能力点：" + abilityPoint;
        }
        if (submission.getVerdict() == Submission.Verdict.ACCEPTED) {
            return "通过后复盘证据";
        }
        return "最近未通过提交";
    }

    private String resolveStudentDisplayName(StudentProfile student, Long studentId) {
        String displayName = student == null ? "" : normalizeNullable(student.getDisplayName());
        if (displayName.isBlank() || displayName.chars().allMatch(ch -> ch == '?' || Character.isWhitespace(ch))) {
            return "学生 #" + studentId;
        }
        return displayName;
    }

    private Map.Entry<String, Long> resolveRepeatedIssue(List<Submission> submissions,
                                                         Map<Long, SubmissionAnalysis> analyses) {
        Map<String, Long> counts = new LinkedHashMap<>();
        submissions.stream()
                .limit(5)
                .map(submission -> analyses.get(submission.getId()))
                .filter(Objects::nonNull)
                .flatMap(analysis -> diagnosisReportReader.issueTags(analysis).stream())
                .filter(tag -> !"CODE_QUALITY".equals(tag) && !"GENERALIZATION_CHECK".equals(tag))
                .forEach(tag -> counts.put(tag, counts.getOrDefault(tag, 0L) + 1));

        return counts.entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .orElse(null);
    }

    private Map.Entry<String, Long> resolveRepeatedFineIssue(List<Submission> submissions,
                                                             Map<Long, SubmissionAnalysis> analyses) {
        Map<String, Long> counts = new LinkedHashMap<>();
        submissions.stream()
                .limit(5)
                .map(submission -> analyses.get(submission.getId()))
                .filter(Objects::nonNull)
                .flatMap(analysis -> diagnosisReportReader.fineGrainedTags(analysis).stream())
                .forEach(tag -> counts.put(tag, counts.getOrDefault(tag, 0L) + 1));

        return counts.entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .orElse(null);
    }

    private String resolveLatestFineGrainedIssue(SubmissionAnalysis analysis) {
        List<String> tags = diagnosisReportReader.fineGrainedTags(analysis);
        return tags.isEmpty() ? null : tags.get(0);
    }

    private String resolveLatestIssueTag(SubmissionAnalysis analysis) {
        List<String> tags = diagnosisReportReader.issueTags(analysis);
        return tags.isEmpty() ? null : tags.get(0);
    }

    private String resolveProgressSignal(SubmissionAnalysis analysis,
                                         Map.Entry<String, Long> repeatedIssue,
                                         Map.Entry<String, Long> repeatedFineIssue) {
        if (repeatedFineIssue != null && repeatedFineIssue.getValue() >= 2) {
            return "连续出现细分卡点：" + diagnosisTaxonomy.label(repeatedFineIssue.getKey());
        }
        if (repeatedIssue != null && repeatedIssue.getValue() >= 3) {
            return "连续多次出现同类问题：" + diagnosisTaxonomy.label(repeatedIssue.getKey());
        }
        if (analysis == null) {
            return "AI 诊断生成中";
        }
        String scenario = analysis.getScenario();
        if ("CE".equals(scenario)) {
            return "仍处在语法/编译修正阶段";
        }
        if ("WA".equals(scenario)) {
            return "正在进入逻辑与边界调试";
        }
        if ("TLE".equals(scenario)) {
            return "正确性接近，复杂度需要关注";
        }
        if ("AC".equals(scenario)) {
            return "本任务已通过，可进入复盘优化";
        }
        return "需要继续观察下一次提交";
    }

    private String buildAttentionReason(List<Submission> submissions,
                                        long passedCount,
                                        Map.Entry<String, Long> repeatedIssue,
                                        Map.Entry<String, Long> repeatedFineIssue) {
        if (repeatedFineIssue != null && repeatedFineIssue.getValue() >= 2) {
            return "最近多次卡在：" + diagnosisTaxonomy.label(repeatedFineIssue.getKey());
        }
        if (repeatedIssue != null && repeatedIssue.getValue() >= 3) {
            return "连续多次出现：" + diagnosisTaxonomy.label(repeatedIssue.getKey());
        }
        if (submissions.size() >= 3 && passedCount == 0) {
            return "已有 3 次以上尝试但暂未通过，建议先缩小调试范围。";
        }
        return "当前无需重点干预。";
    }

    private String normalizeDiagnosisTag(String value, boolean fineGrained, String message) {
        String id = normalizeRequired(value, message).toUpperCase(Locale.ROOT);
        DiagnosisTaxonomy.DiagnosisTag tag = diagnosisTaxonomy.get(id);
        if (tag == null || tag.isFineGrained() != fineGrained) {
            throw new IllegalArgumentException(message + ": " + value);
        }
        return tag.getId();
    }

    private AssignmentResponse toAssignmentResponse(Assignment assignment) {
        String className = assignment.getClassGroupId() == null
                ? null
                : classGroupRepository.findById(assignment.getClassGroupId()).map(ClassGroup::getName).orElse(null);
        AssignmentInvite invite = assignmentInviteRepository.findAll()
                .stream()
                .filter(item -> Objects.equals(item.getAssignmentId(), assignment.getId()))
                .filter(item -> Boolean.TRUE.equals(item.getEnabled()))
                .max(Comparator.comparing(AssignmentInvite::getCreatedAt, Comparator.nullsLast(LocalDateTime::compareTo)))
                .orElse(null);
        List<AssignmentTask> tasks = assignmentTaskRepository.findByAssignmentIdOrderByOrderIndexAsc(assignment.getId());
        Map<Long, Problem> problems = problemRepository.findAllById(tasks.stream().map(AssignmentTask::getProblemId).toList())
                .stream()
                .collect(Collectors.toMap(Problem::getId, Function.identity()));

        List<AssignmentResponse.TaskSummary> taskResponses = tasks.stream()
                .map(task -> {
                    Problem problem = problems.get(task.getProblemId());
                    return AssignmentResponse.TaskSummary.builder()
                            .problemId(task.getProblemId())
                            .title(problem == null ? "未知学习任务" : problem.getTitle())
                            .difficulty(problem == null || problem.getDifficulty() == null ? "" : problem.getDifficulty().name())
                            .orderIndex(task.getOrderIndex())
                            .required(task.getRequired())
                            .build();
                })
                .toList();

        return AssignmentResponse.from(assignment, className, invite, taskResponses);
    }

    private void validateProblemsExist(List<Long> problemIds) {
        if (problemIds == null || problemIds.isEmpty()) {
            throw new IllegalArgumentException("作业至少需要绑定一个学习任务");
        }
        List<Long> existingIds = problemRepository.findAllById(problemIds)
                .stream()
                .map(Problem::getId)
                .toList();
        List<Long> missingIds = problemIds.stream()
                .filter(id -> !existingIds.contains(id))
                .toList();
        if (!missingIds.isEmpty()) {
            throw new IllegalArgumentException("学习任务不存在: " + missingIds);
        }
    }

    private void saveAssignmentTasks(Long assignmentId, List<Long> problemIds) {
        for (int index = 0; index < problemIds.size(); index++) {
            assignmentTaskRepository.save(AssignmentTask.builder()
                    .assignmentId(assignmentId)
                    .problemId(problemIds.get(index))
                    .orderIndex(index)
                    .required(true)
                    .build());
        }
    }

    private AssignmentInvite ensureInviteForAssignment(Assignment assignment) {
        return assignmentInviteRepository.findAll()
                .stream()
                .filter(item -> Objects.equals(item.getAssignmentId(), assignment.getId()))
                .filter(item -> Boolean.TRUE.equals(item.getEnabled()))
                .findFirst()
                .orElseGet(() -> assignmentInviteRepository.save(AssignmentInvite.builder()
                        .assignmentId(assignment.getId())
                        .code(generateInviteCode())
                        .enabled(true)
                        .build()));
    }

    private AssignmentInvite findEnabledInvite(String code) {
        String normalized = normalizeRequired(code, "邀请码不能为空").toUpperCase(Locale.ROOT);
        AssignmentInvite invite = assignmentInviteRepository.findByCodeIgnoreCase(normalized)
                .orElseThrow(() -> new IllegalArgumentException("邀请码不存在或已过期"));
        if (!Boolean.TRUE.equals(invite.getEnabled())) {
            throw new IllegalArgumentException("邀请码已停用");
        }
        return invite;
    }

    private void ensureAssignmentOpen(Assignment assignment, AssignmentInvite invite) {
        LocalDateTime now = LocalDateTime.now();
        if (assignment.getStatus() == Assignment.AssignmentStatus.CLOSED) {
            throw new IllegalArgumentException("该学习任务已结束");
        }
        if (invite.getExpiresAt() != null && invite.getExpiresAt().isBefore(now)) {
            throw new IllegalArgumentException("邀请码已过期");
        }
        if (assignment.getStartsAt() != null && assignment.getStartsAt().isAfter(now)) {
            throw new IllegalArgumentException("该学习任务尚未开始");
        }
    }

    private Assignment findAssignment(Long assignmentId) {
        return assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("作业不存在: " + assignmentId));
    }

    private String generateInviteCode() {
        for (int attempt = 0; attempt < 20; attempt++) {
            StringBuilder code = new StringBuilder("WZ");
            for (int index = 0; index < 4; index++) {
                code.append(INVITE_ALPHABET.charAt(RANDOM.nextInt(INVITE_ALPHABET.length())));
            }
            String candidate = code.toString();
            if (!assignmentInviteRepository.existsByCodeIgnoreCase(candidate)) {
                return candidate;
            }
        }
        return "WZ" + System.currentTimeMillis();
    }

    private String normalizeRequired(String value, String message) {
        String normalized = normalizeNullable(value);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    private String normalizeNullable(String value) {
        return value == null ? "" : value.trim();
    }
}
