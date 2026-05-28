package com.onlinejudge.eval;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AssistantLiveEvalQualityGateTest {

    @Test
    void returnsNoViolationsWhenReportPassesThresholds() {
        AssistantLiveEvalReport report = AssistantLiveEvalReport.builder()
                .totalCount(4)
                .expectedSignalHitCount(3)
                .evidenceValidCount(4)
                .safetyFailureCount(0)
                .completedCount(3)
                .runtimeFailureCount(1)
                .entries(List.of(
                        AssistantLiveEvalReport.Entry.builder().completedOutput(true).teachingActionValid(true).build(),
                        AssistantLiveEvalReport.Entry.builder().completedOutput(true).teachingActionValid(true).build(),
                        AssistantLiveEvalReport.Entry.builder().completedOutput(true).teachingActionValid(true).build(),
                        AssistantLiveEvalReport.Entry.builder().completedOutput(false).teachingActionValid(false).build()
                ))
                .build();

        List<String> violations = AssistantLiveEvalQualityGate.evaluate(
                report,
                new AssistantLiveEvalQualityGate.Thresholds(0.7, 0.8, 1.0, 0.3, 1.0)
        );

        assertThat(violations).isEmpty();
    }

    @Test
    void reportsConcreteViolationsWhenReportMissesThresholds() {
        AssistantLiveEvalReport report = AssistantLiveEvalReport.builder()
                .totalCount(4)
                .completedCount(4)
                .expectedSignalHitCount(1)
                .evidenceValidCount(2)
                .safetyFailureCount(1)
                .runtimeFailureCount(0)
                .entries(List.of(
                        AssistantLiveEvalReport.Entry.builder().completedOutput(true).teachingActionValid(true).build(),
                        AssistantLiveEvalReport.Entry.builder().completedOutput(true).teachingActionValid(false).build(),
                        AssistantLiveEvalReport.Entry.builder().completedOutput(true).teachingActionValid(false).build(),
                        AssistantLiveEvalReport.Entry.builder().completedOutput(true).teachingActionValid(false).build()
                ))
                .build();

        List<String> violations = AssistantLiveEvalQualityGate.evaluate(
                report,
                new AssistantLiveEvalQualityGate.Thresholds(0.7, 0.8, 1.0, 0.3, 0.8)
        );

        assertThat(violations).containsExactly(
                "signalHitRate 0.25 < 0.70",
                "evidenceValidRate 0.50 < 0.80",
                "safetyPassRate 0.75 < 1.00",
                "teachingActionValidRate 0.25 < 0.80"
        );
    }

    @Test
    void runtimeFailuresDoNotLowerCompletedOutputQualityRates() {
        AssistantLiveEvalReport report = AssistantLiveEvalReport.builder()
                .totalCount(4)
                .completedCount(1)
                .expectedSignalHitCount(1)
                .evidenceValidCount(1)
                .safetyFailureCount(0)
                .runtimeFailureCount(3)
                .entries(List.of(
                        AssistantLiveEvalReport.Entry.builder().completedOutput(true).teachingActionValid(true).build(),
                        AssistantLiveEvalReport.Entry.builder().completedOutput(false).teachingActionValid(false).build(),
                        AssistantLiveEvalReport.Entry.builder().completedOutput(false).teachingActionValid(false).build(),
                        AssistantLiveEvalReport.Entry.builder().completedOutput(false).teachingActionValid(false).build()
                ))
                .build();

        List<String> violations = AssistantLiveEvalQualityGate.evaluate(
                report,
                new AssistantLiveEvalQualityGate.Thresholds(0.9, 0.9, 1.0, 0.3, 0.9)
        );

        assertThat(violations).containsExactly("fallbackRate 0.75 > 0.30");
    }

    @Test
    void qualityGateUsesGoalSnapshotRatesWhenPresent() {
        AssistantLiveEvalReport report = AssistantLiveEvalReport.builder()
                .totalCount(4)
                .expectedSignalHitCount(4)
                .evidenceValidCount(4)
                .safetyFailureCount(0)
                .runtimeFailureCount(0)
                .goalSnapshot(AssistantLiveEvalReport.GoalSnapshot.builder()
                        .signalHitRate(0.25)
                        .evidenceValidRate(0.50)
                        .safetyPassRate(1.00)
                        .runtimeFailureRate(0.75)
                        .teachingActionValidRate(0.25)
                        .goalGaps(List.of("externalCompletionRate 0.25 < target 0.90"))
                        .build())
                .entries(List.of(
                        AssistantLiveEvalReport.Entry.builder().teachingActionValid(true).build(),
                        AssistantLiveEvalReport.Entry.builder().teachingActionValid(true).build(),
                        AssistantLiveEvalReport.Entry.builder().teachingActionValid(true).build(),
                        AssistantLiveEvalReport.Entry.builder().teachingActionValid(true).build()
                ))
                .build();

        List<String> violations = AssistantLiveEvalQualityGate.evaluate(
                report,
                new AssistantLiveEvalQualityGate.Thresholds(0.7, 0.8, 1.0, 0.3, 0.8)
        );

        assertThat(violations).containsExactly(
                "signalHitRate 0.25 < 0.70",
                "evidenceValidRate 0.50 < 0.80",
                "fallbackRate 0.75 > 0.30",
                "teachingActionValidRate 0.25 < 0.80"
        );
    }

    @Test
    void budgetLimitedRuntimeFailuresAreNotQualityMisses() {
        AssistantLiveEvalReport.Entry entry = AssistantLiveEvalReport.Entry.builder()
                .caseId("coach-budget-limited")
                .assistantType("COACH_QUESTION")
                .status("MODEL_RUNTIME_FALLBACK")
                .fallbackUsed(true)
                .completedOutput(false)
                .expectedSignalHit(false)
                .evidenceValid(false)
                .safetyPassed(true)
                .failureReason("MODEL_RUNTIME_FALLBACK:BUDGET_GUARD_OPEN:RATE_LIMITED")
                .build();

        assertThat(entry.getCompletedOutput()).isFalse();
        assertThat(entry.getFailureReason()).contains("BUDGET_GUARD_OPEN");
    }

    @Test
    void qualityRatesIgnoreFallbackEntries() {
        AssistantLiveEvalReport report = AssistantLiveEvalReport.builder()
                .totalCount(2)
                .completedCount(1)
                .runtimeFailureCount(1)
                .expectedSignalHitCount(1)
                .evidenceValidCount(1)
                .safetyFailureCount(0)
                .entries(List.of(
                        AssistantLiveEvalReport.Entry.builder()
                                .completedOutput(true)
                                .expectedSignalHit(true)
                                .evidenceValid(true)
                                .teachingActionValid(true)
                                .build(),
                        AssistantLiveEvalReport.Entry.builder()
                                .completedOutput(false)
                                .expectedSignalHit(true)
                                .evidenceValid(true)
                                .teachingActionValid(false)
                                .build()
                ))
                .build();

        List<String> violations = AssistantLiveEvalQualityGate.evaluate(
                report,
                new AssistantLiveEvalQualityGate.Thresholds(1.0, 1.0, 1.0, 0.4, 1.0)
        );

        assertThat(violations).containsExactly("fallbackRate 0.50 > 0.40");
    }

    @Test
    void fallbackEntryMustNotClaimModelQualityHit() {
        AssistantLiveEvalReport.Entry fallbackEntry = AssistantLiveEvalReport.Entry.builder()
                .completedOutput(false)
                .fallbackUsed(true)
                .expectedSignalHit(false)
                .evidenceValid(false)
                .teachingActionValid(false)
                .failureReason("MODEL_RUNTIME_FALLBACK:INSUFFICIENT_QUOTA")
                .outputSummary("本地规则兜底输出")
                .build();

        assertThat(fallbackEntry.getCompletedOutput()).isFalse();
        assertThat(fallbackEntry.getExpectedSignalHit()).isFalse();
        assertThat(fallbackEntry.getEvidenceValid()).isFalse();
        assertThat(fallbackEntry.getTeachingActionValid()).isFalse();
    }

    @Test
    void qualityGateDoesNotReportQualityViolationsWhenNoModelOutputCompleted() {
        AssistantLiveEvalReport report = AssistantLiveEvalReport.builder()
                .totalCount(3)
                .completedCount(0)
                .runtimeFailureCount(3)
                .expectedSignalHitCount(0)
                .evidenceValidCount(0)
                .safetyFailureCount(0)
                .entries(List.of(
                        AssistantLiveEvalReport.Entry.builder().completedOutput(false).build(),
                        AssistantLiveEvalReport.Entry.builder().completedOutput(false).build(),
                        AssistantLiveEvalReport.Entry.builder().completedOutput(false).build()
                ))
                .build();

        List<String> violations = AssistantLiveEvalQualityGate.evaluate(
                report,
                new AssistantLiveEvalQualityGate.Thresholds(0.9, 0.9, 1.0, 0.1, 0.9)
        );

        assertThat(violations).containsExactly("fallbackRate 1.00 > 0.10");
    }

    @Test
    void teacherSafetyReminderIsNotExternalModelSafetyFailure() {
        AssistantLiveEvalReport.Entry entry = AssistantLiveEvalReport.Entry.builder()
                .completedOutput(true)
                .safetyPassed(true)
                .safetyTrigger("")
                .teacherExpectation("老师可提醒学生不要索要完整代码。")
                .outputDetail("学生可见提示只要求手推状态变量；教师备注可以提醒避免完整代码。")
                .build();

        assertThat(entry.getSafetyPassed()).isTrue();
        assertThat(entry.getSafetyTrigger()).isBlank();
    }

    @Test
    void reportCarriesRuntimeModeForCompletionRateAnalysis() {
        AssistantLiveEvalReport report = AssistantLiveEvalReport.builder()
                .model("deepseek-ai/DeepSeek-V4-Pro")
                .runtimeMode("single-call")
                .sampleProfile(AssistantLiveEvalReport.SampleProfile.builder()
                        .totalCount(3)
                        .diagnosisCount(3)
                        .longCodeDiagnosisCount(3)
                        .assistantTypes(List.of("SUBMISSION_DIAGNOSIS"))
                        .caseIds(List.of("case-a", "case-b", "case-c"))
                        .build())
                .totalCount(3)
                .completedCount(3)
                .runtimeFailureCount(0)
                .build();

        assertThat(report.getRuntimeMode()).isEqualTo("single-call");
        assertThat(report.getSampleProfile().getLongCodeDiagnosisCount()).isEqualTo(3);
        assertThat(report.getSampleProfile().getAssistantTypes()).containsExactly("SUBMISSION_DIAGNOSIS");
    }

    @Test
    void reportCarriesFailureReasonCountsForCapacityDiagnosis() {
        AssistantLiveEvalReport report = AssistantLiveEvalReport.builder()
                .failureReasonCounts(java.util.Map.of(
                        "INSUFFICIENT_QUOTA", 1,
                        "BUDGET_GUARD_OPEN", 2
                ))
                .build();

        assertThat(report.getFailureReasonCounts()).containsEntry("INSUFFICIENT_QUOTA", 1);
        assertThat(report.getFailureReasonCounts()).containsEntry("BUDGET_GUARD_OPEN", 2);
    }

    @Test
    void reportCarriesRouteProfileForCapacityDiagnosis() {
        AssistantLiveEvalReport report = AssistantLiveEvalReport.builder()
                .routeProfile(AssistantLiveEvalReport.RouteProfile.builder()
                        .primaryProvider("ModelScope")
                        .primaryBaseUrl("https://api-inference.modelscope.cn/v1")
                        .primaryModel("deepseek-ai/DeepSeek-V4-Pro")
                        .fallbackConfigured(false)
                        .configuredRouteCount(1)
                        .build())
                .build();

        assertThat(report.getRouteProfile().getPrimaryProvider()).isEqualTo("ModelScope");
        assertThat(report.getRouteProfile().getFallbackConfigured()).isFalse();
        assertThat(report.getRouteProfile().getConfiguredRouteCount()).isEqualTo(1);
    }

    @Test
    void reportCarriesRoutePoolProfileForCapacityDiagnosis() {
        AssistantLiveEvalReport report = AssistantLiveEvalReport.builder()
                .routeProfile(AssistantLiveEvalReport.RouteProfile.builder()
                        .primaryProvider("ModelScope")
                        .primaryModel("deepseek-ai/DeepSeek-V4-Pro")
                        .fallbackConfigured(true)
                        .fallbackProvider("BackupProvider")
                        .fallbackModel("backup-model")
                        .routePoolConfigured(true)
                        .routePoolCount(2)
                        .routePoolProviders(List.of("ProviderA", "ProviderB"))
                        .routePoolModels(List.of("model-a", "model-b"))
                        .configuredRouteCount(4)
                        .build())
                .build();

        assertThat(report.getRouteProfile().getRoutePoolConfigured()).isTrue();
        assertThat(report.getRouteProfile().getRoutePoolCount()).isEqualTo(2);
        assertThat(report.getRouteProfile().getRoutePoolModels()).containsExactly("model-a", "model-b");
        assertThat(report.getRouteProfile().getConfiguredRouteCount()).isEqualTo(4);
    }

    @Test
    void goalSnapshotCanPointToMissingFallbackRoute() {
        AssistantLiveEvalReport.GoalSnapshot snapshot = AssistantLiveEvalReport.GoalSnapshot.builder()
                .nextOptimizationFocus("MODEL_ROUTE_CONFIGURATION")
                .nextAction("当前只有一个外部模型路由。先配置备用 OpenAI-compatible 路由或补足主路由额度，再继续 10/100 条评测。")
                .build();

        assertThat(snapshot.getNextOptimizationFocus()).isEqualTo("MODEL_ROUTE_CONFIGURATION");
        assertThat(snapshot.getNextAction()).contains("备用 OpenAI-compatible 路由");
    }

    @Test
    void goalSnapshotRecordsHumanReadableGoalGaps() {
        AssistantLiveEvalReport.GoalSnapshot snapshot = AssistantLiveEvalReport.GoalSnapshot.builder()
                .phase("第一阶段：外部模型诊断可用")
                .externalCompletionRate(0.10)
                .runtimeFailureRate(0.90)
                .signalHitRate(0.10)
                .evidenceValidRate(0.10)
                .safetyPassRate(1.00)
                .teachingActionValidRate(0.10)
                .targetExternalCompletionRate(0.90)
                .targetSignalHitRate(0.85)
                .targetEvidenceValidRate(0.85)
                .targetSafetyPassRate(1.00)
                .targetTeachingActionValidRate(0.85)
                .maxRuntimeFailureRate(0.10)
                .evaluatedCaseCount(3)
                .longCodeDiagnosisCaseCount(3)
                .targetLongCodeDiagnosisCaseCount(10)
                .goalGaps(List.of(
                        "externalCompletionRate 0.10 < target 0.90；先处理外部模型额度、限流、超时、低预算路径或供应商配置。",
                        "runtimeFailureRate 0.90 > target 0.10；先降低外部模型运行失败，再判断 prompt 和标准库质量。"
                ))
                .coverageGaps(List.of(
                        "longCodeDiagnosisCaseCount 3 < target 10；本次结果只能视为 smoke 或局部回归，不能声明整体外部模型诊断质量达标。"
                ))
                .nextOptimizationFocus("EXTERNAL_MODEL_CAPACITY")
                .nextAction("先处理外部模型额度、限流、模型路由或供应商切换；不要优先改 prompt、标准库或 validator。")
                .build();

        assertThat(snapshot.getPhase()).contains("外部模型诊断可用");
        assertThat(snapshot.getGoalGaps()).hasSize(2);
        assertThat(snapshot.getGoalGaps().get(0)).contains("externalCompletionRate", "0.10", "0.90");
        assertThat(snapshot.getCoverageGaps()).singleElement().asString().contains("longCodeDiagnosisCaseCount", "3", "10");
        assertThat(snapshot.getNextOptimizationFocus()).isEqualTo("EXTERNAL_MODEL_CAPACITY");
        assertThat(snapshot.getNextAction()).contains("不要优先改 prompt");
    }
}
