package com.onlinejudge.submission.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlinejudge.problem.persistence.ProblemRepository;
import com.onlinejudge.submission.domain.StudentAiFeedback;
import com.onlinejudge.submission.domain.StudentAiFeedbackEvent;
import com.onlinejudge.submission.domain.Submission;
import com.onlinejudge.submission.dto.StudentAiFeedbackResponse;
import com.onlinejudge.submission.persistence.StudentAiFeedbackEventRepository;
import com.onlinejudge.submission.persistence.StudentAiFeedbackRepository;
import com.onlinejudge.submission.persistence.SubmissionCaseResultRepository;
import com.onlinejudge.submission.persistence.SubmissionRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StudentAiFeedbackEventTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SubmissionRepository submissionRepository = mock(SubmissionRepository.class);
    private final StudentAiFeedbackRepository feedbackRepository = mock(StudentAiFeedbackRepository.class);
    private final StudentAiFeedbackEventRepository eventRepository = mock(StudentAiFeedbackEventRepository.class);
    private final StudentAiFeedbackService service = new StudentAiFeedbackService(
            submissionRepository,
            mock(ProblemRepository.class),
            mock(SubmissionCaseResultRepository.class),
            feedbackRepository,
            eventRepository,
            mock(DiagnosisEvidencePackageBuilder.class),
            mock(RuleSignalAnalyzer.class),
            mock(AiReportService.class),
            objectMapper
    );

    @Test
    void recordsViewedEventWithSubmissionContext() throws Exception {
        Submission submission = submission();
        StudentAiFeedbackResponse response = readyFeedback();
        StudentAiFeedback entity = StudentAiFeedback.builder()
                .submissionId(7L)
                .status("READY")
                .source("MODEL")
                .feedbackJson(objectMapper.writeValueAsString(response))
                .build();
        when(submissionRepository.findById(7L)).thenReturn(Optional.of(submission));
        when(feedbackRepository.findBySubmissionId(7L)).thenReturn(Optional.of(entity));
        when(eventRepository.findTopBySubmissionIdAndEventTypeOrderByCreatedAtDesc(7L, StudentAiFeedbackEvent.EVENT_VIEWED))
                .thenReturn(Optional.empty());

        service.recordViewed(7L);

        ArgumentCaptor<StudentAiFeedbackEvent> captor = ArgumentCaptor.forClass(StudentAiFeedbackEvent.class);
        verify(eventRepository).save(captor.capture());
        StudentAiFeedbackEvent event = captor.getValue();
        assertThat(event.getEventType()).isEqualTo(StudentAiFeedbackEvent.EVENT_VIEWED);
        assertThat(event.getSubmissionId()).isEqualTo(7L);
        assertThat(event.getStudentProfileId()).isEqualTo(41L);
        assertThat(event.getAssignmentId()).isEqualTo(9L);
        assertThat(event.getProblemId()).isEqualTo(101L);
        assertThat(event.getFeedbackStatus()).isEqualTo("READY");
        assertThat(event.getFeedbackSource()).isEqualTo("MODEL");
        assertThat(event.getAnswerLeakRisk()).isEqualTo("LOW");
    }

    @Test
    void doesNotDuplicateViewedEventForSameSubmission() {
        when(submissionRepository.findById(7L)).thenReturn(Optional.of(submission()));
        when(eventRepository.findTopBySubmissionIdAndEventTypeOrderByCreatedAtDesc(7L, StudentAiFeedbackEvent.EVENT_VIEWED))
                .thenReturn(Optional.of(StudentAiFeedbackEvent.builder().id(1L).build()));

        service.recordViewed(7L);

        verify(eventRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private Submission submission() {
        return Submission.builder()
                .id(7L)
                .assignmentId(9L)
                .studentProfileId(41L)
                .problemId(101L)
                .languageId(71)
                .sourceCode("print(1)")
                .verdict(Submission.Verdict.WRONG_ANSWER)
                .build();
    }

    private StudentAiFeedbackResponse readyFeedback() {
        return StudentAiFeedbackResponse.builder()
                .submissionId(7L)
                .status("READY")
                .source("MODEL")
                .repairItems(List.of())
                .improvementItems(List.of())
                .nextQuestion("哪里先偏离？")
                .safety(StudentAiFeedbackResponse.Safety.builder()
                        .answerLeakRisk("LOW")
                        .blockedReasons(List.of())
                        .build())
                .evidenceRefs(List.of("judge:first_failed_case:1"))
                .build();
    }
}
