package com.onlinejudge.classroom.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlinejudge.classroom.domain.StudentRecommendationEvent;
import com.onlinejudge.classroom.dto.StudentRecommendationResponse;
import com.onlinejudge.classroom.persistence.StudentRecommendationEventRepository;
import com.onlinejudge.learning.diagnosis.DiagnosisReportReader;
import com.onlinejudge.submission.domain.Submission;
import com.onlinejudge.submission.domain.SubmissionAnalysis;
import com.onlinejudge.submission.persistence.SubmissionAnalysisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class StudentRecommendationEventService {

    public static final String EVENT_EXPOSED = "EXPOSED";
    public static final String EVENT_CLICKED = "CLICKED";
    public static final String EVENT_ENTERED_PROBLEM = "ENTERED_PROBLEM";
    public static final String EVENT_SUBMITTED = "SUBMITTED";

    private final StudentRecommendationEventRepository eventRepository;
    private final SubmissionAnalysisRepository submissionAnalysisRepository;
    private final DiagnosisReportReader diagnosisReportReader;
    private final ObjectMapper objectMapper;

    @Transactional
    public void recordExposure(Long studentProfileId,
                               StudentRecommendationResponse.RecommendationItem item) {
        if (studentProfileId == null || item == null || item.getRecommendationToken() == null || item.getRecommendationToken().isBlank()) {
            return;
        }
        if (eventRepository.findTopByRecommendationTokenAndEventTypeOrderByCreatedAtDesc(item.getRecommendationToken(), EVENT_EXPOSED).isPresent()) {
            return;
        }
        eventRepository.save(baseEvent(studentProfileId, item, EVENT_EXPOSED)
                .build());
    }

    @Transactional
    public void recordClick(Long studentProfileId, String recommendationToken) {
        recordSimpleEvent(studentProfileId, recommendationToken, EVENT_CLICKED);
    }

    @Transactional
    public void recordEnteredProblem(Long studentProfileId, String recommendationToken) {
        recordSimpleEvent(studentProfileId, recommendationToken, EVENT_ENTERED_PROBLEM);
    }

    private void recordSimpleEvent(Long studentProfileId, String recommendationToken, String eventType) {
        StudentRecommendationEvent exposure = findExposureOrNull(recommendationToken);
        if (exposure == null) {
            return;
        }
        eventRepository.save(StudentRecommendationEvent.builder()
                .recommendationToken(recommendationToken)
                .studentProfileId(studentProfileId == null ? exposure.getStudentProfileId() : studentProfileId)
                .type(exposure.getType())
                .assignmentId(exposure.getAssignmentId())
                .problemId(exposure.getProblemId())
                .focusAbility(exposure.getFocusAbility())
                .focusTags(exposure.getFocusTags())
                .strategy(exposure.getStrategy())
                .learningHypothesis(exposure.getLearningHypothesis())
                .expectedCompletionSignal(exposure.getExpectedCompletionSignal())
                .riskLevel(exposure.getRiskLevel())
                .fallbackAction(exposure.getFallbackAction())
                .eventType(eventType)
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Transactional
    public void recordSubmission(Submission submission, String recommendationToken) {
        if (submission == null || recommendationToken == null || recommendationToken.isBlank()) {
            return;
        }
        StudentRecommendationEvent exposure = findExposureOrNull(recommendationToken);
        if (exposure == null) {
            return;
        }
        SubmissionAnalysis analysis = submissionAnalysisRepository.findBySubmissionId(submission.getId()).orElse(null);
        eventRepository.save(StudentRecommendationEvent.builder()
                .recommendationToken(recommendationToken)
                .studentProfileId(submission.getStudentProfileId() == null ? exposure.getStudentProfileId() : submission.getStudentProfileId())
                .type(exposure.getType())
                .assignmentId(submission.getAssignmentId() == null ? exposure.getAssignmentId() : submission.getAssignmentId())
                .problemId(exposure.getProblemId() == null ? submission.getProblemId() : exposure.getProblemId())
                .focusAbility(exposure.getFocusAbility())
                .focusTags(exposure.getFocusTags())
                .strategy(exposure.getStrategy())
                .learningHypothesis(exposure.getLearningHypothesis())
                .expectedCompletionSignal(exposure.getExpectedCompletionSignal())
                .riskLevel(exposure.getRiskLevel())
                .fallbackAction(exposure.getFallbackAction())
                .eventType(EVENT_SUBMITTED)
                .followupSubmissionId(submission.getId())
                .followupVerdict(submission.getVerdict() == null ? null : submission.getVerdict().name())
                .followupIssueTag(first(diagnosisReportReader.issueTags(analysis)))
                .followupFineGrainedTag(first(diagnosisReportReader.fineGrainedTags(analysis)))
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Transactional
    public void backfillSubmissionAnalysis(Submission submission, SubmissionAnalysis analysis) {
        if (submission == null || submission.getId() == null || analysis == null) {
            return;
        }
        List<StudentRecommendationEvent> events = eventRepository
                .findByFollowupSubmissionIdAndEventTypeOrderByCreatedAtDesc(submission.getId(), EVENT_SUBMITTED);
        if (events.isEmpty()) {
            return;
        }
        String verdict = submission.getVerdict() == null ? null : submission.getVerdict().name();
        String issueTag = first(diagnosisReportReader.issueTags(analysis));
        String fineGrainedTag = first(diagnosisReportReader.fineGrainedTags(analysis));
        events.forEach(event -> {
            boolean changed = false;
            if (!Objects.equals(event.getFollowupVerdict(), verdict)) {
                event.setFollowupVerdict(verdict);
                changed = true;
            }
            if (!Objects.equals(event.getFollowupIssueTag(), issueTag)) {
                event.setFollowupIssueTag(issueTag);
                changed = true;
            }
            if (!Objects.equals(event.getFollowupFineGrainedTag(), fineGrainedTag)) {
                event.setFollowupFineGrainedTag(fineGrainedTag);
                changed = true;
            }
            if (changed) {
                eventRepository.save(event);
            }
        });
    }

    public List<StudentRecommendationEvent> findEvents(Long studentProfileId) {
        if (studentProfileId == null) {
            return List.of();
        }
        return eventRepository.findByStudentProfileIdOrderByCreatedAtDesc(studentProfileId);
    }

    public String tokenFor(Long studentProfileId,
                           StudentRecommendationResponse.RecommendationItem item) {
        if (item == null) {
            return "";
        }
        return String.join(":",
                "rec",
                String.valueOf(studentProfileId == null ? 0 : studentProfileId),
                nullToBlank(item.getType()),
                String.valueOf(item.getProblemId() == null ? 0 : item.getProblemId()),
                Integer.toHexString(Objects.hash(
                        nullToBlank(item.getFocusAbility()),
                        item.getFocusTags() == null ? List.of() : item.getFocusTags(),
                        item.getEvidenceProblemIds() == null ? List.of() : item.getEvidenceProblemIds(),
                        item.getAssignmentId() == null ? 0 : item.getAssignmentId(),
                        nullToBlank(item.getStrategy()),
                        nullToBlank(item.getRiskLevel()),
                        nullToBlank(item.getExpectedCompletionSignal())
                ))
        );
    }

    private StudentRecommendationEvent findExposureOrNull(String recommendationToken) {
        if (recommendationToken == null || recommendationToken.isBlank()) {
            return null;
        }
        return eventRepository.findTopByRecommendationTokenAndEventTypeOrderByCreatedAtDesc(recommendationToken, EVENT_EXPOSED)
                .orElse(null);
    }

    private StudentRecommendationEvent.StudentRecommendationEventBuilder baseEvent(Long studentProfileId,
                                                                                   StudentRecommendationResponse.RecommendationItem item,
                                                                                   String eventType) {
        return StudentRecommendationEvent.builder()
                .recommendationToken(item.getRecommendationToken())
                .studentProfileId(studentProfileId)
                .type(item.getType())
                .assignmentId(item.getAssignmentId())
                .problemId(item.getProblemId())
                .focusAbility(item.getFocusAbility())
                .focusTags(toJson(item.getFocusTags()))
                .strategy(item.getStrategy())
                .learningHypothesis(item.getLearningHypothesis())
                .expectedCompletionSignal(item.getExpectedCompletionSignal())
                .riskLevel(item.getRiskLevel())
                .fallbackAction(item.getFallbackAction())
                .eventType(eventType)
                .createdAt(LocalDateTime.now());
    }

    private String toJson(List<String> values) {
        try {
            List<String> normalized = values == null ? List.of() : values.stream()
                    .filter(value -> value != null && !value.isBlank())
                    .map(String::trim)
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new))
                    .stream()
                    .toList();
            return objectMapper.writeValueAsString(normalized);
        } catch (JsonProcessingException exception) {
            return "[]";
        }
    }

    private String first(List<String> values) {
        return values == null || values.isEmpty() ? null : values.get(0);
    }

    private String nullToBlank(String value) {
        return value == null ? "" : value;
    }
}
