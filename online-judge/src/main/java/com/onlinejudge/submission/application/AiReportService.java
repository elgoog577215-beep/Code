package com.onlinejudge.submission.application;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlinejudge.submission.dto.SubmissionAnalysisResponse;
import com.onlinejudge.problem.domain.Problem;
import com.onlinejudge.submission.domain.Submission;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@RequiredArgsConstructor
@Slf4j
public class AiReportService {

    private static final int MAX_SOURCE_CODE_LENGTH = 12000;
    private static final String AI_SOURCE = "MODEL_SCOPE_MINIMAX_M2_7";
    private static final String PROVIDER = "ModelScope";
    private static final String PROMPT_VERSION = "submission-diagnosis-prompt-v2";
    private static final Pattern NUMBERED_LINE_PATTERN = Pattern.compile("^(\\d+):\\s?(.*)$", Pattern.MULTILINE);
    private static final Pattern REPORT_LINE_ISSUE_PATTERN = Pattern.compile(
            "行号[：:]\\s*(\\d+)\\s*错误[：:]\\s*(.+?)\\s*建议[：:]\\s*(.+?)(?=(?:\\n\\s*行号[：:])|\\Z)",
            Pattern.DOTALL
    );

    private final ObjectMapper objectMapper;
    private final AiCodeAssistSupport aiCodeAssistSupport;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Value("${ai.enabled:true}")
    private boolean enabled;

    @Value("${ai.base-url:https://api-inference.modelscope.cn/v1}")
    private String baseUrl;

    @Value("${ai.api-key:}")
    private String apiKey;

    @Value("${ai.model:MiniMax/MiniMax-M2.7}")
    private String model;

    @Value("${ai.timeout-seconds:25}")
    private long timeoutSeconds;

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
                    4. fineGrainedTags must only reuse candidate or standard fine-grained labels such as OFF_BY_ONE, EMPTY_INPUT, MAX_BOUNDARY, DUPLICATE_CASE, OUTPUT_FORMAT_DETAIL, INPUT_PARSING, INITIAL_STATE, STATE_RESET, OVER_SIMULATION, BRUTE_FORCE_LIMIT, GREEDY_ASSUMPTION, DP_STATE_DESIGN, SAMPLE_OVERFIT, PARTIAL_FIX_REGRESSION.
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
                            .learningInterventionPlan(fallback.getLearningInterventionPlan())
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
                    .learningInterventionPlan(cleanInterventionPlan(payload.learningInterventionPlan, fallback.getLearningInterventionPlan()))
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
            return fallback;
        }
    }

    public String enhanceGrowthReportMarkdown(Problem problem,
                                              List<Map<String, Object>> submissionTimeline,
                                              String fallbackMarkdown) {
        if (!canCallAi()) {
            log.info("AI growth report skipped because AI access is unavailable. problemId={}", problem.getId());
            return fallbackMarkdown;
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
            return fallbackMarkdown;
        }
    }

    private boolean canCallAi() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }

    private String chatCompletion(String systemPrompt, String userPrompt) throws IOException, InterruptedException {
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "temperature", 0.2,
                "stream", false
        );
        String endpoint = baseUrl.endsWith("/") ? baseUrl + "chat/completions" : baseUrl + "/chat/completions";
        log.info("Calling AI chat completion. model={}, timeoutSeconds={}, endpoint={}",
                model,
                Math.max(timeoutSeconds, 5),
                endpoint);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(Math.max(timeoutSeconds, 5)))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            log.warn("AI chat completion returned non-success status. status={}, bodyPreview={}",
                    response.statusCode(),
                    previewBody(response.body()));
            throw new IOException("AI API returned status " + response.statusCode() + ": " + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode messageContent = root.path("choices").path(0).path("message").path("content");
        if (messageContent.isMissingNode() || messageContent.isNull()) {
            throw new IOException("AI response did not include message content");
        }

        if (messageContent.isTextual()) {
            return messageContent.asText();
        }

        if (messageContent.isArray()) {
            StringBuilder content = new StringBuilder();
            for (JsonNode node : messageContent) {
                if (node.hasNonNull("text")) {
                    content.append(node.get("text").asText());
                }
            }
            return content.toString();
        }

        return messageContent.toString();
    }

    private AiAnalysisPayload parseAnalysisPayload(String rawContent) {
        String normalized = cleanupAiText(rawContent);
        try {
            return objectMapper.readValue(normalized, AiAnalysisPayload.class);
        } catch (JsonProcessingException firstError) {
            int start = normalized.indexOf('{');
            int end = normalized.lastIndexOf('}');
            if (start >= 0 && end > start) {
                try {
                    return objectMapper.readValue(normalized.substring(start, end + 1), AiAnalysisPayload.class);
                } catch (JsonProcessingException ignored) {
                    log.warn("AI analysis payload parsing failed. error={}, contentPreview={}",
                            ignored.getMessage(),
                            previewBody(normalized));
                }
            }
            return null;
        }
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

    private SubmissionAnalysisResponse.AiInvocation modelInvocation(SubmissionAnalysisResponse fallback,
                                                                    String status,
                                                                    boolean fallbackUsed) {
        return SubmissionAnalysisResponse.AiInvocation.builder()
                .provider(PROVIDER)
                .model(model)
                .modelVersion(model)
                .promptVersion(PROMPT_VERSION)
                .agentVersion(fallback == null || fallback.getAiInvocation() == null
                        ? null
                        : fallback.getAiInvocation().getAgentVersion())
                .analysisSchemaVersion(fallback == null ? "diagnosis-v1" : fallback.getAnalysisSchemaVersion())
                .evidenceSchemaVersion(fallback == null ? DiagnosisEvidencePackage.SCHEMA_VERSION : fallback.getEvidenceSchemaVersion())
                .taxonomyVersion(fallback == null ? null : fallback.getTaxonomyVersion())
                .status(status)
                .fallbackUsed(fallbackUsed)
                .build();
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
}
