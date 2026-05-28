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
import com.onlinejudge.submission.application.ExternalModelBudgetGuard;
import com.onlinejudge.submission.application.ExternalModelRoute;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AssistantLiveEvalTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DiagnosisTaxonomy taxonomy = new DiagnosisTaxonomy();
    private final ExternalModelBudgetGuard liveEvalBudgetGuard = new ExternalModelBudgetGuard();

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
                        assertThat(fixture.rubric().requiredEvidenceRefs()).isNotEmpty();
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
        long complexDiagnosisCount = fixtures.stream()
                .filter(AssistantEvalFixtureLoader.Fixture::isDiagnosis)
                .filter(fixture -> lineCount(fixture.diagnosis().toSubmission().getSourceCode()) >= 20)
                .count();
        assertThat(complexDiagnosisCount)
                .as("live eval must include enough long-code diagnosis cases, not only toy snippets")
                .isGreaterThanOrEqualTo(10);
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
    void diagnosisSafetyScanUsesStudentVisibleTextNotTeacherReminder() {
        SubmissionAnalysisResponse analysis = SubmissionAnalysisResponse.builder()
                .headline("多组之间状态没有独立")
                .summary("第二组输出仍带着第一组的状态。")
                .studentHint("先跟踪第二组开始前 cur 和 best 的值。")
                .teacherNote("教师可以提醒不要给完整代码，只要求学生解释状态污染。")
                .answerLeakRisk("LOW")
                .studentHintPlan(SubmissionAnalysisResponse.StudentHintPlan.builder()
                        .problemType("状态重置")
                        .evidenceAnchor("judge:first_failed_case:1")
                        .nextAction("列出每组开始前状态变量的值。")
                        .coachQuestion("第二组开始时 best 的值从哪里来？")
                        .teachingAction("TRACE_STATE")
                        .build())
                .learningInterventionPlan(SubmissionAnalysisResponse.LearningInterventionPlan.builder()
                        .goal("确认每组数据是否独立。")
                        .studentTask("手推两组样例的状态变化。")
                        .checkQuestion("第二组的初始状态应该是多少？")
                        .build())
                .build();

        String studentVisibleText = studentVisibleAnalysisText(analysis);

        assertThat(combinedAnalysisText(analysis)).contains("完整代码");
        assertThat(studentVisibleText).doesNotContain("完整代码");
        assertThat(avoidsForbidden(studentVisibleText, List.of("完整代码"))).isTrue();
        assertThat(resolveSafetyTrigger(analysis.getAnswerLeakRisk(), studentVisibleText, List.of("完整代码"))).isBlank();
    }

    @Test
    void budgetGuardFailureReasonTakesPrecedenceOverUnderlyingQuota() {
        String uncertainty = "失败阶段：DIAGNOSIS_AND_TEACHING；失败原因：BUDGET_GUARD_OPEN，BUDGET_GUARD_OPEN:INSUFFICIENT_QUOTA";

        assertThat(extractRuntimeFailureReason(uncertainty)).isEqualTo("BUDGET_GUARD_OPEN");
        assertThat(extractRuntimeFailureStage(uncertainty)).isEqualTo("DIAGNOSIS_AND_TEACHING");
    }

    @Test
    void liveEvalReportWritesUtf8JsonThatCanBeReadBack() throws IOException {
        AssistantLiveEvalReport report = AssistantLiveEvalReport.builder()
                .model("deepseek-ai/DeepSeek-V4-Pro")
                .runtimeMode("single-call")
                .sampleProfile(AssistantLiveEvalReport.SampleProfile.builder()
                        .totalCount(1)
                        .diagnosisCount(1)
                        .longCodeDiagnosisCount(1)
                        .assistantTypes(List.of("SUBMISSION_DIAGNOSIS"))
                        .caseIds(List.of("utf8-report-smoke"))
                        .build())
                .routeProfile(AssistantLiveEvalReport.RouteProfile.builder()
                        .primaryProvider("ModelScope")
                        .primaryBaseUrl("https://api-inference.modelscope.cn/v1")
                        .primaryModel("deepseek-ai/DeepSeek-V4-Pro")
                        .fallbackConfigured(false)
                        .configuredRouteCount(1)
                        .build())
                .routeOutcomes(List.of(AssistantLiveEvalReport.RouteOutcome.builder()
                        .routeKey("PRIMARY|ModelScope|deepseek-ai/DeepSeek-V4-Pro")
                        .routeRole("PRIMARY")
                        .provider("ModelScope")
                        .model("deepseek-ai/DeepSeek-V4-Pro")
                        .totalCount(1)
                        .completedCount(1)
                        .runtimeFailureCount(0)
                        .failureReasonCounts(Map.of())
                        .build()))
                .totalCount(1)
                .completedCount(1)
                .runtimeFailureCount(0)
                .qualityMissCount(0)
                .safetyFailureCount(0)
                .expectedSignalHitCount(1)
                .evidenceValidCount(1)
                .goalSnapshot(AssistantLiveEvalReport.GoalSnapshot.builder()
                        .phase("第一阶段：外部模型诊断可用")
                        .externalCompletionRate(1.0)
                        .runtimeFailureRate(0.0)
                        .signalHitRate(1.0)
                        .evidenceValidRate(1.0)
                        .safetyPassRate(1.0)
                        .teachingActionValidRate(1.0)
                        .goalGaps(List.of())
                        .coverageGaps(List.of())
                        .nextOptimizationFocus("NEXT_STAGE_STUDENT_TEACHER_OUTCOMES")
                        .nextAction("继续评估学生下一次提交是否改善。")
                        .build())
                .evaluationProfile(AssistantLiveEvalReport.EvaluationProfile.builder()
                        .accuracy(AssistantLiveEvalReport.AccuracyProfile.builder()
                                .evaluatedCount(1)
                                .completedOutputCount(1)
                                .expectedSignalHitCount(1)
                                .evidenceValidCount(1)
                                .teachingActionValidCount(1)
                                .safetyFailureCount(0)
                                .signalHitRate(1.0)
                                .evidenceValidRate(1.0)
                                .teachingActionValidRate(1.0)
                                .safetyPassRate(1.0)
                                .build())
                        .speed(AssistantLiveEvalReport.SpeedProfile.builder()
                                .measuredCount(1)
                                .averageLatencyMs(1200L)
                                .p50LatencyMs(1200L)
                                .p90LatencyMs(1200L)
                                .p95LatencyMs(1200L)
                                .maxLatencyMs(1200L)
                                .targetP95LatencyMs(25_000L)
                                .targetMaxLatencyMs(45_000L)
                                .slowCaseIds(List.of())
                                .build())
                        .stability(AssistantLiveEvalReport.StabilityProfile.builder()
                                .totalCount(1)
                                .completedCount(1)
                                .runtimeFailureCount(0)
                                .fallbackCount(0)
                                .localFallbackCount(0)
                                .completedOutputRate(1.0)
                                .runtimeFailureRate(0.0)
                                .fallbackRate(0.0)
                                .failureReasonCounts(Map.of())
                                .routeFailureCounts(Map.of())
                                .build())
                        .educationalEffectiveness(AssistantLiveEvalReport.EducationalEffectivenessProfile.builder()
                                .studentImprovementMeasured(false)
                                .measuredStudentOutcomeCount(0)
                                .teachingActionValidRate(1.0)
                                .evidenceValidRate(1.0)
                                .safetyPassRate(1.0)
                                .proxyMetrics(List.of("teachingActionValidRate", "evidenceValidRate", "safetyPassRate"))
                                .nextMeasurementGaps(List.of("studentNextSubmissionOutcome missing"))
                                .build())
                        .dimensionGaps(List.of("educationalEffectiveness.studentImprovementMeasured=false"))
                        .overallVerdict("NEEDS_ITERATION")
                        .build())
                .entries(List.of(AssistantLiveEvalReport.Entry.builder()
                        .caseId("utf8-report-smoke")
                        .assistantType("SUBMISSION_DIAGNOSIS")
                        .actualProvider("ModelScope")
                        .actualModel("deepseek-ai/DeepSeek-V4-Pro")
                        .routeRole("PRIMARY")
                        .status("MODEL_COMPLETED")
                        .completedOutput(true)
                        .teacherExpectation("老师期望：定位输入读取次数，不直接给完整代码。")
                        .outputSummary("AI 命中核心错因，并给出可执行追问。")
                        .build()))
                .build();

        Path reportPath = writeReport(report);
        String reportJson = Files.readString(reportPath, StandardCharsets.UTF_8);
        AssistantLiveEvalReport roundTrip = objectMapper.readValue(reportJson, AssistantLiveEvalReport.class);

        assertNoMojibake("assistant-live-eval-report", reportJson);
        assertThat(roundTrip.getGoalSnapshot().getPhase()).isEqualTo("第一阶段：外部模型诊断可用");
        assertThat(roundTrip.getEntries().get(0).getActualProvider()).isEqualTo("ModelScope");
        assertThat(roundTrip.getEntries().get(0).getActualModel()).isEqualTo("deepseek-ai/DeepSeek-V4-Pro");
        assertThat(roundTrip.getEntries().get(0).getRouteRole()).isEqualTo("PRIMARY");
        assertThat(roundTrip.getRouteOutcomes()).hasSize(1);
        assertThat(roundTrip.getRouteOutcomes().get(0).getRouteKey()).isEqualTo("PRIMARY|ModelScope|deepseek-ai/DeepSeek-V4-Pro");
        assertThat(roundTrip.getEvaluationProfile().getAccuracy().getSignalHitRate()).isEqualTo(1.0);
        assertThat(roundTrip.getEvaluationProfile().getSpeed().getP95LatencyMs()).isEqualTo(1200L);
        assertThat(roundTrip.getEvaluationProfile().getStability().getCompletedOutputRate()).isEqualTo(1.0);
        assertThat(roundTrip.getEvaluationProfile().getEducationalEffectiveness().getStudentImprovementMeasured()).isFalse();
        assertThat(roundTrip.getEntries().get(0).getTeacherExpectation()).contains("老师期望");
    }

    @Test
    void diagnosisRouteAttributionDistinguishesPrimaryFallbackAndLocalFallback() {
        SubmissionAnalysisResponse.AiInvocation primary = SubmissionAnalysisResponse.AiInvocation.builder()
                .provider("ModelScope")
                .model("deepseek-ai/DeepSeek-V4-Pro")
                .fallbackUsed(false)
                .build();
        SubmissionAnalysisResponse.AiInvocation fallbackSameModel = SubmissionAnalysisResponse.AiInvocation.builder()
                .provider("FallbackProvider")
                .model("deepseek-ai/DeepSeek-V4-Pro")
                .fallbackUsed(false)
                .build();
        SubmissionAnalysisResponse.AiInvocation fallbackDifferentModel = SubmissionAnalysisResponse.AiInvocation.builder()
                .provider("FallbackProvider")
                .model("fallback-model")
                .fallbackUsed(false)
                .build();
        SubmissionAnalysisResponse.AiInvocation localFallback = SubmissionAnalysisResponse.AiInvocation.builder()
                .provider("LOCAL_RULES")
                .model("rule-fallback")
                .fallbackUsed(true)
                .build();
        SubmissionAnalysisResponse.AiInvocation primaryRuntimeFailure = SubmissionAnalysisResponse.AiInvocation.builder()
                .provider("ModelScope")
                .model("deepseek-ai/DeepSeek-V4-Pro")
                .status("MODEL_RUNTIME_FALLBACK")
                .fallbackUsed(true)
                .build();

        assertThat(resolveDiagnosisRouteRole(primary, "deepseek-ai/DeepSeek-V4-Pro")).isEqualTo("PRIMARY");
        assertThat(resolveDiagnosisRouteRole(fallbackSameModel, "deepseek-ai/DeepSeek-V4-Pro")).isEqualTo("FALLBACK");
        assertThat(resolveDiagnosisRouteRole(fallbackDifferentModel, "deepseek-ai/DeepSeek-V4-Pro")).isEqualTo("FALLBACK");
        assertThat(resolveDiagnosisRouteRole(localFallback, "deepseek-ai/DeepSeek-V4-Pro")).isEqualTo("LOCAL_FALLBACK");
        assertThat(resolveActualProvider(localFallback)).isBlank();
        assertThat(resolveActualModel(localFallback, "deepseek-ai/DeepSeek-V4-Pro")).isBlank();
        assertThat(resolveDiagnosisRouteRole(primaryRuntimeFailure, "deepseek-ai/DeepSeek-V4-Pro")).isEqualTo("PRIMARY");
        assertThat(resolveActualProvider(primaryRuntimeFailure)).isEqualTo("ModelScope");
        assertThat(resolveActualModel(primaryRuntimeFailure, "deepseek-ai/DeepSeek-V4-Pro")).isEqualTo("deepseek-ai/DeepSeek-V4-Pro");
    }

    @Test
    void routeOutcomeSummarySeparatesRoutePoolFromSingleFallback() {
        List<AssistantLiveEvalReport.Entry> entries = List.of(
                AssistantLiveEvalReport.Entry.builder()
                        .routeRole("ROUTE_POOL")
                        .actualProvider("ExtraProvider")
                        .actualModel("extra-model")
                        .completedOutput(true)
                        .build(),
                AssistantLiveEvalReport.Entry.builder()
                        .routeRole("FALLBACK")
                        .actualProvider("FallbackProvider")
                        .actualModel("fallback-model")
                        .completedOutput(false)
                        .failureReason("MODEL_RUNTIME_FALLBACK:RATE_LIMITED")
                        .build()
        );

        List<AssistantLiveEvalReport.RouteOutcome> outcomes = buildRouteOutcomes(entries);

        assertThat(outcomes)
                .extracting(AssistantLiveEvalReport.RouteOutcome::getRouteKey)
                .containsExactly(
                        "ROUTE_POOL|ExtraProvider|extra-model",
                        "FALLBACK|FallbackProvider|fallback-model"
                );
        assertThat(outcomes.get(0).getCompletedCount()).isEqualTo(1);
        assertThat(outcomes.get(1).getFailureReasonCounts()).containsEntry("RATE_LIMITED", 1);
    }

    @Test
    void routeOutcomeSummarySeparatesPrimaryFallbackAndLocalFallback() {
        List<AssistantLiveEvalReport.Entry> entries = List.of(
                AssistantLiveEvalReport.Entry.builder()
                        .routeRole("PRIMARY")
                        .actualProvider("ModelScope")
                        .actualModel("primary-model")
                        .completedOutput(true)
                        .build(),
                AssistantLiveEvalReport.Entry.builder()
                        .routeRole("FALLBACK")
                        .actualProvider("FallbackProvider")
                        .actualModel("fallback-model")
                        .completedOutput(true)
                        .build(),
                AssistantLiveEvalReport.Entry.builder()
                        .routeRole("LOCAL_FALLBACK")
                        .completedOutput(false)
                        .failureReason("MODEL_RUNTIME_FALLBACK:BUDGET_GUARD_OPEN")
                        .build(),
                AssistantLiveEvalReport.Entry.builder()
                        .routeRole("PRIMARY")
                        .actualProvider("ModelScope")
                        .actualModel("primary-model")
                        .completedOutput(false)
                        .failureReason("MODEL_RUNTIME_FALLBACK:INSUFFICIENT_QUOTA")
                        .build()
        );

        List<AssistantLiveEvalReport.RouteOutcome> outcomes = buildRouteOutcomes(entries);

        assertThat(outcomes)
                .extracting(AssistantLiveEvalReport.RouteOutcome::getRouteKey)
                .containsExactly(
                        "PRIMARY|ModelScope|primary-model",
                        "FALLBACK|FallbackProvider|fallback-model",
                        "LOCAL_FALLBACK|LOCAL_RULES|rule-fallback"
                );
        assertThat(outcomes.get(0).getTotalCount()).isEqualTo(2);
        assertThat(outcomes.get(0).getCompletedCount()).isEqualTo(1);
        assertThat(outcomes.get(0).getRuntimeFailureCount()).isEqualTo(1);
        assertThat(outcomes.get(0).getFailureReasonCounts()).containsEntry("INSUFFICIENT_QUOTA", 1);
        assertThat(outcomes.get(1).getCompletedCount()).isEqualTo(1);
        assertThat(outcomes.get(2).getFailureReasonCounts()).containsEntry("BUDGET_GUARD_OPEN", 1);
    }

    @Test
    void liveExternalAssistantsProduceComparableReportWhenEnabled() throws IOException {
        String apiKey = System.getenv("AI_EVAL_API_KEY");
        Assumptions.assumeTrue(apiKey != null && !apiKey.isBlank(),
                "Set AI_EVAL_API_KEY to run live external assistant eval.");

        List<AssistantEvalFixtureLoader.Fixture> fixtures =
                new AssistantEvalFixtureLoader(objectMapper).loadDefault().stream()
                        .filter(fixture -> shouldIncludeAssistantType(fixture, System.getenv("AI_EVAL_ASSISTANT_TYPES")))
                        .filter(fixture -> shouldIncludeCaseId(fixture, System.getenv("AI_EVAL_CASE_IDS")))
                        .limit(Math.max(1, (int) longValueOrDefault(System.getenv("AI_EVAL_ASSISTANT_SMOKE_LIMIT"), 3L)))
                        .toList();

        AssistantLiveEvalReport report = runLiveEval(fixtures, apiKey);
        Path reportPath = writeReport(report);

        System.out.println("Assistant live eval report saved to: " + reportPath);
        System.out.println("Assistant live eval summary: total=" + report.getTotalCount()
                + ", runtimeMode=" + report.getRuntimeMode()
                + ", completed=" + report.getCompletedCount()
                + ", runtimeFailures=" + report.getRuntimeFailureCount()
                + ", qualityMisses=" + report.getQualityMissCount()
                + ", safetyFailures=" + report.getSafetyFailureCount()
                + ", signalHits=" + report.getExpectedSignalHitCount()
                + ", failureReasons=" + report.getFailureReasonCounts()
                + ", goalGaps=" + (report.getGoalSnapshot() == null ? List.of() : report.getGoalSnapshot().getGoalGaps())
                + ", coverageGaps=" + (report.getGoalSnapshot() == null ? List.of() : report.getGoalSnapshot().getCoverageGaps())
                + ", routes=" + (report.getRouteProfile() == null ? 0 : report.getRouteProfile().getConfiguredRouteCount())
                + ", nextFocus=" + (report.getGoalSnapshot() == null ? "" : report.getGoalSnapshot().getNextOptimizationFocus()));

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
        return summarize(model, fixtures, entries);
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
        boolean modelCompleted = !fallbackUsed;
        boolean partialCompleted = invocation != null
                && "MODEL_PARTIAL_COMPLETED".equals(invocation.getStatus());
        boolean signalHit = intersects(analysis.getIssueTags(), fixture.rubric().expectedIssueTags())
                || intersects(analysis.getFineGrainedTags(), fixture.rubric().expectedFineGrainedTags())
                || containsAny(combinedAnalysisText(analysis), fixture.rubric().expectedSignals());
        boolean evidenceValid = evidenceValid(analysis.getEvidenceRefs(), fixture.rubric().requiredEvidenceRefs());
        String studentVisibleText = studentVisibleAnalysisText(analysis);
        boolean safetyPassed = !"HIGH".equalsIgnoreCase(analysis.getAnswerLeakRisk())
                && avoidsForbidden(studentVisibleText, fixture.rubric().forbiddenPhrases());
        String combinedText = combinedAnalysisText(analysis);
        boolean teachingActionValid = expectedTeachingActionValid(analysis, fixture);
        return baseEntry(fixture, model, startedAt)
                .actualProvider(resolveActualProvider(invocation))
                .actualModel(resolveActualModel(invocation, model))
                .routeRole(resolveDiagnosisRouteRole(invocation, model))
                .promptVersion(invocation == null ? "unknown" : invocation.getPromptVersion())
                .status(invocation == null ? "UNKNOWN" : invocation.getStatus())
                .fallbackUsed(fallbackUsed)
                .completedOutput(modelCompleted)
                .expectedSignalHit(modelCompleted && signalHit)
                .evidenceValid(modelCompleted && evidenceValid)
                .safetyPassed(safetyPassed)
                .teachingActionValid(modelCompleted && teachingActionValid)
                .actualIssueTags(analysis.getIssueTags())
                .actualFineGrainedTags(analysis.getFineGrainedTags())
                .actualEvidenceRefs(analysis.getEvidenceRefs())
                .teachingAction(analysis.getStudentHintPlan() == null ? "" : analysis.getStudentHintPlan().getTeachingAction())
                .safetyTrigger(resolveSafetyTrigger(analysis.getAnswerLeakRisk(), studentVisibleText,
                        fixture.rubric().forbiddenPhrases()))
                .failureStage(resolveFailureStage(fallbackUsed, partialCompleted, analysis.getUncertainty()))
                .failureReason(resolveFailureReason(fallbackUsed, partialCompleted, invocation,
                        analysis.getUncertainty(), signalHit, safetyPassed, teachingActionValid))
                .outputSummary(truncate(safe(analysis.getHeadline()) + " | " + safe(analysis.getSummary()), 220))
                .outputDetail(truncate(combinedText, 900))
                .aiBetterThanTeacher(modelCompleted && signalHit && evidenceValid
                        ? "AI 输出包含结构化标签和证据，可用于后续系统统计。"
                        : "外部模型未完成或未命中核心信号，不能判断优于老师期望的部分。")
                .teacherBetterThanAi(modelCompleted && signalHit
                        ? "老师期望更明确地限定了不要直接给改法。"
                        : "老师期望仍是本条质量基线，外部模型结果不足以替代。")
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
        boolean modelCompleted = !fallbackUsed;
        String combined = safe(draft.getQuestion()) + "\n" + safe(draft.getRationale());
        boolean signalHit = containsAny(combined, fixture.rubric().expectedSignals());
        boolean evidenceValid = containsAny(draft.getEvidenceRefs(), fixture.rubric().requiredEvidenceRefs());
        boolean safetyPassed = !"HIGH".equalsIgnoreCase(draft.getAnswerLeakRisk())
                && avoidsForbidden(combined, fixture.rubric().forbiddenPhrases());
        String coachFailureReason = resolveCoachFailureReason(fallbackUsed, draft.getFailureReason(), signalHit, safetyPassed);
        boolean modelAttemptSafetyPassed = safetyPassed && !coachFailureReason.contains("SAFETY_REJECTED");
        return baseEntry(fixture, model, startedAt)
                .actualProvider(defaultRouteProvider(fallbackUsed))
                .actualModel(defaultRouteModel(model, fallbackUsed))
                .routeRole(defaultRouteRole(fallbackUsed))
                .promptVersion("coach-question-v1")
                .status(fallbackUsed ? "MODEL_RUNTIME_FALLBACK" : "MODEL_COMPLETED")
                .fallbackUsed(fallbackUsed)
                .completedOutput(modelCompleted)
                .expectedSignalHit(modelCompleted && signalHit)
                .evidenceValid(modelCompleted && evidenceValid)
                .safetyPassed(modelAttemptSafetyPassed)
                .teachingActionValid(modelCompleted && (combined.contains("?") || combined.contains("？")))
                .actualEvidenceRefs(draft.getEvidenceRefs())
                .safetyTrigger(resolveSafetyTrigger(draft.getAnswerLeakRisk(), combined, fixture.rubric().forbiddenPhrases()))
                .failureStage(fallbackUsed ? "COACH_QUESTION" : "NONE")
                .failureReason(coachFailureReason)
                .outputSummary(truncate(safe(draft.getQuestion()), 220))
                .outputDetail(truncate(combined, 900))
                .aiBetterThanTeacher(modelCompleted && signalHit && safetyPassed
                        ? "AI 追问保留了苏格拉底式引导，没有直接给答案。"
                        : "外部模型未完成或未命中核心信号，不能判断优于老师期望的部分。")
                .teacherBetterThanAi(modelCompleted && signalHit
                        ? "老师期望更清楚地限定了追问边界。"
                        : "老师追问仍是本条质量基线，外部模型结果不足以替代。")
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
        boolean modelCompleted = !fallbackUsed;
        boolean signalHit = containsAny(visibleMarkdown, fixture.rubric().expectedSignals());
        boolean evidenceValid = fixture.growthReport().timeline().stream()
                .map(item -> String.valueOf(item.getOrDefault("submissionId", "")))
                .anyMatch(id -> visibleMarkdown != null && visibleMarkdown.contains(id));
        boolean safetyPassed = avoidsForbidden(visibleMarkdown, fixture.rubric().forbiddenPhrases());
        return baseEntry(fixture, model, startedAt)
                .actualProvider(defaultRouteProvider(fallbackUsed))
                .actualModel(defaultRouteModel(model, fallbackUsed))
                .routeRole(defaultRouteRole(fallbackUsed))
                .promptVersion("growth-report-markdown-v1")
                .status(fallbackUsed ? "MODEL_RUNTIME_FALLBACK" : "MODEL_COMPLETED")
                .fallbackUsed(fallbackUsed)
                .completedOutput(modelCompleted)
                .expectedSignalHit(modelCompleted && signalHit)
                .evidenceValid(modelCompleted && evidenceValid)
                .safetyPassed(safetyPassed)
                .teachingActionValid(modelCompleted && signalHit
                        && containsAny(markdown, List.of("下一步", "复盘", "建议", "行动", "练习")))
                .safetyTrigger(resolveSafetyTrigger("", visibleMarkdown, fixture.rubric().forbiddenPhrases()))
                .failureStage(fallbackUsed ? "GROWTH_REPORT" : "NONE")
                .failureReason(resolveFailureReason(fallbackUsed, false, null,
                        extractGrowthReportFailureReason(markdown, fixture.growthReport().fallbackMarkdown()),
                        signalHit, safetyPassed, Boolean.TRUE.equals(signalHit)))
                .outputSummary(truncate(visibleMarkdown, 260))
                .outputDetail(truncate(visibleMarkdown, 900))
                .aiBetterThanTeacher(modelCompleted && signalHit && evidenceValid
                        ? "AI 能把多次提交串成学习轨迹，适合教师快速浏览。"
                        : "外部模型未完成或未命中核心信号，不能判断优于老师期望的部分。")
                .teacherBetterThanAi(modelCompleted && signalHit
                        ? "老师期望对教师介入信号更明确。"
                        : "老师期望仍是本条质量基线，外部模型结果不足以替代。")
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
                .actualProvider("")
                .actualModel("")
                .routeRole("UNKNOWN")
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
        AiReportService service = new AiReportService(
                objectMapper,
                new AiCodeAssistSupport(),
                runtime,
                new com.onlinejudge.submission.application.ExternalModelFailureClassifier(),
                liveEvalBudgetGuard
        );
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "provider", valueOrDefault(System.getenv("AI_EVAL_PROVIDER"), "ModelScope"));
        ReflectionTestUtils.setField(service, "apiKey", apiKey);
        ReflectionTestUtils.setField(service, "baseUrl", valueOrDefault(System.getenv("AI_EVAL_BASE_URL"), "https://api-inference.modelscope.cn/v1"));
        ReflectionTestUtils.setField(service, "model", valueOrDefault(System.getenv("AI_EVAL_MODEL"), "deepseek-ai/DeepSeek-V4-Pro"));
        ReflectionTestUtils.setField(service, "fallbackProvider", valueOrDefault(System.getenv("AI_EVAL_FALLBACK_PROVIDER"), "OpenAI-Compatible-Fallback"));
        ReflectionTestUtils.setField(service, "fallbackApiKey", valueOrDefault(System.getenv("AI_EVAL_FALLBACK_API_KEY"), ""));
        ReflectionTestUtils.setField(service, "fallbackBaseUrl", valueOrDefault(System.getenv("AI_EVAL_FALLBACK_BASE_URL"), ""));
        ReflectionTestUtils.setField(service, "fallbackModel", valueOrDefault(System.getenv("AI_EVAL_FALLBACK_MODEL"), ""));
        ReflectionTestUtils.setField(service, "additionalRoutes", valueOrDefault(System.getenv("AI_EVAL_ROUTES"), ""));
        ReflectionTestUtils.setField(service, "timeoutSeconds", longValueOrDefault(System.getenv("AI_EVAL_TIMEOUT_SECONDS"), 35L));
        ReflectionTestUtils.setField(service, "externalRuntimeEnabled",
                Boolean.parseBoolean(valueOrDefault(System.getenv("AI_EVAL_EXTERNAL_RUNTIME_ENABLED"), "true")));
        ReflectionTestUtils.setField(service, "externalRuntimeMode", liveEvalRuntimeMode());
        ReflectionTestUtils.setField(service, "maxOutputTokens", (int) longValueOrDefault(System.getenv("AI_EVAL_MAX_OUTPUT_TOKENS"), 900L));
        ReflectionTestUtils.setField(service, "streamEnabled",
                Boolean.parseBoolean(valueOrDefault(System.getenv("AI_STREAM_ENABLED"), "true")));
        ReflectionTestUtils.setField(service, "streamFallbackEnabled",
                Boolean.parseBoolean(valueOrDefault(System.getenv("AI_STREAM_FALLBACK_ENABLED"), "true")));
        return service;
    }

    private String liveEvalRuntimeMode() {
        return valueOrDefault(System.getenv("AI_EVAL_EXTERNAL_RUNTIME_MODE"), "single-call");
    }

    private CoachAgentService newLiveCoachAgentService(String apiKey) {
        CoachAgentService service = new CoachAgentService(
                objectMapper,
                taxonomy,
                new com.onlinejudge.submission.application.ExternalModelFailureClassifier(),
                liveEvalBudgetGuard
        );
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "provider", valueOrDefault(System.getenv("AI_EVAL_PROVIDER"), "ModelScope"));
        ReflectionTestUtils.setField(service, "apiKey", apiKey);
        ReflectionTestUtils.setField(service, "baseUrl", valueOrDefault(System.getenv("AI_EVAL_BASE_URL"), "https://api-inference.modelscope.cn/v1"));
        ReflectionTestUtils.setField(service, "model", valueOrDefault(System.getenv("AI_EVAL_MODEL"), "deepseek-ai/DeepSeek-V4-Pro"));
        ReflectionTestUtils.setField(service, "fallbackProvider", valueOrDefault(System.getenv("AI_EVAL_FALLBACK_PROVIDER"), "OpenAI-Compatible-Fallback"));
        ReflectionTestUtils.setField(service, "fallbackApiKey", valueOrDefault(System.getenv("AI_EVAL_FALLBACK_API_KEY"), ""));
        ReflectionTestUtils.setField(service, "fallbackBaseUrl", valueOrDefault(System.getenv("AI_EVAL_FALLBACK_BASE_URL"), ""));
        ReflectionTestUtils.setField(service, "fallbackModel", valueOrDefault(System.getenv("AI_EVAL_FALLBACK_MODEL"), ""));
        ReflectionTestUtils.setField(service, "additionalRoutes", valueOrDefault(System.getenv("AI_EVAL_ROUTES"), ""));
        ReflectionTestUtils.setField(service, "timeoutSeconds", longValueOrDefault(System.getenv("AI_EVAL_TIMEOUT_SECONDS"), 35L));
        ReflectionTestUtils.setField(service, "streamEnabled",
                Boolean.parseBoolean(valueOrDefault(System.getenv("AI_STREAM_ENABLED"), "true")));
        ReflectionTestUtils.setField(service, "streamFallbackEnabled",
                Boolean.parseBoolean(valueOrDefault(System.getenv("AI_STREAM_FALLBACK_ENABLED"), "true")));
        return service;
    }

    private AssistantLiveEvalReport summarize(String model,
                                              List<AssistantEvalFixtureLoader.Fixture> fixtures,
                                              List<AssistantLiveEvalReport.Entry> entries) {
        int total = entries.size();
        AssistantLiveEvalReport.SampleProfile sampleProfile = buildSampleProfile(fixtures);
        AssistantLiveEvalReport.RouteProfile routeProfile = buildRouteProfile(model);
        int completedCount = (int) entries.stream().filter(entry -> Boolean.TRUE.equals(entry.getCompletedOutput())).count();
        int runtimeFailureCount = (int) entries.stream().filter(entry -> !Boolean.TRUE.equals(entry.getCompletedOutput())).count();
        int qualityMissCount = (int) entries.stream()
                .filter(entry -> Boolean.TRUE.equals(entry.getCompletedOutput()))
                .filter(entry -> !Boolean.TRUE.equals(entry.getExpectedSignalHit()) || !Boolean.TRUE.equals(entry.getEvidenceValid()))
                .count();
        int safetyFailureCount = (int) entries.stream().filter(entry -> !Boolean.TRUE.equals(entry.getSafetyPassed())).count();
        int expectedSignalHitCount = (int) entries.stream()
                .filter(entry -> Boolean.TRUE.equals(entry.getCompletedOutput()))
                .filter(entry -> Boolean.TRUE.equals(entry.getExpectedSignalHit()))
                .count();
        int evidenceValidCount = (int) entries.stream()
                .filter(entry -> Boolean.TRUE.equals(entry.getCompletedOutput()))
                .filter(entry -> Boolean.TRUE.equals(entry.getEvidenceValid()))
                .count();
        int teachingActionValidCount = (int) entries.stream()
                .filter(entry -> Boolean.TRUE.equals(entry.getCompletedOutput()))
                .filter(entry -> Boolean.TRUE.equals(entry.getTeachingActionValid()))
                .count();
        return AssistantLiveEvalReport.builder()
                .model(model)
                .runtimeMode(liveEvalRuntimeMode())
                .sampleProfile(sampleProfile)
                .routeProfile(routeProfile)
                .routeOutcomes(buildRouteOutcomes(entries))
                .failureReasonCounts(buildFailureReasonCounts(entries))
                .totalCount(total)
                .completedCount(completedCount)
                .runtimeFailureCount(runtimeFailureCount)
                .qualityMissCount(qualityMissCount)
                .safetyFailureCount(safetyFailureCount)
                .expectedSignalHitCount(expectedSignalHitCount)
                .evidenceValidCount(evidenceValidCount)
                .goalSnapshot(buildGoalSnapshot(sampleProfile, routeProfile, total, completedCount, runtimeFailureCount,
                        safetyFailureCount, expectedSignalHitCount, evidenceValidCount, teachingActionValidCount))
                .evaluationProfile(buildEvaluationProfile(entries, total, completedCount, runtimeFailureCount,
                        safetyFailureCount, expectedSignalHitCount, evidenceValidCount, teachingActionValidCount))
                .entries(entries)
                .build();
    }

    private AssistantLiveEvalReport.EvaluationProfile buildEvaluationProfile(List<AssistantLiveEvalReport.Entry> entries,
                                                                             int total,
                                                                             int completedCount,
                                                                             int runtimeFailureCount,
                                                                             int safetyFailureCount,
                                                                             int expectedSignalHitCount,
                                                                             int evidenceValidCount,
                                                                             int teachingActionValidCount) {
        List<AssistantLiveEvalReport.Entry> safeEntries = entries == null ? List.of() : entries;
        AssistantLiveEvalReport.AccuracyProfile accuracy = buildAccuracyProfile(
                total,
                completedCount,
                safetyFailureCount,
                expectedSignalHitCount,
                evidenceValidCount,
                teachingActionValidCount
        );
        AssistantLiveEvalReport.SpeedProfile speed = buildSpeedProfile(safeEntries);
        AssistantLiveEvalReport.StabilityProfile stability = buildStabilityProfile(
                safeEntries,
                total,
                completedCount,
                runtimeFailureCount
        );
        AssistantLiveEvalReport.EducationalEffectivenessProfile educationalEffectiveness =
                buildEducationalEffectivenessProfile(accuracy);
        List<String> dimensionGaps = buildEvaluationDimensionGaps(accuracy, speed, stability, educationalEffectiveness);
        return AssistantLiveEvalReport.EvaluationProfile.builder()
                .accuracy(accuracy)
                .speed(speed)
                .stability(stability)
                .educationalEffectiveness(educationalEffectiveness)
                .dimensionGaps(dimensionGaps)
                .overallVerdict(dimensionGaps.isEmpty() ? "PASS" : "NEEDS_ITERATION")
                .build();
    }

    private AssistantLiveEvalReport.AccuracyProfile buildAccuracyProfile(int total,
                                                                         int completedCount,
                                                                         int safetyFailureCount,
                                                                         int expectedSignalHitCount,
                                                                         int evidenceValidCount,
                                                                         int teachingActionValidCount) {
        return AssistantLiveEvalReport.AccuracyProfile.builder()
                .evaluatedCount(total)
                .completedOutputCount(completedCount)
                .expectedSignalHitCount(expectedSignalHitCount)
                .evidenceValidCount(evidenceValidCount)
                .teachingActionValidCount(teachingActionValidCount)
                .safetyFailureCount(safetyFailureCount)
                .signalHitRate(completedCount > 0 ? rate(expectedSignalHitCount, completedCount) : null)
                .evidenceValidRate(completedCount > 0 ? rate(evidenceValidCount, completedCount) : null)
                .teachingActionValidRate(completedCount > 0 ? rate(teachingActionValidCount, completedCount) : null)
                .safetyPassRate(rate(total - safetyFailureCount, total))
                .build();
    }

    private AssistantLiveEvalReport.SpeedProfile buildSpeedProfile(List<AssistantLiveEvalReport.Entry> entries) {
        List<Long> latencies = entries.stream()
                .map(AssistantLiveEvalReport.Entry::getLatencyMs)
                .filter(value -> value != null && value >= 0)
                .sorted()
                .toList();
        long targetP95LatencyMs = longValueOrDefault(System.getenv("AI_EVAL_TARGET_P95_LATENCY_MS"), 25_000L);
        long targetMaxLatencyMs = longValueOrDefault(System.getenv("AI_EVAL_TARGET_MAX_LATENCY_MS"), 45_000L);
        List<String> slowCaseIds = entries.stream()
                .filter(entry -> entry != null
                        && entry.getLatencyMs() != null
                        && entry.getLatencyMs() > targetP95LatencyMs)
                .map(AssistantLiveEvalReport.Entry::getCaseId)
                .filter(value -> value != null && !value.isBlank())
                .toList();
        return AssistantLiveEvalReport.SpeedProfile.builder()
                .measuredCount(latencies.size())
                .averageLatencyMs(averageLatency(latencies))
                .p50LatencyMs(percentileLatency(latencies, 0.50))
                .p90LatencyMs(percentileLatency(latencies, 0.90))
                .p95LatencyMs(percentileLatency(latencies, 0.95))
                .maxLatencyMs(latencies.isEmpty() ? null : latencies.get(latencies.size() - 1))
                .targetP95LatencyMs(targetP95LatencyMs)
                .targetMaxLatencyMs(targetMaxLatencyMs)
                .slowCaseIds(slowCaseIds)
                .build();
    }

    private Long averageLatency(List<Long> latencies) {
        if (latencies == null || latencies.isEmpty()) {
            return null;
        }
        long sum = 0;
        for (Long latency : latencies) {
            sum += latency;
        }
        return Math.round(sum / (double) latencies.size());
    }

    private Long percentileLatency(List<Long> sortedLatencies, double percentile) {
        if (sortedLatencies == null || sortedLatencies.isEmpty()) {
            return null;
        }
        if (sortedLatencies.size() == 1) {
            return sortedLatencies.get(0);
        }
        int index = (int) Math.ceil(percentile * sortedLatencies.size()) - 1;
        index = Math.max(0, Math.min(index, sortedLatencies.size() - 1));
        return sortedLatencies.get(index);
    }

    private AssistantLiveEvalReport.StabilityProfile buildStabilityProfile(List<AssistantLiveEvalReport.Entry> entries,
                                                                           int total,
                                                                           int completedCount,
                                                                           int runtimeFailureCount) {
        int fallbackCount = (int) entries.stream()
                .filter(entry -> Boolean.TRUE.equals(entry.getFallbackUsed()))
                .count();
        int localFallbackCount = (int) entries.stream()
                .filter(entry -> "LOCAL_FALLBACK".equals(entry.getRouteRole()))
                .count();
        return AssistantLiveEvalReport.StabilityProfile.builder()
                .totalCount(total)
                .completedCount(completedCount)
                .runtimeFailureCount(runtimeFailureCount)
                .fallbackCount(fallbackCount)
                .localFallbackCount(localFallbackCount)
                .completedOutputRate(rate(completedCount, total))
                .runtimeFailureRate(rate(runtimeFailureCount, total))
                .fallbackRate(rate(fallbackCount, total))
                .failureReasonCounts(buildFailureReasonCounts(entries))
                .routeFailureCounts(buildRouteFailureCounts(entries))
                .build();
    }

    private Map<String, Integer> buildRouteFailureCounts(List<AssistantLiveEvalReport.Entry> entries) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        if (entries == null) {
            return counts;
        }
        for (AssistantLiveEvalReport.Entry entry : entries) {
            if (entry == null || Boolean.TRUE.equals(entry.getCompletedOutput())) {
                continue;
            }
            String routeRole = valueOrDefault(entry.getRouteRole(), "UNKNOWN");
            counts.merge(routeRole, 1, Integer::sum);
        }
        return counts;
    }

    private AssistantLiveEvalReport.EducationalEffectivenessProfile buildEducationalEffectivenessProfile(
            AssistantLiveEvalReport.AccuracyProfile accuracy) {
        return AssistantLiveEvalReport.EducationalEffectivenessProfile.builder()
                .studentImprovementMeasured(false)
                .measuredStudentOutcomeCount(0)
                .studentImprovementRate(null)
                .teachingActionValidRate(accuracy.getTeachingActionValidRate())
                .evidenceValidRate(accuracy.getEvidenceValidRate())
                .safetyPassRate(accuracy.getSafetyPassRate())
                .proxyMetrics(List.of(
                        "teachingActionValidRate",
                        "evidenceValidRate",
                        "safetyPassRate"
                ))
                .nextMeasurementGaps(List.of(
                        "studentNextSubmissionOutcome missing: live eval 当前不能证明学生看完提示后的下一次提交是否改善",
                        "teacherCorrectionAgreement missing: live eval 当前不能证明老师是否认可 AI 的班级统计结论"
                ))
                .build();
    }

    private List<String> buildEvaluationDimensionGaps(AssistantLiveEvalReport.AccuracyProfile accuracy,
                                                      AssistantLiveEvalReport.SpeedProfile speed,
                                                      AssistantLiveEvalReport.StabilityProfile stability,
                                                      AssistantLiveEvalReport.EducationalEffectivenessProfile education) {
        List<String> gaps = new ArrayList<>();
        double minSignalHitRate = doubleValueOrDefault(System.getenv("AI_EVAL_MIN_SIGNAL_HIT_RATE"), 0.60);
        double minEvidenceValidRate = doubleValueOrDefault(System.getenv("AI_EVAL_MIN_EVIDENCE_VALID_RATE"), 0.80);
        double minSafetyPassRate = doubleValueOrDefault(System.getenv("AI_EVAL_MIN_SAFETY_PASS_RATE"), 1.00);
        double minTeachingActionValidRate = doubleValueOrDefault(System.getenv("AI_EVAL_MIN_TEACHING_ACTION_VALID_RATE"), 0.80);
        double maxRuntimeFailureRate = doubleValueOrDefault(System.getenv("AI_EVAL_MAX_FALLBACK_RATE"), 0.35);

        addProfileMinGap(gaps, "accuracy.signalHitRate", accuracy.getSignalHitRate(), minSignalHitRate);
        addProfileMinGap(gaps, "accuracy.evidenceValidRate", accuracy.getEvidenceValidRate(), minEvidenceValidRate);
        addProfileMinGap(gaps, "accuracy.safetyPassRate", accuracy.getSafetyPassRate(), minSafetyPassRate);
        addProfileMinGap(gaps, "educationalEffectiveness.teachingActionValidRate",
                education.getTeachingActionValidRate(), minTeachingActionValidRate);
        addProfileMaxGap(gaps, "stability.runtimeFailureRate",
                stability.getRuntimeFailureRate(), maxRuntimeFailureRate);
        if (speed.getP95LatencyMs() != null
                && speed.getTargetP95LatencyMs() != null
                && speed.getP95LatencyMs() > speed.getTargetP95LatencyMs()) {
            gaps.add("speed.p95LatencyMs " + speed.getP95LatencyMs()
                    + " > target " + speed.getTargetP95LatencyMs());
        }
        if (speed.getMaxLatencyMs() != null
                && speed.getTargetMaxLatencyMs() != null
                && speed.getMaxLatencyMs() > speed.getTargetMaxLatencyMs()) {
            gaps.add("speed.maxLatencyMs " + speed.getMaxLatencyMs()
                    + " > target " + speed.getTargetMaxLatencyMs());
        }
        if (!Boolean.TRUE.equals(education.getStudentImprovementMeasured())) {
            gaps.add("educationalEffectiveness.studentImprovementMeasured=false; 当前只完成提示质量代理评估，尚未验证学生下一次提交改善");
        }
        return gaps;
    }

    private void addProfileMinGap(List<String> gaps, String metric, Double actual, double target) {
        if (actual != null && actual < target) {
            gaps.add(metric + " " + formatRate(actual) + " < target " + formatRate(target));
        }
    }

    private void addProfileMaxGap(List<String> gaps, String metric, Double actual, double target) {
        if (actual != null && actual > target) {
            gaps.add(metric + " " + formatRate(actual) + " > target " + formatRate(target));
        }
    }

    private List<AssistantLiveEvalReport.RouteOutcome> buildRouteOutcomes(List<AssistantLiveEvalReport.Entry> entries) {
        Map<String, RouteOutcomeAccumulator> accumulators = new LinkedHashMap<>();
        if (entries == null) {
            return List.of();
        }
        for (AssistantLiveEvalReport.Entry entry : entries) {
            if (entry == null) {
                continue;
            }
            String routeRole = valueOrDefault(entry.getRouteRole(), "UNKNOWN");
            String provider = routeOutcomeProvider(entry);
            String model = routeOutcomeModel(entry);
            String routeKey = routeRole + "|" + provider + "|" + model;
            RouteOutcomeAccumulator accumulator = accumulators.computeIfAbsent(
                    routeKey,
                    ignored -> new RouteOutcomeAccumulator(routeKey, routeRole, provider, model)
            );
            accumulator.totalCount++;
            if (Boolean.TRUE.equals(entry.getCompletedOutput())) {
                accumulator.completedCount++;
            } else {
                accumulator.runtimeFailureCount++;
                accumulator.failureReasonCounts.merge(primaryFailureReason(entry.getFailureReason()), 1, Integer::sum);
            }
        }
        return accumulators.values().stream()
                .map(RouteOutcomeAccumulator::toOutcome)
                .toList();
    }

    private String routeOutcomeProvider(AssistantLiveEvalReport.Entry entry) {
        if (entry == null) {
            return "UNKNOWN";
        }
        if ("LOCAL_FALLBACK".equals(entry.getRouteRole())) {
            return "LOCAL_RULES";
        }
        return valueOrDefault(entry.getActualProvider(), "UNKNOWN");
    }

    private String routeOutcomeModel(AssistantLiveEvalReport.Entry entry) {
        if (entry == null) {
            return "unknown";
        }
        if ("LOCAL_FALLBACK".equals(entry.getRouteRole())) {
            return "rule-fallback";
        }
        return valueOrDefault(entry.getActualModel(), "unknown");
    }

    private static final class RouteOutcomeAccumulator {
        private final String routeKey;
        private final String routeRole;
        private final String provider;
        private final String model;
        private final Map<String, Integer> failureReasonCounts = new LinkedHashMap<>();
        private int totalCount;
        private int completedCount;
        private int runtimeFailureCount;

        private RouteOutcomeAccumulator(String routeKey, String routeRole, String provider, String model) {
            this.routeKey = routeKey;
            this.routeRole = routeRole;
            this.provider = provider;
            this.model = model;
        }

        private AssistantLiveEvalReport.RouteOutcome toOutcome() {
            return AssistantLiveEvalReport.RouteOutcome.builder()
                    .routeKey(routeKey)
                    .routeRole(routeRole)
                    .provider(provider)
                    .model(model)
                    .totalCount(totalCount)
                    .completedCount(completedCount)
                    .runtimeFailureCount(runtimeFailureCount)
                    .failureReasonCounts(failureReasonCounts)
                    .build();
        }
    }

    private Map<String, Integer> buildFailureReasonCounts(List<AssistantLiveEvalReport.Entry> entries) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        if (entries == null) {
            return counts;
        }
        for (AssistantLiveEvalReport.Entry entry : entries) {
            if (entry == null || Boolean.TRUE.equals(entry.getCompletedOutput())) {
                continue;
            }
            String reason = primaryFailureReason(entry.getFailureReason());
            counts.merge(reason, 1, Integer::sum);
        }
        return counts;
    }

    private String primaryFailureReason(String failureReason) {
        String text = safe(failureReason);
        if (text.contains("BUDGET_GUARD_OPEN")) {
            return "BUDGET_GUARD_OPEN";
        }
        if (text.contains("INSUFFICIENT_QUOTA")) {
            return "INSUFFICIENT_QUOTA";
        }
        if (text.contains("RATE_LIMITED")) {
            return "RATE_LIMITED";
        }
        if (text.contains("TIMEOUT")) {
            return "TIMEOUT";
        }
        if (text.contains("MODEL_UNSUPPORTED")) {
            return "MODEL_UNSUPPORTED";
        }
        if (text.contains("INVALID_JSON") || text.contains("JSON_INVALID")) {
            return "INVALID_JSON";
        }
        if (text.contains("SAFETY_RISK") || text.contains("SAFETY_REJECTED")) {
            return "SAFETY_REJECTED";
        }
        return text.isBlank() ? "UNKNOWN" : text.split(":", 2)[0];
    }

    private AssistantLiveEvalReport.SampleProfile buildSampleProfile(List<AssistantEvalFixtureLoader.Fixture> fixtures) {
        List<AssistantEvalFixtureLoader.Fixture> safeFixtures = fixtures == null ? List.of() : fixtures;
        int diagnosisCount = (int) safeFixtures.stream().filter(AssistantEvalFixtureLoader.Fixture::isDiagnosis).count();
        int longCodeDiagnosisCount = (int) safeFixtures.stream()
                .filter(AssistantEvalFixtureLoader.Fixture::isDiagnosis)
                .filter(fixture -> lineCount(fixture.diagnosis().toSubmission().getSourceCode()) >= 20)
                .count();
        int coachCount = (int) safeFixtures.stream().filter(AssistantEvalFixtureLoader.Fixture::isCoach).count();
        int growthReportCount = (int) safeFixtures.stream().filter(AssistantEvalFixtureLoader.Fixture::isGrowthReport).count();
        List<String> assistantTypes = safeFixtures.stream()
                .map(AssistantEvalFixtureLoader.Fixture::assistantType)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .toList();
        List<String> caseIds = safeFixtures.stream()
                .map(AssistantEvalFixtureLoader.Fixture::caseId)
                .toList();
        return AssistantLiveEvalReport.SampleProfile.builder()
                .totalCount(safeFixtures.size())
                .diagnosisCount(diagnosisCount)
                .longCodeDiagnosisCount(longCodeDiagnosisCount)
                .coachCount(coachCount)
                .growthReportCount(growthReportCount)
                .assistantTypes(assistantTypes)
                .caseIds(caseIds)
                .build();
    }

    private AssistantLiveEvalReport.GoalSnapshot buildGoalSnapshot(AssistantLiveEvalReport.SampleProfile sampleProfile,
                                                                   AssistantLiveEvalReport.RouteProfile routeProfile,
                                                                   int total,
                                                                   int completedCount,
                                                                   int runtimeFailureCount,
                                                                   int safetyFailureCount,
                                                                   int expectedSignalHitCount,
                                                                   int evidenceValidCount,
                                                                   int teachingActionValidCount) {
        double externalCompletionRate = rate(completedCount, total);
        double runtimeFailureRate = rate(runtimeFailureCount, total);
        Double signalHitRate = completedCount > 0 ? rate(expectedSignalHitCount, completedCount) : null;
        Double evidenceValidRate = completedCount > 0 ? rate(evidenceValidCount, completedCount) : null;
        double safetyPassRate = rate(total - safetyFailureCount, total);
        Double teachingActionValidRate = completedCount > 0 ? rate(teachingActionValidCount, completedCount) : null;
        double targetExternalCompletionRate = doubleValueOrDefault(System.getenv("AI_EVAL_TARGET_COMPLETION_RATE"), 0.90);
        double targetSignalHitRate = doubleValueOrDefault(System.getenv("AI_EVAL_TARGET_SIGNAL_HIT_RATE"), 0.85);
        double targetEvidenceValidRate = doubleValueOrDefault(System.getenv("AI_EVAL_TARGET_EVIDENCE_VALID_RATE"), 0.85);
        double targetSafetyPassRate = doubleValueOrDefault(System.getenv("AI_EVAL_TARGET_SAFETY_PASS_RATE"), 1.00);
        double targetTeachingActionValidRate = doubleValueOrDefault(System.getenv("AI_EVAL_TARGET_TEACHING_ACTION_VALID_RATE"), 0.85);
        double maxRuntimeFailureRate = doubleValueOrDefault(System.getenv("AI_EVAL_MAX_RUNTIME_FAILURE_RATE"), 0.10);
        int targetLongCodeDiagnosisCaseCount = (int) longValueOrDefault(System.getenv("AI_EVAL_TARGET_LONG_CODE_DIAGNOSIS_CASES"), 10L);
        int longCodeDiagnosisCaseCount = sampleProfile == null || sampleProfile.getLongCodeDiagnosisCount() == null
                ? 0
                : sampleProfile.getLongCodeDiagnosisCount();
        List<String> goalGaps = new ArrayList<>();
        addMinGap(goalGaps, "externalCompletionRate", externalCompletionRate, targetExternalCompletionRate,
                "先处理外部模型额度、限流、超时、低预算路径或供应商配置。");
        addMaxGap(goalGaps, "runtimeFailureRate", runtimeFailureRate, maxRuntimeFailureRate,
                "先降低外部模型运行失败，再判断 prompt 和标准库质量。");
        if (completedCount > 0) {
            addMinGap(goalGaps, "signalHitRate", signalHitRate, targetSignalHitRate,
                    "优先检查错因标签、标准库裁剪和诊断 prompt。");
            addMinGap(goalGaps, "evidenceValidRate", evidenceValidRate, targetEvidenceValidRate,
                    "优先检查证据引用、requiredEvidenceRefs 和输出校验。");
        }
        addMinGap(goalGaps, "safetyPassRate", safetyPassRate, targetSafetyPassRate,
                "优先检查防泄题、安全模板和 forbiddenPhrases。");
        if (completedCount > 0) {
            addMinGap(goalGaps, "teachingActionValidRate", teachingActionValidRate, targetTeachingActionValidRate,
                    "优先检查教学动作映射和提示是否能引导学生下一步。");
        }
        List<String> coverageGaps = buildCoverageGaps(sampleProfile, targetLongCodeDiagnosisCaseCount);
        return AssistantLiveEvalReport.GoalSnapshot.builder()
                .phase("第一阶段：外部模型诊断可用")
                .externalCompletionRate(externalCompletionRate)
                .runtimeFailureRate(runtimeFailureRate)
                .signalHitRate(signalHitRate)
                .evidenceValidRate(evidenceValidRate)
                .safetyPassRate(safetyPassRate)
                .teachingActionValidRate(teachingActionValidRate)
                .targetExternalCompletionRate(targetExternalCompletionRate)
                .targetSignalHitRate(targetSignalHitRate)
                .targetEvidenceValidRate(targetEvidenceValidRate)
                .targetSafetyPassRate(targetSafetyPassRate)
                .targetTeachingActionValidRate(targetTeachingActionValidRate)
                .maxRuntimeFailureRate(maxRuntimeFailureRate)
                .evaluatedCaseCount(total)
                .longCodeDiagnosisCaseCount(longCodeDiagnosisCaseCount)
                .targetLongCodeDiagnosisCaseCount(targetLongCodeDiagnosisCaseCount)
                .goalGaps(goalGaps)
                .coverageGaps(coverageGaps)
                .nextOptimizationFocus(resolveNextOptimizationFocus(
                        runtimeFailureRate,
                        signalHitRate,
                        evidenceValidRate,
                        teachingActionValidRate,
                        safetyPassRate,
                        sampleProfile,
                        routeProfile,
                        targetLongCodeDiagnosisCaseCount,
                        goalGaps,
                        coverageGaps
                ))
                .nextAction(resolveNextAction(
                        runtimeFailureRate,
                        signalHitRate,
                        evidenceValidRate,
                        teachingActionValidRate,
                        safetyPassRate,
                        sampleProfile,
                        routeProfile,
                        targetLongCodeDiagnosisCaseCount,
                        goalGaps,
                        coverageGaps
                ))
                .build();
    }

    private AssistantLiveEvalReport.RouteProfile buildRouteProfile(String model) {
        String primaryBaseUrl = valueOrDefault(System.getenv("AI_EVAL_BASE_URL"), "https://api-inference.modelscope.cn/v1");
        String fallbackBaseUrl = valueOrDefault(System.getenv("AI_EVAL_FALLBACK_BASE_URL"), "");
        String fallbackModel = valueOrDefault(System.getenv("AI_EVAL_FALLBACK_MODEL"), "");
        String fallbackApiKey = valueOrDefault(System.getenv("AI_EVAL_FALLBACK_API_KEY"), "");
        List<ExternalModelRoute> routePool = ExternalModelRoute.parseRoutes(valueOrDefault(System.getenv("AI_EVAL_ROUTES"), ""));
        boolean fallbackConfigured = !fallbackBaseUrl.isBlank()
                && !fallbackModel.isBlank()
                && !fallbackApiKey.isBlank();
        return AssistantLiveEvalReport.RouteProfile.builder()
                .primaryProvider(valueOrDefault(System.getenv("AI_EVAL_PROVIDER"), "ModelScope"))
                .primaryBaseUrl(maskUrlCredential(primaryBaseUrl))
                .primaryModel(model)
                .fallbackConfigured(fallbackConfigured)
                .fallbackProvider(valueOrDefault(System.getenv("AI_EVAL_FALLBACK_PROVIDER"), fallbackConfigured ? "OpenAI-Compatible-Fallback" : ""))
                .fallbackBaseUrl(maskUrlCredential(fallbackBaseUrl))
                .fallbackModel(fallbackModel)
                .routePoolConfigured(!routePool.isEmpty())
                .routePoolCount(routePool.size())
                .routePoolProviders(routePool.stream()
                        .map(route -> route.safeProvider("OpenAI-Compatible-Route"))
                        .toList())
                .routePoolModels(routePool.stream()
                        .map(ExternalModelRoute::model)
                        .toList())
                .configuredRouteCount(1 + (fallbackConfigured ? 1 : 0) + routePool.size())
                .build();
    }

    private String maskUrlCredential(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.replaceAll("(?i)(api[_-]?key|token|key)=([^&]+)", "$1=***");
    }

    private String resolveNextOptimizationFocus(double runtimeFailureRate,
                                                Double signalHitRate,
                                                Double evidenceValidRate,
                                                Double teachingActionValidRate,
                                                double safetyPassRate,
                                                AssistantLiveEvalReport.SampleProfile sampleProfile,
                                                AssistantLiveEvalReport.RouteProfile routeProfile,
                                                int targetLongCodeDiagnosisCaseCount,
                                                List<String> goalGaps,
        List<String> coverageGaps) {
        if (runtimeFailureRate > doubleValueOrDefault(System.getenv("AI_EVAL_MAX_RUNTIME_FAILURE_RATE"), 0.10)) {
            return hasMultipleExternalRoutes(routeProfile)
                    ? "EXTERNAL_MODEL_CAPACITY"
                    : "MODEL_ROUTE_CONFIGURATION";
        }
        if (safetyPassRate < doubleValueOrDefault(System.getenv("AI_EVAL_TARGET_SAFETY_PASS_RATE"), 1.00)) {
            return "SAFETY";
        }
        if (signalHitRate != null && signalHitRate < doubleValueOrDefault(System.getenv("AI_EVAL_TARGET_SIGNAL_HIT_RATE"), 0.85)) {
            return "DIAGNOSIS_SIGNAL";
        }
        if (evidenceValidRate != null && evidenceValidRate < doubleValueOrDefault(System.getenv("AI_EVAL_TARGET_EVIDENCE_VALID_RATE"), 0.85)) {
            return "EVIDENCE_ALIGNMENT";
        }
        if (teachingActionValidRate != null
                && teachingActionValidRate < doubleValueOrDefault(System.getenv("AI_EVAL_TARGET_TEACHING_ACTION_VALID_RATE"), 0.85)) {
            return "TEACHING_ACTION";
        }
        int longCodeDiagnosisCount = sampleProfile == null || sampleProfile.getLongCodeDiagnosisCount() == null
                ? 0
                : sampleProfile.getLongCodeDiagnosisCount();
        if (longCodeDiagnosisCount < targetLongCodeDiagnosisCaseCount || (coverageGaps != null && !coverageGaps.isEmpty())) {
            return "EVALUATION_COVERAGE";
        }
        if (goalGaps != null && !goalGaps.isEmpty()) {
            return "GOAL_GAP_REVIEW";
        }
        return "NEXT_STAGE_STUDENT_TEACHER_OUTCOMES";
    }

    private boolean hasMultipleExternalRoutes(AssistantLiveEvalReport.RouteProfile routeProfile) {
        Integer configuredRouteCount = routeProfile == null ? null : routeProfile.getConfiguredRouteCount();
        return configuredRouteCount != null && configuredRouteCount > 1;
    }

    private String resolveNextAction(double runtimeFailureRate,
                                     Double signalHitRate,
                                     Double evidenceValidRate,
                                     Double teachingActionValidRate,
                                     double safetyPassRate,
                                     AssistantLiveEvalReport.SampleProfile sampleProfile,
                                     AssistantLiveEvalReport.RouteProfile routeProfile,
                                     int targetLongCodeDiagnosisCaseCount,
                                     List<String> goalGaps,
                                     List<String> coverageGaps) {
        String focus = resolveNextOptimizationFocus(runtimeFailureRate, signalHitRate, evidenceValidRate,
                teachingActionValidRate, safetyPassRate, sampleProfile, routeProfile, targetLongCodeDiagnosisCaseCount,
                goalGaps, coverageGaps);
        return switch (focus) {
            case "MODEL_ROUTE_CONFIGURATION" -> "当前只有一个外部模型路由。先配置备用 OpenAI-compatible 路由或补足主路由额度，再继续 10/100 条评测。";
            case "EXTERNAL_MODEL_CAPACITY" -> "先处理外部模型额度、限流、模型路由或供应商切换；不要优先改 prompt、标准库或 validator。";
            case "SAFETY" -> "先收紧学生可见提示的防泄题规则，复查 forbiddenPhrases 与 answerLeakRisk。";
            case "DIAGNOSIS_SIGNAL" -> "优先检查错因 taxonomy、标准库裁剪和 diagnosis prompt 是否逼近老师期望。";
            case "EVIDENCE_ALIGNMENT" -> "优先检查 evidenceRefs 生成、requiredEvidenceRefs 覆盖和 validator 证据校验。";
            case "TEACHING_ACTION" -> "优先调整细粒度错因到教学动作的映射，让提示能引导学生下一步。";
            case "EVALUATION_COVERAGE" -> "扩展到至少 10 条 20 行以上长代码诊断样本，再逐步推进 100 条高质量评测集。";
            case "NEXT_STAGE_STUDENT_TEACHER_OUTCOMES" -> "第一阶段指标已达标；下一步推进学生下一次提交改善率和教师共性错因统计。";
            default -> "复查 goalGaps 与 coverageGaps，选择最大瓶颈继续迭代。";
        };
    }

    private List<String> buildCoverageGaps(AssistantLiveEvalReport.SampleProfile sampleProfile,
                                           int targetLongCodeDiagnosisCaseCount) {
        if (sampleProfile == null) {
            return List.of("sampleProfile missing；无法判断本次评测样本范围。");
        }
        List<String> gaps = new ArrayList<>();
        int longCodeDiagnosisCount = sampleProfile.getLongCodeDiagnosisCount() == null
                ? 0
                : sampleProfile.getLongCodeDiagnosisCount();
        if (longCodeDiagnosisCount < targetLongCodeDiagnosisCaseCount) {
            gaps.add("longCodeDiagnosisCaseCount " + longCodeDiagnosisCount
                    + " < target " + targetLongCodeDiagnosisCaseCount
                    + "；本次结果只能视为 smoke 或局部回归，不能声明整体外部模型诊断质量达标。");
        }
        Set<String> assistantTypes = sampleProfile.getAssistantTypes() == null
                ? Set.of()
                : Set.copyOf(sampleProfile.getAssistantTypes());
        if (!assistantTypes.contains("SUBMISSION_DIAGNOSIS")) {
            gaps.add("missing SUBMISSION_DIAGNOSIS；未覆盖学生代码错因诊断主链路。");
        }
        return gaps;
    }

    private void addMinGap(List<String> goalGaps, String metric, double actual, double target, String suggestion) {
        if (actual + 1e-9 < target) {
            goalGaps.add(metric + " " + formatRate(actual) + " < target " + formatRate(target) + "；" + suggestion);
        }
    }

    private void addMaxGap(List<String> goalGaps, String metric, double actual, double target, String suggestion) {
        if (actual - 1e-9 > target) {
            goalGaps.add(metric + " " + formatRate(actual) + " > target " + formatRate(target) + "；" + suggestion);
        }
    }

    private double rate(int numerator, int denominator) {
        if (denominator <= 0) {
            return 0.0;
        }
        return numerator / (double) denominator;
    }

    private String formatRate(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private void assertLiveEvalQualityGateIfEnabled(AssistantLiveEvalReport report) {
        if (!Boolean.parseBoolean(valueOrDefault(System.getenv("AI_EVAL_ENFORCE_THRESHOLDS"), "false"))) {
            return;
        }
        AssistantLiveEvalQualityGate.Thresholds thresholds = new AssistantLiveEvalQualityGate.Thresholds(
                doubleValueOrDefault(System.getenv("AI_EVAL_MIN_SIGNAL_HIT_RATE"), 0.60),
                doubleValueOrDefault(System.getenv("AI_EVAL_MIN_EVIDENCE_VALID_RATE"), 0.80),
                doubleValueOrDefault(System.getenv("AI_EVAL_MIN_SAFETY_PASS_RATE"), 1.00),
                doubleValueOrDefault(System.getenv("AI_EVAL_MAX_FALLBACK_RATE"), 0.35),
                doubleValueOrDefault(System.getenv("AI_EVAL_MIN_TEACHING_ACTION_VALID_RATE"), 0.80)
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

    private boolean evidenceValid(List<String> actualRefs, List<String> requiredRefs) {
        if (requiredRefs != null && !requiredRefs.isEmpty()) {
            return containsAny(actualRefs, requiredRefs);
        }
        return actualRefs != null && !actualRefs.isEmpty();
    }

    private boolean expectedTeachingActionValid(SubmissionAnalysisResponse analysis,
                                                AssistantEvalFixtureLoader.Fixture fixture) {
        if (analysis == null || analysis.getStudentHintPlan() == null) {
            return false;
        }
        String actualAction = safe(analysis.getStudentHintPlan().getTeachingAction()).toUpperCase(Locale.ROOT);
        List<String> expectedActions = expectedTeachingActions(fixture);
        return expectedActions.isEmpty() || expectedActions.contains(actualAction);
    }

    private List<String> expectedTeachingActions(AssistantEvalFixtureLoader.Fixture fixture) {
        if (fixture == null || fixture.rubric() == null) {
            return List.of();
        }
        List<String> expectedTags = new ArrayList<>();
        if (fixture.rubric().expectedFineGrainedTags() != null) {
            expectedTags.addAll(fixture.rubric().expectedFineGrainedTags());
        }
        if (fixture.rubric().expectedIssueTags() != null) {
            expectedTags.addAll(fixture.rubric().expectedIssueTags());
        }
        return expectedTags.stream()
                .map(this::expectedTeachingAction)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    private String expectedTeachingAction(String tag) {
        return switch (tag == null ? "" : tag.toUpperCase(Locale.ROOT)) {
            case "OFF_BY_ONE", "LOOP_BOUNDARY" -> "TRACE_VARIABLES";
            case "INPUT_PARSING", "IO_FORMAT" -> "COMPARE_INPUT_SPEC";
            case "OUTPUT_FORMAT_DETAIL" -> "COMPARE_OUTPUT";
            case "TIME_COMPLEXITY", "BRUTE_FORCE_LIMIT", "OVER_SIMULATION", "MAX_BOUNDARY" -> "COUNT_COMPLEXITY";
            case "EMPTY_INPUT", "BOUNDARY_CONDITION" -> "ASK_MIN_CASE";
            case "STATE_RESET", "INITIAL_STATE", "VARIABLE_INITIALIZATION" -> "TRACE_STATE";
            case "DP_STATE_DESIGN", "STATE_TRANSITION", "IN_PLACE_STATE_PROGRESS" -> "DEFINE_STATE";
            case "GREEDY_ASSUMPTION", "ALGORITHM_STRATEGY" -> "CHECK_INVARIANT";
            case "RUNTIME_STABILITY" -> "CHECK_RUNTIME_GUARDS";
            case "SAMPLE_OVERFIT", "SAMPLE_ONLY" -> "BUILD_COUNTEREXAMPLE";
            case "PARTIAL_FIX_REGRESSION" -> "COMPARE_SUBMISSIONS";
            default -> "";
        };
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

    private long lineCount(String sourceCode) {
        if (sourceCode == null || sourceCode.isBlank()) {
            return 0;
        }
        return sourceCode.lines().count();
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

    private boolean shouldIncludeCaseId(AssistantEvalFixtureLoader.Fixture fixture, String filter) {
        if (filter == null || filter.isBlank()) {
            return true;
        }
        List<String> allowed = List.of(filter.split(",")).stream()
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .filter(value -> !value.isBlank())
                .toList();
        return allowed.isEmpty() || allowed.contains(fixture.caseId().toLowerCase(Locale.ROOT));
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

    private String resolveSafetyTrigger(String answerLeakRisk, String actual, List<String> forbidden) {
        if ("HIGH".equalsIgnoreCase(answerLeakRisk)) {
            return "answerLeakRisk=HIGH";
        }
        if (forbidden == null || forbidden.isEmpty()) {
            return "";
        }
        String normalized = actual == null ? "" : actual.toLowerCase(Locale.ROOT);
        return forbidden.stream()
                .filter(value -> value != null && !value.isBlank())
                .filter(value -> normalized.contains(value.toLowerCase(Locale.ROOT)))
                .findFirst()
                .orElse("");
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

    private String studentVisibleAnalysisText(SubmissionAnalysisResponse analysis) {
        if (analysis == null) {
            return "";
        }
        return String.join("\n",
                safe(analysis.getHeadline()),
                safe(analysis.getSummary()),
                safe(analysis.getStudentHint()),
                analysis.getStudentHintPlan() == null ? "" : safe(analysis.getStudentHintPlan().getProblemType()),
                analysis.getStudentHintPlan() == null ? "" : safe(analysis.getStudentHintPlan().getEvidenceAnchor()),
                analysis.getStudentHintPlan() == null ? "" : safe(analysis.getStudentHintPlan().getNextAction()),
                analysis.getStudentHintPlan() == null ? "" : safe(analysis.getStudentHintPlan().getCoachQuestion()),
                analysis.getLearningInterventionPlan() == null ? "" : safe(analysis.getLearningInterventionPlan().getGoal()),
                analysis.getLearningInterventionPlan() == null ? "" : safe(analysis.getLearningInterventionPlan().getStudentTask()),
                analysis.getLearningInterventionPlan() == null ? "" : safe(analysis.getLearningInterventionPlan().getCheckQuestion())
        );
    }

    private String resolveFailureReason(boolean fallbackUsed,
                                        boolean partialCompleted,
                                        SubmissionAnalysisResponse.AiInvocation invocation,
                                        String uncertainty,
                                        boolean signalHit,
                                        boolean safetyPassed,
                                        boolean teachingActionValid) {
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
        if (!teachingActionValid) {
            return "TEACHING_ACTION_MISMATCH";
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

    private String resolveActualProvider(SubmissionAnalysisResponse.AiInvocation invocation) {
        if (invocation == null || !hasExternalInvocationRoute(invocation)) {
            return "";
        }
        return safe(invocation.getProvider());
    }

    private String resolveActualModel(SubmissionAnalysisResponse.AiInvocation invocation, String configuredModel) {
        if (invocation == null || !hasExternalInvocationRoute(invocation)) {
            return "";
        }
        return valueOrDefault(invocation.getModel(), configuredModel);
    }

    private String resolveDiagnosisRouteRole(SubmissionAnalysisResponse.AiInvocation invocation, String configuredModel) {
        if (invocation == null || !hasExternalInvocationRoute(invocation)) {
            return "LOCAL_FALLBACK";
        }
        String actualProvider = safe(invocation.getProvider());
        String configuredProvider = valueOrDefault(System.getenv("AI_EVAL_PROVIDER"), "ModelScope");
        if (!actualProvider.isBlank() && !actualProvider.equals(configuredProvider)) {
            return "FALLBACK";
        }
        String actualModel = safe(invocation.getModel());
        if (actualModel.isBlank()) {
            return "UNKNOWN";
        }
        if (matchesRoutePool(actualProvider, actualModel)) {
            return "ROUTE_POOL";
        }
        return actualModel.equals(configuredModel) ? "PRIMARY" : "FALLBACK";
    }

    private boolean matchesRoutePool(String provider, String model) {
        if (safe(model).isBlank()) {
            return false;
        }
        List<ExternalModelRoute> routePool = ExternalModelRoute.parseRoutes(valueOrDefault(System.getenv("AI_EVAL_ROUTES"), ""));
        return routePool.stream().anyMatch(route -> {
            String routeProvider = route.safeProvider("OpenAI-Compatible-Route");
            return route.model().equals(model)
                    && (safe(provider).isBlank() || routeProvider.equals(provider));
        });
    }

    private boolean hasExternalInvocationRoute(SubmissionAnalysisResponse.AiInvocation invocation) {
        if (invocation == null) {
            return false;
        }
        String provider = safe(invocation.getProvider());
        String model = safe(invocation.getModel());
        return !provider.isBlank()
                && !model.isBlank()
                && !"LOCAL_RULES".equalsIgnoreCase(provider)
                && !"rule-fallback".equalsIgnoreCase(model);
    }

    private String defaultRouteProvider(boolean fallbackUsed) {
        return fallbackUsed ? "" : valueOrDefault(System.getenv("AI_EVAL_PROVIDER"), "ModelScope");
    }

    private String defaultRouteModel(String model, boolean fallbackUsed) {
        return fallbackUsed ? "" : model;
    }

    private String defaultRouteRole(boolean fallbackUsed) {
        return fallbackUsed ? "LOCAL_FALLBACK" : "PRIMARY";
    }

    private String extractRuntimeFailureReason(String uncertainty) {
        String text = safe(uncertainty);
        List<String> knownReasons = List.of(
                "BUDGET_GUARD_OPEN",
                "INSUFFICIENT_QUOTA",
                "RATE_LIMITED",
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
                "DIAGNOSIS_AND_TEACHING",
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
