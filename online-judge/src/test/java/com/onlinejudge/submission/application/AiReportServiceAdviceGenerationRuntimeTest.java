package com.onlinejudge.submission.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlinejudge.learning.diagnosis.DiagnosisTaxonomy;
import com.onlinejudge.learning.standardlibrary.application.AiStandardLibraryService;
import com.onlinejudge.learning.standardlibrary.application.AiStandardLibraryGrowthAgentService;
import com.onlinejudge.learning.standardlibrary.domain.AiStandardLibraryItem;
import com.onlinejudge.learning.standardlibrary.domain.AiStandardLibraryLayer;
import com.onlinejudge.problem.domain.Problem;
import com.onlinejudge.submission.domain.Submission;
import com.onlinejudge.submission.dto.SubmissionAnalysisResponse;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiReportServiceAdviceGenerationRuntimeTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void defaultRuntimeGeneratesStructuredAdviceWithSingleDiagnosisCall() {
        StubAiReportService service = newService(validAdviceResponse());

        SubmissionAnalysisResponse analysis = service.enhanceSubmissionAnalysis(
                problem(),
                submission(),
                fallback(),
                evidencePackage(),
                ruleSignals()
        );

        assertThat(service.callCount()).isEqualTo(1);
        assertThat(service.userPrompt(0))
                .contains("brief", "standardLibrary", "searchLocationSummary")
                .contains("mistakePoints")
                .contains("MP_RANGE_RIGHT_ENDPOINT_MISSING")
                .doesNotContain("candidatePack");
        assertThat(analysis.getAiInvocation().getPromptVersion()).isEqualTo(PromptTemplateRegistry.DIAGNOSIS_REPORT_V2);
        assertThat(analysis.getAiInvocation().getAdviceGenerationStatus()).isEqualTo("SUCCESS");
        assertThat(analysis.getAiInvocation().getSearchLocationStatus()).isEqualTo("LOCAL_RECALL");
        assertThat(analysis.getBasicLayerAdvice()).hasSize(1);
        assertThat(analysis.getStudentFeedback().getBlockingIssues().get(0).getTitle())
                .contains("循环右边界");
    }

    @Test
    void explicitSearchLocationRuntimeGeneratesStructuredAdviceAfterSearchLocation() {
        StubAiReportService service = newService(
                validSearchLocationResponse(),
                validAdviceResponse()
        );
        service.enableSearchLocation();

        SubmissionAnalysisResponse analysis = service.enhanceSubmissionAnalysis(
                problem(),
                submission(),
                fallback(),
                evidencePackage(),
                ruleSignals()
        );

        assertThat(service.callCount()).isEqualTo(2);
        assertThat(service.userPrompt(1))
                .contains("searchLocationSummary", "mistakePoints", "MP_RANGE_RIGHT_ENDPOINT_MISSING")
                .doesNotContain("SK_UNRELATED_ARRAY_INDEX");
        assertThat(analysis.getAiInvocation().getPromptVersion()).isEqualTo(PromptTemplateRegistry.DIAGNOSIS_REPORT_V2);
        assertThat(analysis.getAiInvocation().getAdviceGenerationStatus()).isEqualTo("SUCCESS");
        assertThat(analysis.getAiInvocation().getBasicAdviceCount()).isEqualTo(1);
        assertThat(analysis.getAiInvocation().getImprovementAdviceCount()).isEqualTo(1);
        assertThat(analysis.getAiInvocation().getSearchLocationStatus()).isEqualTo("SUCCESS");
        assertThat(analysis.getCaseUnderstanding().getBehaviorGap()).contains("没有覆盖题目要求的末端");
        assertThat(analysis.getBasicLayerAdvice()).hasSize(1);
        assertThat(analysis.getBasicLayerAdvice().get(0).getMistakePointId())
                .isEqualTo("MP_RANGE_RIGHT_ENDPOINT_MISSING");
        assertThat(analysis.getImprovementLayerAdvice()).hasSize(1);
        assertThat(analysis.getStudentFeedback().getBlockingIssues().get(0).getTitle())
                .contains("循环右边界");
        assertThat(analysis.getStudentFeedback().getImprovementOpportunities().get(0).getCategory())
                .isEqualTo("TESTING_HABIT");
        assertThat(analysis.getReportMarkdown()).contains("## AI 完整诊断与建议", "### 基础层", "### 提高层");
    }

    @Test
    void diagnosisReportV2MarkdownPrefersNaturalStudentReport() {
        StubAiReportService service = newService(diagnosisReportV2WithSoftFixesResponse());

        SubmissionAnalysisResponse analysis = service.enhanceSubmissionAnalysis(
                problem(),
                submission(),
                fallback(),
                evidencePackage(),
                ruleSignals()
        );

        assertThat(analysis.getReportMarkdown())
                .contains("### 基础层", "循环范围和题目边界要求没有完全对齐")
                .contains("### 提高层", "固定自测清单")
                .contains("### 下一步行动", "写出循环变量序列")
                .doesNotContain("发生了什么", "为什么重要");
    }

    @Test
    void adviceValidationFailureFallsBackWithoutPretendingSuccess() {
        StubAiReportService service = newService(
                """
                {
                  "caseUnderstanding": {
                    "problemGoal": "输出 1 到 n 的整数和。",
                    "codeIntent": "学生想用循环累加。",
                    "behaviorGap": "循环没有覆盖末端。",
                    "primaryEvidenceRef": "invented:evidence"
                  },
                  "basicLayerAdvice": [{
                    "mistakePointId": "MP_RANGE_RIGHT_ENDPOINT_MISSING",
                    "skillUnitId": "SK_RANGE_BOUNDARY",
                    "title": "循环右边界漏取",
                    "whatHappened": "当前循环范围没有覆盖题目要求的最后一个数。",
                    "whyItMatters": "少处理端点会让结果偏小。",
                    "studentAction": "先手推 n=1 和 n=2。",
                    "checkQuestion": "最后一个数有没有进入循环？",
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
                """
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
        assertThat(analysis.getAiInvocation().getFailureStage()).isEqualTo("DIAGNOSIS_AND_ADVICE");
        assertThat(analysis.getAiInvocation().getAdviceGenerationStatus()).isEqualTo("FALLBACK_USED");
        assertThat(analysis.getAiInvocation().getAdviceGenerationFallbackReason()).contains("INVALID_EVIDENCE_REF");
    }

    @Test
    void safetyRiskAdviceIsRewrittenBeforeFallback() {
        StubAiReportService service = newService(
                unsafeAdviceResponse(),
                validAdviceResponse()
        );

        SubmissionAnalysisResponse analysis = service.enhanceSubmissionAnalysis(
                problem(),
                submission(),
                fallback(),
                evidencePackage(),
                ruleSignals()
        );

        assertThat(service.callCount()).isEqualTo(2);
        assertThat(service.userPrompt(1)).contains("previousOutput", "validationFailure");
        assertThat(service.systemPrompt(1)).contains("DP 或状态设计问题", "不要写前驱状态", "空间压缩");
        assertThat(analysis.getSourceType()).isEqualTo("MODEL_SCOPE_EXTERNAL_MODEL");
        assertThat(analysis.getAiInvocation().getStatus()).isEqualTo("MODEL_COMPLETED");
        assertThat(analysis.getAiInvocation().getAdviceGenerationStatus()).isEqualTo("SUCCESS");
        assertThat(analysis.getAiInvocation().getSearchLocationStatus()).isEqualTo("LOCAL_RECALL");
        assertThat(analysis.getAiInvocation().getStreamFallbackRetryUsed()).isTrue();
        assertThat(analysis.getStudentFeedback().getBlockingIssues().get(0).getNextAction())
                .doesNotContain("直接改成", "range(1, n + 1)");
    }

    @Test
    void diagnosisReportV2SoftFixesAreVisibleInInvocationTrace() {
        StubAiReportService service = newService(diagnosisReportV2WithSoftFixesResponse());

        SubmissionAnalysisResponse analysis = service.enhanceSubmissionAnalysis(
                problem(),
                submission(),
                fallback(),
                evidencePackage(),
                ruleSignals()
        );

        assertThat(analysis.getSourceType()).isEqualTo("MODEL_SCOPE_EXTERNAL_MODEL");
        assertThat(analysis.getAiInvocation().getPromptVersion()).isEqualTo(PromptTemplateRegistry.DIAGNOSIS_REPORT_V2);
        assertThat(analysis.getAiInvocation().getDiagnosisLibraryFit()).isEqualTo("PARTIAL");
        assertThat(analysis.getAiInvocation().getDiagnosisSoftFixes())
                .contains("evidenceRef alias sourceCode -> code:range_excludes_n")
                .contains("evidenceRef alias problemConstraints -> judge:first_failed_case")
                .noneSatisfy(item -> assertThat(item).contains("unknown anchor id"));
        assertThat(analysis.getAiInvocation().getDiagnosisHardFailures()).isEmpty();
        assertThat(analysis.getStudentFeedback().getBlockingIssues()).singleElement()
                .satisfies(item -> assertThat(item.getStudentMessage()).contains("基础层：循环范围"));
    }

    @Test
    void diagnosisReportV2GrowthCandidatesArePersistedToCandidatePool() {
        AiStandardLibraryGrowthAgentService growthAgentService = mock(AiStandardLibraryGrowthAgentService.class);
        StubAiReportService service = newService(
                growthAgentService,
                diagnosisReportV2WithGrowthCandidateResponse()
        );

        service.enhanceSubmissionAnalysis(
                problem(),
                submission(),
                fallback(),
                evidencePackage(),
                ruleSignals()
        );

        verify(growthAgentService).proposeFromDiagnosisOutput(org.mockito.ArgumentMatchers.argThat(output ->
                output != null
                        && output.getLibraryGrowth() != null
                        && output.getLibraryGrowth().getCandidates() != null
                        && output.getLibraryGrowth().getCandidates().size() == 1
        ));
    }

    private StubAiReportService newService(AiStandardLibraryGrowthAgentService growthAgentService, String... responses) {
        AiStandardLibraryService libraryService = mock(AiStandardLibraryService.class);
        when(libraryService.enabledSearchLocationItems()).thenReturn(searchLocationItems());
        SearchLocationProperties properties = new SearchLocationProperties();
        properties.setMode("text");
        properties.setCandidateLimit(4);
        SearchLocationRetrievalService retrievalService = new SearchLocationRetrievalService(
                libraryService,
                properties,
                mock(EmbeddingClient.class)
        );
        SearchLocationPackSelector selector = new SearchLocationPackSelector(libraryService, new DiagnosisTaxonomy());
        StubAiReportService service = new StubAiReportService(
                objectMapper,
                runtime(),
                retrievalService,
                new SearchLocationOutputValidator(),
                selector,
                properties,
                growthAgentService,
                responses
        );
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "apiKey", "test-key");
        ReflectionTestUtils.setField(service, "model", "test-model");
        ReflectionTestUtils.setField(service, "externalRuntimeEnabled", true);
        ReflectionTestUtils.setField(service, "externalRuntimeProfile", ExternalModelAgentRuntime.RUNTIME_PROFILE_STANDARD);
        ReflectionTestUtils.setField(service, "maxOutputTokens", 1200);
        return service;
    }

    private StubAiReportService newService(String... responses) {
        AiStandardLibraryService libraryService = mock(AiStandardLibraryService.class);
        when(libraryService.enabledSearchLocationItems()).thenReturn(searchLocationItems());
        SearchLocationProperties properties = new SearchLocationProperties();
        properties.setMode("text");
        properties.setCandidateLimit(4);
        SearchLocationRetrievalService retrievalService = new SearchLocationRetrievalService(
                libraryService,
                properties,
                mock(EmbeddingClient.class)
        );
        SearchLocationPackSelector selector = new SearchLocationPackSelector(libraryService, new DiagnosisTaxonomy());
        StubAiReportService service = new StubAiReportService(
                objectMapper,
                runtime(),
                retrievalService,
                new SearchLocationOutputValidator(),
                selector,
                properties,
                null,
                responses
        );
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "apiKey", "test-key");
        ReflectionTestUtils.setField(service, "model", "test-model");
        ReflectionTestUtils.setField(service, "externalRuntimeEnabled", true);
        ReflectionTestUtils.setField(service, "externalRuntimeProfile", ExternalModelAgentRuntime.RUNTIME_PROFILE_STANDARD);
        ReflectionTestUtils.setField(service, "maxOutputTokens", 1200);
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

    private List<AiStandardLibraryItem> searchLocationItems() {
        return List.of(
                item("SK_RANGE_BOUNDARY", AiStandardLibraryLayer.SKILL_UNIT,
                        "循环边界",
                        "理解 Python range 右端不包含以及闭区间题意之间的对应关系。"),
                item("MP_RANGE_RIGHT_ENDPOINT_MISSING", AiStandardLibraryLayer.MISTAKE_POINT,
                        "循环边界",
                        "使用 range(1, n) 表达 1 到 n 的闭区间，导致最后一个数没有处理。"),
                item("TESTING_HABIT", AiStandardLibraryLayer.IMPROVEMENT_POINT,
                        "自测",
                        "修复后补充最小边界和端点样例。"),
                item("SK_UNRELATED_ARRAY_INDEX", AiStandardLibraryLayer.SKILL_UNIT,
                        "数组下标",
                        "数组下标合法范围与越界检查。")
        );
    }

    private AiStandardLibraryItem item(String code,
                                       AiStandardLibraryLayer layer,
                                       String category,
                                       String description) {
        return AiStandardLibraryItem.builder()
                .id((long) Math.abs(code.hashCode()))
                .layer(layer)
                .code(code)
                .category(category)
                .name(description)
                .description(description)
                .studentExplanation(description)
                .teacherExplanation(description)
                .skillUnitCode("SK_RANGE_BOUNDARY")
                .mistakeType("OFF_BY_ONE")
                .commonMisconception(description)
                .evidenceSignals("code:range_excludes_n\njudge:first_failed_case")
                .commonCodePatterns("range(1, n)")
                .judgeSignals("WRONG_ANSWER")
                .abilityPoint("循环边界")
                .severity("HIGH")
                .applicableLanguages("PYTHON")
                .knowledgeNodeCodes("BASIC.LOOP.BOUNDARY")
                .teachingAction("TRACE_VARIABLES")
                .requiredEvidence("code:range_excludes_n")
                .whenToUse(description)
                .studentBenefit("能更早发现边界遗漏。")
                .enabled(true)
                .libraryVersion("test-v1")
                .build();
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

    private String validSearchLocationResponse() {
        return """
                {
                  "basicCandidates": [{
                    "id": "MP_RANGE_RIGHT_ENDPOINT_MISSING",
                    "layer": "MISTAKE_POINT",
                    "mistakePointId": "MP_RANGE_RIGHT_ENDPOINT_MISSING",
                    "priority": 1,
                    "confidence": 0.93,
                    "evidenceRefs": ["code:range_excludes_n"],
                    "reason": "代码中的 range(1, n) 与题目闭区间要求不一致。"
                  }],
                  "improvementCandidates": [{
                    "id": "TESTING_HABIT",
                    "layer": "IMPROVEMENT_POINT",
                    "priority": 1,
                    "confidence": 0.8,
                    "evidenceRefs": ["code:range_excludes_n"],
                    "reason": "修复后应补边界自测。"
                  }],
                  "knowledgeAnchors": [{
                    "id": "SK_RANGE_BOUNDARY",
                    "layer": "SKILL_UNIT",
                    "skillUnitId": "SK_RANGE_BOUNDARY",
                    "priority": 1,
                    "confidence": 0.88,
                    "evidenceRefs": ["code:range_excludes_n"],
                    "reason": "问题落在循环边界能力点。"
                  }],
                  "uncertainty": "可见证据已经较明确。",
                  "needsMoreEvidence": false
                }
                """;
    }

    private String diagnosisReportV2WithGrowthCandidateResponse() {
        return """
                {
                  "diagnosisDecision": {
                    "libraryFit": "MISS",
                    "anchors": [{
                      "id": null,
                      "type": "OUT_OF_LIBRARY",
                      "role": "PRIMARY",
                      "confidence": 0.78,
                      "evidenceRefs": ["code:range_excludes_n"],
                      "reason": "当前候选不能精确表达该错因。"
                    }]
                  },
                  "studentReport": {
                    "hintLevel": "L3",
                    "basicLayerText": "基础层：循环范围和题目边界要求没有完全对齐。",
                    "improvementLayerText": "提高层：补充边界自测清单。",
                    "nextActionText": "下一步：手推 n=1 和 n=2。"
                  },
                  "libraryGrowth": {
                    "candidates": [{
                      "name": "可见失败样例定位端点漏取",
                      "suggestedPath": ["BASIC", "LOOP", "BOUNDARY", "VISIBLE_CASE_ENDPOINT"],
                      "sourceProblemId": 1,
                      "sourceSubmissionId": 11,
                      "similarExistingItems": ["MP_RANGE_RIGHT_ENDPOINT_MISSING"],
                      "reason": "MISS 场景下发现更细颗粒错因。",
                      "status": "PROPOSED",
                      "confidence": 0.78
                    }]
                  },
                  "studentSummary": "这次重点是循环边界。"
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
                    "mistakePointId": "MP_RANGE_RIGHT_ENDPOINT_MISSING",
                    "skillUnitId": "SK_RANGE_BOUNDARY",
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
                    "skillUnitId": "SK_RANGE_BOUNDARY",
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

    private String diagnosisReportV2WithSoftFixesResponse() {
        return """
                {
                  "diagnosisDecision": {
                    "libraryFit": "PARTIAL",
                    "anchors": [{
                      "id": "MP_RANGE_RIGHT_ENDPOINT_MISSING",
                      "type": "MISTAKE_POINT",
                      "role": "PRIMARY",
                      "confidence": 0.82,
                      "evidenceRefs": ["sourceCode", "problemConstraints"],
                      "reason": "模型发现了库里没有精确覆盖的边界错因。"
                    }]
                  },
                  "studentReport": {
                    "hintLevel": "L3",
                    "basicLayerText": "基础层：循环范围和题目边界要求没有完全对齐。先手推最小样例，确认端点是否进入循环。",
                    "improvementLayerText": "提高层：修好后把最小值、端点值、最大值附近样例加入固定自测清单。",
                    "nextActionText": "下一步：用 n=1 和 n=2 写出循环变量序列，再和题目要求逐项对照。"
                  },
                  "studentSummary": "这次重点是循环边界和边界自测。"
                }
                """;
    }

    private String unsafeAdviceResponse() {
        return """
                {
                  "caseUnderstanding": {
                    "problemGoal": "题目要求输出 1 到 n 的整数和。",
                    "codeIntent": "学生使用循环累加 total。",
                    "behaviorGap": "循环实际没有覆盖题目要求的末端。",
                    "primaryEvidenceRef": "code:range_excludes_n"
                  },
                  "basicLayerAdvice": [{
                    "mistakePointId": "MP_RANGE_RIGHT_ENDPOINT_MISSING",
                    "skillUnitId": "SK_RANGE_BOUNDARY",
                    "title": "循环右边界漏取",
                    "whatHappened": "当前循环范围没有覆盖题目要求的最后一个数。",
                    "whyItMatters": "少处理一个端点会让求和结果偏小。",
                    "studentAction": "直接改成 range(1, n + 1)。",
                    "checkQuestion": "最后一个应该被处理的数有没有进入循环？",
                    "evidenceRefs": ["code:range_excludes_n"],
                    "confidence": 0.92
                  }],
                  "improvementLayerAdvice": [{
                    "improvementPointId": "TESTING_HABIT",
                    "skillUnitId": "SK_RANGE_BOUNDARY",
                    "title": "补充边界样例意识",
                    "currentLimit": "这类问题不是算法方向错，而是边界验证不足。",
                    "suggestion": "修复后补测最小值和端点样例。",
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
        private final Queue<String> responses = new ArrayDeque<>();
        private final List<String> systemPrompts = new ArrayList<>();
        private final List<String> userPrompts = new ArrayList<>();
        private int callCount;

        StubAiReportService(ObjectMapper objectMapper,
                            ExternalModelAgentRuntime runtime,
                            SearchLocationRetrievalService retrievalService,
                            SearchLocationOutputValidator outputValidator,
                            SearchLocationPackSelector packSelector,
                            SearchLocationProperties searchLocationProperties,
                            AiStandardLibraryGrowthAgentService growthAgentService,
                            String... responses) {
            super(objectMapper,
                    new AiCodeAssistSupport(),
                    runtime,
                    new ExternalModelFailureClassifier(),
                    new ExternalModelBudgetGuard(),
                    new ExternalModelChatRequestFactory(),
                    retrievalService,
                    outputValidator,
                    new SearchLocationOutputNormalizer(),
                    packSelector,
                    searchLocationProperties,
                    growthAgentService);
            this.responses.addAll(List.of(responses));
        }

        @Override
        protected String chatCompletion(String systemPrompt, String userPrompt) throws IOException {
            callCount++;
            systemPrompts.add(systemPrompt);
            userPrompts.add(userPrompt);
            String response = responses.poll();
            if (response == null) {
                throw new IOException("No stub response configured.");
            }
            return response;
        }

        int callCount() {
            return callCount;
        }

        String userPrompt(int index) {
            return userPrompts.get(index);
        }

        String systemPrompt(int index) {
            return systemPrompts.get(index);
        }

        void enableSearchLocation() {
            SearchLocationProperties properties =
                    (SearchLocationProperties) ReflectionTestUtils.getField(this, "searchLocationProperties");
            if (properties != null) {
                properties.setEnabled(true);
            }
        }
    }
}
