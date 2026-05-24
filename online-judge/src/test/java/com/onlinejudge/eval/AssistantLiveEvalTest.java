package com.onlinejudge.eval;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlinejudge.classroom.application.CoachAgentService;
import com.onlinejudge.classroom.application.HintSafetyService;
import com.onlinejudge.classroom.domain.Assignment;
import com.onlinejudge.learning.diagnosis.DiagnosisTaxonomy;
import com.onlinejudge.submission.application.AiCodeAssistSupport;
import com.onlinejudge.submission.application.AiReportService;
import com.onlinejudge.submission.application.DiagnosisEvidencePackageBuilder;
import com.onlinejudge.submission.application.DiagnosticAgentService;
import com.onlinejudge.submission.application.ExternalModelAgentRuntime;
import com.onlinejudge.submission.application.ModelDiagnosisBriefBuilder;
import com.onlinejudge.submission.application.ModelOutputValidator;
import com.onlinejudge.submission.application.PromptTemplateRegistry;
import com.onlinejudge.submission.application.RuleSignalAnalyzer;
import com.onlinejudge.submission.application.StandardLibraryPackBuilder;
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
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AssistantLiveEvalTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DiagnosisTaxonomy taxonomy = new DiagnosisTaxonomy();

    @Test
    void assistantEvalFixturesExposeTeacherGradeExpectations() throws IOException {
        List<AssistantEvalFixtureLoader.Fixture> fixtures =
                new AssistantEvalFixtureLoader(objectMapper).loadDefault();

        assertThat(fixtures).hasSizeGreaterThanOrEqualTo(6);
        assertThat(fixtures)
                .allSatisfy(fixture -> {
                    assertThat(fixture.caseId()).isNotBlank();
                    assertThat(fixture.assistantType()).isIn("SUBMISSION_DIAGNOSIS", "COACH_QUESTION", "GROWTH_REPORT");
                    assertThat(fixture.teacherExpectation()).isNotBlank();
                    assertThat(fixture.qualityNotes()).isNotBlank();
                    assertThat(fixture.rubric()).isNotNull();
                    assertNoMojibake(fixture.caseId(), objectMapper.writeValueAsString(fixture));
                    assertThat(fixture.rubric().expectedSignals()).as(fixture.caseId()).isNotEmpty();
                    assertThat(fixture.rubric().forbiddenPhrases()).as(fixture.caseId()).isNotEmpty();
                    if (fixture.isDiagnosis()) {
                        assertThat(fixture.diagnosis()).isNotNull();
                        assertThat(fixture.diagnosis().toSubmission().getSourceCode()).isNotBlank();
                        assertThat(fixture.rubric().expectedIssueTags()).isNotEmpty();
                        assertThat(fixture.rubric().expectedFineGrainedTags()).isNotEmpty();
                    }
                    if (fixture.isCoach()) {
                        assertThat(fixture.coach()).isNotNull();
                        assertThat(fixture.coach().evidenceRefs()).isNotEmpty();
                        assertThat(fixture.rubric().requiredEvidenceRefs()).isNotEmpty();
                    }
                    if (fixture.isGrowthReport()) {
                        assertThat(fixture.growthReport()).isNotNull();
                        assertThat(fixture.growthReport().timeline()).hasSizeGreaterThanOrEqualTo(2);
                    }
                });
    }

    @Test
    void studentHintNegativeFixturesDoNotContainCorruptedForbiddenPhrases() throws IOException {
        Path fixturePath = Path.of(
                "src",
                "test",
                "resources",
                "diagnosis-eval-fixtures",
                "student-hint-negative-cases.json"
        );
        String fixtureJson = Files.readString(fixturePath);

        assertNoMojibake("student-hint-negative-cases", fixtureJson);
        assertThat(fixtureJson).contains("隐藏测试点");
    }

    @Test
    void liveExternalAssistantsProduceComparableReportWhenEnabled() throws IOException {
        String apiKey = System.getenv("AI_EVAL_API_KEY");
        Assumptions.assumeTrue(apiKey != null && !apiKey.isBlank(),
                "Set AI_EVAL_API_KEY to run live external assistant eval.");

        List<AssistantEvalFixtureLoader.Fixture> fixtures =
                new AssistantEvalFixtureLoader(objectMapper).loadDefault().stream()
                        .filter(fixture -> shouldIncludeAssistantType(fixture, System.getenv("AI_EVAL_ASSISTANT_TYPES")))
                        .limit(Math.max(1, (int) longValueOrDefault(System.getenv("AI_EVAL_ASSISTANT_SMOKE_LIMIT"), 3L)))
                        .toList();

        AssistantLiveEvalReport report = runLiveEval(fixtures, apiKey);
        Path reportPath = writeReport(report);

        System.out.println("Assistant live eval report saved to: " + reportPath);
        System.out.println("Assistant live eval summary: total=" + report.getTotalCount()
                + ", completed=" + report.getCompletedCount()
                + ", runtimeFailures=" + report.getRuntimeFailureCount()
                + ", qualityMisses=" + report.getQualityMissCount()
                + ", safetyFailures=" + report.getSafetyFailureCount()
                + ", signalHits=" + report.getExpectedSignalHitCount());

        assertThat(report.getEntries()).hasSize(fixtures.size());
        assertThat(reportPath).exists();
        assertLiveEvalQualityGateIfEnabled(report);
    }

    private AssistantLiveEvalReport runLiveEval(List<AssistantEvalFixtureLoader.Fixture> fixtures,
                                                String apiKey) {
        String model = valueOrDefault(System.getenv("AI_EVAL_MODEL"), "deepseek-ai/DeepSeek-V4-Pro");
        long caseDelayMs = Math.max(0, longValueOrDefault(System.getenv("AI_EVAL_CASE_DELAY_MS"), 0L));
        AiReportService aiReportService = newLiveAiReportService(apiKey);
        DiagnosticAgentService diagnosticAgentService = newDiagnosticAgentService(aiReportService);
        CoachAgentService coachAgentService = newLiveCoachAgentService(apiKey);
        List<AssistantLiveEvalReport.Entry> entries = new ArrayList<>();

        for (int index = 0; index < fixtures.size(); index++) {
            AssistantEvalFixtureLoader.Fixture fixture = fixtures.get(index);
            waitBetweenCases(index, caseDelayMs);
            long startedAt = System.nanoTime();
            try {
                AssistantLiveEvalReport.Entry entry = switch (fixture.assistantType()) {
                    case "SUBMISSION_DIAGNOSIS" -> evaluateDiagnosis(fixture, diagnosticAgentService, model, startedAt);
                    case "COACH_QUESTION" -> evaluateCoach(fixture, coachAgentService, model, startedAt);
                    case "GROWTH_REPORT" -> evaluateGrowthReport(fixture, aiReportService, model, startedAt);
                    default -> throw new IllegalArgumentException("Unknown assistant type: " + fixture.assistantType());
                };
                entries.add(entry);
            } catch (Exception exception) {
                entries.add(exceptionEntry(fixture, model, startedAt, exception));
            }
        }
        return summarize(model, entries);
    }

    private void waitBetweenCases(int index, long caseDelayMs) {
        if (index <= 0 || caseDelayMs <= 0) {
            return;
        }
        try {
            Thread.sleep(caseDelayMs);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Assistant live eval interrupted while waiting between cases.", exception);
        }
    }

    private AssistantLiveEvalReport.Entry evaluateDiagnosis(AssistantEvalFixtureLoader.Fixture fixture,
                                                            DiagnosticAgentService service,
                                                            String model,
                                                            long startedAt) {
        DiagnosticAgentService.AgentResult result = service.diagnose(
                fixture.diagnosis().toProblem(),
                fixture.diagnosis().toSubmission(),
                fixture.diagnosis().toCaseResults(),
                fixture.diagnosis().toBaseline(),
                Assignment.HintPolicy.L2
        );
        SubmissionAnalysisResponse analysis = result.analysis();
        SubmissionAnalysisResponse.AiInvocation invocation = analysis.getAiInvocation();
        boolean fallbackUsed = invocation == null || invocation.isFallbackUsed();
        boolean partialCompleted = invocation != null
                && "MODEL_PARTIAL_COMPLETED".equals(invocation.getStatus());
        boolean signalHit = intersects(analysis.getIssueTags(), fixture.rubric().expectedIssueTags())
                || intersects(analysis.getFineGrainedTags(), fixture.rubric().expectedFineGrainedTags())
                || containsAny(combinedAnalysisText(analysis), fixture.rubric().expectedSignals());
        boolean evidenceValid = containsAny(analysis.getEvidenceRefs(), fixture.rubric().requiredEvidenceRefs())
                || (analysis.getEvidenceRefs() != null && !analysis.getEvidenceRefs().isEmpty());
        boolean safetyPassed = !"HIGH".equalsIgnoreCase(analysis.getAnswerLeakRisk())
                && avoidsForbidden(combinedAnalysisText(analysis), fixture.rubric().forbiddenPhrases());
        return baseEntry(fixture, model, startedAt)
                .promptVersion(invocation == null ? "unknown" : invocation.getPromptVersion())
                .status(invocation == null ? "UNKNOWN" : invocation.getStatus())
                .fallbackUsed(fallbackUsed)
                .completedOutput(!fallbackUsed)
                .expectedSignalHit(signalHit)
                .evidenceValid(evidenceValid)
                .safetyPassed(safetyPassed)
                .teachingActionValid(analysis.getStudentHintPlan() != null || analysis.getLearningInterventionPlan() != null)
                .failureStage(resolveFailureStage(fallbackUsed, partialCompleted, analysis.getUncertainty()))
                .failureReason(resolveFailureReason(fallbackUsed, partialCompleted, invocation,
                        analysis.getUncertainty(), signalHit, safetyPassed))
                .outputSummary(truncate(safe(analysis.getHeadline()) + " | " + safe(analysis.getSummary()), 220))
                .aiBetterThanTeacher(signalHit && evidenceValid ? "AI 输出包含结构化标签和证据，可用于后续系统统计。" : "本条未观察到明显优于老师期望的部分。")
                .teacherBetterThanAi(signalHit ? "老师期望更明确地限定了不要直接给改法。" : "老师期望更准确地定位了核心错因。")
                .iterationSuggestion(iterationSuggestion(fixture.assistantType(), fallbackUsed, signalHit, safetyPassed, ""))
                .build();
    }

    private AssistantLiveEvalReport.Entry evaluateCoach(AssistantEvalFixtureLoader.Fixture fixture,
                                                        CoachAgentService service,
                                                        String model,
                                                        long startedAt) {
        Assignment.HintPolicy policy = parseHintPolicy(fixture.coach().hintPolicy());
        CoachAgentService.CoachDraft fallback = CoachAgentService.CoachDraft.fallback("fixture fallback");
        CoachAgentService.CoachDraft draft;
        if ("FOLLOW_UP".equals(fixture.coach().turnType())) {
            draft = service.generateFollowUpQuestion(
                    fixture.coach().toSubmission(),
                    fixture.coach().toAnalysis(),
                    fixture.coach().primaryTag(),
                    policy,
                    fixture.coach().contextSummary(),
                    fixture.coach().evidenceRefs(),
                    fixture.coach().studentAnswer(),
                    1,
                    fallback
            );
        } else {
            draft = service.generateInitialQuestion(
                    fixture.coach().toSubmission(),
                    fixture.coach().toAnalysis(),
                    fixture.coach().primaryTag(),
                    policy,
                    fixture.coach().contextSummary(),
                    fixture.coach().evidenceRefs(),
                    fallback
            );
        }
        boolean fallbackUsed = !"MODEL".equals(draft.getSource());
        String combined = safe(draft.getQuestion()) + "\n" + safe(draft.getRationale());
        boolean signalHit = containsAny(combined, fixture.rubric().expectedSignals());
        boolean evidenceValid = containsAny(draft.getEvidenceRefs(), fixture.rubric().requiredEvidenceRefs());
        boolean safetyPassed = !"HIGH".equalsIgnoreCase(draft.getAnswerLeakRisk())
                && avoidsForbidden(combined, fixture.rubric().forbiddenPhrases());
        String coachFailureReason = resolveCoachFailureReason(fallbackUsed, draft.getFailureReason(), signalHit, safetyPassed);
        boolean modelAttemptSafetyPassed = safetyPassed && !coachFailureReason.contains("SAFETY_REJECTED");
        return baseEntry(fixture, model, startedAt)
                .promptVersion("coach-question-v1")
                .status(fallbackUsed ? "MODEL_RUNTIME_FALLBACK" : "MODEL_COMPLETED")
                .fallbackUsed(fallbackUsed)
                .completedOutput(!fallbackUsed)
                .expectedSignalHit(signalHit)
                .evidenceValid(evidenceValid)
                .safetyPassed(modelAttemptSafetyPassed)
                .teachingActionValid(combined.contains("?") || combined.contains("？"))
                .failureStage(fallbackUsed ? "COACH_QUESTION" : "NONE")
                .failureReason(coachFailureReason)
                .outputSummary(truncate(safe(draft.getQuestion()), 220))
                .aiBetterThanTeacher(signalHit && safetyPassed ? "AI 追问保留了苏格拉底式引导，没有直接给答案。" : "本条未观察到明显优于老师期望的部分。")
                .teacherBetterThanAi(signalHit ? "老师期望更清楚地限定了追问边界。" : "老师追问更能指向当前学生回答里的缺口。")
                .iterationSuggestion(iterationSuggestion(fixture.assistantType(), fallbackUsed, signalHit, modelAttemptSafetyPassed, coachFailureReason))
                .build();
    }

    private AssistantLiveEvalReport.Entry evaluateGrowthReport(AssistantEvalFixtureLoader.Fixture fixture,
                                                               AiReportService service,
                                                               String model,
                                                               long startedAt) {
        String markdown = service.enhanceGrowthReportMarkdown(
                fixture.growthReport().toProblem(),
                fixture.growthReport().timeline(),
                fixture.growthReport().fallbackMarkdown()
        );
        String visibleMarkdown = stripGrowthReportFailureMarker(markdown);
        boolean fallbackUsed = visibleMarkdown.isBlank()
                || visibleMarkdown.equals(safe(fixture.growthReport().fallbackMarkdown()).trim());
        boolean signalHit = containsAny(visibleMarkdown, fixture.rubric().expectedSignals());
        boolean evidenceValid = fixture.growthReport().timeline().stream()
                .map(item -> String.valueOf(item.getOrDefault("submissionId", "")))
                .anyMatch(id -> visibleMarkdown != null && visibleMarkdown.contains(id));
        boolean safetyPassed = avoidsForbidden(visibleMarkdown, fixture.rubric().forbiddenPhrases());
        return baseEntry(fixture, model, startedAt)
                .promptVersion("growth-report-markdown-v1")
                .status(fallbackUsed ? "MODEL_RUNTIME_FALLBACK" : "MODEL_COMPLETED")
                .fallbackUsed(fallbackUsed)
                .completedOutput(!fallbackUsed)
                .expectedSignalHit(signalHit)
                .evidenceValid(evidenceValid)
                .safetyPassed(safetyPassed)
                .teachingActionValid(signalHit && containsAny(markdown, List.of("下一步", "复盘", "建议", "行动", "练习")))
                .failureStage(fallbackUsed ? "GROWTH_REPORT" : "NONE")
                .failureReason(resolveFailureReason(fallbackUsed, false, null,
                        extractGrowthReportFailureReason(markdown, fixture.growthReport().fallbackMarkdown()),
                        signalHit, safetyPassed))
                .outputSummary(truncate(visibleMarkdown, 260))
                .aiBetterThanTeacher(signalHit && evidenceValid ? "AI 能把多次提交串成学习轨迹，适合教师快速浏览。" : "本条未观察到明显优于老师期望的部分。")
                .teacherBetterThanAi(signalHit ? "老师期望对教师介入信号更明确。" : "老师期望更能抓住连续提交背后的学习状态。")
                .iterationSuggestion(iterationSuggestion(fixture.assistantType(), fallbackUsed, signalHit, safetyPassed, ""))
                .build();
    }

    private AssistantLiveEvalReport.Entry.EntryBuilder baseEntry(
            AssistantEvalFixtureLoader.Fixture fixture,
            String model,
            long startedAt) {
        return AssistantLiveEvalReport.Entry.builder()
                .caseId(fixture.caseId())
                .assistantType(fixture.assistantType())
                .model(model)
                .latencyMs((System.nanoTime() - startedAt) / 1_000_000)
                .teacherExpectation(fixture.teacherExpectation());
    }

    private AssistantLiveEvalReport.Entry exceptionEntry(AssistantEvalFixtureLoader.Fixture fixture,
                                                         String model,
                                                         long startedAt,
                                                         Exception exception) {
        return baseEntry(fixture, model, startedAt)
                .promptVersion("unknown")
                .status("EXCEPTION")
                .fallbackUsed(true)
                .completedOutput(false)
                .expectedSignalHit(false)
                .evidenceValid(false)
                .safetyPassed(false)
                .teachingActionValid(false)
                .failureStage(fixture.assistantType())
                .failureReason(classifyException(exception))
                .outputSummary(truncate(exception.getClass().getSimpleName() + ": " + exception.getMessage(), 220))
                .aiBetterThanTeacher("外部模型调用未完成，不能判断。")
                .teacherBetterThanAi("老师期望仍可作为离线质量基线。")
                .iterationSuggestion("先处理外部模型调用稳定性或失败归因，再评估教学质量。")
                .build();
    }

    private DiagnosticAgentService newDiagnosticAgentService(AiReportService aiReportService) {
        return new DiagnosticAgentService(
                new DiagnosisEvidencePackageBuilder(),
                new RuleSignalAnalyzer(),
                aiReportService,
                new HintSafetyService(null, objectMapper, taxonomy),
                taxonomy
        );
    }

    private AiReportService newLiveAiReportService(String apiKey) {
        ExternalModelAgentRuntime runtime = new ExternalModelAgentRuntime(
                new ModelDiagnosisBriefBuilder(),
                new StandardLibraryPackBuilder(taxonomy),
                new PromptTemplateRegistry(),
                new ModelOutputValidator()
        );
        AiReportService service = new AiReportService(objectMapper, new AiCodeAssistSupport(), runtime);
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "apiKey", apiKey);
        ReflectionTestUtils.setField(service, "baseUrl", valueOrDefault(System.getenv("AI_EVAL_BASE_URL"), "https://api-inference.modelscope.cn/v1"));
        ReflectionTestUtils.setField(service, "model", valueOrDefault(System.getenv("AI_EVAL_MODEL"), "deepseek-ai/DeepSeek-V4-Pro"));
        ReflectionTestUtils.setField(service, "timeoutSeconds", longValueOrDefault(System.getenv("AI_EVAL_TIMEOUT_SECONDS"), 35L));
        ReflectionTestUtils.setField(service, "externalRuntimeEnabled",
                Boolean.parseBoolean(valueOrDefault(System.getenv("AI_EVAL_EXTERNAL_RUNTIME_ENABLED"), "true")));
        ReflectionTestUtils.setField(service, "externalRuntimeMode",
                valueOrDefault(System.getenv("AI_EVAL_EXTERNAL_RUNTIME_MODE"), "staged"));
        ReflectionTestUtils.setField(service, "maxOutputTokens", (int) longValueOrDefault(System.getenv("AI_EVAL_MAX_OUTPUT_TOKENS"), 900L));
        ReflectionTestUtils.setField(service, "streamEnabled",
                Boolean.parseBoolean(valueOrDefault(System.getenv("AI_STREAM_ENABLED"), "true")));
        ReflectionTestUtils.setField(service, "streamFallbackEnabled",
                Boolean.parseBoolean(valueOrDefault(System.getenv("AI_STREAM_FALLBACK_ENABLED"), "true")));
        return service;
    }

    private CoachAgentService newLiveCoachAgentService(String apiKey) {
        CoachAgentService service = new CoachAgentService(objectMapper, taxonomy);
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "apiKey", apiKey);
        ReflectionTestUtils.setField(service, "baseUrl", valueOrDefault(System.getenv("AI_EVAL_BASE_URL"), "https://api-inference.modelscope.cn/v1"));
        ReflectionTestUtils.setField(service, "model", valueOrDefault(System.getenv("AI_EVAL_MODEL"), "deepseek-ai/DeepSeek-V4-Pro"));
        ReflectionTestUtils.setField(service, "timeoutSeconds", longValueOrDefault(System.getenv("AI_EVAL_TIMEOUT_SECONDS"), 35L));
        ReflectionTestUtils.setField(service, "streamEnabled",
                Boolean.parseBoolean(valueOrDefault(System.getenv("AI_STREAM_ENABLED"), "true")));
        ReflectionTestUtils.setField(service, "streamFallbackEnabled",
                Boolean.parseBoolean(valueOrDefault(System.getenv("AI_STREAM_FALLBACK_ENABLED"), "true")));
        return service;
    }

    private AssistantLiveEvalReport summarize(String model, List<AssistantLiveEvalReport.Entry> entries) {
        return AssistantLiveEvalReport.builder()
                .model(model)
                .totalCount(entries.size())
                .completedCount((int) entries.stream().filter(entry -> Boolean.TRUE.equals(entry.getCompletedOutput())).count())
                .runtimeFailureCount((int) entries.stream().filter(entry -> !Boolean.TRUE.equals(entry.getCompletedOutput())).count())
                .qualityMissCount((int) entries.stream()
                        .filter(entry -> Boolean.TRUE.equals(entry.getCompletedOutput()))
                        .filter(entry -> !Boolean.TRUE.equals(entry.getExpectedSignalHit()) || !Boolean.TRUE.equals(entry.getEvidenceValid()))
                        .count())
                .safetyFailureCount((int) entries.stream().filter(entry -> !Boolean.TRUE.equals(entry.getSafetyPassed())).count())
                .expectedSignalHitCount((int) entries.stream().filter(entry -> Boolean.TRUE.equals(entry.getExpectedSignalHit())).count())
                .evidenceValidCount((int) entries.stream().filter(entry -> Boolean.TRUE.equals(entry.getEvidenceValid())).count())
                .entries(entries)
                .build();
    }

    private void assertLiveEvalQualityGateIfEnabled(AssistantLiveEvalReport report) {
        if (!Boolean.parseBoolean(valueOrDefault(System.getenv("AI_EVAL_ENFORCE_THRESHOLDS"), "false"))) {
            return;
        }
        AssistantLiveEvalQualityGate.Thresholds thresholds = new AssistantLiveEvalQualityGate.Thresholds(
                doubleValueOrDefault(System.getenv("AI_EVAL_MIN_SIGNAL_HIT_RATE"), 0.60),
                doubleValueOrDefault(System.getenv("AI_EVAL_MIN_EVIDENCE_VALID_RATE"), 0.80),
                doubleValueOrDefault(System.getenv("AI_EVAL_MIN_SAFETY_PASS_RATE"), 1.00),
                doubleValueOrDefault(System.getenv("AI_EVAL_MAX_FALLBACK_RATE"), 0.35)
        );
        List<String> violations = AssistantLiveEvalQualityGate.evaluate(report, thresholds);
        assertThat(violations)
                .as("assistant live eval quality gate violations")
                .isEmpty();
    }

    private Path writeReport(AssistantLiveEvalReport report) throws IOException {
        Path reportDir = Path.of("target", "ai-eval-reports");
        Files.createDirectories(reportDir);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path reportPath = reportDir.resolve("assistant-live-eval-" + timestamp + ".json");
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(reportPath.toFile(), report);
        return reportPath;
    }

    private boolean intersects(List<String> actual, List<String> expected) {
        if (actual == null || expected == null) {
            return false;
        }
        return actual.stream().anyMatch(expected::contains);
    }

    private boolean containsAny(List<String> actual, List<String> expected) {
        if (actual == null || expected == null || expected.isEmpty()) {
            return false;
        }
        return actual.stream().anyMatch(expected::contains);
    }

    private boolean containsAny(String actual, List<String> expected) {
        if (actual == null || expected == null || expected.isEmpty()) {
            return false;
        }
        String normalized = actual.toLowerCase(Locale.ROOT);
        return expected.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.toLowerCase(Locale.ROOT))
                .anyMatch(normalized::contains);
    }

    private boolean shouldIncludeAssistantType(AssistantEvalFixtureLoader.Fixture fixture, String filter) {
        if (filter == null || filter.isBlank()) {
            return true;
        }
        List<String> allowed = List.of(filter.split(",")).stream()
                .map(value -> value.trim().toUpperCase(Locale.ROOT))
                .filter(value -> !value.isBlank())
                .toList();
        return allowed.isEmpty() || allowed.contains(fixture.assistantType());
    }

    private boolean avoidsForbidden(String actual, List<String> forbidden) {
        if (forbidden == null || forbidden.isEmpty()) {
            return true;
        }
        String normalized = actual == null ? "" : actual.toLowerCase(Locale.ROOT);
        return forbidden.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.toLowerCase(Locale.ROOT))
                .noneMatch(normalized::contains);
    }

    private void assertNoMojibake(String caseId, String text) {
        List<String> mojibakeMarkers = List.of(
                "闅愯棌",
                "瀛︾敓",
                "鑰佸笀",
                "涓€",
                "鈥",
                "鐐?",
                "锛?",
                "銆"
        );
        assertThat(text)
                .as("fixture %s must not contain corrupted Chinese text", caseId)
                .doesNotContain(mojibakeMarkers.toArray(String[]::new));
    }

    private String combinedAnalysisText(SubmissionAnalysisResponse analysis) {
        if (analysis == null) {
            return "";
        }
        return String.join("\n",
                safe(analysis.getHeadline()),
                safe(analysis.getSummary()),
                safe(analysis.getStudentHint()),
                safe(analysis.getTeacherNote()),
                safe(analysis.getUncertainty()),
                analysis.getStudentHintPlan() == null ? "" : safe(analysis.getStudentHintPlan().getProblemType()),
                analysis.getStudentHintPlan() == null ? "" : safe(analysis.getStudentHintPlan().getNextAction()),
                analysis.getStudentHintPlan() == null ? "" : safe(analysis.getStudentHintPlan().getCoachQuestion())
        );
    }

    private String resolveFailureReason(boolean fallbackUsed,
                                        boolean partialCompleted,
                                        SubmissionAnalysisResponse.AiInvocation invocation,
                                        String uncertainty,
                                        boolean signalHit,
                                        boolean safetyPassed) {
        if (fallbackUsed) {
            String status = invocation == null ? "MODEL_RUNTIME_FALLBACK" : safe(invocation.getStatus());
            String detail = extractRuntimeFailureReason(uncertainty);
            return detail.isBlank() ? status : status + ":" + detail;
        }
        if (partialCompleted) {
            String detail = extractRuntimeFailureReason(uncertainty);
            return detail.isBlank() ? "MODEL_PARTIAL_COMPLETED" : "MODEL_PARTIAL_COMPLETED:" + detail;
        }
        if (!safetyPassed) {
            return "SAFETY_REJECTED";
        }
        if (!signalHit) {
            return "QUALITY_MISS";
        }
        return "NONE";
    }

    private String resolveCoachFailureReason(boolean fallbackUsed,
                                             String failureReason,
                                             boolean signalHit,
                                             boolean safetyPassed) {
        if (fallbackUsed) {
            String detail = safe(failureReason);
            return detail.isBlank() ? "MODEL_RUNTIME_FALLBACK" : "MODEL_RUNTIME_FALLBACK:" + detail;
        }
        if (!safetyPassed) {
            return "SAFETY_REJECTED";
        }
        if (!signalHit) {
            return "QUALITY_MISS";
        }
        return "NONE";
    }

    private String extractRuntimeFailureReason(String uncertainty) {
        String text = safe(uncertainty);
        List<String> knownReasons = List.of(
                "INSUFFICIENT_QUOTA",
                "RATE_LIMITED",
                "BUDGET_GUARD_OPEN",
                "TIMEOUT",
                "MODEL_UNSUPPORTED",
                "EMPTY_RESPONSE",
                "INVALID_JSON",
                "INVALID_TAG",
                "INVALID_EVIDENCE_REF",
                "SAFETY_RISK",
                "API_ERROR",
                "UNKNOWN_ERROR"
        );
        for (String reason : knownReasons) {
            if (text.contains(reason)) {
                return reason;
            }
        }
        return "";
    }

    private String extractGrowthReportFailureReason(String markdown, String fallbackMarkdown) {
        String text = safe(markdown);
        String fallback = safe(fallbackMarkdown);
        if (text.isBlank() || !text.equals(fallback)) {
            return "";
        }
        String marker = "<!-- AI_FAILURE:";
        int start = text.indexOf(marker);
        if (start < 0) {
            return "";
        }
        int end = text.indexOf("-->", start);
        if (end < 0) {
            return "";
        }
        return text.substring(start + marker.length(), end).trim();
    }

    private String stripGrowthReportFailureMarker(String markdown) {
        String text = safe(markdown);
        return text.replaceAll("(?s)\\n*<!-- AI_FAILURE:.*?-->\\s*$", "").trim();
    }

    private String resolveFailureStage(boolean fallbackUsed, boolean partialCompleted, String uncertainty) {
        if (fallbackUsed || partialCompleted) {
            return extractRuntimeFailureStage(uncertainty);
        }
        return "NONE";
    }

    private String extractRuntimeFailureStage(String uncertainty) {
        String text = safe(uncertainty);
        List<String> knownStages = List.of(
                "DIAGNOSIS_JUDGE",
                "TEACHING_HINT",
                "SUBMISSION_ANALYSIS",
                "UNKNOWN_STAGE"
        );
        for (String stage : knownStages) {
            if (text.contains(stage)) {
                return stage;
            }
        }
        return text.isBlank() ? "NONE" : "UNKNOWN_STAGE";
    }

    private String classifyException(Exception exception) {
        String text = (exception.getClass().getName() + " " + exception.getMessage()).toLowerCase(Locale.ROOT);
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
        if (text.contains("json")) {
            return "JSON_INVALID";
        }
        return "EXCEPTION";
    }

    private String iterationSuggestion(String assistantType,
                                       boolean fallbackUsed,
                                       boolean signalHit,
                                       boolean safetyPassed,
                                       String failureReason) {
        if (isBudgetLimitedFailure(failureReason)) {
            return "优先处理外部模型预算、限流、调用间隔或模型路由；本条不应优先归因到 prompt 或教学策略。";
        }
        if (failureReason != null && failureReason.contains("SAFETY_REJECTED")) {
            return "优先收紧 " + assistantType + " 的 prompt，降低直接引用代码片段、直接改法或过度提示的概率。";
        }
        if (fallbackUsed) {
            return "优先改进外部模型调用稳定性、阶段失败归因或降低单样本调用成本。";
        }
        if (!safetyPassed) {
            return "优先收紧 " + assistantType + " 的安全提示和泄题拦截规则。";
        }
        if (!signalHit) {
            return switch (assistantType) {
                case "SUBMISSION_DIAGNOSIS" -> "优先调整诊断标准库裁剪和 diagnosis prompt，让模型必须命中老师期望的证据链。";
                case "COACH_QUESTION" -> "优先调整 Coach prompt，让模型追问学生当前回答中的具体缺口。";
                case "GROWTH_REPORT" -> "优先增强轨迹输入和成长报告 rubric，要求显式总结进步、卡点和下一步。";
                default -> "优先补充该助手的评分点和提示词约束。";
            };
        }
        return "该样本可作为正向基线，后续扩展同类变体。";
    }

    private boolean isBudgetLimitedFailure(String failureReason) {
        String text = safe(failureReason);
        return text.contains("INSUFFICIENT_QUOTA")
                || text.contains("RATE_LIMITED")
                || text.contains("BUDGET_GUARD_OPEN");
    }

    private Assignment.HintPolicy parseHintPolicy(String value) {
        if (value == null || value.isBlank()) {
            return Assignment.HintPolicy.L2;
        }
        return Assignment.HintPolicy.valueOf(value);
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

    private double doubleValueOrDefault(String value, double fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private String truncate(String value, int maxLength) {
        String normalized = safe(value).replace("\r\n", "\n").replace('\r', '\n').trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength - 15) + "... [truncated]";
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
