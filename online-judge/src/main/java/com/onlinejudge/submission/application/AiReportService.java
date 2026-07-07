package com.onlinejudge.submission.application;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlinejudge.learning.standardlibrary.application.AiStandardLibraryGrowthAgentService;
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

    private static final String AI_SOURCE = "MODEL_SCOPE_EXTERNAL_MODEL";
    private static final String PROVIDER = "ModelScope";
    private static final String PROMPT_VERSION = "submission-diagnosis-prompt-v2";
    private static final String STUDENT_FAST_FEEDBACK_PROMPT_VERSION = "student-fast-feedback-v2";
    private static final Pattern RUNTIME_LINE_PATTERN = Pattern.compile("\\bline\\s+(\\d+)\\b");

    private final ObjectMapper objectMapper;
    private final AiCodeAssistSupport aiCodeAssistSupport;
    private final ExternalModelAgentRuntime externalModelAgentRuntime;
    private final ExternalModelFailureClassifier failureClassifier;
    private final ExternalModelBudgetGuard budgetGuard;
    private final ExternalModelChatRequestFactory chatRequestFactory;
    private final SearchLocationRetrievalService searchLocationRetrievalService;
    private final SearchLocationOutputValidator searchLocationOutputValidator;
    private final SearchLocationOutputNormalizer searchLocationOutputNormalizer;
    private final SearchLocationPackSelector searchLocationPackSelector;
    private final SearchLocationProperties searchLocationProperties;
    private final AiStandardLibraryGrowthAgentService standardLibraryGrowthAgentService;
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

    public AiReportService(ObjectMapper objectMapper,
                           AiCodeAssistSupport aiCodeAssistSupport,
                           ExternalModelAgentRuntime externalModelAgentRuntime,
                           ExternalModelFailureClassifier failureClassifier,
                           ExternalModelBudgetGuard budgetGuard) {
        this(objectMapper, aiCodeAssistSupport, externalModelAgentRuntime, failureClassifier, budgetGuard,
                (ExternalModelChatRequestFactory) null, null, null, null, null, null);
    }

    public AiReportService(ObjectMapper objectMapper,
                           AiCodeAssistSupport aiCodeAssistSupport,
                           ExternalModelAgentRuntime externalModelAgentRuntime,
                           ExternalModelFailureClassifier failureClassifier,
                           ExternalModelBudgetGuard budgetGuard,
                           ExternalModelChatRequestFactory chatRequestFactory) {
        this(objectMapper, aiCodeAssistSupport, externalModelAgentRuntime, failureClassifier, budgetGuard,
                chatRequestFactory, null, null, null, null, null, null);
    }

    @Autowired
    public AiReportService(ObjectMapper objectMapper,
                           AiCodeAssistSupport aiCodeAssistSupport,
                           ExternalModelAgentRuntime externalModelAgentRuntime,
                           ExternalModelFailureClassifier failureClassifier,
                           ExternalModelBudgetGuard budgetGuard,
                           SearchLocationRetrievalService searchLocationRetrievalService,
                           SearchLocationOutputValidator searchLocationOutputValidator,
                           SearchLocationOutputNormalizer searchLocationOutputNormalizer,
                           SearchLocationPackSelector searchLocationPackSelector,
                           SearchLocationProperties searchLocationProperties,
                           AiStandardLibraryGrowthAgentService standardLibraryGrowthAgentService) {
        this(objectMapper, aiCodeAssistSupport, externalModelAgentRuntime, failureClassifier, budgetGuard,
                new ExternalModelChatRequestFactory(), searchLocationRetrievalService, searchLocationOutputValidator,
                searchLocationOutputNormalizer, searchLocationPackSelector, searchLocationProperties,
                standardLibraryGrowthAgentService);
    }

    public AiReportService(ObjectMapper objectMapper,
                           AiCodeAssistSupport aiCodeAssistSupport,
                           ExternalModelAgentRuntime externalModelAgentRuntime,
                           ExternalModelFailureClassifier failureClassifier,
                           ExternalModelBudgetGuard budgetGuard,
                           ExternalModelChatRequestFactory chatRequestFactory,
                           SearchLocationRetrievalService searchLocationRetrievalService,
                           SearchLocationOutputValidator searchLocationOutputValidator,
                           SearchLocationOutputNormalizer searchLocationOutputNormalizer,
                           SearchLocationPackSelector searchLocationPackSelector,
                           SearchLocationProperties searchLocationProperties) {
        this(objectMapper, aiCodeAssistSupport, externalModelAgentRuntime, failureClassifier, budgetGuard,
                chatRequestFactory, searchLocationRetrievalService, searchLocationOutputValidator,
                searchLocationOutputNormalizer, searchLocationPackSelector, searchLocationProperties, null);
    }

    public AiReportService(ObjectMapper objectMapper,
                           AiCodeAssistSupport aiCodeAssistSupport,
                           ExternalModelAgentRuntime externalModelAgentRuntime,
                           ExternalModelFailureClassifier failureClassifier,
                           ExternalModelBudgetGuard budgetGuard,
                           ExternalModelChatRequestFactory chatRequestFactory,
                           SearchLocationRetrievalService searchLocationRetrievalService,
                           SearchLocationOutputValidator searchLocationOutputValidator,
                           SearchLocationOutputNormalizer searchLocationOutputNormalizer,
                           SearchLocationPackSelector searchLocationPackSelector,
                           SearchLocationProperties searchLocationProperties,
                           AiStandardLibraryGrowthAgentService standardLibraryGrowthAgentService) {
        this.objectMapper = objectMapper;
        this.aiCodeAssistSupport = aiCodeAssistSupport;
        this.externalModelAgentRuntime = externalModelAgentRuntime;
        this.failureClassifier = failureClassifier == null ? new ExternalModelFailureClassifier() : failureClassifier;
        this.budgetGuard = budgetGuard == null ? new ExternalModelBudgetGuard() : budgetGuard;
        this.chatRequestFactory = chatRequestFactory == null ? new ExternalModelChatRequestFactory() : chatRequestFactory;
        this.searchLocationRetrievalService = searchLocationRetrievalService;
        this.searchLocationOutputValidator = searchLocationOutputValidator == null
                ? new SearchLocationOutputValidator()
                : searchLocationOutputValidator;
        this.searchLocationOutputNormalizer = searchLocationOutputNormalizer == null
                ? new SearchLocationOutputNormalizer()
                : searchLocationOutputNormalizer;
        this.searchLocationPackSelector = searchLocationPackSelector;
        this.searchLocationProperties = searchLocationProperties == null ? new SearchLocationProperties() : searchLocationProperties;
        this.standardLibraryGrowthAgentService = standardLibraryGrowthAgentService;
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

    private final Object studentFeedbackThrottle = new Object();
    private long lastStudentFeedbackRequestAtMs = 0L;

    public SubmissionAnalysisResponse enhanceSubmissionAnalysis(Problem problem,
                                                                Submission submission,
                                                                SubmissionAnalysisResponse fallback) {
        return enhanceSubmissionAnalysis(problem, submission, fallback, null);
    }

    public SubmissionAnalysisResponse enhanceSubmissionAnalysis(Problem problem,
                                                                Submission submission,
                                                                SubmissionAnalysisResponse fallback,
                                                                DiagnosisEvidencePackage evidencePackage) {
        if (!canCallAi()) {
            log.info("AI submission analysis skipped because AI access is unavailable. submissionId={}", submission.getId());
            return runtimeFailure(fallback, aiUnavailableFailure("SUBMISSION_ANALYSIS"));
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
            if (shouldUseExternalRuntime(evidencePackage)) {
                return enhanceWithExternalRuntime(
                        submission,
                        fallback,
                        evidencePackage,
                        rawSourceCode,
                        baselineLineIssues
                );
            }
            log.info("AI submission analysis skipped because new runtime context is incomplete. submissionId={}",
                    submission.getId());
            return runtimeFailure(fallback, stageFailure(
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
                return runtimeFailure(fallback, stageFailureFromException("SUBMISSION_ANALYSIS", exception));
            }
            return runtimeFailure(fallback, stageFailureFromException("SUBMISSION_ANALYSIS", exception));
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
                    14. knowledgePath 是父子关系路径，不是独立标签；使用 3-5 段中文知识树路径，例如 ["程序基础","数组/列表","下标访问","越界检查"]；不确定时可以留空，由后端兜底。
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
                                                                  SubmissionAnalysisResponse fallback,
                                                                  DiagnosisEvidencePackage evidencePackage,
                                                                  String rawSourceCode,
                                                                  List<SubmissionAnalysisResponse.LineIssue> baselineLineIssues)
            throws JsonProcessingException, IOException, InterruptedException {
        ExternalModelAgentRuntime.RuntimePlan runtimePlan = externalModelAgentRuntime.prepare(
                evidencePackage,
                fallback,
                teacherDiagnosisRuntimeProfile()
        );
        runtimePlan = applySearchLocationIfAvailable(runtimePlan);
        return enhanceWithAdviceGenerationRuntime(
                submission,
                fallback,
                runtimePlan,
                rawSourceCode,
                baselineLineIssues
        );
    }

    private ExternalModelAgentRuntime.RuntimePlan prepareStudentFeedbackRuntimePlan(
            DiagnosisEvidencePackage evidencePackage) {
        if (externalModelAgentRuntime == null || evidencePackage == null) {
            return null;
        }
        try {
            ExternalModelAgentRuntime.RuntimePlan runtimePlan = externalModelAgentRuntime.prepare(
                    evidencePackage,
                    null,
                    externalRuntimeProfile
            );
            return applyLocalSearchLocationOnly(runtimePlan);
        } catch (Exception exception) {
            log.warn("Student AI feedback standard library context unavailable. reason={}", exception.getMessage());
            return null;
        }
    }

    private ExternalModelAgentRuntime.RuntimePlan applyLocalSearchLocationOnly(
            ExternalModelAgentRuntime.RuntimePlan runtimePlan) {
        if (runtimePlan == null || searchLocationRetrievalService == null || searchLocationPackSelector == null) {
            return runtimePlan;
        }
        SearchLocationCandidatePack candidatePack = searchLocationRetrievalService.retrieve(runtimePlan.getBrief());
        if (candidatePack == null) {
            return runtimePlan;
        }
        if (candidatePack.getCandidates() == null || candidatePack.getCandidates().isEmpty()) {
            runtimePlan.setSearchLocationResult(SearchLocationResult.localOnly(
                    candidatePack.getEmbeddingStatus(),
                    candidatePack,
                    runtimePlan.getStandardLibraryPack()
            ));
            return runtimePlan;
        }
        SearchLocationOutput localOutput = localSearchLocationOutput(candidatePack);
        StandardLibraryPack selectedPack = searchLocationPackSelector.select(
                localOutput,
                candidatePack,
                runtimePlan.getStandardLibraryPack()
        );
        runtimePlan.setSearchLocationResult(SearchLocationResult.builder()
                .enabled(false)
                .status("LOCAL_RECALL")
                .embeddingStatus(candidatePack.getEmbeddingStatus())
                .fallbackReason("")
                .candidateCount(candidatePack.getCandidateCount())
                .selectedCount(selectedCount(localOutput))
                .candidatePack(candidatePack)
                .output(localOutput)
                .selectedPack(selectedPack)
                .build());
        runtimePlan.setStandardLibraryPack(selectedPack);
        return runtimePlan;
    }

    private SubmissionAnalysisResponse enhanceWithAdviceGenerationRuntime(
            Submission submission,
            SubmissionAnalysisResponse fallback,
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
            runtimePlan.setAdviceGenerationResult(AdviceGenerationResult.fallback(
                    adviceFallbackReason(failure),
                    promptVersion
            ));
            return runtimeFailure(fallback, runtimePlan, failure);
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
            runtimePlan.setAdviceGenerationResult(AdviceGenerationResult.fallback(
                    adviceFallbackReason(adviceValidation),
                    promptVersion
            ));
            log.warn("External model advice generation failed validation. submissionId={}, reason={}, message={}",
                    submission.getId(),
                    adviceValidation.getFailureReason(),
                    adviceValidation.getMessage());
            return runtimeFailure(fallback, runtimePlan, adviceValidation);
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
            runtimePlan.setAdviceGenerationResult(AdviceGenerationResult.fallback(
                    adviceFallbackReason(feedbackValidation),
                    promptVersion
            ));
            return runtimeFailure(fallback, runtimePlan, feedbackValidation);
        }

        SubmissionAnalysisResponse response = buildAdviceRuntimeAnalysisResponse(
                fallback,
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
        request.put("task", "请完成一次高中信息学提交诊断：先整体理解题目、代码和判题结果，再参考标准库候选，最后生成 studentReport 和结构化元数据。");
        request.put("contextPolicy", List.of(
                "problem.description 是完整题目描述；submission.sourceCodeWithLineNumbers 是完整学生代码或最大可用带行号代码。",
                "submission.verdict、visibleCaseFacts、runtimeErrorMessage、compileOutput 和 evidenceRefs 只是判题参考信号，用来验证诊断。",
                "standardLibrary.knowledgeGroups 是结构化标准库邻域，包含相关知识路径、能力点、易错点和提升点；不要把它当成无关全库倾倒。",
                "standardLibrary 是教学参考规范包，像课程标准和教案目录，用于细颗粒定位、标准化命名和区分基础层/提高层，不是强制答案表。",
                "primaryKnowledgeNodeCode 是主知识路径，relatedKnowledgeNodeCodes 只是辅助上下文；学生端知识路径优先沿主路径表达，不把相关标签平铺成独立问题。",
                "先基于题目、代码、判题结果和 evidenceRefs 自由诊断，再输出 diagnosisCandidates 并评判它们对标准库是 HIT、PARTIAL、MISS 还是 OUT_OF_LIBRARY。",
                "判题结果只是参考信号；如果代码语义已经显示题意约束和实际转移不一致，应优先诊断这类真实差异。",
                "每个 diagnosisCandidates 项必须携带 libraryPath；如果具体错因未覆盖，挂到最接近的上级路径，并给出可审核的 libraryGrowth 候选。",
                "如果候选不匹配，允许 null id、MISS 或 OUT_OF_LIBRARY，并给出可审核的库外发现。",
                "学生可见反馈要自然、具体、可行动，但不能给完整答案或逐行改法。"
        ));
        request.put("brief", runtimePlan.getBrief());
        request.put("standardLibrary", runtimePlan.getStandardLibraryPack());
        request.put("searchLocationSummary", runtimePlan.getStandardLibraryPack() == null
                ? null
                : runtimePlan.getStandardLibraryPack().getSearchLocationSummary());
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
        request.put("searchLocationSummary", runtimePlan.getStandardLibraryPack() == null
                ? null
                : runtimePlan.getStandardLibraryPack().getSearchLocationSummary());
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

    private ExternalModelAgentRuntime.RuntimePlan applySearchLocationIfAvailable(
            ExternalModelAgentRuntime.RuntimePlan runtimePlan) {
        if (runtimePlan == null) {
            return runtimePlan;
        }
        if (searchLocationRetrievalService == null || searchLocationPackSelector == null) {
            if (!searchLocationProperties.isEnabled()) {
                runtimePlan.setSearchLocationResult(SearchLocationResult.disabled());
                return runtimePlan;
            }
            runtimePlan.setSearchLocationResult(SearchLocationResult.fallback(
                    "FALLBACK_USED",
                    "UNAVAILABLE",
                    "SEARCH_LOCATION_SERVICES_UNAVAILABLE",
                    null
            ));
            return runtimePlan;
        }
        SearchLocationCandidatePack candidatePack = null;
        try {
            candidatePack = searchLocationRetrievalService.retrieve(runtimePlan.getBrief());
            if (candidatePack.getCandidates() == null || candidatePack.getCandidates().isEmpty()) {
                if (!searchLocationProperties.isEnabled()) {
                    runtimePlan.setSearchLocationResult(SearchLocationResult.localOnly(
                            candidatePack.getEmbeddingStatus(),
                            candidatePack,
                            runtimePlan.getStandardLibraryPack()
                    ));
                    return runtimePlan;
                }
                runtimePlan.setSearchLocationResult(SearchLocationResult.fallback(
                        "FALLBACK_USED",
                        candidatePack.getEmbeddingStatus(),
                        "NO_SEARCH_LOCATION_CANDIDATES",
                        candidatePack
                ));
                return runtimePlan;
            }
            if (!searchLocationProperties.isEnabled()) {
                SearchLocationOutput localOutput = localSearchLocationOutput(candidatePack);
                StandardLibraryPack selectedPack = searchLocationPackSelector.select(
                        localOutput,
                        candidatePack,
                        runtimePlan.getStandardLibraryPack()
                );
                SearchLocationResult result = SearchLocationResult.builder()
                        .enabled(false)
                        .status("LOCAL_RECALL")
                        .embeddingStatus(candidatePack.getEmbeddingStatus())
                        .fallbackReason("")
                        .candidateCount(candidatePack.getCandidateCount())
                        .selectedCount(selectedCount(localOutput))
                        .candidatePack(candidatePack)
                        .output(localOutput)
                        .selectedPack(selectedPack)
                        .build();
                runtimePlan.setSearchLocationResult(result);
                runtimePlan.setStandardLibraryPack(selectedPack);
                return runtimePlan;
            }
            if (searchLocationProperties.isRequireVector()
                    && candidatePack.getEmbeddingStatus() != null
                    && !"READY".equalsIgnoreCase(candidatePack.getEmbeddingStatus())) {
                runtimePlan.setSearchLocationResult(SearchLocationResult.fallback(
                        "FALLBACK_USED",
                        candidatePack.getEmbeddingStatus(),
                        "VECTOR_REQUIRED_BUT_UNAVAILABLE",
                        candidatePack
                ));
                return runtimePlan;
            }
            SearchLocationOutput output = callSearchLocationStage(runtimePlan, candidatePack);
            output = searchLocationOutputNormalizer.normalize(output, candidatePack, runtimePlan.getBrief());
            ExternalModelStagePayloads.StageValidationResult validation =
                    searchLocationOutputValidator.validate(output, candidatePack, runtimePlan.getBrief());
            if (validation == null || !validation.isValid()) {
                runtimePlan.setSearchLocationResult(SearchLocationResult.fallback(
                        "FALLBACK_USED",
                        candidatePack.getEmbeddingStatus(),
                        validation == null ? "SEARCH_LOCATION_INVALID" : validation.getMessage(),
                        candidatePack
                ));
                return runtimePlan;
            }
            StandardLibraryPack selectedPack = searchLocationPackSelector.select(
                    output,
                    candidatePack,
                    runtimePlan.getStandardLibraryPack()
            );
            SearchLocationResult result = SearchLocationResult.builder()
                    .enabled(true)
                    .status("SUCCESS")
                    .embeddingStatus(candidatePack.getEmbeddingStatus())
                    .fallbackReason("")
                    .candidateCount(candidatePack.getCandidateCount())
                    .selectedCount(selectedCount(output))
                    .candidatePack(candidatePack)
                    .output(output)
                    .selectedPack(selectedPack)
                    .build();
            runtimePlan.setSearchLocationResult(result);
            runtimePlan.setStandardLibraryPack(selectedPack);
            return runtimePlan;
        } catch (Exception exception) {
            runtimePlan.setSearchLocationResult(SearchLocationResult.fallback(
                    "FALLBACK_USED",
                    candidatePack == null ? "UNKNOWN" : candidatePack.getEmbeddingStatus(),
                    "SEARCH_LOCATION_EXCEPTION:" + exception.getClass().getSimpleName(),
                    candidatePack
            ));
            return runtimePlan;
        }
    }

    private SearchLocationOutput localSearchLocationOutput(SearchLocationCandidatePack candidatePack) {
        List<SearchLocationCandidate> candidates = candidatePack == null || candidatePack.getCandidates() == null
                ? List.of()
                : candidatePack.getCandidates();
        return SearchLocationOutput.builder()
                .libraryFit(candidates.isEmpty() ? "MISS" : "PARTIAL")
                .basicCandidates(localSelectedCandidates(candidates, List.of("MISTAKE_POINT", "BASIC_CAUSE"), 8))
                .improvementCandidates(localSelectedCandidates(candidates, List.of("IMPROVEMENT_POINT"), 4))
                .knowledgeAnchors(localSelectedCandidates(candidates, List.of("SKILL_UNIT", "KNOWLEDGE_NODE"), 5))
                .uncertainty("本地召回候选，最终诊断由单诊断 Agent 完成。")
                .needsMoreEvidence(false)
                .build();
    }

    private List<SearchLocationOutput.SelectedCandidate> localSelectedCandidates(List<SearchLocationCandidate> candidates,
                                                                                 List<String> layers,
                                                                                 int limit) {
        return candidates.stream()
                .filter(candidate -> candidate != null && layers.contains(candidate.getLayer()))
                .limit(limit)
                .map(this::toLocalSelectedCandidate)
                .toList();
    }

    private SearchLocationOutput.SelectedCandidate toLocalSelectedCandidate(SearchLocationCandidate candidate) {
        String layer = candidate.getLayer();
        String id = candidate.getId();
        return SearchLocationOutput.SelectedCandidate.builder()
                .id(id)
                .layer(layer)
                .skillUnitId("SKILL_UNIT".equals(layer) ? id : candidate.getSkillUnitCode())
                .mistakePointId("MISTAKE_POINT".equals(layer) ? id : null)
                .libraryFit("PARTIAL")
                .priority(1)
                .confidence(candidate.getFinalScore() == null ? 0.5 : Math.max(0.0, Math.min(1.0, candidate.getFinalScore())))
                .evidenceRefs(List.of())
                .reason("召回：" + candidate.getName())
                .recallReason(String.join(", ", candidate.getRecallSources() == null
                        ? List.of()
                        : candidate.getRecallSources().stream().limit(3).toList()))
                .evidenceSource("LOCAL_RECALL")
                .uncertainty("由单诊断 Agent 最终确认。")
                .build();
    }

    private SearchLocationOutput callSearchLocationStage(ExternalModelAgentRuntime.RuntimePlan runtimePlan,
                                                        SearchLocationCandidatePack candidatePack)
            throws JsonProcessingException, IOException, InterruptedException {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("brief", runtimePlan.getBrief());
        request.put("candidatePack", candidatePack);
        activateRuntimePlan(runtimePlan);
        String content = chatCompletion(
                runtimePlan.getSearchLocationPrompt().getSystemPrompt(),
                objectMapper.writeValueAsString(request)
        );
        return parseModelStagePayload(content, SearchLocationOutput.class);
    }

    private int selectedCount(SearchLocationOutput output) {
        if (output == null) {
            return 0;
        }
        return size(output.getBasicCandidates())
                + size(output.getImprovementCandidates())
                + size(output.getKnowledgeAnchors());
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

    private String adviceFallbackReason(ExternalModelStagePayloads.StageValidationResult failure) {
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
                                             AdviceGenerationOutput output,
                                             String fallbackMarkdown) {
        if (output == null) {
            return fallbackMarkdown;
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
                && runtimePlan.getStandardLibraryPack().getSearchLocationSummary() != null) {
            StandardLibraryPack.SearchLocationSummary summary =
                    runtimePlan.getStandardLibraryPack().getSearchLocationSummary();
            builder.append("### 定位说明\n\n");
            appendMarkdownLine(builder, "定位状态", summary.getStatus());
            appendMarkdownLine(builder, "候选数量", summary.getCandidateCount() == null ? "" : summary.getCandidateCount().toString());
            appendMarkdownLine(builder, "精选数量", summary.getSelectedCount() == null ? "" : summary.getSelectedCount().toString());
        }
        String markdown = builder.toString().trim();
        return markdown.isBlank() ? fallbackMarkdown : markdown;
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
            SubmissionAnalysisResponse fallback,
            ExternalModelAgentRuntime.RuntimePlan runtimePlan,
            AdviceGenerationOutput adviceOutput,
            SubmissionAnalysisResponse.StudentFeedback studentFeedback,
            String rawSourceCode,
            List<SubmissionAnalysisResponse.LineIssue> baselineLineIssues) {
        List<String> evidenceRefs = adviceEvidenceRefs(adviceOutput, null);
        SubmissionAnalysisResponse.NextLearningAction nextAction =
                studentFeedback == null ? null : studentFeedback.getNextLearningAction();
        return SubmissionAnalysisResponse.builder()
                .submissionId(fallback.getSubmissionId())
                .analysisSchemaVersion(fallback.getAnalysisSchemaVersion())
                .evidenceSchemaVersion(fallback.getEvidenceSchemaVersion())
                .taxonomyVersion(fallback.getTaxonomyVersion())
                .sourceType(AI_SOURCE)
                .scenario(fallback.getScenario())
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
                .studentHintPlan(buildAdviceHintPlan(adviceOutput, studentFeedback, fallback))
                .studentFeedback(studentFeedback)
                .learningInterventionPlan(buildAdviceInterventionPlan(studentFeedback, fallback))
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
                .aiInvocation(modelInvocation(fallback, "MODEL_COMPLETED", false, runtimePromptVersion(runtimePlan), runtimePlan))
                .answerLeakRisk(resolveAnswerLeakRisk(nextAction == null ? "" : nextAction.getAnswerLeakRisk(),
                        "LOW"))
                .wrongSolution(null)
                .correctSolution(null)
                .lineIssues(List.of())
                .firstFailedCase(fallback.getFirstFailedCase())
                .reportMarkdown(buildAdviceReportMarkdown(runtimePlan, adviceOutput, ""))
                .generatedAt(fallback.getGeneratedAt())
                .build();
    }

    private List<String> adviceEvidenceRefs(AdviceGenerationOutput output, SubmissionAnalysisResponse fallback) {
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

    private String firstAdviceAction(AdviceGenerationOutput output, String fallback) {
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
        return fallback;
    }

    private SubmissionAnalysisResponse.StudentHintPlan buildAdviceHintPlan(
            AdviceGenerationOutput output,
            SubmissionAnalysisResponse.StudentFeedback studentFeedback,
            SubmissionAnalysisResponse fallback) {
        SubmissionAnalysisResponse.NextLearningAction nextAction =
                studentFeedback == null ? null : studentFeedback.getNextLearningAction();
        if (nextAction == null) {
            return null;
        }
        List<String> evidenceRefs = cleanList(nextAction.getEvidenceRefs(), adviceEvidenceRefs(output, null));
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
            SubmissionAnalysisResponse.StudentFeedback studentFeedback,
            SubmissionAnalysisResponse fallback) {
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
                .evidenceRefs(adviceEvidenceRefs(output, null))
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
        return PromptTemplateRegistry.DIAGNOSIS_REPORT_V2;
    }

    private String failureSummary(ExternalModelStagePayloads.StageValidationResult failure) {
        if (failure == null) {
            return "（失败阶段：UNKNOWN_STAGE；失败原因：UNKNOWN_ERROR）";
        }
        String stage = failure.getStage() == null || failure.getStage().isBlank() ? "UNKNOWN_STAGE" : failure.getStage();
        String reason = failure.getFailureReason() == null ? "UNKNOWN_ERROR" : failure.getFailureReason().name();
        return "（失败阶段：" + stage + "；失败原因：" + reason + "）";
    }

    private SubmissionAnalysisResponse runtimeFailure(SubmissionAnalysisResponse fallback,
                                                      ExternalModelStagePayloads.StageValidationResult validationResult) {
        return runtimeFailure(fallback, null, validationResult);
    }

    private SubmissionAnalysisResponse runtimeFailure(SubmissionAnalysisResponse fallback,
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
        String uncertainty = "外部模型未完成，本次未使用本地规则兜底。失败阶段：" + stage + "；失败原因：" + reason
                + (message.isBlank() ? "" : "，" + message);
        return SubmissionAnalysisResponse.builder()
                .submissionId(fallback.getSubmissionId())
                .analysisSchemaVersion(fallback.getAnalysisSchemaVersion())
                .evidenceSchemaVersion(fallback.getEvidenceSchemaVersion())
                .taxonomyVersion(fallback.getTaxonomyVersion())
                .sourceType(AI_SOURCE)
                .scenario(fallback.getScenario())
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
                        fallback,
                        "MODEL_FAILED",
                        false,
                        runtimePromptVersion(runtimePlan),
                        runtimePlan,
                        attributionResult
                ))
                .answerLeakRisk(defaultIfBlank(fallback.getAnswerLeakRisk(), "LOW"))
                .wrongSolution(null)
                .correctSolution(null)
                .lineIssues(List.of())
                .firstFailedCase(fallback.getFirstFailedCase())
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

    private String previewBody(String body) {
        String normalized = body == null ? "" : body.replace("\r", "").replace("\n", "\\n").trim();
        if (normalized.length() <= 320) {
            return normalized;
        }
        return normalized.substring(0, 317) + "...";
    }

    private List<String> cleanList(List<String> candidate, List<String> fallback) {
        List<String> source = candidate == null || candidate.isEmpty() ? fallback : candidate;
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

    private String defaultIfBlank(String candidate, String fallback) {
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
        return modelInvocation(fallback, status, fallbackUsed, PROMPT_VERSION);
    }

    private SubmissionAnalysisResponse.AiInvocation modelInvocation(SubmissionAnalysisResponse fallback,
                                                                    String status,
                                                                    boolean fallbackUsed,
                                                                    String promptVersion) {
        return modelInvocation(fallback, status, fallbackUsed, promptVersion,
                (ExternalModelAgentRuntime.RuntimePlan) null);
    }

    private SubmissionAnalysisResponse.AiInvocation modelInvocation(SubmissionAnalysisResponse fallback,
                                                                    String status,
                                                                    boolean fallbackUsed,
                                                                    String promptVersion,
                                                                    ExternalModelAgentRuntime.RuntimePlan runtimePlan) {
        return modelInvocation(fallback, status, fallbackUsed, promptVersion, runtimePlan, null);
    }

    private SubmissionAnalysisResponse.AiInvocation modelInvocation(SubmissionAnalysisResponse fallback,
                                                                    String status,
                                                                    boolean fallbackUsed,
                                                                    String promptVersion,
                                                                    ExternalModelStagePayloads.StageValidationResult failure) {
        return modelInvocation(fallback, status, fallbackUsed, promptVersion, null, failure);
    }

    private SubmissionAnalysisResponse.AiInvocation modelInvocation(SubmissionAnalysisResponse fallback,
                                                                    String status,
                                                                    boolean fallbackUsed,
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
                .searchLocationEnabled(searchLocation(runtimePlan).enabled())
                .searchLocationStatus(searchLocation(runtimePlan).status())
                .searchLocationCandidateCount(searchLocation(runtimePlan).candidateCount())
                .searchLocationSelectedCount(searchLocation(runtimePlan).selectedCount())
                .searchLocationFallbackReason(searchLocation(runtimePlan).fallbackReason())
                .recallSources(recallSources(runtimePlan))
                .embeddingStatus(searchLocation(runtimePlan).embeddingStatus())
                .adviceGenerationStatus(adviceGeneration(runtimePlan).status())
                .adviceGenerationFallbackReason(adviceGeneration(runtimePlan).fallbackReason())
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

    private List<String> recallSources(ExternalModelAgentRuntime.RuntimePlan runtimePlan) {
        SearchLocationCandidatePack candidatePack = searchLocation(runtimePlan).candidatePack();
        return candidatePack == null || candidatePack.getRecallSources() == null
                ? List.of()
                : candidatePack.getRecallSources();
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

    private SearchLocationResult searchLocation(ExternalModelAgentRuntime.RuntimePlan runtimePlan) {
        if (runtimePlan == null || runtimePlan.getSearchLocationResult() == null) {
            return SearchLocationResult.disabled();
        }
        return runtimePlan.getSearchLocationResult();
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
        if (PromptTemplateRegistry.DIAGNOSIS_REPORT_V2.equals(version)) {
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
            context.put("searchLocationSummary", runtimePlan.getStandardLibraryPack().getSearchLocationSummary());
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
        String cleaned = cleanupAiText(ref);
        if (cleaned.isBlank()) {
            return "";
        }
        String mapped = evidenceCandidateRefs == null ? null : evidenceCandidateRefs.get(cleaned);
        return mapped == null ? cleaned : mapped;
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
