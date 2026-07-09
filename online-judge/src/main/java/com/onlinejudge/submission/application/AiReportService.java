package com.onlinejudge.submission.application;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.onlinejudge.learning.standardlibrary.application.AiStandardLibraryGrowthAgentService;
import com.onlinejudge.learning.standardlibrary.application.AiStandardLibraryService;
import com.onlinejudge.learning.standardlibrary.domain.AiStandardLibraryItem;
import com.onlinejudge.learning.standardlibrary.dto.AiStandardLibraryDiagnosticLayerResponse;
import com.onlinejudge.learning.standardlibrary.dto.AiStandardLibraryNavigationExpansionResponse;
import com.onlinejudge.learning.standardlibrary.dto.AiStandardLibraryNavigationNodeResponse;
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
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class AiReportService {

    private static final String AI_SOURCE = "MODEL_SCOPE_EXTERNAL_MODEL";
    private static final String PROVIDER = "ModelScope";
    private static final String PROMPT_VERSION = "submission-diagnosis-prompt-v2";
    private static final String STUDENT_FAST_FEEDBACK_PROMPT_VERSION = "student-fast-feedback-v2";
    private static final Pattern RUNTIME_LINE_PATTERN = Pattern.compile("\\bline\\s+(\\d+)\\b");
    private static final int STANDARD_LIBRARY_NAVIGATION_DEFAULT_MAX_ROUNDS = 6;
    private static final int STANDARD_LIBRARY_NAVIGATION_DEFAULT_MAX_ISSUES = 1;
    private static final int STANDARD_LIBRARY_NAVIGATION_MAX_BRANCHES = 3;

    private final ObjectMapper objectMapper;
    private final AiCodeAssistSupport aiCodeAssistSupport;
    private final ExternalModelAgentRuntime externalModelAgentRuntime;
    private final ExternalModelFailureClassifier failureClassifier;
    private final ExternalModelBudgetGuard budgetGuard;
    private final ExternalModelChatRequestFactory chatRequestFactory;
    private final AiStandardLibraryGrowthAgentService standardLibraryGrowthAgentService;
    private final AiStandardLibraryService standardLibraryService;
    private final StandardLibraryNavigationOutputValidator standardLibraryNavigationOutputValidator;
    private final StandardLibraryNavigationPackBuilder standardLibraryNavigationPackBuilder;
    private final ThreadLocal<ExternalModelCallTelemetry> lastCallTelemetry = ThreadLocal.withInitial(ExternalModelCallTelemetry::empty);
    private final ThreadLocal<ExternalModelCallTelemetry> lastStructuredRetrySourceTelemetry =
            ThreadLocal.withInitial(ExternalModelCallTelemetry::empty);
    private final ThreadLocal<ExternalModelRequestContext> nextRequestContext =
            ThreadLocal.withInitial(ExternalModelRequestContext::standard);
    private final ThreadLocal<String> lastModelStageRawContent = ThreadLocal.withInitial(() -> "");
    private static final ThreadLocal<Consumer<ModelCallTraceEvent>> MODEL_CALL_TRACE_SINK = new ThreadLocal<>();
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

    public AiReportService(ObjectMapper objectMapper,
                           AiCodeAssistSupport aiCodeAssistSupport,
                           ExternalModelAgentRuntime externalModelAgentRuntime,
                           ExternalModelFailureClassifier failureClassifier,
                           ExternalModelBudgetGuard budgetGuard) {
        this(objectMapper, aiCodeAssistSupport, externalModelAgentRuntime, failureClassifier, budgetGuard,
                (ExternalModelChatRequestFactory) null, null, null, null, null);
    }

    public AiReportService(ObjectMapper objectMapper,
                           AiCodeAssistSupport aiCodeAssistSupport,
                           ExternalModelAgentRuntime externalModelAgentRuntime,
                           ExternalModelFailureClassifier failureClassifier,
                           ExternalModelBudgetGuard budgetGuard,
                           ExternalModelChatRequestFactory chatRequestFactory) {
        this(objectMapper, aiCodeAssistSupport, externalModelAgentRuntime, failureClassifier, budgetGuard,
                chatRequestFactory, null, null, null, null);
    }

    @Autowired
    public AiReportService(ObjectMapper objectMapper,
                           AiCodeAssistSupport aiCodeAssistSupport,
                           ExternalModelAgentRuntime externalModelAgentRuntime,
                           ExternalModelFailureClassifier failureClassifier,
                           ExternalModelBudgetGuard budgetGuard,
                           AiStandardLibraryGrowthAgentService standardLibraryGrowthAgentService,
                           AiStandardLibraryService standardLibraryService,
                           StandardLibraryNavigationOutputValidator standardLibraryNavigationOutputValidator,
                           StandardLibraryNavigationPackBuilder standardLibraryNavigationPackBuilder) {
        this(objectMapper, aiCodeAssistSupport, externalModelAgentRuntime, failureClassifier, budgetGuard,
                new ExternalModelChatRequestFactory(), standardLibraryGrowthAgentService, standardLibraryService,
                standardLibraryNavigationOutputValidator, standardLibraryNavigationPackBuilder);
    }

    public AiReportService(ObjectMapper objectMapper,
                           AiCodeAssistSupport aiCodeAssistSupport,
                           ExternalModelAgentRuntime externalModelAgentRuntime,
                           ExternalModelFailureClassifier failureClassifier,
                           ExternalModelBudgetGuard budgetGuard,
                           ExternalModelChatRequestFactory chatRequestFactory,
                           AiStandardLibraryGrowthAgentService standardLibraryGrowthAgentService,
                           AiStandardLibraryService standardLibraryService,
                           StandardLibraryNavigationOutputValidator standardLibraryNavigationOutputValidator,
                           StandardLibraryNavigationPackBuilder standardLibraryNavigationPackBuilder) {
        this.objectMapper = objectMapper;
        this.aiCodeAssistSupport = aiCodeAssistSupport;
        this.externalModelAgentRuntime = externalModelAgentRuntime;
        this.failureClassifier = failureClassifier == null ? new ExternalModelFailureClassifier() : failureClassifier;
        this.budgetGuard = budgetGuard == null ? new ExternalModelBudgetGuard() : budgetGuard;
        this.chatRequestFactory = chatRequestFactory == null ? new ExternalModelChatRequestFactory() : chatRequestFactory;
        this.standardLibraryGrowthAgentService = standardLibraryGrowthAgentService;
        this.standardLibraryService = standardLibraryService;
        this.standardLibraryNavigationOutputValidator = standardLibraryNavigationOutputValidator == null
                ? new StandardLibraryNavigationOutputValidator()
                : standardLibraryNavigationOutputValidator;
        this.standardLibraryNavigationPackBuilder = standardLibraryNavigationPackBuilder;
    }

    @Value("${ai.enabled:true}")
    private boolean enabled;

    @Value("${ai.base-url:https://api-inference.modelscope.cn/v1}")
    private String baseUrl;

    @Value("${ai.api-key:}")
    private String apiKey;

    @Value("${ai.model:Qwen/Qwen3-235B-A22B-Instruct-2507}")
    private String model;

    @Value("${ai.modelscope-compatible-request:auto}")
    private String modelScopeCompatibleRequest = "auto";

    @Value("${ai.timeout-seconds:30}")
    private long timeoutSeconds;

    @Value("${ai.external-runtime-enabled:true}")
    private boolean externalRuntimeEnabled;

    @Value("${ai.external-runtime-profile:standard}")
    private String externalRuntimeProfile = ExternalModelAgentRuntime.RUNTIME_PROFILE_STANDARD;

    @Value("${ai.max-output-tokens:1800}")
    private int maxOutputTokens = 1800;

    @Value("${ai.structured-retry-output-tokens:2600}")
    private int structuredRetryOutputTokens = 2600;

    @Value("${ai.stream-enabled:true}")
    private boolean streamEnabled;

    @Value("${ai.stream-fallback-enabled:false}")
    private boolean streamFallbackEnabled = false;

    @Value("${ai.structured-retry-enabled:false}")
    private boolean structuredRetryEnabled = false;

    @Value("${ai.retry.max-attempts:1}")
    private int retryMaxAttempts;

    @Value("${ai.retry.backoff-ms:700}")
    private long retryBackoffMs;

    @Value("${ai.student-feedback.min-request-interval-ms:2000}")
    private long studentFeedbackMinRequestIntervalMs;

    @Value("${ai.standard-library-navigation.max-rounds:6}")
    private int standardLibraryNavigationMaxRounds = STANDARD_LIBRARY_NAVIGATION_DEFAULT_MAX_ROUNDS;

    @Value("${ai.standard-library-navigation.max-issues:1}")
    private int standardLibraryNavigationMaxIssues = STANDARD_LIBRARY_NAVIGATION_DEFAULT_MAX_ISSUES;

    private final Object studentFeedbackThrottle = new Object();
    private long lastStudentFeedbackRequestAtMs = 0L;

    public SubmissionAnalysisResponse enhanceSubmissionAnalysis(Problem problem,
                                                                Submission submission,
                                                                SubmissionAnalysisResponse baseline) {
        return enhanceSubmissionAnalysis(problem, submission, baseline, null);
    }

    public SubmissionAnalysisResponse enhanceSubmissionAnalysis(Problem problem,
                                                                Submission submission,
                                                                SubmissionAnalysisResponse baseline,
                                                                DiagnosisEvidencePackage evidencePackage) {
        if (!canCallAi()) {
            log.info("AI submission analysis skipped because AI access is unavailable. submissionId={}", submission.getId());
            return runtimeFailure(baseline, aiUnavailableFailure("SUBMISSION_ANALYSIS"));
        }

        try {
            log.info("AI submission analysis started. submissionId={}, problemId={}, language={}",
                    submission.getId(),
                    submission.getProblemId(),
                    submission.getLanguageName());
            String rawSourceCode = submission.getSourceCode() == null ? "" : submission.getSourceCode();
            List<SubmissionAnalysisResponse.LineIssue> baselineLineIssues = baseline == null || baseline.getLineIssues() == null
                    ? List.of()
                    : baseline.getLineIssues();
            if (shouldUseExternalRuntime(evidencePackage)) {
                return enhanceWithExternalRuntime(
                        submission,
                        baseline,
                        evidencePackage,
                        rawSourceCode,
                        baselineLineIssues
                );
            }
            log.info("AI submission analysis skipped because new runtime context is incomplete. submissionId={}",
                    submission.getId());
            return runtimeFailure(baseline, stageFailure(
                    "SUBMISSION_ANALYSIS",
                    ModelStageFailureReason.UNKNOWN_ERROR,
                    "External model runtime context is incomplete."
            ));
        } catch (Exception exception) {
            log.error("AI submission analysis enhancement failed. submissionId={}", submission.getId(), exception);
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            if (shouldUseExternalRuntime(evidencePackage)) {
                return runtimeFailure(baseline, stageFailureFromException("SUBMISSION_ANALYSIS", exception));
            }
            return runtimeFailure(baseline, stageFailureFromException("SUBMISSION_ANALYSIS", exception));
        }
    }

    public StudentAiFeedbackResponse generateStudentAiFeedback(Problem problem,
                                                               Submission submission,
                                                               DiagnosisEvidencePackage evidencePackage) {
        long startedAt = System.nanoTime();
        if (!canCallAi()) {
            return feedbackFailure(submission, "FAILED", "AI_UNAVAILABLE", startedAt);
        }

        try {
            Map<String, String> evidenceCandidateRefs = Map.of();
            ExternalModelAgentRuntime.RuntimePlan runtimePlan = prepareStudentFeedbackRuntimePlan(evidencePackage);
            Map<String, Object> context = compactStudentFastFeedbackContext(problem, submission, evidencePackage, runtimePlan);

            String systemPrompt = """
                    你是高中信息学在线判题系统的学生快反馈教练。
                    只返回严格压缩 JSON。不要输出 markdown、思维链、解释性前后缀或额外文本。
                    所有学生可见文字必须使用简体中文。

                    Shape:
                    {"studentReport":{"basicLayerText":"","improvementLayerText":"","nextActionText":""},"repairItems":[{"title":"","body":"","kind":"","libraryItemId":"","skillUnitId":"","mistakePointId":"","improvementPointId":"","libraryFit":"HIT|PARTIAL|MISS|OUT_OF_LIBRARY","knowledgePath":[],"evidenceRefs":[],"qualitySignals":["evidence_grounded","actionable","no_answer_leak"]}],"improvementItems":[{"title":"","body":"","kind":"IMPROVEMENT","libraryItemId":"","skillUnitId":"","mistakePointId":"","improvementPointId":"","libraryFit":"HIT|PARTIAL|MISS|OUT_OF_LIBRARY","knowledgePath":[],"evidenceRefs":[],"qualitySignals":["transfer"]}],"diagnosisCandidates":[{"name":"","layer":"BASIC|IMPROVEMENT","libraryFit":"HIT|PARTIAL|MISS|OUT_OF_LIBRARY","anchorId":"","anchorType":"KNOWLEDGE_NODE|SKILL_UNIT|MISTAKE_POINT|IMPROVEMENT_POINT|OUT_OF_LIBRARY","libraryPath":[],"role":"PRIMARY|SECONDARY","evidenceRefs":[],"reason":"","confidence":0.8}],"libraryGrowth":{"candidates":[{"name":"","suggestedPath":[],"similarExistingItems":[],"evidenceRefs":[],"evidenceStatus":"SUPPORTED|NO_DIRECT_CODE_EVIDENCE","errorSymptom":"","typicalCodePattern":"","studentExplanation":"","reason":"","status":"NEEDS_REVIEW","confidence":0.75}]},"nextQuestion":"","safety":{"answerLeakRisk":"LOW|MEDIUM|HIGH","blockedReasons":[]},"evidenceRefs":[]}

                    规则:
                    1. studentReport 是学生真正看到的摘要。用自然短句写：先说现象，再说可能原因，再给检查动作。
                    2. basicLayerText 写 2-3 个短句，90-170 个中文字符，概括当前基础层重点；不要把多条独立错误硬塞成一个长句。
                    3. improvementLayerText 写 1-2 个短句，70-140 个中文字符，概括修复基础问题后值得提升的方向；不要复述基础层。
                    4. nextActionText 只写 1 个学生马上能做的自查动作，用一句话表达，不要编号，不超过 60 个中文字符。
                    5. repairItems 和 improvementItems 都按真实证据返回 0 到多条：有几个彼此独立的错误就给几条 repairItems，有几个真实提升方向就给几条 improvementItems；不要强行合并成一条，也不要为了显得丰富而凑数。
                    6. 必须让题目目标、代码行为、评测结果三者能对上；不要猜测判题未提供的信息。
                    7. 后端没有预选错误代码行；你必须自己阅读 submission.sourceCodeWithLineNumbers，并在 evidenceRefs 中填写 code:line:N 或 code:range:A-B。不要使用 E1/E2 这类候选 ID。不能猜隐藏测试。
                    8. 禁止给最终代码、完整答案、完整修改方案、逐行改法或“把这一行改成...”这类可复制表达；建议动作写成检查、手推、比较、核对，不写删除、替换、改成。
                    9. 学生可见文字不能复述 verdict:、code:、evidenceRefs、judgeFacts 等内部字段名或证据标记。
                    10. 如果证据不足或泄题风险为 HIGH，返回空建议，并在 blockedReasons 说明原因。
                    11. 如果运行错误信息里出现行号，基础层必须回到 submission.sourceCodeWithLineNumbers 中核对对应代码行；不要把“代码很长、helper 太多、需要精简”当作主因，除非它直接导致该行异常。
                    12. 如果异常是 IndexError/list index out of range，基础层必须围绕“下标访问范围”和“容器长度”解释，不要只给代码整理建议。
                    13. 如果基础层是下标越界，提高层优先写边界样例、循环边界、数组长度核对等迁移能力；不要把“精简代码/删除 helper”作为唯一提高层。
                    14. knowledgePath 是父子关系路径，不是独立标签；使用 3-5 段中文知识树路径，例如 ["程序基础","数组/列表","下标访问","越界检查"]；不确定时可以留空，后端只做结构校验。
                    15. standardLibrary 是教学参考规范包，不是强制答案；先自由诊断真实问题，再逐条对齐标准库。能精确命中就填写对应 ID，半命中写 PARTIAL，不匹配写 MISS 或 OUT_OF_LIBRARY 并允许 ID 留空。
                    16. 如果 standardLibrary 中有 primaryKnowledgeNodeCode，knowledgePath 优先沿“主知识点 -> 能力点 -> 易错点/提升点”生成；relatedKnowledgeNodeCodes 只作辅助区分，不要拆成多个独立标签或独立错误。
                    17. 如果 judgeFacts 暴露多个不同失败现象，逐个核对是否来自不同根因；不要只解释第一个能说通的现象就停止。
                    18. diagnosisCandidates 是后台审计线，不展示给学生；每个真实诊断点都要有一个候选，说明它如何 HIT/PARTIAL/MISS/OUT_OF_LIBRARY。
                    19. libraryGrowth 是标准库成长线，只来自 PARTIAL、MISS 或 OUT_OF_LIBRARY 的真实诊断点；HIT 不要生成成长候选。status 固定 NEEDS_REVIEW，sourceProblemId/sourceSubmissionId 不要填写，后端会补。
                    """;
            String userPrompt = "请根据以下上下文生成 StudentAiFeedback。上下文只用于诊断，不要把内部字段名写进学生反馈："
                    + objectMapper.writeValueAsString(context);
            int fastFeedbackOutputTokens = Math.max(700, maxOutputTokens);
            String content = chatCompletionForStudentFeedback(systemPrompt, userPrompt, fastFeedbackOutputTokens);
            StudentFastFeedbackPayload payload = parseModelStagePayload(content, StudentFastFeedbackPayload.class);
            StudentAiFeedbackResponse response = normalizeStudentFastFeedback(payload, submission, startedAt, evidenceCandidateRefs);
            if (!"READY".equals(response.getStatus())) {
                return response;
            }
            persistStudentFastFeedbackGrowthCandidates(payload, response, submission, evidenceCandidateRefs);
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

    private boolean shouldUseExternalRuntime(DiagnosisEvidencePackage evidencePackage) {
        return externalRuntimeEnabled
                && externalModelAgentRuntime != null
                && evidencePackage != null;
    }

    private SubmissionAnalysisResponse enhanceWithExternalRuntime(Submission submission,
                                                                  SubmissionAnalysisResponse baseline,
                                                                  DiagnosisEvidencePackage evidencePackage,
                                                                  String rawSourceCode,
                                                                  List<SubmissionAnalysisResponse.LineIssue> baselineLineIssues)
            throws JsonProcessingException, IOException, InterruptedException {
        ExternalModelAgentRuntime.RuntimePlan runtimePlan = externalModelAgentRuntime.prepare(
                evidencePackage,
                baseline,
                teacherDiagnosisRuntimeProfile()
        );
        ExternalModelStagePayloads.StageValidationResult navigationResult =
                applyAiStandardLibraryNavigation(runtimePlan);
        if (!navigationResult.isValid()) {
            return runtimeFailure(baseline, runtimePlan, navigationResult);
        }
        return enhanceWithAdviceGenerationRuntime(
                submission,
                baseline,
                runtimePlan,
                rawSourceCode,
                baselineLineIssues
        );
    }

    private ExternalModelAgentRuntime.RuntimePlan prepareStudentFeedbackRuntimePlan(
            DiagnosisEvidencePackage evidencePackage) {
        ExternalModelAgentRuntime.RuntimePlan runtimePlan = null;
        if (externalModelAgentRuntime == null || evidencePackage == null) {
            StandardLibraryPack pack = localStudentStandardLibraryPack(evidencePackage);
            return pack == null ? null : ExternalModelAgentRuntime.RuntimePlan.builder()
                    .standardLibraryPack(pack)
                    .build();
        }
        try {
            runtimePlan = externalModelAgentRuntime.prepare(
                    evidencePackage,
                    null,
                    externalRuntimeProfile
            );
        } catch (Exception exception) {
            log.warn("Student AI feedback runtime context unavailable. reason={}", exception.getMessage());
        }
        StandardLibraryPack pack = localStudentStandardLibraryPack(evidencePackage);
        if (pack == null) {
            return runtimePlan;
        }
        if (runtimePlan == null) {
            return ExternalModelAgentRuntime.RuntimePlan.builder()
                    .standardLibraryPack(pack)
                    .build();
        }
        if (runtimePlan.getStandardLibraryPack() == null) {
            runtimePlan.setStandardLibraryPack(pack);
        }
        return runtimePlan;
    }

    private StandardLibraryPack localStudentStandardLibraryPack(DiagnosisEvidencePackage evidencePackage) {
        if (standardLibraryService == null || standardLibraryNavigationPackBuilder == null || evidencePackage == null) {
            return null;
        }
        try {
            List<AiStandardLibraryItem> items = standardLibraryService.enabledNavigationItems();
            if (items.isEmpty()) {
                return emptyStandardLibraryPack("LIBRARY_EMPTY", "", "");
            }
            String haystack = studentRecallHaystack(evidencePackage);
            List<AiStandardLibraryItem> ranked = items.stream()
                    .filter(item -> item != null)
                    .sorted(Comparator
                            .comparingInt((AiStandardLibraryItem item) -> localRecallScore(item, haystack))
                            .reversed()
                            .thenComparing(item -> defaultIfBlank(item.getCategory(), ""))
                            .thenComparing(item -> defaultIfBlank(item.getCode(), "")))
                    .limit(40)
                    .toList();
            return standardLibraryNavigationPackBuilder.buildFromItems(ranked, "LOCAL_RECALL",
                    "本地召回树形标准库包，供单诊断 Agent 参考。");
        } catch (Exception exception) {
            log.warn("Student AI feedback local standard library recall failed. reason={}", exception.getMessage());
            return emptyStandardLibraryPack("ATTACHMENT_FAILED", exception.getMessage(), "");
        }
    }

    private String studentRecallHaystack(DiagnosisEvidencePackage evidencePackage) {
        StringBuilder builder = new StringBuilder();
        DiagnosisEvidencePackage.ProblemEvidence problem = evidencePackage.getProblem();
        if (problem != null) {
            builder.append(problem.getTitle()).append('\n')
                    .append(problem.getDescription()).append('\n')
                    .append(problem.getDifficulty()).append('\n')
                    .append(String.join("\n", cleanList(problem.getKnowledgePoints(), List.of()))).append('\n')
                    .append(String.join("\n", cleanList(problem.getCommonMistakes(), List.of()))).append('\n');
        }
        DiagnosisEvidencePackage.SubmissionEvidence submission = evidencePackage.getSubmission();
        if (submission != null) {
            builder.append(submission.getLanguage()).append('\n')
                    .append(submission.getVerdict()).append('\n')
                    .append(submission.getSourceCodeWithLineNumbers()).append('\n');
        }
        DiagnosisEvidencePackage.JudgeFacts judgeFacts = evidencePackage.getJudgeFacts();
        if (judgeFacts != null) {
            builder.append(judgeFacts.getRuntimeErrorMessage()).append('\n')
                    .append(judgeFacts.getCompileOutput()).append('\n');
            for (DiagnosisEvidencePackage.CaseSummary item : judgeFacts.getCaseResultsSummary() == null
                    ? List.<DiagnosisEvidencePackage.CaseSummary>of()
                    : judgeFacts.getCaseResultsSummary()) {
                if (item == null || Boolean.TRUE.equals(item.getHidden())) {
                    continue;
                }
                builder.append(item.getActualOutputPreview()).append('\n')
                        .append(item.getExpectedOutputPreview()).append('\n');
            }
        }
        return normalizeRecallText(builder.toString());
    }

    private int localRecallScore(AiStandardLibraryItem item, String haystack) {
        if (item == null || haystack == null || haystack.isBlank()) {
            return 0;
        }
        int score = 0;
        score += recallFieldScore(haystack, item.getName(), 8);
        score += recallFieldScore(haystack, item.getCategory(), 5);
        score += recallFieldScore(haystack, item.getDescription(), 3);
        score += recallFieldScore(haystack, item.getCommonMisconception(), 3);
        score += recallFieldScore(haystack, item.getEvidenceSignals(), 4);
        score += recallFieldScore(haystack, item.getCommonCodePatterns(), 6);
        score += recallFieldScore(haystack, item.getJudgeSignals(), 6);
        score += recallFieldScore(haystack, item.getWhenToUse(), 3);
        score += recallFieldScore(haystack, item.getStudentBenefit(), 2);
        return score;
    }

    private int recallFieldScore(String haystack, String rawValue, int weight) {
        String value = normalizeRecallText(rawValue);
        if (value.isBlank()) {
            return 0;
        }
        if (haystack.contains(value)) {
            return weight * 3;
        }
        int score = 0;
        for (String token : value.split("[^\\p{IsHan}A-Za-z0-9_]+")) {
            if (token.length() >= 2 && haystack.contains(token)) {
                score += weight;
            }
        }
        for (int index = 0; index + 4 <= value.length(); index += 2) {
            String fragment = value.substring(index, index + 4);
            if (haystack.contains(fragment)) {
                score += Math.max(1, weight / 2);
                break;
            }
        }
        return score;
    }

    private String normalizeRecallText(String value) {
        return value == null ? "" : value.toLowerCase().replaceAll("\\s+", " ").trim();
    }

    private SubmissionAnalysisResponse enhanceWithAdviceGenerationRuntime(
            Submission submission,
            SubmissionAnalysisResponse baseline,
            ExternalModelAgentRuntime.RuntimePlan runtimePlan,
            String rawSourceCode,
            List<SubmissionAnalysisResponse.LineIssue> baselineLineIssues)
            throws JsonProcessingException, IOException, InterruptedException {
        String promptVersion = runtimePromptVersion(runtimePlan);
        AdviceGenerationOutput adviceOutput;
        try {
            adviceOutput = callAdviceGenerationStage(runtimePlan);
        } catch (Exception exception) {
            ExternalModelStagePayloads.StageValidationResult failure =
                    stageFailureFromException("DIAGNOSIS_AND_ADVICE", exception);
            runtimePlan.setAdviceGenerationResult(AdviceGenerationResult.failed(
                    adviceFailureReason(failure),
                    promptVersion
            ));
            return runtimeFailure(baseline, runtimePlan, failure);
        }
        adviceOutput = externalModelAgentRuntime.normalizeAdviceGeneration(adviceOutput, runtimePlan);

        ExternalModelStagePayloads.StageValidationResult adviceValidation =
                withStage("DIAGNOSIS_AND_ADVICE",
                        externalModelAgentRuntime.validateAdviceGeneration(adviceOutput, runtimePlan));
        if (!adviceValidation.isValid()
                && adviceValidation.getFailureReason() == ModelStageFailureReason.SAFETY_RISK) {
            try {
                AdviceGenerationOutput rewrittenOutput =
                        retryAdviceGenerationForSafety(runtimePlan, adviceOutput, adviceValidation);
                rewrittenOutput = externalModelAgentRuntime.normalizeAdviceGeneration(rewrittenOutput, runtimePlan);
                ExternalModelStagePayloads.StageValidationResult rewrittenValidation =
                        withStage("DIAGNOSIS_AND_ADVICE",
                                externalModelAgentRuntime.validateAdviceGeneration(rewrittenOutput, runtimePlan));
                if (rewrittenValidation.isValid()) {
                    adviceOutput = rewrittenOutput;
                    adviceValidation = rewrittenValidation;
                } else {
                    adviceValidation = rewrittenValidation;
                }
            } catch (Exception exception) {
                adviceValidation = stageFailureFromException("DIAGNOSIS_AND_ADVICE", exception);
            }
        }
        if (!adviceValidation.isValid()) {
            adviceValidation = withTransportAttribution(adviceValidation);
            runtimePlan.setAdviceGenerationResult(AdviceGenerationResult.failed(
                    adviceFailureReason(adviceValidation),
                    promptVersion
            ));
            log.warn("External model advice generation failed validation. submissionId={}, reason={}, message={}",
                    submission.getId(),
                    adviceValidation.getFailureReason(),
                    adviceValidation.getMessage());
            return runtimeFailure(baseline, runtimePlan, adviceValidation);
        }

        persistStandardLibraryGrowthCandidates(adviceOutput, submission);
        runtimePlan.setAdviceGenerationResult(AdviceGenerationResult.success(adviceOutput, promptVersion));

        SubmissionAnalysisResponse.StudentFeedback studentFeedback =
                externalModelAgentRuntime.mapAdviceStudentFeedback(adviceOutput, runtimePlan);
        studentFeedback = externalModelAgentRuntime.normalizeStudentFeedback(studentFeedback, runtimePlan);
        ExternalModelStagePayloads.StageValidationResult feedbackValidation =
                withStage("DIAGNOSIS_AND_ADVICE",
                        externalModelAgentRuntime.validateStudentFeedback(studentFeedback, runtimePlan));
        if (!feedbackValidation.isValid()) {
            feedbackValidation = withTransportAttribution(feedbackValidation);
            runtimePlan.setAdviceGenerationResult(AdviceGenerationResult.failed(
                    adviceFailureReason(feedbackValidation),
                    promptVersion
            ));
            return runtimeFailure(baseline, runtimePlan, feedbackValidation);
        }

        SubmissionAnalysisResponse response = buildAdviceRuntimeAnalysisResponse(
                baseline,
                runtimePlan,
                adviceOutput,
                studentFeedback,
                rawSourceCode,
                baselineLineIssues
        );
        return response;
    }

    private void persistStandardLibraryGrowthCandidates(AdviceGenerationOutput adviceOutput, Submission submission) {
        if (standardLibraryGrowthAgentService == null || adviceOutput == null) {
            return;
        }
        try {
            Long sourceProblemId = submission == null ? null : submission.getProblemId();
            Long sourceSubmissionId = submission == null ? null : submission.getId();
            standardLibraryGrowthAgentService.proposeFromDiagnosisOutput(
                    adviceOutput,
                    sourceProblemId,
                    sourceSubmissionId,
                    null
            );
        } catch (Exception exception) {
            log.warn("Failed to persist AI standard library growth candidates. reason={}", exception.getMessage());
        }
    }

    private void persistStudentFastFeedbackGrowthCandidates(StudentFastFeedbackPayload payload,
                                                            StudentAiFeedbackResponse response,
                                                            Submission submission,
                                                            Map<String, String> evidenceCandidateRefs) {
        if (standardLibraryGrowthAgentService == null || response == null) {
            return;
        }
        AdviceGenerationOutput.LibraryGrowth growth = normalizeFastFeedbackGrowthEvidence(
                payload == null ? null : payload.libraryGrowth,
                evidenceCandidateRefs
        );
        if (growth == null || growth.getCandidates() == null || growth.getCandidates().isEmpty()) {
            growth = deriveFastFeedbackGrowth(response);
        }
        if (growth == null || growth.getCandidates() == null || growth.getCandidates().isEmpty()) {
            return;
        }
        persistStandardLibraryGrowthCandidates(AdviceGenerationOutput.builder()
                .libraryGrowth(growth)
                .build(), submission);
    }

    private AdviceGenerationOutput.LibraryGrowth normalizeFastFeedbackGrowthEvidence(
            AdviceGenerationOutput.LibraryGrowth growth,
            Map<String, String> evidenceCandidateRefs) {
        if (growth == null || growth.getCandidates() == null) {
            return growth;
        }
        for (AdviceGenerationOutput.LibraryGrowthCandidate candidate : growth.getCandidates()) {
            if (candidate != null) {
                candidate.setEvidenceRefs(resolveStudentEvidenceRefs(candidate.getEvidenceRefs(), evidenceCandidateRefs));
            }
        }
        return growth;
    }

    private AdviceGenerationOutput.LibraryGrowth deriveFastFeedbackGrowth(StudentAiFeedbackResponse response) {
        if (response.getRepairItems() == null || response.getRepairItems().isEmpty()) {
            return null;
        }
        List<AdviceGenerationOutput.LibraryGrowthCandidate> candidates = response.getRepairItems().stream()
                .filter(item -> item != null && shouldEnterGrowthPool(item.getLibraryFit()))
                .map(this::growthCandidateFromFastFeedback)
                .toList();
        return candidates.isEmpty() ? null : AdviceGenerationOutput.LibraryGrowth.builder()
                .candidates(candidates)
                .build();
    }

    private boolean shouldEnterGrowthPool(String libraryFit) {
        String fit = defaultIfBlank(libraryFit, "").trim().toUpperCase();
        return "PARTIAL".equals(fit) || "MISS".equals(fit) || "OUT_OF_LIBRARY".equals(fit);
    }

    private AdviceGenerationOutput.LibraryGrowthCandidate growthCandidateFromFastFeedback(
            StudentAiFeedbackResponse.FeedbackItem item) {
        List<String> path = item.getKnowledgePath() == null || item.getKnowledgePath().isEmpty()
                ? List.of("未归类", "AI快反馈库外发现")
                : item.getKnowledgePath();
        List<String> similarItems = java.util.stream.Stream.of(
                        item.getLibraryItemId(),
                        item.getSkillUnitId(),
                        item.getMistakePointId(),
                        item.getImprovementPointId()
                )
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .toList();
        return AdviceGenerationOutput.LibraryGrowthCandidate.builder()
                .name(defaultIfBlank(item.getTitle(), "AI快反馈库外发现"))
                .suggestedPath(path)
                .similarExistingItems(similarItems)
                .evidenceRefs(item.getEvidenceRefs())
                .evidenceStatus(item.getEvidenceRefs() == null || item.getEvidenceRefs().isEmpty()
                        ? "NO_DIRECT_CODE_EVIDENCE"
                        : "SUPPORTED")
                .errorSymptom(truncateText(item.getBody(), 180))
                .typicalCodePattern(item.getEvidenceSnippets() == null || item.getEvidenceSnippets().isEmpty()
                        ? "见本次提交诊断证据。"
                        : truncateText(item.getEvidenceSnippets().get(0).getCode(), 180))
                .studentExplanation(truncateText(item.getBody(), 180))
                .reason("学生快反馈中出现 " + defaultIfBlank(item.getLibraryFit(), "UNKNOWN")
                        + " 诊断点，说明当前标准库可能缺少对应细颗粒条目。")
                .status("NEEDS_REVIEW")
                .confidence(0.7)
                .build();
    }

    private String teacherDiagnosisRuntimeProfile() {
        return ExternalModelAgentRuntime.RUNTIME_PROFILE_STANDARD;
    }

    private AdviceGenerationOutput callAdviceGenerationStage(
            ExternalModelAgentRuntime.RuntimePlan runtimePlan) throws JsonProcessingException, IOException, InterruptedException {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("task", "请完成一次高中信息学提交最终诊断：以自由诊断 issues 为主输入，结合可选标准库挂接结果，生成多条学生建议和结构化元数据。");
        request.put("contextPolicy", List.of(
                "problem.description 是完整题目描述；submission.sourceCodeWithLineNumbers 是完整学生代码或最大可用带行号代码。",
                "submission.verdict、visibleCaseFacts、runtimeErrorMessage、compileOutput 和 evidenceRefs 只是判题参考信号，用来验证诊断。",
                "freeDiagnosis.issues 是本阶段最重要的输入；每个 issue 都代表一个有证据支持的独立诊断点。",
                "libraryAnchors 是后端按 issue 逐层浏览标准库得到的可选挂接结果；anchorStatus 不是学生可见文案。",
                "standardLibrary 是教学参考规范包，用于统一术语、路径和颗粒度；即使为空或未命中，也必须继续基于 issues 生成学生建议。",
                "primaryKnowledgeNodeCode 是主知识路径，relatedKnowledgeNodeCodes 只是辅助上下文；学生端知识路径优先沿主路径表达，不把相关标签平铺成独立问题。",
                "basicLayerAdvice 应尽量一条对应一个主要 issue；多个 issue 不要压缩成一条笼统建议。",
                "improvementLayerAdvice 可以围绕复盘、自测、迁移和调试习惯给出多条方向，但不能重复基础层建议。",
                "如果某个 issue 没有标准库 anchor，相关标准库 id 可以留空，不要因此删除该 issue 的建议。",
                "学生可见反馈要自然、具体、可行动，但不能给完整答案或逐行改法。"
        ));
        request.put("brief", runtimePlan.getBrief());
        request.put("freeDiagnosis", runtimePlan.getFreeDiagnosisOutput());
        request.put("issues", runtimePlan.getFreeDiagnosisOutput() == null
                ? List.of()
                : safeList(runtimePlan.getFreeDiagnosisOutput().getIssues()));
        request.put("libraryAnchors", safeList(runtimePlan.getIssueLibraryAnchors()));
        request.put("navigationResult", runtimePlan.getStandardLibraryNavigationOutput());
        request.put("standardLibrary", runtimePlan.getStandardLibraryPack());
        activateRuntimePlan(runtimePlan);
        String systemPrompt = runtimePlan.getAdvicePrompt().getSystemPrompt();
        String userPrompt = objectMapper.writeValueAsString(request);
        String content = chatCompletion(systemPrompt, userPrompt);
        lastStructuredRetrySourceTelemetry.set(ExternalModelCallTelemetry.empty());
        AdviceGenerationOutput output = parseModelStagePayload(content, AdviceGenerationOutput.class);
        if (output != null) {
            return output;
        }
        return retryStructuredModelStagePayload(
                systemPrompt,
                userPrompt,
                runtimePlan,
                AdviceGenerationOutput.class
        );
    }

    private ExternalModelStagePayloads.StageValidationResult applyAiStandardLibraryNavigation(
            ExternalModelAgentRuntime.RuntimePlan runtimePlan) {
        if (runtimePlan == null) {
            return invalidStage("AI_NAVIGATION", ModelStageFailureReason.INVALID_JSON, "runtimePlan is missing.");
        }
        FreeDiagnosisOutput freeDiagnosis;
        try {
            freeDiagnosis = callFreeDiagnosisStage(runtimePlan);
            if (freeDiagnosis == null) {
                return invalidStage("FREE_DIAGNOSIS", ModelStageFailureReason.EMPTY_RESPONSE,
                        "free diagnosis output is empty.");
            }
        } catch (Exception exception) {
            return stageFailureFromException("FREE_DIAGNOSIS", exception);
        }
        List<FreeDiagnosisOutput.Issue> issues = validatedFreeDiagnosisIssues(
                freeDiagnosis,
                runtimePlan.getBrief()
        );
        if (issues.isEmpty()) {
            return invalidStage("FREE_DIAGNOSIS", ModelStageFailureReason.INVALID_EVIDENCE_REF,
                    "free diagnosis output contains no valid issues.");
        }
        freeDiagnosis.setIssues(issues);
        runtimePlan.setFreeDiagnosisOutput(freeDiagnosis);
        applyIssueLibraryAnchors(runtimePlan, issues);
        return ExternalModelStagePayloads.StageValidationResult.builder()
                .valid(true)
                .stage("AI_NAVIGATION")
                .failureReason(ModelStageFailureReason.NONE)
                .message("")
                .softFixes(List.of())
                .hardFailures(List.of())
                .build();
    }

    private void applyIssueLibraryAnchors(ExternalModelAgentRuntime.RuntimePlan runtimePlan,
                                          List<FreeDiagnosisOutput.Issue> issues) {
        if (standardLibraryService == null || standardLibraryNavigationPackBuilder == null) {
            List<IssueLibraryAnchor> anchors = anchorsWithStatus(issues, "ATTACHMENT_FAILED",
                    "STANDARD_LIBRARY_NAVIGATION_SERVICES_UNAVAILABLE");
            setLayeredAttachmentArtifacts(runtimePlan, anchors, "ATTACHMENT_FAILED",
                    "STANDARD_LIBRARY_NAVIGATION_SERVICES_UNAVAILABLE");
            return;
        }
        List<AiStandardLibraryNavigationNodeResponse> roots;
        try {
            roots = safeNavigationNodes(standardLibraryService.listRootKnowledgeAreas());
        } catch (Exception exception) {
            List<IssueLibraryAnchor> anchors = anchorsWithStatus(issues, "ATTACHMENT_FAILED",
                    exception.getMessage());
            setLayeredAttachmentArtifacts(runtimePlan, anchors, "ATTACHMENT_FAILED", exception.getMessage());
            return;
        }
        if (roots.isEmpty()) {
            List<IssueLibraryAnchor> anchors = anchorsWithStatus(issues, "LIBRARY_EMPTY",
                    "standard library root is empty.");
            setLayeredAttachmentArtifacts(runtimePlan, anchors, "LIBRARY_EMPTY", "standard library root is empty.");
            return;
        }
        List<IssueLibraryAnchor> anchors = new ArrayList<>();
        int maxIssues = navigationMaxIssues();
        for (int index = 0; index < safeList(issues).size(); index++) {
            FreeDiagnosisOutput.Issue issue = issues.get(index);
            if (index >= maxIssues) {
                anchors.add(anchorWithStatus(issue, "NO_MATCH", List.of(),
                        "standard library attachment skipped by max issue limit.", null));
                continue;
            }
            anchors.add(attachIssueToStandardLibrary(runtimePlan, issue, roots));
        }
        setLayeredAttachmentArtifacts(runtimePlan, anchors, aggregateAnchorStatus(anchors), "");
    }

    private List<FreeDiagnosisOutput.Issue> validatedFreeDiagnosisIssues(FreeDiagnosisOutput output,
                                                                         ModelDiagnosisBrief brief) {
        List<FreeDiagnosisOutput.Issue> source = output == null ? List.of() : safeList(output.getIssues());
        if (source.isEmpty()) {
            source = issuesFromHypotheses(output);
        }
        Set<String> validRefs = EvidenceRefSupport.validEvidenceRefs(brief);
        List<String> orderedRefs = EvidenceRefSupport.orderedEvidenceRefs(brief);
        List<FreeDiagnosisOutput.Issue> validIssues = new ArrayList<>();
        int index = 1;
        for (FreeDiagnosisOutput.Issue raw : source) {
            if (raw == null) {
                continue;
            }
            List<String> evidenceRefs = EvidenceRefSupport.normalizeEvidenceRefs(
                    raw.getEvidenceRefs(),
                    validRefs,
                    orderedRefs,
                    brief,
                    null
            );
            String invalidEvidence = EvidenceRefSupport.invalidEvidenceRefs(
                    evidenceRefs,
                    validRefs,
                    brief,
                    "freeDiagnosis.issues.evidenceRefs",
                    true
            );
            if (!invalidEvidence.isBlank()) {
                continue;
            }
            String title = cleanupAiText(raw.getTitle());
            if (title.isBlank()) {
                continue;
            }
            Double confidence = raw.getConfidence();
            if (confidence == null || confidence < 0 || confidence > 1
                    || confidence.isNaN() || confidence.isInfinite()) {
                confidence = 0.6;
            }
            validIssues.add(FreeDiagnosisOutput.Issue.builder()
                    .issueId(defaultIfBlank(cleanupAiText(raw.getIssueId()), "I" + index))
                    .title(title)
                    .whatHappened(defaultIfBlank(cleanupAiText(raw.getWhatHappened()), title))
                    .whyItMatters(defaultIfBlank(cleanupAiText(raw.getWhyItMatters()), cleanupAiText(raw.getWhatHappened())))
                    .evidenceRefs(evidenceRefs)
                    .severity(defaultIfBlank(cleanupAiText(raw.getSeverity()), "MAJOR"))
                    .confidence(confidence)
                    .build());
            index++;
        }
        return validIssues;
    }

    private List<FreeDiagnosisOutput.Issue> issuesFromHypotheses(FreeDiagnosisOutput output) {
        if (output == null || output.getHypotheses() == null) {
            return List.of();
        }
        List<FreeDiagnosisOutput.Issue> issues = new ArrayList<>();
        int index = 1;
        for (FreeDiagnosisOutput.Hypothesis hypothesis : output.getHypotheses()) {
            if (hypothesis == null) {
                continue;
            }
            issues.add(FreeDiagnosisOutput.Issue.builder()
                    .issueId("I" + index++)
                    .title(hypothesis.getName())
                    .whatHappened(hypothesis.getReason())
                    .whyItMatters(hypothesis.getReason())
                    .evidenceRefs(hypothesis.getEvidenceRefs())
                    .severity("MAJOR")
                    .confidence(hypothesis.getConfidence())
                    .build());
        }
        return issues;
    }

    private IssueLibraryAnchor attachIssueToStandardLibrary(ExternalModelAgentRuntime.RuntimePlan runtimePlan,
                                                            FreeDiagnosisOutput.Issue issue,
                                                            List<AiStandardLibraryNavigationNodeResponse> roots) {
        List<IssueLibraryAnchor.PathNode> breadcrumb = new ArrayList<>();
        List<AiStandardLibraryNavigationNodeResponse> nodes = roots;
        List<AiStandardLibraryDiagnosticLayerResponse> diagnosticLayers = List.of();
        int maxRounds = navigationMaxRounds();
        for (int round = 1; round <= maxRounds; round++) {
            if (nodes.isEmpty() && diagnosticLayers.isEmpty()) {
                return anchorWithStatus(issue, "NO_MATCH", breadcrumb, "current standard library layer is empty.", null);
            }
            LayeredAttachmentAction action;
            try {
                action = callLayeredAttachmentActionStage(
                        runtimePlan,
                        issue,
                        breadcrumb,
                        nodes,
                        diagnosticLayers,
                        round,
                        maxRounds
                );
            } catch (Exception exception) {
                return anchorWithStatus(issue, "ATTACHMENT_FAILED", breadcrumb, exception.getMessage(), null);
            }
            if (action == null) {
                return anchorWithStatus(issue, "ATTACHMENT_FAILED", breadcrumb, "layered attachment output is empty.", null);
            }
            String actionName = defaultIfBlank(action.getAction(), "").trim().toUpperCase();
            if ("DONE".equals(actionName)) {
                return anchorFromDiagnosticSelection(issue, breadcrumb, diagnosticLayers, action, false);
            }
            if ("NO_MATCH".equals(actionName)) {
                return anchorWithStatus(issue, "NO_MATCH", breadcrumb, action.getReason(), action.getConfidence());
            }
            if (!"SELECT".equals(actionName)) {
                return anchorWithStatus(issue, "ATTACHMENT_FAILED", breadcrumb,
                        "invalid layered attachment action: " + action.getAction(), action.getConfidence());
            }
            List<String> codes = cleanList(action.getCodes(), List.of()).stream()
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .limit(2)
                    .toList();
            if (codes.isEmpty()) {
                return anchorWithStatus(issue, "NO_MATCH", breadcrumb, "SELECT action returned empty codes.",
                        action.getConfidence());
            }
            String code = codes.get(0);
            if (diagnosticCodeVisible(code, diagnosticLayers)) {
                return anchorFromDiagnosticSelection(issue, breadcrumb, diagnosticLayers, action, true);
            }
            AiStandardLibraryNavigationNodeResponse node = findNavigationNode(nodes, code);
            if (node == null) {
                return anchorWithStatus(issue, "ATTACHMENT_FAILED", breadcrumb,
                        "ATTACHMENT_INVALID_CODE:" + code, action.getConfidence());
            }
            breadcrumb.add(pathNode(node));
            try {
                diagnosticLayers = List.of(standardLibraryService.expandDiagnosticLayer(code));
                nodes = List.of();
            } catch (IllegalArgumentException notKnowledgePoint) {
                try {
                    AiStandardLibraryNavigationExpansionResponse expansion =
                            standardLibraryService.expandKnowledgeNode(code, 0, 50);
                    nodes = safeNavigationNodes(expansion == null ? null : expansion.getChildren());
                    diagnosticLayers = List.of();
                } catch (Exception exception) {
                    return anchorWithStatus(issue, "ATTACHMENT_FAILED", breadcrumb, exception.getMessage(),
                            action.getConfidence());
                }
            } catch (Exception exception) {
                return anchorWithStatus(issue, "ATTACHMENT_FAILED", breadcrumb, exception.getMessage(),
                        action.getConfidence());
            }
        }
        if (!breadcrumb.isEmpty()) {
            return anchorWithStatus(issue, "PARTIAL", breadcrumb, "AI_NAVIGATION_ROUND_LIMIT_REACHED", null);
        }
        return anchorWithStatus(issue, "ATTACHMENT_FAILED", breadcrumb, "AI_NAVIGATION_ROUND_LIMIT_REACHED", null);
    }

    private LayeredAttachmentAction callLayeredAttachmentActionStage(
            ExternalModelAgentRuntime.RuntimePlan runtimePlan,
            FreeDiagnosisOutput.Issue issue,
            List<IssueLibraryAnchor.PathNode> breadcrumb,
            List<AiStandardLibraryNavigationNodeResponse> nodes,
            List<AiStandardLibraryDiagnosticLayerResponse> diagnosticLayers,
            int round,
            int maxRounds) throws JsonProcessingException, IOException, InterruptedException {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("task", "为当前 issue 从当前可见标准库目录中选择下一层，或结束挂接。");
        request.put("issue", issue);
        request.put("breadcrumb", breadcrumb);
        request.put("currentLayer", Map.of(
                "round", round,
                "maxRounds", maxRounds,
                "nodes", currentLayerNodes(nodes),
                "diagnosticItems", currentDiagnosticItems(diagnosticLayers)
        ));
        request.put("allowedActions", List.of("SELECT", "DONE", "NO_MATCH"));
        activateRuntimePlan(runtimePlan);
        String systemPrompt = runtimePlan.getStandardLibraryNavigationPrompt().getSystemPrompt();
        String userPrompt = objectMapper.writeValueAsString(request);
        String content = chatCompletion(systemPrompt, userPrompt);
        LayeredAttachmentAction output = parseModelStagePayload(content, LayeredAttachmentAction.class);
        if (output != null) {
            return output;
        }
        return retryStructuredModelStagePayload(systemPrompt, userPrompt, runtimePlan, LayeredAttachmentAction.class);
    }

    private List<Map<String, Object>> currentLayerNodes(List<AiStandardLibraryNavigationNodeResponse> nodes) {
        return safeNavigationNodes(nodes).stream()
                .map(node -> {
                    Map<String, Object> value = new LinkedHashMap<>();
                    value.put("code", node.getCode());
                    value.put("name", node.getName());
                    value.put("type", node.getType());
                    value.put("description", node.getDescription());
                    return value;
                })
                .toList();
    }

    private List<Map<String, Object>> currentDiagnosticItems(
            List<AiStandardLibraryDiagnosticLayerResponse> diagnosticLayers) {
        List<Map<String, Object>> items = new ArrayList<>();
        for (AiStandardLibraryDiagnosticLayerResponse layer : safeDiagnosticLayers(diagnosticLayers)) {
            addDiagnosticItem(items, pathNode(layer.getKnowledgePoint()), "KNOWLEDGE_POINT", null);
            for (AiStandardLibraryDiagnosticLayerResponse.SkillUnit skill : safeList(layer.getSkillUnits())) {
                addDiagnosticItem(items, skill.getCode(), skill.getName(), "SKILL_UNIT", skill.getDescription());
                for (AiStandardLibraryDiagnosticLayerResponse.MistakePoint mistake : safeList(skill.getMistakePoints())) {
                    addDiagnosticItem(items, mistake.getCode(), mistake.getName(), "MISTAKE_POINT",
                            mistake.getDescription());
                }
                for (AiStandardLibraryDiagnosticLayerResponse.ImprovementPoint improvement :
                        safeList(skill.getImprovementPoints())) {
                    addDiagnosticItem(items, improvement.getCode(), improvement.getName(), "IMPROVEMENT_POINT",
                            improvement.getDescription());
                }
            }
            for (AiStandardLibraryDiagnosticLayerResponse.ImprovementPoint improvement :
                    safeList(layer.getDirectImprovementPoints())) {
                addDiagnosticItem(items, improvement.getCode(), improvement.getName(), "IMPROVEMENT_POINT",
                        improvement.getDescription());
            }
        }
        return items;
    }

    private void addDiagnosticItem(List<Map<String, Object>> items,
                                   IssueLibraryAnchor.PathNode node,
                                   String type,
                                   String description) {
        if (node == null) {
            return;
        }
        addDiagnosticItem(items, node.getCode(), node.getName(), type, description);
    }

    private void addDiagnosticItem(List<Map<String, Object>> items,
                                   String code,
                                   String name,
                                   String type,
                                   String description) {
        String normalizedCode = defaultIfBlank(code, "").trim();
        if (normalizedCode.isBlank()) {
            return;
        }
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("code", normalizedCode);
        item.put("name", defaultIfBlank(name, normalizedCode));
        item.put("type", type);
        item.put("description", defaultIfBlank(description, ""));
        items.add(item);
    }

    private IssueLibraryAnchor anchorFromDiagnosticSelection(FreeDiagnosisOutput.Issue issue,
                                                            List<IssueLibraryAnchor.PathNode> breadcrumb,
                                                            List<AiStandardLibraryDiagnosticLayerResponse> layers,
                                                            LayeredAttachmentAction action,
                                                            boolean selectedDiagnosticCode) {
        LinkedHashSet<String> codes = new LinkedHashSet<>(cleanList(action == null ? null : action.getCodes(), List.of()));
        String skillUnitCode = "";
        String mistakePointCode = "";
        String improvementPointCode = "";
        List<IssueLibraryAnchor.PathNode> path = new ArrayList<>(breadcrumb);
        for (AiStandardLibraryDiagnosticLayerResponse layer : safeDiagnosticLayers(layers)) {
            IssueLibraryAnchor.PathNode knowledgePoint = pathNode(layer.getKnowledgePoint());
            if (knowledgePoint != null && path.stream().noneMatch(item -> knowledgePoint.getCode().equals(item.getCode()))) {
                path.add(knowledgePoint);
            }
            for (AiStandardLibraryDiagnosticLayerResponse.SkillUnit skill : safeList(layer.getSkillUnits())) {
                if (codes.contains(skill.getCode())) {
                    skillUnitCode = skill.getCode();
                }
                for (AiStandardLibraryDiagnosticLayerResponse.MistakePoint mistake : safeList(skill.getMistakePoints())) {
                    if (codes.contains(mistake.getCode())) {
                        mistakePointCode = mistake.getCode();
                        skillUnitCode = defaultIfBlank(skillUnitCode, skill.getCode());
                    }
                }
                for (AiStandardLibraryDiagnosticLayerResponse.ImprovementPoint improvement :
                        safeList(skill.getImprovementPoints())) {
                    if (codes.contains(improvement.getCode())) {
                        improvementPointCode = improvement.getCode();
                        skillUnitCode = defaultIfBlank(skillUnitCode, skill.getCode());
                    }
                }
            }
            for (AiStandardLibraryDiagnosticLayerResponse.ImprovementPoint improvement :
                    safeList(layer.getDirectImprovementPoints())) {
                if (codes.contains(improvement.getCode())) {
                    improvementPointCode = improvement.getCode();
                }
            }
        }
        boolean hasFormalAnchor = !skillUnitCode.isBlank() || !mistakePointCode.isBlank() || !improvementPointCode.isBlank();
        return IssueLibraryAnchor.builder()
                .issueId(issue.getIssueId())
                .anchorStatus(hasFormalAnchor || selectedDiagnosticCode ? "HIT" : "PARTIAL")
                .path(path)
                .skillUnitCode(blankToNull(skillUnitCode))
                .mistakePointCode(blankToNull(mistakePointCode))
                .improvementPointCode(blankToNull(improvementPointCode))
                .reason(defaultIfBlank(action == null ? "" : action.getReason(), "standard library attachment completed."))
                .confidence(action == null ? null : action.getConfidence())
                .build();
    }

    private boolean diagnosticCodeVisible(String code, List<AiStandardLibraryDiagnosticLayerResponse> layers) {
        String normalized = defaultIfBlank(code, "").trim();
        if (normalized.isBlank()) {
            return false;
        }
        return currentDiagnosticItems(layers).stream()
                .map(item -> defaultIfBlank((String) item.get("code"), ""))
                .anyMatch(normalized::equals);
    }

    private AiStandardLibraryNavigationNodeResponse findNavigationNode(
            List<AiStandardLibraryNavigationNodeResponse> nodes,
            String code) {
        String normalized = defaultIfBlank(code, "").trim();
        return safeNavigationNodes(nodes).stream()
                .filter(node -> normalized.equals(defaultIfBlank(node.getCode(), "").trim()))
                .findFirst()
                .orElse(null);
    }

    private List<IssueLibraryAnchor> anchorsWithStatus(List<FreeDiagnosisOutput.Issue> issues,
                                                       String status,
                                                       String reason) {
        return safeList(issues).stream()
                .map(issue -> anchorWithStatus(issue, status, List.of(), reason, null))
                .toList();
    }

    private IssueLibraryAnchor anchorWithStatus(FreeDiagnosisOutput.Issue issue,
                                                String status,
                                                List<IssueLibraryAnchor.PathNode> breadcrumb,
                                                String reason,
                                                Double confidence) {
        return IssueLibraryAnchor.builder()
                .issueId(issue == null ? "" : issue.getIssueId())
                .anchorStatus(status)
                .path(breadcrumb == null ? List.of() : List.copyOf(breadcrumb))
                .reason(defaultIfBlank(reason, status))
                .confidence(confidence)
                .build();
    }

    private void setLayeredAttachmentArtifacts(ExternalModelAgentRuntime.RuntimePlan runtimePlan,
                                               List<IssueLibraryAnchor> anchors,
                                               String status,
                                               String failureReason) {
        StandardLibraryNavigationOutput output = navigationOutputFromAnchors(anchors);
        StandardLibraryPack pack = packForNavigationOutput(output, status, failureReason);
        runtimePlan.setIssueLibraryAnchors(anchors);
        runtimePlan.setStandardLibraryNavigationOutput(output);
        runtimePlan.setStandardLibraryPack(pack);
        runtimePlan.setStandardLibraryNavigationResult(StandardLibraryNavigationResult.builder()
                .enabled(true)
                .status(status)
                .failureReason(defaultIfBlank(failureReason, ""))
                .selectedCount(navigationSelectedCount(output))
                .selectedPack(pack)
                .output(output)
                .build());
    }

    private StandardLibraryNavigationOutput navigationOutputFromAnchors(List<IssueLibraryAnchor> anchors) {
        List<StandardLibraryNavigationOutput.SelectedPath> selectedPaths = safeList(anchors).stream()
                .filter(anchor -> List.of("HIT", "PARTIAL").contains(defaultIfBlank(anchor.getAnchorStatus(), "")))
                .map(anchor -> StandardLibraryNavigationOutput.SelectedPath.builder()
                        .knowledgeNodeCode(lastPathCode(anchor.getPath()))
                        .skillUnitCode(anchor.getSkillUnitCode())
                        .mistakePointCode(anchor.getMistakePointCode())
                        .improvementPointCode(anchor.getImprovementPointCode())
                        .libraryFit("HIT".equals(anchor.getAnchorStatus()) ? "HIT" : "PARTIAL")
                        .reason(anchor.getReason())
                        .evidenceRefs(List.of())
                        .confidence(anchor.getConfidence())
                        .build())
                .toList();
        List<StandardLibraryNavigationOutput.UnresolvedGap> gaps = safeList(anchors).stream()
                .filter(anchor -> !List.of("HIT", "PARTIAL").contains(defaultIfBlank(anchor.getAnchorStatus(), "")))
                .map(anchor -> StandardLibraryNavigationOutput.UnresolvedGap.builder()
                        .name(defaultIfBlank(anchor.getIssueId(), "ISSUE"))
                        .reason(anchor.getAnchorStatus() + ":" + defaultIfBlank(anchor.getReason(), ""))
                        .suggestedPath(List.of())
                        .evidenceRefs(List.of())
                        .confidence(anchor.getConfidence())
                        .build())
                .toList();
        return StandardLibraryNavigationOutput.builder()
                .status(selectedPaths.isEmpty() ? "NO_MATCH" : "DONE")
                .selectedBranches(List.of())
                .selectedPaths(selectedPaths)
                .unresolvedGaps(gaps)
                .uncertainty(anchorSummary(anchors))
                .build();
    }

    private StandardLibraryPack packForNavigationOutput(StandardLibraryNavigationOutput output,
                                                        String status,
                                                        String failureReason) {
        StandardLibraryPack pack;
        try {
            pack = standardLibraryNavigationPackBuilder == null
                    ? emptyStandardLibraryPack(status, failureReason, output == null ? "" : output.getUncertainty())
                    : standardLibraryNavigationPackBuilder.build(output);
        } catch (Exception exception) {
            pack = emptyStandardLibraryPack("ATTACHMENT_FAILED", exception.getMessage(),
                    output == null ? "" : output.getUncertainty());
            status = "ATTACHMENT_FAILED";
            failureReason = exception.getMessage();
        }
        if (pack.getStandardLibraryNavigationSummary() == null) {
            pack.setStandardLibraryNavigationSummary(StandardLibraryPack.StandardLibraryNavigationSummary.builder()
                    .build());
        }
        pack.getStandardLibraryNavigationSummary().setStatus(status);
        pack.getStandardLibraryNavigationSummary().setFailureReason(defaultIfBlank(failureReason, ""));
        pack.getStandardLibraryNavigationSummary().setSelectedCount(navigationSelectedCount(output));
        pack.getStandardLibraryNavigationSummary().setUncertainty(output == null ? "" : output.getUncertainty());
        return pack;
    }

    private StandardLibraryPack emptyStandardLibraryPack(String status, String failureReason, String uncertainty) {
        return StandardLibraryPack.builder()
                .schemaVersion(StandardLibraryPack.SCHEMA_VERSION)
                .structureVersion(StandardLibraryPack.STRUCTURE_VERSION)
                .knowledgeGroups(List.of())
                .basicCauses(List.of())
                .improvementPoints(List.of())
                .knowledgeAnchors(List.of())
                .skillUnits(List.of())
                .mistakePoints(List.of())
                .standardLibraryNavigationSummary(StandardLibraryPack.StandardLibraryNavigationSummary.builder()
                        .status(status)
                        .failureReason(defaultIfBlank(failureReason, ""))
                        .selectedCount(0)
                        .uncertainty(defaultIfBlank(uncertainty, ""))
                        .build())
                .issueTags(List.of())
                .fineGrainedTags(List.of())
                .improvementTags(List.of())
                .teachingActions(List.of())
                .build();
    }

    private String aggregateAnchorStatus(List<IssueLibraryAnchor> anchors) {
        if (safeList(anchors).stream().anyMatch(anchor ->
                List.of("HIT", "PARTIAL").contains(defaultIfBlank(anchor.getAnchorStatus(), "")))) {
            return "LAYERED_ATTACHMENT";
        }
        if (!safeList(anchors).isEmpty() && safeList(anchors).stream()
                .allMatch(anchor -> "LIBRARY_EMPTY".equals(anchor.getAnchorStatus()))) {
            return "LIBRARY_EMPTY";
        }
        if (!safeList(anchors).isEmpty() && safeList(anchors).stream()
                .allMatch(anchor -> "NO_MATCH".equals(anchor.getAnchorStatus()))) {
            return "NO_MATCH";
        }
        return "ATTACHMENT_FAILED";
    }

    private String anchorSummary(List<IssueLibraryAnchor> anchors) {
        return safeList(anchors).stream()
                .map(anchor -> defaultIfBlank(anchor.getIssueId(), "ISSUE")
                        + "=" + defaultIfBlank(anchor.getAnchorStatus(), "UNKNOWN"))
                .toList()
                .toString();
    }

    private String lastPathCode(List<IssueLibraryAnchor.PathNode> path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        return path.get(path.size() - 1).getCode();
    }

    private IssueLibraryAnchor.PathNode pathNode(AiStandardLibraryNavigationNodeResponse node) {
        if (node == null || defaultIfBlank(node.getCode(), "").isBlank()) {
            return null;
        }
        return IssueLibraryAnchor.PathNode.builder()
                .code(node.getCode())
                .name(node.getName())
                .type(node.getType())
                .build();
    }

    private String blankToNull(String value) {
        String normalized = defaultIfBlank(value, "").trim();
        return normalized.isBlank() ? null : normalized;
    }

    private FreeDiagnosisOutput callFreeDiagnosisStage(ExternalModelAgentRuntime.RuntimePlan runtimePlan)
            throws JsonProcessingException, IOException, InterruptedException {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("task", "先独立理解题目、完整代码和判题事实，生成不含标准库 ID 的初步诊断。");
        request.put("brief", runtimePlan.getBrief());
        activateRuntimePlan(runtimePlan);
        String systemPrompt = runtimePlan.getFreeDiagnosisPrompt().getSystemPrompt();
        String userPrompt = objectMapper.writeValueAsString(request);
        String content = chatCompletion(systemPrompt, userPrompt);
        FreeDiagnosisOutput output = parseModelStagePayload(content, FreeDiagnosisOutput.class);
        if (output != null) {
            return output;
        }
        return retryStructuredModelStagePayload(systemPrompt, userPrompt, runtimePlan, FreeDiagnosisOutput.class);
    }

    private NavigationLoopResult callStandardLibraryNavigationLoop(
            ExternalModelAgentRuntime.RuntimePlan runtimePlan,
            FreeDiagnosisOutput freeDiagnosis)
            throws JsonProcessingException, IOException, InterruptedException {
        List<AiStandardLibraryNavigationNodeResponse> roots = safeNavigationNodes(standardLibraryService.listRootKnowledgeAreas());
        List<AiStandardLibraryNavigationExpansionResponse> expandedNodes = List.of();
        List<AiStandardLibraryDiagnosticLayerResponse> diagnosticLayers = List.of();
        int maxRounds = navigationMaxRounds();
        StandardLibraryNavigationOutput latest = null;
        for (int round = 1; round <= maxRounds; round++) {
            List<AiStandardLibraryNavigationNodeResponse> visibleRoots = round == 1 ? roots : List.of();
            Map<String, Object> navigationView = navigationView(
                    round,
                    maxRounds,
                    visibleRoots,
                    expandedNodes,
                    diagnosticLayers,
                    null
            );
            latest = callStandardLibraryNavigationStage(runtimePlan, freeDiagnosis, navigationView);
            if (latest == null) {
                return NavigationLoopResult.empty();
            }
            String status = defaultIfBlank(latest.getStatus(), "").trim().toUpperCase();
            if ("DONE".equals(status) || "NO_MATCH".equals(status)) {
                return NavigationLoopResult.success(latest);
            }
            if (!"CONTINUE".equals(status)) {
                return NavigationLoopResult.success(latest);
            }
            if (round >= maxRounds) {
                return NavigationLoopResult.failure(invalidStage("STANDARD_LIBRARY_NAVIGATION",
                        ModelStageFailureReason.INVALID_JSON,
                        "AI_NAVIGATION_ROUND_LIMIT_REACHED"));
            }
            List<StandardLibraryNavigationOutput.SelectedBranch> branches = selectedNavigationBranches(latest);
            if (branches.isEmpty()) {
                return NavigationLoopResult.failure(invalidStage("STANDARD_LIBRARY_NAVIGATION",
                        ModelStageFailureReason.INVALID_JSON,
                        "CONTINUE navigation requires selectedBranches."));
            }
            Set<String> visibleKnowledgeCodes = visibleKnowledgeNodeCodes(visibleRoots, expandedNodes);
            List<String> invalidCodes = invalidSelectedBranchCodes(branches, visibleKnowledgeCodes);
            if (!invalidCodes.isEmpty()) {
                latest = repairInvalidNavigationBranches(
                        runtimePlan,
                        freeDiagnosis,
                        navigationView,
                        latest,
                        invalidCodes);
                if (latest == null) {
                    return NavigationLoopResult.empty();
                }
                status = defaultIfBlank(latest.getStatus(), "").trim().toUpperCase();
                if ("DONE".equals(status) || "NO_MATCH".equals(status)) {
                    return NavigationLoopResult.success(latest);
                }
                if (!"CONTINUE".equals(status)) {
                    return NavigationLoopResult.success(latest);
                }
                branches = selectedNavigationBranches(latest);
                invalidCodes = invalidSelectedBranchCodes(branches, visibleKnowledgeCodes);
                if (branches.isEmpty()) {
                    return NavigationLoopResult.failure(invalidStage("STANDARD_LIBRARY_NAVIGATION",
                            ModelStageFailureReason.INVALID_JSON,
                            "CONTINUE navigation repair requires selectedBranches."));
                }
                if (!invalidCodes.isEmpty()) {
                    return NavigationLoopResult.failure(invalidStage("STANDARD_LIBRARY_NAVIGATION",
                            ModelStageFailureReason.INVALID_TAG,
                            "selected branch knowledgeNodeCode is not visible in current navigation view: "
                                    + String.join(", ", invalidCodes)));
                }
            }
            NavigationExpansionBatch next = expandSelectedNavigationBranches(branches);
            if (next.expandedNodes().isEmpty() && next.diagnosticLayers().isEmpty()) {
                return NavigationLoopResult.failure(invalidStage("STANDARD_LIBRARY_NAVIGATION",
                        ModelStageFailureReason.INVALID_TAG,
                        "selected branches did not expand to knowledge nodes or diagnostic layers."));
            }
            expandedNodes = next.expandedNodes();
            diagnosticLayers = next.diagnosticLayers();
        }
        return NavigationLoopResult.failure(invalidStage("STANDARD_LIBRARY_NAVIGATION",
                ModelStageFailureReason.INVALID_JSON,
                "AI_NAVIGATION_ROUND_LIMIT_REACHED"));
    }

    private StandardLibraryNavigationOutput callStandardLibraryNavigationStage(
            ExternalModelAgentRuntime.RuntimePlan runtimePlan,
            FreeDiagnosisOutput freeDiagnosis,
            Map<String, Object> navigationView)
            throws JsonProcessingException, IOException, InterruptedException {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("task", "按当前导航视图选择要继续展开的标准库分支，或返回最终标准库路径。");
        request.put("brief", runtimePlan.getBrief());
        request.put("freeDiagnosis", freeDiagnosis);
        request.put("navigationView", navigationView);
        activateRuntimePlan(runtimePlan);
        String systemPrompt = runtimePlan.getStandardLibraryNavigationPrompt().getSystemPrompt();
        String userPrompt = objectMapper.writeValueAsString(request);
        String content = chatCompletion(systemPrompt, userPrompt);
        StandardLibraryNavigationOutput output = parseModelStagePayload(content, StandardLibraryNavigationOutput.class);
        if (output != null) {
            return output;
        }
        return retryStructuredModelStagePayload(systemPrompt, userPrompt, runtimePlan,
                StandardLibraryNavigationOutput.class);
    }

    private Map<String, Object> navigationView(
            int round,
            int maxRounds,
            List<AiStandardLibraryNavigationNodeResponse> roots,
            List<AiStandardLibraryNavigationExpansionResponse> expandedNodes,
            List<AiStandardLibraryDiagnosticLayerResponse> diagnosticLayers,
            Map<String, Object> repair) {
        Map<String, Object> navigationView = new LinkedHashMap<>();
        boolean mustFinishNow = round >= maxRounds;
        navigationView.put("round", round);
        navigationView.put("roots", roots);
        navigationView.put("expandedNode", firstOrNull(expandedNodes));
        navigationView.put("diagnosticLayer", firstOrNull(diagnosticLayers));
        navigationView.put("expandedNodes", expandedNodes);
        navigationView.put("diagnosticLayers", diagnosticLayers);
        navigationView.put("visibleKnowledgeNodeCodes", visibleKnowledgeNodeCodes(roots, expandedNodes));
        navigationView.put("visibleDiagnosticCodes", visibleDiagnosticCodes(diagnosticLayers));
        navigationView.put("mustFinishNow", mustFinishNow);
        navigationView.put("navigationInstruction", mustFinishNow
                ? "这是本次允许的最后一轮，必须返回 DONE 或 NO_MATCH，不要返回 CONTINUE。"
                : "如果需要继续展开，只能从 visibleKnowledgeNodeCodes 中选择 selectedBranches；如果已经看到 diagnosticLayers，优先返回 DONE 和 selectedPaths。");
        navigationView.put("maxRounds", maxRounds);
        navigationView.put("maxBranchesPerRound", STANDARD_LIBRARY_NAVIGATION_MAX_BRANCHES);
        navigationView.put("maxFinalAnchors", 12);
        if (repair != null && !repair.isEmpty()) {
            navigationView.put("repair", repair);
        }
        return navigationView;
    }

    private StandardLibraryNavigationOutput repairInvalidNavigationBranches(
            ExternalModelAgentRuntime.RuntimePlan runtimePlan,
            FreeDiagnosisOutput freeDiagnosis,
            Map<String, Object> previousNavigationView,
            StandardLibraryNavigationOutput invalidOutput,
            List<String> invalidCodes)
            throws JsonProcessingException, IOException, InterruptedException {
        Map<String, Object> repair = new LinkedHashMap<>();
        repair.put("reason", "INVALID_BRANCH_CODE");
        repair.put("invalidKnowledgeNodeCodes", invalidCodes);
        repair.put("previousOutput", invalidOutput);
        repair.put("instruction", "上一轮 selectedBranches 引用了当前视图未出现的知识节点 code。请只从 visibleKnowledgeNodeCodes 中重选，或返回 NO_MATCH。");
        Map<String, Object> repairedView = new LinkedHashMap<>(previousNavigationView);
        repairedView.put("repair", repair);
        repairedView.put("navigationInstruction",
                "修正上一轮非法分支：只能从 visibleKnowledgeNodeCodes 中选择 selectedBranches；如果没有合适分支，返回 NO_MATCH。");
        return callStandardLibraryNavigationStage(runtimePlan, freeDiagnosis, repairedView);
    }

    private NavigationExpansionBatch expandSelectedNavigationBranches(
            List<StandardLibraryNavigationOutput.SelectedBranch> branches) {
        List<AiStandardLibraryNavigationExpansionResponse> expandedNodes = new ArrayList<>();
        List<AiStandardLibraryDiagnosticLayerResponse> diagnosticLayers = new ArrayList<>();
        for (StandardLibraryNavigationOutput.SelectedBranch branch : branches) {
            String code = defaultIfBlank(branch.getKnowledgeNodeCode(), "").trim();
            if (code.isBlank()) {
                continue;
            }
            try {
                diagnosticLayers.add(standardLibraryService.expandDiagnosticLayer(code));
            } catch (IllegalArgumentException exception) {
                try {
                    expandedNodes.add(standardLibraryService.expandKnowledgeNode(code, 0, 50));
                } catch (IllegalArgumentException nested) {
                    throw new IllegalArgumentException("selected branch cannot be expanded: " + code, nested);
                }
            }
        }
        return new NavigationExpansionBatch(expandedNodes, diagnosticLayers);
    }

    private List<StandardLibraryNavigationOutput.SelectedBranch> selectedNavigationBranches(
            StandardLibraryNavigationOutput output) {
        if (output == null || output.getSelectedBranches() == null) {
            return List.of();
        }
        LinkedHashMap<String, StandardLibraryNavigationOutput.SelectedBranch> branches = new LinkedHashMap<>();
        for (StandardLibraryNavigationOutput.SelectedBranch branch : output.getSelectedBranches()) {
            if (branch == null || defaultIfBlank(branch.getKnowledgeNodeCode(), "").isBlank()) {
                continue;
            }
            String code = branch.getKnowledgeNodeCode().trim();
            branches.putIfAbsent(code, branch);
            if (branches.size() >= STANDARD_LIBRARY_NAVIGATION_MAX_BRANCHES) {
                break;
            }
        }
        return branches.values().stream().toList();
    }

    private List<String> invalidSelectedBranchCodes(
            List<StandardLibraryNavigationOutput.SelectedBranch> branches,
            Set<String> visibleKnowledgeCodes) {
        if (branches == null || branches.isEmpty()) {
            return List.of();
        }
        Set<String> visible = visibleKnowledgeCodes == null ? Set.of() : visibleKnowledgeCodes;
        return branches.stream()
                .map(StandardLibraryNavigationOutput.SelectedBranch::getKnowledgeNodeCode)
                .map(code -> defaultIfBlank(code, "").trim())
                .filter(code -> !code.isBlank())
                .filter(code -> !visible.contains(code))
                .distinct()
                .toList();
    }

    private Set<String> visibleKnowledgeNodeCodes(
            List<AiStandardLibraryNavigationNodeResponse> roots,
            List<AiStandardLibraryNavigationExpansionResponse> expandedNodes) {
        LinkedHashSet<String> codes = new LinkedHashSet<>();
        safeNavigationNodes(roots).forEach(node -> addNavigationNodeCode(codes, node));
        safeNavigationExpansions(expandedNodes).forEach(expansion ->
                safeNavigationNodes(expansion.getChildren()).forEach(node -> addNavigationNodeCode(codes, node)));
        return codes;
    }

    private Map<String, Object> visibleDiagnosticCodes(
            List<AiStandardLibraryDiagnosticLayerResponse> diagnosticLayers) {
        LinkedHashSet<String> knowledgeNodeCodes = new LinkedHashSet<>();
        LinkedHashSet<String> skillUnitCodes = new LinkedHashSet<>();
        LinkedHashSet<String> mistakePointCodes = new LinkedHashSet<>();
        LinkedHashSet<String> improvementPointCodes = new LinkedHashSet<>();
        for (AiStandardLibraryDiagnosticLayerResponse layer : safeDiagnosticLayers(diagnosticLayers)) {
            addNavigationNodeCode(knowledgeNodeCodes, layer.getKnowledgePoint());
            for (AiStandardLibraryDiagnosticLayerResponse.SkillUnit skill : safeList(layer.getSkillUnits())) {
                addIfNotBlank(skillUnitCodes, skill.getCode());
                for (AiStandardLibraryDiagnosticLayerResponse.MistakePoint mistake : safeList(skill.getMistakePoints())) {
                    addIfNotBlank(mistakePointCodes, mistake.getCode());
                }
                for (AiStandardLibraryDiagnosticLayerResponse.ImprovementPoint improvement : safeList(skill.getImprovementPoints())) {
                    addIfNotBlank(improvementPointCodes, improvement.getCode());
                }
            }
            for (AiStandardLibraryDiagnosticLayerResponse.ImprovementPoint improvement :
                    safeList(layer.getDirectImprovementPoints())) {
                addIfNotBlank(improvementPointCodes, improvement.getCode());
            }
        }
        Map<String, Object> codes = new LinkedHashMap<>();
        codes.put("knowledgeNodeCodes", knowledgeNodeCodes.stream().toList());
        codes.put("skillUnitCodes", skillUnitCodes.stream().toList());
        codes.put("mistakePointCodes", mistakePointCodes.stream().toList());
        codes.put("improvementPointCodes", improvementPointCodes.stream().toList());
        return codes;
    }

    private int navigationMaxRounds() {
        return Math.max(1, Math.min(Math.max(standardLibraryNavigationMaxRounds, 0), 10));
    }

    private int navigationMaxIssues() {
        return Math.max(1, Math.min(Math.max(standardLibraryNavigationMaxIssues, 0), 5));
    }

    private <T> T firstOrNull(List<T> values) {
        return values == null || values.isEmpty() ? null : values.get(0);
    }

    private List<AiStandardLibraryNavigationNodeResponse> safeNavigationNodes(
            List<AiStandardLibraryNavigationNodeResponse> values) {
        return values == null ? List.of() : values;
    }

    private List<AiStandardLibraryNavigationExpansionResponse> safeNavigationExpansions(
            List<AiStandardLibraryNavigationExpansionResponse> values) {
        return values == null ? List.of() : values;
    }

    private List<AiStandardLibraryDiagnosticLayerResponse> safeDiagnosticLayers(
            List<AiStandardLibraryDiagnosticLayerResponse> values) {
        return values == null ? List.of() : values;
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private void addNavigationNodeCode(Set<String> codes, AiStandardLibraryNavigationNodeResponse node) {
        if (node != null) {
            addIfNotBlank(codes, node.getCode());
        }
    }

    private void addIfNotBlank(Set<String> values, String value) {
        String normalized = defaultIfBlank(value, "").trim();
        if (!normalized.isBlank()) {
            values.add(normalized);
        }
    }

    private StandardLibraryNavigationOutput.SelectedBranch firstNavigationBranch(
            StandardLibraryNavigationOutput output) {
        if (output == null || output.getSelectedBranches() == null) {
            return null;
        }
        return output.getSelectedBranches().stream()
                .filter(item -> item != null && !defaultIfBlank(item.getKnowledgeNodeCode(), "").isBlank())
                .findFirst()
                .orElse(null);
    }

    private int navigationSelectedCount(StandardLibraryNavigationOutput output) {
        if (output == null) {
            return 0;
        }
        return size(output.getSelectedBranches()) + size(output.getSelectedPaths()) + size(output.getUnresolvedGaps());
    }

    private ExternalModelStagePayloads.StageValidationResult invalidStage(String stage,
                                                                          ModelStageFailureReason reason,
                                                                          String message) {
        return ExternalModelStagePayloads.StageValidationResult.builder()
                .valid(false)
                .stage(stage)
                .failureReason(reason)
                .message(message)
                .softFixes(List.of())
                .hardFailures(List.of(message))
                .build();
    }

    private AdviceGenerationOutput retryAdviceGenerationForSafety(
            ExternalModelAgentRuntime.RuntimePlan runtimePlan,
            AdviceGenerationOutput unsafeOutput,
            ExternalModelStagePayloads.StageValidationResult failure)
            throws JsonProcessingException, IOException, InterruptedException {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("task", "重写上一轮学生可见反馈，保留正确诊断，但去掉过度直接的答案式表达。");
        request.put("contextPolicy", List.of(
                "可以保留有效 evidenceRefs 和仍然匹配的标准库 id。",
                "只把直接答案改写成观察动作、手推方法或检查问题。",
                "不要给完整代码、替换表达式、逐行改法或隐藏测试推测。"
        ));
        request.put("brief", runtimePlan.getBrief());
        request.put("standardLibrary", runtimePlan.getStandardLibraryPack());
        request.put("standardLibraryNavigationSummary", runtimePlan.getStandardLibraryPack() == null
                ? null
                : runtimePlan.getStandardLibraryPack().getStandardLibraryNavigationSummary());
        request.put("previousOutput", unsafeOutput);
        request.put("validationFailure", Map.of(
                "stage", failure == null ? "DIAGNOSIS_AND_ADVICE" : defaultIfBlank(failure.getStage(), "DIAGNOSIS_AND_ADVICE"),
                "reason", failure == null || failure.getFailureReason() == null
                        ? ModelStageFailureReason.SAFETY_RISK.name()
                        : failure.getFailureReason().name(),
                "message", failure == null ? "" : cleanupAiText(failure.getMessage())
        ));
        String systemPrompt = runtimePlan.getAdvicePrompt().getSystemPrompt()
                + "\n\n安全重写重试:\n"
                + "上一轮 JSON 被拒绝，因为学生可见字段太接近直接答案。\n"
                + "请返回同一个输出 schema，但把不安全的学生可见文本改写为诊断式提示。\n"
                + "保留仍然有效的 evidenceRefs 和标准库 id。\n"
                + "不要使用直接改成、替换为、最终答案、完整代码、精确可执行表达式等表达。\n"
                + "边界问题只能引导学生手推取值、比较目标区间、检查端点是否进入循环。\n"
                + "DP 或状态设计问题只能提示学生重述状态含义、核对转移来源、手推可见失败样例；不要写前驱状态、具体下标组合、dp[i-1]、skip_current、take_current、两状态、多一维、空间压缩。\n"
                + "不要写出替换表达式或精确循环头。\n";
        activateRuntimePlan(runtimePlan);
        String content = chatCompletion(systemPrompt, objectMapper.writeValueAsString(request));
        lastCallTelemetry.set(lastCallTelemetry.get().withFallbackRetryUsed(true));
        AdviceGenerationOutput output = parseModelStagePayload(content, AdviceGenerationOutput.class);
        if (output != null) {
            return output;
        }
        return retryStructuredModelStagePayload(
                systemPrompt,
                objectMapper.writeValueAsString(request),
                runtimePlan,
                AdviceGenerationOutput.class
        );
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

    private int size(List<?> values) {
        return values == null ? 0 : values.size();
    }

    private boolean shouldRetryStructuredPayload(Class<?> payloadType,
                                                 ExternalModelCallTelemetry telemetry,
                                                 String rawContent) {
        if (payloadType != AdviceGenerationOutput.class) {
            return false;
        }
        if (!structuredRetryEnabled) {
            return false;
        }
        String finishReason = telemetry == null ? "" : defaultIfBlank(telemetry.streamFinishReason(), "");
        String cleaned = cleanupAiText(rawContent);
        if ("length".equalsIgnoreCase(finishReason)) {
            return true;
        }
        return cleaned.contains("\"caseUnderstanding\"")
                || cleaned.contains("\"basicLayerAdvice\"")
                || cleaned.contains("\"nextStepPlan\"");
    }

    private String adviceFailureReason(ExternalModelStagePayloads.StageValidationResult failure) {
        if (failure == null) {
            return "UNKNOWN_ERROR";
        }
        String reason = failure.getFailureReason() == null
                ? "UNKNOWN_ERROR"
                : failure.getFailureReason().name();
        String message = cleanupAiText(failure.getMessage());
        return message.isBlank() ? reason : reason + ":" + message;
    }

    private SubmissionAnalysisResponse.CaseUnderstanding toResponseCaseUnderstanding(
            AdviceGenerationOutput.CaseUnderstanding source) {
        if (source == null) {
            return null;
        }
        return SubmissionAnalysisResponse.CaseUnderstanding.builder()
                .problemGoal(cleanupAiText(source.getProblemGoal()))
                .codeIntent(cleanupAiText(source.getCodeIntent()))
                .behaviorGap(cleanupAiText(source.getBehaviorGap()))
                .primaryEvidenceRef(cleanupAiText(source.getPrimaryEvidenceRef()))
                .build();
    }

    private List<SubmissionAnalysisResponse.BasicLayerAdvice> toResponseBasicLayerAdvice(
            List<AdviceGenerationOutput.BasicLayerAdvice> source) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        return source.stream()
                .filter(item -> item != null)
                .map(item -> SubmissionAnalysisResponse.BasicLayerAdvice.builder()
                        .mistakePointId(cleanupAiText(item.getMistakePointId()))
                        .skillUnitId(cleanupAiText(item.getSkillUnitId()))
                        .title(cleanupAiText(item.getTitle()))
                        .whatHappened(cleanupAiText(item.getWhatHappened()))
                        .whyItMatters(cleanupAiText(item.getWhyItMatters()))
                        .studentAction(cleanupAiText(item.getStudentAction()))
                        .checkQuestion(cleanupAiText(item.getCheckQuestion()))
                        .evidenceRefs(cleanList(item.getEvidenceRefs(), List.of()))
                        .confidence(item.getConfidence())
                        .build())
                .toList();
    }

    private List<SubmissionAnalysisResponse.ImprovementLayerAdvice> toResponseImprovementLayerAdvice(
            List<AdviceGenerationOutput.ImprovementLayerAdvice> source) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        return source.stream()
                .filter(item -> item != null)
                .map(item -> SubmissionAnalysisResponse.ImprovementLayerAdvice.builder()
                        .improvementPointId(cleanupAiText(item.getImprovementPointId()))
                        .skillUnitId(cleanupAiText(item.getSkillUnitId()))
                        .title(cleanupAiText(item.getTitle()))
                        .currentLimit(cleanupAiText(item.getCurrentLimit()))
                        .suggestion(cleanupAiText(item.getSuggestion()))
                        .studentBenefit(cleanupAiText(item.getStudentBenefit()))
                        .evidenceRefs(cleanList(item.getEvidenceRefs(), List.of()))
                        .confidence(item.getConfidence())
                        .build())
                .toList();
    }

    private String buildAdviceReportMarkdown(ExternalModelAgentRuntime.RuntimePlan runtimePlan,
                                             AdviceGenerationOutput output) {
        if (output == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        builder.append("## AI 完整诊断与建议\n\n");
        if (output.getStudentReport() != null) {
            AdviceGenerationOutput.StudentReport report = output.getStudentReport();
            appendStudentReportSection(builder, "基础层", report.getBasicLayerText());
            appendStudentReportSection(builder, "提高层", report.getImprovementLayerText());
            appendStudentReportSection(builder, "下一步行动", report.getNextActionText());
        }
        if (output.getCaseUnderstanding() != null) {
            AdviceGenerationOutput.CaseUnderstanding understanding = output.getCaseUnderstanding();
            builder.append("### 题目与代码理解\n\n");
            appendMarkdownLine(builder, "题目目标", understanding.getProblemGoal());
            appendMarkdownLine(builder, "代码意图", understanding.getCodeIntent());
            appendMarkdownLine(builder, "行为差距", understanding.getBehaviorGap());
            appendMarkdownLine(builder, "主要证据", understanding.getPrimaryEvidenceRef());
            builder.append('\n');
        }
        if (output.getBasicLayerAdvice() != null && !output.getBasicLayerAdvice().isEmpty()) {
            builder.append(output.getStudentReport() == null ? "### 基础层\n\n" : "### 基础层明细\n\n");
            int index = 1;
            for (AdviceGenerationOutput.BasicLayerAdvice item : output.getBasicLayerAdvice()) {
                if (item == null) {
                    continue;
                }
                builder.append(index++).append(". ")
                        .append(defaultIfBlank(item.getTitle(), "基础层问题"))
                        .append('\n');
                appendMarkdownLine(builder, "发生了什么", item.getWhatHappened());
                appendMarkdownLine(builder, "为什么重要", item.getWhyItMatters());
                appendMarkdownLine(builder, "下一步", item.getStudentAction());
                appendMarkdownLine(builder, "自查问题", item.getCheckQuestion());
                if (item.getEvidenceRefs() != null && !item.getEvidenceRefs().isEmpty()) {
                    appendMarkdownLine(builder, "证据", String.join(", ", item.getEvidenceRefs()));
                }
                builder.append('\n');
            }
        }
        if (output.getImprovementLayerAdvice() != null && !output.getImprovementLayerAdvice().isEmpty()) {
            builder.append(output.getStudentReport() == null ? "### 提高层\n\n" : "### 提高层明细\n\n");
            int index = 1;
            for (AdviceGenerationOutput.ImprovementLayerAdvice item : output.getImprovementLayerAdvice()) {
                if (item == null) {
                    continue;
                }
                builder.append(index++).append(". ")
                        .append(defaultIfBlank(item.getTitle(), "提高层建议"))
                        .append('\n');
                appendMarkdownLine(builder, "当前限制", item.getCurrentLimit());
                appendMarkdownLine(builder, "建议", item.getSuggestion());
                appendMarkdownLine(builder, "收益", item.getStudentBenefit());
                if (item.getEvidenceRefs() != null && !item.getEvidenceRefs().isEmpty()) {
                    appendMarkdownLine(builder, "证据", String.join(", ", item.getEvidenceRefs()));
                }
                builder.append('\n');
            }
        }
        if (runtimePlan != null && runtimePlan.getStandardLibraryPack() != null
                && runtimePlan.getStandardLibraryPack().getStandardLibraryNavigationSummary() != null) {
            StandardLibraryPack.StandardLibraryNavigationSummary summary =
                    runtimePlan.getStandardLibraryPack().getStandardLibraryNavigationSummary();
            builder.append("### 定位说明\n\n");
            appendMarkdownLine(builder, "定位状态", summary.getStatus());
            appendMarkdownLine(builder, "选中数量", summary.getSelectedCount() == null ? "" : summary.getSelectedCount().toString());
        }
        String markdown = builder.toString().trim();
        return markdown;
    }

    private void appendStudentReportSection(StringBuilder builder, String title, String value) {
        String cleaned = cleanupAiText(value);
        if (!cleaned.isBlank()) {
            builder.append("### ").append(title).append("\n\n")
                    .append(cleaned)
                    .append("\n\n");
        }
    }

    private void appendMarkdownLine(StringBuilder builder, String label, String value) {
        String cleaned = cleanupAiText(value);
        if (!cleaned.isBlank()) {
            builder.append("- ").append(label).append("：").append(cleaned).append('\n');
        }
    }

    private SubmissionAnalysisResponse buildAdviceRuntimeAnalysisResponse(
            SubmissionAnalysisResponse baseline,
            ExternalModelAgentRuntime.RuntimePlan runtimePlan,
            AdviceGenerationOutput adviceOutput,
            SubmissionAnalysisResponse.StudentFeedback studentFeedback,
            String rawSourceCode,
            List<SubmissionAnalysisResponse.LineIssue> baselineLineIssues) {
        List<String> evidenceRefs = adviceEvidenceRefs(adviceOutput);
        SubmissionAnalysisResponse.NextLearningAction nextAction =
                studentFeedback == null ? null : studentFeedback.getNextLearningAction();
        return SubmissionAnalysisResponse.builder()
                .submissionId(baseline.getSubmissionId())
                .analysisSchemaVersion(baseline.getAnalysisSchemaVersion())
                .evidenceSchemaVersion(baseline.getEvidenceSchemaVersion())
                .taxonomyVersion(baseline.getTaxonomyVersion())
                .sourceType(AI_SOURCE)
                .scenario(baseline.getScenario())
                .headline("AI 完整诊断已完成")
                .summary(defaultIfBlank(adviceOutput == null ? "" : adviceOutput.getStudentSummary(), "AI 已完成本次诊断。"))
                .issueTags(List.of())
                .fineGrainedTags(List.of())
                .abilityPoints(List.of())
                .focusPoints(adviceFocusPoints(adviceOutput))
                .fixDirections(adviceFixDirections(adviceOutput))
                .evidenceRefs(evidenceRefs)
                .studentHint(defaultIfBlank(nextAction == null ? "" : nextAction.getTask(),
                        firstAdviceAction(adviceOutput, "先复核模型指出的证据。")))
                .studentHintPlan(buildAdviceHintPlan(adviceOutput, studentFeedback))
                .studentFeedback(studentFeedback)
                .learningInterventionPlan(buildAdviceInterventionPlan(studentFeedback))
                .learningActionEvidence(null)
                .teacherNote(adviceTeacherNote(adviceOutput))
                .progressSignal("")
                .confidence(resolveConfidence(adviceConfidence(adviceOutput), null))
                .uncertainty(defaultIfBlank(runtimePlan == null || runtimePlan.getBrief() == null
                        ? ""
                        : runtimePlan.getBrief().getUncertainty(), ""))
                .diagnosticTrace("")
                .modelEducationTrace(adviceModelEducationTrace(adviceOutput))
                .caseUnderstanding(toResponseCaseUnderstanding(adviceOutput == null ? null : adviceOutput.getCaseUnderstanding()))
                .basicLayerAdvice(toResponseBasicLayerAdvice(adviceOutput == null ? null : adviceOutput.getBasicLayerAdvice()))
                .improvementLayerAdvice(toResponseImprovementLayerAdvice(adviceOutput == null ? null : adviceOutput.getImprovementLayerAdvice()))
                .aiInvocation(modelInvocation(baseline, "MODEL_COMPLETED", runtimePromptVersion(runtimePlan), runtimePlan, null))
                .answerLeakRisk(resolveAnswerLeakRisk(nextAction == null ? "" : nextAction.getAnswerLeakRisk(),
                        "LOW"))
                .wrongSolution(null)
                .correctSolution(null)
                .lineIssues(List.of())
                .firstFailedCase(baseline.getFirstFailedCase())
                .reportMarkdown(buildAdviceReportMarkdown(runtimePlan, adviceOutput))
                .generatedAt(baseline.getGeneratedAt())
                .build();
    }

    private List<String> adviceEvidenceRefs(AdviceGenerationOutput output) {
        List<String> refs = new ArrayList<>();
        if (output != null && output.getCaseUnderstanding() != null) {
            addIfNotBlank(refs, output.getCaseUnderstanding().getPrimaryEvidenceRef());
        }
        if (output != null && output.getBasicLayerAdvice() != null) {
            output.getBasicLayerAdvice().stream()
                    .filter(item -> item != null && item.getEvidenceRefs() != null)
                    .flatMap(item -> item.getEvidenceRefs().stream())
                    .forEach(ref -> addIfNotBlank(refs, ref));
        }
        if (output != null && output.getImprovementLayerAdvice() != null) {
            output.getImprovementLayerAdvice().stream()
                    .filter(item -> item != null && item.getEvidenceRefs() != null)
                    .flatMap(item -> item.getEvidenceRefs().stream())
                    .forEach(ref -> addIfNotBlank(refs, ref));
        }
        return refs.stream().distinct().toList();
    }

    private List<String> adviceFocusPoints(AdviceGenerationOutput output) {
        if (output == null || output.getBasicLayerAdvice() == null) {
            return List.of();
        }
        return output.getBasicLayerAdvice().stream()
                .filter(item -> item != null)
                .map(AdviceGenerationOutput.BasicLayerAdvice::getTitle)
                .map(this::cleanupAiText)
                .filter(value -> !value.isBlank())
                .distinct()
                .limit(5)
                .toList();
    }

    private List<String> adviceFixDirections(AdviceGenerationOutput output) {
        List<String> directions = new ArrayList<>();
        if (output != null && output.getNextStepPlan() != null) {
            output.getNextStepPlan().stream()
                    .filter(item -> item != null)
                    .map(AdviceGenerationOutput.NextStepAdvice::getTarget)
                    .forEach(value -> addIfNotBlank(directions, value));
        }
        if (directions.isEmpty() && output != null && output.getBasicLayerAdvice() != null) {
            output.getBasicLayerAdvice().stream()
                    .filter(item -> item != null)
                    .map(AdviceGenerationOutput.BasicLayerAdvice::getStudentAction)
                    .forEach(value -> addIfNotBlank(directions, value));
        }
        return directions.stream().distinct().limit(5).toList();
    }

    private String firstAdviceAction(AdviceGenerationOutput output, String defaultAction) {
        if (output != null && output.getNextStepPlan() != null) {
            String target = output.getNextStepPlan().stream()
                    .filter(item -> item != null)
                    .map(AdviceGenerationOutput.NextStepAdvice::getTarget)
                    .map(this::cleanupAiText)
                    .filter(value -> !value.isBlank())
                    .findFirst()
                    .orElse("");
            if (!target.isBlank()) {
                return target;
            }
        }
        if (output != null && output.getBasicLayerAdvice() != null) {
            String action = output.getBasicLayerAdvice().stream()
                    .filter(item -> item != null)
                    .map(AdviceGenerationOutput.BasicLayerAdvice::getStudentAction)
                    .map(this::cleanupAiText)
                    .filter(value -> !value.isBlank())
                    .findFirst()
                    .orElse("");
            if (!action.isBlank()) {
                return action;
            }
        }
        return defaultAction;
    }

    private SubmissionAnalysisResponse.StudentHintPlan buildAdviceHintPlan(
            AdviceGenerationOutput output,
            SubmissionAnalysisResponse.StudentFeedback studentFeedback) {
        SubmissionAnalysisResponse.NextLearningAction nextAction =
                studentFeedback == null ? null : studentFeedback.getNextLearningAction();
        if (nextAction == null) {
            return null;
        }
        List<String> evidenceRefs = cleanList(nextAction.getEvidenceRefs(), adviceEvidenceRefs(output));
        return SubmissionAnalysisResponse.StudentHintPlan.builder()
                .hintLevel(defaultIfBlank(nextAction.getHintLevel(), "L2"))
                .problemType("AI 完整诊断")
                .evidenceAnchor(evidenceRefs.isEmpty() ? "" : evidenceRefs.get(0))
                .nextAction(defaultIfBlank(nextAction.getTask(), firstAdviceAction(output, "")))
                .coachQuestion(defaultIfBlank(nextAction.getCheckQuestion(), firstAdviceQuestion(output)))
                .teachingAction(defaultIfBlank(nextAction.getAction(), "COLLECT_EVIDENCE"))
                .evidenceRefs(evidenceRefs)
                .answerLeakRisk(resolveAnswerLeakRisk(nextAction.getAnswerLeakRisk(), "LOW"))
                .build();
    }

    private SubmissionAnalysisResponse.LearningInterventionPlan buildAdviceInterventionPlan(
            SubmissionAnalysisResponse.StudentFeedback studentFeedback) {
        SubmissionAnalysisResponse.NextLearningAction nextAction =
                studentFeedback == null ? null : studentFeedback.getNextLearningAction();
        if (nextAction == null) {
            return null;
        }
        return SubmissionAnalysisResponse.LearningInterventionPlan.builder()
                .interventionType("ADVICE_GENERATION")
                .goal("完成 AI 建议中的第一个可观察动作。")
                .studentTask(nextAction.getTask())
                .checkQuestion(nextAction.getCheckQuestion())
                .completionSignal("学生能用证据说明当前问题和下一步验证动作。")
                .evidenceRefs(cleanList(nextAction.getEvidenceRefs(), List.of()))
                .estimatedMinutes(6)
                .answerLeakRisk(resolveAnswerLeakRisk(nextAction.getAnswerLeakRisk(), "LOW"))
                .build();
    }

    private Double adviceConfidence(AdviceGenerationOutput output) {
        if (output == null || output.getBasicLayerAdvice() == null || output.getBasicLayerAdvice().isEmpty()) {
            return null;
        }
        return output.getBasicLayerAdvice().stream()
                .filter(item -> item != null && item.getConfidence() != null)
                .map(AdviceGenerationOutput.BasicLayerAdvice::getConfidence)
                .max(Double::compareTo)
                .orElse(null);
    }

    private String adviceTeacherNote(AdviceGenerationOutput output) {
        String reasoning = output == null || output.getTeacherTrace() == null
                ? ""
                : cleanupAiText(output.getTeacherTrace().getReasoningSummary());
        if (!reasoning.isBlank()) {
            return reasoning;
        }
        return "外部模型已生成基础层与提高层结构化建议。";
    }

    private SubmissionAnalysisResponse.ModelEducationTrace adviceModelEducationTrace(AdviceGenerationOutput output) {
        if (output == null) {
            return null;
        }
        AdviceGenerationOutput.BasicLayerAdvice firstBasic = firstBasicAdvice(output);
        return SubmissionAnalysisResponse.ModelEducationTrace.builder()
                .source("adviceGeneration")
                .primaryIssueTag(firstBasic == null ? "" : cleanupAiText(firstBasic.getSkillUnitId()).toUpperCase())
                .fineGrainedTag(firstBasic == null ? "" : cleanupAiText(firstBasic.getMistakePointId()).toUpperCase())
                .evidenceRefs(adviceEvidenceRefs(output))
                .primaryReasoning(firstBasic == null ? "" : cleanupAiText(firstBasic.getWhatHappened()))
                .secondaryIssues(List.of())
                .distractorNotes(List.of())
                .teachingPriority(cleanupAiText(output.getStudentSummary()))
                .improvementCategories(adviceImprovementCategories(output))
                .nextLearningAction(firstAdviceAction(output, ""))
                .nextLearningActionEvidenceRefs(firstStepEvidenceRefs(output))
                .confidence(adviceConfidence(output))
                .uncertainty("")
                .needsMoreEvidence(false)
                .answerLeakRisk("LOW")
                .build();
    }

    private List<String> adviceImprovementCategories(AdviceGenerationOutput output) {
        if (output == null || output.getImprovementLayerAdvice() == null) {
            return List.of();
        }
        return output.getImprovementLayerAdvice().stream()
                .filter(item -> item != null)
                .map(AdviceGenerationOutput.ImprovementLayerAdvice::getImprovementPointId)
                .map(this::cleanupAiText)
                .filter(value -> !value.isBlank())
                .map(value -> value.toUpperCase(java.util.Locale.ROOT))
                .distinct()
                .toList();
    }

    private AdviceGenerationOutput.BasicLayerAdvice firstBasicAdvice(AdviceGenerationOutput output) {
        if (output == null || output.getBasicLayerAdvice() == null) {
            return null;
        }
        return output.getBasicLayerAdvice().stream()
                .filter(item -> item != null)
                .findFirst()
                .orElse(null);
    }

    private String firstAdviceQuestion(AdviceGenerationOutput output) {
        AdviceGenerationOutput.BasicLayerAdvice first = firstBasicAdvice(output);
        return first == null ? "这条证据说明了什么代码行为？" : first.getCheckQuestion();
    }

    private List<String> firstStepEvidenceRefs(AdviceGenerationOutput output) {
        if (output == null || output.getNextStepPlan() == null) {
            return List.of();
        }
        return output.getNextStepPlan().stream()
                .filter(item -> item != null && item.getEvidenceRef() != null && !item.getEvidenceRef().isBlank())
                .map(AdviceGenerationOutput.NextStepAdvice::getEvidenceRef)
                .distinct()
                .toList();
    }

    private void addIfNotBlank(List<String> values, String candidate) {
        String cleaned = cleanupAiText(candidate);
        if (!cleaned.isBlank()) {
            values.add(cleaned);
        }
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

    private String runtimePromptVersion(ExternalModelAgentRuntime.RuntimePlan runtimePlan) {
        if (runtimePlan != null && runtimePlan.getAdvicePrompt() != null
                && !cleanupAiText(runtimePlan.getAdvicePrompt().getVersion()).isBlank()) {
            return cleanupAiText(runtimePlan.getAdvicePrompt().getVersion());
        }
        return PromptTemplateRegistry.DIAGNOSIS_REPORT_V3;
    }

    private String failureSummary(ExternalModelStagePayloads.StageValidationResult failure) {
        if (failure == null) {
            return "（失败阶段：UNKNOWN_STAGE；失败原因：UNKNOWN_ERROR）";
        }
        String stage = failure.getStage() == null || failure.getStage().isBlank() ? "UNKNOWN_STAGE" : failure.getStage();
        String reason = failure.getFailureReason() == null ? "UNKNOWN_ERROR" : failure.getFailureReason().name();
        return "（失败阶段：" + stage + "；失败原因：" + reason + "）";
    }

    private SubmissionAnalysisResponse runtimeFailure(SubmissionAnalysisResponse baseline,
                                                      ExternalModelStagePayloads.StageValidationResult validationResult) {
        return runtimeFailure(baseline, null, validationResult);
    }

    private SubmissionAnalysisResponse runtimeFailure(SubmissionAnalysisResponse baseline,
                                                      ExternalModelAgentRuntime.RuntimePlan runtimePlan,
                                                      ExternalModelStagePayloads.StageValidationResult validationResult) {
        if (baseline == null) {
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
        String uncertainty = "外部模型未完成，本次未使用本地规则兜底。失败阶段：" + stage + "；失败原因：" + reason
                + (message.isBlank() ? "" : "，" + message);
        return SubmissionAnalysisResponse.builder()
                .submissionId(baseline.getSubmissionId())
                .analysisSchemaVersion(baseline.getAnalysisSchemaVersion())
                .evidenceSchemaVersion(baseline.getEvidenceSchemaVersion())
                .taxonomyVersion(baseline.getTaxonomyVersion())
                .sourceType(AI_SOURCE)
                .scenario(baseline.getScenario())
                .headline("AI 诊断暂不可用")
                .summary("本次没有生成 AI 诊断，请稍后重试或先查看原始评测结果。")
                .issueTags(List.of())
                .fineGrainedTags(List.of())
                .abilityPoints(List.of())
                .focusPoints(List.of())
                .fixDirections(List.of())
                .evidenceRefs(List.of())
                .studentHint("AI 诊断未生成，请稍后重试。")
                .studentHintPlan(null)
                .studentFeedback(null)
                .learningInterventionPlan(null)
                .learningActionEvidence(null)
                .teacherNote(uncertainty)
                .progressSignal("")
                .confidence(null)
                .uncertainty(uncertainty)
                .diagnosticTrace("model=failed stage=" + stage + " reason=" + reason)
                .modelEducationTrace(null)
                .caseUnderstanding(null)
                .basicLayerAdvice(List.of())
                .improvementLayerAdvice(List.of())
                .aiInvocation(modelInvocation(
                        baseline,
                        "MODEL_FAILED",
                        runtimePromptVersion(runtimePlan),
                        runtimePlan,
                        attributionResult
                ))
                .answerLeakRisk(defaultIfBlank(baseline.getAnswerLeakRisk(), "LOW"))
                .wrongSolution(null)
                .correctSolution(null)
                .lineIssues(List.of())
                .firstFailedCase(baseline.getFirstFailedCase())
                .reportMarkdown("")
                .build();
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
        return stageFailure(stage, reason, exception == null ? "" : exception.getMessage());
    }

    private ExternalModelStagePayloads.StageValidationResult aiUnavailableFailure(String stage) {
        return stageFailure(stage, ModelStageFailureReason.UNKNOWN_ERROR, "AI access is unavailable.");
    }

    private ExternalModelStagePayloads.StageValidationResult stageFailure(String stage,
                                                                         ModelStageFailureReason reason,
                                                                         String message) {
        return ExternalModelStagePayloads.StageValidationResult.builder()
                .valid(false)
                .stage(stage)
                .failureReason(reason)
                .message(message == null ? "" : message)
                .build();
    }

    public String enhanceGrowthReportMarkdown(Problem problem,
                                              List<Map<String, Object>> submissionTimeline) {
        if (!canCallAi()) {
            log.info("AI growth report unavailable because AI access is unavailable. problemId={}", problem.getId());
            return growthReportUnavailableMarkdown(problem, aiUnavailableFailure("GROWTH_REPORT"));
        }
        ExternalModelBudgetGuard.Decision decision = budgetGuard.check(PROVIDER, model);
        if (!decision.allowed()) {
            return growthReportUnavailableMarkdown(problem, ExternalModelStagePayloads.StageValidationResult.builder()
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
            return markdown.isBlank()
                    ? growthReportUnavailableMarkdown(problem, stageFailure("GROWTH_REPORT", ModelStageFailureReason.INVALID_JSON,
                    "AI growth report response is blank."))
                    : markdown;
        } catch (Exception exception) {
            log.error("AI growth report generation failed. problemId={}", problem.getId(), exception);
            return growthReportUnavailableMarkdown(problem, stageFailureFromException("GROWTH_REPORT", exception));
        }
    }

    private String growthReportUnavailableMarkdown(Problem problem,
                                                   ExternalModelStagePayloads.StageValidationResult failure) {
        String title = problem == null || problem.getTitle() == null || problem.getTitle().isBlank()
                ? "AI 成长报告"
                : "AI 成长报告 - " + cleanupAiText(problem.getTitle());
        return "# " + title + " 暂不可用\n\n"
                + "外部模型未完成，本次未使用本地报告兜底。请在 AI smoke 通过或模型额度恢复后重新生成。\n\n"
                + "<!-- AI_FAILURE:" + failureSummary(failure) + " -->";
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

    public static AutoCloseable captureModelCallTrace(Consumer<ModelCallTraceEvent> sink) {
        MODEL_CALL_TRACE_SINK.set(sink);
        return MODEL_CALL_TRACE_SINK::remove;
    }

    public record ModelCallTraceEvent(
            String stage,
            boolean stream,
            int outputTokens,
            String runtimeProfile,
            boolean requestCompact,
            String systemPrompt,
            String userPrompt,
            String requestBody,
            String responseBody,
            String content,
            String error,
            long latencyMs
    ) {
    }

    public String smokeChatCompletion() throws IOException, InterruptedException {
        if (!canCallAi()) {
            throw new IOException("AI is disabled or API key is blank");
        }
        return chatCompletionWithOverrides(
                "You are a production readiness smoke test. Reply with exactly OK.",
                "Return OK.",
                streamEnabled,
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
        throttleStudentFeedbackRequest();
        return chatCompletionWithOverrides(systemPrompt, userPrompt, streamEnabled, outputTokens);
    }

    private void throttleStudentFeedbackRequest() throws InterruptedException {
        long intervalMs = Math.max(0, studentFeedbackMinRequestIntervalMs);
        if (intervalMs <= 0) {
            return;
        }
        synchronized (studentFeedbackThrottle) {
            long now = System.currentTimeMillis();
            long waitMs = lastStudentFeedbackRequestAtMs + intervalMs - now;
            if (waitMs > 0) {
                Thread.sleep(waitMs);
                now = System.currentTimeMillis();
            }
            lastStudentFeedbackRequestAtMs = now;
        }
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
            if (!stream && streamFallbackEnabled && shouldRetryWithStreaming(exception)) {
                log.warn("Retrying AI chat completion with stream=true after non-stream response was unusable. model={}", model);
                String content = doChatCompletionWithRetry(systemPrompt, userPrompt, true, requestContext, outputTokens);
                lastCallTelemetry.set(lastCallTelemetry.get().withFallbackRetryUsed(true));
                budgetGuard.recordSuccess(PROVIDER, model);
                return content;
            }
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
        Map<String, Object> requestBody = chatRequestFactory.build(
                baseUrl,
                modelScopeCompatibleRequest,
                model,
                systemPrompt,
                userPrompt,
                stream,
                outputTokens
        );
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

        long startedAt = System.nanoTime();
        String responseBody = "";
        String content = "";
        try {
            responseBody = sendChatCompletionRequest(serializedRequest, stream);
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
            recordModelCallTrace(systemPrompt, userPrompt, serializedRequest, responseBody, content, "",
                    stream, outputTokens, activeProfile, activeCompact, startedAt);
            return content;
        } catch (IOException | InterruptedException | RuntimeException exception) {
            recordModelCallTrace(systemPrompt, userPrompt, serializedRequest, responseBody, content,
                    exception.getMessage(), stream, outputTokens, activeProfile, activeCompact, startedAt);
            throw exception;
        }
    }

    private void recordModelCallTrace(String systemPrompt,
                                      String userPrompt,
                                      String requestBody,
                                      String responseBody,
                                      String content,
                                      String error,
                                      boolean stream,
                                      int outputTokens,
                                      String runtimeProfile,
                                      boolean requestCompact,
                                      long startedAt) {
        Consumer<ModelCallTraceEvent> sink = MODEL_CALL_TRACE_SINK.get();
        if (sink == null) {
            return;
        }
        long latencyMs = (System.nanoTime() - startedAt) / 1_000_000;
        sink.accept(new ModelCallTraceEvent(
                traceStage(systemPrompt),
                stream,
                outputTokens,
                runtimeProfile,
                requestCompact,
                systemPrompt,
                userPrompt,
                requestBody,
                responseBody,
                content,
                defaultIfBlank(error, ""),
                latencyMs
        ));
    }

    private String traceStage(String systemPrompt) {
        String text = defaultIfBlank(systemPrompt, "");
        if (text.contains("free-diagnosis-v1")) {
            return "FREE_DIAGNOSIS";
        }
        if (text.contains("standard-library-navigation-v1")) {
            return "LAYERED_ATTACHMENT";
        }
        if (text.contains("diagnosis-report-v3")) {
            return "ADVICE_GENERATION";
        }
        if (text.contains("student-fast-feedback")) {
            return "STUDENT_FAST_FEEDBACK";
        }
        return "MODEL_CALL";
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

    private <T> T parseModelStagePayload(String rawContent, Class<T> payloadType) {
        String normalized = cleanupAiText(rawContent);
        lastModelStageRawContent.set(normalized);
        try {
            return objectMapper.readValue(normalized, payloadType);
        } catch (JsonProcessingException firstError) {
            T repairedOutput = parseRepairedModelStagePayload(normalized, payloadType);
            if (repairedOutput != null) {
                return repairedOutput;
            }
            int start = normalized.indexOf('{');
            int end = normalized.lastIndexOf('}');
            if (start >= 0 && end > start) {
                String objectPayload = normalized.substring(start, end + 1);
                try {
                    return objectMapper.readValue(objectPayload, payloadType);
                } catch (JsonProcessingException ignored) {
                    repairedOutput = parseRepairedModelStagePayload(objectPayload, payloadType);
                    if (repairedOutput != null) {
                        return repairedOutput;
                    }
                    log.warn("AI payload parsing failed. type={}, error={}, contentPreview={}",
                            payloadType.getSimpleName(),
                            ignored.getMessage(),
                            previewBody(normalized));
                }
            }
            return null;
        }
    }

    private <T> T parseRepairedModelStagePayload(String rawJson, Class<T> payloadType) {
        if (payloadType != AdviceGenerationOutput.class || rawJson == null || rawJson.isBlank()) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            if (!(root instanceof ObjectNode objectNode)) {
                return null;
            }
            boolean changed = repairAdviceGenerationObjectNode(objectNode);
            if (!changed) {
                return null;
            }
            return objectMapper.treeToValue(objectNode, payloadType);
        } catch (JsonProcessingException exception) {
            log.warn("AI advice payload repair failed. error={}, contentPreview={}",
                    exception.getMessage(),
                    previewBody(rawJson));
            return null;
        }
    }

    private boolean repairAdviceGenerationObjectNode(ObjectNode root) {
        boolean changed = false;
        changed |= repairAdviceStudentReport(root);
        changed |= repairAdviceDiagnosisDecision(root);
        changed |= repairAdviceDiagnosisCandidates(root);
        changed |= repairAdviceLibraryGrowth(root);
        return changed;
    }

    private boolean repairAdviceStudentReport(ObjectNode root) {
        JsonNode studentReport = root.get("studentReport");
        if (studentReport == null || !studentReport.isTextual()) {
            return false;
        }
        String reportText = cleanupAiText(studentReport.asText());
        if (reportText.isBlank()) {
            return false;
        }
        ObjectNode repaired = objectMapper.createObjectNode();
        repaired.put("hintLevel", "L3");
        repaired.put("basicLayerText", stripInlineNextAction(reportText));
        repaired.put("improvementLayerText", "");
        repaired.put("nextActionText", extractInlineNextAction(reportText));
        root.set("studentReport", repaired);
        if (!root.hasNonNull("studentSummary")) {
            root.put("studentSummary", stripInlineNextAction(reportText));
        }
        return true;
    }

    private boolean repairAdviceDiagnosisDecision(ObjectNode root) {
        JsonNode decisionNode = root.get("diagnosisDecision");
        if (!(decisionNode instanceof ObjectNode decision)) {
            return false;
        }
        boolean changed = false;
        if (!decision.hasNonNull("libraryFit") && decision.hasNonNull("status")) {
            decision.set("libraryFit", decision.get("status"));
            changed = true;
        }
        if (!decision.has("anchors") && (decision.has("id") || decision.has("evidenceRefs") || decision.has("status"))) {
            ArrayNode anchors = objectMapper.createArrayNode();
            ObjectNode anchor = objectMapper.createObjectNode();
            if (decision.hasNonNull("id") && !decision.get("id").isNull()) {
                anchor.set("id", decision.get("id"));
            }
            String fit = decision.hasNonNull("libraryFit") ? decision.get("libraryFit").asText("") : "";
            anchor.put("type", "OUT_OF_LIBRARY".equalsIgnoreCase(fit) ? "OUT_OF_LIBRARY" : "KNOWLEDGE_NODE");
            anchor.put("role", "PRIMARY");
            if (decision.hasNonNull("confidence")) {
                anchor.set("confidence", decision.get("confidence"));
            } else {
                anchor.put("confidence", 0.75);
            }
            if (decision.has("evidenceRefs")) {
                anchor.set("evidenceRefs", ensureTextArray(decision.get("evidenceRefs")));
            }
            anchor.put("reason", decision.path("reason").asText("模型给出的诊断决策。"));
            anchors.add(anchor);
            decision.set("anchors", anchors);
            changed = true;
        }
        if (decision.has("libraryPath") && decision.get("libraryPath").isTextual()) {
            ArrayNode path = objectMapper.createArrayNode();
            path.add(decision.get("libraryPath").asText());
            decision.set("libraryPath", path);
            changed = true;
        }
        if (decision.has("id") || decision.has("status") || decision.has("evidenceRefs")
                || decision.has("confidence") || decision.has("reason") || decision.has("libraryPath")) {
            decision.remove(List.of("id", "status", "evidenceRefs", "confidence", "reason", "libraryPath"));
            changed = true;
        }
        return changed;
    }

    private boolean repairAdviceDiagnosisCandidates(ObjectNode root) {
        JsonNode candidatesNode = root.get("diagnosisCandidates");
        if (candidatesNode == null || !candidatesNode.isArray()) {
            return false;
        }
        boolean changed = false;
        for (JsonNode node : candidatesNode) {
            if (!(node instanceof ObjectNode candidate)) {
                continue;
            }
            if (!candidate.hasNonNull("libraryFit") && candidate.hasNonNull("status")) {
                candidate.set("libraryFit", candidate.get("status"));
                changed = true;
            }
            if (!candidate.hasNonNull("anchorId") && candidate.hasNonNull("id")) {
                candidate.set("anchorId", candidate.get("id"));
                changed = true;
            }
            if (!candidate.hasNonNull("anchorType")) {
                String fit = candidate.path("libraryFit").asText("");
                candidate.put("anchorType", "OUT_OF_LIBRARY".equalsIgnoreCase(fit) ? "OUT_OF_LIBRARY" : "KNOWLEDGE_NODE");
                changed = true;
            }
            if (candidate.has("libraryPath") && candidate.get("libraryPath").isTextual()) {
                ArrayNode path = objectMapper.createArrayNode();
                path.add(candidate.get("libraryPath").asText());
                candidate.set("libraryPath", path);
                changed = true;
            }
            if (candidate.has("id") || candidate.has("status")) {
                candidate.remove(List.of("id", "status"));
                changed = true;
            }
        }
        return changed;
    }

    private boolean repairAdviceLibraryGrowth(ObjectNode root) {
        JsonNode growthNode = root.get("libraryGrowth");
        if (!(growthNode instanceof ObjectNode growth)) {
            return false;
        }
        JsonNode candidatesNode = growth.get("candidates");
        if (candidatesNode == null || !candidatesNode.isArray()) {
            return false;
        }
        boolean changed = false;
        for (JsonNode node : candidatesNode) {
            if (!(node instanceof ObjectNode candidate)) {
                continue;
            }
            if (candidate.has("suggestedPath") && candidate.get("suggestedPath").isTextual()) {
                ArrayNode path = objectMapper.createArrayNode();
                path.add(candidate.get("suggestedPath").asText());
                candidate.set("suggestedPath", path);
                changed = true;
            }
            if (!candidate.hasNonNull("status")) {
                candidate.put("status", "NEEDS_REVIEW");
                changed = true;
            }
        }
        return changed;
    }

    private ArrayNode ensureTextArray(JsonNode node) {
        ArrayNode array = objectMapper.createArrayNode();
        if (node == null || node.isNull()) {
            return array;
        }
        if (node.isArray()) {
            node.forEach(array::add);
            return array;
        }
        if (node.isTextual() && !node.asText().isBlank()) {
            array.add(node.asText());
        }
        return array;
    }

    private String stripInlineNextAction(String text) {
        String normalized = cleanupAiText(text);
        int index = inlineNextActionIndex(normalized);
        if (index < 0) {
            return normalized;
        }
        return normalized.substring(0, index).trim();
    }

    private String extractInlineNextAction(String text) {
        String normalized = cleanupAiText(text);
        int index = inlineNextActionIndex(normalized);
        if (index < 0) {
            return "先复核模型指出的证据。";
        }
        String action = normalized.substring(index)
                .replaceFirst("^下一步(?:动作|建议|行动)?[：:]?", "")
                .trim();
        return action.isBlank() ? "先复核模型指出的证据。" : action;
    }

    private int inlineNextActionIndex(String text) {
        String normalized = cleanupAiText(text);
        int best = -1;
        for (String marker : List.of("下一步动作", "下一步建议", "下一步行动", "下一步")) {
            int index = normalized.indexOf(marker);
            if (index >= 0 && (best < 0 || index < best)) {
                best = index;
            }
        }
        return best;
    }

    private String previewBody(String body) {
        String normalized = body == null ? "" : body.replace("\r", "").replace("\n", "\\n").trim();
        if (normalized.length() <= 320) {
            return normalized;
        }
        return normalized.substring(0, 317) + "...";
    }

    private List<String> cleanList(List<String> candidate, List<String> defaultValues) {
        List<String> source = candidate == null || candidate.isEmpty() ? defaultValues : candidate;
        if (source == null || source.isEmpty()) {
            return List.of();
        }
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
        return sourceCode == null ? "" : sourceCode;
    }

    private String truncateText(String value, int maxLength) {
        String text = cleanupAiText(value);
        int limit = Math.max(0, maxLength);
        if (limit == 0 || text.length() <= limit) {
            return text;
        }
        return text.substring(0, limit).trim() + "...";
    }

    private String sourceLine(String sourceCode, int targetLine) {
        if (sourceCode == null || targetLine <= 0) {
            return "";
        }
        String[] lines = sourceCode.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        if (targetLine > lines.length) {
            return "";
        }
        return cleanupAiText(lines[targetLine - 1]).trim();
    }

    private Integer lastReferencedLineNumber(String text) {
        Matcher matcher = RUNTIME_LINE_PATTERN.matcher(defaultIfBlank(text, ""));
        Integer line = null;
        while (matcher.find()) {
            try {
                line = Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException ignored) {
                // Ignore malformed traceback fragments.
            }
        }
        return line;
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
            if (index > 0) {
                builder.append('\n');
            }
            builder.append(numberedLine);
        }
        return builder.toString();
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

    private String defaultIfBlank(String candidate, String defaultValue) {
        String normalized = cleanupAiText(candidate);
        if (normalized.isBlank()) {
            return defaultValue;
        }
        return normalized;
    }

    private Double resolveConfidence(Double candidate, Double defaultValue) {
        if (candidate == null || candidate.isNaN() || candidate < 0 || candidate > 1) {
            return defaultValue;
        }
        return candidate;
    }

    private String resolveAnswerLeakRisk(String candidate, String defaultRisk) {
        String normalized = cleanupAiText(candidate).toUpperCase();
        if ("LOW".equals(normalized) || "MEDIUM".equals(normalized) || "HIGH".equals(normalized)) {
            return normalized;
        }
        return defaultRisk == null || defaultRisk.isBlank() ? "UNKNOWN" : defaultRisk;
    }

    private SubmissionAnalysisResponse.AiInvocation modelInvocation(SubmissionAnalysisResponse baseline,
                                                                    String status,
                                                                    String promptVersion,
                                                                    ExternalModelAgentRuntime.RuntimePlan runtimePlan,
                                                                    ExternalModelStagePayloads.StageValidationResult failure) {
        ExternalModelCallTelemetry telemetry = lastCallTelemetry.get();
        int requestBytes = telemetry.requestBytes() == null ? 0 : telemetry.requestBytes();
        return SubmissionAnalysisResponse.AiInvocation.builder()
                .provider(PROVIDER)
                .model(model)
                .modelVersion(model)
                .promptVersion(promptVersion)
                .agentVersion(baseline == null || baseline.getAiInvocation() == null
                        ? null
                        : baseline.getAiInvocation().getAgentVersion())
                .analysisSchemaVersion(baseline == null ? "diagnosis-v1" : baseline.getAnalysisSchemaVersion())
                .evidenceSchemaVersion(baseline == null ? DiagnosisEvidencePackage.SCHEMA_VERSION : baseline.getEvidenceSchemaVersion())
                .taxonomyVersion(baseline == null ? null : baseline.getTaxonomyVersion())
                .status(status)
                .fallbackUsed(false)
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
                .standardLibraryNavigationEnabled(standardLibraryNavigation(runtimePlan).enabled())
                .standardLibraryNavigationStatus(standardLibraryNavigation(runtimePlan).status())
                .standardLibraryNavigationSelectedCount(standardLibraryNavigation(runtimePlan).selectedCount())
                .standardLibraryNavigationFailureReason(standardLibraryNavigation(runtimePlan).failureReason())
                .adviceGenerationStatus(adviceGeneration(runtimePlan).status())
                .adviceGenerationFailureReason(adviceGeneration(runtimePlan).failureReason())
                .basicAdviceCount(adviceGeneration(runtimePlan).basicAdviceCount())
                .improvementAdviceCount(adviceGeneration(runtimePlan).improvementAdviceCount())
                .advicePromptVersion(adviceGeneration(runtimePlan).promptVersion())
                .diagnosisPromptVersion(promptVersion)
                .studentReportLength(studentReportLength(runtimePlan))
                .answerLeakRisk(answerLeakRisk(runtimePlan))
                .libraryFit(diagnosisLibraryFit(runtimePlan))
                .diagnosisLibraryFit(diagnosisLibraryFit(runtimePlan))
                .diagnosisSoftFixes(diagnosisSoftFixes(runtimePlan))
                .diagnosisHardFailures(diagnosisHardFailures(runtimePlan))
                .build();
    }

    private Integer studentReportLength(ExternalModelAgentRuntime.RuntimePlan runtimePlan) {
        AdviceGenerationOutput.StudentReport report = studentReport(runtimePlan);
        if (report == null) {
            return 0;
        }
        return cleanupAiText(report.getBasicLayerText()).length()
                + cleanupAiText(report.getImprovementLayerText()).length()
                + cleanupAiText(report.getNextActionText()).length();
    }

    private String answerLeakRisk(ExternalModelAgentRuntime.RuntimePlan runtimePlan) {
        AdviceGenerationOutput.StudentReport report = studentReport(runtimePlan);
        if (report == null) {
            return "UNKNOWN";
        }
        return ModelOutputSafetyPolicy.containsUnsafeLeak(
                report.getBasicLayerText(),
                report.getImprovementLayerText(),
                report.getNextActionText()
        ) ? "HIGH" : "LOW";
    }

    private AdviceGenerationOutput.StudentReport studentReport(ExternalModelAgentRuntime.RuntimePlan runtimePlan) {
        AdviceGenerationOutput output = adviceGeneration(runtimePlan).output();
        return output == null ? null : output.getStudentReport();
    }

    private String diagnosisLibraryFit(ExternalModelAgentRuntime.RuntimePlan runtimePlan) {
        AdviceGenerationOutput output = adviceGeneration(runtimePlan).output();
        return output == null || output.getDiagnosisDecision() == null
                ? ""
                : cleanupAiText(output.getDiagnosisDecision().getLibraryFit());
    }

    private List<String> diagnosisSoftFixes(ExternalModelAgentRuntime.RuntimePlan runtimePlan) {
        AdviceGenerationOutput output = adviceGeneration(runtimePlan).output();
        return output == null || output.getTeacherTrace() == null || output.getTeacherTrace().getSoftFixes() == null
                ? List.of()
                : output.getTeacherTrace().getSoftFixes();
    }

    private List<String> diagnosisHardFailures(ExternalModelAgentRuntime.RuntimePlan runtimePlan) {
        AdviceGenerationOutput output = adviceGeneration(runtimePlan).output();
        return output == null || output.getTeacherTrace() == null || output.getTeacherTrace().getHardFailures() == null
                ? List.of()
                : output.getTeacherTrace().getHardFailures();
    }

    private StandardLibraryNavigationResult standardLibraryNavigation(ExternalModelAgentRuntime.RuntimePlan runtimePlan) {
        if (runtimePlan == null || runtimePlan.getStandardLibraryNavigationResult() == null) {
            return StandardLibraryNavigationResult.disabled();
        }
        return runtimePlan.getStandardLibraryNavigationResult();
    }

    private AdviceGenerationResult adviceGeneration(ExternalModelAgentRuntime.RuntimePlan runtimePlan) {
        if (runtimePlan == null || runtimePlan.getAdviceGenerationResult() == null) {
            return AdviceGenerationResult.disabled();
        }
        return runtimePlan.getAdviceGenerationResult();
    }

    private String runtimeModeForPrompt(String promptVersion) {
        String version = cleanupAiText(promptVersion);
        if (STUDENT_FAST_FEEDBACK_PROMPT_VERSION.equals(version)) {
            return "student-fast-feedback";
        }
        if (PromptTemplateRegistry.DIAGNOSIS_AND_ADVICE_V1.equals(version)) {
            return "advice-generation";
        }
        if (PromptTemplateRegistry.DIAGNOSIS_REPORT_V2.equals(version)
                || PromptTemplateRegistry.DIAGNOSIS_REPORT_V3.equals(version)) {
            return "diagnosis-report";
        }
        return "external-model";
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

    private Map<String, Object> compactStudentFastFeedbackContext(Problem problem,
                                                                  Submission submission,
                                                                  DiagnosisEvidencePackage evidencePackage,
                                                                  ExternalModelAgentRuntime.RuntimePlan runtimePlan) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("problem", compactStudentProblemContext(problem, evidencePackage));
        context.put("submission", compactStudentSubmissionContext(submission, evidencePackage));
        context.put("judgeFacts", compactStudentJudgeFacts(evidencePackage == null ? null : evidencePackage.getJudgeFacts()));
        if (runtimePlan != null && runtimePlan.getStandardLibraryPack() != null) {
            context.put("standardLibrary", runtimePlan.getStandardLibraryPack());
            context.put("standardLibraryNavigationSummary", runtimePlan.getStandardLibraryPack().getStandardLibraryNavigationSummary());
        }
        context.put("safetyRules", List.of(
                "只给定位和验证动作，不给完整代码或完整答案。",
                "不猜隐藏测试数据。",
                "每条建议必须自己从 submission.sourceCodeWithLineNumbers 定位 code:line 或 code:range；学生可见文字不能复述内部字段名或证据标记。"
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
        context.put("sourceCodeWithLineNumbers", numberedSource);
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

    private StudentAiFeedbackResponse normalizeStudentFastFeedback(StudentFastFeedbackPayload payload,
                                                                   Submission submission,
                                                                   long startedAt,
                                                                   Map<String, String> evidenceCandidateRefs) {
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
                    .studentReport(null)
                    .nextQuestion(null)
                    .safety(StudentAiFeedbackResponse.Safety.builder()
                            .answerLeakRisk("HIGH")
                    .blockedReasons(mergeStringLists(safety.getBlockedReasons(), List.of("ANSWER_LEAK_RISK")))
                            .build())
                    .evidenceRefs(resolveStudentEvidenceRefs(payload.evidenceRefs, evidenceCandidateRefs))
                    .build();
        }
        List<StudentAiFeedbackResponse.FeedbackItem> repairItems =
                enrichFeedbackItems(normalizeFeedbackItems(payload.repairItems, evidenceCandidateRefs), submission);
        List<StudentAiFeedbackResponse.FeedbackItem> improvementItems =
                enrichFeedbackItems(normalizeFeedbackItems(payload.improvementItems, evidenceCandidateRefs), submission);
        String nextQuestion = cleanStudentFeedbackText(payload.nextQuestion);
        StudentAiFeedbackResponse.StudentReport studentReport =
                normalizeStudentReport(payload.studentReport, repairItems, improvementItems, nextQuestion);
        if (studentReportLeaksAnswer(studentReport)) {
            return StudentAiFeedbackResponse.builder()
                    .submissionId(submission == null ? null : submission.getId())
                    .status("SAFETY_REJECTED")
                    .source("MODEL")
                    .latencyMs(elapsedMs(startedAt))
                    .repairItems(List.of())
                    .improvementItems(List.of())
                    .studentReport(null)
                    .nextQuestion(null)
                    .safety(StudentAiFeedbackResponse.Safety.builder()
                            .answerLeakRisk("HIGH")
                            .blockedReasons(mergeStringLists(safety.getBlockedReasons(), List.of("ANSWER_LEAK_RISK")))
                            .build())
                    .evidenceRefs(cleanList(payload.evidenceRefs, List.of()))
                    .build();
        }
        List<String> evidenceRefs = mergeStringLists(
                resolveStudentEvidenceRefs(payload.evidenceRefs, evidenceCandidateRefs),
                mergeItemRefs(repairItems, improvementItems)
        );
        if (!hasStudentReportText(studentReport) && repairItems.isEmpty() && improvementItems.isEmpty() && nextQuestion.isBlank()) {
            return feedbackFailure(submission, "FAILED", "EMPTY_MODEL_FEEDBACK", startedAt);
        }
        StudentAiFeedbackResponse response = StudentAiFeedbackResponse.builder()
                .submissionId(submission == null ? null : submission.getId())
                .status("READY")
                .source("MODEL")
                .latencyMs(elapsedMs(startedAt))
                .repairItems(repairItems)
                .improvementItems(improvementItems)
                .studentReport(studentReport)
                .nextQuestion(nextQuestion.isBlank() ? null : nextQuestion)
                .safety(safety)
                .evidenceRefs(evidenceRefs)
                .build();
        return enforceRuntimeEvidenceGrounding(response, submission);
    }

    private StudentAiFeedbackResponse enforceRuntimeEvidenceGrounding(StudentAiFeedbackResponse response,
                                                                      Submission submission) {
        if (response == null || submission == null || !"READY".equals(response.getStatus())) {
            return response;
        }
        String errorMessage = defaultIfBlank(submission.getErrorMessage(), "");
        if (submission.getVerdict() != Submission.Verdict.RUNTIME_ERROR
                || !errorMessage.toLowerCase().contains("indexerror")
                || !errorMessage.toLowerCase().contains("list index out of range")) {
            return response;
        }
        Integer lineNumber = lastReferencedLineNumber(errorMessage);
        String lineText = lineNumber == null
                ? ""
                : sourceLine(submission.getSourceCode(), lineNumber);
        String evidenceRef = lineNumber == null ? "verdict:runtime_error" : "code:line:" + lineNumber;
        if (mentionsIndexRuntimeCause(response)) {
            return alignIndexRuntimeImprovement(response, evidenceRef);
        }
        if (lineNumber == null && lineText.isBlank()) {
            return response;
        }
        String linePhrase = lineText.isBlank()
                ? "traceback 指向的列表访问位置"
                : "第 " + lineNumber + " 行 `" + truncateText(lineText, 70) + "`";
        StudentAiFeedbackResponse.StudentReport groundedReport = StudentAiFeedbackResponse.StudentReport.builder()
                .basicLayerText("这次运行错误的主因不是代码长本身，而是 " + linePhrase
                        + " 发生了列表下标越界。IndexError: list index out of range 表示某次访问时，下标已经不在数组的合法范围内。先手推数组长度、循环最后一次下标取值和题目要求处理的个数是否一致。")
                .improvementLayerText("修复越界后，再补测最小规模、普通样例和边界样例。这样能更早发现循环次数、数组长度、左右端点没有对齐的问题。")
                .nextActionText("手推循环最后一次下标是否仍小于数组长度。")
                .build();
        StudentAiFeedbackResponse.FeedbackItem repairItem = indexRuntimeRepairItem(evidenceRef, submission);
        StudentAiFeedbackResponse.FeedbackItem improvementItem = indexRuntimeImprovementItem(evidenceRef, submission);
        return StudentAiFeedbackResponse.builder()
                .submissionId(response.getSubmissionId())
                .status(response.getStatus())
                .source(response.getSource())
                .generatedAt(response.getGeneratedAt())
                .latencyMs(response.getLatencyMs())
                .repairItems(mergeRuntimeRepairItems(repairItem, response.getRepairItems()))
                .improvementItems(mergeRuntimeImprovementItems(improvementItem, response.getImprovementItems()))
                .studentReport(groundedReport)
                .nextQuestion(response.getNextQuestion())
                .safety(response.getSafety())
                .evidenceRefs(mergeStringLists(response.getEvidenceRefs(), List.of(evidenceRef, "verdict:runtime_error")))
                .build();
    }

    private StudentAiFeedbackResponse alignIndexRuntimeImprovement(StudentAiFeedbackResponse response,
                                                                   String evidenceRef) {
        StudentAiFeedbackResponse.StudentReport originalReport = response.getStudentReport();
        String basicLayerText = originalReport == null
                ? null
                : removeCodeCleanupDriftSentence(originalReport.getBasicLayerText());
        boolean improvementDrifts = indexImprovementDriftsToCodeCleanup(response);
        boolean basicChanged = originalReport != null && !defaultIfBlank(originalReport.getBasicLayerText(), "")
                .equals(defaultIfBlank(basicLayerText, ""));
        if (!improvementDrifts && !basicChanged) {
            return response;
        }
        StudentAiFeedbackResponse.StudentReport alignedReport = StudentAiFeedbackResponse.StudentReport.builder()
                .basicLayerText(basicLayerText)
                .improvementLayerText(improvementDrifts
                        ? "修复越界后，重点补强边界样例意识：用最小规模、普通样例和最大边界附近数据，专门检查循环次数、数组长度和左右端点是否对齐。"
                        : originalReport == null ? null : originalReport.getImprovementLayerText())
                .nextActionText(originalReport == null ? null : originalReport.getNextActionText())
                .build();
        List<StudentAiFeedbackResponse.FeedbackItem> improvementItems = improvementDrifts
                ? mergeRuntimeImprovementItems(indexRuntimeImprovementItem(evidenceRef, response), response.getImprovementItems())
                : response.getImprovementItems();
        return StudentAiFeedbackResponse.builder()
                .submissionId(response.getSubmissionId())
                .status(response.getStatus())
                .source(response.getSource())
                .generatedAt(response.getGeneratedAt())
                .latencyMs(response.getLatencyMs())
                .repairItems(response.getRepairItems())
                .improvementItems(improvementItems)
                .studentReport(alignedReport)
                .nextQuestion(response.getNextQuestion())
                .safety(response.getSafety())
                .evidenceRefs(mergeStringLists(response.getEvidenceRefs(), List.of(evidenceRef)))
                .build();
    }

    private StudentAiFeedbackResponse.FeedbackItem indexRuntimeRepairItem(String evidenceRef, Submission submission) {
        return StudentAiFeedbackResponse.FeedbackItem.builder()
                .title("列表下标越界")
                .body("traceback 已经指到一次数组访问越界；先核对循环次数和数组长度的关系。")
                .kind("REPAIR")
                .knowledgePath(List.of("程序基础", "数组/列表", "下标访问", "越界检查"))
                .evidenceSnippets(evidenceSnippets(List.of(evidenceRef), submission))
                .evidenceRefs(List.of(evidenceRef, "verdict:runtime_error"))
                .qualitySignals(List.of("evidence_grounded", "actionable", "no_answer_leak"))
                .build();
    }

    private StudentAiFeedbackResponse.FeedbackItem indexRuntimeImprovementItem(String evidenceRef, Submission submission) {
        return StudentAiFeedbackResponse.FeedbackItem.builder()
                .title("边界样例意识")
                .body("修复后用最小 n、普通样例和边界样例复测，观察是否仍有多访问或少访问。")
                .kind("IMPROVEMENT")
                .knowledgePath(List.of("调试能力", "测试设计", "边界样例", "错误复现"))
                .evidenceSnippets(evidenceSnippets(List.of(evidenceRef), submission))
                .evidenceRefs(List.of(evidenceRef))
                .qualitySignals(List.of("transfer"))
                .build();
    }

    private StudentAiFeedbackResponse.FeedbackItem indexRuntimeImprovementItem(String evidenceRef,
                                                                               StudentAiFeedbackResponse response) {
        List<StudentAiFeedbackResponse.EvidenceSnippet> snippets = response == null
                || response.getRepairItems() == null
                || response.getRepairItems().isEmpty()
                ? List.of()
                : response.getRepairItems().get(0).getEvidenceSnippets();
        return StudentAiFeedbackResponse.FeedbackItem.builder()
                .title("边界样例意识")
                .body("修复后用最小 n、普通样例和边界样例复测，观察是否仍有多访问或少访问。")
                .kind("IMPROVEMENT")
                .knowledgePath(List.of("调试能力", "测试设计", "边界样例", "错误复现"))
                .evidenceSnippets(snippets == null ? List.of() : snippets)
                .evidenceRefs(List.of(evidenceRef))
                .qualitySignals(List.of("transfer"))
                .build();
    }

    private List<StudentAiFeedbackResponse.FeedbackItem> mergeRuntimeRepairItems(
            StudentAiFeedbackResponse.FeedbackItem primary,
            List<StudentAiFeedbackResponse.FeedbackItem> existingItems
    ) {
        List<StudentAiFeedbackResponse.FeedbackItem> merged = new ArrayList<>();
        addFeedbackItemIfDistinct(merged, primary);
        for (StudentAiFeedbackResponse.FeedbackItem item : existingItems == null ? List.<StudentAiFeedbackResponse.FeedbackItem>of() : existingItems) {
            if (item == null || isCodeCleanupDriftItem(item) || mentionsIndexRuntimeCause(item)) {
                continue;
            }
            addFeedbackItemIfDistinct(merged, item);
        }
        return merged;
    }

    private List<StudentAiFeedbackResponse.FeedbackItem> mergeRuntimeImprovementItems(
            StudentAiFeedbackResponse.FeedbackItem primary,
            List<StudentAiFeedbackResponse.FeedbackItem> existingItems
    ) {
        List<StudentAiFeedbackResponse.FeedbackItem> merged = new ArrayList<>();
        addFeedbackItemIfDistinct(merged, primary);
        for (StudentAiFeedbackResponse.FeedbackItem item : existingItems == null ? List.<StudentAiFeedbackResponse.FeedbackItem>of() : existingItems) {
            if (item == null || isCodeCleanupDriftItem(item)) {
                continue;
            }
            addFeedbackItemIfDistinct(merged, item);
        }
        return merged;
    }

    private void addFeedbackItemIfDistinct(List<StudentAiFeedbackResponse.FeedbackItem> items,
                                           StudentAiFeedbackResponse.FeedbackItem candidate) {
        if (candidate == null) {
            return;
        }
        String key = feedbackItemKey(candidate);
        boolean exists = items.stream()
                .filter(item -> item != null)
                .map(this::feedbackItemKey)
                .anyMatch(existingKey -> existingKey.equals(key));
        if (!exists) {
            items.add(candidate);
        }
    }

    private String feedbackItemKey(StudentAiFeedbackResponse.FeedbackItem item) {
        return (defaultIfBlank(item.getKind(), "") + "|"
                + defaultIfBlank(item.getTitle(), "") + "|"
                + defaultIfBlank(item.getBody(), ""))
                .replaceAll("\\s+", "");
    }

    private String removeCodeCleanupDriftSentence(String text) {
        String normalized = cleanupAiText(text);
        if (normalized.isBlank()) {
            return normalized;
        }
        String[] sentences = normalized.split("(?<=[。！？])");
        StringBuilder builder = new StringBuilder();
        for (String sentence : sentences) {
            String item = cleanupAiText(sentence);
            if (item.isBlank()) {
                continue;
            }
            String lower = item.toLowerCase();
            boolean codeCleanup = lower.contains("helper")
                    || lower.contains("辅助函数")
                    || lower.contains("无关函数")
                    || lower.contains("代码长")
                    || lower.contains("代码过长")
                    || lower.contains("精简代码");
            boolean runtimeCause = lower.contains("下标")
                    || lower.contains("索引")
                    || lower.contains("越界")
                    || lower.contains("indexerror")
                    || lower.contains("list index");
            if (codeCleanup && !runtimeCause) {
                continue;
            }
            builder.append(item);
        }
        String cleaned = builder.toString().trim();
        return cleaned.isBlank() ? normalized : cleaned;
    }

    private boolean indexImprovementDriftsToCodeCleanup(StudentAiFeedbackResponse response) {
        if (response != null && response.getStudentReport() != null) {
            if (textDriftsToCodeCleanup(response.getStudentReport().getImprovementLayerText())) {
                return true;
            }
        }
        if (response != null && response.getImprovementItems() != null) {
            for (StudentAiFeedbackResponse.FeedbackItem item : response.getImprovementItems()) {
                if (isCodeCleanupDriftItem(item)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isCodeCleanupDriftItem(StudentAiFeedbackResponse.FeedbackItem item) {
        if (item == null) {
            return false;
        }
        String text = defaultIfBlank(item.getTitle(), "") + "\n" + defaultIfBlank(item.getBody(), "");
        return textDriftsToCodeCleanup(text);
    }

    private boolean textDriftsToCodeCleanup(String text) {
        String normalized = defaultIfBlank(text, "").toLowerCase();
        boolean codeCleanup = normalized.contains("helper")
                || normalized.contains("辅助函数")
                || normalized.contains("冗余")
                || normalized.contains("精简")
                || normalized.contains("代码结构");
        boolean boundaryAware = normalized.contains("边界")
                || normalized.contains("下标")
                || normalized.contains("索引")
                || normalized.contains("循环")
                || normalized.contains("数组长度");
        return codeCleanup && !boundaryAware;
    }

    private boolean mentionsIndexRuntimeCause(StudentAiFeedbackResponse.FeedbackItem item) {
        if (item == null) {
            return false;
        }
        String text = (defaultIfBlank(item.getTitle(), "") + "\n" + defaultIfBlank(item.getBody(), "")).toLowerCase();
        return mentionsIndexRuntimeCause(text);
    }

    private boolean mentionsIndexRuntimeCause(StudentAiFeedbackResponse response) {
        StringBuilder builder = new StringBuilder();
        if (response.getStudentReport() != null) {
            builder.append(defaultIfBlank(response.getStudentReport().getBasicLayerText(), "")).append('\n')
                    .append(defaultIfBlank(response.getStudentReport().getImprovementLayerText(), "")).append('\n')
                    .append(defaultIfBlank(response.getStudentReport().getNextActionText(), "")).append('\n');
        }
        if (response.getRepairItems() != null) {
            response.getRepairItems().stream()
                    .filter(item -> item != null)
                    .forEach(item -> builder.append(defaultIfBlank(item.getTitle(), "")).append('\n')
                            .append(defaultIfBlank(item.getBody(), "")).append('\n'));
        }
        return mentionsIndexRuntimeCause(builder.toString().toLowerCase());
    }

    private boolean mentionsIndexRuntimeCause(String text) {
        return text.contains("下标")
                || text.contains("索引")
                || text.contains("越界")
                || text.contains("数组外")
                || text.contains("indexerror")
                || text.contains("list index");
    }

    private StudentAiFeedbackResponse.StudentReport normalizeStudentReport(StudentReportPayload payload,
                                                                           List<StudentAiFeedbackResponse.FeedbackItem> repairItems,
                                                                           List<StudentAiFeedbackResponse.FeedbackItem> improvementItems,
                                                                           String nextQuestion) {
        String basicLayerText = cleanStudentReportText(payload == null ? null : payload.basicLayerText);
        String improvementLayerText = cleanStudentReportText(payload == null ? null : payload.improvementLayerText);
        String nextActionText = cleanNextActionText(payload == null ? null : payload.nextActionText);
        if (basicLayerText.isBlank() && repairItems != null && !repairItems.isEmpty()) {
            basicLayerText = repairItems.stream()
                    .map(StudentAiFeedbackResponse.FeedbackItem::getBody)
                    .filter(text -> text != null && !text.isBlank())
                    .findFirst()
                    .orElse("");
        }
        if (improvementLayerText.isBlank() && improvementItems != null && !improvementItems.isEmpty()) {
            improvementLayerText = improvementItems.stream()
                    .map(StudentAiFeedbackResponse.FeedbackItem::getBody)
                    .filter(text -> text != null && !text.isBlank())
                    .findFirst()
                    .orElse("");
        }
        if (nextActionText.isBlank()) {
            nextActionText = cleanNextActionText(nextQuestion);
        }
        if (basicLayerText.isBlank() && improvementLayerText.isBlank() && nextActionText.isBlank()) {
            return null;
        }
        return StudentAiFeedbackResponse.StudentReport.builder()
                .basicLayerText(basicLayerText.isBlank() ? null : basicLayerText)
                .improvementLayerText(improvementLayerText.isBlank() ? null : improvementLayerText)
                .nextActionText(nextActionText.isBlank() ? null : nextActionText)
                .build();
    }

    private List<StudentAiFeedbackResponse.FeedbackItem> normalizeFeedbackItems(List<StudentFeedbackItemPayload> payloadItems,
                                                                                Map<String, String> evidenceCandidateRefs) {
        if (payloadItems == null || payloadItems.isEmpty()) {
            return List.of();
        }
        List<StudentAiFeedbackResponse.FeedbackItem> items = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (StudentFeedbackItemPayload payload : payloadItems) {
            if (payload == null) {
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
                    .libraryItemId(cleanStudentFeedbackId(payload.libraryItemId))
                    .skillUnitId(cleanStudentFeedbackId(payload.skillUnitId))
                    .mistakePointId(cleanStudentFeedbackId(payload.mistakePointId))
                    .improvementPointId(cleanStudentFeedbackId(payload.improvementPointId))
                    .libraryFit(cleanLibraryFit(payload.libraryFit))
                    .knowledgePath(cleanList(payload.knowledgePath, List.of()).stream().limit(5).toList())
                    .evidenceRefs(resolveStudentEvidenceRefs(payload.evidenceRefs, evidenceCandidateRefs).stream().limit(4).toList())
                    .qualitySignals(cleanList(payload.qualitySignals, List.of()).stream().limit(5).toList())
                    .build());
        }
        return items;
    }

    private String cleanStudentFeedbackId(String value) {
        String id = cleanupAiText(value).trim();
        return id.isBlank() ? null : id;
    }

    private String cleanLibraryFit(String value) {
        String fit = cleanupAiText(value).trim().toUpperCase();
        return switch (fit) {
            case "HIT", "PARTIAL", "MISS", "OUT_OF_LIBRARY" -> fit;
            default -> null;
        };
    }

    private List<String> resolveStudentEvidenceRefs(List<String> refs, Map<String, String> evidenceCandidateRefs) {
        if (refs == null || refs.isEmpty()) {
            return List.of();
        }
        return refs.stream()
                .map(ref -> resolveStudentEvidenceRef(ref, evidenceCandidateRefs))
                .filter(ref -> !ref.isBlank())
                .distinct()
                .toList();
    }

    private String resolveStudentEvidenceRef(String ref, Map<String, String> evidenceCandidateRefs) {
        String cleaned = normalizeStudentEvidenceRef(cleanupAiText(ref));
        if (cleaned.isBlank()) {
            return "";
        }
        String mapped = evidenceCandidateRefs == null ? null : evidenceCandidateRefs.get(cleaned);
        return mapped == null ? cleaned : mapped;
    }

    private String normalizeStudentEvidenceRef(String value) {
        String cleaned = defaultIfBlank(value, "").trim();
        if (cleaned.isBlank()) {
            return "";
        }
        String compact = cleaned.replaceAll("\\s+", "");
        Matcher lineMatcher = Pattern.compile("(?i)^code[:_\\-]?line[:_\\-]?(\\d+)$").matcher(compact);
        if (lineMatcher.find()) {
            return "code:line:" + lineMatcher.group(1);
        }
        Matcher rangeMatcher = Pattern.compile("(?i)^code[:_\\-]?range[:_\\-]?(\\d+)[:_\\-](\\d+)$").matcher(compact);
        if (rangeMatcher.find()) {
            return "code:range:" + rangeMatcher.group(1) + "-" + rangeMatcher.group(2);
        }
        Matcher judgeCaseMatcher = Pattern.compile("(?i)^judge[:_\\-]?case[:_\\-]?(\\d+)(?:[:_\\-]?output)?$").matcher(compact);
        if (judgeCaseMatcher.find()) {
            return "judge:first_failed_case:" + judgeCaseMatcher.group(1);
        }
        return cleaned;
    }

    private List<StudentAiFeedbackResponse.FeedbackItem> enrichFeedbackItems(List<StudentAiFeedbackResponse.FeedbackItem> items,
                                                                             Submission submission) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        return items.stream()
                .filter(item -> item != null)
                .map(item -> enrichFeedbackItem(item, submission))
                .toList();
    }

    private StudentAiFeedbackResponse.FeedbackItem enrichFeedbackItem(StudentAiFeedbackResponse.FeedbackItem item,
                                                                      Submission submission) {
        List<String> knowledgePath = cleanList(item.getKnowledgePath(), List.of());
        if (knowledgePath.isEmpty()) {
            knowledgePath = inferredKnowledgePath(item);
        }
        List<StudentAiFeedbackResponse.EvidenceSnippet> snippets = evidenceSnippets(item.getEvidenceRefs(), submission);
        return StudentAiFeedbackResponse.FeedbackItem.builder()
                .title(item.getTitle())
                .body(item.getBody())
                .kind(item.getKind())
                .libraryItemId(item.getLibraryItemId())
                .skillUnitId(item.getSkillUnitId())
                .mistakePointId(item.getMistakePointId())
                .improvementPointId(item.getImprovementPointId())
                .libraryFit(item.getLibraryFit())
                .knowledgePath(knowledgePath)
                .evidenceSnippets(snippets)
                .evidenceRefs(item.getEvidenceRefs())
                .qualitySignals(item.getQualitySignals())
                .build();
    }

    private List<StudentAiFeedbackResponse.EvidenceSnippet> evidenceSnippets(List<String> evidenceRefs,
                                                                             Submission submission) {
        if (submission == null || submission.getSourceCode() == null || evidenceRefs == null || evidenceRefs.isEmpty()) {
            return List.of();
        }
        List<StudentAiFeedbackResponse.EvidenceSnippet> snippets = new ArrayList<>();
        for (String ref : evidenceRefs) {
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
        String ref = cleanupAiText(evidenceRef);
        if (ref.isBlank() || sourceCode == null) {
            return null;
        }
        Matcher lineMatcher = Pattern.compile("^code:line:(\\d+)$").matcher(ref);
        if (lineMatcher.find()) {
            int line = parsePositiveInt(lineMatcher.group(1));
            String code = sourceLine(sourceCode, line);
            if (line > 0 && !code.isBlank()) {
                return StudentAiFeedbackResponse.EvidenceSnippet.builder()
                        .evidenceRef(ref)
                        .lineNumber(line)
                        .lineEnd(line)
                        .code(code)
                        .build();
            }
        }
        Matcher rangeMatcher = Pattern.compile("^code:range:(\\d+)-(\\d+)$").matcher(ref);
        if (rangeMatcher.find()) {
            int start = parsePositiveInt(rangeMatcher.group(1));
            int end = Math.min(start + 4, parsePositiveInt(rangeMatcher.group(2)));
            String code = sourceLines(sourceCode, start, end);
            if (start > 0 && end >= start && !code.isBlank()) {
                return StudentAiFeedbackResponse.EvidenceSnippet.builder()
                        .evidenceRef(ref)
                        .lineNumber(start)
                        .lineEnd(end)
                        .code(code)
                        .build();
            }
        }
        return null;
    }

    private int parsePositiveInt(String value) {
        try {
            return Math.max(0, Integer.parseInt(value));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private String sourceLines(String sourceCode, int startLine, int endLine) {
        if (sourceCode == null || startLine <= 0 || endLine < startLine) {
            return "";
        }
        String[] lines = sourceCode.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        if (startLine > lines.length) {
            return "";
        }
        int end = Math.min(endLine, lines.length);
        StringBuilder builder = new StringBuilder();
        for (int line = startLine; line <= end; line++) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(line).append(": ").append(lines[line - 1]);
        }
        return cleanupAiText(builder.toString());
    }

    private List<String> inferredKnowledgePath(StudentAiFeedbackResponse.FeedbackItem item) {
        String text = (defaultIfBlank(item.getKind(), "") + "\n"
                + defaultIfBlank(item.getTitle(), "") + "\n"
                + defaultIfBlank(item.getBody(), "")).toLowerCase();
        if (text.contains("indexerror") || text.contains("下标") || text.contains("索引") || text.contains("越界")) {
            return List.of("程序基础", "数组/列表", "下标访问", "越界检查");
        }
        if (text.contains("二分") || text.contains("边界更新") || text.contains("搜索范围")) {
            return List.of("算法基础", "二分查找", "边界更新", "可行区间维护");
        }
        if (text.contains("输入") || text.contains("读取") || text.contains("格式")) {
            return List.of("程序基础", "输入输出", "输入解析", "格式匹配");
        }
        if (text.contains("循环") || text.contains("range") || text.contains("边界")) {
            return List.of("程序基础", "循环结构", "循环边界", "端点取值");
        }
        if (text.contains("字符串") || text.contains("子串") || text.contains("哈希")) {
            return List.of("算法提高", "字符串", "子串查找", "重复判断");
        }
        if (text.contains("测试") || text.contains("样例")) {
            return List.of("调试能力", "测试设计", "边界样例", "错误复现");
        }
        return List.of();
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
                .source("AI_UNAVAILABLE")
                .latencyMs(elapsedMs(startedAt))
                .repairItems(List.of())
                .improvementItems(List.of())
                .studentReport(failureStudentReport(reason))
                .nextQuestion(null)
                .safety(StudentAiFeedbackResponse.Safety.builder()
                        .answerLeakRisk("LOW")
                        .blockedReasons(reason == null || reason.isBlank() ? List.of() : List.of(reason))
                        .build())
                .evidenceRefs(List.of())
                .build();
    }

    private StudentAiFeedbackResponse.StudentReport failureStudentReport(String reason) {
        String normalizedReason = defaultIfBlank(reason, "GENERATION_FAILED");
        String basic = "AI 暂不可用。先根据评测结果修改代码，稍后重试。";
        if (normalizedReason.toLowerCase().contains("limit") || normalizedReason.toLowerCase().contains("429")) {
            basic = "AI 请求受限。先根据评测结果修改代码，稍后重试。";
        }
        return StudentAiFeedbackResponse.StudentReport.builder()
                .basicLayerText(basic)
                .improvementLayerText("AI 暂不可用，暂无提升建议。")
                .nextActionText("先查看首个失败测试点，修完后重试 AI。")
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

    private String cleanStudentReportText(String value) {
        String text = cleanupAiText(value).trim();
        text = text.replaceAll("（?\\(?\\s*(?:verdict|code|evidenceRefs?|judgeFacts)\\s*:[^）)；;。\\n]*(?:，\\s*(?:verdict|code|evidenceRefs?|judgeFacts)\\s*:[^）)；;。\\n]*)*\\s*\\)?）?", "").trim();
        if (text.length() > 320) {
            text = text.substring(0, 320).trim();
        }
        return text;
    }

    private String cleanNextActionText(String value) {
        String text = cleanStudentReportText(value);
        text = text.replaceFirst("^\\s*1[.、)]\\s*", "").trim();
        String[] laterSteps = {" 2.", " 2、", " 2)", "；2.", "；2、", ";2.", ";2、", "\n2.", "\n2、", " ②", "；②", "\n②"};
        for (String marker : laterSteps) {
            int index = text.indexOf(marker);
            if (index > 0) {
                text = text.substring(0, index).trim();
                break;
            }
        }
        if (text.length() > 70) {
            text = text.substring(0, 70).trim();
        }
        return text;
    }

    private boolean hasStudentReportText(StudentAiFeedbackResponse.StudentReport report) {
        return report != null && (!defaultIfBlank(report.getBasicLayerText(), "").isBlank()
                || !defaultIfBlank(report.getImprovementLayerText(), "").isBlank()
                || !defaultIfBlank(report.getNextActionText(), "").isBlank());
    }

    private boolean studentReportLeaksAnswer(StudentAiFeedbackResponse.StudentReport report) {
        return report != null && (leaksAnswer(report.getBasicLayerText())
                || leaksAnswer(report.getImprovementLayerText())
                || leaksAnswer(report.getNextActionText()));
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

    private record NavigationLoopResult(StandardLibraryNavigationOutput output,
                                        ExternalModelStagePayloads.StageValidationResult failure) {
        static NavigationLoopResult success(StandardLibraryNavigationOutput output) {
            return new NavigationLoopResult(output, null);
        }

        static NavigationLoopResult failure(ExternalModelStagePayloads.StageValidationResult failure) {
            return new NavigationLoopResult(null, failure);
        }

        static NavigationLoopResult empty() {
            return new NavigationLoopResult(null, null);
        }
    }

    private record NavigationExpansionBatch(
            List<AiStandardLibraryNavigationExpansionResponse> expandedNodes,
            List<AiStandardLibraryDiagnosticLayerResponse> diagnosticLayers) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class StudentFastFeedbackPayload {
        public StudentReportPayload studentReport;
        public List<StudentFeedbackItemPayload> repairItems;
        public List<StudentFeedbackItemPayload> improvementItems;
        public List<AdviceGenerationOutput.DiagnosisCandidate> diagnosisCandidates;
        public AdviceGenerationOutput.LibraryGrowth libraryGrowth;
        public String nextQuestion;
        public StudentFeedbackSafetyPayload safety;
        public List<String> evidenceRefs;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class StudentReportPayload {
        public String basicLayerText;
        public String improvementLayerText;
        public String nextActionText;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class StudentFeedbackItemPayload {
        public String title;
        public String body;
        public String kind;
        public String libraryItemId;
        public String skillUnitId;
        public String mistakePointId;
        public String improvementPointId;
        public String libraryFit;
        public List<String> knowledgePath;
        public List<String> evidenceRefs;
        public List<String> qualitySignals;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class StudentFeedbackSafetyPayload {
        public String answerLeakRisk;
        public List<String> blockedReasons;
    }
}
