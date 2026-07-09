package com.onlinejudge.submission.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlinejudge.problem.domain.Problem;
import com.onlinejudge.problem.persistence.ProblemRepository;
import com.onlinejudge.submission.domain.StudentAiFeedback;
import com.onlinejudge.submission.domain.StudentAiFeedbackEvent;
import com.onlinejudge.submission.domain.Submission;
import com.onlinejudge.submission.domain.SubmissionCaseResult;
import com.onlinejudge.submission.dto.SubmissionAnalysisResponse;
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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudentAiFeedbackService {

    private static final String SOURCE_MODEL = "MODEL";
    private static final String SOURCE_AI_UNAVAILABLE = "AI_UNAVAILABLE";
    private static final Pattern CODE_LINE_REF = Pattern.compile("^code:line:(\\d+)$");
    private static final Pattern CODE_RANGE_REF = Pattern.compile("^code:range:(\\d+)-(\\d+)$");

    private final SubmissionRepository submissionRepository;
    private final ProblemRepository problemRepository;
    private final SubmissionCaseResultRepository submissionCaseResultRepository;
    private final StudentAiFeedbackRepository studentAiFeedbackRepository;
    private final StudentAiFeedbackEventRepository studentAiFeedbackEventRepository;
    private final SubmissionAnalysisService submissionAnalysisService;
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

    public StudentAiFeedbackResponse generateAndStore(Long submissionId) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new IllegalArgumentException("提交记录不存在: " + submissionId));
        Problem problem = problemRepository.findById(submission.getProblemId())
                .orElseThrow(() -> new IllegalArgumentException("题目不存在: " + submission.getProblemId()));
        List<SubmissionCaseResult> caseResults = submissionCaseResultRepository.findBySubmissionIdOrderByTestCaseNumberAsc(submissionId);
        SubmissionAnalysisResponse analysis = submissionAnalysisService.generateAndStoreAnalysis(problem, submission, caseResults);
        StudentAiFeedbackResponse feedback = toStudentAiFeedback(submission, analysis);
        return saveFeedback(submission, feedback);
    }

    private StudentAiFeedbackResponse toStudentAiFeedback(Submission submission, SubmissionAnalysisResponse analysis) {
        if (analysis == null || modelFailed(analysis)) {
            return failedFeedback(submission.getId(), "FULL_CHAIN_FAILED");
        }
        List<StudentAiFeedbackResponse.FeedbackItem> repairItems = repairItems(submission, analysis);
        List<StudentAiFeedbackResponse.FeedbackItem> improvementItems = improvementItems(submission, analysis);
        StudentAiFeedbackResponse.StudentReport report = studentReport(analysis, repairItems, improvementItems);
        if (!hasStudentReport(report) && repairItems.isEmpty() && improvementItems.isEmpty()) {
            return failedFeedback(submission.getId(), "FULL_CHAIN_FEEDBACK_EMPTY");
        }
        return StudentAiFeedbackResponse.builder()
                .submissionId(submission.getId())
                .status("READY")
                .source(SOURCE_MODEL)
                .generatedAt(LocalDateTime.now())
                .repairItems(repairItems)
                .improvementItems(improvementItems)
                .studentReport(report)
                .nextQuestion(nextQuestion(analysis))
                .safety(StudentAiFeedbackResponse.Safety.builder()
                        .answerLeakRisk(firstNonBlank(analysis.getAnswerLeakRisk(), nextActionRisk(analysis), "LOW"))
                        .blockedReasons(List.of())
                        .build())
                .evidenceRefs(evidenceRefs(analysis, repairItems, improvementItems))
                .build();
    }

    private boolean modelFailed(SubmissionAnalysisResponse analysis) {
        SubmissionAnalysisResponse.AiInvocation invocation = analysis.getAiInvocation();
        return invocation != null && "MODEL_FAILED".equalsIgnoreCase(invocation.getStatus());
    }

    private List<StudentAiFeedbackResponse.FeedbackItem> repairItems(Submission submission, SubmissionAnalysisResponse analysis) {
        List<StudentAiFeedbackResponse.FeedbackItem> items = new ArrayList<>();
        for (SubmissionAnalysisResponse.BasicLayerAdvice advice : safe(analysis.getBasicLayerAdvice())) {
            if (advice == null) {
                continue;
            }
            addItem(items, StudentAiFeedbackResponse.FeedbackItem.builder()
                    .title(clean(advice.getTitle()))
                    .body(joinNonBlank(advice.getWhatHappened(), advice.getWhyItMatters(), advice.getStudentAction()))
                    .kind("REPAIR")
                    .skillUnitId(cleanId(advice.getSkillUnitId()))
                    .mistakePointId(cleanId(advice.getMistakePointId()))
                    .evidenceRefs(safe(advice.getEvidenceRefs()))
                    .evidenceSnippets(evidenceSnippets(safe(advice.getEvidenceRefs()), submission))
                    .qualitySignals(List.of("evidence_grounded", "actionable", "no_answer_leak"))
                    .build());
        }
        if (!items.isEmpty() || analysis.getStudentFeedback() == null) {
            return items;
        }
        for (SubmissionAnalysisResponse.FeedbackIssue issue : safe(analysis.getStudentFeedback().getBlockingIssues())) {
            if (issue == null) {
                continue;
            }
            addItem(items, StudentAiFeedbackResponse.FeedbackItem.builder()
                    .title(clean(issue.getTitle()))
                    .body(joinNonBlank(issue.getStudentMessage(), issue.getNextAction()))
                    .kind("REPAIR")
                    .skillUnitId(cleanId(issue.getIssueTag()))
                    .mistakePointId(cleanId(issue.getFineGrainedTag()))
                    .evidenceRefs(safe(issue.getEvidenceRefs()))
                    .evidenceSnippets(evidenceSnippets(safe(issue.getEvidenceRefs()), submission))
                    .qualitySignals(List.of("evidence_grounded", "actionable", "no_answer_leak"))
                    .build());
        }
        return items;
    }

    private List<StudentAiFeedbackResponse.FeedbackItem> improvementItems(Submission submission, SubmissionAnalysisResponse analysis) {
        List<StudentAiFeedbackResponse.FeedbackItem> items = new ArrayList<>();
        for (SubmissionAnalysisResponse.ImprovementLayerAdvice advice : safe(analysis.getImprovementLayerAdvice())) {
            if (advice == null) {
                continue;
            }
            addItem(items, StudentAiFeedbackResponse.FeedbackItem.builder()
                    .title(clean(advice.getTitle()))
                    .body(joinNonBlank(advice.getCurrentLimit(), advice.getSuggestion(), advice.getStudentBenefit()))
                    .kind("IMPROVEMENT")
                    .skillUnitId(cleanId(advice.getSkillUnitId()))
                    .improvementPointId(cleanId(advice.getImprovementPointId()))
                    .evidenceRefs(safe(advice.getEvidenceRefs()))
                    .evidenceSnippets(evidenceSnippets(safe(advice.getEvidenceRefs()), submission))
                    .qualitySignals(List.of("transfer"))
                    .build());
        }
        if (!items.isEmpty() || analysis.getStudentFeedback() == null) {
            return items;
        }
        for (SubmissionAnalysisResponse.ImprovementOpportunity item : safe(analysis.getStudentFeedback().getImprovementOpportunities())) {
            if (item == null) {
                continue;
            }
            addItem(items, StudentAiFeedbackResponse.FeedbackItem.builder()
                    .title(clean(item.getTitle()))
                    .body(joinNonBlank(item.getStudentMessage(), item.getBenefit()))
                    .kind("IMPROVEMENT")
                    .improvementPointId(cleanId(item.getCategory()))
                    .evidenceRefs(safe(item.getEvidenceRefs()))
                    .evidenceSnippets(evidenceSnippets(safe(item.getEvidenceRefs()), submission))
                    .qualitySignals(List.of("transfer"))
                    .build());
        }
        return items;
    }

    private StudentAiFeedbackResponse.StudentReport studentReport(
            SubmissionAnalysisResponse analysis,
            List<StudentAiFeedbackResponse.FeedbackItem> repairItems,
            List<StudentAiFeedbackResponse.FeedbackItem> improvementItems
    ) {
        SubmissionAnalysisResponse.StudentFeedback feedback = analysis.getStudentFeedback();
        String basic = firstNonBlank(
                feedback == null ? "" : feedback.getSummary(),
                analysis.getSummary(),
                repairItems.isEmpty() ? "" : "下面是本次完整诊断给出的修正重点。"
        );
        String improvement = improvementItems.isEmpty() ? "" : "修完基础问题后，再按下面的提升建议做迁移检查。";
        String nextAction = firstNonBlank(nextActionTask(analysis), nextQuestion(analysis), analysis.getStudentHint());
        if (basic.isBlank() && improvement.isBlank() && nextAction.isBlank()) {
            return null;
        }
        return StudentAiFeedbackResponse.StudentReport.builder()
                .basicLayerText(blankToNull(basic))
                .improvementLayerText(blankToNull(improvement))
                .nextActionText(blankToNull(nextAction))
                .build();
    }

    private String nextQuestion(SubmissionAnalysisResponse analysis) {
        SubmissionAnalysisResponse.NextLearningAction action = nextAction(analysis);
        return firstNonBlank(action == null ? "" : action.getCheckQuestion(), action == null ? "" : action.getTask());
    }

    private String nextActionTask(SubmissionAnalysisResponse analysis) {
        SubmissionAnalysisResponse.NextLearningAction action = nextAction(analysis);
        return action == null ? "" : clean(action.getTask());
    }

    private String nextActionRisk(SubmissionAnalysisResponse analysis) {
        SubmissionAnalysisResponse.NextLearningAction action = nextAction(analysis);
        return action == null ? "" : clean(action.getAnswerLeakRisk());
    }

    private SubmissionAnalysisResponse.NextLearningAction nextAction(SubmissionAnalysisResponse analysis) {
        SubmissionAnalysisResponse.StudentFeedback feedback = analysis == null ? null : analysis.getStudentFeedback();
        return feedback == null ? null : feedback.getNextLearningAction();
    }

    private List<String> evidenceRefs(SubmissionAnalysisResponse analysis,
                                      List<StudentAiFeedbackResponse.FeedbackItem> repairItems,
                                      List<StudentAiFeedbackResponse.FeedbackItem> improvementItems) {
        Set<String> refs = new LinkedHashSet<>(safe(analysis.getEvidenceRefs()));
        for (StudentAiFeedbackResponse.FeedbackItem item : safe(repairItems)) {
            refs.addAll(safe(item.getEvidenceRefs()));
        }
        for (StudentAiFeedbackResponse.FeedbackItem item : safe(improvementItems)) {
            refs.addAll(safe(item.getEvidenceRefs()));
        }
        return refs.stream().filter(value -> value != null && !value.isBlank()).toList();
    }

    private List<StudentAiFeedbackResponse.EvidenceSnippet> evidenceSnippets(List<String> refs, Submission submission) {
        if (submission == null || submission.getSourceCode() == null || refs == null || refs.isEmpty()) {
            return List.of();
        }
        List<StudentAiFeedbackResponse.EvidenceSnippet> snippets = new ArrayList<>();
        for (String ref : refs) {
            if (snippets.size() >= 3) {
                break;
            }
            StudentAiFeedbackResponse.EvidenceSnippet snippet = evidenceSnippet(ref, submission.getSourceCode());
            if (snippet != null) {
                snippets.add(snippet);
            }
        }
        return snippets;
    }

    private StudentAiFeedbackResponse.EvidenceSnippet evidenceSnippet(String evidenceRef, String sourceCode) {
        String ref = clean(evidenceRef);
        Matcher lineMatcher = CODE_LINE_REF.matcher(ref);
        if (lineMatcher.find()) {
            int line = parsePositiveInt(lineMatcher.group(1));
            String code = sourceLine(sourceCode, line);
            return code.isBlank() ? null : StudentAiFeedbackResponse.EvidenceSnippet.builder()
                    .evidenceRef(ref)
                    .lineNumber(line)
                    .lineEnd(line)
                    .code(code)
                    .build();
        }
        Matcher rangeMatcher = CODE_RANGE_REF.matcher(ref);
        if (rangeMatcher.find()) {
            int start = parsePositiveInt(rangeMatcher.group(1));
            int end = Math.min(start + 4, parsePositiveInt(rangeMatcher.group(2)));
            String code = sourceLines(sourceCode, start, end);
            return code.isBlank() ? null : StudentAiFeedbackResponse.EvidenceSnippet.builder()
                    .evidenceRef(ref)
                    .lineNumber(start)
                    .lineEnd(end)
                    .code(code)
                    .build();
        }
        return null;
    }

    private String sourceLine(String sourceCode, int lineNumber) {
        return sourceLines(sourceCode, lineNumber, lineNumber).replaceFirst("^\\d+:\\s*", "");
    }

    private String sourceLines(String sourceCode, int startLine, int endLine) {
        if (sourceCode == null || startLine <= 0 || endLine < startLine) {
            return "";
        }
        String[] lines = sourceCode.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        if (startLine > lines.length) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int line = startLine; line <= Math.min(endLine, lines.length); line++) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(line).append(": ").append(lines[line - 1]);
        }
        return clean(builder.toString());
    }

    private int parsePositiveInt(String value) {
        try {
            return Math.max(0, Integer.parseInt(value));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private void addItem(List<StudentAiFeedbackResponse.FeedbackItem> items, StudentAiFeedbackResponse.FeedbackItem item) {
        if (item == null || (clean(item.getTitle()).isBlank() && clean(item.getBody()).isBlank())) {
            return;
        }
        String key = (clean(item.getTitle()) + "|" + clean(item.getBody())).replaceAll("\\s+", "");
        boolean duplicate = items.stream()
                .anyMatch(existing -> (clean(existing.getTitle()) + "|" + clean(existing.getBody()))
                        .replaceAll("\\s+", "")
                        .equals(key));
        if (!duplicate) {
            items.add(item);
        }
    }

    private boolean hasStudentReport(StudentAiFeedbackResponse.StudentReport report) {
        return report != null && (!clean(report.getBasicLayerText()).isBlank()
                || !clean(report.getImprovementLayerText()).isBlank()
                || !clean(report.getNextActionText()).isBlank());
    }

    private String joinNonBlank(String... values) {
        List<String> parts = new ArrayList<>();
        for (String value : values) {
            String cleaned = clean(value);
            if (!cleaned.isBlank()) {
                parts.add(cleaned);
            }
        }
        return String.join(" ", parts);
    }

    private <T> List<T> safe(List<T> values) {
        return values == null ? List.of() : values;
    }

    private String cleanId(String value) {
        String cleaned = clean(value);
        return cleaned.isBlank() ? null : cleaned;
    }

    private String blankToNull(String value) {
        String cleaned = clean(value);
        return cleaned.isBlank() ? null : cleaned;
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            String cleaned = clean(value);
            if (!cleaned.isBlank()) {
                return cleaned;
            }
        }
        return "";
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
