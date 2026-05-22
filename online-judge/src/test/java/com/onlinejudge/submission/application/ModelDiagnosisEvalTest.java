package com.onlinejudge.submission.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlinejudge.classroom.application.HintSafetyService;
import com.onlinejudge.classroom.domain.Assignment;
import com.onlinejudge.learning.diagnosis.DiagnosisTaxonomy;
import com.onlinejudge.problem.domain.Problem;
import com.onlinejudge.submission.domain.Submission;
import com.onlinejudge.submission.domain.SubmissionCaseResult;
import com.onlinejudge.submission.dto.SubmissionAnalysisResponse;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ModelDiagnosisEvalTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void liveModelKeepsDiagnosisWithinExpectedTagsWhenEnabled() {
        String apiKey = System.getenv("AI_EVAL_API_KEY");
        Assumptions.assumeTrue(apiKey != null && !apiKey.isBlank(), "Set AI_EVAL_API_KEY to run live model diagnosis eval.");

        DiagnosticAgentService service = newLiveService(apiKey);
        List<EvalCase> cases = allEvalCases();

        for (EvalCase evalCase : cases) {
            DiagnosticAgentService.AgentResult result = service.diagnose(
                    evalCase.problem(),
                    evalCase.submission(),
                    evalCase.caseResults(),
                    evalCase.baseline(),
                    Assignment.HintPolicy.L2
            );

            assertThat(result.analysis().getIssueTags())
                    .as(evalCase.name() + " issue tags")
                    .containsAnyElementsOf(evalCase.expectedIssueTags());
            assertThat(result.analysis().getFineGrainedTags())
                    .as(evalCase.name() + " fine-grained tags")
                    .containsAnyElementsOf(evalCase.expectedFineTags());
            assertThat(result.analysis().getEvidenceRefs())
                    .as(evalCase.name() + " evidence refs")
                    .isNotEmpty();
            assertThat(result.analysis().getAnswerLeakRisk())
                    .as(evalCase.name() + " answer leak risk")
                    .isIn("LOW", "MEDIUM", "UNKNOWN");
            assertThat(result.traceSummary())
                    .as(evalCase.name() + " trace")
                    .contains("diagnostic-agent-v2");
            assertThat(result.analysis().getAiInvocation().isFallbackUsed())
                    .as(evalCase.name() + " live model should be used")
                    .isFalse();
            assertThat(result.traceSummary())
                    .as(evalCase.name() + " trace")
                    .contains("model=completed");
        }
    }

    @Test
    void evalCasesExposeStableExpectedTagsWithoutLiveModel() {
        List<EvalCase> cases = allEvalCases();

        assertThat(cases).hasSizeGreaterThanOrEqualTo(5);
        assertThat(cases)
                .allSatisfy(evalCase -> {
                    assertThat(evalCase.expectedIssueTags()).isNotEmpty();
                    assertThat(evalCase.expectedFineTags()).isNotEmpty();
                    assertThat(evalCase.submission().getSourceCode()).isNotBlank();
                    assertThat(evalCase.baseline().getScenario()).isNotBlank();
                });
    }

    @Test
    void teacherCorrectionFixturesLoadAsRegressionEvalCases() throws IOException {
        List<TeacherCorrectionEvalFixtureLoader.Fixture> fixtures =
                new TeacherCorrectionEvalFixtureLoader(objectMapper).loadDefault();

        assertThat(fixtures).hasSizeGreaterThanOrEqualTo(2);
        assertThat(fixtures)
                .allSatisfy(fixture -> {
                    assertThat(fixture.source()).isEqualTo("teacher-correction");
                    assertThat(fixture.teacherCorrection().correctedIssueTag()).isNotBlank();
                    assertThat(fixture.expectedIssueTags()).contains(fixture.teacherCorrection().correctedIssueTag());
                    assertThat(fixture.expectedFineTags()).contains(fixture.teacherCorrection().correctedFineGrainedTag());
                    assertThat(fixture.toSubmission().getSourceCode()).isNotBlank();
                    assertThat(fixture.toCaseResults()).as(fixture.name() + " case results").isNotEmpty();
                    assertThat(fixture.mustMention()).as(fixture.name() + " must mention").isNotEmpty();
                    assertThat(fixture.mustNotMention()).as(fixture.name() + " must not mention")
                            .contains("完整代码");
                    assertThat(fixture.sourceMaterial()).as(fixture.name() + " source material").isNotNull();
                    assertThat(fixture.sourceMaterial().artifacts()).as(fixture.name() + " source artifacts").isNotEmpty();
                    assertThat(fixture.sourceMaterial().anonymizationNote()).as(fixture.name() + " anonymization note")
                            .contains("身份");
                    assertThat(fixture.quality()).as(fixture.name() + " quality").isNotNull();
                    assertThat(fixture.quality().bugPattern()).as(fixture.name() + " bug pattern").isNotBlank();
                    assertThat(fixture.quality().misconception()).as(fixture.name() + " misconception").isNotBlank();
                    assertThat(fixture.quality().expectedStudentMove()).as(fixture.name() + " expected student move").isNotBlank();
                    assertThat(fixture.quality().evalPurpose()).as(fixture.name() + " eval purpose").isNotBlank();
                    assertThat(fixture.toBaseline().getEvidenceRefs()).contains("teacher_correction:" + fixture.correctionId());
                });
    }

    @Test
    void teacherCorrectionFixturesConstrainLocalDiagnosticAgent() throws IOException {
        DiagnosticAgentService service = newOfflineService();
        List<TeacherCorrectionEvalFixtureLoader.Fixture> fixtures =
                new TeacherCorrectionEvalFixtureLoader(objectMapper).loadDefault();

        for (TeacherCorrectionEvalFixtureLoader.Fixture fixture : fixtures) {
            DiagnosticAgentService.AgentResult result = service.diagnose(
                    fixture.toProblem(),
                    fixture.toSubmission(),
                    fixture.toCaseResults(),
                    fixture.toBaseline(),
                    Assignment.HintPolicy.L2
            );
            SubmissionAnalysisResponse analysis = result.analysis();
            String combinedText = String.join("\n",
                    safe(analysis.getSummary()),
                    safe(analysis.getStudentHint()),
                    analysis.getStudentHintPlan() == null ? "" : safe(analysis.getStudentHintPlan().getProblemType()),
                    analysis.getStudentHintPlan() == null ? "" : safe(analysis.getStudentHintPlan().getEvidenceAnchor()),
                    analysis.getStudentHintPlan() == null ? "" : safe(analysis.getStudentHintPlan().getNextAction()),
                    analysis.getStudentHintPlan() == null ? "" : safe(analysis.getStudentHintPlan().getCoachQuestion()),
                    analysis.getLearningInterventionPlan() == null ? "" : safe(analysis.getLearningInterventionPlan().getStudentTask())
            );

            assertThat(analysis.getIssueTags())
                    .as(fixture.name() + " issue tags")
                    .containsAnyElementsOf(fixture.expectedIssueTags());
            assertThat(analysis.getFineGrainedTags())
                    .as(fixture.name() + " fine tags")
                    .containsAnyElementsOf(fixture.expectedFineTags());
            assertThat(analysis.getEvidenceRefs())
                    .as(fixture.name() + " evidence refs")
                    .contains("teacher_correction:" + fixture.correctionId());
            fixture.mustMention().forEach(phrase -> assertThat(combinedText)
                    .as(fixture.name() + " mentions " + phrase)
                    .contains(phrase));
            fixture.mustNotMention().forEach(phrase -> assertThat(combinedText)
                    .as(fixture.name() + " avoids " + phrase)
                    .doesNotContain(phrase));
            assertThat(analysis.getAnswerLeakRisk())
                    .as(fixture.name() + " answer leak risk")
                    .isIn("LOW", "MEDIUM", "UNKNOWN");
        }
    }

    @Test
    void studentHintFixturesLoadAsLargeScaleEvalCases() throws IOException {
        List<StudentHintEvalFixtureLoader.Fixture> fixtures =
                new StudentHintEvalFixtureLoader(objectMapper).loadDefault();

        assertThat(fixtures).hasSize(100);
        assertThat(fixtures)
                .allSatisfy(fixture -> {
                    assertThat(fixture.source()).isEqualTo("synthetic-student-hint-v1");
                    assertThat(fixture.expected().issueTags()).isNotEmpty();
                    assertThat(fixture.expected().teachingAction()).isNotBlank();
                    assertThat(fixture.expected().mustNotMention()).contains("完整代码", "参考答案", "隐藏测试点");
                    assertThat(fixture.toSubmission().getSourceCode()).isNotBlank();
                    assertThat(fixture.toBaseline().getStudentHint()).isNotBlank();
                });
    }

    private DiagnosticAgentService newLiveService(String apiKey) {
        DiagnosisTaxonomy taxonomy = new DiagnosisTaxonomy();
        AiReportService aiReportService = new AiReportService(objectMapper, new AiCodeAssistSupport());
        ReflectionTestUtils.setField(aiReportService, "enabled", true);
        ReflectionTestUtils.setField(aiReportService, "apiKey", apiKey);
        ReflectionTestUtils.setField(aiReportService, "baseUrl", valueOrDefault(System.getenv("AI_EVAL_BASE_URL"), "https://api-inference.modelscope.cn/v1"));
        ReflectionTestUtils.setField(aiReportService, "model", valueOrDefault(System.getenv("AI_EVAL_MODEL"), "MiniMax/MiniMax-M2.7"));
        ReflectionTestUtils.setField(aiReportService, "timeoutSeconds", longValueOrDefault(System.getenv("AI_EVAL_TIMEOUT_SECONDS"), 35L));
        return new DiagnosticAgentService(
                new DiagnosisEvidencePackageBuilder(),
                new RuleSignalAnalyzer(),
                aiReportService,
                new HintSafetyService(null, new ObjectMapper(), taxonomy),
                taxonomy
        );
    }

    private DiagnosticAgentService newOfflineService() {
        DiagnosisTaxonomy taxonomy = new DiagnosisTaxonomy();
        return new DiagnosticAgentService(
                new DiagnosisEvidencePackageBuilder(),
                new RuleSignalAnalyzer(),
                new PassThroughAiReportService(),
                new HintSafetyService(null, objectMapper, taxonomy),
                taxonomy
        );
    }

    private EvalCase offByOneCase() {
        Submission submission = Submission.builder()
                .id(101L)
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
        return new EvalCase(
                "off-by-one",
                Problem.builder()
                        .id(1L)
                        .title("求 1 到 n 的和")
                        .description("输入一个正整数 n，输出 1 到 n 的整数和。")
                        .difficulty(Problem.Difficulty.EASY)
                        .timeLimit(1000)
                        .memoryLimit(65536)
                        .build(),
                submission,
                List.of(),
                baseline(submission, "WA", List.of("BOUNDARY_CONDITION"), List.of()),
                List.of("LOOP_BOUNDARY", "BOUNDARY_CONDITION"),
                List.of("OFF_BY_ONE")
        );
    }

    private EvalCase outputFormatCase() {
        Submission submission = Submission.builder()
                .id(102L)
                .problemId(2L)
                .languageName("Python 3")
                .verdict(Submission.Verdict.WRONG_ANSWER)
                .sourceCode("""
                        n = int(input())
                        print(n, end=" ")
                        """)
                .build();
        List<SubmissionCaseResult> cases = List.of(SubmissionCaseResult.builder()
                .testCaseNumber(1)
                .passed(false)
                .hidden(false)
                .actualOutput("42 ")
                .expectedOutput("42")
                .build());
        return new EvalCase(
                "output-format",
                Problem.builder()
                        .id(2L)
                        .title("原样输出")
                        .description("输入一个整数，输出该整数，不要输出多余空格。")
                        .difficulty(Problem.Difficulty.EASY)
                        .timeLimit(1000)
                        .memoryLimit(65536)
                        .build(),
                submission,
                cases,
                baseline(submission, "WA", List.of("IO_FORMAT"), List.of()),
                List.of("IO_FORMAT"),
                List.of("OUTPUT_FORMAT_DETAIL")
        );
    }

    private EvalCase bruteForceCase() {
        Submission submission = Submission.builder()
                .id(103L)
                .problemId(3L)
                .languageName("Python 3")
                .verdict(Submission.Verdict.TIME_LIMIT_EXCEEDED)
                .sourceCode("""
                        n = int(input())
                        arr = list(map(int, input().split()))
                        ans = 0
                        for i in range(n):
                            for j in range(n):
                                if arr[i] < arr[j]:
                                    ans += 1
                        print(ans)
                        """)
                .build();
        return new EvalCase(
                "brute-force",
                Problem.builder()
                        .id(3L)
                        .title("统计更大元素")
                        .description("给定 n 个整数，统计每个元素右侧比它大的数量总和，n 最大为 200000。")
                        .difficulty(Problem.Difficulty.MEDIUM)
                        .timeLimit(1000)
                        .memoryLimit(65536)
                        .build(),
                submission,
                List.of(),
                baseline(submission, "TLE", List.of("TIME_COMPLEXITY"), List.of()),
                List.of("TIME_COMPLEXITY"),
                List.of("BRUTE_FORCE_LIMIT")
        );
    }

    private SubmissionAnalysisResponse baseline(Submission submission,
                                                String scenario,
                                                List<String> issueTags,
                                                List<String> fineTags) {
        return SubmissionAnalysisResponse.builder()
                .submissionId(submission.getId())
                .sourceType("RULE_BASED_V1")
                .scenario(scenario)
                .headline("规则层初步诊断")
                .summary("规则层初步诊断")
                .issueTags(issueTags)
                .fineGrainedTags(fineTags)
                .evidenceRefs(List.of())
                .confidence(0.7)
                .answerLeakRisk("LOW")
                .build();
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private long longValueOrDefault(String value, long fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private List<EvalCase> allEvalCases() {
        List<EvalCase> cases = new ArrayList<>();
        cases.add(offByOneCase());
        cases.add(outputFormatCase());
        cases.add(bruteForceCase());
        cases.addAll(teacherCorrectionEvalCases());
        return cases;
    }

    private List<EvalCase> teacherCorrectionEvalCases() {
        try {
            return new TeacherCorrectionEvalFixtureLoader(objectMapper).loadDefault()
                    .stream()
                    .map(fixture -> new EvalCase(
                            fixture.name(),
                            fixture.toProblem(),
                            fixture.toSubmission(),
                            fixture.toCaseResults(),
                            fixture.toBaseline(),
                            fixture.expectedIssueTags(),
                            fixture.expectedFineTags()
                    ))
                    .toList();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load teacher correction eval fixtures", exception);
        }
    }

    private record EvalCase(String name,
                            Problem problem,
                            Submission submission,
                            List<SubmissionCaseResult> caseResults,
                            SubmissionAnalysisResponse baseline,
                            List<String> expectedIssueTags,
                            List<String> expectedFineTags) {
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private static class PassThroughAiReportService extends AiReportService {
        PassThroughAiReportService() {
            super(new ObjectMapper(), new AiCodeAssistSupport());
        }

        @Override
        public SubmissionAnalysisResponse enhanceSubmissionAnalysis(Problem problem,
                                                                    Submission submission,
                                                                    SubmissionAnalysisResponse fallback,
                                                                    DiagnosisEvidencePackage evidencePackage,
                                                                    RuleSignalAnalyzer.RuleSignalResult ruleSignals) {
            return fallback;
        }
    }
}
