package com.onlinejudge.submission.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlinejudge.learning.diagnosis.DiagnosisTaxonomy;
import com.onlinejudge.problem.domain.Problem;
import com.onlinejudge.submission.domain.Submission;
import com.onlinejudge.submission.dto.SubmissionAnalysisResponse;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

import static org.assertj.core.api.Assertions.assertThat;

class AiReportServiceExternalRuntimeTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void externalRuntimeCompletesTwoStagesAndMergesTeachingOutput() {
        StubAiReportService service = newService(
                """
                {
                  "primaryIssueTag": "LOOP_BOUNDARY",
                  "fineGrainedTag": "OFF_BY_ONE",
                  "evidenceRefs": ["code:range_excludes_n"],
                  "confidence": 0.91,
                  "uncertainty": "range(1, n) 没有覆盖 n，需要保留隐藏测试不确定性。",
                  "needsMoreEvidence": false,
                  "answerLeakRisk": "LOW"
                }
                """,
                """
                {
                  "studentHint": "先用 n=1 和 n=2 手推循环实际执行了哪些 i。",
                  "studentHintPlan": {
                    "hintLevel": "L2",
                    "problemType": "循环边界",
                    "evidenceAnchor": "code:range_excludes_n",
                    "nextAction": "列出 range 产生的每个 i，再和题目要求的 1 到 n 对齐。",
                    "coachQuestion": "当 n=1 时，循环体会执行几次？",
                    "teachingAction": "TRACE_VARIABLES",
                    "evidenceRefs": ["code:range_excludes_n"],
                    "answerLeakRisk": "LOW"
                  },
                  "learningInterventionPlan": {
                    "interventionType": "VARIABLE_TRACE",
                    "goal": "确认循环上下界是否覆盖题目要求。",
                    "studentTask": "写出 n=1、n=2 时 i 的取值表。",
                    "checkQuestion": "最后一次循环是否处理到了 n？",
                    "completionSignal": "学生能给出 i 的取值表并指出缺失位置。",
                    "evidenceRefs": ["code:range_excludes_n"],
                    "estimatedMinutes": 6,
                    "answerLeakRisk": "LOW"
                  },
                  "teacherNote": "学生需要把 range 的右边界与闭区间要求对应起来。",
                  "answerLeakRisk": "LOW"
                }
                """
        );

        SubmissionAnalysisResponse analysis = service.enhanceSubmissionAnalysis(
                problem(),
                submission(),
                fallback(),
                evidencePackage(),
                ruleSignals()
        );

        assertThat(analysis.getSourceType()).isEqualTo("MODEL_SCOPE_EXTERNAL_MODEL");
        assertThat(analysis.getIssueTags()).containsExactly("LOOP_BOUNDARY");
        assertThat(analysis.getFineGrainedTags()).containsExactly("OFF_BY_ONE");
        assertThat(analysis.getEvidenceRefs()).contains("code:range_excludes_n");
        assertThat(analysis.getStudentHint()).contains("n=1");
        assertThat(analysis.getStudentHintPlan().getTeachingAction()).isEqualTo("TRACE_VARIABLES");
        assertThat(analysis.getLearningInterventionPlan().getInterventionType()).isEqualTo("VARIABLE_TRACE");
        assertThat(analysis.getLineIssues()).hasSize(1);
        assertThat(analysis.getAiInvocation().getStatus()).isEqualTo("MODEL_COMPLETED");
        assertThat(analysis.getAiInvocation().isFallbackUsed()).isFalse();
        assertThat(analysis.getAiInvocation().getPromptVersion()).isEqualTo("diagnosis-judge-v1+teaching-hint-v1");
        assertThat(service.callCount()).isEqualTo(2);
    }

    @Test
    void externalRuntimeFallsBackWhenDiagnosisTagIsInvalid() {
        StubAiReportService service = newService(
                """
                {
                  "primaryIssueTag": "MADE_UP_TAG",
                  "fineGrainedTag": "OFF_BY_ONE",
                  "evidenceRefs": ["code:range_excludes_n"],
                  "confidence": 0.5,
                  "uncertainty": "bad tag",
                  "needsMoreEvidence": false,
                  "answerLeakRisk": "LOW"
                }
                """,
                "{}"
        );

        SubmissionAnalysisResponse analysis = service.enhanceSubmissionAnalysis(
                problem(),
                submission(),
                fallback(),
                evidencePackage(),
                ruleSignals()
        );

        assertThat(analysis.getSourceType()).isEqualTo("RULE_BASED_V1");
        assertThat(analysis.getAiInvocation().getStatus()).isEqualTo("MODEL_RUNTIME_FALLBACK");
        assertThat(analysis.getAiInvocation().isFallbackUsed()).isTrue();
        assertThat(analysis.getUncertainty()).contains("INVALID_TAG");
        assertThat(service.callCount()).isEqualTo(1);
    }

    @Test
    void externalRuntimeRetainsDiagnosisWhenTeachingCallFails() {
        StubAiReportService service = newService(
                """
                {
                  "primaryIssueTag": "LOOP_BOUNDARY",
                  "fineGrainedTag": "OFF_BY_ONE",
                  "evidenceRefs": ["code:range_excludes_n"],
                  "confidence": 0.91,
                  "uncertainty": "ok",
                  "needsMoreEvidence": false,
                  "answerLeakRisk": "LOW"
                }
                """
        );

        SubmissionAnalysisResponse analysis = service.enhanceSubmissionAnalysis(
                problem(),
                submission(),
                fallback(),
                evidencePackage(),
                ruleSignals()
        );

        assertThat(analysis.getSourceType()).isEqualTo("MODEL_SCOPE_EXTERNAL_MODEL");
        assertThat(analysis.getIssueTags()).containsExactly("LOOP_BOUNDARY");
        assertThat(analysis.getFineGrainedTags()).containsExactly("OFF_BY_ONE");
        assertThat(analysis.getEvidenceRefs()).contains("code:range_excludes_n");
        assertThat(analysis.getStudentHintPlan()).isNotNull();
        assertThat(analysis.getStudentHintPlan().getTeachingAction()).isEqualTo("TRACE_VARIABLES");
        assertThat(analysis.getStudentHintPlan().getAnswerLeakRisk()).isEqualTo("LOW");
        assertThat(analysis.getLearningInterventionPlan()).isNotNull();
        assertThat(analysis.getAiInvocation().getStatus()).isEqualTo("MODEL_PARTIAL_COMPLETED");
        assertThat(analysis.getAiInvocation().isFallbackUsed()).isFalse();
        assertThat(analysis.getUncertainty()).contains("TEACHING_HINT").contains("API_ERROR");
        assertThat(service.callCount()).isEqualTo(2);
    }

    @Test
    void externalRuntimeRetainsDiagnosisWhenTeachingOutputIsUnsafe() {
        StubAiReportService service = newService(
                """
                {
                  "primaryIssueTag": "LOOP_BOUNDARY",
                  "fineGrainedTag": "OFF_BY_ONE",
                  "evidenceRefs": ["code:range_excludes_n"],
                  "confidence": 0.91,
                  "uncertainty": "ok",
                  "needsMoreEvidence": false,
                  "answerLeakRisk": "LOW"
                }
                """,
                """
                {
                  "studentHint": "完整代码如下：def solve(): pass",
                  "studentHintPlan": {
                    "hintLevel": "L4",
                    "problemType": "循环边界",
                    "evidenceAnchor": "code:range_excludes_n",
                    "nextAction": "复制完整代码",
                    "coachQuestion": "无",
                    "teachingAction": "TRACE_VARIABLES",
                    "evidenceRefs": ["code:range_excludes_n"],
                    "answerLeakRisk": "HIGH"
                  },
                  "learningInterventionPlan": {
                    "interventionType": "VARIABLE_TRACE",
                    "goal": "直接给答案",
                    "studentTask": "复制答案",
                    "checkQuestion": "无",
                    "completionSignal": "复制完成",
                    "evidenceRefs": ["code:range_excludes_n"],
                    "estimatedMinutes": 1,
                    "answerLeakRisk": "HIGH"
                  },
                  "teacherNote": "unsafe",
                  "answerLeakRisk": "HIGH"
                }
                """
        );

        SubmissionAnalysisResponse analysis = service.enhanceSubmissionAnalysis(
                problem(),
                submission(),
                fallback(),
                evidencePackage(),
                ruleSignals()
        );

        assertThat(analysis.getSourceType()).isEqualTo("MODEL_SCOPE_EXTERNAL_MODEL");
        assertThat(analysis.getIssueTags()).containsExactly("LOOP_BOUNDARY");
        assertThat(analysis.getFineGrainedTags()).containsExactly("OFF_BY_ONE");
        assertThat(analysis.getStudentHint()).doesNotContain("def solve").doesNotContain("pass");
        assertThat(analysis.getStudentHintPlan()).isNotNull();
        assertThat(analysis.getStudentHintPlan().getHintLevel()).isEqualTo("L2");
        assertThat(analysis.getStudentHintPlan().getTeachingAction()).isEqualTo("TRACE_VARIABLES");
        assertThat(analysis.getStudentHintPlan().getAnswerLeakRisk()).isEqualTo("LOW");
        assertThat(analysis.getLearningInterventionPlan()).isNotNull();
        assertThat(analysis.getLearningInterventionPlan().getAnswerLeakRisk()).isEqualTo("LOW");
        assertThat(analysis.getAiInvocation().getStatus()).isEqualTo("MODEL_PARTIAL_COMPLETED");
        assertThat(analysis.getAiInvocation().isFallbackUsed()).isFalse();
        assertThat(analysis.getUncertainty()).contains("TEACHING_HINT").contains("SAFETY_RISK");
        assertThat(service.callCount()).isEqualTo(2);
    }

    @Test
    void disabledExternalRuntimeUsesLegacyLongPromptPath() {
        StubAiReportService service = newService(
                """
                {
                  "headline": "模型长 prompt 诊断",
                  "summary": "旧路径仍可作为配置回滚。",
                  "issueTags": ["LOOP_BOUNDARY"],
                  "fineGrainedTags": ["OFF_BY_ONE"],
                  "abilityPoints": ["循环边界"],
                  "focusPoints": ["range 右边界"],
                  "fixDirections": ["手推最小样例"],
                  "evidenceRefs": ["code:range_excludes_n"],
                  "studentHint": "旧路径提示",
                  "studentHintPlan": {
                    "hintLevel": "L2",
                    "problemType": "循环边界",
                    "evidenceAnchor": "code:range_excludes_n",
                    "nextAction": "手推 range",
                    "coachQuestion": "右边界包含吗？",
                    "teachingAction": "TRACE_VARIABLES",
                    "evidenceRefs": ["code:range_excludes_n"],
                    "answerLeakRisk": "LOW"
                  },
                  "learningInterventionPlan": {
                    "interventionType": "VARIABLE_TRACE",
                    "goal": "确认边界",
                    "studentTask": "列变量表",
                    "checkQuestion": "是否处理 n",
                    "completionSignal": "写出变量表",
                    "evidenceRefs": ["code:range_excludes_n"],
                    "estimatedMinutes": 5,
                    "answerLeakRisk": "LOW"
                  },
                  "teacherNote": "legacy",
                  "progressSignal": "current",
                  "confidence": 0.8,
                  "uncertainty": "仍需结合隐藏测试。",
                  "answerLeakRisk": "LOW",
                  "wrongSolution": null,
                  "correctSolution": null,
                  "lineIssues": [],
                  "reportMarkdown": "旧路径报告"
                }
                """
        );
        ReflectionTestUtils.setField(service, "externalRuntimeEnabled", false);

        SubmissionAnalysisResponse analysis = service.enhanceSubmissionAnalysis(
                problem(),
                submission(),
                fallback(),
                evidencePackage(),
                ruleSignals()
        );

        assertThat(analysis.getSourceType()).isEqualTo("MODEL_SCOPE_EXTERNAL_MODEL");
        assertThat(analysis.getAiInvocation().getStatus()).isEqualTo("MODEL_COMPLETED");
        assertThat(analysis.getAiInvocation().getPromptVersion()).isEqualTo("submission-diagnosis-prompt-v2");
        assertThat(service.callCount()).isEqualTo(1);
    }

    @Test
    void diagnosticAgentPreservesRuntimeFallbackStatus() {
        StubAiReportService service = newService(
                """
                {
                  "primaryIssueTag": "MADE_UP_TAG",
                  "fineGrainedTag": "OFF_BY_ONE",
                  "evidenceRefs": ["code:range_excludes_n"],
                  "confidence": 0.5,
                  "uncertainty": "bad tag",
                  "needsMoreEvidence": false,
                  "answerLeakRisk": "LOW"
                }
                """
        );
        DiagnosisTaxonomy taxonomy = new DiagnosisTaxonomy();
        DiagnosticAgentService diagnosticAgentService = new DiagnosticAgentService(
                new DiagnosisEvidencePackageBuilder(),
                new RuleSignalAnalyzer(),
                service,
                new com.onlinejudge.classroom.application.HintSafetyService(null, objectMapper, taxonomy),
                taxonomy
        );

        DiagnosticAgentService.AgentResult result = diagnosticAgentService.diagnose(
                problem(),
                submission(),
                List.of(),
                fallback(),
                com.onlinejudge.classroom.domain.Assignment.HintPolicy.L2
        );

        assertThat(result.analysis().getAiInvocation().isFallbackUsed()).isTrue();
        assertThat(result.analysis().getAiInvocation().getStatus()).isEqualTo("MODEL_RUNTIME_FALLBACK");
        assertThat(result.traceSummary()).contains("model=rule-fallback");
    }

    @Test
    void chatCompletionFallsBackToStreamingWhenNonStreamResponseHasNoChoices() throws Exception {
        StreamingFallbackAiReportService service = new StreamingFallbackAiReportService(objectMapper);
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "apiKey", "test-key");
        ReflectionTestUtils.setField(service, "baseUrl", "https://example.test/v1");
        ReflectionTestUtils.setField(service, "model", "test-model");
        ReflectionTestUtils.setField(service, "timeoutSeconds", 5L);
        ReflectionTestUtils.setField(service, "maxOutputTokens", 128);
        ReflectionTestUtils.setField(service, "streamEnabled", false);
        ReflectionTestUtils.setField(service, "streamFallbackEnabled", true);

        String content = service.chatCompletion("system", "user");

        assertThat(content).isEqualTo("{\"primaryIssueTag\":\"LOOP_BOUNDARY\"}");
        assertThat(service.streamFlags()).containsExactly(false, true);
    }

    @Test
    void chatCompletionRetriesRateLimitAndThenSucceeds() throws Exception {
        RetryingAiReportService service = new RetryingAiReportService(
                objectMapper,
                new IOException("AI API returned status 429: {\"error\":{\"message\":\"rate limit\"}}"),
                """
                {"choices":[{"message":{"content":"{\\"primaryIssueTag\\":\\"LOOP_BOUNDARY\\"}"}}]}
                """
        );
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "apiKey", "test-key");
        ReflectionTestUtils.setField(service, "baseUrl", "https://example.test/v1");
        ReflectionTestUtils.setField(service, "model", "test-model");
        ReflectionTestUtils.setField(service, "timeoutSeconds", 5L);
        ReflectionTestUtils.setField(service, "maxOutputTokens", 128);
        ReflectionTestUtils.setField(service, "streamEnabled", false);
        ReflectionTestUtils.setField(service, "streamFallbackEnabled", false);
        ReflectionTestUtils.setField(service, "retryMaxAttempts", 2);
        ReflectionTestUtils.setField(service, "retryBackoffMs", 0L);

        String content = service.chatCompletion("system", "user");

        assertThat(content).isEqualTo("{\"primaryIssueTag\":\"LOOP_BOUNDARY\"}");
        assertThat(service.callCount()).isEqualTo(2);
    }

    @Test
    void growthReportFallbackKeepsExternalFailureReason() {
        FailingGrowthReportAiReportService service = new FailingGrowthReportAiReportService(
                objectMapper,
                new IOException("AI API returned status 429: {\"error\":{\"code\":\"insufficient_quota\"}}")
        );
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "apiKey", "test-key");

        String markdown = service.enhanceGrowthReportMarkdown(problem(), List.of(), "# 成长报告");

        assertThat(markdown).contains("# 成长报告");
        assertThat(markdown).contains("AI_FAILURE").contains("GROWTH_REPORT").contains("INSUFFICIENT_QUOTA");
    }

    @Test
    void budgetGuardShortCircuitsAfterQuotaFailureAcrossSubmissionAndGrowthReport() {
        ExternalModelBudgetGuard budgetGuard = new ExternalModelBudgetGuard();
        StubAiReportService service = new StubAiReportService(
                objectMapper,
                runtime(),
                budgetGuard,
                new IOException("AI API returned status 429: {\"error\":{\"code\":\"insufficient_quota\"}}")
        );
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "apiKey", "test-key");
        ReflectionTestUtils.setField(service, "model", "test-model");
        ReflectionTestUtils.setField(service, "externalRuntimeEnabled", true);
        ReflectionTestUtils.setField(service, "maxOutputTokens", 900);

        SubmissionAnalysisResponse analysis = service.enhanceSubmissionAnalysis(
                problem(),
                submission(),
                fallback(),
                evidencePackage(),
                ruleSignals()
        );

        assertThat(analysis.getAiInvocation().getStatus()).isEqualTo("MODEL_RUNTIME_FALLBACK");
        assertThat(analysis.getUncertainty()).contains("INSUFFICIENT_QUOTA");
        assertThat(service.callCount()).isEqualTo(1);

        String markdown = service.enhanceGrowthReportMarkdown(problem(), List.of(), "# 成长报告");

        assertThat(markdown).contains("BUDGET_GUARD_OPEN");
        assertThat(service.callCount()).isEqualTo(1);
    }

    private StubAiReportService newService(String... responses) {
        StubAiReportService service = new StubAiReportService(objectMapper, runtime(), responses);
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "apiKey", "test-key");
        ReflectionTestUtils.setField(service, "model", "test-model");
        ReflectionTestUtils.setField(service, "externalRuntimeEnabled", true);
        ReflectionTestUtils.setField(service, "maxOutputTokens", 900);
        return service;
    }

    private ExternalModelAgentRuntime runtime() {
        DiagnosisTaxonomy taxonomy = new DiagnosisTaxonomy();
        return new ExternalModelAgentRuntime(
                new ModelDiagnosisBriefBuilder(),
                new StandardLibraryPackBuilder(taxonomy),
                new PromptTemplateRegistry(),
                new ModelOutputValidator()
        );
    }

    private Problem problem() {
        return Problem.builder()
                .id(1L)
                .title("求 1 到 n 的和")
                .description("输入正整数 n，输出 1 到 n 的整数和。")
                .timeLimit(1000)
                .memoryLimit(65536)
                .build();
    }

    private Submission submission() {
        return Submission.builder()
                .id(11L)
                .problemId(1L)
                .languageName("Python 3")
                .verdict(Submission.Verdict.WRONG_ANSWER)
                .sourceCode("""
                        n = int(input())
                        total = 0
                        for i in range(1, n):
                            total += i
                        print(total)
                        """)
                .build();
    }

    private DiagnosisEvidencePackage evidencePackage() {
        return DiagnosisEvidencePackage.builder()
                .schemaVersion(DiagnosisEvidencePackage.SCHEMA_VERSION)
                .problem(DiagnosisEvidencePackage.ProblemEvidence.builder()
                        .title("求 1 到 n 的和")
                        .description("输入正整数 n，输出 1 到 n 的整数和。")
                        .build())
                .submission(DiagnosisEvidencePackage.SubmissionEvidence.builder()
                        .id(11L)
                        .language("Python 3")
                        .verdict("WRONG_ANSWER")
                        .sourceCode(submission().getSourceCode())
                        .sourceCodeWithLineNumbers("""
                                1: n = int(input())
                                2: total = 0
                                3: for i in range(1, n):
                                4:     total += i
                                5: print(total)
                                """)
                        .sourceCodeLineCount(5)
                        .build())
                .judgeFacts(DiagnosisEvidencePackage.JudgeFacts.builder()
                        .hiddenFailureObserved(false)
                        .build())
                .build();
    }

    private RuleSignalAnalyzer.RuleSignalResult ruleSignals() {
        return RuleSignalAnalyzer.RuleSignalResult.builder()
                .candidateIssueTags(List.of("LOOP_BOUNDARY"))
                .candidateFineGrainedTags(List.of("OFF_BY_ONE"))
                .evidenceRefs(List.of("code:range_excludes_n"))
                .signals(List.of(RuleSignalAnalyzer.Signal.builder()
                        .evidenceRef("code:range_excludes_n")
                        .coarseTag("LOOP_BOUNDARY")
                        .fineTag("OFF_BY_ONE")
                        .confidence(0.9)
                        .message("range(1, n) excludes n")
                        .build()))
                .build();
    }

    private SubmissionAnalysisResponse fallback() {
        return SubmissionAnalysisResponse.builder()
                .submissionId(11L)
                .analysisSchemaVersion("diagnosis-v1")
                .evidenceSchemaVersion(DiagnosisEvidencePackage.SCHEMA_VERSION)
                .taxonomyVersion(DiagnosisTaxonomy.TAXONOMY_VERSION)
                .sourceType("RULE_BASED_V1")
                .scenario("WA")
                .headline("规则层初步诊断")
                .summary("规则层认为可能存在循环边界问题。")
                .issueTags(List.of("LOOP_BOUNDARY"))
                .fineGrainedTags(List.of("OFF_BY_ONE"))
                .abilityPoints(List.of("循环边界"))
                .focusPoints(List.of("range 右边界"))
                .fixDirections(List.of("手推最小样例"))
                .evidenceRefs(List.of("code:range_excludes_n"))
                .studentHint("先手推最小样例。")
                .studentHintPlan(SubmissionAnalysisResponse.StudentHintPlan.builder()
                        .hintLevel("L2")
                        .problemType("循环边界")
                        .evidenceAnchor("code:range_excludes_n")
                        .nextAction("手推 range。")
                        .coachQuestion("右边界包含吗？")
                        .teachingAction("TRACE_VARIABLES")
                        .evidenceRefs(List.of("code:range_excludes_n"))
                        .answerLeakRisk("LOW")
                        .build())
                .confidence(0.72)
                .uncertainty("规则层初步判断。")
                .answerLeakRisk("LOW")
                .lineIssues(List.of(SubmissionAnalysisResponse.LineIssue.builder()
                        .lineNumber(3)
                        .error("循环未覆盖 n")
                        .suggestion("核对 range 的右边界")
                        .build()))
                .build();
    }

    private static class StubAiReportService extends AiReportService {
        private final Queue<Object> responses = new ArrayDeque<>();
        private int callCount;

        StubAiReportService(ObjectMapper objectMapper,
                            ExternalModelAgentRuntime runtime,
                            Object... responses) {
            this(objectMapper, runtime, new ExternalModelBudgetGuard(), responses);
        }

        StubAiReportService(ObjectMapper objectMapper,
                            ExternalModelAgentRuntime runtime,
                            ExternalModelBudgetGuard budgetGuard,
                            Object... responses) {
            super(objectMapper, new AiCodeAssistSupport(), runtime, new ExternalModelFailureClassifier(), budgetGuard);
            this.responses.addAll(List.of(responses));
        }

        @Override
        protected String chatCompletion(String systemPrompt, String userPrompt) throws IOException {
            callCount++;
            Object response = responses.poll();
            if (response instanceof IOException exception) {
                recordBudgetFailureForTest(exception);
                throw exception;
            }
            if (response == null) {
                throw new IOException("No stub response configured.");
            }
            return response.toString();
        }

        int callCount() {
            return callCount;
        }
    }

    private static class StreamingFallbackAiReportService extends AiReportService {
        private final List<Boolean> streamFlags = new java.util.ArrayList<>();

        StreamingFallbackAiReportService(ObjectMapper objectMapper) {
            super(objectMapper, new AiCodeAssistSupport());
        }

        @Override
        protected String sendChatCompletionRequest(String requestBody, boolean stream) {
            streamFlags.add(stream);
            if (!stream) {
                return """
                        {"choices":null}
                        """;
            }
            return """
                    data: {"choices":[{"delta":{"reasoning_content":"先判断。","content":""}}]}

                    data: {"choices":[{"delta":{"reasoning_content":"","content":"{\\"primaryIssueTag\\":\\"LOOP_BOUNDARY\\"}"}}]}

                    data: [DONE]
                    """;
        }

        List<Boolean> streamFlags() {
            return streamFlags;
        }
    }

    private static class RetryingAiReportService extends AiReportService {
        private final Queue<Object> responses = new ArrayDeque<>();
        private int callCount;

        RetryingAiReportService(ObjectMapper objectMapper, Object... responses) {
            super(objectMapper, new AiCodeAssistSupport());
            this.responses.addAll(List.of(responses));
        }

        @Override
        protected String sendChatCompletionRequest(String requestBody, boolean stream) throws IOException {
            callCount++;
            Object response = responses.poll();
            if (response instanceof IOException exception) {
                throw exception;
            }
            if (response == null) {
                throw new IOException("No stub response configured.");
            }
            return response.toString();
        }

        int callCount() {
            return callCount;
        }
    }

    private static class FailingGrowthReportAiReportService extends AiReportService {
        private final IOException exception;

        FailingGrowthReportAiReportService(ObjectMapper objectMapper, IOException exception) {
            super(objectMapper, new AiCodeAssistSupport());
            this.exception = exception;
        }

        @Override
        protected String chatCompletion(String systemPrompt, String userPrompt) throws IOException {
            throw exception;
        }
    }
}
