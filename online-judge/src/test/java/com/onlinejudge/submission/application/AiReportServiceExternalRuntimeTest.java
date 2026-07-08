package com.onlinejudge.submission.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlinejudge.learning.diagnosis.DiagnosisTaxonomy;
import com.onlinejudge.learning.knowledge.persistence.InformaticsKnowledgeNodeRepository;
import com.onlinejudge.learning.standardlibrary.application.AiStandardLibraryService;
import com.onlinejudge.learning.standardlibrary.domain.AiStandardLibraryItem;
import com.onlinejudge.learning.standardlibrary.domain.AiStandardLibraryLayer;
import com.onlinejudge.learning.standardlibrary.dto.AiStandardLibraryNavigationNodeResponse;
import com.onlinejudge.problem.domain.Problem;
import com.onlinejudge.submission.domain.Submission;
import com.onlinejudge.submission.dto.SubmissionAnalysisResponse;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiReportServiceExternalRuntimeTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void externalRuntimeCompletesAdviceGenerationWithStandardLibraryNavigation() {
        StubAiReportService service = newService(validAdviceResponse());

        SubmissionAnalysisResponse analysis = service.enhanceSubmissionAnalysis(
                problem(),
                submission(),
                fallback(),
                evidencePackage()
        );

        assertThat(service.callCount()).isEqualTo(3);
        assertThat(service.lastSystemPrompt()).contains("diagnosis-report-v3");
        assertThat(service.lastSystemPrompt())
                .contains("diagnosisDecision")
                .doesNotContain("teachingHint");
        assertThat(service.lastUserPrompt())
                .contains("task", "contextPolicy", "brief", "standardLibrary")
                .contains("标准库")
                .contains("不是强制答案")
                .contains("OUT_OF_LIBRARY");
        assertThat(analysis.getSourceType()).isEqualTo("MODEL_SCOPE_EXTERNAL_MODEL");
        assertThat(analysis.getAiInvocation().getStatus()).isEqualTo("MODEL_COMPLETED");
        assertThat(analysis.getAiInvocation().isFallbackUsed()).isFalse();
        assertThat(analysis.getAiInvocation().getPromptVersion()).isEqualTo(PromptTemplateRegistry.DIAGNOSIS_REPORT_V3);
        assertThat(analysis.getAiInvocation().getRuntimeMode()).isEqualTo("diagnosis-report");
        assertThat(analysis.getAiInvocation().getFailureStage()).isEmpty();
        assertThat(analysis.getAiInvocation().getAdviceGenerationStatus()).isEqualTo("SUCCESS");
        assertThat(analysis.getBasicLayerAdvice()).singleElement()
                .satisfies(item -> assertThat(item.getMistakePointId()).isEqualTo("OFF_BY_ONE"));
        assertThat(analysis.getStudentFeedback().getBlockingIssues()).singleElement()
                .satisfies(issue -> assertThat(issue.getTitle()).contains("循环右边界"));
        assertThat(analysis.getReportMarkdown()).contains("## AI 完整诊断与建议", "### 基础层", "### 提高层");
    }

    @Test
    void adviceValidationFailureFallsBackToRuleResult() {
        StubAiReportService service = newService("""
                {
                  "caseUnderstanding": {
                    "problemGoal": "输出 1 到 n 的整数和。",
                    "codeIntent": "学生想用循环累加。",
                    "behaviorGap": "循环没有覆盖末端。",
                    "primaryEvidenceRef": "invented:evidence"
                  },
                  "basicLayerAdvice": [{
                    "mistakePointId": "OFF_BY_ONE",
                    "skillUnitId": null,
                    "title": "循环右边界漏取",
                    "whatHappened": "当前循环范围没有覆盖题目要求。",
                    "whyItMatters": "结果会偏小。",
                    "studentAction": "先手推最小样例。",
                    "checkQuestion": "末端有没有进入循环？",
                    "evidenceRefs": ["invented:evidence"],
                    "confidence": 0.9
                  }],
                  "improvementLayerAdvice": [],
                  "nextStepPlan": [{
                    "step": 1,
                    "target": "手推循环变量取值。",
                    "reason": "这是当前阻塞通过的问题。",
                    "evidenceRef": "invented:evidence"
                  }],
                  "studentSummary": "边界没有对齐。"
                }
                """);

        SubmissionAnalysisResponse analysis = service.enhanceSubmissionAnalysis(
                problem(),
                submission(),
                fallback(),
                evidencePackage()
        );

        assertThat(analysis.getSourceType()).isEqualTo("MODEL_SCOPE_EXTERNAL_MODEL");
        assertThat(analysis.getAiInvocation().getStatus()).isEqualTo("MODEL_FAILED");
        assertThat(analysis.getAiInvocation().isFallbackUsed()).isFalse();
        assertThat(analysis.getAiInvocation().getFailureStage()).isEqualTo("DIAGNOSIS_AND_ADVICE");
        assertThat(analysis.getAiInvocation().getFailureReason()).contains("INVALID_EVIDENCE_REF");
        assertThat(analysis.getAiInvocation().getAdviceGenerationStatus()).isEqualTo("FAILED");
        assertThat(analysis.getIssueTags()).isEmpty();
        assertThat(analysis.getFineGrainedTags()).isEmpty();
        assertThat(analysis.getLineIssues()).isEmpty();
        assertThat(analysis.getUncertainty()).contains("未使用本地规则兜底");
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
        configure(service);

        SubmissionAnalysisResponse analysis = service.enhanceSubmissionAnalysis(
                problem(),
                submission(),
                fallback(),
                evidencePackage()
        );

        assertThat(analysis.getSourceType()).isEqualTo("MODEL_SCOPE_EXTERNAL_MODEL");
        assertThat(analysis.getAiInvocation().getStatus()).isEqualTo("MODEL_FAILED");
        assertThat(analysis.getAiInvocation().isFallbackUsed()).isFalse();
        assertThat(analysis.getAiInvocation().getRuntimeMode()).isEqualTo("diagnosis-report");
        assertThat(analysis.getAiInvocation().getFailureStage()).isEqualTo("AI_NAVIGATION");
        assertThat(analysis.getAiInvocation().getFailureReason()).isEqualTo("INSUFFICIENT_QUOTA");
        assertThat(analysis.getUncertainty()).contains("INSUFFICIENT_QUOTA");
        assertThat(analysis.getIssueTags()).isEmpty();
        assertThat(service.callCount()).isEqualTo(1);

        String markdown = service.enhanceGrowthReportMarkdown(problem(), List.of());

        assertThat(markdown).contains("AI 成长报告", "暂不可用", "BUDGET_GUARD_OPEN", "未使用本地报告兜底");
        assertThat(service.callCount()).isEqualTo(1);
    }

    @Test
    void compatibleRequestMergesSystemPromptForModelScope() throws Exception {
        CapturingAiReportService service = new CapturingAiReportService(
                objectMapper,
                apiResponse(freeDiagnosisResponse()),
                apiResponse(navigationDoneResponse()),
                apiResponse(validAdviceResponse())
        );
        configure(service);
        ReflectionTestUtils.setField(service, "streamEnabled", false);
        ReflectionTestUtils.setField(service, "modelScopeCompatibleRequest", "true");

        SubmissionAnalysisResponse analysis = service.enhanceSubmissionAnalysis(
                problem(),
                submission(),
                fallback(),
                evidencePackage()
        );

        assertThat(analysis.getAiInvocation().getStatus()).isEqualTo("MODEL_COMPLETED");
        assertThat(service.lastRequestBody()).contains("diagnosis-report-v3");
        assertThat(service.lastRequestBody()).contains("\"role\":\"user\"");
        assertThat(service.lastRequestBody()).doesNotContain("\"role\":\"system\"");
    }

    private StubAiReportService newService(Object... responses) {
        StubAiReportService service = new StubAiReportService(objectMapper, runtime(), withNavigationResponses(responses));
        configure(service);
        return service;
    }

    private void configure(AiReportService service) {
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "apiKey", "test-key");
        ReflectionTestUtils.setField(service, "baseUrl", "https://api-inference.modelscope.cn/v1");
        ReflectionTestUtils.setField(service, "model", "test-model");
        ReflectionTestUtils.setField(service, "externalRuntimeEnabled", true);
        ReflectionTestUtils.setField(service, "externalRuntimeProfile", ExternalModelAgentRuntime.RUNTIME_PROFILE_STANDARD);
        ReflectionTestUtils.setField(service, "maxOutputTokens", 1200);
        ReflectionTestUtils.setField(service, "retryMaxAttempts", 1);
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
                        .firstFailedCase(SubmissionAnalysisResponse.FailedCaseSnapshot.builder()
                                .testCaseNumber(1)
                                .hidden(false)
                                .input("1")
                                .expectedOutput("1")
                                .actualOutput("0")
                                .build())
                        .build())
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

    private Object[] withNavigationResponses(Object... adviceResponses) {
        List<Object> responses = new ArrayList<>();
        responses.add(freeDiagnosisResponse());
        responses.add(navigationDoneResponse());
        responses.addAll(List.of(adviceResponses));
        return responses.toArray(Object[]::new);
    }

    private String apiResponse(String content) {
        return """
                {
                  "choices": [{
                    "message": {
                      "content": %s
                    },
                    "finish_reason": "stop"
                  }]
                }
                """.formatted(objectMapper.valueToTree(content).toString());
    }

    private String freeDiagnosisResponse() {
        return """
                {
                  "problemUnderstanding": "题目要求输出 1 到 n 的整数和。",
                  "codeIntent": "学生想用循环累加 total。",
                  "behaviorGap": "循环没有覆盖题目要求的末端。",
                  "hypotheses": [{
                    "name": "循环右边界漏取",
                    "reason": "range(1, n) 与闭区间题意不一致。",
                    "evidenceRefs": ["code:range_excludes_n"],
                    "confidence": 0.9
                  }],
                  "navigationIntent": {
                    "preferredDirections": ["基础语法", "循环边界"],
                    "reason": "当前错因首先落在循环边界。"
                  }
                }
                """;
    }

    private String navigationDoneResponse() {
        return """
                {
                  "status": "DONE",
                  "selectedPaths": [{
                    "knowledgeNodeCode": "BASIC.LOOP.BOUNDARY",
                    "mistakePointCode": "OFF_BY_ONE",
                    "improvementPointCode": "TESTING_HABIT",
                    "libraryFit": "HIT",
                    "reason": "代码中的 range(1, n) 与题目闭区间要求不一致。",
                    "evidenceRefs": ["code:range_excludes_n"],
                    "confidence": 0.9
                  }],
                  "unresolvedGaps": []
                }
                """;
    }

    private String validAdviceResponse() {
        return """
                {
                  "caseUnderstanding": {
                    "problemGoal": "题目要求输出 1 到 n 的整数和。",
                    "codeIntent": "学生使用循环累加 total。",
                    "behaviorGap": "循环实际没有覆盖题目要求的末端。",
                    "primaryEvidenceRef": "code:range_excludes_n"
                  },
                  "basicLayerAdvice": [{
                    "mistakePointId": "OFF_BY_ONE",
                    "skillUnitId": null,
                    "title": "循环右边界漏取",
                    "whatHappened": "当前循环范围没有覆盖题目要求的最后一个数。",
                    "whyItMatters": "少处理一个端点会让求和结果偏小。",
                    "studentAction": "先手推 n=1 和 n=2 时循环变量实际出现过哪些值。",
                    "checkQuestion": "最后一个应该被处理的数有没有进入循环？",
                    "evidenceRefs": ["code:range_excludes_n"],
                    "confidence": 0.92
                  }],
                  "improvementLayerAdvice": [{
                    "improvementPointId": "TESTING_HABIT",
                    "skillUnitId": null,
                    "title": "补充边界样例意识",
                    "currentLimit": "这类问题不是算法方向错，而是边界验证不足。",
                    "suggestion": "修复后补测最小值、端点值和最大值附近样例。",
                    "studentBenefit": "能更早发现开闭区间和下标边界问题。",
                    "evidenceRefs": ["code:range_excludes_n"],
                    "confidence": 0.8
                  }],
                  "nextStepPlan": [{
                    "step": 1,
                    "target": "手推循环变量取值。",
                    "reason": "这是当前阻塞通过的主要问题。",
                    "evidenceRef": "code:range_excludes_n"
                  }],
                  "studentSummary": "这次主要卡在循环边界和题目要求范围没有对齐。"
                }
                """;
    }

    private static class StubAiReportService extends AiReportService {
        private final Queue<Object> responses = new ArrayDeque<>();
        private final List<String> systemPrompts = new ArrayList<>();
        private final List<String> userPrompts = new ArrayList<>();
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
            this(objectMapper, runtime, budgetGuard, navigationDeps(), responses);
        }

        private StubAiReportService(ObjectMapper objectMapper,
                                    ExternalModelAgentRuntime runtime,
                                    ExternalModelBudgetGuard budgetGuard,
                                    NavigationDeps navigationDeps,
                                    Object... responses) {
            super(objectMapper,
                    new AiCodeAssistSupport(),
                    runtime,
                    new ExternalModelFailureClassifier(),
                    budgetGuard,
                    new ExternalModelChatRequestFactory(),
                    null,
                    navigationDeps.standardLibraryService(),
                    new StandardLibraryNavigationOutputValidator(),
                    new StandardLibraryNavigationPackBuilder(
                            navigationDeps.standardLibraryService(),
                            navigationDeps.knowledgeRepository()));
            this.responses.addAll(List.of(responses));
        }

        @Override
        protected String chatCompletion(String systemPrompt, String userPrompt) throws IOException {
            callCount++;
            systemPrompts.add(systemPrompt);
            userPrompts.add(userPrompt);
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

        String lastSystemPrompt() {
            return systemPrompts.isEmpty() ? "" : systemPrompts.get(systemPrompts.size() - 1);
        }

        String lastUserPrompt() {
            return userPrompts.isEmpty() ? "" : userPrompts.get(userPrompts.size() - 1);
        }
    }

    private static class CapturingAiReportService extends AiReportService {
        private final Queue<String> responses = new ArrayDeque<>();
        private final List<String> requestBodies = new ArrayList<>();

        CapturingAiReportService(ObjectMapper objectMapper, String... responses) {
            this(objectMapper, navigationDeps(), responses);
        }

        private CapturingAiReportService(ObjectMapper objectMapper,
                                         NavigationDeps navigationDeps,
                                         String... responses) {
            super(objectMapper,
                    new AiCodeAssistSupport(),
                    new ExternalModelAgentRuntime(
                            new ModelDiagnosisBriefBuilder(),
                            new StandardLibraryPackBuilder(new DiagnosisTaxonomy()),
                            new PromptTemplateRegistry(),
                            new ModelOutputValidator()
                    ),
                    new ExternalModelFailureClassifier(),
                    new ExternalModelBudgetGuard(),
                    new ExternalModelChatRequestFactory(),
                    null,
                    navigationDeps.standardLibraryService(),
                    new StandardLibraryNavigationOutputValidator(),
                    new StandardLibraryNavigationPackBuilder(
                            navigationDeps.standardLibraryService(),
                            navigationDeps.knowledgeRepository()));
            this.responses.addAll(List.of(responses));
        }

        @Override
        protected String sendChatCompletionRequest(String requestBody, boolean stream) throws IOException {
            requestBodies.add(requestBody);
            String response = responses.poll();
            if (response == null) {
                throw new IOException("No stub response configured.");
            }
            return response;
        }

        String lastRequestBody() {
            return requestBodies.isEmpty() ? "" : requestBodies.get(requestBodies.size() - 1);
        }
    }

    private record NavigationDeps(AiStandardLibraryService standardLibraryService,
                                  InformaticsKnowledgeNodeRepository knowledgeRepository) {
    }

    private static NavigationDeps navigationDeps() {
        AiStandardLibraryService libraryService = mock(AiStandardLibraryService.class);
        when(libraryService.listRootKnowledgeAreas()).thenReturn(List.of(
                AiStandardLibraryNavigationNodeResponse.builder()
                        .code("BASIC")
                        .type("DOMAIN")
                        .name("基础语法")
                        .path("基础语法")
                        .hasChildren(true)
                        .build()
        ));
        when(libraryService.findFormalItemAsLegacy(AiStandardLibraryLayer.MISTAKE_POINT, "OFF_BY_ONE"))
                .thenReturn(Optional.of(item("OFF_BY_ONE", AiStandardLibraryLayer.MISTAKE_POINT)));
        when(libraryService.findFormalItemAsLegacy(AiStandardLibraryLayer.IMPROVEMENT_POINT, "TESTING_HABIT"))
                .thenReturn(Optional.of(item("TESTING_HABIT", AiStandardLibraryLayer.IMPROVEMENT_POINT)));
        InformaticsKnowledgeNodeRepository repository = mock(InformaticsKnowledgeNodeRepository.class);
        when(repository.findByCode("BASIC.LOOP.BOUNDARY")).thenReturn(Optional.empty());
        return new NavigationDeps(libraryService, repository);
    }

    private static AiStandardLibraryItem item(String code, AiStandardLibraryLayer layer) {
        return AiStandardLibraryItem.builder()
                .layer(layer)
                .code(code)
                .category("循环边界")
                .name(code)
                .description("循环边界诊断项。")
                .studentExplanation("循环边界诊断项。")
                .teacherExplanation("循环边界诊断项。")
                .primaryKnowledgeNodeCode("BASIC.LOOP.BOUNDARY")
                .knowledgeNodeCodes("BASIC.LOOP.BOUNDARY")
                .mistakeType("BOUNDARY")
                .commonMisconception("没有区分 range 右端开区间和题目闭区间。")
                .skillUnitCode("")
                .abilityPoint("")
                .requiredEvidence("code:range_excludes_n")
                .whenToUse("修复边界问题后复盘。")
                .studentBenefit("能更早发现端点漏取。")
                .applicableLanguages("PYTHON")
                .build();
    }
}
