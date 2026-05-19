package com.onlinejudge.classroom.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlinejudge.classroom.domain.StudentRecommendationEvent;
import com.onlinejudge.classroom.persistence.StudentRecommendationEventRepository;
import com.onlinejudge.submission.domain.Submission;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.*;
import org.springframework.data.repository.query.FluentQuery;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

class RecommendationEffectivenessServiceTest {

    private final FakeStudentRecommendationEventRepository eventRepository = new FakeStudentRecommendationEventRepository();
    private final RecommendationEffectivenessService service = new RecommendationEffectivenessService(eventRepository, new ObjectMapper());

    @Test
    void summarizesRecommendationFunnelAndFollowupOutcomes() {
        eventRepository.items.add(event("rec-redo", "REDO", StudentRecommendationEventService.EVENT_EXPOSED, "[\"OFF_BY_ONE\"]", null, null));
        eventRepository.items.add(event("rec-redo", "REDO", StudentRecommendationEventService.EVENT_CLICKED, "[\"OFF_BY_ONE\"]", null, null));
        eventRepository.items.add(event("rec-redo", "REDO", StudentRecommendationEventService.EVENT_ENTERED_PROBLEM, "[\"OFF_BY_ONE\"]", null, null));
        eventRepository.items.add(event("rec-redo", "REDO", StudentRecommendationEventService.EVENT_SUBMITTED, "[\"OFF_BY_ONE\"]", Submission.Verdict.WRONG_ANSWER.name(), "OFF_BY_ONE"));

        eventRepository.items.add(event("rec-next", "NEXT_PROBLEM", StudentRecommendationEventService.EVENT_EXPOSED, "[\"INPUT_PARSING\"]", null, null));
        eventRepository.items.add(event("rec-next", "NEXT_PROBLEM", StudentRecommendationEventService.EVENT_CLICKED, "[\"INPUT_PARSING\"]", null, null));
        eventRepository.items.add(event("rec-next", "NEXT_PROBLEM", StudentRecommendationEventService.EVENT_SUBMITTED, "[\"INPUT_PARSING\"]", Submission.Verdict.ACCEPTED.name(), null));

        eventRepository.items.add(event("rec-review", "REVIEW", StudentRecommendationEventService.EVENT_EXPOSED, "[\"COMPLEXITY\"]", null, null));
        eventRepository.items.add(event("rec-review", "REVIEW", StudentRecommendationEventService.EVENT_CLICKED, "[\"COMPLEXITY\"]", null, null));

        var overview = service.buildOverview();

        assertThat(overview.getRecentEventCount()).isEqualTo(9);
        assertThat(overview.getUniqueRecommendationCount()).isEqualTo(3);
        assertThat(overview.getExposureCount()).isEqualTo(3);
        assertThat(overview.getClickCount()).isEqualTo(3);
        assertThat(overview.getEnteredProblemCount()).isEqualTo(1);
        assertThat(overview.getFollowupSubmissionCount()).isEqualTo(2);
        assertThat(overview.getAcceptedFollowupCount()).isEqualTo(1);
        assertThat(overview.getSameFocusIssueCount()).isEqualTo(1);
        assertThat(overview.getClickedWithoutSubmissionCount()).isEqualTo(1);
        assertThat(overview.getClickThroughRate()).isEqualTo(100.0);
        assertThat(overview.getFollowupSubmissionRate()).isEqualTo(66.7);
        assertThat(overview.getAcceptedFollowupRate()).isEqualTo(50.0);
        assertThat(overview.getSameFocusIssueRate()).isEqualTo(50.0);
        assertThat(overview.getSummary()).contains("推荐后已有 1 次后续提交通过");
        assertThat(overview.getByType()).extracting("key").contains("REDO", "NEXT_PROBLEM", "REVIEW");
        assertThat(overview.getFocusTags()).extracting("key").contains("OFF_BY_ONE", "INPUT_PARSING", "COMPLEXITY");
        assertThat(overview.getFocusTags().stream()
                .filter(item -> "OFF_BY_ONE".equals(item.getKey()))
                .findFirst()
                .orElseThrow()
                .getSameFocusIssueCount()).isEqualTo(1);
    }

    @Test
    void parsesLegacyFocusTagsAndReportsClickedWithoutSubmission() {
        eventRepository.items.add(event("rec-legacy", "REVIEW", StudentRecommendationEventService.EVENT_EXPOSED, "OFF_BY_ONE,BOUNDARY", null, null));
        eventRepository.items.add(event("rec-legacy", "REVIEW", StudentRecommendationEventService.EVENT_ENTERED_PROBLEM, "OFF_BY_ONE,BOUNDARY", null, null));

        var overview = service.buildOverview();

        assertThat(overview.getFollowupSubmissionCount()).isZero();
        assertThat(overview.getClickedWithoutSubmissionCount()).isEqualTo(1);
        assertThat(overview.getSummary()).contains("尚未提交");
        assertThat(overview.getFocusTags()).extracting("key").contains("OFF_BY_ONE", "BOUNDARY");
    }

    private StudentRecommendationEvent event(String token,
                                             String type,
                                             String eventType,
                                             String focusTags,
                                             String verdict,
                                             String issueTag) {
        return StudentRecommendationEvent.builder()
                .recommendationToken(token)
                .studentProfileId(41L)
                .type(type)
                .problemId(101L)
                .focusAbility("Boundary reasoning")
                .focusTags(focusTags)
                .eventType(eventType)
                .followupSubmissionId(StudentRecommendationEventService.EVENT_SUBMITTED.equals(eventType) ? 9001L : null)
                .followupVerdict(verdict)
                .followupIssueTag(issueTag)
                .followupFineGrainedTag(issueTag)
                .createdAt(LocalDateTime.of(2026, 5, 19, 10, 0).plusMinutes(eventRepository.items.size()))
                .build();
    }

    private static class FakeStudentRecommendationEventRepository extends UnsupportedJpaRepository<StudentRecommendationEvent, Long>
            implements StudentRecommendationEventRepository {
        private final List<StudentRecommendationEvent> items = new ArrayList<>();

        @Override
        public List<StudentRecommendationEvent> findByStudentProfileIdOrderByCreatedAtDesc(Long studentProfileId) {
            return items.stream()
                    .filter(item -> Objects.equals(item.getStudentProfileId(), studentProfileId))
                    .sorted(Comparator.comparing(StudentRecommendationEvent::getCreatedAt, Comparator.nullsLast(LocalDateTime::compareTo)).reversed())
                    .toList();
        }

        @Override
        public List<StudentRecommendationEvent> findTop500ByOrderByCreatedAtDesc() {
            return items.stream()
                    .sorted(Comparator.comparing(StudentRecommendationEvent::getCreatedAt, Comparator.nullsLast(LocalDateTime::compareTo)).reversed())
                    .limit(500)
                    .toList();
        }

        @Override
        public List<StudentRecommendationEvent> findByFollowupSubmissionIdAndEventTypeOrderByCreatedAtDesc(Long followupSubmissionId, String eventType) {
            return items.stream()
                    .filter(item -> Objects.equals(item.getFollowupSubmissionId(), followupSubmissionId))
                    .filter(item -> Objects.equals(item.getEventType(), eventType))
                    .sorted(Comparator.comparing(StudentRecommendationEvent::getCreatedAt, Comparator.nullsLast(LocalDateTime::compareTo)).reversed())
                    .toList();
        }

        @Override
        public Optional<StudentRecommendationEvent> findTopByRecommendationTokenAndEventTypeOrderByCreatedAtDesc(String recommendationToken, String eventType) {
            return items.stream()
                    .filter(item -> Objects.equals(item.getRecommendationToken(), recommendationToken))
                    .filter(item -> Objects.equals(item.getEventType(), eventType))
                    .max(Comparator.comparing(StudentRecommendationEvent::getCreatedAt, Comparator.nullsLast(LocalDateTime::compareTo)));
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
