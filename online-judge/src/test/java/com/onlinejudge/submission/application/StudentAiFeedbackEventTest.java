package com.onlinejudge.submission.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlinejudge.submission.domain.StudentAiFeedback;
import com.onlinejudge.submission.domain.StudentAiFeedbackEvent;
import com.onlinejudge.submission.domain.Submission;
import com.onlinejudge.submission.dto.StudentAiFeedbackResponse;
import com.onlinejudge.submission.dto.SubmissionAnalysisResponse;
import com.onlinejudge.submission.persistence.StudentAiFeedbackEventRepository;
import com.onlinejudge.submission.persistence.StudentAiFeedbackRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class StudentAiFeedbackEventTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final SubmissionRepository submissionRepository = mock(SubmissionRepository.class);
    private final StudentAiFeedbackRepository feedbackRepository = mock(StudentAiFeedbackRepository.class);
    private final StudentAiFeedbackEventRepository eventRepository = mock(StudentAiFeedbackEventRepository.class);
    private final SubmissionAnalysisService submissionAnalysisService = mock(SubmissionAnalysisService.class);
    private final StudentAiFeedbackService service = new StudentAiFeedbackService(
            submissionRepository,
            feedbackRepository,
            eventRepository,
            submissionAnalysisService,
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

    @Test
    void generateAndStoreReusesFormalDiagnosisStudentView() {
        when(submissionRepository.findById(7L)).thenReturn(Optional.of(submission()));
        when(submissionAnalysisService.generateAndStoreAnalysisForSubmission(7L))
                .thenReturn(analysisWithStudentView());
        when(feedbackRepository.findBySubmissionId(7L)).thenReturn(Optional.empty());
        when(feedbackRepository.save(any(StudentAiFeedback.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventRepository.findTopBySubmissionIdAndEventTypeOrderByCreatedAtDesc(eq(7L), eq(StudentAiFeedbackEvent.EVENT_READY)))
                .thenReturn(Optional.empty());

        StudentAiFeedbackResponse response = service.generateAndStore(7L);

        assertThat(response.getStatus()).isEqualTo("READY");
        assertThat(response.getSource()).isEqualTo("MODEL");
        assertThat(response.getStudentReport().getBasicLayerText()).contains("模型摘要");
        assertThat(response.getStudentReport().getImprovementLayerText()).contains("补测");
        assertThat(response.getStudentReport().getNextActionText()).contains("手推");
    }

    @Test
    void generateAndStoreMarksFallbackAsFailedWhenFormalDiagnosisIsNotModel() {
        when(submissionRepository.findById(7L)).thenReturn(Optional.of(submission()));
        when(submissionAnalysisService.generateAndStoreAnalysisForSubmission(7L))
                .thenReturn(analysisWithStudentView("RULE_BASED_V1"));
        when(feedbackRepository.findBySubmissionId(7L)).thenReturn(Optional.empty());
        when(feedbackRepository.save(any(StudentAiFeedback.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventRepository.findTopBySubmissionIdAndEventTypeOrderByCreatedAtDesc(eq(7L), eq(StudentAiFeedbackEvent.EVENT_FAILED)))
                .thenReturn(Optional.empty());

        StudentAiFeedbackResponse response = service.generateAndStore(7L);

        assertThat(response.getStatus()).isEqualTo("FAILED");
        assertThat(response.getSource()).isEqualTo("RULE_FALLBACK");
        assertThat(response.getSafety().getBlockedReasons()).contains("AI_UNAVAILABLE");
    }

    @Test
    void generateAndStoreFailsClearlyWhenFormalDiagnosisHasNoStudentView() {
        when(submissionRepository.findById(7L)).thenReturn(Optional.of(submission()));
        when(submissionAnalysisService.generateAndStoreAnalysisForSubmission(7L))
                .thenReturn(SubmissionAnalysisResponse.builder().sourceType("MODEL_SCOPE_EXTERNAL_MODEL").build());
        when(feedbackRepository.findBySubmissionId(7L)).thenReturn(Optional.empty());
        when(feedbackRepository.save(any(StudentAiFeedback.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventRepository.findTopBySubmissionIdAndEventTypeOrderByCreatedAtDesc(eq(7L), eq(StudentAiFeedbackEvent.EVENT_FAILED)))
                .thenReturn(Optional.empty());

        StudentAiFeedbackResponse response = service.generateAndStore(7L);

        assertThat(response.getStatus()).isEqualTo("FAILED");
        assertThat(response.getSafety().getBlockedReasons()).contains("STUDENT_FEEDBACK_VIEW_MISSING");
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

    private SubmissionAnalysisResponse analysisWithStudentView() {
        return analysisWithStudentView("MODEL_SCOPE_EXTERNAL_MODEL");
    }

    private SubmissionAnalysisResponse analysisWithStudentView(String sourceType) {
        return SubmissionAnalysisResponse.builder()
                .sourceType(sourceType)
                .summary("模型摘要：代码使用了不可靠的局部选择，需要先定位主因。")
                .studentFeedbackView(SubmissionAnalysisResponse.StudentFeedbackView.builder()
                        .repairItems(List.of(SubmissionAnalysisResponse.FeedbackViewItem.builder()
                                .title("循环边界")
                                .body("基础层：这里主要是循环边界没有和题意对齐。")
                                .kind("basic")
                                .evidenceRefs(List.of("code:loop_range"))
                                .build()))
                        .improvementItems(List.of(SubmissionAnalysisResponse.FeedbackViewItem.builder()
                                .title("边界测试")
                                .body("提高层：修完后补测最小值和右端点附近的数据。")
                                .kind("improvement")
                                .evidenceRefs(List.of("code:loop_range"))
                                .build()))
                        .nextQuestion("先手推 n=1 时循环变量有没有出现。")
                        .evidenceRefs(List.of("code:loop_range"))
                        .build())
                .build();
    }
}
