package com.onlinejudge.submission.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlinejudge.learning.diagnosis.DiagnosisTaxonomy;
import com.onlinejudge.learning.standardlibrary.application.AiStandardLibraryService;
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
import static org.mockito.Mockito.when;

class AiReportServiceSearchLocationRuntimeTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void searchLocationSuccessUsesSelectedPackBeforeDiagnosis() {
        StubAiReportService service = newService(
                validSearchLocationResponse(),
                validAdviceResponse()
        );

        SubmissionAnalysisResponse analysis = service.enhanceSubmissionAnalysis(
                problem(),
                submission(),
                fallback(),
                evidencePackage()
        );

        assertThat(service.callCount()).isEqualTo(2);
        assertThat(service.userPrompt(0)).contains("candidatePack", "MP_RANGE_RIGHT_ENDPOINT_MISSING");
        assertThat(service.userPrompt(1))
                .contains("searchLocationSummary", "mistakePoints", "MP_RANGE_RIGHT_ENDPOINT_MISSING")
                .doesNotContain("SK_UNRELATED_ARRAY_INDEX");
        assertThat(analysis.getAiInvocation().getSearchLocationEnabled()).isTrue();
        assertThat(analysis.getAiInvocation().getSearchLocationStatus()).isEqualTo("SUCCESS");
        assertThat(analysis.getAiInvocation().getSearchLocationCandidateCount()).isEqualTo(3);
        assertThat(analysis.getAiInvocation().getSearchLocationSelectedCount()).isEqualTo(2);
        assertThat(analysis.getAiInvocation().getSearchLocationFailureReason()).isEmpty();
        assertThat(analysis.getAiInvocation().getEmbeddingStatus()).isEqualTo("DISABLED");
        assertThat(analysis.getIssueTags()).isEmpty();
        assertThat(analysis.getStudentFeedback().getSummary()).contains("循环边界");
    }

    @Test
    void searchLocationInvalidOutputMarksFailedAndContinuesDiagnosis() {
        StubAiReportService service = newService(
                """
                {
                  "basicCandidates": [{
                    "id": "UNKNOWN",
                    "layer": "MISTAKE_POINT",
                    "priority": 1,
                    "confidence": 0.91,
                    "evidenceRefs": ["code:range_excludes_n"],
                    "reason": "模型返回了不存在的候选。"
                  }],
                  "improvementCandidates": [],
                  "knowledgeAnchors": [],
                  "uncertainty": "定位输出包含非法 ID。",
                  "needsMoreEvidence": false
                }
                """,
                validAdviceResponse()
        );

        SubmissionAnalysisResponse analysis = service.enhanceSubmissionAnalysis(
                problem(),
                submission(),
                fallback(),
                evidencePackage()
        );

        assertThat(service.callCount()).isEqualTo(2);
        assertThat(analysis.getAiInvocation().getSearchLocationEnabled()).isTrue();
        assertThat(analysis.getAiInvocation().getSearchLocationStatus()).isEqualTo("FAILED");
        assertThat(analysis.getAiInvocation().getSearchLocationFailureReason()).contains("not in candidate pack");
        assertThat(analysis.getAiInvocation().getSearchLocationCandidateCount()).isEqualTo(3);
        assertThat(analysis.getAiInvocation().getSearchLocationSelectedCount()).isEqualTo(0);
        assertThat(analysis.getIssueTags()).isEmpty();
        assertThat(service.userPrompt(1)).contains("\"searchLocationSummary\":null");
    }

    @Test
    void externalSearchLocationDisabledUsesLocalRecallBeforeAdviceGeneration() {
        StubAiReportService service = newService(validAdviceResponse());
        service.searchLocationProperties.setEnabled(false);

        SubmissionAnalysisResponse analysis = service.enhanceSubmissionAnalysis(
                problem(),
                submission(),
                fallback(),
                evidencePackage()
        );

        assertThat(service.callCount()).isEqualTo(1);
        assertThat(service.userPrompt(0)).doesNotContain("candidatePack");
        assertThat(analysis.getAiInvocation().getSearchLocationEnabled()).isFalse();
        assertThat(analysis.getAiInvocation().getSearchLocationStatus()).isEqualTo("LOCAL_RECALL");
        assertThat(analysis.getAiInvocation().getEmbeddingStatus()).isEqualTo("DISABLED");
        assertThat(analysis.getIssueTags()).isEmpty();
    }

    private StubAiReportService newService(String... responses) {
        AiStandardLibraryService libraryService = mock(AiStandardLibraryService.class);
        when(libraryService.enabledSearchLocationItems()).thenReturn(searchLocationItems());
        SearchLocationProperties properties = new SearchLocationProperties();
        properties.setEnabled(true);
        properties.setMode("text");
        properties.setCandidateLimit(3);
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
                  "improvementCandidates": [],
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
                    "mistakePointId": null,
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
                    "improvementPointId": null,
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
                  "studentSummary": "这次主要问题是循环边界没有覆盖到题目要求的范围。"
                }
                """;
    }

    private static class StubAiReportService extends AiReportService {
        private final Queue<String> responses = new ArrayDeque<>();
        private final List<String> userPrompts = new ArrayList<>();
        private int callCount;
        private final SearchLocationProperties searchLocationProperties;

        StubAiReportService(ObjectMapper objectMapper,
                            ExternalModelAgentRuntime runtime,
                            SearchLocationRetrievalService retrievalService,
                            SearchLocationOutputValidator outputValidator,
                            SearchLocationPackSelector packSelector,
                            SearchLocationProperties searchLocationProperties,
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
                    searchLocationProperties);
            this.searchLocationProperties = searchLocationProperties;
            this.responses.addAll(List.of(responses));
        }

        @Override
        protected String chatCompletion(String systemPrompt, String userPrompt) throws IOException {
            callCount++;
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
    }
}
