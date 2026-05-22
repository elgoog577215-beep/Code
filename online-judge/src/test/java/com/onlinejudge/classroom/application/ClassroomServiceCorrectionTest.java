package com.onlinejudge.classroom.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlinejudge.classroom.domain.*;
import com.onlinejudge.classroom.dto.TeacherDiagnosisCorrectionRequest;
import com.onlinejudge.classroom.persistence.*;
import com.onlinejudge.leaderboard.persistence.ProblemSubmissionStatsProjection;
import com.onlinejudge.learning.diagnosis.DiagnosisReportReader;
import com.onlinejudge.learning.diagnosis.DiagnosisTaxonomy;
import com.onlinejudge.problem.domain.Problem;
import com.onlinejudge.problem.persistence.ProblemCatalogProjection;
import com.onlinejudge.problem.persistence.ProblemRepository;
import com.onlinejudge.submission.domain.Submission;
import com.onlinejudge.submission.domain.SubmissionAnalysis;
import com.onlinejudge.submission.persistence.SubmissionHistoryProjection;
import com.onlinejudge.submission.persistence.SubmissionAnalysisRepository;
import com.onlinejudge.submission.persistence.SubmissionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.*;
import org.springframework.data.repository.query.FluentQuery;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClassroomServiceCorrectionTest {

    private final FakeAssignmentRepository assignmentRepository = new FakeAssignmentRepository();
    private final FakeSubmissionRepository submissionRepository = new FakeSubmissionRepository();
    private final FakeSubmissionAnalysisRepository submissionAnalysisRepository = new FakeSubmissionAnalysisRepository();
    private final FakeTeacherDiagnosisCorrectionRepository correctionRepository = new FakeTeacherDiagnosisCorrectionRepository();
    private final FakeClassReviewFeedbackRepository classReviewFeedbackRepository = new FakeClassReviewFeedbackRepository();
    private final FakeCoachPromptRepository coachPromptRepository = new FakeCoachPromptRepository();
    private final FakeProblemRepository problemRepository = new FakeProblemRepository();
    private final DiagnosisTaxonomy taxonomy = new DiagnosisTaxonomy();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final ClassroomService service = new ClassroomService(
            new EmptyClassGroupRepository(),
            new EmptyStudentProfileRepository(),
            assignmentRepository,
            new EmptyAssignmentInviteRepository(),
            new EmptyAssignmentTaskRepository(),
            problemRepository,
            submissionRepository,
            submissionAnalysisRepository,
            correctionRepository,
            objectMapper,
            taxonomy,
            new DiagnosisReportReader(objectMapper, taxonomy),
            new AbilitySignalAnalyzer(new DiagnosisReportReader(objectMapper, taxonomy), taxonomy),
            new CoachInteractionAnalyzer(coachPromptRepository, new CoachAnswerQualityAnalyzer()),
            new StudentIdentityService(),
            new ClassReviewFeedbackService(classReviewFeedbackRepository, assignmentRepository, objectMapper),
            new CoachImpactAnalyzer(new DiagnosisReportReader(objectMapper, taxonomy), taxonomy),
            new LearningInterventionImpactAnalyzer(new DiagnosisReportReader(objectMapper, taxonomy), taxonomy),
            new LearningActionEvidenceAnalyzer(new DiagnosisReportReader(objectMapper, taxonomy)),
            new TeacherActionPriorityAnalyzer(new DiagnosisReportReader(objectMapper, taxonomy), taxonomy)
    );

    @Test
    void overviewIncludesTeachingInterventionForTopIssues() {
        Assignment assignment = Assignment.builder()
                .id(7L)
                .title("课堂作业")
                .build();
        Submission submission = Submission.builder()
                .id(21L)
                .assignmentId(assignment.getId())
                .studentProfileId(3L)
                .problemId(101L)
                .languageId(71)
                .sourceCode("print(1)")
                .verdict(Submission.Verdict.WRONG_ANSWER)
                .submittedAt(LocalDateTime.of(2026, 5, 18, 10, 0))
                .build();
        assignmentRepository.items.put(assignment.getId(), assignment);
        problemRepository.items.put(101L, Problem.builder()
                .id(101L)
                .title("array boundary")
                .description("array boundary")
                .difficulty(Problem.Difficulty.EASY)
                .timeLimit(1000)
                .memoryLimit(65536)
                .build());
        submissionRepository.items.put(submission.getId(), submission);
        submissionAnalysisRepository.save(SubmissionAnalysis.builder()
                .submissionId(submission.getId())
                .analysisSource("RULE_BASED_V1")
                .scenario("WA")
                .headline("边界问题")
                .summary("可能是边界问题")
                .reportMarkdown("检查边界")
                .reportJson("""
                        {
                          "issueTags": ["BOUNDARY_CONDITION"],
                          "fineGrainedTags": ["OFF_BY_ONE"]
                        }
                        """)
                .build());

        var overview = service.getAssignmentOverview(assignment.getId());

        assertThat(overview.getTopIssues()).first()
                .satisfies(issue -> {
                    assertThat(issue.getLabel()).isEqualTo("OFF_BY_ONE");
                    assertThat(issue.getExplanation()).contains("循环变量");
                    assertThat(issue.getAbilityPoint()).isEqualTo("循环与边界");
                    assertThat(issue.getRecommendedHintPolicy()).isEqualTo("L2");
                    assertThat(issue.getInterventionSuggestion()).contains("循环变量表");
                });
        assertThat(overview.getClassAbilityWeaknesses()).first()
                .satisfies(ability -> {
                    assertThat(ability.getAbilityPoint()).isEqualTo("循环与边界");
                    assertThat(ability.getSubmissionCount()).isEqualTo(1);
                });
        assertThat(overview.getStudents()).first()
                .satisfies(student -> {
                    assertThat(student.getAttentionEvidence()).hasSize(1);
                    assertThat(student.getAttentionEvidence().get(0).getSubmissionId()).isEqualTo(submission.getId());
                    assertThat(student.getAttentionEvidence().get(0).getFineGrainedTag()).isEqualTo("OFF_BY_ONE");
                    assertThat(student.getAttentionEvidence().get(0).getAbilityPoint()).isEqualTo("循环与边界");
                });
    }

    @Test
    void overviewIncludesClassReviewSuggestions() {
        Assignment assignment = Assignment.builder()
                .id(17L)
                .title("class review")
                .build();
        Submission submission = Submission.builder()
                .id(31L)
                .assignmentId(assignment.getId())
                .studentProfileId(5L)
                .problemId(101L)
                .languageId(71)
                .sourceCode("print(1)")
                .verdict(Submission.Verdict.WRONG_ANSWER)
                .submittedAt(LocalDateTime.of(2026, 5, 18, 10, 0))
                .build();
        assignmentRepository.items.put(assignment.getId(), assignment);
        problemRepository.items.put(101L, Problem.builder()
                .id(101L)
                .title("array boundary")
                .description("array boundary")
                .difficulty(Problem.Difficulty.EASY)
                .timeLimit(1000)
                .memoryLimit(65536)
                .build());
        submissionRepository.items.put(submission.getId(), submission);
        submissionAnalysisRepository.save(SubmissionAnalysis.builder()
                .submissionId(submission.getId())
                .analysisSource("RULE_BASED_V1")
                .scenario("WA")
                .headline("boundary")
                .summary("boundary")
                .reportMarkdown("boundary")
                .reportJson("""
                        {
                          "issueTags": ["BOUNDARY_CONDITION"],
                          "fineGrainedTags": ["OFF_BY_ONE"]
                        }
                        """)
                .build());

        var overview = service.getAssignmentOverview(assignment.getId());

        assertThat(overview.getClassReviewSuggestions()).first()
                .satisfies(suggestion -> {
                    assertThat(suggestion.getSuggestionKey()).startsWith("review:17:");
                    assertThat(suggestion.getExampleProblemId()).isEqualTo(101L);
                    assertThat(suggestion.getExampleProblemTitle()).isEqualTo("array boundary");
                    assertThat(suggestion.getEvidenceTags()).contains("OFF_BY_ONE");
                    assertThat(suggestion.getEvidenceSubmissionIds()).contains(submission.getId());
                    assertThat(suggestion.getGuidingQuestion()).isNotBlank();
                    assertThat(suggestion.getAction()).isNotBlank();
                });
    }

    @Test
    void overviewIncludesLatestClassReviewFeedback() {
        Assignment assignment = Assignment.builder()
                .id(18L)
                .title("class review feedback")
                .build();
        Submission submission = Submission.builder()
                .id(32L)
                .assignmentId(assignment.getId())
                .studentProfileId(5L)
                .problemId(101L)
                .languageId(71)
                .sourceCode("print(1)")
                .verdict(Submission.Verdict.WRONG_ANSWER)
                .submittedAt(LocalDateTime.of(2026, 5, 18, 10, 0))
                .build();
        assignmentRepository.items.put(assignment.getId(), assignment);
        problemRepository.items.put(101L, Problem.builder()
                .id(101L)
                .title("array boundary")
                .description("array boundary")
                .difficulty(Problem.Difficulty.EASY)
                .timeLimit(1000)
                .memoryLimit(65536)
                .build());
        submissionRepository.items.put(submission.getId(), submission);
        submissionAnalysisRepository.save(SubmissionAnalysis.builder()
                .submissionId(submission.getId())
                .analysisSource("RULE_BASED_V1")
                .scenario("WA")
                .headline("boundary")
                .summary("boundary")
                .reportMarkdown("boundary")
                .reportJson("""
                        {
                          "issueTags": ["BOUNDARY_CONDITION"],
                          "fineGrainedTags": ["OFF_BY_ONE"]
                        }
                        """)
                .build());

        var initialOverview = service.getAssignmentOverview(assignment.getId());
        var suggestion = initialOverview.getClassReviewSuggestions().get(0);

        var request = new com.onlinejudge.classroom.dto.ClassReviewFeedbackRequest();
        request.setSuggestionKey(suggestion.getSuggestionKey());
        request.setActionType(ClassReviewFeedbackService.ACTION_MODIFIED);
        request.setTargetAbility(suggestion.getTargetAbility());
        request.setExampleProblemId(suggestion.getExampleProblemId());
        request.setEvidenceTags(suggestion.getEvidenceTags());
        request.setTeacherNote("改成先做一个更小的边界样例");
        request.setCreatedBy("teacher");
        new ClassReviewFeedbackService(classReviewFeedbackRepository, assignmentRepository, objectMapper)
                .recordFeedback(assignment.getId(), request);

        var overview = service.getAssignmentOverview(assignment.getId());

        assertThat(overview.getClassReviewSuggestions()).first()
                .satisfies(item -> {
                    assertThat(item.getLatestFeedback()).isNotNull();
                    assertThat(item.getLatestFeedback().getActionType()).isEqualTo(ClassReviewFeedbackService.ACTION_MODIFIED);
                    assertThat(item.getLatestFeedback().getTeacherNote()).contains("更小的边界样例");
                });
    }

    @Test
    void overviewIncludesCoachImpactForStudentProgress() {
        Assignment assignment = Assignment.builder()
                .id(19L)
                .title("coach impact")
                .build();
        Submission coached = Submission.builder()
                .id(41L)
                .assignmentId(assignment.getId())
                .studentProfileId(5L)
                .problemId(101L)
                .languageId(71)
                .sourceCode("print(1)")
                .verdict(Submission.Verdict.WRONG_ANSWER)
                .submittedAt(LocalDateTime.of(2026, 5, 18, 10, 0))
                .build();
        Submission followup = Submission.builder()
                .id(42L)
                .assignmentId(assignment.getId())
                .studentProfileId(5L)
                .problemId(101L)
                .languageId(71)
                .sourceCode("print(sum(values))")
                .verdict(Submission.Verdict.ACCEPTED)
                .submittedAt(LocalDateTime.of(2026, 5, 18, 10, 12))
                .build();
        assignmentRepository.items.put(assignment.getId(), assignment);
        problemRepository.items.put(101L, Problem.builder()
                .id(101L)
                .title("array boundary")
                .description("array boundary")
                .difficulty(Problem.Difficulty.EASY)
                .timeLimit(1000)
                .memoryLimit(65536)
                .build());
        submissionRepository.items.put(coached.getId(), coached);
        submissionRepository.items.put(followup.getId(), followup);
        submissionAnalysisRepository.save(SubmissionAnalysis.builder()
                .submissionId(coached.getId())
                .analysisSource("RULE_BASED_V1")
                .scenario("WA")
                .headline("boundary")
                .summary("boundary")
                .reportMarkdown("boundary")
                .reportJson("""
                        {
                          "issueTags": ["BOUNDARY_CONDITION"],
                          "fineGrainedTags": ["OFF_BY_ONE"]
                        }
                        """)
                .build());
        coachPromptRepository.saved.add(CoachPrompt.builder()
                .id(51L)
                .submissionId(coached.getId())
                .turnIndex(1)
                .hintPolicy("L2")
                .promptType("SOCRATIC_NEXT_STEP")
                .question("请手推 n=1")
                .studentAnswer("循环应该执行一次")
                .coachFeedback("继续验证最后一个元素")
                .createdAt(LocalDateTime.of(2026, 5, 18, 10, 5))
                .build());

        var overview = service.getAssignmentOverview(assignment.getId());

        assertThat(overview.getStudents()).first()
                .satisfies(student -> {
                    assertThat(student.getLatestCoachImpact()).isNotNull();
                    assertThat(student.getLatestCoachImpact().getStatus()).isEqualTo("FOLLOWUP_ACCEPTED");
                    assertThat(student.getLatestCoachInteraction().getImpact().getFollowupSubmissionId()).isEqualTo(followup.getId());
                });
    }

    @Test
    void exportsTeacherCorrectionsAsEvalCandidates() {
        TestFixture fixture = createFixture(7L, 11L);
        fixture.submission().setProblemId(101L);
        fixture.submission().setVerdict(Submission.Verdict.WRONG_ANSWER);
        fixture.submission().setSourceCode("print('wrong')");
        problemRepository.items.put(101L, Problem.builder()
                .id(101L)
                .title("array boundary")
                .description("array boundary")
                .difficulty(Problem.Difficulty.EASY)
                .timeLimit(1000)
                .memoryLimit(65536)
                .build());
        submissionAnalysisRepository.save(SubmissionAnalysis.builder()
                .submissionId(fixture.submission().getId())
                .analysisSource("RULE_BASED_V1")
                .scenario("WA")
                .headline("边界问题")
                .summary("可能是边界问题")
                .reportMarkdown("检查边界")
                .reportJson("""
                        {
                          "issueTags": ["BOUNDARY_CONDITION"],
                          "fineGrainedTags": ["OFF_BY_ONE"]
                        }
                        """)
                .build());

        TeacherDiagnosisCorrection correction = TeacherDiagnosisCorrection.builder()
                .assignmentId(fixture.assignment().getId())
                .submissionId(fixture.submission().getId())
                .studentProfileId(3L)
                .originalIssueTag("BOUNDARY_CONDITION")
                .originalFineGrainedTag("OFF_BY_ONE")
                .correctedIssueTag("IO_FORMAT")
                .correctedFineGrainedTag("INPUT_PARSING")
                .teacherNote("实际是输入结构读错")
                .evalCandidate(true)
                .correctedBy("teacher")
                .build();
        correctionRepository.save(correction);

        var response = service.getDiagnosisEvalCandidates(fixture.assignment().getId());

        assertThat(response.getCandidateCount()).isEqualTo(1);
        assertThat(response.getCandidates()).first()
                .satisfies(candidate -> {
                    assertThat(candidate.getSubmissionId()).isEqualTo(fixture.submission().getId());
                    assertThat(candidate.getProblemId()).isEqualTo(101L);
                    assertThat(candidate.getProblemTitle()).isEqualTo("array boundary");
                    assertThat(candidate.getProblemDescription()).isEqualTo("array boundary");
                    assertThat(candidate.getProblemDifficulty()).isEqualTo("EASY");
                    assertThat(candidate.getProblemTimeLimit()).isEqualTo(1000);
                    assertThat(candidate.getProblemMemoryLimit()).isEqualTo(65536);
                    assertThat(candidate.getVerdict()).isEqualTo("WRONG_ANSWER");
                    assertThat(candidate.getSourceCode()).contains("wrong");
                    assertThat(candidate.getScenario()).isEqualTo("WA");
                    assertThat(candidate.getCorrectedIssueTag()).isEqualTo("IO_FORMAT");
                    assertThat(candidate.getCorrectedFineGrainedTag()).isEqualTo("INPUT_PARSING");
                    assertThat(candidate.getTeacherNote()).contains("输入结构");
                    assertThat(candidate.getSourceCodePreview()).contains("wrong");
                });
    }

    @Test
    void savesTeacherCorrectionAsEvalCandidate() {
        TestFixture fixture = createFixture(7L, 11L);
        submissionAnalysisRepository.save(SubmissionAnalysis.builder()
                .submissionId(fixture.submission().getId())
                .analysisSource("RULE_BASED_V1")
                .scenario("WA")
                .headline("边界问题")
                .summary("可能是边界问题")
                .reportMarkdown("检查边界")
                .reportJson("""
                        {
                          "issueTags": ["BOUNDARY_CONDITION"],
                          "fineGrainedTags": ["OFF_BY_ONE"]
                        }
                        """)
                .build());

        TeacherDiagnosisCorrectionRequest request = new TeacherDiagnosisCorrectionRequest();
        request.setSubmissionId(fixture.submission().getId());
        request.setCorrectedIssueTag("IO_FORMAT");
        request.setCorrectedFineGrainedTag("INPUT_PARSING");
        request.setTeacherNote("实际问题是输入读取结构理解错。");

        var response = service.correctDiagnosis(fixture.assignment().getId(), request);

        assertThat(response.getOriginalIssueTag()).isEqualTo("BOUNDARY_CONDITION");
        assertThat(response.getOriginalFineGrainedTag()).isEqualTo("OFF_BY_ONE");
        assertThat(response.getCorrectedIssueTag()).isEqualTo("IO_FORMAT");
        assertThat(response.getCorrectedFineGrainedTag()).isEqualTo("INPUT_PARSING");
        assertThat(response.isEvalCandidate()).isTrue();
        assertThat(correctionRepository.saved).hasSize(1);
    }

    @Test
    void rejectsSubmissionOutsideAssignment() {
        TestFixture fixture = createFixture(7L, 11L);
        Assignment otherAssignment = Assignment.builder()
                .id(8L)
                .title("另一个作业")
                .build();
        assignmentRepository.items.put(otherAssignment.getId(), otherAssignment);

        TeacherDiagnosisCorrectionRequest request = new TeacherDiagnosisCorrectionRequest();
        request.setSubmissionId(fixture.submission().getId());
        request.setCorrectedIssueTag("IO_FORMAT");

        assertThatThrownBy(() -> service.correctDiagnosis(otherAssignment.getId(), request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不属于当前作业");
    }

    @Test
    void rejectsUnknownCorrectionTag() {
        TestFixture fixture = createFixture(7L, 11L);

        TeacherDiagnosisCorrectionRequest request = new TeacherDiagnosisCorrectionRequest();
        request.setSubmissionId(fixture.submission().getId());
        request.setCorrectedIssueTag("NOT_A_REAL_TAG");

        assertThatThrownBy(() -> service.correctDiagnosis(fixture.assignment().getId(), request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("粗粒度错因不存在");
    }

    private TestFixture createFixture(Long assignmentId, Long submissionId) {
        Assignment assignment = Assignment.builder()
                .id(assignmentId)
                .title("课堂作业")
                .build();
        Submission submission = Submission.builder()
                .id(submissionId)
                .assignmentId(assignmentId)
                .studentProfileId(3L)
                .build();
        assignmentRepository.items.put(assignmentId, assignment);
        submissionRepository.items.put(submissionId, submission);
        return new TestFixture(assignment, submission);
    }

    record TestFixture(Assignment assignment, Submission submission) {
    }

    private static class FakeAssignmentRepository extends UnsupportedJpaRepository<Assignment, Long> implements AssignmentRepository {
        private final Map<Long, Assignment> items = new LinkedHashMap<>();

        @Override
        public Optional<Assignment> findById(Long id) {
            return Optional.ofNullable(items.get(id));
        }

        @Override
        public boolean existsById(Long id) {
            return items.containsKey(id);
        }

        @Override
        public List<Assignment> findAllByOrderByCreatedAtDesc() {
            return List.copyOf(items.values());
        }
    }

    private static class FakeSubmissionRepository extends UnsupportedJpaRepository<Submission, Long> implements SubmissionRepository {
        private final Map<Long, Submission> items = new LinkedHashMap<>();

        @Override
        public Optional<Submission> findById(Long id) {
            return Optional.ofNullable(items.get(id));
        }

        @Override
        public List<Submission> findByProblemIdOrderBySubmittedAtDesc(Long problemId) {
            return List.of();
        }

        @Override
        public List<Submission> findByProblemIdOrderBySubmittedAtAsc(Long problemId) {
            return List.of();
        }

        @Override
        public List<Submission> findByAssignmentIdOrderBySubmittedAtDesc(Long assignmentId) {
            return items.values()
                    .stream()
                    .filter(item -> Objects.equals(item.getAssignmentId(), assignmentId))
                    .sorted(Comparator.comparing(Submission::getSubmittedAt, Comparator.nullsLast(LocalDateTime::compareTo)).reversed())
                    .toList();
        }

        @Override
        public List<Submission> findByAssignmentIdIn(Collection<Long> assignmentIds) {
            return items.values()
                    .stream()
                    .filter(item -> assignmentIds.contains(item.getAssignmentId()))
                    .toList();
        }

        @Override
        public List<Submission> findByStudentProfileIdInOrderBySubmittedAtDesc(Collection<Long> studentProfileIds) {
            return List.of();
        }

        @Override
        public List<Submission> findByAssignmentIdAndStudentProfileIdOrderBySubmittedAtDesc(Long assignmentId, Long studentProfileId) {
            return List.of();
        }

        @Override
        public List<Submission> findAllById(Iterable<Long> ids) {
            Set<Long> wanted = new LinkedHashSet<>();
            ids.forEach(wanted::add);
            return items.values()
                    .stream()
                    .filter(item -> wanted.contains(item.getId()))
                    .toList();
        }

        @Override
        public List<Submission> findByAssignmentIdAndProblemIdAndStudentProfileIdOrderBySubmittedAtDesc(Long assignmentId, Long problemId, Long studentProfileId) {
            return List.of();
        }

        @Override
        public List<Submission> findTop10ByOrderBySubmittedAtDesc() {
            return List.of();
        }

        @Override
        public List<SubmissionHistoryProjection> findHistorySummariesByProblemId(Long problemId) {
            return List.of();
        }

        @Override
        public List<ProblemSubmissionStatsProjection> summarizeByProblem() {
            return List.of();
        }

        @Override
        public long deleteByProblemId(Long problemId) {
            return 0;
        }
    }

    private static class FakeSubmissionAnalysisRepository extends UnsupportedJpaRepository<SubmissionAnalysis, Long> implements SubmissionAnalysisRepository {
        private final Map<Long, SubmissionAnalysis> bySubmissionId = new LinkedHashMap<>();

        @Override
        public SubmissionAnalysis save(SubmissionAnalysis analysis) {
            bySubmissionId.put(analysis.getSubmissionId(), analysis);
            return analysis;
        }

        @Override
        public Optional<SubmissionAnalysis> findBySubmissionId(Long submissionId) {
            return Optional.ofNullable(bySubmissionId.get(submissionId));
        }

        @Override
        public List<SubmissionAnalysis> findBySubmissionIdIn(Collection<Long> submissionIds) {
            return bySubmissionId.entrySet()
                    .stream()
                    .filter(entry -> submissionIds.contains(entry.getKey()))
                    .map(Map.Entry::getValue)
                    .toList();
        }

        @Override
        public long deleteBySubmissionId(Long submissionId) {
            return bySubmissionId.remove(submissionId) == null ? 0 : 1;
        }

        @Override
        public long deleteBySubmissionIdIn(Collection<Long> submissionIds) {
            long before = bySubmissionId.size();
            submissionIds.forEach(bySubmissionId::remove);
            return before - bySubmissionId.size();
        }
    }

    private static class FakeTeacherDiagnosisCorrectionRepository extends UnsupportedJpaRepository<TeacherDiagnosisCorrection, Long>
            implements TeacherDiagnosisCorrectionRepository {
        private final List<TeacherDiagnosisCorrection> saved = new ArrayList<>();
        private long nextId = 1;

        @Override
        public TeacherDiagnosisCorrection save(TeacherDiagnosisCorrection correction) {
            correction.setId(nextId++);
            correction.setCorrectedAt(LocalDateTime.of(2026, 5, 18, 11, 30));
            saved.add(correction);
            return correction;
        }

        @Override
        public Optional<TeacherDiagnosisCorrection> findTopBySubmissionIdOrderByCorrectedAtDesc(Long submissionId) {
            return saved.stream()
                    .filter(item -> Objects.equals(item.getSubmissionId(), submissionId))
                    .reduce((left, right) -> right);
        }

        @Override
        public List<TeacherDiagnosisCorrection> findBySubmissionIdIn(Collection<Long> submissionIds) {
            return saved.stream()
                    .filter(item -> submissionIds.contains(item.getSubmissionId()))
                    .toList();
        }

        @Override
        public List<TeacherDiagnosisCorrection> findByAssignmentIdIn(Collection<Long> assignmentIds) {
            return saved.stream()
                    .filter(item -> assignmentIds.contains(item.getAssignmentId()))
                    .toList();
        }

        @Override
        public List<TeacherDiagnosisCorrection> findByAssignmentIdOrderByCorrectedAtDesc(Long assignmentId) {
            return saved.stream()
                    .filter(item -> Objects.equals(item.getAssignmentId(), assignmentId))
                    .sorted(Comparator.comparing(TeacherDiagnosisCorrection::getCorrectedAt, Comparator.nullsLast(LocalDateTime::compareTo)).reversed())
                    .toList();
        }

        @Override
        public List<TeacherDiagnosisCorrection> findByAssignmentIdAndEvalCandidateTrueOrderByCorrectedAtDesc(Long assignmentId) {
            return saved.stream()
                    .filter(item -> Objects.equals(item.getAssignmentId(), assignmentId))
                    .filter(TeacherDiagnosisCorrection::isEvalCandidate)
                    .sorted(Comparator.comparing(TeacherDiagnosisCorrection::getCorrectedAt, Comparator.nullsLast(LocalDateTime::compareTo)).reversed())
                    .toList();
        }
    }

    private static class FakeClassReviewFeedbackRepository extends UnsupportedJpaRepository<ClassReviewFeedback, Long>
            implements ClassReviewFeedbackRepository {
        private final List<ClassReviewFeedback> saved = new ArrayList<>();
        private long nextId = 1;

        @Override
        public ClassReviewFeedback save(ClassReviewFeedback feedback) {
            feedback.setId(nextId++);
            feedback.setCreatedAt(LocalDateTime.of(2026, 5, 18, 12, 0).plusMinutes(saved.size()));
            saved.add(feedback);
            return feedback;
        }

        @Override
        public List<ClassReviewFeedback> findByAssignmentIdOrderByCreatedAtDesc(Long assignmentId) {
            return saved.stream()
                    .filter(item -> Objects.equals(item.getAssignmentId(), assignmentId))
                    .sorted(Comparator.comparing(ClassReviewFeedback::getCreatedAt, Comparator.nullsLast(LocalDateTime::compareTo)).reversed())
                    .toList();
        }

        @Override
        public List<ClassReviewFeedback> findByAssignmentIdIn(Collection<Long> assignmentIds) {
            return saved.stream()
                    .filter(item -> assignmentIds.contains(item.getAssignmentId()))
                    .toList();
        }

        @Override
        public Optional<ClassReviewFeedback> findTopByAssignmentIdAndSuggestionKeyOrderByCreatedAtDesc(Long assignmentId, String suggestionKey) {
            return saved.stream()
                    .filter(item -> Objects.equals(item.getAssignmentId(), assignmentId))
                    .filter(item -> Objects.equals(item.getSuggestionKey(), suggestionKey))
                    .max(Comparator.comparing(ClassReviewFeedback::getCreatedAt, Comparator.nullsLast(LocalDateTime::compareTo)));
        }
    }

    private static class FakeCoachPromptRepository extends UnsupportedJpaRepository<CoachPrompt, Long> implements CoachPromptRepository {
        private final List<CoachPrompt> saved = new ArrayList<>();

        @Override
        public Optional<CoachPrompt> findTopBySubmissionIdOrderByCreatedAtDesc(Long submissionId) {
            return saved.stream()
                    .filter(item -> Objects.equals(item.getSubmissionId(), submissionId))
                    .reduce((left, right) -> right);
        }

        @Override
        public List<CoachPrompt> findBySubmissionIdOrderByTurnIndexAscCreatedAtAsc(Long submissionId) {
            return saved.stream()
                    .filter(item -> Objects.equals(item.getSubmissionId(), submissionId))
                    .sorted(Comparator
                            .comparing(CoachPrompt::getTurnIndex, Comparator.nullsLast(Integer::compareTo))
                            .thenComparing(CoachPrompt::getCreatedAt, Comparator.nullsLast(LocalDateTime::compareTo)))
                    .toList();
        }

        @Override
        public List<CoachPrompt> findBySubmissionIdIn(Collection<Long> submissionIds) {
            return saved.stream()
                    .filter(item -> submissionIds.contains(item.getSubmissionId()))
                    .toList();
        }
    }

    private static class EmptyClassGroupRepository extends UnsupportedJpaRepository<ClassGroup, Long> implements ClassGroupRepository {
        @Override
        public List<ClassGroup> findAllByOrderByCreatedAtDesc() {
            return List.of();
        }

        @Override
        public Optional<ClassGroup> findByNameIgnoreCase(String name) {
            return Optional.empty();
        }
    }

    private static class EmptyStudentProfileRepository extends UnsupportedJpaRepository<StudentProfile, Long> implements StudentProfileRepository {
        @Override
        public List<StudentProfile> findAllById(Iterable<Long> ids) {
            return List.of();
        }

        @Override
        public Optional<StudentProfile> findByIdentityKey(String identityKey) {
            return Optional.empty();
        }

        @Override
        public List<StudentProfile> findByIdentityKeyOrderByCreatedAtDesc(String identityKey) {
            return List.of();
        }

        @Override
        public List<StudentProfile> findByIdentityKeyIn(Collection<String> identityKeys) {
            return List.of();
        }

        @Override
        public List<StudentProfile> findByClassGroupIdOrderByStudentNoAscDisplayNameAsc(Long classGroupId) {
            return List.of();
        }

        @Override
        public List<StudentProfile> findByClassGroupIdAndStudentNoIgnoreCaseOrderByCreatedAtDesc(Long classGroupId, String studentNo) {
            return List.of();
        }

        @Override
        public List<StudentProfile> findByClassGroupIdAndDisplayNameIgnoreCaseOrderByCreatedAtDesc(Long classGroupId, String displayName) {
            return List.of();
        }
    }

    private static class EmptyAssignmentInviteRepository extends UnsupportedJpaRepository<AssignmentInvite, Long> implements AssignmentInviteRepository {
        @Override
        public List<AssignmentInvite> findAll() {
            return List.of();
        }

        @Override
        public Optional<AssignmentInvite> findByCodeIgnoreCase(String code) {
            return Optional.empty();
        }

        @Override
        public boolean existsByCodeIgnoreCase(String code) {
            return false;
        }
    }

    private static class EmptyAssignmentTaskRepository extends UnsupportedJpaRepository<AssignmentTask, Long> implements AssignmentTaskRepository {
        @Override
        public List<AssignmentTask> findByAssignmentIdOrderByOrderIndexAsc(Long assignmentId) {
            return List.of();
        }

        @Override
        public List<AssignmentTask> findByAssignmentIdIn(Collection<Long> assignmentIds) {
            return List.of();
        }

        @Override
        public boolean existsByAssignmentIdAndProblemId(Long assignmentId, Long problemId) {
            return false;
        }

        @Override
        public long deleteByAssignmentId(Long assignmentId) {
            return 0;
        }
    }

    private static class FakeProblemRepository extends UnsupportedJpaRepository<Problem, Long> implements ProblemRepository {
        private final Map<Long, Problem> items = new LinkedHashMap<>();

        @Override
        public List<Problem> findAllById(Iterable<Long> ids) {
            Set<Long> wanted = new LinkedHashSet<>();
            ids.forEach(wanted::add);
            return items.values()
                    .stream()
                    .filter(item -> wanted.contains(item.getId()))
                    .toList();
        }

        @Override
        public List<Problem> findAllByOrderByIdAsc() {
            return List.of();
        }

        @Override
        public List<ProblemCatalogProjection> findCatalogItems() {
            return List.of();
        }

        @Override
        public Optional<String> findTitleById(Long id) {
            return Optional.empty();
        }
    }

    private abstract static class UnsupportedJpaRepository<T, ID> {
        public List<T> findAll() {
            throw unsupported();
        }

        public List<T> findAllById(Iterable<ID> ids) {
            throw unsupported();
        }

        public <S extends T> S save(S entity) {
            throw unsupported();
        }

        public <S extends T> List<S> saveAll(Iterable<S> entities) {
            throw unsupported();
        }

        public Optional<T> findById(ID id) {
            throw unsupported();
        }

        public boolean existsById(ID id) {
            throw unsupported();
        }

        public long count() {
            throw unsupported();
        }

        public void deleteById(ID id) {
            throw unsupported();
        }

        public void delete(T entity) {
            throw unsupported();
        }

        public void deleteAllById(Iterable<? extends ID> ids) {
            throw unsupported();
        }

        public void deleteAll(Iterable<? extends T> entities) {
            throw unsupported();
        }

        public void deleteAll() {
            throw unsupported();
        }

        public void flush() {
            throw unsupported();
        }

        public <S extends T> S saveAndFlush(S entity) {
            throw unsupported();
        }

        public <S extends T> List<S> saveAllAndFlush(Iterable<S> entities) {
            throw unsupported();
        }

        public void deleteAllInBatch(Iterable<T> entities) {
            throw unsupported();
        }

        public void deleteAllByIdInBatch(Iterable<ID> ids) {
            throw unsupported();
        }

        public void deleteAllInBatch() {
            throw unsupported();
        }

        public T getOne(ID id) {
            throw unsupported();
        }

        public T getById(ID id) {
            throw unsupported();
        }

        public T getReferenceById(ID id) {
            throw unsupported();
        }

        public List<T> findAll(Sort sort) {
            throw unsupported();
        }

        public Page<T> findAll(Pageable pageable) {
            throw unsupported();
        }

        public <S extends T> Optional<S> findOne(Example<S> example) {
            throw unsupported();
        }

        public <S extends T> List<S> findAll(Example<S> example) {
            throw unsupported();
        }

        public <S extends T> List<S> findAll(Example<S> example, Sort sort) {
            throw unsupported();
        }

        public <S extends T> Page<S> findAll(Example<S> example, Pageable pageable) {
            throw unsupported();
        }

        public <S extends T> long count(Example<S> example) {
            throw unsupported();
        }

        public <S extends T> boolean exists(Example<S> example) {
            throw unsupported();
        }

        public <S extends T, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) {
            throw unsupported();
        }

        private static UnsupportedOperationException unsupported() {
            return new UnsupportedOperationException("Not used in this test");
        }
    }

}
