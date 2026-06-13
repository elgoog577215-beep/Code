package com.onlinejudge.submission.application;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlinejudge.submission.dto.SubmissionAnalysisResponse;
import com.onlinejudge.submission.dto.StudentAiFeedbackResponse;
import com.onlinejudge.problem.domain.Problem;
import com.onlinejudge.submission.domain.Submission;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class AiReportService {

    private static final int MAX_SOURCE_CODE_LENGTH = 12000;
    private static final String AI_SOURCE = "MODEL_SCOPE_EXTERNAL_MODEL";
    private static final String PROVIDER = "ModelScope";
    private static final String PROMPT_VERSION = "submission-diagnosis-prompt-v2";
    private static final String RUNTIME_PROMPT_VERSION = "diagnosis-judge-v2+teaching-hint-v1";
    private static final String SINGLE_CALL_RUNTIME_PROMPT_VERSION = "diagnosis-and-teaching-v3";
    private static final String STUDENT_FAST_FEEDBACK_PROMPT_VERSION = "student-fast-feedback-v2";
    private static final String RUNTIME_MODE_SINGLE_CALL = "single-call";
    private static final Pattern NUMBERED_LINE_PATTERN = Pattern.compile("^(\\d+):\\s?(.*)$", Pattern.MULTILINE);
    private static final Pattern REPORT_LINE_ISSUE_PATTERN = Pattern.compile(
            "行号[：:]\\s*(\\d+)\\s*错误[：:]\\s*(.+?)\\s*建议[：:]\\s*(.+?)(?=(?:\\n\\s*行号[：:])|\\Z)",
            Pattern.DOTALL
    );

    private final ObjectMapper objectMapper;
    private final AiCodeAssistSupport aiCodeAssistSupport;
    private final ExternalModelAgentRuntime externalModelAgentRuntime;
    private final ExternalModelFailureClassifier failureClassifier;
    private final ExternalModelBudgetGuard budgetGuard;
    private final ThreadLocal<ExternalModelCallTelemetry> lastCallTelemetry = ThreadLocal.withInitial(ExternalModelCallTelemetry::empty);
    private final ThreadLocal<ExternalModelCallTelemetry> lastStructuredRetrySourceTelemetry =
            ThreadLocal.withInitial(ExternalModelCallTelemetry::empty);
    private final ThreadLocal<ExternalModelRequestContext> nextRequestContext =
            ThreadLocal.withInitial(ExternalModelRequestContext::standard);
    private final ThreadLocal<String> lastModelStageRawContent = ThreadLocal.withInitial(() -> "");
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public AiReportService(ObjectMapper objectMapper, AiCodeAssistSupport aiCodeAssistSupport) {
        this(objectMapper, aiCodeAssistSupport, null, new ExternalModelFailureClassifier(), new ExternalModelBudgetGuard());
    }

    public AiReportService(ObjectMapper objectMapper,
                           AiCodeAssistSupport aiCodeAssistSupport,
                           ExternalModelAgentRuntime externalModelAgentRuntime) {
        this(objectMapper, aiCodeAssistSupport, externalModelAgentRuntime, new ExternalModelFailureClassifier(), new ExternalModelBudgetGuard());
    }

    @Autowired
    public AiReportService(ObjectMapper objectMapper,
                           AiCodeAssistSupport aiCodeAssistSupport,
                           ExternalModelAgentRuntime externalModelAgentRuntime,
                           ExternalModelFailureClassifier failureClassifier,
                           ExternalModelBudgetGuard budgetGuard) {
        this.objectMapper = objectMapper;
        this.aiCodeAssistSupport = aiCodeAssistSupport;
        this.externalModelAgentRuntime = externalModelAgentRuntime;
        this.failureClassifier = failureClassifier == null ? new ExternalModelFailureClassifier() : failureClassifier;
        this.budgetGuard = budgetGuard == null ? new ExternalModelBudgetGuard() : budgetGuard;
    }

    @Value("${ai.enabled:true}")
    private boolean enabled;

    @Value("${ai.base-url:https://api-inference.modelscope.cn/v1}")
    private String baseUrl;

    @Value("${ai.api-key:}")
    private String apiKey;

    @Value("${ai.model:deepseek-ai/DeepSeek-V4-Pro}")
    private String model;

    @Value("${ai.timeout-seconds:25}")
    private long timeoutSeconds;

    @Value("${ai.external-runtime-enabled:true}")
    private boolean externalRuntimeEnabled;

    @Value("${ai.external-runtime-mode:single-call}")
    private String externalRuntimeMode = RUNTIME_MODE_SINGLE_CALL;

    @Value("${ai.external-runtime-profile:auto}")
    private String externalRuntimeProfile = ExternalModelAgentRuntime.RUNTIME_PROFILE_AUTO;

    @Value("${ai.external-single-call-prompt-version:}")
    private String externalSingleCallPromptVersion = "";

    @Value("${ai.max-output-tokens:1800}")
    private int maxOutputTokens = 1800;

    @Value("${ai.structured-retry-output-tokens:2600}")
    private int structuredRetryOutputTokens = 2600;

    @Value("${ai.stream-enabled:true}")
    private boolean streamEnabled;

    @Value("${ai.stream-fallback-enabled:true}")
    private boolean streamFallbackEnabled = true;

    @Value("${ai.structured-retry-enabled:true}")
    private boolean structuredRetryEnabled = true;

    @Value("${ai.retry.max-attempts:2}")
    private int retryMaxAttempts;

    @Value("${ai.retry.backoff-ms:700}")
    private long retryBackoffMs;

    public SubmissionAnalysisResponse enhanceSubmissionAnalysis(Problem problem,
                                                                Submission submission,
                                                                SubmissionAnalysisResponse fallback) {
        return enhanceSubmissionAnalysis(problem, submission, fallback, null, null);
    }

    public SubmissionAnalysisResponse enhanceSubmissionAnalysis(Problem problem,
                                                                Submission submission,
                                                                SubmissionAnalysisResponse fallback,
                                                                DiagnosisEvidencePackage evidencePackage,
                                                                RuleSignalAnalyzer.RuleSignalResult ruleSignals) {
        if (!canCallAi()) {
            log.info("AI submission analysis skipped because AI access is unavailable. submissionId={}", submission.getId());
            return fallback;
        }

        try {
            log.info("AI submission analysis started. submissionId={}, problemId={}, language={}",
                    submission.getId(),
                    submission.getProblemId(),
                    submission.getLanguageName());
            String rawSourceCode = submission.getSourceCode() == null ? "" : submission.getSourceCode();
            List<SubmissionAnalysisResponse.LineIssue> baselineLineIssues = fallback == null || fallback.getLineIssues() == null
                    ? List.of()
                    : fallback.getLineIssues();
            if (shouldUseExternalRuntime(evidencePackage, ruleSignals)) {
                return enhanceWithExternalRuntime(
                        submission,
                        fallback,
                        evidencePackage,
                        ruleSignals,
                        rawSourceCode,
                        baselineLineIssues
                );
            }

            Map<String, Object> context = new LinkedHashMap<>();
            context.put("problemTitle", problem.getTitle());
            context.put("problemDescription", problem.getDescription());
            context.put("aiPromptDirection", problem.getAiPromptDirection() == null ? "" : problem.getAiPromptDirection());
            context.put("verdict", submission.getVerdict() == null ? "UNKNOWN" : submission.getVerdict().name());
            context.put("language", submission.getLanguageName());
            context.put("sourceCode", truncateSourceCode(rawSourceCode));
            context.put("sourceCodeLineCount", countSourceLines(rawSourceCode));
            context.put("sourceCodeForLineAnalysis", buildLineAwareSourceCode(rawSourceCode));
            context.put("baselineLineIssues", baselineLineIssues);
            context.put("compileOutput", submission.getCompileOutput() == null ? "" : submission.getCompileOutput());
            context.put("runtimeErrorMessage", submission.getErrorMessage() == null ? "" : submission.getErrorMessage());
            context.put("baselineAnalysis", fallback);
            context.put("firstFailedCase", fallback == null ? null : fallback.getFirstFailedCase());
            context.put("evidencePackage", evidencePackage);
            context.put("ruleSignals", ruleSignals);

            String systemPrompt = """
                    You are an online judge debugging assistant.
                    Return strict JSON only. Do not wrap the response in markdown fences.
                    Do not output explanations, chain-of-thought, XML, or any text outside the JSON object.
                    All generated user-facing strings must be Simplified Chinese.

                    Required JSON fields:
                    headline(string),
                    summary(string),
                    issueTags(string[]),
                    fineGrainedTags(string[]),
                    abilityPoints(string[]),
                    focusPoints(string[]),
                    fixDirections(string[]),
                    evidenceRefs(string[]),
                    studentHint(string),
                    studentHintPlan({
                      hintLevel("L1"|"L2"|"L3"|"L4"),
                      problemType(string),
                      evidenceAnchor(string),
                      nextAction(string),
                      coachQuestion(string),
                      teachingAction(string),
                      evidenceRefs(string[]),
                      answerLeakRisk("LOW"|"MEDIUM"|"HIGH")
                    }),
                    learningInterventionPlan({
                      interventionType(string),
                      goal(string),
                      studentTask(string),
                      checkQuestion(string),
                      completionSignal(string),
                      evidenceRefs(string[]),
                      estimatedMinutes(number),
                      answerLeakRisk("LOW"|"MEDIUM"|"HIGH")
                    }),
                    teacherNote(string),
                    progressSignal(string),
                    confidence(number),
                    uncertainty(string),
                    answerLeakRisk("LOW"|"MEDIUM"|"HIGH"),
                    wrongSolution(string|null),
                    correctSolution(string|null),
                    lineIssues([{lineNumber(number), error(string), suggestion(string)}]),
                    reportMarkdown(string)

                    Teaching and safety rules:
                    1. Prefer hints about thinking path, boundary categories, complexity, and debugging direction.
                    2. Do not provide complete code, final answers, hidden test data, or a step-by-step solution that removes the student's thinking work.
                    3. issueTags must reuse standard learning-diagnosis labels such as SYNTAX_ERROR, IO_FORMAT, BOUNDARY_CONDITION, CONDITION_BRANCH, LOOP_BOUNDARY, DATA_STRUCTURE_CHOICE, TIME_COMPLEXITY, SPACE_COMPLEXITY, VARIABLE_INITIALIZATION, STATE_TRANSITION, RECURSION_EXIT, CODE_READABILITY, SAMPLE_ONLY, ALGORITHM_STRATEGY, RUNTIME_STABILITY, NEEDS_MORE_EVIDENCE.
                    4. fineGrainedTags must only reuse candidate or standard fine-grained labels such as OFF_BY_ONE, EMPTY_INPUT, MAX_BOUNDARY, DUPLICATE_CASE, OUTPUT_FORMAT_DETAIL, INPUT_PARSING, INITIAL_STATE, STATE_RESET, OVER_SIMULATION, BRUTE_FORCE_LIMIT, GREEDY_ASSUMPTION, DP_STATE_DESIGN, IN_PLACE_STATE_PROGRESS, SAMPLE_OVERFIT, PARTIAL_FIX_REGRESSION.
                    5. evidenceRefs must cite evidencePackage or ruleSignals references, not invented evidence.
                    6. uncertainty must state what is unknown or inferred, especially for hidden tests.
                    7. studentHint should be scaffolded at hint level 1-2: problem type and locating direction, not a full fix.
                    8. teacherNote should summarize what the teacher can act on.
                    9. answerLeakRisk must be HIGH if the response contains complete code, direct final solution, or hidden data.
                    10. studentHintPlan must split the student-facing guidance into: current problem type, evidence anchor, next action, and one coach question. Keep it short and actionable.
                    11. teachingAction must be one of ASK_MIN_CASE, TRACE_VARIABLES, COMPARE_OUTPUT, COUNT_COMPLEXITY, DEFINE_STATE, CHECK_INVARIANT, BUILD_COUNTEREXAMPLE, COMPARE_SUBMISSIONS, CHECK_RUNTIME_GUARDS, COLLECT_EVIDENCE, FIX_FIRST_COMPILER_ERROR, COMPARE_INPUT_SPEC, TRACE_STATE, COMPARE_STRUCTURES, DRAW_RECURSION_TREE, EXPLAIN_GENERALITY, CHECK_BRANCH_COVERAGE.
                    12. learningInterventionPlan must describe one small, verifiable learning action. It must not include complete code or final solution. The completionSignal must tell a teacher what observable student work counts as done.

                    Rules for line-aware analysis:
                    1. You must analyze sourceCodeForLineAnalysis first. It contains the real source code with line numbers.
                    2. If baselineLineIssues, compileOutput, or runtimeErrorMessage already expose concrete line numbers, preserve or refine those lines instead of inventing new ones.
                    3. Any code-specific diagnosis must include a concrete lineNumber.
                    4. Every lineIssues item must contain both error and suggestion.
                    5. If you cannot confidently locate a concrete line, return an empty array [] instead of guessing.
                    6. If verdict is COMPILATION_ERROR, prioritize compileOutput. If verdict is RUNTIME_ERROR, prioritize runtimeErrorMessage and stack traces.
                    7. reportMarkdown should mention concrete line numbers whenever it talks about code issues.
                    8. Hidden test cases must not be guessed or leaked.
                    9. Keep the advice grounded in the judging facts and the provided code.
                    10. If ruleSignals contain candidate tags, prefer selecting from them unless the evidence clearly suggests another standard tag.
                    """;
            String userPrompt = "Generate the JSON from this context: " + objectMapper.writeValueAsString(context);
            String content = chatCompletion(systemPrompt, userPrompt); /*
                    """
                    你是中文 OJ 智能教练。
                    请根据评测结果输出严格 JSON，不要输出 Markdown 代码块，不要输出 JSON 之外的任何解释，不要输出 <think>、思考过程或草稿。
                    JSON 字段必须包含：
                    headline(string),
                    summary(string),
                    focusPoints(string[]),
                    fixDirections(string[]),
                    wrongSolution(string|null),
                    correctSolution(string|null),
                    lineIssues([{lineNumber(number), error(string), suggestion(string)}]),
                    reportMarkdown(string)
                    如果 verdict 是 COMPILATION_ERROR，必须优先根据 compileOutput 给出带行号的 lineIssues；如果是 RUNTIME_ERROR 且 runtimeErrorMessage 含有行号，也必须优先使用这些行号。

                    纠错格式要求：
                    1. 必须优先基于带行号代码进行分析。
                    2. 只要指出代码问题，就必须给出具体 lineNumber。
                    3. 每条 lineIssues 都必须同时包含 error 和 suggestion。
                    4. 禁止返回“检查循环”“看看边界”这类不带行号的模糊建议放进 lineIssues。
                    5. 如果暂时无法定位到具体行，就返回空数组 []，不要编造行号。
                    6. reportMarkdown 中如果提到代码问题，也必须尽量引用具体行号，格式示例：
                       行号：5
                       错误：变量未定义
                       建议：定义变量后再使用

                    要求：
                    1. 全部使用中文。
                    2. 结论必须贴合 OJ 评测场景，不要空泛。
                    3. 如果失败测试点是隐藏的，不要猜测或泄露具体隐藏数据。
                    4. 可以比 baseline 更自然，但不能偏离评测事实。
                    """,
                    "请基于以下上下文生成 JSON：" + objectMapper.writeValueAsString(context)
            ); */

            AiAnalysisPayload payload = parseAnalysisPayload(content);
            if (payload == null || payload.reportMarkdown == null || payload.reportMarkdown.isBlank()) {
                log.warn("AI submission analysis returned no structured markdown payload. submissionId={}", submission.getId());
                String markdownFallback = cleanupAiText(content);
                if (!markdownFallback.isBlank()) {
                    return SubmissionAnalysisResponse.builder()
                            .submissionId(fallback.getSubmissionId())
                            .analysisSchemaVersion(fallback.getAnalysisSchemaVersion())
                            .evidenceSchemaVersion(fallback.getEvidenceSchemaVersion())
                            .taxonomyVersion(fallback.getTaxonomyVersion())
                            .sourceType(AI_SOURCE)
                            .scenario(fallback.getScenario())
                            .headline(fallback.getHeadline())
                            .summary(fallback.getSummary())
                            .issueTags(cleanList(fallback.getIssueTags(), List.of()))
                            .fineGrainedTags(cleanList(fallback.getFineGrainedTags(), List.of()))
                            .abilityPoints(cleanList(fallback.getAbilityPoints(), List.of()))
                            .focusPoints(cleanList(fallback.getFocusPoints(), List.of()))
                            .fixDirections(cleanList(fallback.getFixDirections(), List.of()))
                            .evidenceRefs(cleanList(fallback.getEvidenceRefs(), List.of()))
                            .studentHint(fallback.getStudentHint())
                            .studentHintPlan(fallback.getStudentHintPlan())
                            .studentFeedback(fallback.getStudentFeedback())
                            .learningInterventionPlan(fallback.getLearningInterventionPlan())
                            .learningActionEvidence(fallback.getLearningActionEvidence())
                            .teacherNote(fallback.getTeacherNote())
                            .progressSignal(fallback.getProgressSignal())
                            .confidence(fallback.getConfidence())
                            .uncertainty(fallback.getUncertainty())
                            .diagnosticTrace(fallback.getDiagnosticTrace())
                            .aiInvocation(modelInvocation(fallback, "MODEL_TEXT_FALLBACK", false))
                            .answerLeakRisk(fallback.getAnswerLeakRisk())
                            .wrongSolution(fallback.getWrongSolution())
                            .correctSolution(fallback.getCorrectSolution())
                            .lineIssues(baselineLineIssues)
                            .firstFailedCase(fallback.getFirstFailedCase())
                            .reportMarkdown(markdownFallback)
                            .generatedAt(fallback.getGeneratedAt())
                            .build();
                }
                return fallback;
            }

            return SubmissionAnalysisResponse.builder()
                    .submissionId(fallback.getSubmissionId())
                    .analysisSchemaVersion(defaultIfBlank(payload.analysisSchemaVersion, fallback.getAnalysisSchemaVersion()))
                    .evidenceSchemaVersion(defaultIfBlank(payload.evidenceSchemaVersion, fallback.getEvidenceSchemaVersion()))
                    .taxonomyVersion(defaultIfBlank(payload.taxonomyVersion, fallback.getTaxonomyVersion()))
                    .sourceType(AI_SOURCE)
                    .scenario(fallback.getScenario())
                    .headline(defaultIfBlank(payload.headline, fallback.getHeadline()))
                    .summary(defaultIfBlank(payload.summary, fallback.getSummary()))
                    .issueTags(cleanList(payload.issueTags, fallback.getIssueTags()))
                    .fineGrainedTags(cleanList(payload.fineGrainedTags, fallback.getFineGrainedTags()))
                    .abilityPoints(cleanList(payload.abilityPoints, fallback.getAbilityPoints()))
                    .focusPoints(cleanList(payload.focusPoints, fallback.getFocusPoints()))
                    .fixDirections(cleanList(payload.fixDirections, fallback.getFixDirections()))
                    .evidenceRefs(cleanList(payload.evidenceRefs, fallback.getEvidenceRefs()))
                    .studentHint(defaultIfBlank(payload.studentHint, fallback.getStudentHint()))
                    .studentHintPlan(cleanHintPlan(payload.studentHintPlan, fallback.getStudentHintPlan()))
                    .studentFeedback(fallback.getStudentFeedback())
                    .learningInterventionPlan(cleanInterventionPlan(payload.learningInterventionPlan, fallback.getLearningInterventionPlan()))
                    .learningActionEvidence(fallback.getLearningActionEvidence())
                    .teacherNote(defaultIfBlank(payload.teacherNote, fallback.getTeacherNote()))
                    .progressSignal(defaultIfBlank(payload.progressSignal, fallback.getProgressSignal()))
                    .confidence(resolveConfidence(payload.confidence, fallback.getConfidence()))
                    .uncertainty(defaultIfBlank(payload.uncertainty, fallback.getUncertainty()))
                    .diagnosticTrace(fallback.getDiagnosticTrace())
                    .aiInvocation(modelInvocation(fallback, "MODEL_COMPLETED", false))
                    .answerLeakRisk(resolveAnswerLeakRisk(payload.answerLeakRisk, fallback.getAnswerLeakRisk()))
                    .wrongSolution(defaultNullable(payload.wrongSolution, fallback.getWrongSolution()))
                    .correctSolution(defaultNullable(payload.correctSolution, fallback.getCorrectSolution()))
                    .lineIssues(aiCodeAssistSupport.resolveLineIssues(
                            toLineIssueCandidates(payload),
                            payload == null ? null : payload.reportMarkdown,
                            content,
                            rawSourceCode,
                            baselineLineIssues
                    ))
                    .firstFailedCase(fallback.getFirstFailedCase())
                    .reportMarkdown(cleanupAiText(payload.reportMarkdown))
                    .generatedAt(fallback.getGeneratedAt())
                    .build();
        } catch (Exception exception) {
            log.error("AI submission analysis enhancement failed. submissionId={}", submission.getId(), exception);
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            if (shouldUseExternalRuntime(evidencePackage, ruleSignals)) {
                return runtimeFallback(fallback, stageFailureFromException("SUBMISSION_ANALYSIS", exception));
            }
            return fallback;
        }
    }

    public StudentAiFeedbackResponse generateStudentAiFeedback(Problem problem,
                                                               Submission submission,
                                                               DiagnosisEvidencePackage evidencePackage,
                                                               RuleSignalAnalyzer.RuleSignalResult ruleSignals) {
        long startedAt = System.nanoTime();
        if (!canCallAi()) {
            return feedbackFailure(submission, "FAILED", "AI_UNAVAILABLE", startedAt);
        }

        try {
            Map<String, Object> context = compactStudentFastFeedbackContext(problem, submission, evidencePackage, ruleSignals);

            String systemPrompt = """
                    You are a fast student-facing OJ coach. Return strict minified JSON only. No markdown. No chain-of-thought.
                    All visible strings must be Simplified Chinese.

                    Shape:
                    {"repairItems":[{"title":"","body":"","kind":"","evidenceRefs":[],"qualitySignals":["evidence_grounded","actionable","no_answer_leak"]}],"improvementItems":[{"title":"","body":"","kind":"IMPROVEMENT","evidenceRefs":[],"qualitySignals":["transfer"]}],"nextQuestion":"","safety":{"answerLeakRisk":"LOW|MEDIUM|HIGH","blockedReasons":[]},"evidenceRefs":[]}

                    Rules:
                    1. repairItems max 1, body <= 70 Chinese chars. Teach where/how to verify the first fix; never give final code or full answer.
                    2. improvementItems max 1, body <= 60 Chinese chars. Prefer testing habit, boundary awareness, or code clarity.
                    3. nextQuestion <= 35 Chinese chars and must be a self-check question.
                    4. Use only evidenceRefs from input. Do not guess hidden tests.
                    5. If evidence is insufficient or leak risk is HIGH, return empty items and explain in blockedReasons.
                    """;
            String userPrompt = "Generate StudentAiFeedback from this context: " + objectMapper.writeValueAsString(context);
            int fastFeedbackOutputTokens = Math.min(Math.max(320, maxOutputTokens), 520);
            String content = chatCompletionForStudentFeedback(systemPrompt, userPrompt, fastFeedbackOutputTokens);
            StudentFastFeedbackPayload payload = parseModelStagePayload(content, StudentFastFeedbackPayload.class);
            StudentAiFeedbackResponse response = normalizeStudentFastFeedback(payload, submission, startedAt);
            if (!"READY".equals(response.getStatus())) {
                return response;
            }
            return response;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return feedbackFailure(submission, "TIMEOUT", "INTERRUPTED", startedAt);
        } catch (IOException exception) {
            return feedbackFailure(submission, classifyFeedbackFailure(exception), exception.getMessage(), startedAt);
        } catch (Exception exception) {
            return feedbackFailure(submission, "FAILED", exception.getMessage(), startedAt);
        }
    }

    private boolean shouldUseExternalRuntime(DiagnosisEvidencePackage evidencePackage,
                                             RuleSignalAnalyzer.RuleSignalResult ruleSignals) {
        return externalRuntimeEnabled
                && externalModelAgentRuntime != null
                && evidencePackage != null
                && ruleSignals != null;
    }

    private SubmissionAnalysisResponse enhanceWithExternalRuntime(Submission submission,
                                                                  SubmissionAnalysisResponse fallback,
                                                                  DiagnosisEvidencePackage evidencePackage,
                                                                  RuleSignalAnalyzer.RuleSignalResult ruleSignals,
                                                                  String rawSourceCode,
                                                                  List<SubmissionAnalysisResponse.LineIssue> baselineLineIssues)
            throws JsonProcessingException, IOException, InterruptedException {
        ExternalModelAgentRuntime.RuntimePlan runtimePlan = externalModelAgentRuntime.prepare(
                evidencePackage,
                ruleSignals,
                fallback,
                externalRuntimeProfile,
                externalSingleCallPromptVersion
        );
        if (useSingleCallRuntime()) {
            return enhanceWithSingleCallRuntime(
                    submission,
                    fallback,
                    runtimePlan,
                    rawSourceCode,
                    baselineLineIssues
            );
        }

        ExternalModelStagePayloads.DiagnosisJudgeOutput decision;
        try {
            decision = callDiagnosisJudgeStage(runtimePlan);
            decision = externalModelAgentRuntime.normalizeDiagnosisDecision(decision, runtimePlan);
        } catch (Exception exception) {
            return runtimeFallback(fallback, runtimePlan, stageFailureFromException("DIAGNOSIS_JUDGE", exception));
        }
        ExternalModelStagePayloads.StageValidationResult decisionValidation =
                withStage("DIAGNOSIS_JUDGE", externalModelAgentRuntime.validateDiagnosisDecision(decision, runtimePlan));
        if (!decisionValidation.isValid()) {
            decisionValidation = withTransportAttribution(decisionValidation);
            log.warn("External model diagnosis stage failed validation. submissionId={}, reason={}, message={}",
                    submission.getId(),
                    decisionValidation.getFailureReason(),
                    decisionValidation.getMessage());
            return runtimeFallback(fallback, runtimePlan, decisionValidation);
        }

        ExternalModelStagePayloads.TeachingHintOutput teachingHint;
        try {
            teachingHint = callTeachingHintStage(runtimePlan, decision);
            teachingHint = externalModelAgentRuntime.normalizeTeachingHint(teachingHint, runtimePlan);
        } catch (Exception exception) {
            return buildPartialRuntimeAnalysisResponse(
                    fallback,
                    runtimePlan,
                    decision,
                    stageFailureFromException("TEACHING_HINT", exception),
                    rawSourceCode,
                    baselineLineIssues
            );
        }
        ExternalModelStagePayloads.StageValidationResult teachingValidation =
                withStage("TEACHING_HINT", externalModelAgentRuntime.validateTeachingHint(teachingHint, decision, runtimePlan));
        if (!teachingValidation.isValid()) {
            teachingValidation = withTransportAttribution(teachingValidation);
            log.warn("External model teaching stage failed validation. submissionId={}, reason={}, message={}",
                    submission.getId(),
                    teachingValidation.getFailureReason(),
                    teachingValidation.getMessage());
            return buildPartialRuntimeAnalysisResponse(
                    fallback,
                    runtimePlan,
                    decision,
                    teachingValidation,
                    rawSourceCode,
                    baselineLineIssues
            );
        }

        return buildRuntimeAnalysisResponse(
                fallback,
                runtimePlan,
                decision,
                teachingHint,
                null,
                rawSourceCode,
                baselineLineIssues
        );
    }

    private ExternalModelStagePayloads.DiagnosisJudgeOutput callDiagnosisJudgeStage(
            ExternalModelAgentRuntime.RuntimePlan runtimePlan) throws JsonProcessingException, IOException, InterruptedException {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("brief", runtimePlan.getBrief());
        request.put("standardLibrary", runtimePlan.getStandardLibraryPack());
        activateRuntimePlan(runtimePlan);
        String content = chatCompletion(
                runtimePlan.getDiagnosisPrompt().getSystemPrompt(),
                objectMapper.writeValueAsString(request)
        );
        ExternalModelStagePayloads.DiagnosisJudgeOutput output =
                parseModelStagePayload(content, ExternalModelStagePayloads.DiagnosisJudgeOutput.class);
        return output;
    }

    private SubmissionAnalysisResponse enhanceWithSingleCallRuntime(
            Submission submission,
            SubmissionAnalysisResponse fallback,
            ExternalModelAgentRuntime.RuntimePlan runtimePlan,
            String rawSourceCode,
            List<SubmissionAnalysisResponse.LineIssue> baselineLineIssues)
            throws JsonProcessingException, IOException, InterruptedException {
        ExternalModelStagePayloads.CombinedOutput combinedOutput;
        try {
            combinedOutput = callSingleCallRuntimeStage(runtimePlan);
            combinedOutput = externalModelAgentRuntime.normalizeCombinedOutput(combinedOutput, runtimePlan);
        } catch (Exception exception) {
            return runtimeFallback(fallback, runtimePlan, stageFailureFromException("DIAGNOSIS_AND_TEACHING", exception));
        }
        if (combinedOutput == null) {
            ExternalModelStagePayloads.DiagnosisJudgeOutput retainedDecision =
                    retainDiagnosisDecisionFromTruncatedSingleCall(runtimePlan);
            if (retainedDecision != null) {
                return buildPartialRuntimeAnalysisResponse(
                        fallback,
                        runtimePlan,
                        retainedDecision,
                        withStage("DIAGNOSIS_AND_TEACHING", ExternalModelStagePayloads.StageValidationResult.builder()
                                .valid(false)
                                .failureReason(ModelStageFailureReason.OUTPUT_TRUNCATED)
                                .message("Single-call output was truncated after a valid diagnosisDecision.")
                                .build()),
                        rawSourceCode,
                        baselineLineIssues
                );
            }
        }

        ExternalModelStagePayloads.DiagnosisJudgeOutput decision =
                combinedOutput == null ? null : combinedOutput.getDiagnosisDecision();
        ExternalModelStagePayloads.StageValidationResult decisionValidation =
                withStage("DIAGNOSIS_AND_TEACHING", externalModelAgentRuntime.validateDiagnosisDecision(decision, runtimePlan));
        if (!decisionValidation.isValid()) {
            decisionValidation = withTransportAttribution(decisionValidation);
            log.warn("External model single-call diagnosis failed validation. submissionId={}, reason={}, message={}",
                    submission.getId(),
                    decisionValidation.getFailureReason(),
                    decisionValidation.getMessage());
            return runtimeFallback(fallback, runtimePlan, decisionValidation);
        }

        ExternalModelStagePayloads.TeachingHintOutput teachingHint =
                combinedOutput == null ? null : combinedOutput.getTeachingHint();
        if (teachingHint != null) {
            ExternalModelStagePayloads.StageValidationResult teachingValidation =
                    withStage("DIAGNOSIS_AND_TEACHING", externalModelAgentRuntime.validateTeachingHint(teachingHint, decision, runtimePlan));
            if (!teachingValidation.isValid()) {
                teachingValidation = withTransportAttribution(teachingValidation);
                log.warn("External model single-call teaching failed validation. submissionId={}, reason={}, message={}",
                        submission.getId(),
                        teachingValidation.getFailureReason(),
                        teachingValidation.getMessage());
                return buildPartialRuntimeAnalysisResponse(
                        fallback,
                        runtimePlan,
                        decision,
                        teachingValidation,
                        rawSourceCode,
                        baselineLineIssues
                );
            }
        } else {
            teachingHint = buildModelActionTeachingHintFromDecision(decision, runtimePlan, fallback);
        }

        SubmissionAnalysisResponse.StudentFeedback studentFeedback =
                combinedOutput == null ? null : combinedOutput.getStudentFeedback();
        ExternalModelStagePayloads.StageValidationResult feedbackValidation =
                withStage("DIAGNOSIS_AND_TEACHING", externalModelAgentRuntime.validateStudentFeedback(
                        studentFeedback,
                        decision,
                        runtimePlan
                ));
        if (!feedbackValidation.isValid()) {
            feedbackValidation = withTransportAttribution(feedbackValidation);
            log.warn("External model single-call student feedback failed validation. submissionId={}, reason={}, message={}",
                    submission.getId(),
                    feedbackValidation.getFailureReason(),
                    feedbackValidation.getMessage());
            return buildPartialRuntimeAnalysisResponse(
                    fallback,
                    runtimePlan,
                    decision,
                    feedbackValidation,
                    rawSourceCode,
                    baselineLineIssues
            );
        }

        SubmissionAnalysisResponse response = buildRuntimeAnalysisResponse(
                fallback,
                runtimePlan,
                decision,
                teachingHint,
                studentFeedback,
                rawSourceCode,
                baselineLineIssues
        );
        response.setAiInvocation(modelInvocation(fallback, "MODEL_COMPLETED", false,
                runtimePromptVersion(runtimePlan)));
        return response;
    }

    private ExternalModelStagePayloads.CombinedOutput callSingleCallRuntimeStage(
            ExternalModelAgentRuntime.RuntimePlan runtimePlan) throws JsonProcessingException, IOException, InterruptedException {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("brief", runtimePlan.getBrief());
        request.put("standardLibrary", runtimePlan.getStandardLibraryPack());
        activateRuntimePlan(runtimePlan);
        String systemPrompt = runtimePlan.getSingleCallPrompt().getSystemPrompt();
        String userPrompt = objectMapper.writeValueAsString(request);
        String content = chatCompletion(systemPrompt, userPrompt);
        lastStructuredRetrySourceTelemetry.set(ExternalModelCallTelemetry.empty());
        ExternalModelStagePayloads.CombinedOutput output =
                parseModelStagePayload(content, ExternalModelStagePayloads.CombinedOutput.class);
        if (output != null) {
            return output;
        }
        return retryStructuredModelStagePayload(
                systemPrompt,
                userPrompt,
                runtimePlan,
                ExternalModelStagePayloads.CombinedOutput.class
        );
    }

    private ExternalModelStagePayloads.DiagnosisJudgeOutput retainDiagnosisDecisionFromTruncatedSingleCall(
            ExternalModelAgentRuntime.RuntimePlan runtimePlan) {
        if (!"length".equalsIgnoreCase(defaultIfBlank(lastCallTelemetry.get().streamFinishReason(), ""))) {
            return null;
        }
        String fragment = extractJsonObjectField(lastModelStageRawContent.get(), "diagnosisDecision");
        if (fragment.isBlank()) {
            return null;
        }
        ExternalModelStagePayloads.DiagnosisJudgeOutput decision;
        try {
            decision = objectMapper.readValue(fragment, ExternalModelStagePayloads.DiagnosisJudgeOutput.class);
            decision = externalModelAgentRuntime.normalizeDiagnosisDecision(decision, runtimePlan);
        } catch (JsonProcessingException exception) {
            log.warn("Truncated single-call diagnosisDecision extraction failed. error={}, contentPreview={}",
                    exception.getMessage(),
                    previewBody(fragment));
            return null;
        }
        ExternalModelStagePayloads.StageValidationResult validation =
                externalModelAgentRuntime.validateDiagnosisDecision(decision, runtimePlan);
        if (validation == null || !validation.isValid()) {
            log.warn("Truncated single-call diagnosisDecision failed validation. reason={}, message={}",
                    validation == null ? ModelStageFailureReason.UNKNOWN_ERROR : validation.getFailureReason(),
                    validation == null ? "" : validation.getMessage());
            return null;
        }
        return decision;
    }

    private ExternalModelStagePayloads.TeachingHintOutput callTeachingHintStage(
            ExternalModelAgentRuntime.RuntimePlan runtimePlan,
            ExternalModelStagePayloads.DiagnosisJudgeOutput decision)
            throws JsonProcessingException, IOException, InterruptedException {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("brief", runtimePlan.getBrief());
        request.put("standardLibrary", runtimePlan.getStandardLibraryPack());
        request.put("diagnosisDecision", decision);
        activateRuntimePlan(runtimePlan);
        String content = chatCompletion(
                runtimePlan.getTeachingPrompt().getSystemPrompt(),
                objectMapper.writeValueAsString(request)
        );
        return parseModelStagePayload(content, ExternalModelStagePayloads.TeachingHintOutput.class);
    }

    private <T> T retryStructuredModelStagePayload(String systemPrompt,
                                                   String userPrompt,
                                                   ExternalModelAgentRuntime.RuntimePlan runtimePlan,
                                                   Class<T> payloadType)
            throws IOException, InterruptedException {
        ExternalModelCallTelemetry firstTelemetry = lastCallTelemetry.get();
        String firstRawContent = lastModelStageRawContent.get();
        if (!shouldRetryStructuredPayload(payloadType, firstTelemetry, firstRawContent)) {
            return null;
        }
        try {
            int retryMaxTokens = Math.max(Math.max(128, maxOutputTokens), Math.max(128, structuredRetryOutputTokens));
            activateRuntimePlan(runtimePlan);
            log.warn("Retrying structured AI payload after unusable {} output. streamFinishReason={}, retryMaxTokens={}",
                    payloadType.getSimpleName(),
                    firstTelemetry.streamFinishReason(),
                    retryMaxTokens);
            String retryContent = chatCompletionWithOverrides(systemPrompt, userPrompt, false, retryMaxTokens);
            T retryOutput = parseModelStagePayload(retryContent, payloadType);
            if (retryOutput != null) {
                lastCallTelemetry.set(lastCallTelemetry.get().withFallbackRetryUsed(true));
                lastStructuredRetrySourceTelemetry.set(firstTelemetry);
                return retryOutput;
            }
            lastCallTelemetry.set(firstTelemetry);
            lastModelStageRawContent.set(firstRawContent);
            return null;
        } catch (IOException | InterruptedException exception) {
            lastCallTelemetry.set(firstTelemetry);
            lastModelStageRawContent.set(firstRawContent);
            throw exception;
        }
    }

    private boolean shouldRetryStructuredPayload(Class<?> payloadType,
                                                 ExternalModelCallTelemetry telemetry,
                                                 String rawContent) {
        if (payloadType != ExternalModelStagePayloads.CombinedOutput.class) {
            return false;
        }
        if (!structuredRetryEnabled) {
            return false;
        }
        String finishReason = telemetry == null ? "" : defaultIfBlank(telemetry.streamFinishReason(), "");
        return "length".equalsIgnoreCase(finishReason) || cleanupAiText(rawContent).contains("\"diagnosisDecision\"");
    }

    private SubmissionAnalysisResponse buildRuntimeAnalysisResponse(
            SubmissionAnalysisResponse fallback,
            ExternalModelAgentRuntime.RuntimePlan runtimePlan,
            ExternalModelStagePayloads.DiagnosisJudgeOutput decision,
            ExternalModelStagePayloads.TeachingHintOutput teachingHint,
            SubmissionAnalysisResponse.StudentFeedback studentFeedback,
            String rawSourceCode,
            List<SubmissionAnalysisResponse.LineIssue> baselineLineIssues) {
        String primaryIssueTag = cleanupAiText(decision.getPrimaryIssueTag()).toUpperCase();
        String fineGrainedTag = cleanupAiText(decision.getFineGrainedTag()).toUpperCase();
        String studentHint = defaultIfBlank(teachingHint.getStudentHint(), fallback.getStudentHint());
        String teacherNote = defaultIfBlank(teachingHint.getTeacherNote(), fallback.getTeacherNote());
        List<String> evidenceRefs = cleanList(decision.getEvidenceRefs(), fallback.getEvidenceRefs());
        List<String> runtimeFocusPoints = buildRuntimeFocusPoints(primaryIssueTag, fineGrainedTag, teachingHint);
        List<String> runtimeFixDirections = buildRuntimeFixDirections(teachingHint, primaryIssueTag, fineGrainedTag);
        ExternalModelStagePayloads.TeachingHintOutput alignedTeachingHint =
                alignTeachingHintWithDiagnosis(teachingHint, primaryIssueTag, fineGrainedTag, evidenceRefs);
        SubmissionAnalysisResponse.StudentFeedback resolvedStudentFeedback = resolveModelStudentFeedback(
                studentFeedback,
                decision,
                alignedTeachingHint,
                primaryIssueTag,
                fineGrainedTag,
                evidenceRefs
        );

        return SubmissionAnalysisResponse.builder()
                .submissionId(fallback.getSubmissionId())
                .analysisSchemaVersion(fallback.getAnalysisSchemaVersion())
                .evidenceSchemaVersion(fallback.getEvidenceSchemaVersion())
                .taxonomyVersion(fallback.getTaxonomyVersion())
                .sourceType(AI_SOURCE)
                .scenario(fallback.getScenario())
                .headline(defaultIfBlank(fallback.getHeadline(), "AI 阶段化诊断已完成"))
                .summary(defaultIfBlank(fallback.getSummary(), buildRuntimeSummary(primaryIssueTag, fineGrainedTag, decision)))
                .issueTags(cleanList(List.of(primaryIssueTag), fallback.getIssueTags()))
                .fineGrainedTags(fineGrainedTag.isBlank()
                        ? cleanList(List.of(), fallback.getFineGrainedTags())
                        : cleanList(List.of(fineGrainedTag), fallback.getFineGrainedTags()))
                .abilityPoints(cleanList(fallback.getAbilityPoints(), List.of()))
                .focusPoints(cleanList(runtimeFocusPoints, fallback.getFocusPoints()))
                .fixDirections(cleanList(runtimeFixDirections, fallback.getFixDirections()))
                .evidenceRefs(evidenceRefs)
                .studentHint(studentHint)
                .studentHintPlan(alignedTeachingHint.getStudentHintPlan() == null
                        ? fallback.getStudentHintPlan()
                        : alignedTeachingHint.getStudentHintPlan())
                .studentFeedback(resolvedStudentFeedback)
                .learningInterventionPlan(alignedTeachingHint.getLearningInterventionPlan() == null
                        ? fallback.getLearningInterventionPlan()
                        : alignedTeachingHint.getLearningInterventionPlan())
                .learningActionEvidence(fallback.getLearningActionEvidence())
                .teacherNote(teacherNote)
                .progressSignal(fallback.getProgressSignal())
                .confidence(resolveConfidence(decision.getConfidence(), fallback.getConfidence()))
                .uncertainty(defaultIfBlank(decision.getUncertainty(), fallback.getUncertainty()))
                .diagnosticTrace(fallback.getDiagnosticTrace())
                .modelEducationTrace(modelEducationTrace(decision))
                .aiInvocation(modelInvocation(fallback, "MODEL_COMPLETED", false, RUNTIME_PROMPT_VERSION))
                .answerLeakRisk(resolveRuntimeAnswerLeakRisk(alignedTeachingHint, fallback.getAnswerLeakRisk()))
                .wrongSolution(fallback.getWrongSolution())
                .correctSolution(fallback.getCorrectSolution())
                .lineIssues(baselineLineIssues == null ? List.of() : baselineLineIssues)
                .firstFailedCase(fallback.getFirstFailedCase())
                .reportMarkdown(buildRuntimeReportMarkdown(runtimePlan, decision, alignedTeachingHint, rawSourceCode))
                .generatedAt(fallback.getGeneratedAt())
                .build();
    }

    private SubmissionAnalysisResponse.ModelEducationTrace modelEducationTrace(
            ExternalModelStagePayloads.DiagnosisJudgeOutput decision) {
        if (decision == null) {
            return null;
        }
        return SubmissionAnalysisResponse.ModelEducationTrace.builder()
                .source("diagnosisDecision")
                .primaryIssueTag(cleanupAiText(decision.getPrimaryIssueTag()).toUpperCase())
                .fineGrainedTag(cleanupAiText(decision.getFineGrainedTag()).toUpperCase())
                .evidenceRefs(cleanList(decision.getEvidenceRefs(), List.of()))
                .primaryReasoning(cleanupAiText(decision.getPrimaryReasoning()))
                .secondaryIssues(modelEducationIssueNotes(decision.getSecondaryIssues()))
                .distractorNotes(modelEducationIssueNotes(decision.getDistractorNotes()))
                .teachingPriority(cleanupAiText(decision.getTeachingPriority()))
                .improvementCategories(modelImprovementCategories(decision.getImprovementOpportunities()))
                .nextLearningAction(decision.getNextLearningAction() == null
                        ? ""
                        : cleanupAiText(decision.getNextLearningAction().getTask()))
                .nextLearningActionEvidenceRefs(decision.getNextLearningAction() == null
                        ? List.of()
                        : cleanList(decision.getNextLearningAction().getEvidenceRefs(), List.of()))
                .confidence(decision.getConfidence())
                .uncertainty(cleanupAiText(decision.getUncertainty()))
                .needsMoreEvidence(decision.getNeedsMoreEvidence())
                .answerLeakRisk(cleanupAiText(decision.getAnswerLeakRisk()).toUpperCase())
                .build();
    }

    private List<SubmissionAnalysisResponse.ModelEducationIssueNote> modelEducationIssueNotes(
            List<ExternalModelStagePayloads.EducationIssueNote> notes) {
        if (notes == null || notes.isEmpty()) {
            return List.of();
        }
        return notes.stream()
                .filter(note -> note != null)
                .map(note -> SubmissionAnalysisResponse.ModelEducationIssueNote.builder()
                        .title(cleanupAiText(note.getTitle()))
                        .message(cleanupAiText(note.getMessage()))
                        .issueTag(cleanupAiText(note.getIssueTag()).toUpperCase())
                        .fineGrainedTag(cleanupAiText(note.getFineGrainedTag()).toUpperCase())
                        .evidenceRefs(cleanList(note.getEvidenceRefs(), List.of()))
                        .build())
                .toList();
    }

    private List<String> modelImprovementCategories(
            List<SubmissionAnalysisResponse.ImprovementOpportunity> opportunities) {
        if (opportunities == null || opportunities.isEmpty()) {
            return List.of();
        }
        return opportunities.stream()
                .filter(item -> item != null)
                .map(item -> cleanupAiText(item.getCategory()).toUpperCase())
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    private ExternalModelStagePayloads.TeachingHintOutput alignTeachingHintWithDiagnosis(
            ExternalModelStagePayloads.TeachingHintOutput teachingHint,
            String primaryIssueTag,
            String fineGrainedTag,
            List<String> evidenceRefs) {
        if (teachingHint == null) {
            return ExternalModelStagePayloads.TeachingHintOutput.builder().build();
        }
        String teachingTag = fineGrainedTag == null || fineGrainedTag.isBlank() ? primaryIssueTag : fineGrainedTag;
        String expectedAction = localTeachingAction(teachingTag);
        SubmissionAnalysisResponse.StudentHintPlan alignedHintPlan =
                alignStudentHintPlan(teachingHint.getStudentHintPlan(), teachingTag, expectedAction, evidenceRefs);
        SubmissionAnalysisResponse.LearningInterventionPlan alignedInterventionPlan =
                alignInterventionPlan(teachingHint.getLearningInterventionPlan(), teachingTag, expectedAction, evidenceRefs);
        return ExternalModelStagePayloads.TeachingHintOutput.builder()
                .studentHint(teachingHint.getStudentHint())
                .studentHintPlan(alignedHintPlan)
                .learningInterventionPlan(alignedInterventionPlan)
                .teacherNote(teachingHint.getTeacherNote())
                .answerLeakRisk(teachingHint.getAnswerLeakRisk())
                .build();
    }

    private ExternalModelStagePayloads.TeachingHintOutput buildModelActionTeachingHintFromDecision(
            ExternalModelStagePayloads.DiagnosisJudgeOutput decision,
            ExternalModelAgentRuntime.RuntimePlan runtimePlan,
            SubmissionAnalysisResponse fallback) {
        if (decision == null) {
            return ExternalModelStagePayloads.TeachingHintOutput.builder().build();
        }
        String primaryIssueTag = cleanupAiText(decision.getPrimaryIssueTag()).toUpperCase();
        String fineGrainedTag = cleanupAiText(decision.getFineGrainedTag()).toUpperCase();
        String teachingTag = fineGrainedTag.isBlank() ? primaryIssueTag : fineGrainedTag;
        String expectedAction = localTeachingAction(teachingTag);
        String label = localTagLabel(teachingTag);
        List<String> evidenceRefs = cleanList(decision.getEvidenceRefs(), fallback == null ? List.of() : fallback.getEvidenceRefs());
        String evidenceAnchor = evidenceRefs.isEmpty() ? "model:diagnosis_decision" : evidenceRefs.get(0);
        SubmissionAnalysisResponse.NextLearningAction modelAction = decision.getNextLearningAction();
        String task = defaultIfBlank(modelAction == null ? "" : modelAction.getTask(),
                localNextAction(teachingTag, expectedAction));
        String checkQuestion = defaultIfBlank(modelAction == null ? "" : modelAction.getCheckQuestion(),
                localCoachQuestion(teachingTag, expectedAction));
        String hintLevel = defaultIfBlank(modelAction == null ? "" : modelAction.getHintLevel(), "L2");
        List<String> actionRefs = cleanList(
                modelAction == null ? List.of() : modelAction.getEvidenceRefs(),
                evidenceRefs
        );
        String risk = resolveAnswerLeakRisk(modelAction == null ? "" : modelAction.getAnswerLeakRisk(), "LOW");
        String hint = defaultIfBlank(decision.getTeachingPriority(),
                "先围绕「" + label + "」做一次可观察验证。");
        return ExternalModelStagePayloads.TeachingHintOutput.builder()
                .studentHint(hint)
                .studentHintPlan(SubmissionAnalysisResponse.StudentHintPlan.builder()
                        .hintLevel(hintLevel)
                        .problemType(label)
                        .evidenceAnchor(evidenceAnchor)
                        .nextAction(task)
                        .coachQuestion(checkQuestion)
                        .teachingAction(expectedAction)
                        .evidenceRefs(actionRefs)
                        .answerLeakRisk(risk)
                        .build())
                .learningInterventionPlan(SubmissionAnalysisResponse.LearningInterventionPlan.builder()
                        .interventionType(localInterventionType(expectedAction))
                        .goal(defaultIfBlank(decision.getPrimaryReasoning(), "把模型判断转化为一个可验证的小动作。"))
                        .studentTask(task)
                        .checkQuestion(checkQuestion)
                        .completionSignal(localCompletionSignal(teachingTag, expectedAction))
                        .evidenceRefs(actionRefs)
                        .estimatedMinutes(6)
                        .answerLeakRisk(risk)
                        .build())
                .teacherNote("外部模型采用轻量单调用协议，学生提示由模型下一步学习动作派生。")
                .answerLeakRisk(risk)
                .build();
    }

    private SubmissionAnalysisResponse.StudentFeedback resolveModelStudentFeedback(
            SubmissionAnalysisResponse.StudentFeedback modelFeedback,
            ExternalModelStagePayloads.DiagnosisJudgeOutput decision,
            ExternalModelStagePayloads.TeachingHintOutput teachingHint,
            String primaryIssueTag,
            String fineGrainedTag,
            List<String> evidenceRefs) {
        if (modelFeedback != null
                && modelFeedback.getBlockingIssues() != null
                && !modelFeedback.getBlockingIssues().isEmpty()
                && modelFeedback.getNextLearningAction() != null) {
            return modelFeedback;
        }
        if (!hasEducationAgentJudgment(decision)) {
            return modelFeedback;
        }
        String teachingTag = fineGrainedTag == null || fineGrainedTag.isBlank() ? primaryIssueTag : fineGrainedTag;
        String label = localTagLabel(teachingTag);
        List<String> refs = cleanList(evidenceRefs, List.of());
        String evidence = refs.isEmpty()
                ? "外接模型基于判题证据和代码片段做出判断。"
                : "外接模型引用证据：" + String.join(", ", refs);
        SubmissionAnalysisResponse.NextLearningAction nextAction = decision.getNextLearningAction();
        String modelNextAction = defaultIfBlank(
                nextAction == null ? "" : nextAction.getTask(),
                teachingHint == null || teachingHint.getStudentHintPlan() == null
                        ? localNextAction(teachingTag, localTeachingAction(teachingTag))
                        : teachingHint.getStudentHintPlan().getNextAction()
        );
        return SubmissionAnalysisResponse.StudentFeedback.builder()
                .summary(defaultIfBlank(decision.getTeachingPriority(),
                        "先处理外接模型判断的当前主错因：" + label + "。"))
                .blockingIssues(List.of(SubmissionAnalysisResponse.FeedbackIssue.builder()
                        .priority(1)
                        .title("当前最需要先处理的问题")
                        .studentMessage(defaultIfBlank(decision.getPrimaryReasoning(),
                                "外接模型判断当前最需要先处理的是“" + label + "”。"))
                        .evidence(evidence)
                        .nextAction(modelNextAction)
                        .issueTag(primaryIssueTag)
                        .fineGrainedTag(fineGrainedTag == null || fineGrainedTag.isBlank() ? null : fineGrainedTag)
                        .evidenceRefs(refs)
                        .build()))
                .secondaryIssues(toStudentSecondaryIssues(decision.getSecondaryIssues(), refs))
                .improvementOpportunities(cleanImprovementOpportunities(decision.getImprovementOpportunities()))
                .nextLearningAction(nextAction == null
                        ? SubmissionAnalysisResponse.NextLearningAction.builder()
                        .hintLevel("L2")
                        .action(localTeachingAction(teachingTag))
                        .task(modelNextAction)
                        .checkQuestion(localCoachQuestion(teachingTag, localTeachingAction(teachingTag)))
                        .evidenceRefs(refs)
                        .answerLeakRisk("LOW")
                        .build()
                        : nextAction)
                .build();
    }

    private List<SubmissionAnalysisResponse.ImprovementOpportunity> cleanImprovementOpportunities(
            List<SubmissionAnalysisResponse.ImprovementOpportunity> opportunities) {
        if (opportunities == null || opportunities.isEmpty()) {
            return List.of();
        }
        return opportunities.stream()
                .filter(item -> item != null)
                .toList();
    }

    private boolean hasEducationAgentJudgment(ExternalModelStagePayloads.DiagnosisJudgeOutput decision) {
        if (decision == null) {
            return false;
        }
        return !cleanupAiText(decision.getPrimaryReasoning()).isBlank()
                || !cleanupAiText(decision.getTeachingPriority()).isBlank()
                || decision.getNextLearningAction() != null
                || decision.getImprovementOpportunities() != null && !decision.getImprovementOpportunities().isEmpty()
                || decision.getSecondaryIssues() != null && !decision.getSecondaryIssues().isEmpty();
    }

    private List<SubmissionAnalysisResponse.SecondaryIssue> toStudentSecondaryIssues(
            List<ExternalModelStagePayloads.EducationIssueNote> notes,
            List<String> fallbackRefs) {
        if (notes == null || notes.isEmpty()) {
            return List.of();
        }
        return notes.stream()
                .filter(note -> note != null && !cleanupAiText(note.getMessage()).isBlank())
                .limit(2)
                .map(note -> SubmissionAnalysisResponse.SecondaryIssue.builder()
                        .title(defaultIfBlank(note.getTitle(), "可能的次要问题"))
                        .studentMessage(note.getMessage())
                        .whyNotPrimary("外接模型将它作为次要信号，不应压过当前第一失败证据。")
                        .issueTag(note.getIssueTag())
                        .evidenceRefs(cleanList(note.getEvidenceRefs(), fallbackRefs))
                        .build())
                .toList();
    }

    private SubmissionAnalysisResponse.StudentHintPlan alignStudentHintPlan(
            SubmissionAnalysisResponse.StudentHintPlan plan,
            String teachingTag,
            String expectedAction,
            List<String> evidenceRefs) {
        if (plan == null) {
            return null;
        }
        String currentAction = cleanupAiText(plan.getTeachingAction()).toUpperCase();
        boolean needsAlignment = currentAction.isBlank()
                || "COLLECT_EVIDENCE".equals(currentAction) && !"NEEDS_MORE_EVIDENCE".equals(teachingTag)
                || !expectedAction.equals(currentAction) && directTeachingActionRequired(teachingTag);
        if (!needsAlignment) {
            return plan;
        }
        String nextAction = localNextAction(teachingTag, expectedAction);
        String coachQuestion = localCoachQuestion(teachingTag, expectedAction);
        List<String> alignedRefs = evidenceRefs == null || evidenceRefs.isEmpty()
                ? cleanList(plan.getEvidenceRefs(), List.of())
                : evidenceRefs;
        return SubmissionAnalysisResponse.StudentHintPlan.builder()
                .hintLevel(defaultIfBlank(plan.getHintLevel(), "L2"))
                .problemType(defaultIfBlank(plan.getProblemType(), localTagLabel(teachingTag)))
                .evidenceAnchor(defaultIfBlank(plan.getEvidenceAnchor(),
                        alignedRefs.isEmpty() ? "" : alignedRefs.get(0)))
                .nextAction(nextAction)
                .coachQuestion(coachQuestion)
                .teachingAction(expectedAction)
                .evidenceRefs(alignedRefs)
                .answerLeakRisk(resolveAnswerLeakRisk(plan.getAnswerLeakRisk(), "LOW"))
                .build();
    }

    private SubmissionAnalysisResponse.LearningInterventionPlan alignInterventionPlan(
            SubmissionAnalysisResponse.LearningInterventionPlan plan,
            String teachingTag,
            String expectedAction,
            List<String> evidenceRefs) {
        if (plan == null || "COLLECT_EVIDENCE".equals(expectedAction)) {
            return plan;
        }
        String currentType = cleanupAiText(plan.getInterventionType()).toUpperCase();
        String expectedType = localInterventionType(expectedAction);
        if (currentType.equals(expectedType)) {
            return plan;
        }
        List<String> alignedRefs = evidenceRefs == null || evidenceRefs.isEmpty()
                ? cleanList(plan.getEvidenceRefs(), List.of())
                : evidenceRefs;
        return SubmissionAnalysisResponse.LearningInterventionPlan.builder()
                .interventionType(expectedType)
                .goal(defaultIfBlank(plan.getGoal(), "把诊断结论转化为一个可观察的小动作。"))
                .studentTask(localNextAction(teachingTag, expectedAction))
                .checkQuestion(localCoachQuestion(teachingTag, expectedAction))
                .completionSignal(defaultIfBlank(plan.getCompletionSignal(),
                        "学生能用证据说明自己验证了当前判断。"))
                .evidenceRefs(alignedRefs)
                .estimatedMinutes(plan.getEstimatedMinutes() == null ? 6 : plan.getEstimatedMinutes())
                .answerLeakRisk(resolveAnswerLeakRisk(plan.getAnswerLeakRisk(), "LOW"))
                .build();
    }

    private boolean directTeachingActionRequired(String teachingTag) {
        String normalized = cleanupAiText(teachingTag).toUpperCase();
        return switch (normalized) {
            case "TIME_COMPLEXITY", "BRUTE_FORCE_LIMIT", "OVER_SIMULATION", "MAX_BOUNDARY",
                    "OUTPUT_FORMAT_DETAIL", "INPUT_PARSING", "IO_FORMAT",
                    "EMPTY_INPUT", "IN_PLACE_STATE_PROGRESS", "SAMPLE_OVERFIT", "SAMPLE_ONLY" -> true;
            default -> false;
        };
    }

    private String localInterventionType(String teachingAction) {
        return switch (teachingAction == null ? "" : teachingAction) {
            case "TRACE_VARIABLES" -> "VARIABLE_TRACE";
            case "COMPARE_INPUT_SPEC", "COMPARE_OUTPUT" -> "IO_COMPARE";
            case "COUNT_COMPLEXITY" -> "COMPLEXITY_ESTIMATE";
            case "ASK_MIN_CASE" -> "MIN_CASE";
            case "TRACE_STATE" -> "MIN_CASE_TRACE";
            case "DEFINE_STATE" -> "STATE_EXPLANATION";
            case "CHECK_INVARIANT", "BUILD_COUNTEREXAMPLE" -> "COUNTEREXAMPLE";
            case "CHECK_RUNTIME_GUARDS" -> "RUNTIME_GUARD_CHECK";
            case "COMPARE_SUBMISSIONS" -> "COMPARE_SUBMISSIONS";
            default -> "COLLECT_EVIDENCE";
        };
    }

    private String resolveRuntimeAnswerLeakRisk(ExternalModelStagePayloads.TeachingHintOutput teachingHint,
                                                String fallbackRisk) {
        String visibleRisk = higherRisk(
                teachingHint == null ? "" : teachingHint.getAnswerLeakRisk(),
                higherRisk(
                        teachingHint == null || teachingHint.getStudentHintPlan() == null
                                ? ""
                                : teachingHint.getStudentHintPlan().getAnswerLeakRisk(),
                        teachingHint == null || teachingHint.getLearningInterventionPlan() == null
                                ? ""
                                : teachingHint.getLearningInterventionPlan().getAnswerLeakRisk()
                )
        );
        String normalizedVisibleRisk = resolveAnswerLeakRisk(visibleRisk, "");
        if (!normalizedVisibleRisk.isBlank() && !"UNKNOWN".equalsIgnoreCase(normalizedVisibleRisk)) {
            return normalizedVisibleRisk;
        }
        return resolveAnswerLeakRisk("", fallbackRisk);
    }

    private List<String> buildRuntimeFocusPoints(String primaryIssueTag,
                                                 String fineGrainedTag,
                                                 ExternalModelStagePayloads.TeachingHintOutput teachingHint) {
        List<String> focusPoints = new ArrayList<>();
        String selectedTag = fineGrainedTag == null || fineGrainedTag.isBlank() ? primaryIssueTag : fineGrainedTag;
        if (selectedTag != null && !selectedTag.isBlank()) {
            focusPoints.add(localTagLabel(selectedTag));
        }
        SubmissionAnalysisResponse.StudentHintPlan plan =
                teachingHint == null ? null : teachingHint.getStudentHintPlan();
        if (plan != null) {
            addIfUseful(focusPoints, plan.getProblemType());
            addIfUseful(focusPoints, plan.getEvidenceAnchor());
        }
        return focusPoints;
    }

    private List<String> buildRuntimeFixDirections(ExternalModelStagePayloads.TeachingHintOutput teachingHint,
                                                   String primaryIssueTag,
                                                   String fineGrainedTag) {
        List<String> directions = new ArrayList<>();
        SubmissionAnalysisResponse.StudentHintPlan hintPlan =
                teachingHint == null ? null : teachingHint.getStudentHintPlan();
        SubmissionAnalysisResponse.LearningInterventionPlan interventionPlan =
                teachingHint == null ? null : teachingHint.getLearningInterventionPlan();
        if (hintPlan != null) {
            addIfUseful(directions, hintPlan.getNextAction());
            addIfUseful(directions, hintPlan.getCoachQuestion());
        }
        if (interventionPlan != null) {
            addIfUseful(directions, interventionPlan.getStudentTask());
            addIfUseful(directions, interventionPlan.getCheckQuestion());
        }
        if (directions.isEmpty()) {
            String selectedTag = fineGrainedTag == null || fineGrainedTag.isBlank() ? primaryIssueTag : fineGrainedTag;
            String teachingAction = localTeachingAction(selectedTag);
            directions.add(localNextAction(selectedTag, teachingAction));
        }
        return directions;
    }

    private void addIfUseful(List<String> values, String candidate) {
        String cleaned = cleanupAiText(candidate);
        if (!cleaned.isBlank()) {
            values.add(cleaned);
        }
    }

    private SubmissionAnalysisResponse buildPartialRuntimeAnalysisResponse(
            SubmissionAnalysisResponse fallback,
            ExternalModelAgentRuntime.RuntimePlan runtimePlan,
            ExternalModelStagePayloads.DiagnosisJudgeOutput decision,
            ExternalModelStagePayloads.StageValidationResult teachingFailure,
            String rawSourceCode,
            List<SubmissionAnalysisResponse.LineIssue> baselineLineIssues) {
        ExternalModelStagePayloads.TeachingHintOutput localTeachingHint =
                buildLocalTeachingHintFromDecision(decision, runtimePlan, teachingFailure, fallback);
        SubmissionAnalysisResponse response = buildRuntimeAnalysisResponse(
                fallback,
                runtimePlan,
                decision,
                localTeachingHint,
                null,
                rawSourceCode,
                baselineLineIssues
        );
        response.setAiInvocation(modelInvocation(
                fallback,
                "MODEL_PARTIAL_COMPLETED",
                false,
                runtimePromptVersion(runtimePlan),
                teachingFailure
        ));
        response.setUncertainty(appendFailureNote(
                response.getUncertainty(),
                "教学提示阶段由本地安全模板补齐",
                teachingFailure
        ));
        response.setReportMarkdown(response.getReportMarkdown()
                + "\n\n## 模型调用说明\n\n"
                + "- 错因裁决来自外部模型。\n"
                + "- 教学表达阶段未完成，已使用本地安全教学模板补齐。"
                + failureSummary(teachingFailure));
        return response;
    }

    private boolean useSingleCallRuntime() {
        return RUNTIME_MODE_SINGLE_CALL.equalsIgnoreCase(cleanupAiText(externalRuntimeMode));
    }

    private void activateRuntimePlan(ExternalModelAgentRuntime.RuntimePlan runtimePlan) {
        if (runtimePlan == null) {
            nextRequestContext.set(ExternalModelRequestContext.standard());
            lastCallTelemetry.set(ExternalModelCallTelemetry.empty());
            return;
        }
        ExternalModelRequestContext context = new ExternalModelRequestContext(
                runtimePlan.getRuntimeProfile(),
                runtimePlan.isRequestCompact()
        );
        nextRequestContext.set(context);
        lastCallTelemetry.set(ExternalModelCallTelemetry.request(
                context.runtimeProfile(),
                context.requestCompact(),
                0
        ));
    }

    private String activeRuntimePromptVersion() {
        if (!useSingleCallRuntime()) {
            return RUNTIME_PROMPT_VERSION;
        }
        String configured = cleanupAiText(externalSingleCallPromptVersion);
        return configured.isBlank() ? SINGLE_CALL_RUNTIME_PROMPT_VERSION : configured;
    }

    private String runtimePromptVersion(ExternalModelAgentRuntime.RuntimePlan runtimePlan) {
        if (!useSingleCallRuntime()) {
            return RUNTIME_PROMPT_VERSION;
        }
        if (runtimePlan != null && runtimePlan.getSingleCallPrompt() != null
                && !cleanupAiText(runtimePlan.getSingleCallPrompt().getVersion()).isBlank()) {
            return cleanupAiText(runtimePlan.getSingleCallPrompt().getVersion());
        }
        return activeRuntimePromptVersion();
    }

    private ExternalModelStagePayloads.TeachingHintOutput buildLocalTeachingHintFromDecision(
            ExternalModelStagePayloads.DiagnosisJudgeOutput decision,
            ExternalModelAgentRuntime.RuntimePlan runtimePlan,
            ExternalModelStagePayloads.StageValidationResult teachingFailure,
            SubmissionAnalysisResponse fallback) {
        String primaryIssueTag = cleanupAiText(decision.getPrimaryIssueTag()).toUpperCase();
        String fineGrainedTag = cleanupAiText(decision.getFineGrainedTag()).toUpperCase();
        String teachingTag = fineGrainedTag.isBlank() ? primaryIssueTag : fineGrainedTag;
        String teachingAction = localTeachingAction(teachingTag);
        List<String> evidenceRefs = cleanList(decision.getEvidenceRefs(), fallback == null ? List.of() : fallback.getEvidenceRefs());
        String label = localTagLabel(teachingTag);
        String evidenceAnchor = evidenceRefs.isEmpty() ? "model:diagnosis_decision" : evidenceRefs.get(0);
        String nextAction = localNextAction(teachingTag, teachingAction);
        String coachQuestion = localCoachQuestion(teachingTag, teachingAction);
        String hint = "先不要大改代码。把当前问题当作「" + label + "」来验证：" + nextAction;

        return ExternalModelStagePayloads.TeachingHintOutput.builder()
                .studentHint(hint)
                .studentHintPlan(SubmissionAnalysisResponse.StudentHintPlan.builder()
                        .hintLevel("L2")
                        .problemType(label)
                        .evidenceAnchor(evidenceAnchor)
                        .nextAction(nextAction)
                        .coachQuestion(coachQuestion)
                        .teachingAction(teachingAction)
                        .evidenceRefs(evidenceRefs)
                        .answerLeakRisk("LOW")
                        .build())
                .learningInterventionPlan(SubmissionAnalysisResponse.LearningInterventionPlan.builder()
                        .interventionType(localInterventionType(teachingAction))
                        .goal("把外部模型判断转化为一个可验证的小动作。")
                        .studentTask(nextAction)
                        .checkQuestion(coachQuestion)
                        .completionSignal(localCompletionSignal(teachingTag, teachingAction))
                        .evidenceRefs(evidenceRefs)
                        .estimatedMinutes(6)
                        .answerLeakRisk("LOW")
                        .build())
                .teacherNote("外部模型已完成错因裁决；教学表达阶段失败，系统使用本地安全模板补齐。"
                        + failureSummary(teachingFailure))
                .answerLeakRisk("LOW")
                .build();
    }

    private String buildRuntimeSummary(String primaryIssueTag,
                                       String fineGrainedTag,
                                       ExternalModelStagePayloads.DiagnosisJudgeOutput decision) {
        String finePart = fineGrainedTag == null || fineGrainedTag.isBlank() ? "" : " / " + fineGrainedTag;
        return "外部模型裁决的主要错因是 " + primaryIssueTag + finePart
                + "，置信度 " + (decision.getConfidence() == null ? "未知" : decision.getConfidence())
                + "。";
    }

    private String localTagLabel(String tag) {
        return switch (tag == null ? "" : tag) {
            case "OFF_BY_ONE", "LOOP_BOUNDARY" -> "循环边界";
            case "INPUT_PARSING", "IO_FORMAT" -> "输入读取";
            case "OUTPUT_FORMAT_DETAIL" -> "输出格式";
            case "TIME_COMPLEXITY", "BRUTE_FORCE_LIMIT", "OVER_SIMULATION" -> "复杂度";
            case "MAX_BOUNDARY" -> "最大规模边界";
            case "EMPTY_INPUT", "BOUNDARY_CONDITION" -> "边界条件";
            case "STATE_RESET", "INITIAL_STATE", "VARIABLE_INITIALIZATION" -> "状态初始化或重置";
            case "DP_STATE_DESIGN", "STATE_TRANSITION", "IN_PLACE_STATE_PROGRESS" -> "状态设计";
            case "GREEDY_ASSUMPTION", "ALGORITHM_STRATEGY" -> "算法策略";
            case "RUNTIME_STABILITY" -> "运行稳定性";
            case "SAMPLE_OVERFIT", "SAMPLE_ONLY" -> "样例泛化";
            case "PARTIAL_FIX_REGRESSION" -> "局部修复回退";
            default -> "证据不足";
        };
    }

    private String localTeachingAction(String tag) {
        return switch (tag == null ? "" : tag) {
            case "OFF_BY_ONE", "LOOP_BOUNDARY" -> "TRACE_VARIABLES";
            case "INPUT_PARSING", "IO_FORMAT" -> "COMPARE_INPUT_SPEC";
            case "OUTPUT_FORMAT_DETAIL" -> "COMPARE_OUTPUT";
            case "TIME_COMPLEXITY", "BRUTE_FORCE_LIMIT", "OVER_SIMULATION", "MAX_BOUNDARY" -> "COUNT_COMPLEXITY";
            case "EMPTY_INPUT", "BOUNDARY_CONDITION" -> "ASK_MIN_CASE";
            case "STATE_RESET", "INITIAL_STATE", "VARIABLE_INITIALIZATION" -> "TRACE_STATE";
            case "DP_STATE_DESIGN", "STATE_TRANSITION", "IN_PLACE_STATE_PROGRESS" -> "DEFINE_STATE";
            case "GREEDY_ASSUMPTION", "ALGORITHM_STRATEGY" -> "CHECK_INVARIANT";
            case "RUNTIME_STABILITY" -> "CHECK_RUNTIME_GUARDS";
            case "SAMPLE_OVERFIT", "SAMPLE_ONLY" -> "BUILD_COUNTEREXAMPLE";
            case "PARTIAL_FIX_REGRESSION" -> "COMPARE_SUBMISSIONS";
            default -> "COLLECT_EVIDENCE";
        };
    }

    private String localNextAction(String tag, String teachingAction) {
        return switch (teachingAction == null ? "" : teachingAction) {
            case "TRACE_VARIABLES" -> "选一个最小样例，列出循环第一次、最后一次以及关键变量的实际取值。";
            case "COMPARE_INPUT_SPEC" -> "把题面每一行输入和代码每一次读取按顺序对齐，找出第一处对不上的位置。";
            case "COMPARE_OUTPUT" -> "逐字比较你的输出和期望输出，只圈出第一处多余、缺失或格式不同的字符。";
            case "COUNT_COMPLEXITY" -> "用题面最大规模估算核心循环或核心操作大约执行多少次。";
            case "ASK_MIN_CASE" -> "构造一个最小边界样例，写出期望输出和当前实际输出。";
            case "TRACE_STATE" -> "列出状态变量在进入循环前、一次循环后、下一组数据前的值。";
            case "DEFINE_STATE" -> "先用一句话说明这个状态表示什么，再用一个小样例验证状态是否包含足够信息。";
            case "CHECK_INVARIANT" -> "构造一个能挑战当前策略的小反例，说明它破坏了哪条假设。";
            case "CHECK_RUNTIME_GUARDS" -> "列出最可能触发越界、空值或除零的输入，并检查代码是否有保护条件。";
            case "BUILD_COUNTEREXAMPLE" -> "构造一个不同于样例的最小输入，验证当前做法是否仍然成立。";
            case "COMPARE_SUBMISSIONS" -> "对比最近两次提交，只记录一个代码变化和一个行为变化。";
            default -> "补充一个最小样例、实际输出或错误信息，用来确认当前判断。";
        };
    }

    private String localCoachQuestion(String tag, String teachingAction) {
        return switch (teachingAction == null ? "" : teachingAction) {
            case "TRACE_VARIABLES" -> "这个最小样例里，第一次偏离预期的变量是哪一个？";
            case "COMPARE_INPUT_SPEC" -> "从题面第几行开始，你的读取次数或读取顺序和要求不一致？";
            case "COMPARE_OUTPUT" -> "你的输出和期望输出第一处不同是什么字符？";
            case "COUNT_COMPLEXITY" -> "当输入取最大值时，最内层操作大约会发生多少次？";
            case "ASK_MIN_CASE" -> "这个边界样例为什么足以暴露当前问题？";
            case "TRACE_STATE" -> "状态变量第一次偏离预期发生在进入哪一轮之前或之后？";
            case "DEFINE_STATE" -> "这个状态是否包含判断答案所需的全部信息？";
            case "CHECK_INVARIANT" -> "什么输入会挑战你当前的选择依据？";
            case "CHECK_RUNTIME_GUARDS" -> "哪一种极端输入最可能触发运行错误？";
            case "BUILD_COUNTEREXAMPLE" -> "这个反例破坏了当前做法的哪条假设？";
            case "COMPARE_SUBMISSIONS" -> "哪一处改动让原本更好的行为变差了？";
            default -> "你下一步准备用哪条证据确认这个判断？";
        };
    }

    private String localCompletionSignal(String tag, String teachingAction) {
        return switch (teachingAction == null ? "" : teachingAction) {
            case "TRACE_VARIABLES", "TRACE_STATE" -> "学生能给出变量或状态表，并指出第一处偏离预期的位置。";
            case "COMPARE_INPUT_SPEC", "COMPARE_OUTPUT" -> "学生能指出第一处输入读取或输出格式差异。";
            case "COUNT_COMPLEXITY" -> "学生能给出最大规模下的数量级估算，并判断当前做法是否可能通过。";
            case "ASK_MIN_CASE", "BUILD_COUNTEREXAMPLE" -> "学生能给出最小样例、期望结果和当前结果的对比。";
            case "DEFINE_STATE" -> "学生能说明状态含义，并用小样例验证状态是否足够。";
            case "CHECK_INVARIANT" -> "学生能给出反例，并说明当前策略的假设在哪里失效。";
            case "CHECK_RUNTIME_GUARDS" -> "学生能指出风险输入和对应保护条件。";
            case "COMPARE_SUBMISSIONS" -> "学生能指出一个代码变化、一个行为变化和受影响样例。";
            default -> "学生能补充一条新的可观察证据。";
        };
    }

    private String appendFailureNote(String base,
                                     String prefix,
                                     ExternalModelStagePayloads.StageValidationResult failure) {
        String note = prefix + failureSummary(failure);
        if (base == null || base.isBlank()) {
            return note;
        }
        return base + " " + note;
    }

    private String failureSummary(ExternalModelStagePayloads.StageValidationResult failure) {
        if (failure == null) {
            return "（失败阶段：UNKNOWN_STAGE；失败原因：UNKNOWN_ERROR）";
        }
        String stage = failure.getStage() == null || failure.getStage().isBlank() ? "UNKNOWN_STAGE" : failure.getStage();
        String reason = failure.getFailureReason() == null ? "UNKNOWN_ERROR" : failure.getFailureReason().name();
        return "（失败阶段：" + stage + "；失败原因：" + reason + "）";
    }

    private String buildRuntimeReportMarkdown(ExternalModelAgentRuntime.RuntimePlan runtimePlan,
                                              ExternalModelStagePayloads.DiagnosisJudgeOutput decision,
                                              ExternalModelStagePayloads.TeachingHintOutput teachingHint,
                                              String rawSourceCode) {
        StringBuilder builder = new StringBuilder();
        builder.append("## AI 阶段化诊断\n\n");
        builder.append("- 错因阶段：")
                .append(decision.getPrimaryIssueTag());
        if (decision.getFineGrainedTag() != null && !decision.getFineGrainedTag().isBlank()) {
            builder.append(" / ").append(decision.getFineGrainedTag());
        }
        builder.append('\n');
        builder.append("- 证据引用：")
                .append(String.join(", ", decision.getEvidenceRefs() == null ? List.of() : decision.getEvidenceRefs()))
                .append('\n');
        builder.append("- 不确定性：")
                .append(defaultIfBlank(decision.getUncertainty(), runtimePlan.getBrief().getUncertainty()))
                .append("\n\n");
        builder.append("## 给学生的下一步\n\n");
        builder.append(defaultIfBlank(teachingHint.getStudentHint(), "先选一个最小样例，验证当前判断是否成立。"));
        List<String> refs = decision.getEvidenceRefs() == null ? List.of() : decision.getEvidenceRefs();
        if (!refs.isEmpty()) {
            builder.append("\n\n## 证据定位\n\n");
            refs.stream()
                    .limit(5)
                    .forEach(ref -> builder.append("- ").append(ref).append('\n'));
        }
        return builder.toString().trim();
    }

    private SubmissionAnalysisResponse runtimeFallback(SubmissionAnalysisResponse fallback,
                                                       ExternalModelStagePayloads.StageValidationResult validationResult) {
        return runtimeFallback(fallback, null, validationResult);
    }

    private SubmissionAnalysisResponse runtimeFallback(SubmissionAnalysisResponse fallback,
                                                       ExternalModelAgentRuntime.RuntimePlan runtimePlan,
                                                       ExternalModelStagePayloads.StageValidationResult validationResult) {
        if (fallback == null) {
            return null;
        }
        ExternalModelStagePayloads.StageValidationResult attributionResult =
                withTransportAttribution(validationResult);
        String reason = attributionResult == null || attributionResult.getFailureReason() == null
                ? ModelStageFailureReason.UNKNOWN_ERROR.name()
                : attributionResult.getFailureReason().name();
        String stage = attributionResult == null || attributionResult.getStage() == null || attributionResult.getStage().isBlank()
                ? "UNKNOWN_STAGE"
                : attributionResult.getStage();
        String message = attributionResult == null ? "" : cleanupAiText(attributionResult.getMessage());
        fallback.setAiInvocation(modelInvocation(
                fallback,
                "MODEL_RUNTIME_FALLBACK",
                true,
                runtimePromptVersion(runtimePlan),
                attributionResult
        ));
        fallback.setUncertainty(defaultIfBlank(
                "外部模型阶段化诊断未通过校验，已使用本地规则兜底。失败阶段：" + stage + "；失败原因：" + reason
                        + (message.isBlank() ? "" : "，" + message),
                fallback.getUncertainty()
        ));
        return fallback;
    }

    private ExternalModelStagePayloads.StageValidationResult withTransportAttribution(
            ExternalModelStagePayloads.StageValidationResult result) {
        if (result == null) {
            return null;
        }
        if (!"length".equalsIgnoreCase(defaultIfBlank(lastCallTelemetry.get().streamFinishReason(), ""))) {
            return withStructuredRetrySourceAttribution(result);
        }
        ModelStageFailureReason reason = result.getFailureReason();
        if (reason != ModelStageFailureReason.EMPTY_RESPONSE
                && reason != ModelStageFailureReason.INVALID_JSON
                && reason != ModelStageFailureReason.UNKNOWN_ERROR) {
            return result;
        }
        return ExternalModelStagePayloads.StageValidationResult.builder()
                .valid(false)
                .stage(result.getStage())
                .failureReason(ModelStageFailureReason.OUTPUT_TRUNCATED)
                .message(defaultIfBlank(result.getMessage(), "External model output ended with finish_reason=length."))
                .build();
    }

    private ExternalModelStagePayloads.StageValidationResult withStructuredRetrySourceAttribution(
            ExternalModelStagePayloads.StageValidationResult result) {
        ExternalModelCallTelemetry sourceTelemetry = lastStructuredRetrySourceTelemetry.get();
        if (!"length".equalsIgnoreCase(defaultIfBlank(sourceTelemetry.streamFinishReason(), ""))) {
            return result;
        }
        ModelStageFailureReason reason = result.getFailureReason();
        if (reason != ModelStageFailureReason.EMPTY_RESPONSE
                && reason != ModelStageFailureReason.INVALID_JSON
                && reason != ModelStageFailureReason.UNKNOWN_ERROR) {
            return result;
        }
        lastCallTelemetry.set(sourceTelemetry.withFallbackRetryUsed(true));
        return ExternalModelStagePayloads.StageValidationResult.builder()
                .valid(false)
                .stage(result.getStage())
                .failureReason(ModelStageFailureReason.OUTPUT_TRUNCATED)
                .message(defaultIfBlank(result.getMessage(), "External model structured retry did not recover truncated output."))
                .build();
    }

    private ExternalModelStagePayloads.StageValidationResult withStage(String stage,
                                                                       ExternalModelStagePayloads.StageValidationResult result) {
        if (result == null) {
            return ExternalModelStagePayloads.StageValidationResult.builder()
                    .valid(false)
                    .stage(stage)
                    .failureReason(ModelStageFailureReason.UNKNOWN_ERROR)
                    .message("")
                    .build();
        }
        result.setStage(stage);
        return result;
    }

    private ExternalModelStagePayloads.StageValidationResult stageFailureFromException(String stage,
                                                                                      Exception exception) {
        ModelStageFailureReason reason = failureClassifier.classify(exception);
        return ExternalModelStagePayloads.StageValidationResult.builder()
                .valid(false)
                .stage(stage)
                .failureReason(reason)
                .message(exception == null ? "" : exception.getMessage())
                .build();
    }

    public String enhanceGrowthReportMarkdown(Problem problem,
                                              List<Map<String, Object>> submissionTimeline,
                                              String fallbackMarkdown) {
        if (!canCallAi()) {
            log.info("AI growth report skipped because AI access is unavailable. problemId={}", problem.getId());
            return fallbackMarkdown;
        }
        ExternalModelBudgetGuard.Decision decision = budgetGuard.check(PROVIDER, model);
        if (!decision.allowed()) {
            return appendGrowthReportFailureMarker(fallbackMarkdown, ExternalModelStagePayloads.StageValidationResult.builder()
                    .valid(false)
                    .stage("GROWTH_REPORT")
                    .failureReason(ModelStageFailureReason.BUDGET_GUARD_OPEN)
                    .message(decision.message())
                    .build());
        }

        try {
            Map<String, Object> context = new LinkedHashMap<>();
            context.put("problemTitle", problem.getTitle());
            context.put("problemDescription", problem.getDescription());
            context.put("aiPromptDirection", problem.getAiPromptDirection() == null ? "" : problem.getAiPromptDirection());
            context.put("submissionTimeline", submissionTimeline);
            context.put("fallbackMarkdown", fallbackMarkdown);

            String content = chatCompletion(
                    """
                    你是中文 OJ 成长报告助手。
                    请直接输出 Markdown，不要输出额外解释，不要输出 <think>、思考过程、XML 标签或草稿。

                    报告需要包含：
                    1. 总览
                    2. 错误复盘
                    3. 优化历程
                    4. 学习总结

                    内容要结合提交轨迹，语言简洁、自然、可执行。
                    """,
                    "请基于以下上下文生成成长报告 Markdown：" + objectMapper.writeValueAsString(context)
            );

            String markdown = cleanupAiText(content);
            return markdown.isBlank() ? fallbackMarkdown : markdown;
        } catch (Exception exception) {
            log.error("AI growth report generation failed. problemId={}", problem.getId(), exception);
            return appendGrowthReportFailureMarker(fallbackMarkdown, stageFailureFromException("GROWTH_REPORT", exception));
        }
    }

    private String appendGrowthReportFailureMarker(String fallbackMarkdown,
                                                   ExternalModelStagePayloads.StageValidationResult failure) {
        String base = fallbackMarkdown == null ? "" : fallbackMarkdown;
        return base + "\n\n<!-- AI_FAILURE:" + failureSummary(failure) + " -->";
    }

    private boolean canCallAi() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }

    public String providerName() {
        return PROVIDER;
    }

    public String modelName() {
        return model;
    }

    public String smokeChatCompletion() throws IOException, InterruptedException {
        if (!canCallAi()) {
            throw new IOException("AI is disabled or API key is blank");
        }
        return chatCompletionWithOverrides(
                "You are a production readiness smoke test. Reply with exactly OK.",
                "Return OK.",
                false,
                128
        );
    }

    protected String chatCompletion(String systemPrompt, String userPrompt) throws IOException, InterruptedException {
        ExternalModelRequestContext requestContext = nextRequestContext.get();
        nextRequestContext.set(ExternalModelRequestContext.standard());
        lastCallTelemetry.set(ExternalModelCallTelemetry.request(
                requestContext.runtimeProfile(),
                requestContext.requestCompact(),
                0
        ));
        ExternalModelBudgetGuard.Decision decision = budgetGuard.check(PROVIDER, model);
        if (!decision.allowed()) {
            throw new IOException(decision.message());
        }
        try {
            String content = doChatCompletionWithRetry(systemPrompt, userPrompt, streamEnabled, requestContext);
            budgetGuard.recordSuccess(PROVIDER, model);
            return content;
        } catch (IOException exception) {
            if (!streamEnabled && streamFallbackEnabled && shouldRetryWithStreaming(exception)) {
                log.warn("Retrying AI chat completion with stream=true after non-stream response was unusable. model={}", model);
                String content = doChatCompletionWithRetry(systemPrompt, userPrompt, true, requestContext);
                lastCallTelemetry.set(lastCallTelemetry.get().withFallbackRetryUsed(true));
                budgetGuard.recordSuccess(PROVIDER, model);
                return content;
            }
            recordBudgetFailure(exception);
            throw exception;
        }
    }

    protected String chatCompletionForStudentFeedback(String systemPrompt,
                                                      String userPrompt,
                                                      int outputTokens) throws IOException, InterruptedException {
        return chatCompletionWithOverrides(systemPrompt, userPrompt, streamEnabled, outputTokens);
    }

    private String chatCompletionWithOverrides(String systemPrompt,
                                               String userPrompt,
                                               boolean stream,
                                               int outputTokens) throws IOException, InterruptedException {
        ExternalModelRequestContext requestContext = nextRequestContext.get();
        nextRequestContext.set(ExternalModelRequestContext.standard());
        lastCallTelemetry.set(ExternalModelCallTelemetry.request(
                requestContext.runtimeProfile(),
                requestContext.requestCompact(),
                0
        ));
        ExternalModelBudgetGuard.Decision decision = budgetGuard.check(PROVIDER, model);
        if (!decision.allowed()) {
            throw new IOException(decision.message());
        }
        try {
            String content = doChatCompletionWithRetry(systemPrompt, userPrompt, stream, requestContext, outputTokens);
            budgetGuard.recordSuccess(PROVIDER, model);
            return content;
        } catch (IOException exception) {
            recordBudgetFailure(exception);
            throw exception;
        }
    }

    private String doChatCompletionWithRetry(String systemPrompt,
                                             String userPrompt,
                                             boolean stream,
                                             ExternalModelRequestContext requestContext) throws IOException, InterruptedException {
        return doChatCompletionWithRetry(systemPrompt, userPrompt, stream, requestContext, maxOutputTokens);
    }

    private String doChatCompletionWithRetry(String systemPrompt,
                                             String userPrompt,
                                             boolean stream,
                                             ExternalModelRequestContext requestContext,
                                             int outputTokens) throws IOException, InterruptedException {
        int attempts = Math.max(1, retryMaxAttempts);
        IOException lastException = null;
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                return doChatCompletion(systemPrompt, userPrompt, stream, requestContext, outputTokens);
            } catch (IOException exception) {
                lastException = exception;
                if (attempt >= attempts || !isRetryableCallFailure(exception)) {
                    throw exception;
                }
                long backoff = Math.max(0, retryBackoffMs) * attempt;
                log.warn("Retrying AI chat completion after transient failure. model={}, attempt={}/{}, waitMs={}, reason={}",
                        model,
                        attempt + 1,
                        attempts,
                        backoff,
                        previewBody(exception.getMessage()));
                sleepBeforeRetry(backoff);
            }
        }
        throw lastException == null ? new IOException("AI API call failed") : lastException;
    }

    private String doChatCompletion(String systemPrompt,
                                    String userPrompt,
                                    boolean stream,
                                    ExternalModelRequestContext requestContext) throws IOException, InterruptedException {
        return doChatCompletion(systemPrompt, userPrompt, stream, requestContext, maxOutputTokens);
    }

    private String doChatCompletion(String systemPrompt,
                                    String userPrompt,
                                    boolean stream,
                                    ExternalModelRequestContext requestContext,
                                    int outputTokens) throws IOException, InterruptedException {
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        ));
        requestBody.put("temperature", 0.2);
        requestBody.put("stream", stream);
        requestBody.put("max_tokens", Math.max(128, outputTokens));
        String endpoint = baseUrl.endsWith("/") ? baseUrl + "chat/completions" : baseUrl + "/chat/completions";
        log.info("Calling AI chat completion. model={}, timeoutSeconds={}, stream={}, endpoint={}",
                model,
                Math.max(timeoutSeconds, 5),
                stream,
                endpoint);
        String activeProfile = requestContext.runtimeProfile();
        boolean activeCompact = requestContext.requestCompact();

        String serializedRequest = objectMapper.writeValueAsString(requestBody);
        int requestBytes = serializedRequest.getBytes(StandardCharsets.UTF_8).length;
        lastCallTelemetry.set((stream
                ? ExternalModelCallTelemetry.stream(0, 0, 0, 0, "")
                : ExternalModelCallTelemetry.nonStream("")).withRequestTelemetry(requestBytes, activeProfile, activeCompact));

        String responseBody = sendChatCompletionRequest(serializedRequest, stream);
        String content;
        if (stream) {
            ParsedStreamingContent parsed = extractStreamingChatMessageContent(responseBody);
            content = parsed.content();
            lastCallTelemetry.set(parsed.telemetry().withRequestTelemetry(requestBytes, activeProfile, activeCompact));
        } else {
            JsonNode root = objectMapper.readTree(responseBody);
            content = extractChatMessageContent(root);
            lastCallTelemetry.set(ExternalModelCallTelemetry.nonStream(extractFinishReason(root.path("choices").path(0)))
                    .withRequestTelemetry(requestBytes, activeProfile, activeCompact));
        }
        if (content.isBlank()) {
            log.warn("AI response did not include usable message content. bodyPreview={}",
                    previewBody(responseBody));
            throw new IOException("AI response did not include message content");
        }
        return content;
    }

    protected String sendChatCompletionRequest(String requestBody, boolean stream) throws IOException, InterruptedException {
        String endpoint = baseUrl.endsWith("/") ? baseUrl + "chat/completions" : baseUrl + "/chat/completions";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(Math.max(timeoutSeconds, 5)))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            log.warn("AI chat completion returned non-success status. status={}, bodyPreview={}",
                    response.statusCode(),
                    previewBody(response.body()));
            throw new IOException("AI API returned status " + response.statusCode() + ": " + response.body());
        }
        return response.body();
    }

    private boolean shouldRetryWithStreaming(IOException exception) {
        String message = exception == null || exception.getMessage() == null ? "" : exception.getMessage();
        return message.contains("did not include message content");
    }

    private boolean isRetryableCallFailure(IOException exception) {
        String text = exception == null ? "" : exception.getMessage();
        return failureClassifier.isRetryable(failureClassifier.classify(exception), text);
    }

    protected void recordBudgetFailureForTest(Exception exception) {
        recordBudgetFailure(exception);
    }

    private void recordBudgetFailure(Exception exception) {
        ModelStageFailureReason reason = failureClassifier.classify(exception);
        if (failureClassifier.shouldOpenBudgetGuard(reason)) {
            budgetGuard.recordFailure(PROVIDER, model, reason);
        }
    }

    private void sleepBeforeRetry(long waitMs) throws InterruptedException {
        if (waitMs <= 0) {
            return;
        }
        Thread.sleep(waitMs);
    }

    private String extractChatMessageContent(JsonNode root) {
        if (root == null || root.isMissingNode() || root.isNull()) {
            return "";
        }
        JsonNode firstChoice = root.path("choices").path(0);
        return firstNonBlank(
                extractContentNode(firstChoice.path("message").path("content")),
                extractContentNode(firstChoice.path("delta").path("content")),
                extractContentNode(firstChoice.path("message").path("reasoning_content")),
                extractContentNode(firstChoice.path("delta").path("reasoning_content"))
        );
    }

    private ParsedStreamingContent extractStreamingChatMessageContent(String responseBody) throws IOException {
        if (responseBody == null || responseBody.isBlank()) {
            return new ParsedStreamingContent("", ExternalModelCallTelemetry.stream(0, 0, 0, 0, ""));
        }
        StringBuilder content = new StringBuilder();
        int chunkCount = 0;
        int contentChunkCount = 0;
        int reasoningChunkCount = 0;
        int invalidChunkCount = 0;
        String finishReason = "";
        String[] lines = responseBody.replace("\r\n", "\n").replace('\r', '\n').split("\n");
        for (String line : lines) {
            String trimmed = line == null ? "" : line.trim();
            if (trimmed.isBlank()) {
                continue;
            }
            String payload = trimmed.startsWith("data:") ? trimmed.substring(5).trim() : trimmed;
            if (payload.isBlank() || "[DONE]".equals(payload)) {
                continue;
            }
            chunkCount++;
            if (!payload.startsWith("{")) {
                invalidChunkCount++;
                continue;
            }
            JsonNode root;
            try {
                root = objectMapper.readTree(payload);
            } catch (JsonProcessingException exception) {
                log.debug("Skipping unparsable AI stream chunk. chunkPreview={}", previewBody(payload));
                invalidChunkCount++;
                continue;
            }
            JsonNode choice = root.path("choices").path(0);
            finishReason = firstNonBlank(finishReason, extractFinishReason(choice));
            String reasoningChunk = extractStreamingChoiceReasoning(choice);
            if (!reasoningChunk.isEmpty()) {
                reasoningChunkCount++;
            }
            String chunk = extractStreamingChoiceContent(choice);
            if (!chunk.isEmpty()) {
                content.append(chunk);
                contentChunkCount++;
            }
        }
        return new ParsedStreamingContent(
                content.toString(),
                ExternalModelCallTelemetry.stream(
                        chunkCount,
                        contentChunkCount,
                        reasoningChunkCount,
                        invalidChunkCount,
                        finishReason
                )
        );
    }

    private String extractStreamingChoiceContent(JsonNode firstChoice) {
        String deltaContent = extractContentNode(firstChoice.path("delta").path("content"));
        if (!deltaContent.isEmpty()) {
            return deltaContent;
        }
        String messageContent = extractContentNode(firstChoice.path("message").path("content"));
        if (!messageContent.isEmpty()) {
            return messageContent;
        }
        return extractContentNode(firstChoice.path("text"));
    }

    private String extractStreamingChoiceReasoning(JsonNode firstChoice) {
        return firstNonBlank(
                extractContentNode(firstChoice.path("delta").path("reasoning_content")),
                extractContentNode(firstChoice.path("message").path("reasoning_content"))
        );
    }

    private String extractFinishReason(JsonNode firstChoice) {
        return extractContentNode(firstChoice.path("finish_reason"));
    }

    private String extractContentNode(JsonNode contentNode) {
        if (contentNode == null || contentNode.isMissingNode() || contentNode.isNull()) {
            return "";
        }
        if (contentNode.isTextual()) {
            return contentNode.asText();
        }
        if (contentNode.isArray()) {
            StringBuilder content = new StringBuilder();
            for (JsonNode node : contentNode) {
                if (node.hasNonNull("text")) {
                    content.append(node.get("text").asText());
                } else if (node.isTextual()) {
                    content.append(node.asText());
                }
            }
            return content.toString();
        }
        return contentNode.toString();
    }

    private String firstNonBlank(String... candidates) {
        if (candidates == null) {
            return "";
        }
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate;
            }
        }
        return "";
    }

    private AiAnalysisPayload parseAnalysisPayload(String rawContent) {
        return parseModelStagePayload(rawContent, AiAnalysisPayload.class);
    }

    private <T> T parseModelStagePayload(String rawContent, Class<T> payloadType) {
        String normalized = cleanupAiText(rawContent);
        lastModelStageRawContent.set(normalized);
        try {
            return objectMapper.readValue(normalized, payloadType);
        } catch (JsonProcessingException firstError) {
            int start = normalized.indexOf('{');
            int end = normalized.lastIndexOf('}');
            if (start >= 0 && end > start) {
                try {
                    return objectMapper.readValue(normalized.substring(start, end + 1), payloadType);
                } catch (JsonProcessingException ignored) {
                    log.warn("AI payload parsing failed. type={}, error={}, contentPreview={}",
                            payloadType.getSimpleName(),
                            ignored.getMessage(),
                            previewBody(normalized));
                }
            }
            return null;
        }
    }

    private String extractJsonObjectField(String rawContent, String fieldName) {
        String normalized = cleanupAiText(rawContent);
        String fieldToken = "\"" + fieldName + "\"";
        int fieldIndex = normalized.indexOf(fieldToken);
        if (fieldIndex < 0) {
            return "";
        }
        int colonIndex = normalized.indexOf(':', fieldIndex + fieldToken.length());
        if (colonIndex < 0) {
            return "";
        }
        int objectStart = normalized.indexOf('{', colonIndex + 1);
        if (objectStart < 0) {
            return "";
        }
        int objectEnd = findBalancedObjectEnd(normalized, objectStart);
        if (objectEnd <= objectStart) {
            return "";
        }
        return normalized.substring(objectStart, objectEnd + 1);
    }

    private int findBalancedObjectEnd(String text, int objectStart) {
        if (text == null || objectStart < 0 || objectStart >= text.length() || text.charAt(objectStart) != '{') {
            return -1;
        }
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int index = objectStart; index < text.length(); index++) {
            char current = text.charAt(index);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (current == '\\') {
                    escaped = true;
                } else if (current == '"') {
                    inString = false;
                }
                continue;
            }
            if (current == '"') {
                inString = true;
                continue;
            }
            if (current == '{') {
                depth++;
            } else if (current == '}') {
                depth--;
                if (depth == 0) {
                    return index;
                }
            }
        }
        return -1;
    }

    private String previewBody(String body) {
        String normalized = body == null ? "" : body.replace("\r", "").replace("\n", "\\n").trim();
        if (normalized.length() <= 320) {
            return normalized;
        }
        return normalized.substring(0, 317) + "...";
    }

    private List<AiCodeAssistSupport.LineIssueCandidate> toLineIssueCandidates(AiAnalysisPayload payload) {
        if (payload == null || payload.lineIssues == null || payload.lineIssues.isEmpty()) {
            return List.of();
        }

        return payload.lineIssues.stream()
                .filter(item -> item != null)
                .map(item -> new AiCodeAssistSupport.LineIssueCandidate(item.lineNumber, item.error, item.suggestion))
                .toList();
    }

    private SubmissionAnalysisResponse.StudentHintPlan cleanHintPlan(AiStudentHintPlanPayload candidate,
                                                                     SubmissionAnalysisResponse.StudentHintPlan fallback) {
        if (candidate == null) {
            return fallback;
        }
        String hintLevel = normalizeHintLevel(candidate.hintLevel, fallback == null ? null : fallback.getHintLevel());
        String problemType = defaultIfBlank(candidate.problemType, fallback == null ? "" : fallback.getProblemType());
        String evidenceAnchor = defaultIfBlank(candidate.evidenceAnchor, fallback == null ? "" : fallback.getEvidenceAnchor());
        String nextAction = defaultIfBlank(candidate.nextAction, fallback == null ? "" : fallback.getNextAction());
        String coachQuestion = defaultIfBlank(candidate.coachQuestion, fallback == null ? "" : fallback.getCoachQuestion());
        String teachingAction = normalizeTeachingAction(candidate.teachingAction, fallback == null ? null : fallback.getTeachingAction());
        List<String> evidenceRefs = cleanList(candidate.evidenceRefs, fallback == null ? List.of() : fallback.getEvidenceRefs());
        String answerLeakRisk = resolveAnswerLeakRisk(candidate.answerLeakRisk, fallback == null ? "UNKNOWN" : fallback.getAnswerLeakRisk());
        if (problemType.isBlank() && evidenceAnchor.isBlank() && nextAction.isBlank() && coachQuestion.isBlank()) {
            return fallback;
        }
        return SubmissionAnalysisResponse.StudentHintPlan.builder()
                .hintLevel(hintLevel)
                .problemType(problemType)
                .evidenceAnchor(evidenceAnchor)
                .nextAction(nextAction)
                .coachQuestion(coachQuestion)
                .teachingAction(teachingAction)
                .evidenceRefs(evidenceRefs)
                .answerLeakRisk(answerLeakRisk)
                .build();
    }

    private SubmissionAnalysisResponse.LearningInterventionPlan cleanInterventionPlan(
            AiLearningInterventionPlanPayload candidate,
            SubmissionAnalysisResponse.LearningInterventionPlan fallback) {
        if (candidate == null) {
            return fallback;
        }
        String interventionType = normalizeInterventionType(
                candidate.interventionType,
                fallback == null ? null : fallback.getInterventionType()
        );
        String goal = defaultIfBlank(candidate.goal, fallback == null ? "" : fallback.getGoal());
        String studentTask = defaultIfBlank(candidate.studentTask, fallback == null ? "" : fallback.getStudentTask());
        String checkQuestion = defaultIfBlank(candidate.checkQuestion, fallback == null ? "" : fallback.getCheckQuestion());
        String completionSignal = defaultIfBlank(candidate.completionSignal, fallback == null ? "" : fallback.getCompletionSignal());
        List<String> evidenceRefs = cleanList(candidate.evidenceRefs, fallback == null ? List.of() : fallback.getEvidenceRefs());
        Integer estimatedMinutes = candidate.estimatedMinutes == null || candidate.estimatedMinutes <= 0
                ? fallback == null ? 5 : fallback.getEstimatedMinutes()
                : Math.min(candidate.estimatedMinutes, 20);
        String answerLeakRisk = resolveAnswerLeakRisk(candidate.answerLeakRisk, fallback == null ? "UNKNOWN" : fallback.getAnswerLeakRisk());
        if (studentTask.isBlank() && checkQuestion.isBlank() && completionSignal.isBlank()) {
            return fallback;
        }
        return SubmissionAnalysisResponse.LearningInterventionPlan.builder()
                .interventionType(interventionType)
                .goal(goal)
                .studentTask(studentTask)
                .checkQuestion(checkQuestion)
                .completionSignal(completionSignal)
                .evidenceRefs(evidenceRefs)
                .estimatedMinutes(estimatedMinutes == null || estimatedMinutes <= 0 ? 5 : estimatedMinutes)
                .answerLeakRisk(answerLeakRisk)
                .build();
    }

    private String normalizeHintLevel(String candidate, String fallback) {
        String normalized = cleanupAiText(candidate).toUpperCase();
        return switch (normalized) {
            case "L1", "L2", "L3", "L4" -> normalized;
            default -> fallback == null || fallback.isBlank() ? "L2" : fallback;
        };
    }

    private String normalizeTeachingAction(String candidate, String fallback) {
        String normalized = cleanupAiText(candidate).toUpperCase();
        return switch (normalized) {
            case "ASK_MIN_CASE", "TRACE_VARIABLES", "COMPARE_OUTPUT", "COUNT_COMPLEXITY", "DEFINE_STATE",
                    "CHECK_INVARIANT", "BUILD_COUNTEREXAMPLE", "COMPARE_SUBMISSIONS", "CHECK_RUNTIME_GUARDS",
                    "COLLECT_EVIDENCE", "FIX_FIRST_COMPILER_ERROR", "COMPARE_INPUT_SPEC", "TRACE_STATE",
                    "COMPARE_STRUCTURES", "DRAW_RECURSION_TREE", "EXPLAIN_GENERALITY", "CHECK_BRANCH_COVERAGE" -> normalized;
            default -> fallback == null || fallback.isBlank() ? "TRACE_VARIABLES" : fallback;
        };
    }

    private String normalizeInterventionType(String candidate, String fallback) {
        String normalized = cleanupAiText(candidate).toUpperCase();
        return switch (normalized) {
            case "MIN_CASE", "MIN_CASE_TRACE", "VARIABLE_TRACE", "IO_COMPARE", "COMPLEXITY_ESTIMATE",
                    "STATE_EXPLANATION", "COUNTEREXAMPLE", "RUNTIME_GUARD_CHECK", "COMPARE_SUBMISSIONS",
                    "FIX_FIRST_COMPILER_ERROR", "EXPLAIN_GENERALITY", "COLLECT_EVIDENCE" -> normalized;
            default -> fallback == null || fallback.isBlank() ? "COLLECT_EVIDENCE" : fallback;
        };
    }

    private List<String> cleanList(List<String> candidate, List<String> fallback) {
        List<String> source = candidate == null || candidate.isEmpty() ? fallback : candidate;
        Set<String> unique = new LinkedHashSet<>();
        for (String item : source) {
            if (item == null) {
                continue;
            }
            String normalized = cleanupAiText(item);
            if (!normalized.isBlank()) {
                unique.add(normalized);
            }
        }
        return new ArrayList<>(unique);
    }

    private String truncateSourceCode(String sourceCode) {
        if (sourceCode == null) {
            return "";
        }
        if (sourceCode.length() <= MAX_SOURCE_CODE_LENGTH) {
            return sourceCode;
        }
        return sourceCode.substring(0, MAX_SOURCE_CODE_LENGTH) + "\n// ... truncated ...";
    }

    private String truncateText(String value, int maxLength) {
        String text = cleanupAiText(value);
        int limit = Math.max(0, maxLength);
        if (limit == 0 || text.length() <= limit) {
            return text;
        }
        return text.substring(0, limit).trim() + "...";
    }

    private String compactLineAwareSourceExcerpt(String numberedSourceCode, int maxLines, int maxChars) {
        String normalized = cleanupAiText(numberedSourceCode);
        if (normalized.isBlank()) {
            return "";
        }
        String[] lines = normalized.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        StringBuilder builder = new StringBuilder();
        int lineLimit = Math.max(1, maxLines);
        int charLimit = Math.max(80, maxChars);
        for (int index = 0; index < lines.length && index < lineLimit; index++) {
            String line = lines[index];
            if (builder.length() > 0) {
                builder.append('\n');
            }
            if (builder.length() + line.length() > charLimit) {
                builder.append(line, 0, Math.max(0, charLimit - builder.length())).append("...");
                break;
            }
            builder.append(line);
        }
        if (lines.length > lineLimit) {
            builder.append("\n... truncated ").append(lines.length - lineLimit).append(" more lines ...");
        }
        return builder.toString();
    }

    private String buildLineAwareSourceCode(String sourceCode) {
        if (sourceCode == null || sourceCode.isBlank()) {
            return "";
        }

        String normalized = sourceCode.replace("\r\n", "\n").replace('\r', '\n');
        String[] lines = normalized.split("\n", -1);
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < lines.length; index++) {
            String numberedLine = (index + 1) + ": " + lines[index];
            if (builder.length() > 0 && builder.length() + numberedLine.length() + 1 > MAX_SOURCE_CODE_LENGTH) {
                builder.append("\n... truncated after line ")
                        .append(index)
                        .append(" of ")
                        .append(lines.length)
                        .append(" ...");
                break;
            }
            if (index > 0) {
                builder.append('\n');
            }
            builder.append(numberedLine);
        }
        return builder.toString();
    }

    private List<SubmissionAnalysisResponse.LineIssue> cleanLineIssues(List<AiLineIssuePayload> issues,
                                                                      String sourceCode,
                                                                      List<SubmissionAnalysisResponse.LineIssue> fallback) {
        int maxLineNumber = countSourceLines(sourceCode);
        if (issues == null || issues.isEmpty() || maxLineNumber == 0) {
            return fallback == null ? List.of() : fallback;
        }

        List<SubmissionAnalysisResponse.LineIssue> normalized = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (AiLineIssuePayload issue : issues) {
            if (issue == null) {
                continue;
            }

            Integer lineNumber = normalizeLineNumber(issue.lineNumber, maxLineNumber);
            String error = cleanupAiText(issue.error);
            String suggestion = cleanupAiText(issue.suggestion);
            if (lineNumber == null || error.isBlank() || suggestion.isBlank()) {
                continue;
            }

            String dedupeKey = lineNumber + "|" + error + "|" + suggestion;
            if (!seen.add(dedupeKey)) {
                continue;
            }

            normalized.add(SubmissionAnalysisResponse.LineIssue.builder()
                    .lineNumber(lineNumber)
                    .error(error)
                    .suggestion(suggestion)
                    .build());
        }

        normalized.sort(Comparator.comparing(SubmissionAnalysisResponse.LineIssue::getLineNumber));
        return normalized;
    }

    private List<SubmissionAnalysisResponse.LineIssue> resolveLineIssues(AiAnalysisPayload payload,
                                                                         String rawContent,
                                                                         String sourceCode,
                                                                         SubmissionAnalysisResponse fallback) {
        List<SubmissionAnalysisResponse.LineIssue> fallbackIssues = fallback.getLineIssues() == null ? List.of() : fallback.getLineIssues();
        List<SubmissionAnalysisResponse.LineIssue> direct = cleanLineIssues(
                payload == null ? null : payload.lineIssues,
                sourceCode,
                List.of()
        );
        if (!direct.isEmpty()) {
            return direct;
        }

        List<AiLineIssuePayload> parsedFromMarkdown = parseLineIssuesFromText(payload == null ? null : payload.reportMarkdown);
        List<SubmissionAnalysisResponse.LineIssue> fromMarkdown = cleanLineIssues(parsedFromMarkdown, sourceCode, List.of());
        if (!fromMarkdown.isEmpty()) {
            return fromMarkdown;
        }

        List<AiLineIssuePayload> parsedFromRaw = parseLineIssuesFromText(rawContent);
        List<SubmissionAnalysisResponse.LineIssue> fromRaw = cleanLineIssues(parsedFromRaw, sourceCode, List.of());
        if (!fromRaw.isEmpty()) {
            return fromRaw;
        }

        return fallbackIssues;
    }

    private Integer normalizeLineNumber(Integer lineNumber, int maxLineNumber) {
        if (lineNumber == null || lineNumber < 1 || lineNumber > maxLineNumber) {
            return null;
        }
        return lineNumber;
    }

    private int countSourceLines(String sourceCode) {
        if (sourceCode == null) {
            return 0;
        }
        return sourceCode.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1).length;
    }

    private String cleanupAiText(String text) {
        if (text == null) {
            return "";
        }

        String normalized = stripReasoningBlocks(text.trim());
        if (normalized.startsWith("```")) {
            normalized = normalized.replaceFirst("^```[a-zA-Z0-9_-]*\\s*", "");
            normalized = normalized.replaceFirst("\\s*```$", "");
        }
        normalized = normalized.replace("<think>", "")
                .replace("</think>", "")
                .trim();
        return normalized;
    }

    private String stripReasoningBlocks(String text) {
        String normalized = text;
        normalized = normalized.replaceAll("(?is)<think>.*?</think>", "");
        normalized = normalized.replaceAll("(?is)```(?:thinking|thought|analysis)[^\\n]*\\n.*?```", "");
        normalized = normalized.replaceAll("(?is)^思考过程[:：].*?(?=\\n#|\\n##|\\n\\{|\\Z)", "");
        return normalized.trim();
    }

    private String extractLineNumberedBlock(String text) {
        Matcher matcher = NUMBERED_LINE_PATTERN.matcher(text == null ? "" : text);
        StringBuilder builder = new StringBuilder();
        while (matcher.find()) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(matcher.group(1)).append(": ").append(matcher.group(2));
        }
        return builder.toString();
    }

    private List<AiLineIssuePayload> parseLineIssuesFromText(String text) {
        String normalized = cleanupAiText(text);
        if (normalized.isBlank()) {
            return List.of();
        }

        List<AiLineIssuePayload> issues = new ArrayList<>();
        Matcher matcher = REPORT_LINE_ISSUE_PATTERN.matcher(normalized);
        while (matcher.find()) {
            AiLineIssuePayload issue = new AiLineIssuePayload();
            issue.lineNumber = Integer.valueOf(matcher.group(1));
            issue.error = cleanupAiText(matcher.group(2));
            issue.suggestion = cleanupAiText(matcher.group(3));
            issues.add(issue);
        }
        return issues;
    }

    private String defaultIfBlank(String candidate, String fallback) {
        String normalized = cleanupAiText(candidate);
        if (normalized.isBlank()) {
            return fallback;
        }
        return normalized;
    }

    private String defaultNullable(String candidate, String fallback) {
        String normalized = cleanupAiText(candidate);
        if (normalized.isBlank()) {
            return fallback;
        }
        return normalized;
    }

    private Double resolveConfidence(Double candidate, Double fallback) {
        if (candidate == null || candidate.isNaN() || candidate < 0 || candidate > 1) {
            return fallback;
        }
        return candidate;
    }

    private String resolveAnswerLeakRisk(String candidate, String fallback) {
        String normalized = cleanupAiText(candidate).toUpperCase();
        if ("LOW".equals(normalized) || "MEDIUM".equals(normalized) || "HIGH".equals(normalized)) {
            return normalized;
        }
        return fallback == null || fallback.isBlank() ? "UNKNOWN" : fallback;
    }

    private String higherRisk(String left, String right) {
        int leftScore = riskScore(left);
        int rightScore = riskScore(right);
        int score = Math.max(leftScore, rightScore);
        return switch (score) {
            case 3 -> "HIGH";
            case 2 -> "MEDIUM";
            case 1 -> "LOW";
            default -> "";
        };
    }

    private int riskScore(String risk) {
        return switch (cleanupAiText(risk).toUpperCase()) {
            case "HIGH" -> 3;
            case "MEDIUM" -> 2;
            case "LOW" -> 1;
            default -> 0;
        };
    }

    private SubmissionAnalysisResponse.AiInvocation modelInvocation(SubmissionAnalysisResponse fallback,
                                                                    String status,
                                                                    boolean fallbackUsed) {
        return modelInvocation(fallback, status, fallbackUsed, PROMPT_VERSION);
    }

    private SubmissionAnalysisResponse.AiInvocation modelInvocation(SubmissionAnalysisResponse fallback,
                                                                    String status,
                                                                    boolean fallbackUsed,
                                                                    String promptVersion) {
        return modelInvocation(fallback, status, fallbackUsed, promptVersion, null);
    }

    private SubmissionAnalysisResponse.AiInvocation modelInvocation(SubmissionAnalysisResponse fallback,
                                                                    String status,
                                                                    boolean fallbackUsed,
                                                                    String promptVersion,
                                                                    ExternalModelStagePayloads.StageValidationResult failure) {
        ExternalModelCallTelemetry telemetry = lastCallTelemetry.get();
        int requestBytes = telemetry.requestBytes() == null ? 0 : telemetry.requestBytes();
        return SubmissionAnalysisResponse.AiInvocation.builder()
                .provider(PROVIDER)
                .model(model)
                .modelVersion(model)
                .promptVersion(promptVersion)
                .agentVersion(fallback == null || fallback.getAiInvocation() == null
                        ? null
                        : fallback.getAiInvocation().getAgentVersion())
                .analysisSchemaVersion(fallback == null ? "diagnosis-v1" : fallback.getAnalysisSchemaVersion())
                .evidenceSchemaVersion(fallback == null ? DiagnosisEvidencePackage.SCHEMA_VERSION : fallback.getEvidenceSchemaVersion())
                .taxonomyVersion(fallback == null ? null : fallback.getTaxonomyVersion())
                .status(status)
                .fallbackUsed(fallbackUsed)
                .runtimeMode(runtimeModeForPrompt(promptVersion))
                .runtimeProfile(defaultIfBlank(telemetry.runtimeProfile(),
                        ExternalModelAgentRuntime.RUNTIME_PROFILE_STANDARD))
                .requestBytes(requestBytes)
                .requestCompact(Boolean.TRUE.equals(telemetry.requestCompact()))
                .failureStage(failure == null ? "" : defaultIfBlank(failure.getStage(), "UNKNOWN_STAGE"))
                .failureReason(failure == null || failure.getFailureReason() == null
                        ? ""
                        : failure.getFailureReason().name())
                .transportMode(telemetry.transportMode())
                .streamChunkCount(telemetry.streamChunkCount())
                .streamContentChunkCount(telemetry.streamContentChunkCount())
                .streamReasoningChunkCount(telemetry.streamReasoningChunkCount())
                .streamInvalidChunkCount(telemetry.streamInvalidChunkCount())
                .streamFinishReason(telemetry.streamFinishReason())
                .streamFallbackRetryUsed(telemetry.streamFallbackRetryUsed())
                .build();
    }

    private String runtimeModeForPrompt(String promptVersion) {
        String version = cleanupAiText(promptVersion);
        if (STUDENT_FAST_FEEDBACK_PROMPT_VERSION.equals(version)) {
            return "student-fast-feedback";
        }
        if (version.startsWith("diagnosis-and-teaching-")) {
            return RUNTIME_MODE_SINGLE_CALL;
        }
        if (RUNTIME_PROMPT_VERSION.equals(version)) {
            return "staged";
        }
        return "legacy-long-prompt";
    }

    private record ParsedStreamingContent(String content, ExternalModelCallTelemetry telemetry) {
    }

    private record ExternalModelRequestContext(String runtimeProfile, boolean requestCompact) {
        ExternalModelRequestContext {
            runtimeProfile = runtimeProfile == null || runtimeProfile.isBlank()
                    ? ExternalModelAgentRuntime.RUNTIME_PROFILE_STANDARD
                    : runtimeProfile;
        }

        static ExternalModelRequestContext standard() {
            return new ExternalModelRequestContext(ExternalModelAgentRuntime.RUNTIME_PROFILE_STANDARD, false);
        }
    }

    private record ExternalModelCallTelemetry(String transportMode,
                                              Integer streamChunkCount,
                                              Integer streamContentChunkCount,
                                              Integer streamReasoningChunkCount,
                                              Integer streamInvalidChunkCount,
                                              String streamFinishReason,
                                              Boolean streamFallbackRetryUsed,
                                              Integer requestBytes,
                                              String runtimeProfile,
                                              Boolean requestCompact) {
        static ExternalModelCallTelemetry empty() {
            return request(ExternalModelAgentRuntime.RUNTIME_PROFILE_STANDARD, false, 0);
        }

        static ExternalModelCallTelemetry request(String runtimeProfile,
                                                  boolean requestCompact,
                                                  int requestBytes) {
            return new ExternalModelCallTelemetry(
                    "",
                    0,
                    0,
                    0,
                    0,
                    "",
                    false,
                    Math.max(0, requestBytes),
                    runtimeProfile == null || runtimeProfile.isBlank()
                            ? ExternalModelAgentRuntime.RUNTIME_PROFILE_STANDARD
                            : runtimeProfile,
                    requestCompact
            );
        }

        static ExternalModelCallTelemetry nonStream(String finishReason) {
            return new ExternalModelCallTelemetry(
                    "non-stream",
                    0,
                    0,
                    0,
                    0,
                    finishReason == null ? "" : finishReason,
                    false,
                    0,
                    ExternalModelAgentRuntime.RUNTIME_PROFILE_STANDARD,
                    false
            );
        }

        static ExternalModelCallTelemetry stream(int chunkCount,
                                                 int contentChunkCount,
                                                 int reasoningChunkCount,
                                                 int invalidChunkCount,
                                                 String finishReason) {
            return new ExternalModelCallTelemetry(
                    "stream",
                    Math.max(0, chunkCount),
                    Math.max(0, contentChunkCount),
                    Math.max(0, reasoningChunkCount),
                    Math.max(0, invalidChunkCount),
                    finishReason == null ? "" : finishReason,
                    false,
                    0,
                    ExternalModelAgentRuntime.RUNTIME_PROFILE_STANDARD,
                    false
            );
        }

        ExternalModelCallTelemetry withFallbackRetryUsed(boolean fallbackRetryUsed) {
            return new ExternalModelCallTelemetry(
                    transportMode,
                    streamChunkCount,
                    streamContentChunkCount,
                    streamReasoningChunkCount,
                    streamInvalidChunkCount,
                    streamFinishReason,
                    fallbackRetryUsed,
                    requestBytes,
                    runtimeProfile,
                    requestCompact
            );
        }

        ExternalModelCallTelemetry withRequestTelemetry(int requestBytes,
                                                        String runtimeProfile,
                                                        boolean requestCompact) {
            return new ExternalModelCallTelemetry(
                    transportMode,
                    streamChunkCount,
                    streamContentChunkCount,
                    streamReasoningChunkCount,
                    streamInvalidChunkCount,
                    streamFinishReason,
                    streamFallbackRetryUsed,
                    Math.max(0, requestBytes),
                    runtimeProfile == null || runtimeProfile.isBlank()
                            ? ExternalModelAgentRuntime.RUNTIME_PROFILE_STANDARD
                            : runtimeProfile,
                    requestCompact
            );
        }
    }

    private Map<String, Object> compactProblemContext(Problem problem) {
        Map<String, Object> context = new LinkedHashMap<>();
        if (problem == null) {
            return context;
        }
        context.put("id", problem.getId());
        context.put("title", problem.getTitle());
        context.put("description", problem.getDescription());
        context.put("difficulty", problem.getDifficulty() == null ? "" : problem.getDifficulty().name());
        context.put("timeLimit", problem.getTimeLimit());
        context.put("memoryLimit", problem.getMemoryLimit());
        context.put("knowledgePoints", problem.getKnowledgePoints());
        context.put("algorithmStrategies", problem.getAlgorithmStrategies());
        context.put("commonMistakes", problem.getCommonMistakes());
        context.put("boundaryTypes", problem.getBoundaryTypes());
        return context;
    }

    private Map<String, Object> compactSubmissionContext(Submission submission,
                                                         DiagnosisEvidencePackage evidencePackage) {
        Map<String, Object> context = new LinkedHashMap<>();
        if (submission == null) {
            return context;
        }
        context.put("id", submission.getId());
        context.put("language", submission.getLanguageName());
        context.put("verdict", submission.getVerdict() == null ? "UNKNOWN" : submission.getVerdict().name());
        context.put("compileOutput", defaultIfBlank(submission.getCompileOutput(), ""));
        context.put("runtimeErrorMessage", defaultIfBlank(submission.getErrorMessage(), ""));
        context.put("sourceCodeWithLineNumbers", evidencePackage == null || evidencePackage.getSubmission() == null
                ? buildLineAwareSourceCode(submission.getSourceCode())
                : evidencePackage.getSubmission().getSourceCodeWithLineNumbers());
        context.put("sourceCodeLineCount", evidencePackage == null || evidencePackage.getSubmission() == null
                ? countSourceLines(submission.getSourceCode())
                : evidencePackage.getSubmission().getSourceCodeLineCount());
        return context;
    }

    private Map<String, Object> compactRuleSignals(RuleSignalAnalyzer.RuleSignalResult ruleSignals) {
        Map<String, Object> context = new LinkedHashMap<>();
        if (ruleSignals == null) {
            context.put("signals", List.of());
            context.put("candidateIssueTags", List.of());
            context.put("candidateFineGrainedTags", List.of());
            context.put("evidenceRefs", List.of());
            return context;
        }
        context.put("signals", ruleSignals.getSignals() == null ? List.of() : ruleSignals.getSignals().stream()
                .limit(8)
                .map(signal -> Map.of(
                        "evidenceRef", defaultIfBlank(signal.getEvidenceRef(), ""),
                        "coarseTag", defaultIfBlank(signal.getCoarseTag(), ""),
                        "fineTag", defaultIfBlank(signal.getFineTag(), ""),
                        "message", defaultIfBlank(signal.getMessage(), ""),
                        "confidence", signal.getConfidence() == null ? 0 : signal.getConfidence()
                ))
                .toList());
        context.put("candidateIssueTags", cleanList(ruleSignals.getCandidateIssueTags(), List.of()).stream().limit(6).toList());
        context.put("candidateFineGrainedTags", cleanList(ruleSignals.getCandidateFineGrainedTags(), List.of()).stream().limit(6).toList());
        context.put("evidenceRefs", cleanList(ruleSignals.getEvidenceRefs(), List.of()).stream().limit(10).toList());
        return context;
    }

    private Map<String, Object> compactStudentFastFeedbackContext(Problem problem,
                                                                  Submission submission,
                                                                  DiagnosisEvidencePackage evidencePackage,
                                                                  RuleSignalAnalyzer.RuleSignalResult ruleSignals) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("problem", compactStudentProblemContext(problem, evidencePackage));
        context.put("submission", compactStudentSubmissionContext(submission, evidencePackage));
        context.put("judgeFacts", compactStudentJudgeFacts(evidencePackage == null ? null : evidencePackage.getJudgeFacts()));
        context.put("candidateSignals", compactStudentRuleSignals(ruleSignals));
        context.put("safetyRules", List.of(
                "只给定位和验证动作，不给完整代码或完整答案。",
                "不猜隐藏测试数据。",
                "每条建议必须引用输入 evidenceRefs。"
        ));
        return context;
    }

    private Map<String, Object> compactStudentProblemContext(Problem problem,
                                                             DiagnosisEvidencePackage evidencePackage) {
        Map<String, Object> context = new LinkedHashMap<>();
        DiagnosisEvidencePackage.ProblemEvidence evidence = evidencePackage == null ? null : evidencePackage.getProblem();
        if (problem == null && evidence == null) {
            return context;
        }
        context.put("id", problem == null ? evidence.getId() : problem.getId());
        context.put("title", problem == null ? evidence.getTitle() : problem.getTitle());
        context.put("difficulty", problem == null ? defaultIfBlank(evidence.getDifficulty(), "") : problem.getDifficulty() == null ? "" : problem.getDifficulty().name());
        context.put("brief", truncateText(problem == null ? evidence.getDescription() : problem.getDescription(), 260));
        context.put("knowledgePoints", problem == null ? cleanList(evidence.getKnowledgePoints(), List.of()).stream().limit(3).toList()
                : cleanList(problem.getKnowledgePoints(), List.of()).stream().limit(3).toList());
        context.put("commonMistakes", problem == null ? cleanList(evidence.getCommonMistakes(), List.of()).stream().limit(3).toList()
                : cleanList(problem.getCommonMistakes(), List.of()).stream().limit(3).toList());
        return context;
    }

    private Map<String, Object> compactStudentSubmissionContext(Submission submission,
                                                                DiagnosisEvidencePackage evidencePackage) {
        Map<String, Object> context = new LinkedHashMap<>();
        DiagnosisEvidencePackage.SubmissionEvidence evidence = evidencePackage == null ? null : evidencePackage.getSubmission();
        if (submission == null && evidence == null) {
            return context;
        }
        String sourceCode = evidence == null || evidence.getSourceCode() == null
                ? submission == null ? "" : submission.getSourceCode()
                : evidence.getSourceCode();
        String numberedSource = evidence == null || evidence.getSourceCodeWithLineNumbers() == null
                ? buildLineAwareSourceCode(sourceCode)
                : evidence.getSourceCodeWithLineNumbers();
        context.put("id", submission == null ? evidence.getId() : submission.getId());
        context.put("language", submission == null ? evidence.getLanguage() : submission.getLanguageName());
        context.put("verdict", submission == null ? defaultIfBlank(evidence.getVerdict(), "UNKNOWN") : submission.getVerdict() == null ? "UNKNOWN" : submission.getVerdict().name());
        context.put("sourceCodeLineCount", evidence == null || evidence.getSourceCodeLineCount() == null
                ? countSourceLines(sourceCode)
                : evidence.getSourceCodeLineCount());
        context.put("sourceExcerpt", compactLineAwareSourceExcerpt(numberedSource, 8, 520));
        context.put("compileOutput", truncateText(submission == null ? "" : submission.getCompileOutput(), 220));
        context.put("runtimeErrorMessage", truncateText(submission == null ? "" : submission.getErrorMessage(), 220));
        return context;
    }

    private Map<String, Object> compactStudentJudgeFacts(DiagnosisEvidencePackage.JudgeFacts judgeFacts) {
        Map<String, Object> context = new LinkedHashMap<>();
        if (judgeFacts == null) {
            return context;
        }
        context.put("passedCount", judgeFacts.getPassedCount());
        context.put("totalCount", judgeFacts.getTotalCount());
        context.put("hiddenFailureObserved", Boolean.TRUE.equals(judgeFacts.getHiddenFailureObserved()));
        context.put("runtimeErrorMessage", truncateText(judgeFacts.getRuntimeErrorMessage(), 220));
        context.put("compileOutput", truncateText(judgeFacts.getCompileOutput(), 220));
        context.put("firstFailedCase", compactFirstVisibleCase(judgeFacts));
        context.put("caseResultsSummary", judgeFacts.getCaseResultsSummary() == null ? List.of() : judgeFacts.getCaseResultsSummary().stream()
                .filter(item -> item != null)
                .limit(2)
                .map(item -> Map.of(
                        "testCaseNumber", item.getTestCaseNumber() == null ? 0 : item.getTestCaseNumber(),
                        "passed", Boolean.TRUE.equals(item.getPassed()),
                        "hidden", Boolean.TRUE.equals(item.getHidden()),
                        "actualOutputPreview", Boolean.TRUE.equals(item.getHidden()) ? "[隐藏]" : truncateText(item.getActualOutputPreview(), 120),
                        "expectedOutputPreview", Boolean.TRUE.equals(item.getHidden()) ? "[隐藏]" : truncateText(item.getExpectedOutputPreview(), 120)
                ))
                .toList());
        return context;
    }

    private Map<String, Object> compactFirstVisibleCase(DiagnosisEvidencePackage.JudgeFacts judgeFacts) {
        if (judgeFacts == null || judgeFacts.getCaseResultsSummary() == null) {
            return Map.of();
        }
        return judgeFacts.getCaseResultsSummary().stream()
                .filter(item -> item != null && !Boolean.TRUE.equals(item.getPassed()) && !Boolean.TRUE.equals(item.getHidden()))
                .findFirst()
                .map(item -> Map.<String, Object>of(
                        "testCaseNumber", item.getTestCaseNumber() == null ? 0 : item.getTestCaseNumber(),
                        "actualOutputPreview", truncateText(item.getActualOutputPreview(), 120),
                        "expectedOutputPreview", truncateText(item.getExpectedOutputPreview(), 120)
                ))
                .orElse(Map.of());
    }

    private Map<String, Object> compactStudentRuleSignals(RuleSignalAnalyzer.RuleSignalResult ruleSignals) {
        Map<String, Object> context = new LinkedHashMap<>();
        if (ruleSignals == null) {
            context.put("signals", List.of());
            context.put("candidateIssueTags", List.of());
            context.put("candidateFineGrainedTags", List.of());
            context.put("evidenceRefs", List.of());
            return context;
        }
        context.put("signals", ruleSignals.getSignals() == null ? List.of() : ruleSignals.getSignals().stream()
                .filter(signal -> signal != null)
                .limit(3)
                .map(signal -> Map.of(
                        "evidenceRef", defaultIfBlank(signal.getEvidenceRef(), ""),
                        "coarseTag", defaultIfBlank(signal.getCoarseTag(), ""),
                        "fineTag", defaultIfBlank(signal.getFineTag(), ""),
                        "message", truncateText(signal.getMessage(), 120)
                ))
                .toList());
        context.put("candidateIssueTags", cleanList(ruleSignals.getCandidateIssueTags(), List.of()).stream().limit(3).toList());
        context.put("candidateFineGrainedTags", cleanList(ruleSignals.getCandidateFineGrainedTags(), List.of()).stream().limit(3).toList());
        context.put("evidenceRefs", cleanList(ruleSignals.getEvidenceRefs(), List.of()).stream().limit(6).toList());
        return context;
    }

    private StudentAiFeedbackResponse normalizeStudentFastFeedback(StudentFastFeedbackPayload payload,
                                                                   Submission submission,
                                                                   long startedAt) {
        if (payload == null) {
            return feedbackFailure(submission, "FAILED", "STRUCTURED_OUTPUT_INVALID", startedAt);
        }
        StudentAiFeedbackResponse.Safety safety = StudentAiFeedbackResponse.Safety.builder()
                .answerLeakRisk(normalizeLeakRisk(payload.safety == null ? null : payload.safety.answerLeakRisk))
                .blockedReasons(cleanList(payload.safety == null ? null : payload.safety.blockedReasons, List.of()))
                .build();
        if ("HIGH".equals(safety.getAnswerLeakRisk())) {
            return StudentAiFeedbackResponse.builder()
                    .submissionId(submission == null ? null : submission.getId())
                    .status("SAFETY_REJECTED")
                    .source("MODEL")
                    .latencyMs(elapsedMs(startedAt))
                    .repairItems(List.of())
                    .improvementItems(List.of())
                    .nextQuestion(null)
                    .safety(StudentAiFeedbackResponse.Safety.builder()
                            .answerLeakRisk("HIGH")
                    .blockedReasons(mergeStringLists(safety.getBlockedReasons(), List.of("ANSWER_LEAK_RISK")))
                            .build())
                    .evidenceRefs(cleanList(payload.evidenceRefs, List.of()))
                    .build();
        }
        List<StudentAiFeedbackResponse.FeedbackItem> repairItems = normalizeFeedbackItems(payload.repairItems, 1);
        List<StudentAiFeedbackResponse.FeedbackItem> improvementItems = normalizeFeedbackItems(payload.improvementItems, 2);
        String nextQuestion = cleanStudentFeedbackText(payload.nextQuestion);
        List<String> evidenceRefs = mergeStringLists(
                payload.evidenceRefs,
                mergeItemRefs(repairItems, improvementItems)
        );
        if (repairItems.isEmpty() && improvementItems.isEmpty() && nextQuestion.isBlank()) {
            return feedbackFailure(submission, "FAILED", "EMPTY_MODEL_FEEDBACK", startedAt);
        }
        return StudentAiFeedbackResponse.builder()
                .submissionId(submission == null ? null : submission.getId())
                .status("READY")
                .source("MODEL")
                .latencyMs(elapsedMs(startedAt))
                .repairItems(repairItems)
                .improvementItems(improvementItems)
                .nextQuestion(nextQuestion.isBlank() ? null : nextQuestion)
                .safety(safety)
                .evidenceRefs(evidenceRefs)
                .build();
    }

    private List<StudentAiFeedbackResponse.FeedbackItem> normalizeFeedbackItems(List<StudentFeedbackItemPayload> payloadItems,
                                                                                int maxItems) {
        if (payloadItems == null || payloadItems.isEmpty()) {
            return List.of();
        }
        List<StudentAiFeedbackResponse.FeedbackItem> items = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (StudentFeedbackItemPayload payload : payloadItems) {
            if (payload == null || items.size() >= maxItems) {
                continue;
            }
            String title = cleanStudentFeedbackText(payload.title);
            String body = cleanStudentFeedbackText(payload.body);
            if (body.isBlank() || leaksAnswer(body) || leaksAnswer(title)) {
                continue;
            }
            String dedupeKey = (title + "|" + body).replaceAll("\\s+", "");
            if (!seen.add(dedupeKey)) {
                continue;
            }
            items.add(StudentAiFeedbackResponse.FeedbackItem.builder()
                    .title(title.isBlank() ? null : title)
                    .body(body)
                    .kind(defaultIfBlank(cleanupAiText(payload.kind).toUpperCase(), "GUIDANCE"))
                    .evidenceRefs(cleanList(payload.evidenceRefs, List.of()).stream().limit(4).toList())
                    .qualitySignals(cleanList(payload.qualitySignals, List.of()).stream().limit(5).toList())
                    .build());
        }
        return items;
    }

    private List<String> mergeItemRefs(List<StudentAiFeedbackResponse.FeedbackItem> repairItems,
                                       List<StudentAiFeedbackResponse.FeedbackItem> improvementItems) {
        List<String> refs = List.of();
        List<StudentAiFeedbackResponse.FeedbackItem> items = new ArrayList<>();
        if (repairItems != null) {
            items.addAll(repairItems);
        }
        if (improvementItems != null) {
            items.addAll(improvementItems);
        }
        for (StudentAiFeedbackResponse.FeedbackItem item : items) {
            refs = mergeStringLists(refs, item.getEvidenceRefs());
        }
        return refs;
    }

    private List<String> mergeStringLists(List<String> left, List<String> right) {
        Set<String> unique = new LinkedHashSet<>();
        for (String item : left == null ? List.<String>of() : left) {
            String normalized = cleanupAiText(item);
            if (!normalized.isBlank()) {
                unique.add(normalized);
            }
        }
        for (String item : right == null ? List.<String>of() : right) {
            String normalized = cleanupAiText(item);
            if (!normalized.isBlank()) {
                unique.add(normalized);
            }
        }
        return new ArrayList<>(unique);
    }

    private StudentAiFeedbackResponse feedbackFailure(Submission submission,
                                                      String status,
                                                      String reason,
                                                      long startedAt) {
        String normalizedStatus = defaultIfBlank(status, "FAILED").toUpperCase();
        return StudentAiFeedbackResponse.builder()
                .submissionId(submission == null ? null : submission.getId())
                .status(normalizedStatus)
                .source("RULE_FALLBACK")
                .latencyMs(elapsedMs(startedAt))
                .repairItems(List.of())
                .improvementItems(List.of())
                .nextQuestion(null)
                .safety(StudentAiFeedbackResponse.Safety.builder()
                        .answerLeakRisk("LOW")
                        .blockedReasons(reason == null || reason.isBlank() ? List.of() : List.of(reason))
                        .build())
                .evidenceRefs(List.of())
                .build();
    }

    private String classifyFeedbackFailure(IOException exception) {
        String message = exception == null ? "" : defaultIfBlank(exception.getMessage(), "").toLowerCase();
        if (message.contains("timeout") || message.contains("timed out")) {
            return "TIMEOUT";
        }
        if (message.contains("budget")) {
            return "FAILED";
        }
        return "FAILED";
    }

    private long elapsedMs(long startedAt) {
        if (startedAt <= 0) {
            return 0L;
        }
        return Math.max(0L, Duration.ofNanos(System.nanoTime() - startedAt).toMillis());
    }

    private String cleanStudentFeedbackText(String value) {
        String text = cleanupAiText(value)
                .replaceFirst("^当前最需要先处理的问题[是：:]?\\s*", "")
                .replaceFirst("^先改这里[：:]?\\s*", "")
                .trim();
        if (text.length() > 180) {
            text = text.substring(0, 180).trim();
        }
        return text;
    }

    private boolean leaksAnswer(String text) {
        String compact = defaultIfBlank(text, "").replaceAll("\\s+", "");
        return compact.contains("完整代码")
                || compact.contains("参考代码")
                || compact.contains("答案如下")
                || compact.contains("直接改成")
                || compact.contains("替换为")
                || compact.contains("最终代码")
                || compact.contains("隐藏测试");
    }

    private String normalizeLeakRisk(String value) {
        String normalized = cleanupAiText(value).toUpperCase();
        if ("LOW".equals(normalized) || "MEDIUM".equals(normalized) || "HIGH".equals(normalized)) {
            return normalized;
        }
        return "LOW";
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class AiAnalysisPayload {
        public String analysisSchemaVersion;
        public String evidenceSchemaVersion;
        public String taxonomyVersion;
        public String headline;
        public String summary;
        public List<String> issueTags;
        public List<String> fineGrainedTags;
        public List<String> abilityPoints;
        public List<String> focusPoints;
        public List<String> fixDirections;
        public List<String> evidenceRefs;
        public String studentHint;
        public AiStudentHintPlanPayload studentHintPlan;
        public AiLearningInterventionPlanPayload learningInterventionPlan;
        public String teacherNote;
        public String progressSignal;
        public Double confidence;
        public String uncertainty;
        public String answerLeakRisk;
        public String wrongSolution;
        public String correctSolution;
        public List<AiLineIssuePayload> lineIssues;
        public String reportMarkdown;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class AiStudentHintPlanPayload {
        public String hintLevel;
        public String problemType;
        public String evidenceAnchor;
        public String nextAction;
        public String coachQuestion;
        public String teachingAction;
        public List<String> evidenceRefs;
        public String answerLeakRisk;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class AiLearningInterventionPlanPayload {
        public String interventionType;
        public String goal;
        public String studentTask;
        public String checkQuestion;
        public String completionSignal;
        public List<String> evidenceRefs;
        public Integer estimatedMinutes;
        public String answerLeakRisk;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class AiLineIssuePayload {
        public Integer lineNumber;
        public String error;
        public String suggestion;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class StudentFastFeedbackPayload {
        public List<StudentFeedbackItemPayload> repairItems;
        public List<StudentFeedbackItemPayload> improvementItems;
        public String nextQuestion;
        public StudentFeedbackSafetyPayload safety;
        public List<String> evidenceRefs;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class StudentFeedbackItemPayload {
        public String title;
        public String body;
        public String kind;
        public List<String> evidenceRefs;
        public List<String> qualitySignals;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class StudentFeedbackSafetyPayload {
        public String answerLeakRisk;
        public List<String> blockedReasons;
    }
}
