package com.onlinejudge.submission.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlinejudge.problem.domain.Problem;
import com.onlinejudge.problem.persistence.ProblemRepository;
import com.onlinejudge.submission.domain.StudentAiFeedback;
import com.onlinejudge.submission.domain.StudentAiFeedbackEvent;
import com.onlinejudge.submission.domain.Submission;
import com.onlinejudge.submission.domain.SubmissionCaseResult;
import com.onlinejudge.submission.dto.StudentAiFeedbackLookupResponse;
import com.onlinejudge.submission.dto.StudentAiFeedbackResponse;
import com.onlinejudge.submission.persistence.StudentAiFeedbackEventRepository;
import com.onlinejudge.submission.persistence.StudentAiFeedbackRepository;
import com.onlinejudge.submission.persistence.SubmissionCaseResultRepository;
import com.onlinejudge.submission.persistence.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudentAiFeedbackService {

    private static final String SOURCE_MODEL = "MODEL";
    private static final String SOURCE_AI_UNAVAILABLE = "AI_UNAVAILABLE";

    private final SubmissionRepository submissionRepository;
    private final ProblemRepository problemRepository;
    private final SubmissionCaseResultRepository submissionCaseResultRepository;
    private final StudentAiFeedbackRepository studentAiFeedbackRepository;
    private final StudentAiFeedbackEventRepository studentAiFeedbackEventRepository;
    private final AiReportService aiReportService;
    private final DiagnosisEvidencePackageBuilder evidencePackageBuilder;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public StudentAiFeedbackLookupResponse getLookup(Long submissionId) {
        ensureSubmissionExists(submissionId);
        return studentAiFeedbackRepository.findBySubmissionId(submissionId)
                .map(this::toLookup)
                .orElse(StudentAiFeedbackLookupResponse.builder()
                        .status("NOT_REQUESTED")
                        .feedback(notRequested(submissionId))
                        .build());
    }

    @Transactional
    public StudentAiFeedbackLookupResponse markGenerating(Long submissionId) {
        ensureSubmissionExists(submissionId);
        StudentAiFeedback entity = studentAiFeedbackRepository.findBySubmissionId(submissionId)
                .orElse(StudentAiFeedback.builder()
                        .submissionId(submissionId)
                        .source(SOURCE_MODEL)
                        .build());
        if ("READY".equals(entity.getStatus()) || "GENERATING".equals(entity.getStatus())) {
            return toLookup(entity);
        }
        StudentAiFeedbackResponse feedback = StudentAiFeedbackResponse.builder()
                .submissionId(submissionId)
                .status("GENERATING")
                .source(SOURCE_AI_UNAVAILABLE)
                .generatedAt(LocalDateTime.now())
                .repairItems(List.of())
                .improvementItems(List.of())
                .safety(StudentAiFeedbackResponse.Safety.builder()
                        .answerLeakRisk("LOW")
                        .blockedReasons(List.of())
                        .build())
                .evidenceRefs(List.of())
                .build();
        entity.setStatus("GENERATING");
        entity.setSource(SOURCE_AI_UNAVAILABLE);
        entity.setFailureReason(null);
        entity.setFeedbackJson(serialize(feedback));
        return toLookup(studentAiFeedbackRepository.save(entity));
    }

    @Transactional
    public StudentAiFeedbackResponse markFailed(Long submissionId, String reason) {
        ensureSubmissionExists(submissionId);
        StudentAiFeedbackResponse feedback = StudentAiFeedbackResponse.builder()
                .submissionId(submissionId)
                .status("FAILED")
                .source(SOURCE_AI_UNAVAILABLE)
                .generatedAt(LocalDateTime.now())
                .repairItems(List.of())
                .improvementItems(List.of())
                .safety(StudentAiFeedbackResponse.Safety.builder()
                        .answerLeakRisk("LOW")
                        .blockedReasons(reason == null || reason.isBlank() ? List.of("GENERATION_FAILED") : List.of(reason))
                        .build())
                .evidenceRefs(List.of())
                .build();
        StudentAiFeedback entity = studentAiFeedbackRepository.findBySubmissionId(submissionId)
                .orElse(StudentAiFeedback.builder()
                        .submissionId(submissionId)
                        .source(SOURCE_MODEL)
                        .build());
        entity.setStatus("FAILED");
        entity.setSource(SOURCE_AI_UNAVAILABLE);
        entity.setFailureReason(failureReason(feedback));
        entity.setFeedbackJson(serialize(feedback));
        StudentAiFeedback saved = studentAiFeedbackRepository.save(entity);
        recordGenerationEvent(
                submissionRepository.findById(submissionId).orElse(null),
                saved,
                feedback
        );
        feedback.setGeneratedAt(saved.getGeneratedAt());
        return feedback;
    }

    @Transactional
    public StudentAiFeedbackResponse generateAndStore(Long submissionId) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new IllegalArgumentException("提交记录不存在: " + submissionId));
        Problem problem = problemRepository.findById(submission.getProblemId())
                .orElseThrow(() -> new IllegalArgumentException("题目不存在: " + submission.getProblemId()));
        List<SubmissionCaseResult> caseResults = submissionCaseResultRepository.findBySubmissionIdOrderByTestCaseNumberAsc(submissionId);
        DiagnosisEvidencePackage evidencePackage = evidencePackageBuilder.build(problem, submission, caseResults, null, null);
        StudentAiFeedbackResponse feedback = aiReportService.generateStudentAiFeedback(
                problem,
                submission,
                evidencePackage
        );
        return saveFeedback(submission, feedback == null ? failedFeedback(submissionId, "STUDENT_FEEDBACK_EMPTY") : feedback);
    }

    private StudentAiFeedbackResponse saveFeedback(Submission submission, StudentAiFeedbackResponse feedback) {
        Long submissionId = submission.getId();
        if (feedback.getGeneratedAt() == null) {
            feedback.setGeneratedAt(LocalDateTime.now());
        }
        StudentAiFeedback entity = studentAiFeedbackRepository.findBySubmissionId(submissionId)
                .orElse(StudentAiFeedback.builder()
                        .submissionId(submissionId)
                        .source(SOURCE_MODEL)
                        .build());
        entity.setStatus(feedback.getStatus());
        entity.setSource(hasText(feedback.getSource()) ? feedback.getSource() : SOURCE_AI_UNAVAILABLE);
        entity.setFeedbackJson(serialize(feedback));
        entity.setFailureReason(failureReason(feedback));
        StudentAiFeedback saved = studentAiFeedbackRepository.save(entity);
        recordGenerationEvent(submission, saved, feedback);
        feedback.setGeneratedAt(saved.getGeneratedAt());
        return feedback;
    }

    private StudentAiFeedbackResponse failedFeedback(Long submissionId, String reason) {
        return StudentAiFeedbackResponse.builder()
                .submissionId(submissionId)
                .status("FAILED")
                .source(SOURCE_AI_UNAVAILABLE)
                .generatedAt(LocalDateTime.now())
                .repairItems(List.of())
                .improvementItems(List.of())
                .safety(StudentAiFeedbackResponse.Safety.builder()
                        .answerLeakRisk("LOW")
                        .blockedReasons(List.of(reason))
                        .build())
                .evidenceRefs(List.of())
                .build();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    @Transactional
    public void recordViewed(Long submissionId) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new IllegalArgumentException("提交记录不存在: " + submissionId));
        StudentAiFeedback feedback = studentAiFeedbackRepository.findBySubmissionId(submissionId)
                .orElse(null);
        studentAiFeedbackEventRepository
                .findTopBySubmissionIdAndEventTypeOrderByCreatedAtDesc(submissionId, StudentAiFeedbackEvent.EVENT_VIEWED)
                .ifPresentOrElse(
                        ignored -> {
                        },
                        () -> studentAiFeedbackEventRepository.save(eventFor(
                                submission,
                                feedback,
                                feedback == null ? null : deserialize(feedback),
                                StudentAiFeedbackEvent.EVENT_VIEWED
                        ))
                );
    }

    private StudentAiFeedbackLookupResponse toLookup(StudentAiFeedback entity) {
        StudentAiFeedbackResponse feedback = deserialize(entity);
        if (feedback == null) {
            feedback = StudentAiFeedbackResponse.builder()
                    .submissionId(entity.getSubmissionId())
                    .status(statusOrFailed(entity.getStatus()))
                    .source(SOURCE_AI_UNAVAILABLE)
                    .generatedAt(entity.getGeneratedAt())
                    .repairItems(List.of())
                    .improvementItems(List.of())
                    .safety(StudentAiFeedbackResponse.Safety.builder()
                            .answerLeakRisk("LOW")
                            .blockedReasons(entity.getFailureReason() == null || entity.getFailureReason().isBlank()
                                    ? List.of("STRUCTURED_OUTPUT_INVALID")
                                    : List.of(entity.getFailureReason()))
                            .build())
                    .evidenceRefs(List.of())
                    .build();
        }
        if (feedback.getGeneratedAt() == null) {
            feedback.setGeneratedAt(entity.getGeneratedAt());
        }
        return StudentAiFeedbackLookupResponse.builder()
                .status(statusOrFailed(entity.getStatus()))
                .feedback(feedback)
                .build();
    }

    private StudentAiFeedbackResponse deserialize(StudentAiFeedback entity) {
        if (entity.getFeedbackJson() == null || entity.getFeedbackJson().isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(entity.getFeedbackJson(), StudentAiFeedbackResponse.class);
        } catch (JsonProcessingException exception) {
            log.warn("Student AI feedback JSON parse failed. submissionId={}, error={}",
                    entity.getSubmissionId(),
                    exception.getMessage());
            return null;
        }
    }

    private String serialize(StudentAiFeedbackResponse feedback) {
        try {
            return objectMapper.writeValueAsString(feedback);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("学生 AI 反馈序列化失败", exception);
        }
    }

    private StudentAiFeedbackResponse notRequested(Long submissionId) {
        return StudentAiFeedbackResponse.builder()
                .submissionId(submissionId)
                .status("NOT_REQUESTED")
                .source(SOURCE_MODEL)
                .repairItems(List.of())
                .improvementItems(List.of())
                .safety(StudentAiFeedbackResponse.Safety.builder()
                        .answerLeakRisk("LOW")
                        .blockedReasons(List.of())
                        .build())
                .evidenceRefs(List.of())
                .build();
    }

    private void ensureSubmissionExists(Long submissionId) {
        submissionRepository.findById(submissionId)
                .orElseThrow(() -> new IllegalArgumentException("提交记录不存在: " + submissionId));
    }

    private String statusOrFailed(String status) {
        return status == null || status.isBlank() ? "FAILED" : status;
    }

    private String failureReason(StudentAiFeedbackResponse feedback) {
        if (feedback == null || "READY".equals(feedback.getStatus())) {
            return null;
        }
        if (feedback.getSafety() == null || feedback.getSafety().getBlockedReasons() == null) {
            return feedback == null ? "UNKNOWN" : feedback.getStatus();
        }
        return String.join(",", feedback.getSafety().getBlockedReasons());
    }

    private void recordGenerationEvent(Submission submission,
                                       StudentAiFeedback entity,
                                       StudentAiFeedbackResponse feedback) {
        if (submission == null || entity == null || feedback == null || feedback.getStatus() == null) {
            return;
        }
        String eventType = "READY".equals(feedback.getStatus())
                ? StudentAiFeedbackEvent.EVENT_READY
                : StudentAiFeedbackEvent.EVENT_FAILED;
        if (!StudentAiFeedbackEvent.EVENT_READY.equals(eventType) && "GENERATING".equals(feedback.getStatus())) {
            return;
        }
        if (studentAiFeedbackEventRepository
                .findTopBySubmissionIdAndEventTypeOrderByCreatedAtDesc(submission.getId(), eventType)
                .isPresent()) {
            return;
        }
        studentAiFeedbackEventRepository.save(eventFor(submission, entity, feedback, eventType));
    }

    private StudentAiFeedbackEvent eventFor(Submission submission,
                                            StudentAiFeedback entity,
                                            StudentAiFeedbackResponse feedback,
                                            String eventType) {
        return StudentAiFeedbackEvent.builder()
                .submissionId(submission.getId())
                .studentProfileId(submission.getStudentProfileId())
                .assignmentId(submission.getAssignmentId())
                .problemId(submission.getProblemId())
                .eventType(eventType)
                .feedbackStatus(feedback == null ? entityStatus(entity) : feedback.getStatus())
                .feedbackSource(feedback == null ? entitySource(entity) : feedback.getSource())
                .answerLeakRisk(feedback == null || feedback.getSafety() == null
                        ? null
                        : feedback.getSafety().getAnswerLeakRisk())
                .failureReason(entity == null ? failureReason(feedback) : entity.getFailureReason())
                .createdAt(LocalDateTime.now())
                .build();
    }

    private String entityStatus(StudentAiFeedback entity) {
        return entity == null ? null : entity.getStatus();
    }

    private String entitySource(StudentAiFeedback entity) {
        return entity == null ? SOURCE_MODEL : entity.getSource();
    }
}
