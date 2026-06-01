package com.onlinejudge.classroom.application;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlinejudge.classroom.domain.Assignment;
import com.onlinejudge.learning.diagnosis.DiagnosisTaxonomy;
import com.onlinejudge.submission.application.ExternalModelBudgetGuard;
import com.onlinejudge.submission.application.ExternalModelFailureClassifier;
import com.onlinejudge.submission.application.ModelDiagnosisBrief;
import com.onlinejudge.submission.application.ModelStageFailureReason;
import com.onlinejudge.submission.application.StandardLibraryPack;
import com.onlinejudge.submission.application.StandardLibraryPackBuilder;
import com.onlinejudge.submission.domain.Submission;
import com.onlinejudge.submission.domain.SubmissionAnalysis;
import lombok.Builder;
import lombok.Data;
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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
@Slf4j
public class CoachAgentService {

    private final ObjectMapper objectMapper;
    private final DiagnosisTaxonomy diagnosisTaxonomy;
    private final ExternalModelFailureClassifier failureClassifier;
    private final ExternalModelBudgetGuard budgetGuard;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public CoachAgentService(ObjectMapper objectMapper, DiagnosisTaxonomy diagnosisTaxonomy) {
        this(objectMapper, diagnosisTaxonomy, new ExternalModelFailureClassifier(), new ExternalModelBudgetGuard());
    }

    @Autowired
    public CoachAgentService(ObjectMapper objectMapper,
                             DiagnosisTaxonomy diagnosisTaxonomy,
                             ExternalModelFailureClassifier failureClassifier,
                             ExternalModelBudgetGuard budgetGuard) {
        this.objectMapper = objectMapper;
        this.diagnosisTaxonomy = diagnosisTaxonomy;
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

    @Value("${ai.stream-enabled:true}")
    private boolean streamEnabled;

    @Value("${ai.stream-fallback-enabled:true}")
    private boolean streamFallbackEnabled;

    @Value("${ai.retry.max-attempts:2}")
    private int retryMaxAttempts;

    @Value("${ai.retry.backoff-ms:700}")
    private long retryBackoffMs;

    public CoachDraft generateInitialQuestion(Submission submission,
                                              SubmissionAnalysis analysis,
                                              String primaryTag,
                                              Assignment.HintPolicy hintPolicy,
                                              String contextSummary,
                                              List<String> evidenceRefs,
                                              CoachDraft fallback) {
        Map<String, Object> context = baseContext(submission, analysis, primaryTag, hintPolicy, contextSummary, evidenceRefs);
        context.put("turnType", "INITIAL_QUESTION");
        return generate(context, hintPolicy, evidenceRefs, fallback);
    }

    public CoachDraft generateFollowUpQuestion(Submission submission,
                                               SubmissionAnalysis analysis,
                                               String primaryTag,
                                               Assignment.HintPolicy hintPolicy,
                                               String contextSummary,
                                               List<String> evidenceRefs,
                                               String studentAnswer,
                                               Integer currentTurnIndex,
                                               CoachDraft fallback) {
        Map<String, Object> context = baseContext(submission, analysis, primaryTag, hintPolicy, contextSummary, evidenceRefs);
        context.put("turnType", "FOLLOW_UP");
        context.put("studentAnswer", studentAnswer == null ? "" : studentAnswer);
        context.put("currentTurnIndex", currentTurnIndex == null ? 0 : currentTurnIndex);
        return generate(context, hintPolicy, evidenceRefs, fallback);
    }

    private CoachDraft generate(Map<String, Object> context,
                                Assignment.HintPolicy hintPolicy,
                                List<String> allowedEvidenceRefs,
                                CoachDraft fallback) {
        CoachDraft safeFallback = fallback == null ? CoachDraft.fallback("请先补一个最小样例，说明你准备验证什么。") : fallback;
        if (!canCallAi()) {
            return safeFallback;
        }
        ExternalModelBudgetGuard.Decision decision = budgetGuard.check("ModelScope", model);
        if (!decision.allowed()) {
            safeFallback.setFailureReason(ModelStageFailureReason.BUDGET_GUARD_OPEN.name());
            return safeFallback;
        }
        try {
            String raw = chatCompletion(systemPrompt(), "请基于以下上下文生成 JSON：" + objectMapper.writeValueAsString(context));
            CoachPayload payload = parsePayload(raw);
            CoachDraft draft = toDraft(payload, allowedEvidenceRefs, safeFallback);
            if (!isSafe(draft, hintPolicy, allowedEvidenceRefs)) {
                log.warn("Coach model draft rejected by safety gate. answerLeakRisk={}, questionPreview={}",
                        draft.getAnswerLeakRisk(),
                        preview(draft.getQuestion()));
                safeFallback.setFailureReason("SAFETY_REJECTED");
                return safeFallback;
            }
            return draft;
        } catch (Exception exception) {
            log.warn("Coach model generation failed. fallback=rule question. reason={}", exception.getMessage());
            safeFallback.setFailureReason(classifyFailure(exception));
            return safeFallback;
        }
    }

    private Map<String, Object> baseContext(Submission submission,
                                            SubmissionAnalysis analysis,
                                            String primaryTag,
                                            Assignment.HintPolicy hintPolicy,
                                            String contextSummary,
                                            List<String> evidenceRefs) {
        Map<String, Object> context = new LinkedHashMap<>();
        String normalizedPrimaryTag = normalizePrimaryTag(primaryTag);
        List<String> safeEvidenceRefs = evidenceRefs == null ? List.of() : evidenceRefs;
        context.put("submissionId", submission == null ? null : submission.getId());
        context.put("verdict", submission == null || submission.getVerdict() == null ? "UNKNOWN" : submission.getVerdict().name());
        context.put("scenario", analysis == null ? "" : analysis.getScenario());
        context.put("headline", analysis == null ? "" : analysis.getHeadline());
        context.put("primaryTag", normalizedPrimaryTag);
        context.put("primaryTagLabel", diagnosisTaxonomy.label(normalizedPrimaryTag));
        context.put("teachingAction", diagnosisTaxonomy.teachingAction(normalizedPrimaryTag));
        context.put("standardLibrary", buildCoachStandardLibrary(normalizedPrimaryTag));
        context.put("allowedEvidenceRefs", safeEvidenceRefs);
        context.put("hintPolicy", hintPolicy == null ? Assignment.HintPolicy.L2.name() : hintPolicy.name());
        context.put("contextSummary", contextSummary == null ? "" : contextSummary);
        context.put("evidenceRefs", safeEvidenceRefs);
        return context;
    }

    private String systemPrompt() {
        return """
                你是中文 OJ AI 教练，只生成苏格拉底式追问。
                必须返回严格 JSON，不要 Markdown 代码块，不要解释，不要思考过程。

                JSON 字段：
                question(string),
                rationale(string),
                evidenceRefs(string[]),
                confidence(number 0-1),
                answerLeakRisk("LOW"|"MEDIUM"|"HIGH")

                安全规则：
                1. 只能基于输入 standardLibrary、teachingAction、allowedEvidenceRefs 和 contextSummary 追问学生下一步如何验证。
                2. 不给完整答案、完整代码、最终算法步骤或隐藏测试数据。
                3. 不要直接说“把 X 改成 Y”，不要提供可照抄的修复。
                4. 必须引用 allowedEvidenceRefs 中已有的证据 ID，不要编造证据。
                5. 如果 contextSummary 或 allowedEvidenceRefs 包含 coach-strategy:*，必须优先执行该自适应策略，再沿用 standardLibrary.teachingActions 中与 primaryTag 对应的教学动作。
                6. REDUCE_GRANULARITY 要缩到最小失败样例、关键变量轨迹或单行输入输出对照；COLLECT_EVIDENCE 要补证据；VERIFY_MINIMAL_CHANGE 要预测一次最小修改后的评测现象；TRANSFER_REFLECTION 要做通过后迁移复盘；SAFETY_RESET 要把对话从答案/完整代码拉回证据层。
                7. 所有面向学生的文本必须简体中文，语气短、具体、可执行。
                8. 如果你不确定是否安全，将 answerLeakRisk 设为 HIGH。
                """;
    }

    private String normalizePrimaryTag(String primaryTag) {
        DiagnosisTaxonomy.DiagnosisTag tag = diagnosisTaxonomy.get(primaryTag);
        return tag == null ? "NEEDS_MORE_EVIDENCE" : tag.getId();
    }

    private StandardLibraryPack buildCoachStandardLibrary(String primaryTag) {
        DiagnosisTaxonomy.DiagnosisTag tag = diagnosisTaxonomy.get(primaryTag);
        LinkedHashSet<String> issueTags = new LinkedHashSet<>();
        LinkedHashSet<String> fineTags = new LinkedHashSet<>();
        if (tag == null) {
            issueTags.add("NEEDS_MORE_EVIDENCE");
        } else if (tag.isFineGrained()) {
            fineTags.add(tag.getId());
            if (tag.getParentTag() != null && !tag.getParentTag().isBlank()) {
                issueTags.add(tag.getParentTag());
            }
        } else {
            issueTags.add(tag.getId());
        }
        issueTags.add("NEEDS_MORE_EVIDENCE");
        ModelDiagnosisBrief brief = ModelDiagnosisBrief.builder()
                .allowedIssueTags(List.copyOf(issueTags))
                .allowedFineGrainedTags(List.copyOf(fineTags))
                .build();
        return new StandardLibraryPackBuilder(diagnosisTaxonomy).build(brief, null);
    }

    protected String chatCompletion(String systemPrompt, String userPrompt) throws IOException, InterruptedException {
        try {
            String content = doChatCompletionWithRetry(systemPrompt, userPrompt, streamEnabled);
            budgetGuard.recordSuccess("ModelScope", model);
            return content;
        } catch (IOException exception) {
            if (!streamEnabled && streamFallbackEnabled && shouldRetryWithStreaming(exception)) {
                log.warn("Retrying coach chat completion with stream=true after non-stream response was unusable. model={}", model);
                String content = doChatCompletionWithRetry(systemPrompt, userPrompt, true);
                budgetGuard.recordSuccess("ModelScope", model);
                return content;
            }
            recordBudgetFailure(exception);
            throw exception;
        }
    }

    private String doChatCompletionWithRetry(String systemPrompt, String userPrompt, boolean stream)
            throws IOException, InterruptedException {
        int attempts = Math.max(1, retryMaxAttempts);
        IOException lastException = null;
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                return doChatCompletion(systemPrompt, userPrompt, stream);
            } catch (IOException exception) {
                lastException = exception;
                if (attempt >= attempts || !isRetryableCallFailure(exception)) {
                    throw exception;
                }
                long backoff = Math.max(0, retryBackoffMs) * attempt;
                log.warn("Retrying coach chat completion after transient failure. model={}, attempt={}/{}, waitMs={}, reason={}",
                        model,
                        attempt + 1,
                        attempts,
                        backoff,
                        preview(exception.getMessage()));
                sleepBeforeRetry(backoff);
            }
        }
        throw lastException == null ? new IOException("AI API call failed") : lastException;
    }

    private String doChatCompletion(String systemPrompt, String userPrompt, boolean stream)
            throws IOException, InterruptedException {
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "temperature", 0.2,
                "stream", stream
        );
        String endpoint = baseUrl.endsWith("/") ? baseUrl + "chat/completions" : baseUrl + "/chat/completions";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(Math.max(timeoutSeconds, 5)))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody), StandardCharsets.UTF_8))
                .build();

        return sendChatCompletionRequest(request, stream);
    }

    protected String sendChatCompletionRequest(HttpRequest request, boolean stream) throws IOException, InterruptedException {
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("AI API returned status " + response.statusCode() + ": " + response.body());
        }
        String content = stream
                ? extractStreamingChatMessageContent(response.body())
                : extractChatMessageContent(objectMapper.readTree(response.body()));
        if (content.isBlank()) {
            throw new IOException("AI response did not include message content");
        }
        return content;
    }

    private boolean shouldRetryWithStreaming(IOException exception) {
        String message = exception == null || exception.getMessage() == null ? "" : exception.getMessage();
        return message.contains("did not include message content");
    }

    private boolean isRetryableCallFailure(IOException exception) {
        String text = exception == null ? "" : exception.getMessage();
        return failureClassifier.isRetryable(failureClassifier.classify(exception), text);
    }

    private void recordBudgetFailure(Exception exception) {
        ModelStageFailureReason reason = failureClassifier.classify(exception);
        if (failureClassifier.shouldOpenBudgetGuard(reason)) {
            budgetGuard.recordFailure("ModelScope", model, reason);
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
        String messageContent = extractContentNode(firstChoice.path("message").path("content"));
        if (!messageContent.isBlank()) {
            return messageContent;
        }
        return extractContentNode(firstChoice.path("delta").path("content"));
    }

    private String extractStreamingChatMessageContent(String responseBody) throws IOException {
        if (responseBody == null || responseBody.isBlank()) {
            return "";
        }
        StringBuilder content = new StringBuilder();
        String[] lines = responseBody.replace("\r\n", "\n").replace('\r', '\n').split("\n");
        for (String line : lines) {
            String trimmed = line == null ? "" : line.trim();
            if (trimmed.isBlank()) {
                continue;
            }
            String payload = trimmed.startsWith("data:") ? trimmed.substring(5).trim() : trimmed;
            if (payload.isBlank() || "[DONE]".equals(payload) || !payload.startsWith("{")) {
                continue;
            }
            JsonNode root;
            try {
                root = objectMapper.readTree(payload);
            } catch (JsonProcessingException exception) {
                continue;
            }
            String chunk = extractChatMessageContent(root);
            if (!chunk.isBlank()) {
                content.append(chunk);
            }
        }
        return content.toString();
    }

    private String extractContentNode(JsonNode contentNode) {
        if (contentNode == null || contentNode.isMissingNode() || contentNode.isNull()) {
            return "";
        }
        return contentNode.isTextual() ? contentNode.asText() : contentNode.toString();
    }

    private CoachPayload parsePayload(String raw) {
        String normalized = cleanup(raw);
        try {
            return objectMapper.readValue(normalized, CoachPayload.class);
        } catch (JsonProcessingException firstError) {
            int start = normalized.indexOf('{');
            int end = normalized.lastIndexOf('}');
            if (start >= 0 && end > start) {
                try {
                    return objectMapper.readValue(normalized.substring(start, end + 1), CoachPayload.class);
                } catch (JsonProcessingException ignored) {
                    return null;
                }
            }
            return null;
        }
    }

    private CoachDraft toDraft(CoachPayload payload,
                               List<String> allowedEvidenceRefs,
                               CoachDraft fallback) {
        if (payload == null || payload.question == null || payload.question.isBlank()) {
            return fallback;
        }
        return CoachDraft.builder()
                .question(cleanup(payload.question))
                .rationale(cleanup(payload.rationale))
                .evidenceRefs(normalizeEvidenceRefs(payload.evidenceRefs, allowedEvidenceRefs))
                .confidence(payload.confidence == null ? 0.5 : Math.max(0, Math.min(1, payload.confidence)))
                .answerLeakRisk(normalizeLeakRisk(payload.answerLeakRisk))
                .source("MODEL")
                .build();
    }

    private List<String> normalizeEvidenceRefs(List<String> candidate, List<String> allowed) {
        LinkedHashSet<String> allowedSet = new LinkedHashSet<>(allowed == null ? List.of() : allowed);
        if (allowedSet.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> refs = new LinkedHashSet<>();
        if (candidate != null) {
            candidate.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(allowedSet::contains)
                    .forEach(refs::add);
        }
        return List.copyOf(refs);
    }

    private boolean isSafe(CoachDraft draft,
                           Assignment.HintPolicy hintPolicy,
                           List<String> allowedEvidenceRefs) {
        if (draft == null || draft.getQuestion() == null || draft.getQuestion().isBlank()) {
            return false;
        }
        if ("HIGH".equalsIgnoreCase(draft.getAnswerLeakRisk())) {
            return false;
        }
        Assignment.HintPolicy effectivePolicy = hintPolicy == null ? Assignment.HintPolicy.L2 : hintPolicy;
        if (diagnosisTaxonomy.isBeyondPolicy(draft.getQuestion(), effectivePolicy)) {
            return false;
        }
        if (containsLeakLikeText(draft.getQuestion()) || containsLeakLikeText(draft.getRationale())) {
            return false;
        }
        return allowedEvidenceRefs == null || allowedEvidenceRefs.isEmpty() || !draft.getEvidenceRefs().isEmpty();
    }

    private boolean containsLeakLikeText(String text) {
        String normalized = text == null ? "" : text.toLowerCase(Locale.ROOT);
        return normalized.contains("完整代码")
                || normalized.contains("参考代码")
                || normalized.contains("答案如下")
                || normalized.contains("直接改成")
                || normalized.contains("#include")
                || normalized.contains("int main")
                || normalized.contains("def ")
                || normalized.contains("```");
    }

    private String normalizeLeakRisk(String risk) {
        String normalized = cleanup(risk).toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "LOW", "MEDIUM", "HIGH" -> normalized;
            default -> "UNKNOWN";
        };
    }

    private String classifyFailure(Exception exception) {
        return failureClassifier.classify(exception).name();
    }

    private boolean canCallAi() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }

    private String cleanup(String text) {
        if (text == null) {
            return "";
        }
        String normalized = text.trim()
                .replaceAll("(?is)<think>.*?</think>", "")
                .replace("<think>", "")
                .replace("</think>", "")
                .trim();
        if (normalized.startsWith("```")) {
            normalized = normalized.replaceFirst("^```[a-zA-Z0-9_-]*\\s*", "");
            normalized = normalized.replaceFirst("\\s*```$", "");
        }
        return normalized.trim();
    }

    private String preview(String text) {
        String normalized = text == null ? "" : text.replace("\r", "").replace("\n", "\\n").trim();
        return normalized.length() <= 120 ? normalized : normalized.substring(0, 117) + "...";
    }

    @Data
    @Builder
    public static class CoachDraft {
        private String question;
        private String rationale;
        private List<String> evidenceRefs;
        private Double confidence;
        private String answerLeakRisk;
        private String source;
        private String failureReason;

        public static CoachDraft fallback(String question) {
            return CoachDraft.builder()
                    .question(question)
                    .rationale("规则追问回退。")
                    .evidenceRefs(List.of())
                    .confidence(1.0)
                    .answerLeakRisk("LOW")
                    .source("RULE")
                    .failureReason("")
                    .build();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class CoachPayload {
        public String question;
        public String rationale;
        public List<String> evidenceRefs;
        public Double confidence;
        public String answerLeakRisk;
    }
}
