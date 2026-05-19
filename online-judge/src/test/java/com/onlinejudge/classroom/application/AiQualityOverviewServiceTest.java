package com.onlinejudge.classroom.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlinejudge.classroom.domain.Assignment;
import com.onlinejudge.classroom.domain.TeacherDiagnosisCorrection;
import com.onlinejudge.classroom.persistence.AssignmentRepository;
import com.onlinejudge.classroom.persistence.TeacherDiagnosisCorrectionRepository;
import com.onlinejudge.learning.diagnosis.DiagnosisReportReader;
import com.onlinejudge.learning.diagnosis.DiagnosisTaxonomy;
import com.onlinejudge.submission.domain.Submission;
import com.onlinejudge.submission.domain.SubmissionAnalysis;
import com.onlinejudge.submission.persistence.SubmissionAnalysisRepository;
import com.onlinejudge.submission.persistence.SubmissionHistoryProjection;
import com.onlinejudge.submission.persistence.SubmissionRepository;
import com.onlinejudge.leaderboard.persistence.ProblemSubmissionStatsProjection;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.*;
import org.springframework.data.repository.query.FluentQuery;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

class AiQualityOverviewServiceTest {

    private final FakeAssignmentRepository assignmentRepository = new FakeAssignmentRepository();
    private final FakeSubmissionRepository submissionRepository = new FakeSubmissionRepository();
    private final FakeSubmissionAnalysisRepository analysisRepository = new FakeSubmissionAnalysisRepository();
    private final FakeTeacherDiagnosisCorrectionRepository correctionRepository = new FakeTeacherDiagnosisCorrectionRepository();
    private final DiagnosisTaxonomy taxonomy = new DiagnosisTaxonomy();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AiQualityOverviewService service = new AiQualityOverviewService(
            assignmentRepository,
            submissionRepository,
            analysisRepository,
            correctionRepository,
            new DiagnosisReportReader(objectMapper, taxonomy),
            taxonomy
    );

    @Test
    void summarizesAiQualitySignalsForAssignment() {
        assignmentRepository.items.put(7L, Assignment.builder().id(7L).title("作业").build());
        submissionRepository.items.add(submission(11L, 7L));
        submissionRepository.items.add(submission(12L, 7L));
        analysisRepository.save(analysis(11L, 0.55, "LOW", "[\"BOUNDARY_CONDITION\"]", "[\"OFF_BY_ONE\"]"));
        analysisRepository.save(analysis(12L, 0.82, "HIGH", "[\"IO_FORMAT\"]", "[\"INPUT_PARSING\"]"));
        correctionRepository.saved.add(correction(1L, 7L, 11L, "BOUNDARY_CONDITION", "OFF_BY_ONE", "IO_FORMAT", "INPUT_PARSING", true));

        var overview = service.buildOverview(7L);

        assertThat(overview.getAnalyzedSubmissionCount()).isEqualTo(2);
        assertThat(overview.getCorrectionCount()).isEqualTo(1);
        assertThat(overview.getEvalCandidateCount()).isEqualTo(1);
        assertThat(overview.getLowConfidenceCount()).isEqualTo(1);
        assertThat(overview.getHighLeakRiskCount()).isEqualTo(1);
        assertThat(overview.getCorrectionRate()).isEqualTo(50.0);
        assertThat(overview.getSummary()).contains("高泄题风险");
        assertThat(overview.getCorrectedTags()).first()
                .satisfies(tag -> {
                    assertThat(tag.getOriginalTag()).isEqualTo("OFF_BY_ONE");
                    assertThat(tag.getCorrectedTag()).isEqualTo("INPUT_PARSING");
                    assertThat(tag.getOriginalLabel()).isEqualTo("差一位错误");
                    assertThat(tag.getCorrectedLabel()).isEqualTo("输入读取理解");
                });
    }

    private Submission submission(Long id, Long assignmentId) {
        return Submission.builder()
                .id(id)
                .assignmentId(assignmentId)
                .problemId(101L)
                .languageId(71)
                .sourceCode("print(1)")
                .verdict(Submission.Verdict.WRONG_ANSWER)
                .submittedAt(LocalDateTime.of(2026, 5, 18, 10, 0))
                .build();
    }

    private SubmissionAnalysis analysis(Long submissionId, double confidence, String leakRisk, String issueTags, String fineTags) {
        return SubmissionAnalysis.builder()
                .submissionId(submissionId)
                .analysisSource("TEST")
                .scenario("WA")
                .headline("诊断")
                .summary("summary")
                .reportMarkdown("markdown")
                .reportJson("""
                        {
                          "issueTags": %s,
                          "fineGrainedTags": %s,
                          "confidence": %s,
                          "answerLeakRisk": "%s"
                        }
                        """.formatted(issueTags, fineTags, confidence, leakRisk))
                .build();
    }

    private TeacherDiagnosisCorrection correction(Long id,
                                                  Long assignmentId,
                                                  Long submissionId,
                                                  String originalIssue,
                                                  String originalFine,
                                                  String correctedIssue,
                                                  String correctedFine,
                                                  boolean evalCandidate) {
        return TeacherDiagnosisCorrection.builder()
                .id(id)
                .assignmentId(assignmentId)
                .submissionId(submissionId)
                .originalIssueTag(originalIssue)
                .originalFineGrainedTag(originalFine)
                .correctedIssueTag(correctedIssue)
                .correctedFineGrainedTag(correctedFine)
                .evalCandidate(evalCandidate)
                .correctedAt(LocalDateTime.of(2026, 5, 18, 11, 0))
                .build();
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
        private final List<Submission> items = new ArrayList<>();

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
            return items.stream()
                    .filter(item -> Objects.equals(item.getAssignmentId(), assignmentId))
                    .toList();
        }

        @Override
        public List<Submission> findByAssignmentIdIn(Collection<Long> assignmentIds) {
            return items.stream()
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
                    .toList();
        }
    }

    private abstract static class UnsupportedJpaRepository<T, ID> {
        public List<T> findAll() { throw unsupported(); }
        public List<T> findAllById(Iterable<ID> ids) { throw unsupported(); }
        public <S extends T> S save(S entity) { throw unsupported(); }
        public <S extends T> List<S> saveAll(Iterable<S> entities) { throw unsupported(); }
        public Optional<T> findById(ID id) { throw unsupported(); }
        public boolean existsById(ID id) { throw unsupported(); }
        public long count() { throw unsupported(); }
        public void deleteById(ID id) { throw unsupported(); }
        public void delete(T entity) { throw unsupported(); }
        public void deleteAllById(Iterable<? extends ID> ids) { throw unsupported(); }
        public void deleteAll(Iterable<? extends T> entities) { throw unsupported(); }
        public void deleteAll() { throw unsupported(); }
        public void flush() { throw unsupported(); }
        public <S extends T> S saveAndFlush(S entity) { throw unsupported(); }
        public <S extends T> List<S> saveAllAndFlush(Iterable<S> entities) { throw unsupported(); }
        public void deleteAllInBatch(Iterable<T> entities) { throw unsupported(); }
        public void deleteAllByIdInBatch(Iterable<ID> ids) { throw unsupported(); }
        public void deleteAllInBatch() { throw unsupported(); }
        public T getOne(ID id) { throw unsupported(); }
        public T getById(ID id) { throw unsupported(); }
        public T getReferenceById(ID id) { throw unsupported(); }
        public List<T> findAll(Sort sort) { throw unsupported(); }
        public Page<T> findAll(Pageable pageable) { throw unsupported(); }
        public <S extends T> Optional<S> findOne(Example<S> example) { throw unsupported(); }
        public <S extends T> List<S> findAll(Example<S> example) { throw unsupported(); }
        public <S extends T> List<S> findAll(Example<S> example, Sort sort) { throw unsupported(); }
        public <S extends T> Page<S> findAll(Example<S> example, Pageable pageable) { throw unsupported(); }
        public <S extends T> long count(Example<S> example) { throw unsupported(); }
        public <S extends T> boolean exists(Example<S> example) { throw unsupported(); }
        public <S extends T, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { throw unsupported(); }
        private static UnsupportedOperationException unsupported() {
            return new UnsupportedOperationException("Not used in this test");
        }
    }
}
