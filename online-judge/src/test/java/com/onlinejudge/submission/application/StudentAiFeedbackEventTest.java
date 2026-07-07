package com.onlinejudge.submission.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlinejudge.problem.domain.Problem;
import com.onlinejudge.problem.persistence.ProblemRepository;
import com.onlinejudge.submission.domain.StudentAiFeedback;
import com.onlinejudge.submission.domain.StudentAiFeedbackEvent;
import com.onlinejudge.submission.domain.Submission;
import com.onlinejudge.submission.domain.SubmissionCaseResult;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class StudentAiFeedbackEventTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final SubmissionRepository submissionRepository = mock(SubmissionRepository.class);
    private final ProblemRepository problemRepository = mock(ProblemRepository.class);
    private final SubmissionCaseResultRepository caseResultRepository = mock(SubmissionCaseResultRepository.class);
    private final StudentAiFeedbackRepository feedbackRepository = mock(StudentAiFeedbackRepository.class);
    private final StudentAiFeedbackEventRepository eventRepository = mock(StudentAiFeedbackEventRepository.class);
    private final AiReportService aiReportService = mock(AiReportService.class);
    private final DiagnosisEvidencePackageBuilder evidencePackageBuilder = new DiagnosisEvidencePackageBuilder();
    private final StudentAiFeedbackService service = new StudentAiFeedbackService(
            submissionRepository,
            problemRepository,
            caseResultRepository,
            feedbackRepository,
            eventRepository,
            aiReportService,
            evidencePackageBuilder,
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
    void generateAndStoreUsesFastStudentFeedbackModel() {
        when(submissionRepository.findById(7L)).thenReturn(Optional.of(submission()));
        when(problemRepository.findById(101L)).thenReturn(Optional.of(problem()));
        when(caseResultRepository.findBySubmissionIdOrderByTestCaseNumberAsc(7L)).thenReturn(caseResults());
        when(aiReportService.generateStudentAiFeedback(
                any(Problem.class),
                any(Submission.class),
                any(DiagnosisEvidencePackage.class)
        )).thenReturn(fastReadyFeedback());
        when(feedbackRepository.findBySubmissionId(7L)).thenReturn(Optional.empty());
        when(feedbackRepository.save(any(StudentAiFeedback.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventRepository.findTopBySubmissionIdAndEventTypeOrderByCreatedAtDesc(eq(7L), eq(StudentAiFeedbackEvent.EVENT_READY)))
                .thenReturn(Optional.empty());

        StudentAiFeedbackResponse response = service.generateAndStore(7L);

        assertThat(response.getStatus()).isEqualTo("READY");
        assertThat(response.getSource()).isEqualTo("MODEL");
        assertThat(response.getStudentReport().getBasicLayerText()).contains("输入读取");
        assertThat(response.getStudentReport().getImprovementLayerText()).contains("边界样例");
        assertThat(response.getStudentReport().getNextActionText()).contains("手推");
        verify(aiReportService).generateStudentAiFeedback(
                any(Problem.class),
                any(Submission.class),
                any(DiagnosisEvidencePackage.class)
        );
    }

    @Test
    void generateAndStoreStoresFastFeedbackFailureClearly() {
        when(submissionRepository.findById(7L)).thenReturn(Optional.of(submission()));
        when(problemRepository.findById(101L)).thenReturn(Optional.of(problem()));
        when(caseResultRepository.findBySubmissionIdOrderByTestCaseNumberAsc(7L)).thenReturn(caseResults());
        when(aiReportService.generateStudentAiFeedback(
                any(Problem.class),
                any(Submission.class),
                any(DiagnosisEvidencePackage.class)
        )).thenReturn(failedFastFeedback("AI_UNAVAILABLE"));
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
    void generateAndStoreFailsClearlyWhenFastFeedbackIsEmpty() {
        when(submissionRepository.findById(7L)).thenReturn(Optional.of(submission()));
        when(problemRepository.findById(101L)).thenReturn(Optional.of(problem()));
        when(caseResultRepository.findBySubmissionIdOrderByTestCaseNumberAsc(7L)).thenReturn(caseResults());
        when(aiReportService.generateStudentAiFeedback(
                any(Problem.class),
                any(Submission.class),
                any(DiagnosisEvidencePackage.class)
        )).thenReturn(null);
        when(feedbackRepository.findBySubmissionId(7L)).thenReturn(Optional.empty());
        when(feedbackRepository.save(any(StudentAiFeedback.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventRepository.findTopBySubmissionIdAndEventTypeOrderByCreatedAtDesc(eq(7L), eq(StudentAiFeedbackEvent.EVENT_FAILED)))
                .thenReturn(Optional.empty());

        StudentAiFeedbackResponse response = service.generateAndStore(7L);

        assertThat(response.getStatus()).isEqualTo("FAILED");
        assertThat(response.getSafety().getBlockedReasons()).contains("STUDENT_FEEDBACK_EMPTY");
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

    private Problem problem() {
        return Problem.builder()
                .id(101L)
                .title("两数求和")
                .description("给定两个整数 a 和 b，输出它们的和。")
                .difficulty(Problem.Difficulty.EASY)
                .timeLimit(1000)
                .memoryLimit(128 * 1024)
                .build();
    }

    private List<SubmissionCaseResult> caseResults() {
        return List.of(SubmissionCaseResult.builder()
                .submissionId(7L)
                .testCaseNumber(1)
                .passed(false)
                .hidden(false)
                .inputSnapshot("3 5")
                .actualOutput("1")
                .expectedOutput("8")
                .executionTime(0.01)
                .memoryUsed(1024)
                .build());
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

    private StudentAiFeedbackResponse fastReadyFeedback() {
        return StudentAiFeedbackResponse.builder()
                .submissionId(7L)
                .status("READY")
                .source("MODEL")
                .studentReport(StudentAiFeedbackResponse.StudentReport.builder()
                        .basicLayerText("基础层：这里主要是输入读取和题面结构没对齐。")
                        .improvementLayerText("提高层：修完后补测边界样例，确认不是只适配样例。")
                        .nextActionText("先手推第一行输入每个数分别被哪句代码读走。")
                        .build())
                .repairItems(List.of())
                .improvementItems(List.of())
                .nextQuestion("第一行输入里第二个数在哪里被读取？")
                .safety(StudentAiFeedbackResponse.Safety.builder()
                        .answerLeakRisk("LOW")
                        .blockedReasons(List.of())
                        .build())
                .evidenceRefs(List.of("judge:first_failed_case:1"))
                .build();
    }

    private StudentAiFeedbackResponse failedFastFeedback(String reason) {
        return StudentAiFeedbackResponse.builder()
                .submissionId(7L)
                .status("FAILED")
                .source("RULE_FALLBACK")
                .repairItems(List.of())
                .improvementItems(List.of())
                .safety(StudentAiFeedbackResponse.Safety.builder()
                        .answerLeakRisk("LOW")
                        .blockedReasons(List.of(reason))
                        .build())
                .evidenceRefs(List.of())
                .build();
    }
}
