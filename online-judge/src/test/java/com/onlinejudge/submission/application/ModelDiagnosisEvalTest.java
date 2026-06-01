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
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ModelDiagnosisEvalTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void liveModelKeepsDiagnosisWithinExpectedTagsWhenEnabled() {
        String apiKey = System.getenv("AI_EVAL_API_KEY");
        Assumptions.assumeTrue(apiKey != null && !apiKey.isBlank(), "Set AI_EVAL_API_KEY to run live model diagnosis eval.");
        Assumptions.assumeTrue(Boolean.parseBoolean(valueOrDefault(System.getenv("AI_EVAL_FULL"), "false")),
                "Set AI_EVAL_FULL=true to run the full live model diagnosis eval.");

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
    void liveModelSmokeProducesPerCaseReportWhenEnabled() throws IOException {
        String apiKey = System.getenv("AI_EVAL_API_KEY");
        Assumptions.assumeTrue(apiKey != null && !apiKey.isBlank(), "Set AI_EVAL_API_KEY to run live model smoke eval.");

        DiagnosticAgentService service = newLiveService(apiKey);
        List<EvalCase> cases = allEvalCases().stream()
                .limit(Math.max(1, (int) longValueOrDefault(System.getenv("AI_EVAL_SMOKE_LIMIT"), 2L)))
                .toList();

        LiveModelEvalReport report = runLiveEvalReport(service, cases);
        Path reportPath = writeLiveEvalReport(report);

        System.out.println("Live model eval report saved to: " + reportPath);
        System.out.println("Live model eval summary: total=" + report.getTotalCount()
                + ", completed=" + report.getCompletedCount()
                + ", fallback=" + report.getFallbackCount()
                + ", timeout=" + report.getTimeoutCount()
                + ", issueHits=" + report.getIssueTagHitCount()
                + ", fineHits=" + report.getFineTagHitCount()
                + ", safetyPassed=" + report.getSafetyPassedCount());

        assertThat(report.getEntries()).hasSize(cases.size());
        assertThat(reportPath).exists();
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

    @Test
    void promptSafetyFixturesLoadAsRegressionSafetyCases() throws IOException {
        List<PromptSafetyEvalFixtureLoader.Fixture> fixtures =
                new PromptSafetyEvalFixtureLoader(objectMapper).loadDefault();

        assertThat(fixtures).hasSizeGreaterThanOrEqualTo(3);
        assertThat(fixtures)
                .allSatisfy(fixture -> {
                    assertThat(fixture.source()).isEqualTo("prompt-safety-eval-v1");
                    assertThat(fixture.name()).isNotBlank();
                    assertThat(fixture.toProblem().getTitle()).isNotBlank();
                    assertThat(fixture.toSubmission().getSourceCode()).isNotBlank();
                    assertThat(fixture.toCaseResults()).as(fixture.name() + " case results").isNotEmpty();
                    assertThat(fixture.toUnsafeAnalysis().getStudentHint()).as(fixture.name() + " unsafe hint").isNotBlank();
                    assertThat(fixture.expected().riskLevel()).as(fixture.name() + " risk level").isIn("MEDIUM", "HIGH");
                    assertThat(fixture.expected().blockedReasons()).as(fixture.name() + " blocked reasons").isNotEmpty();
                    assertThat(fixture.expected().expectedSafetyAction()).as(fixture.name() + " safety action").isEqualTo("COLLECT_EVIDENCE");
                    assertThat(fixture.expected().mustNotMention()).as(fixture.name() + " forbidden phrases").isNotEmpty();
                    assertThat(fixture.expected().requiredEvidenceRefs()).as(fixture.name() + " evidence refs").isNotEmpty();
                    assertThat(fixture.sourceMaterial().artifacts()).as(fixture.name() + " source artifacts").isNotEmpty();
                    assertThat(fixture.quality().evalPurpose()).as(fixture.name() + " eval purpose").isNotBlank();
                });
    }

    @Test
    void promptSafetyFixturesConstrainLocalSafetyGate() throws IOException {
        HintSafetyService safetyService = new HintSafetyService(null, objectMapper, new DiagnosisTaxonomy());
        List<PromptSafetyEvalFixtureLoader.Fixture> fixtures =
                new PromptSafetyEvalFixtureLoader(objectMapper).loadDefault();

        for (PromptSafetyEvalFixtureLoader.Fixture fixture : fixtures) {
            SubmissionAnalysisResponse safeAnalysis = safetyService.verifyAndRecord(
                    fixture.toUnsafeAnalysis(),
                    Assignment.HintPolicy.L2
            );
            String combinedText = String.join("\n",
                    safe(safeAnalysis.getStudentHint()),
                    safeAnalysis.getStudentHintPlan() == null ? "" : safe(safeAnalysis.getStudentHintPlan().getNextAction()),
                    safeAnalysis.getStudentHintPlan() == null ? "" : safe(safeAnalysis.getStudentHintPlan().getCoachQuestion()),
                    safeAnalysis.getLearningInterventionPlan() == null ? "" : safe(safeAnalysis.getLearningInterventionPlan().getStudentTask()),
                    safeAnalysis.getLearningInterventionPlan() == null ? "" : safe(safeAnalysis.getLearningInterventionPlan().getCheckQuestion()),
                    safe(safeAnalysis.getReportMarkdown())
            ).toLowerCase();

            assertThat(riskWeight(safeAnalysis.getAnswerLeakRisk()))
                    .as(fixture.name() + " risk level")
                    .isGreaterThanOrEqualTo(riskWeight(fixture.expected().riskLevel()));
            assertThat(safeAnalysis.getStudentHintPlan()).as(fixture.name() + " safe hint plan").isNotNull();
            assertThat(safeAnalysis.getStudentHintPlan().getTeachingAction())
                    .as(fixture.name() + " teaching action")
                    .isEqualTo(fixture.expected().expectedSafetyAction());
            assertThat(safeAnalysis.getLearningInterventionPlan()).as(fixture.name() + " intervention plan").isNotNull();
            assertThat(safeAnalysis.getLearningInterventionPlan().getInterventionType())
                    .as(fixture.name() + " intervention action")
                    .isEqualTo(fixture.expected().expectedSafetyAction());
            assertThat(safeAnalysis.getEvidenceRefs()).as(fixture.name() + " evidence refs")
                    .containsAll(fixture.expected().requiredEvidenceRefs());
            fixture.expected().mustNotMention().forEach(phrase -> assertThat(combinedText)
                    .as(fixture.name() + " avoids " + phrase)
                    .doesNotContain(phrase.toLowerCase()));
            assertThat(combinedText)
                    .as(fixture.name() + " safe evidence action")
                    .containsAnyOf("最小", "evidence", "样例", "输出对比", "验证");
        }
    }

    private DiagnosticAgentService newLiveService(String apiKey) {
        DiagnosisTaxonomy taxonomy = new DiagnosisTaxonomy();
        ExternalModelAgentRuntime runtime = new ExternalModelAgentRuntime(
                new ModelDiagnosisBriefBuilder(),
                new StandardLibraryPackBuilder(taxonomy),
                new PromptTemplateRegistry(),
                new ModelOutputValidator()
        );
        AiReportService aiReportService = new AiReportService(objectMapper, new AiCodeAssistSupport(), runtime);
        ReflectionTestUtils.setField(aiReportService, "enabled", true);
        ReflectionTestUtils.setField(aiReportService, "apiKey", apiKey);
        ReflectionTestUtils.setField(aiReportService, "baseUrl", valueOrDefault(System.getenv("AI_EVAL_BASE_URL"), "https://api-inference.modelscope.cn/v1"));
        ReflectionTestUtils.setField(aiReportService, "model", valueOrDefault(System.getenv("AI_EVAL_MODEL"), "deepseek-ai/DeepSeek-V4-Pro"));
        ReflectionTestUtils.setField(aiReportService, "timeoutSeconds", longValueOrDefault(System.getenv("AI_EVAL_TIMEOUT_SECONDS"), 35L));
        ReflectionTestUtils.setField(aiReportService, "externalRuntimeEnabled",
                Boolean.parseBoolean(valueOrDefault(System.getenv("AI_EVAL_EXTERNAL_RUNTIME_ENABLED"), "true")));
        ReflectionTestUtils.setField(aiReportService, "maxOutputTokens", (int) longValueOrDefault(System.getenv("AI_EVAL_MAX_OUTPUT_TOKENS"), 900L));
        ReflectionTestUtils.setField(aiReportService, "streamEnabled",
                Boolean.parseBoolean(valueOrDefault(System.getenv("AI_STREAM_ENABLED"), "true")));
        ReflectionTestUtils.setField(aiReportService, "streamFallbackEnabled",
                Boolean.parseBoolean(valueOrDefault(System.getenv("AI_STREAM_FALLBACK_ENABLED"), "true")));
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

    private int riskWeight(String risk) {
        return switch (risk == null ? "" : risk.toUpperCase()) {
            case "HIGH" -> 3;
            case "MEDIUM" -> 2;
            case "LOW" -> 1;
            default -> 0;
        };
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

    private LiveModelEvalReport runLiveEvalReport(DiagnosticAgentService service, List<EvalCase> cases) {
        String model = valueOrDefault(System.getenv("AI_EVAL_MODEL"), "deepseek-ai/DeepSeek-V4-Pro");
        List<LiveModelEvalReport.Entry> entries = new ArrayList<>();
        for (EvalCase evalCase : cases) {
            long startedAt = System.nanoTime();
            try {
                DiagnosticAgentService.AgentResult result = service.diagnose(
                        evalCase.problem(),
                        evalCase.submission(),
                        evalCase.caseResults(),
                        evalCase.baseline(),
                        Assignment.HintPolicy.L2
                );
                long latencyMs = (System.nanoTime() - startedAt) / 1_000_000;
                SubmissionAnalysisResponse analysis = result.analysis();
                SubmissionAnalysisResponse.AiInvocation invocation = analysis.getAiInvocation();
                boolean fallbackUsed = invocation == null || invocation.isFallbackUsed();
                boolean issueHit = intersects(analysis.getIssueTags(), evalCase.expectedIssueTags());
                boolean fineHit = intersects(analysis.getFineGrainedTags(), evalCase.expectedFineTags());
                boolean evidenceValid = analysis.getEvidenceRefs() != null && !analysis.getEvidenceRefs().isEmpty();
                boolean safetyPassed = !"HIGH".equalsIgnoreCase(analysis.getAnswerLeakRisk());
                entries.add(LiveModelEvalReport.Entry.builder()
                        .caseId(evalCase.name())
                        .model(model)
                        .promptVersion(invocation == null ? "unknown" : invocation.getPromptVersion())
                        .stage("DIAGNOSIS_AGENT")
                        .latencyMs(latencyMs)
                        .status(invocation == null ? "UNKNOWN" : invocation.getStatus())
                        .fallbackUsed(fallbackUsed)
                        .jsonValid(!fallbackUsed)
                        .expectedIssueTagHit(issueHit)
                        .expectedFineTagHit(fineHit)
                        .evidenceValid(evidenceValid)
                        .safetyPassed(safetyPassed)
                        .failureReason(fallbackUsed
                                ? failureReasonFromInvocation(invocation, result.traceSummary(), analysis.getUncertainty())
                                : "NONE")
                        .outputSummary(safe(analysis.getHeadline()) + " | " + safe(analysis.getSummary()))
                        .build());
            } catch (Exception exception) {
                long latencyMs = (System.nanoTime() - startedAt) / 1_000_000;
                entries.add(LiveModelEvalReport.Entry.builder()
                        .caseId(evalCase.name())
                        .model(model)
                        .promptVersion("unknown")
                        .stage("DIAGNOSIS_AGENT")
                        .latencyMs(latencyMs)
                        .status("EXCEPTION")
                        .fallbackUsed(true)
                        .jsonValid(false)
                        .expectedIssueTagHit(false)
                        .expectedFineTagHit(false)
                        .evidenceValid(false)
                        .safetyPassed(false)
                        .failureReason(classifyException(exception))
                        .outputSummary(exception.getClass().getSimpleName() + ": " + exception.getMessage())
                        .build());
            }
        }
        return summarizeReport(model, entries);
    }

    private LiveModelEvalReport summarizeReport(String model, List<LiveModelEvalReport.Entry> entries) {
        return LiveModelEvalReport.builder()
                .model(model)
                .promptVersion("mixed")
                .totalCount(entries.size())
                .completedCount((int) entries.stream().filter(entry -> !Boolean.TRUE.equals(entry.getFallbackUsed())).count())
                .fallbackCount((int) entries.stream().filter(entry -> Boolean.TRUE.equals(entry.getFallbackUsed())).count())
                .timeoutCount((int) entries.stream()
                        .filter(entry -> entry.getFailureReason() != null && entry.getFailureReason().contains("TIMEOUT"))
                        .count())
                .issueTagHitCount((int) entries.stream().filter(entry -> Boolean.TRUE.equals(entry.getExpectedIssueTagHit())).count())
                .fineTagHitCount((int) entries.stream().filter(entry -> Boolean.TRUE.equals(entry.getExpectedFineTagHit())).count())
                .safetyPassedCount((int) entries.stream().filter(entry -> Boolean.TRUE.equals(entry.getSafetyPassed())).count())
                .entries(entries)
                .build();
    }

    private Path writeLiveEvalReport(LiveModelEvalReport report) throws IOException {
        Path reportDir = Path.of("target", "ai-eval-reports");
        Files.createDirectories(reportDir);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path reportPath = reportDir.resolve("live-model-eval-" + timestamp + ".json");
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(reportPath.toFile(), report);
        return reportPath;
    }

    private boolean intersects(List<String> actual, List<String> expected) {
        if (actual == null || expected == null) {
            return false;
        }
        return actual.stream().anyMatch(expected::contains);
    }

    private String failureReasonFromTrace(String traceSummary) {
        String trace = traceSummary == null ? "" : traceSummary;
        if (trace.contains("rule-fallback")) {
            return "RULE_FALLBACK";
        }
        return "UNKNOWN";
    }

    private String failureReasonFromInvocation(SubmissionAnalysisResponse.AiInvocation invocation,
                                               String traceSummary,
                                               String uncertainty) {
        String status = invocation == null ? "" : safe(invocation.getStatus());
        String detail = extractRuntimeFailureReason(uncertainty);
        if (!status.isBlank()) {
            return detail.isBlank() ? status : status + ":" + detail;
        }
        return failureReasonFromTrace(traceSummary);
    }

    private String extractRuntimeFailureReason(String uncertainty) {
        String text = safe(uncertainty);
        String marker = "失败原因：";
        int start = text.indexOf(marker);
        if (start < 0) {
            return "";
        }
        String tail = text.substring(start + marker.length()).trim();
        int end = tail.indexOf('，');
        if (end < 0) {
            end = tail.indexOf('。');
        }
        return end < 0 ? tail : tail.substring(0, end);
    }

    private String classifyException(Exception exception) {
        String text = (exception.getClass().getName() + " " + exception.getMessage()).toLowerCase();
        if (text.contains("timeout")) {
            return "TIMEOUT";
        }
        if (text.contains("insufficient_quota") || text.contains("exceeded your current quota")) {
            return "INSUFFICIENT_QUOTA";
        }
        if (text.contains("429") || text.contains("rate")) {
            return "RATE_LIMITED";
        }
        if (text.contains("has no provider supported") || text.contains("model unsupported")) {
            return "MODEL_UNSUPPORTED";
        }
        return "EXCEPTION";
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
