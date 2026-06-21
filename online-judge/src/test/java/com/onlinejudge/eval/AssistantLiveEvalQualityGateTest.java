package com.onlinejudge.eval;

import com.fasterxml.jackson.databind.ObjectMapper;
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
                .runtimeFailureCount(1)
                .entries(List.of(
                        AssistantLiveEvalReport.Entry.builder().teachingActionValid(true).build(),
                        AssistantLiveEvalReport.Entry.builder().teachingActionValid(true).build(),
                        AssistantLiveEvalReport.Entry.builder().teachingActionValid(true).build(),
                        AssistantLiveEvalReport.Entry.builder().teachingActionValid(true).build()
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
                .expectedSignalHitCount(1)
                .evidenceValidCount(2)
                .safetyFailureCount(1)
                .runtimeFailureCount(3)
                .entries(List.of(
                        AssistantLiveEvalReport.Entry.builder().teachingActionValid(true).build(),
                        AssistantLiveEvalReport.Entry.builder().teachingActionValid(false).build(),
                        AssistantLiveEvalReport.Entry.builder().teachingActionValid(false).build(),
                        AssistantLiveEvalReport.Entry.builder().teachingActionValid(false).build()
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
    void runtimeDraftFactoryExportsFallbackPartialAndQualityMissDraftsWithRedaction() {
        List<AssistantLiveEvalReport.Entry> entries = List.of(
                AssistantLiveEvalReport.Entry.builder()
                        .caseId("coach-budget-limited")
                        .assistantType("COACH_QUESTION")
                        .model("deepseek-ai/DeepSeek-V4-Pro")
                        .promptVersion("coach-question-v1")
                        .status("MODEL_RUNTIME_FALLBACK")
                        .fallbackUsed(true)
                        .completedOutput(false)
                        .failureStage("COACH_QUESTION")
                        .failureReason("MODEL_RUNTIME_FALLBACK:BUDGET_GUARD_OPEN token=ms-secret-token-should-not-leak")
                        .actualEvidenceRefs(List.of("coach:case:1"))
                        .teacherExpectation("追问最小样例")
                        .outputSummary("provider said api_key=ms-secret-token-should-not-leak")
                        .iterationSuggestion("优先处理预算保护")
                        .build(),
                AssistantLiveEvalReport.Entry.builder()
                        .caseId("diagnosis-partial")
                        .assistantType("SUBMISSION_DIAGNOSIS")
                        .model("deepseek-ai/DeepSeek-V4-Pro")
                        .promptVersion("diagnosis-and-advice-v1")
                        .status("MODEL_PARTIAL_COMPLETED")
                        .fallbackUsed(false)
                        .completedOutput(true)
                        .failureStage("DIAGNOSIS_AND_ADVICE")
                        .failureReason("MODEL_PARTIAL_COMPLETED:DIAGNOSIS_AND_ADVICE:SAFETY_RISK")
                        .actualEvidenceRefs(List.of("case:2"))
                        .teacherExpectation("保留诊断，复核提示")
                        .build(),
                AssistantLiveEvalReport.Entry.builder()
                        .caseId("diagnosis-quality-miss")
                        .assistantType("SUBMISSION_DIAGNOSIS")
                        .model("deepseek-ai/DeepSeek-V4-Pro")
                        .promptVersion("diagnosis-and-advice-v1")
                        .status("MODEL_COMPLETED")
                        .fallbackUsed(false)
                        .completedOutput(true)
                        .failureStage("NONE")
                        .failureReason("QUALITY_MISS")
                        .actualEvidenceRefs(List.of("case:3"))
                        .teacherExpectation("命中输入读取错因")
                        .build()
        );

        List<LiveEvalRuntimeFixtureDraft> drafts =
                new LiveEvalRuntimeFixtureDraftFactory().fromAssistantEntries(entries);

        assertThat(drafts).hasSize(3);
        assertThat(drafts).extracting(LiveEvalRuntimeFixtureDraft::getFailureType)
                .containsExactly("BUDGET_GUARD", "PARTIAL_COMPLETION", "QUALITY_MISS");
        assertThat(drafts.get(0))
                .satisfies(draft -> {
                    assertThat(draft.getName()).contains("assistant-live-eval-coach-budget-limited-budget-guard");
                    assertThat(draft.getSourceReportType()).isEqualTo("assistant-live-eval");
                    assertThat(draft.getExpectedRuntimeAction()).contains("解除预算保护", "重跑小样本 live eval");
                    assertThat(draft.getEvidenceRefs()).contains("live_eval_case:coach-budget-limited", "coach:case:1");
                    assertThat(draft.getFailureReason()).contains("BUDGET_GUARD_OPEN", "[redacted]");
                    assertThat(draft.getFailureReason()).doesNotContain("ms-secret-token-should-not-leak");
                    assertThat(draft.getOutputSummary()).contains("[redacted]");
                    assertThat(draft.getOutputSummary()).doesNotContain("ms-secret-token-should-not-leak");
                    assertThat(draft.getMustNotMention()).contains("API Key", "token", "密钥");
                });
        assertThat(drafts.get(1).getExpectedRuntimeAction()).contains("保留可用诊断", "复核教学提示阶段");
        assertThat(drafts.get(2).getExpectedRuntimeAction()).contains("prompt", "rubric", "fixture");
    }

    @Test
    void runtimeDraftFactoryExportsModelEvalRuntimeDrafts() throws Exception {
        List<com.onlinejudge.submission.application.LiveModelEvalReport.Entry> entries = List.of(
                com.onlinejudge.submission.application.LiveModelEvalReport.Entry.builder()
                        .caseId("off-by-one-smoke")
                        .model("deepseek-ai/DeepSeek-V4-Pro")
                        .promptVersion("diagnosis-and-advice-v1")
                        .stage("DIAGNOSIS_AGENT")
                        .status("MODEL_RUNTIME_FALLBACK")
                        .failureStage("DIAGNOSIS_AND_ADVICE")
                        .latencyMs(650L)
                        .latencyBudgetMs(35_000L)
                        .latencyBudgetExceeded(false)
                        .fallbackUsed(true)
                        .transportMode("stream")
                        .streamChunkCount(0)
                        .streamContentChunkCount(0)
                        .streamReasoningChunkCount(0)
                        .streamInvalidChunkCount(2)
                        .streamFinishReason("")
                        .streamFallbackRetryUsed(true)
                        .failureReason("MODEL_RUNTIME_FALLBACK:DIAGNOSIS_AND_ADVICE:INSUFFICIENT_QUOTA")
                        .outputSummary("fallback")
                        .build(),
                com.onlinejudge.submission.application.LiveModelEvalReport.Entry.builder()
                        .caseId("budget-guard-smoke")
                        .model("deepseek-ai/DeepSeek-V4-Pro")
                        .promptVersion("diagnosis-and-advice-v1")
                        .stage("DIAGNOSIS_AGENT")
                        .status("MODEL_RUNTIME_FALLBACK")
                        .failureStage("DIAGNOSIS_AND_ADVICE")
                        .latencyMs(10L)
                        .latencyBudgetMs(35_000L)
                        .latencyBudgetExceeded(false)
                        .fallbackUsed(true)
                        .failureReason("MODEL_RUNTIME_FALLBACK:BUDGET_GUARD_OPEN:RATE_LIMITED")
                        .outputSummary("budget guard")
                        .build(),
                com.onlinejudge.submission.application.LiveModelEvalReport.Entry.builder()
                        .caseId("output-truncated-smoke")
                        .model("deepseek-ai/DeepSeek-V4-Pro")
                        .promptVersion("diagnosis-and-advice-v1")
                        .stage("DIAGNOSIS_AGENT")
                        .status("MODEL_RUNTIME_FALLBACK")
                        .failureStage("DIAGNOSIS_AND_ADVICE")
                        .latencyMs(33_589L)
                        .latencyBudgetMs(35_000L)
                        .latencyBudgetExceeded(false)
                        .fallbackUsed(true)
                        .transportMode("stream")
                        .streamChunkCount(242)
                        .streamContentChunkCount(77)
                        .streamReasoningChunkCount(164)
                        .streamInvalidChunkCount(0)
                        .streamFinishReason("length")
                        .streamFallbackRetryUsed(false)
                        .failureReason("MODEL_RUNTIME_FALLBACK:DIAGNOSIS_AND_ADVICE:OUTPUT_TRUNCATED")
                        .outputSummary("fallback")
                        .build(),
                com.onlinejudge.submission.application.LiveModelEvalReport.Entry.builder()
                        .caseId("rate-limited-smoke")
                        .model("deepseek-ai/DeepSeek-V4-Pro")
                        .promptVersion("diagnosis-and-advice-v1")
                        .stage("DIAGNOSIS_AGENT")
                        .status("MODEL_RUNTIME_FALLBACK")
                        .failureStage("DIAGNOSIS_AND_ADVICE")
                        .latencyMs(525L)
                        .latencyBudgetMs(35_000L)
                        .latencyBudgetExceeded(false)
                        .fallbackUsed(true)
                        .transportMode("stream")
                        .streamChunkCount(0)
                        .streamContentChunkCount(0)
                        .streamReasoningChunkCount(0)
                        .streamInvalidChunkCount(0)
                        .failureReason("MODEL_RUNTIME_FALLBACK:DIAGNOSIS_AND_ADVICE:RATE_LIMITED")
                        .outputSummary("fallback")
                        .build(),
                com.onlinejudge.submission.application.LiveModelEvalReport.Entry.builder()
                        .caseId("provider-error-smoke")
                        .model("deepseek-ai/DeepSeek-V4-Pro")
                        .promptVersion("diagnosis-and-advice-v1")
                        .stage("DIAGNOSIS_AGENT")
                        .status("MODEL_RUNTIME_FALLBACK")
                        .failureStage("DIAGNOSIS_AND_ADVICE")
                        .latencyMs(900L)
                        .latencyBudgetMs(35_000L)
                        .latencyBudgetExceeded(false)
                        .fallbackUsed(true)
                        .failureReason("MODEL_RUNTIME_FALLBACK:DIAGNOSIS_AND_ADVICE:HTTP_500_PROVIDER_DOWN")
                        .outputSummary("fallback")
                        .build()
                ,
                com.onlinejudge.submission.application.LiveModelEvalReport.Entry.builder()
                        .caseId("quality-miss-smoke")
                        .model("deepseek-ai/DeepSeek-V4-Pro")
                        .promptVersion("diagnosis-and-advice-v1")
                        .stage("DIAGNOSIS_AGENT")
                        .status("MODEL_COMPLETED")
                        .failureStage("DIAGNOSIS_AND_ADVICE")
                        .latencyMs(1_200L)
                        .latencyBudgetMs(35_000L)
                        .latencyBudgetExceeded(false)
                        .fallbackUsed(false)
                        .failureReason("QUALITY_MISS")
                        .outputSummary("model completed but missed expected issue")
                        .build()
        );

        List<LiveEvalRuntimeFixtureDraft> drafts =
                new LiveEvalRuntimeFixtureDraftFactory().fromModelEntries(entries);

        assertThat(drafts).hasSize(6);
        assertThat(drafts.get(0))
                .satisfies(draft -> {
                    assertThat(draft.getSourceReportType()).isEqualTo("live-model-eval");
                    assertThat(draft.getAssistantType()).isEqualTo("SUBMISSION_DIAGNOSIS");
                    assertThat(draft.getFailureType()).isEqualTo("QUOTA_LIMIT");
                    assertThat(draft.getTransportMode()).isEqualTo("stream");
                    assertThat(draft.getStreamChunkCount()).isZero();
                    assertThat(draft.getStreamContentChunkCount()).isZero();
                    assertThat(draft.getStreamReasoningChunkCount()).isZero();
                    assertThat(draft.getStreamInvalidChunkCount()).isEqualTo(2);
                    assertThat(draft.getStreamFinishReason()).isEmpty();
                    assertThat(draft.getStreamFallbackRetryUsed()).isTrue();
                    assertThat(draft.getExpectedRuntimeAction()).contains("ModelScope 额度", "降低 live eval 调用规模",
                            "offline runtime profile eval", "offline-runtime-profile-eval-*.json",
                            "request bytes", "requestCompact=true", "结构锚点", "stream smoke",
                            "content chunk", "SSE/JSON", "fallback retry");
                    assertThat(draft.getIterationSuggestion()).contains("offline-runtime-profile-eval-*.json",
                            "content chunk", "SSE/JSON", "fallback retry");
                    assertThat(draft.getOfflineProfileEvalRecommended()).isTrue();
                    assertThat(draft.getOfflineProfileReportPattern())
                            .isEqualTo("target/ai-eval-reports/offline-runtime-profile-eval-*.json");
                    assertThat(draft.getOfflineProfileCaseId()).isEqualTo("off-by-one-smoke");
                    assertThat(draft.getOfflineProfileRequiredChecks()).contains(
                            "lowLatencyRequestBytes < standardRequestBytes",
                            "lowLatencyRequestCompact=true",
                            "compressionRatio < 1.0",
                            "candidateSignalCount > 0",
                            "evidenceRefCount > 0",
                            "issueTagCount > 0",
                            "teachingActionCount > 0",
                            "hiddenBoundaryPresent=true"
                    );
                    assertThat(draft.getRecoverySmokeRecommended()).isTrue();
                    assertThat(draft.getRecoverySmokeCaseId()).isEqualTo("off-by-one-smoke");
                    assertThat(draft.getRecoverySmokeRuntimeProfile()).isEqualTo("low-latency");
                    assertThat(draft.getRecoverySmokeCommandHint()).contains(
                            "AI_EVAL_RUNTIME_PROFILE=low-latency",
                            "ModelDiagnosisEvalTest#liveModelSmokeProducesPerCaseReportWhenEnabled",
                            "caseId=off-by-one-smoke"
                    );
                    assertThat(draft.getRecoverySmokeRequiredChecks()).contains(
                            "status=MODEL_COMPLETED",
                            "fallbackUsed=false",
                            "modelCompleted=true",
                            "modelIssueTagHit=true or modelFineTagHit=true",
                            "evidenceValid=true",
                            "safetyPassed=true",
                            "streamContentChunkCount>0"
                    );
                    assertThat(draft.getEvidenceRefs()).contains("live_eval_case:off-by-one-smoke");
                    assertThat(draft.getMustMention()).contains("transport:stream", "streamContentChunkCount=0",
                            "streamInvalidChunkCount=2", "streamFallbackRetryUsed=true");
                });
        String serializedQuotaDraft = new ObjectMapper().writeValueAsString(drafts.get(0));
        assertThat(serializedQuotaDraft)
                .doesNotContain("\"messages\"")
                .doesNotContain("\"brief\"")
                .doesNotContain("\"standardLibrary\"")
                .doesNotContain("sourceCode")
                .doesNotContain("api_key=ms-")
                .doesNotContain("Authorization: Bearer")
                .doesNotContain("Bearer " + "ms-")
                .doesNotContain("ms-secret")
                .doesNotContain("ms-");
        assertThat(drafts.get(1))
                .satisfies(draft -> {
                    assertThat(draft.getFailureType()).isEqualTo("BUDGET_GUARD");
                    assertThat(draft.getTransportMode()).isEmpty();
                    assertThat(draft.getOfflineProfileEvalRecommended()).isFalse();
                    assertThat(draft.getOfflineProfileReportPattern()).isEmpty();
                    assertThat(draft.getOfflineProfileCaseId()).isEmpty();
                    assertThat(draft.getOfflineProfileRequiredChecks()).isEmpty();
                    assertThat(draft.getRecoverySmokeRecommended()).isTrue();
                    assertThat(draft.getRecoverySmokeCaseId()).isEqualTo("budget-guard-smoke");
                    assertThat(draft.getRecoverySmokeRequiredChecks()).contains("status=MODEL_COMPLETED", "fallbackUsed=false");
                    assertThat(draft.getExpectedRuntimeAction()).contains("解除预算保护", "重跑小样本 live eval");
                    assertThat(draft.getExpectedRuntimeAction()).doesNotContain("content chunk", "SSE/JSON", "fallback retry");
                });
        assertThat(drafts.get(2))
                .satisfies(draft -> {
                    assertThat(draft.getFailureType()).isEqualTo("OUTPUT_TRUNCATED");
                    assertThat(draft.getStreamFinishReason()).isEqualTo("length");
                    assertThat(draft.getOfflineProfileEvalRecommended()).isFalse();
                    assertThat(draft.getOfflineProfileRequiredChecks()).isEmpty();
                    assertThat(draft.getRecoverySmokeRecommended()).isFalse();
                    assertThat(draft.getRecoverySmokeRequiredChecks()).isEmpty();
                    assertThat(draft.getExpectedRuntimeAction()).contains("输出 token 预算", "JSON schema", "结构化重试", "finish_reason=length");
                    assertThat(draft.getMustMention()).contains("OUTPUT_TRUNCATED", "输出截断", "streamFinishReason=length");
                });
        assertThat(drafts.get(3))
                .satisfies(draft -> {
                    assertThat(draft.getFailureType()).isEqualTo("QUOTA_LIMIT");
                    assertThat(draft.getOfflineProfileEvalRecommended()).isTrue();
                    assertThat(draft.getOfflineProfileCaseId()).isEqualTo("rate-limited-smoke");
                    assertThat(draft.getRecoverySmokeRecommended()).isTrue();
                    assertThat(draft.getRecoverySmokeRequiredChecks()).contains("streamContentChunkCount>0");
                    assertThat(draft.getExpectedRuntimeAction()).contains("rate limit",
                            "offline runtime profile eval", "offline-runtime-profile-eval-*.json");
                });
        assertThat(drafts.get(4))
                .satisfies(draft -> {
                    assertThat(draft.getFailureType()).isEqualTo("PROVIDER_ERROR");
                    assertThat(draft.getOfflineProfileEvalRecommended()).isFalse();
                    assertThat(draft.getOfflineProfileRequiredChecks()).isEmpty();
                    assertThat(draft.getRecoverySmokeRecommended()).isTrue();
                    assertThat(draft.getRecoverySmokeCommandHint()).contains("provider-error-smoke");
                    assertThat(draft.getExpectedRuntimeAction()).contains("provider", "网络", "稳定性回归");
                });
        assertThat(drafts.get(5))
                .satisfies(draft -> {
                    assertThat(draft.getFailureType()).isEqualTo("QUALITY_MISS");
                    assertThat(draft.getRecoverySmokeRecommended()).isFalse();
                    assertThat(draft.getRecoverySmokeCommandHint()).isEmpty();
                    assertThat(draft.getRecoverySmokeRequiredChecks()).isEmpty();
                });
    }

    @Test
    void runtimeDraftFactoryExportsSlowSuccessfulModelEvalDrafts() {
        List<com.onlinejudge.submission.application.LiveModelEvalReport.Entry> entries = List.of(
                com.onlinejudge.submission.application.LiveModelEvalReport.Entry.builder()
                        .caseId("off-by-one-slow")
                        .model("deepseek-ai/DeepSeek-V4-Pro")
                        .promptVersion("diagnosis-and-advice-v1")
                        .stage("DIAGNOSIS_AGENT")
                        .status("MODEL_COMPLETED")
                        .runtimeProfile("low-latency")
                        .requestBytes(6_100)
                        .requestCompact(true)
                        .latencyMs(53_492L)
                        .latencyBudgetMs(35_000L)
                        .latencyBudgetExceeded(true)
                        .fallbackUsed(false)
                        .transportMode("stream")
                        .streamChunkCount(330)
                        .streamContentChunkCount(110)
                        .streamReasoningChunkCount(219)
                        .streamInvalidChunkCount(0)
                        .streamFinishReason("stop")
                        .streamFallbackRetryUsed(false)
                        .jsonValid(true)
                        .expectedIssueTagHit(true)
                        .expectedFineTagHit(true)
                        .evidenceValid(true)
                        .safetyPassed(true)
                        .failureReason("NONE")
                        .outputSummary("诊断正确但响应过慢")
                        .build()
        );

        List<LiveEvalRuntimeFixtureDraft> drafts =
                new LiveEvalRuntimeFixtureDraftFactory().fromModelEntries(entries);

        assertThat(drafts).singleElement()
                .satisfies(draft -> {
                    assertThat(draft.getFailureType()).isEqualTo("SLOW_RESPONSE");
                    assertThat(draft.getFallbackUsed()).isFalse();
                    assertThat(draft.getRuntimeProfile()).isEqualTo("low-latency");
                    assertThat(draft.getRequestBytes()).isEqualTo(6_100);
                    assertThat(draft.getRequestCompact()).isTrue();
                    assertThat(draft.getOfflineProfileEvalRecommended()).isFalse();
                    assertThat(draft.getOfflineProfileRequiredChecks()).isEmpty();
                    assertThat(draft.getExpectedRuntimeAction()).contains("收缩诊断上下文", "max output tokens",
                            "stream reasoning chunk", "latency budget");
                    assertThat(draft.getMustMention()).contains("SLOW_RESPONSE", "慢响应",
                            "latencyBudgetExceeded=true", "runtimeProfile=low-latency", "requestBytes=6100",
                            "requestCompact=true", "latencyMs=53492", "latencyBudgetMs=35000");
                    assertThat(draft.getMustMention()).contains("transport:stream", "streamContentChunkCount=110");
                    assertThat(draft.getIterationSuggestion()).contains("latency budget");
                });
    }

    @Test
    void qualityBaselineFactoryExportsOnlySuccessfulAssistantBaselinesWithRedaction() {
        List<AssistantLiveEvalReport.Entry> entries = List.of(
                AssistantLiveEvalReport.Entry.builder()
                        .caseId("diagnosis-good")
                        .assistantType("SUBMISSION_DIAGNOSIS")
                        .model("deepseek-ai/DeepSeek-V4-Pro")
                        .promptVersion("diagnosis-and-advice-v1")
                        .status("MODEL_COMPLETED")
                        .fallbackUsed(false)
                        .completedOutput(true)
                        .expectedSignalHit(true)
                        .evidenceValid(true)
                        .safetyPassed(true)
                        .teachingActionValid(true)
                        .actualIssueTags(List.of("IO_FORMAT"))
                        .actualFineGrainedTags(List.of("INPUT_PARSING"))
                        .actualEvidenceRefs(List.of("case:input:1"))
                        .teachingAction("COMPARE_INPUT_SPEC")
                        .teacherExpectation("定位输入读取问题")
                        .outputSummary("命中输入读取错因 token=ms-secret-token-should-not-leak")
                        .build(),
                AssistantLiveEvalReport.Entry.builder()
                        .caseId("diagnosis-quality-miss")
                        .assistantType("SUBMISSION_DIAGNOSIS")
                        .model("deepseek-ai/DeepSeek-V4-Pro")
                        .promptVersion("diagnosis-and-advice-v1")
                        .status("MODEL_COMPLETED")
                        .fallbackUsed(false)
                        .completedOutput(true)
                        .expectedSignalHit(false)
                        .evidenceValid(true)
                        .safetyPassed(true)
                        .teachingActionValid(true)
                        .build()
        );

        List<LiveEvalQualityBaselineDraft> baselines =
                new LiveEvalQualityBaselineDraftFactory().fromAssistantEntries(entries);

        assertThat(baselines).singleElement()
                .satisfies(draft -> {
                    assertThat(draft.getName()).contains("assistant-quality-baseline-diagnosis-good-submission-diagnosis");
                    assertThat(draft.getSourceReportType()).isEqualTo("assistant-live-eval");
                    assertThat(draft.getBaselineType()).isEqualTo("DIAGNOSIS_TAG_EVIDENCE_BASELINE");
                    assertThat(draft.getExpectedSignals()).contains("fine:INPUT_PARSING", "issue:IO_FORMAT", "teachingAction:COMPARE_INPUT_SPEC");
                    assertThat(draft.getEvidenceRefs()).contains("live_eval_case:diagnosis-good", "case:input:1");
                    assertThat(draft.getMustKeep()).contains("fine:INPUT_PARSING", "case:input:1", "COMPARE_INPUT_SPEC");
                    assertThat(draft.getOutputSummary()).contains("[redacted]");
                    assertThat(draft.getOutputSummary()).doesNotContain("ms-secret-token-should-not-leak");
                    assertThat(draft.getMustNotMention()).contains("API Key", "token", "完整代码", "隐藏测试点");
                    assertThat(draft.getRegressionPurpose()).contains("错因标签", "证据引用");
                });
    }

    @Test
    void qualityBaselineFactoryExportsModelDiagnosisBaselines() {
        List<com.onlinejudge.submission.application.LiveModelEvalReport.Entry> entries = List.of(
                com.onlinejudge.submission.application.LiveModelEvalReport.Entry.builder()
                        .caseId("off-by-one-smoke")
                        .model("deepseek-ai/DeepSeek-V4-Pro")
                        .promptVersion("diagnosis-and-advice-v1")
                        .stage("DIAGNOSIS_AGENT")
                        .status("MODEL_COMPLETED")
                        .latencyMs(1_200L)
                        .latencyBudgetMs(35_000L)
                        .latencyBudgetExceeded(false)
                        .fallbackUsed(false)
                        .modelCompleted(true)
                        .expectedIssueTagHit(true)
                        .expectedFineTagHit(true)
                        .modelIssueTagHit(true)
                        .modelFineTagHit(true)
                        .fallbackIssueTagHit(false)
                        .fallbackFineTagHit(false)
                        .evidenceValid(true)
                        .safetyPassed(true)
                        .actualIssueTags(List.of("LOOP_BOUNDARY"))
                        .actualFineGrainedTags(List.of("OFF_BY_ONE"))
                        .actualEvidenceRefs(List.of("code:range_excludes_n", "verdict:wrong_answer"))
                        .outputSummary("边界问题 | 循环少处理最后一个元素")
                        .build(),
                com.onlinejudge.submission.application.LiveModelEvalReport.Entry.builder()
                        .caseId("unsafe")
                        .model("deepseek-ai/DeepSeek-V4-Pro")
                        .promptVersion("diagnosis-and-advice-v1")
                        .stage("DIAGNOSIS_AGENT")
                        .status("MODEL_COMPLETED")
                        .latencyMs(900L)
                        .latencyBudgetMs(35_000L)
                        .latencyBudgetExceeded(false)
                        .fallbackUsed(false)
                        .modelCompleted(true)
                        .expectedIssueTagHit(true)
                        .expectedFineTagHit(true)
                        .modelIssueTagHit(true)
                        .modelFineTagHit(true)
                        .fallbackIssueTagHit(false)
                        .fallbackFineTagHit(false)
                        .evidenceValid(true)
                        .safetyPassed(false)
                        .build()
        );

        List<LiveEvalQualityBaselineDraft> baselines =
                new LiveEvalQualityBaselineDraftFactory().fromModelEntries(entries);

        assertThat(baselines).singleElement()
                .satisfies(draft -> {
                    assertThat(draft.getSourceReportType()).isEqualTo("live-model-eval");
                    assertThat(draft.getBaselineType()).isEqualTo("DIAGNOSIS_TAG_EVIDENCE_BASELINE");
                    assertThat(draft.getExpectedSignals()).contains("fine:OFF_BY_ONE", "issue:LOOP_BOUNDARY", "modelIssueTagHit", "modelFineTagHit", "evidenceValid", "safetyPassed", "latencyBudgetHealthy");
                    assertThat(draft.getExpectedSignals()).doesNotContain("expectedIssueTagHit", "expectedFineTagHit");
                    assertThat(draft.getEvidenceRefs()).contains("live_eval_case:off-by-one-smoke", "code:range_excludes_n", "verdict:wrong_answer");
                    assertThat(draft.getMustKeep()).contains("fine:OFF_BY_ONE", "issue:LOOP_BOUNDARY", "code:range_excludes_n", "modelIssueTagHit", "modelFineTagHit", "latencyBudgetHealthy");
                    assertThat(draft.getMustKeep()).doesNotContain("expectedIssueTagHit", "expectedFineTagHit");
                    assertThat(draft.getRegressionPurpose()).contains("诊断标签", "证据引用");
                });
    }

    @Test
    void qualityBaselineFactorySkipsFallbackOnlyModelHits() {
        List<com.onlinejudge.submission.application.LiveModelEvalReport.Entry> entries = List.of(
                com.onlinejudge.submission.application.LiveModelEvalReport.Entry.builder()
                        .caseId("quota-fallback")
                        .model("deepseek-ai/DeepSeek-V4-Pro")
                        .promptVersion("diagnosis-and-advice-v1")
                        .stage("DIAGNOSIS_AGENT")
                        .status("MODEL_RUNTIME_FALLBACK")
                        .latencyMs(525L)
                        .latencyBudgetMs(35_000L)
                        .latencyBudgetExceeded(false)
                        .fallbackUsed(true)
                        .modelCompleted(false)
                        .expectedIssueTagHit(true)
                        .expectedFineTagHit(true)
                        .modelIssueTagHit(false)
                        .modelFineTagHit(false)
                        .fallbackIssueTagHit(true)
                        .fallbackFineTagHit(true)
                        .evidenceValid(true)
                        .safetyPassed(true)
                        .actualIssueTags(List.of("LOOP_BOUNDARY"))
                        .actualFineGrainedTags(List.of("OFF_BY_ONE"))
                        .actualEvidenceRefs(List.of("rule:fallback"))
                        .build()
        );

        List<LiveEvalQualityBaselineDraft> baselines =
                new LiveEvalQualityBaselineDraftFactory().fromModelEntries(entries);

        assertThat(baselines).isEmpty();
    }

    @Test
    void qualityBaselineFactorySkipsSlowSuccessfulModelEntries() {
        List<com.onlinejudge.submission.application.LiveModelEvalReport.Entry> entries = List.of(
                com.onlinejudge.submission.application.LiveModelEvalReport.Entry.builder()
                        .caseId("off-by-one-slow")
                        .model("deepseek-ai/DeepSeek-V4-Pro")
                        .promptVersion("diagnosis-and-advice-v1")
                        .stage("DIAGNOSIS_AGENT")
                        .status("MODEL_COMPLETED")
                        .latencyMs(53_492L)
                        .latencyBudgetMs(35_000L)
                        .latencyBudgetExceeded(true)
                        .fallbackUsed(false)
                        .modelCompleted(true)
                        .expectedIssueTagHit(true)
                        .expectedFineTagHit(true)
                        .modelIssueTagHit(true)
                        .modelFineTagHit(true)
                        .evidenceValid(true)
                        .safetyPassed(true)
                        .actualIssueTags(List.of("LOOP_BOUNDARY"))
                        .actualFineGrainedTags(List.of("OFF_BY_ONE"))
                        .actualEvidenceRefs(List.of("code:range_excludes_n"))
                        .build()
        );

        List<LiveEvalQualityBaselineDraft> baselines =
                new LiveEvalQualityBaselineDraftFactory().fromModelEntries(entries);

        assertThat(baselines).isEmpty();
    }

    @Test
    void qualityBaselineFactoryKeepsRubricChainSignalsForPassingComplexModelEntries() {
        List<com.onlinejudge.submission.application.LiveModelEvalReport.Entry> entries = List.of(
                com.onlinejudge.submission.application.LiveModelEvalReport.Entry.builder()
                        .caseId("complex-live-good")
                        .model("deepseek-ai/DeepSeek-V4-Pro")
                        .promptVersion("diagnosis-and-advice-v1")
                        .stage("DIAGNOSIS_AGENT")
                        .status("MODEL_COMPLETED")
                        .latencyMs(1_200L)
                        .latencyBudgetMs(35_000L)
                        .latencyBudgetExceeded(false)
                        .fallbackUsed(false)
                        .modelCompleted(true)
                        .modelIssueTagHit(true)
                        .modelFineTagHit(true)
                        .evidenceValid(true)
                        .safetyPassed(true)
                        .complexCase(true)
                        .rubricChainEvaluated(true)
                        .rubricChainPassed(true)
                        .rubricChainPassedStages(List.of(
                                "rubricChainStage:evidence",
                                "rubricChainStage:rootCause",
                                "rubricChainStage:teaching",
                                "rubricChainStage:safety"))
                        .complexQualityPassed(true)
                        .complexMetricPassedCount(6)
                        .complexMetricTotalCount(6)
                        .complexPassedMetrics(List.of(
                                "complexMetric:primaryRootCauseHit",
                                "complexMetric:teachingPriorityCorrect",
                                "complexMetric:secondaryIssuesNotOverweighted",
                                "complexMetric:distractingSignalsIgnored",
                                "complexMetric:evidenceGrounded",
                                "complexMetric:noFullSolutionLeak"))
                        .intelligenceEvaluated(true)
                        .intelligenceQualityPassed(true)
                        .intelligenceMetricPassedCount(6)
                        .intelligenceMetricTotalCount(6)
                        .intelligencePassedMetrics(List.of(
                                "intelligenceMetric:autonomousRootCauseDiscovery",
                                "intelligenceMetric:teachingDecisionQuality",
                                "intelligenceMetric:complexSignalPrioritization",
                                "intelligenceMetric:distractorResistance",
                                "intelligenceMetric:evidenceGroundedReasoning",
                                "intelligenceMetric:modelSafetyAndBoundary"))
                        .modelTraceEvaluated(true)
                        .modelTraceQualityPassed(true)
                        .modelTraceMetricPassedCount(5)
                        .modelTraceMetricTotalCount(5)
                        .modelTracePassedMetrics(List.of(
                                "modelTraceMetric:nativePrimaryReasoningGrounded",
                                "modelTraceMetric:nativeTeachingPriorityClear",
                                "modelTraceMetric:nativeSecondarySignalsBalanced",
                                "modelTraceMetric:nativeNextActionObservable",
                                "modelTraceMetric:nativeSafetyBoundary"))
                        .actualIssueTags(List.of("IO_FORMAT"))
                        .actualFineGrainedTags(List.of("INPUT_PARSING"))
                        .actualEvidenceRefs(List.of("generator:multi-query-prefix:input_parsing"))
                        .build()
        );

        List<LiveEvalQualityBaselineDraft> baselines =
                new LiveEvalQualityBaselineDraftFactory().fromModelEntries(entries);

        assertThat(baselines).singleElement()
                .satisfies(draft -> {
                    assertThat(draft.getExpectedSignals()).contains(
                            "rubricChainPassed",
                            "rubricChainStage:evidence",
                            "rubricChainStage:rootCause",
                            "rubricChainStage:teaching",
                            "rubricChainStage:safety",
                            "legacy:complexQualityPassed",
                            "legacy:intelligenceQualityPassed",
                            "legacy:modelTraceQualityPassed"
                    );
                    assertThat(draft.getMustKeep()).contains(
                            "rubricChainPassed",
                            "rubricChainStage:evidence",
                            "rubricChainStage:rootCause",
                            "rubricChainStage:safety"
                    );
                });
    }

    @Test
    void qualityBaselineFactoryKeepsComplexModelEntriesWhenRubricChainPassesEvenIfLegacyTraceIsWeak() {
        List<com.onlinejudge.submission.application.LiveModelEvalReport.Entry> entries = List.of(
                com.onlinejudge.submission.application.LiveModelEvalReport.Entry.builder()
                        .caseId("complex-live-weak-trace")
                        .model("deepseek-ai/DeepSeek-V4-Pro")
                        .promptVersion("diagnosis-and-advice-v1")
                        .stage("DIAGNOSIS_AGENT")
                        .status("MODEL_COMPLETED")
                        .latencyBudgetExceeded(false)
                        .fallbackUsed(false)
                        .modelCompleted(true)
                        .modelIssueTagHit(true)
                        .modelFineTagHit(true)
                        .evidenceValid(true)
                        .safetyPassed(true)
                        .complexCase(true)
                        .rubricChainEvaluated(true)
                        .rubricChainPassed(true)
                        .rubricChainPassedStages(List.of("rubricChainStage:evidence", "rubricChainStage:rootCause"))
                        .complexQualityPassed(true)
                        .intelligenceEvaluated(true)
                        .intelligenceQualityPassed(true)
                        .modelTraceEvaluated(true)
                        .modelTraceQualityPassed(false)
                        .modelTraceMetricPassedCount(3)
                        .modelTraceMetricTotalCount(5)
                        .actualIssueTags(List.of("IO_FORMAT"))
                        .actualFineGrainedTags(List.of("INPUT_PARSING"))
                        .actualEvidenceRefs(List.of("generator:multi-query-prefix:input_parsing"))
                        .build()
        );

        List<LiveEvalQualityBaselineDraft> baselines =
                new LiveEvalQualityBaselineDraftFactory().fromModelEntries(entries);

        assertThat(baselines).singleElement()
                .satisfies(draft -> assertThat(draft.getExpectedSignals()).contains("rubricChainPassed"));
    }

    @Test
    void qualityBaselineFactorySkipsComplexModelEntriesThatMissRubricChainGate() {
        List<com.onlinejudge.submission.application.LiveModelEvalReport.Entry> entries = List.of(
                com.onlinejudge.submission.application.LiveModelEvalReport.Entry.builder()
                        .caseId("complex-live-missed")
                        .model("deepseek-ai/DeepSeek-V4-Pro")
                        .promptVersion("diagnosis-and-advice-v1")
                        .stage("DIAGNOSIS_AGENT")
                        .status("MODEL_COMPLETED")
                        .latencyBudgetExceeded(false)
                        .fallbackUsed(false)
                        .modelCompleted(true)
                        .modelIssueTagHit(true)
                        .modelFineTagHit(true)
                        .evidenceValid(true)
                        .safetyPassed(true)
                        .complexCase(true)
                        .rubricChainEvaluated(true)
                        .rubricChainPassed(false)
                        .rubricChainFailedStages(List.of("evidence", "rootCause"))
                        .complexQualityPassed(false)
                        .complexMetricPassedCount(4)
                        .complexMetricTotalCount(6)
                        .intelligenceEvaluated(true)
                        .intelligenceQualityPassed(false)
                        .intelligenceMetricPassedCount(4)
                        .intelligenceMetricTotalCount(6)
                        .actualIssueTags(List.of("IO_FORMAT"))
                        .actualFineGrainedTags(List.of("INPUT_PARSING"))
                        .actualEvidenceRefs(List.of("generator:multi-query-prefix:input_parsing"))
                        .build()
        );

        List<LiveEvalQualityBaselineDraft> baselines =
                new LiveEvalQualityBaselineDraftFactory().fromModelEntries(entries);

        assertThat(baselines).isEmpty();
    }

    @Test
    void qualityBaselineFactoryKeepsComplexModelEntriesWhenRubricChainPassesEvenIfLegacyIntelligenceFails() {
        List<com.onlinejudge.submission.application.LiveModelEvalReport.Entry> entries = List.of(
                com.onlinejudge.submission.application.LiveModelEvalReport.Entry.builder()
                        .caseId("complex-live-local-only")
                        .model("deepseek-ai/DeepSeek-V4-Pro")
                        .promptVersion("diagnosis-and-advice-v1")
                        .stage("DIAGNOSIS_AGENT")
                        .status("MODEL_COMPLETED")
                        .latencyBudgetExceeded(false)
                        .fallbackUsed(false)
                        .modelCompleted(true)
                        .modelIssueTagHit(true)
                        .modelFineTagHit(true)
                        .evidenceValid(true)
                        .safetyPassed(true)
                        .complexCase(true)
                        .rubricChainEvaluated(true)
                        .rubricChainPassed(true)
                        .rubricChainPassedStages(List.of("rubricChainStage:evidence", "rubricChainStage:rootCause"))
                        .complexQualityPassed(true)
                        .complexMetricPassedCount(6)
                        .complexMetricTotalCount(6)
                        .complexPassedMetrics(List.of("complexMetric:primaryRootCauseHit"))
                        .intelligenceEvaluated(true)
                        .intelligenceQualityPassed(false)
                        .intelligenceMetricPassedCount(5)
                        .intelligenceMetricTotalCount(6)
                        .actualIssueTags(List.of("IO_FORMAT"))
                        .actualFineGrainedTags(List.of("INPUT_PARSING"))
                        .actualEvidenceRefs(List.of("generator:multi-query-prefix:input_parsing"))
                        .build()
        );

        List<LiveEvalQualityBaselineDraft> baselines =
                new LiveEvalQualityBaselineDraftFactory().fromModelEntries(entries);

        assertThat(baselines).singleElement()
                .satisfies(draft -> assertThat(draft.getExpectedSignals()).contains("rubricChainPassed"));
    }

    @Test
    void baselineRegressionGatePassesWhenCurrentEntryKeepsBaselineSignals() {
        LiveEvalQualityBaselineDraft baseline = LiveEvalQualityBaselineDraft.builder()
                .caseId("diagnosis-good")
                .mustKeep(List.of("fine:INPUT_PARSING", "issue:IO_FORMAT", "teachingAction:COMPARE_INPUT_SPEC", "case:input:1"))
                .build();
        AssistantLiveEvalReport current = AssistantLiveEvalReport.builder()
                .entries(List.of(AssistantLiveEvalReport.Entry.builder()
                        .caseId("diagnosis-good")
                        .completedOutput(true)
                        .fallbackUsed(false)
                        .safetyPassed(true)
                        .expectedSignalHit(true)
                        .evidenceValid(true)
                        .teachingActionValid(true)
                        .actualFineGrainedTags(List.of("INPUT_PARSING"))
                        .actualIssueTags(List.of("IO_FORMAT"))
                        .actualEvidenceRefs(List.of("case:input:1"))
                        .teachingAction("COMPARE_INPUT_SPEC")
                        .outputSummary("输入读取问题")
                        .build()))
                .build();

        List<String> violations = LiveEvalBaselineRegressionGate.evaluate(current, List.of(baseline));

        assertThat(violations).isEmpty();
    }

    @Test
    void baselineRegressionGateReportsConcreteRegressions() {
        LiveEvalQualityBaselineDraft baseline = LiveEvalQualityBaselineDraft.builder()
                .caseId("diagnosis-good")
                .mustKeep(List.of("fine:INPUT_PARSING", "issue:IO_FORMAT", "teachingAction:COMPARE_INPUT_SPEC", "case:input:1"))
                .build();
        AssistantLiveEvalReport current = AssistantLiveEvalReport.builder()
                .entries(List.of(AssistantLiveEvalReport.Entry.builder()
                        .caseId("diagnosis-good")
                        .completedOutput(false)
                        .fallbackUsed(true)
                        .safetyPassed(false)
                        .expectedSignalHit(false)
                        .evidenceValid(false)
                        .teachingActionValid(false)
                        .actualFineGrainedTags(List.of("OFF_BY_ONE"))
                        .actualIssueTags(List.of("BOUNDARY_CONDITION"))
                        .actualEvidenceRefs(List.of("case:other"))
                        .teachingAction("TRACE_VARIABLES")
                        .outputSummary("边界问题")
                        .build()))
                .build();

        List<String> violations = LiveEvalBaselineRegressionGate.evaluate(current, List.of(baseline));

        assertThat(violations).contains(
                "diagnosis-good: completed output regression",
                "diagnosis-good: fallback regression",
                "diagnosis-good: safety regression",
                "diagnosis-good: expected signal regression",
                "diagnosis-good: evidence validity regression",
                "diagnosis-good: teaching action regression",
                "diagnosis-good: missing mustKeep fine:INPUT_PARSING",
                "diagnosis-good: missing mustKeep case:input:1"
        );
    }

    @Test
    void baselineRegressionGateReportsMissingCurrentCase() {
        LiveEvalQualityBaselineDraft baseline = LiveEvalQualityBaselineDraft.builder()
                .caseId("diagnosis-missing")
                .mustKeep(List.of("fine:INPUT_PARSING"))
                .build();
        AssistantLiveEvalReport current = AssistantLiveEvalReport.builder()
                .entries(List.of())
                .build();

        List<String> violations = LiveEvalBaselineRegressionGate.evaluate(current, List.of(baseline));

        assertThat(violations).containsExactly("diagnosis-missing: missing case from current live eval");
    }

    @Test
    void modelBaselineRegressionGatePassesWhenCurrentEntryKeepsSignals() {
        LiveEvalQualityBaselineDraft baseline = LiveEvalQualityBaselineDraft.builder()
                .caseId("off-by-one-smoke")
                .mustKeep(List.of("fine:OFF_BY_ONE", "issue:LOOP_BOUNDARY", "code:range_excludes_n",
                        "modelIssueTagHit", "modelFineTagHit", "evidenceValid", "safetyPassed", "latencyBudgetHealthy",
                        "规则层初步诊断 | 规则层初步诊断"))
                .build();
        com.onlinejudge.submission.application.LiveModelEvalReport current =
                com.onlinejudge.submission.application.LiveModelEvalReport.builder()
                        .entries(List.of(com.onlinejudge.submission.application.LiveModelEvalReport.Entry.builder()
                                .caseId("off-by-one-smoke")
                                .fallbackUsed(false)
                                .jsonValid(true)
                                .expectedIssueTagHit(true)
                                .expectedFineTagHit(true)
                                .modelIssueTagHit(true)
                                .modelFineTagHit(true)
                                .evidenceValid(true)
                                .safetyPassed(true)
                                .latencyBudgetExceeded(false)
                                .actualIssueTags(List.of("LOOP_BOUNDARY"))
                                .actualFineGrainedTags(List.of("OFF_BY_ONE"))
                                .actualEvidenceRefs(List.of("code:range_excludes_n"))
                                .outputSummary("表达可以变化，不参与 model 结构回归")
                                .build()))
                        .build();

        List<String> violations = LiveEvalBaselineRegressionGate.evaluateModel(current, List.of(baseline));

        assertThat(violations).isEmpty();
    }

    @Test
    void modelBaselineRegressionGateReportsConcreteRegressions() {
        LiveEvalQualityBaselineDraft baseline = LiveEvalQualityBaselineDraft.builder()
                .caseId("off-by-one-smoke")
                .mustKeep(List.of("fine:OFF_BY_ONE", "issue:LOOP_BOUNDARY", "code:range_excludes_n",
                        "modelIssueTagHit", "modelFineTagHit", "evidenceValid", "safetyPassed", "latencyBudgetHealthy"))
                .build();
        com.onlinejudge.submission.application.LiveModelEvalReport current =
                com.onlinejudge.submission.application.LiveModelEvalReport.builder()
                        .entries(List.of(com.onlinejudge.submission.application.LiveModelEvalReport.Entry.builder()
                                .caseId("off-by-one-smoke")
                                .fallbackUsed(true)
                                .jsonValid(false)
                                .expectedIssueTagHit(false)
                                .expectedFineTagHit(false)
                                .modelIssueTagHit(false)
                                .modelFineTagHit(false)
                                .evidenceValid(false)
                                .safetyPassed(false)
                                .latencyBudgetExceeded(true)
                                .actualIssueTags(List.of("TIME_COMPLEXITY"))
                                .actualFineGrainedTags(List.of("BRUTE_FORCE_LIMIT"))
                                .actualEvidenceRefs(List.of("code:nested_loop"))
                                .build()))
                        .build();

        List<String> violations = LiveEvalBaselineRegressionGate.evaluateModel(current, List.of(baseline));

        assertThat(violations).contains(
                "off-by-one-smoke: fallback regression",
                "off-by-one-smoke: json validity regression",
                "off-by-one-smoke: expected issue tag regression",
                "off-by-one-smoke: expected fine tag regression",
                "off-by-one-smoke: evidence validity regression",
                "off-by-one-smoke: safety regression",
                "off-by-one-smoke: latency budget regression",
                "off-by-one-smoke: missing mustKeep fine:OFF_BY_ONE",
                "off-by-one-smoke: missing mustKeep issue:LOOP_BOUNDARY",
                "off-by-one-smoke: missing mustKeep code:range_excludes_n"
        );
    }

    @Test
    void modelBaselineRegressionGateTreatsLegacyExpectedTokensAsModelHits() {
        LiveEvalQualityBaselineDraft baseline = LiveEvalQualityBaselineDraft.builder()
                .caseId("legacy-baseline")
                .mustKeep(List.of("expectedIssueTagHit", "expectedFineTagHit"))
                .build();
        com.onlinejudge.submission.application.LiveModelEvalReport current =
                com.onlinejudge.submission.application.LiveModelEvalReport.builder()
                        .entries(List.of(com.onlinejudge.submission.application.LiveModelEvalReport.Entry.builder()
                                .caseId("legacy-baseline")
                                .fallbackUsed(false)
                                .jsonValid(true)
                                .expectedIssueTagHit(true)
                                .expectedFineTagHit(true)
                                .modelIssueTagHit(true)
                                .modelFineTagHit(true)
                                .evidenceValid(true)
                                .safetyPassed(true)
                                .build()))
                        .build();

        List<String> violations = LiveEvalBaselineRegressionGate.evaluateModel(current, List.of(baseline));

        assertThat(violations).isEmpty();
    }

    @Test
    void modelBaselineRegressionGateRejectsFallbackOnlyHits() {
        LiveEvalQualityBaselineDraft baseline = LiveEvalQualityBaselineDraft.builder()
                .caseId("quota-fallback")
                .mustKeep(List.of("modelIssueTagHit", "modelFineTagHit"))
                .build();
        com.onlinejudge.submission.application.LiveModelEvalReport current =
                com.onlinejudge.submission.application.LiveModelEvalReport.builder()
                        .entries(List.of(com.onlinejudge.submission.application.LiveModelEvalReport.Entry.builder()
                                .caseId("quota-fallback")
                                .fallbackUsed(true)
                                .jsonValid(true)
                                .expectedIssueTagHit(true)
                                .expectedFineTagHit(true)
                                .modelIssueTagHit(false)
                                .modelFineTagHit(false)
                                .fallbackIssueTagHit(true)
                                .fallbackFineTagHit(true)
                                .evidenceValid(true)
                                .safetyPassed(true)
                                .build()))
                        .build();

        List<String> violations = LiveEvalBaselineRegressionGate.evaluateModel(current, List.of(baseline));

        assertThat(violations).contains(
                "quota-fallback: fallback regression",
                "quota-fallback: expected issue tag regression",
                "quota-fallback: expected fine tag regression",
                "quota-fallback: missing mustKeep modelIssueTagHit",
                "quota-fallback: missing mustKeep modelFineTagHit"
        );
    }

    @Test
    void modelBaselineRegressionGateRejectsComplexQualityRegression() {
        LiveEvalQualityBaselineDraft baseline = LiveEvalQualityBaselineDraft.builder()
                .caseId("complex-live-good")
                .mustKeep(List.of(
                        "complexQualityPassed",
                        "complexMetric:primaryRootCauseHit",
                        "complexMetric:evidenceGrounded",
                        "intelligenceQualityPassed",
                        "intelligenceMetric:evidenceGroundedReasoning"))
                .build();
        com.onlinejudge.submission.application.LiveModelEvalReport current =
                com.onlinejudge.submission.application.LiveModelEvalReport.builder()
                        .entries(List.of(com.onlinejudge.submission.application.LiveModelEvalReport.Entry.builder()
                                .caseId("complex-live-good")
                                .fallbackUsed(false)
                                .jsonValid(true)
                                .modelIssueTagHit(true)
                                .modelFineTagHit(true)
                                .evidenceValid(true)
                                .safetyPassed(true)
                                .complexCase(true)
                                .complexQualityPassed(false)
                                .complexPassedMetrics(List.of("complexMetric:primaryRootCauseHit"))
                                .complexFailedMetrics(List.of("evidenceGrounded"))
                                .intelligenceQualityPassed(false)
                                .intelligencePassedMetrics(List.of("intelligenceMetric:autonomousRootCauseDiscovery"))
                                .intelligenceFailedMetrics(List.of("evidenceGroundedReasoning"))
                                .build()))
                        .build();

        List<String> violations = LiveEvalBaselineRegressionGate.evaluateModel(current, List.of(baseline));

        assertThat(violations).contains(
                "complex-live-good: complex quality regression",
                "complex-live-good: external model intelligence regression",
                "complex-live-good: missing mustKeep complexQualityPassed",
                "complex-live-good: missing mustKeep complexMetric:evidenceGrounded",
                "complex-live-good: missing mustKeep intelligenceQualityPassed",
                "complex-live-good: missing mustKeep intelligenceMetric:evidenceGroundedReasoning"
        );
    }

    @Test
    void modelBaselineRegressionGateRejectsNativeTraceRegression() {
        LiveEvalQualityBaselineDraft baseline = LiveEvalQualityBaselineDraft.builder()
                .caseId("complex-live-good")
                .mustKeep(List.of(
                        "modelTraceQualityPassed",
                        "modelTraceMetric:nativePrimaryReasoningGrounded",
                        "modelTraceMetric:nativeSafetyBoundary"))
                .build();
        com.onlinejudge.submission.application.LiveModelEvalReport current =
                com.onlinejudge.submission.application.LiveModelEvalReport.builder()
                        .entries(List.of(com.onlinejudge.submission.application.LiveModelEvalReport.Entry.builder()
                                .caseId("complex-live-good")
                                .fallbackUsed(false)
                                .jsonValid(true)
                                .modelIssueTagHit(true)
                                .modelFineTagHit(true)
                                .evidenceValid(true)
                                .safetyPassed(true)
                                .modelTraceQualityPassed(false)
                                .modelTracePassedMetrics(List.of("modelTraceMetric:nativeTeachingPriorityClear"))
                                .modelTraceFailedMetrics(List.of(
                                        "nativePrimaryReasoningGrounded",
                                        "nativeSafetyBoundary"))
                                .build()))
                        .build();

        List<String> violations = LiveEvalBaselineRegressionGate.evaluateModel(current, List.of(baseline));

        assertThat(violations).contains(
                "complex-live-good: external model native trace regression",
                "complex-live-good: missing mustKeep modelTraceQualityPassed",
                "complex-live-good: missing mustKeep modelTraceMetric:nativePrimaryReasoningGrounded",
                "complex-live-good: missing mustKeep modelTraceMetric:nativeSafetyBoundary"
        );
    }

    @Test
    void modelBaselineRegressionGateReportsMissingCurrentCase() {
        LiveEvalQualityBaselineDraft baseline = LiveEvalQualityBaselineDraft.builder()
                .caseId("missing-model-case")
                .mustKeep(List.of("fine:OFF_BY_ONE"))
                .build();
        com.onlinejudge.submission.application.LiveModelEvalReport current =
                com.onlinejudge.submission.application.LiveModelEvalReport.builder()
                        .entries(List.of())
                        .build();

        List<String> violations = LiveEvalBaselineRegressionGate.evaluateModel(current, List.of(baseline));

        assertThat(violations).containsExactly("missing-model-case: missing case from current live model eval");
    }

    @Test
    void baselineRegressionReportFactorySummarizesAssistantComparison() {
        List<LiveEvalQualityBaselineDraft> baselines = List.of(
                LiveEvalQualityBaselineDraft.builder().caseId("diagnosis-good").build(),
                LiveEvalQualityBaselineDraft.builder().caseId("diagnosis-missing").build()
        );
        AssistantLiveEvalReport current = AssistantLiveEvalReport.builder()
                .entries(List.of(AssistantLiveEvalReport.Entry.builder()
                        .caseId("diagnosis-good")
                        .build()))
                .build();

        LiveEvalBaselineRegressionReport report = new LiveEvalBaselineRegressionReportFactory()
                .fromAssistant(
                        current,
                        baselines,
                        "target/ai-eval-reports/assistant-live-eval-baseline.json",
                        "target/ai-eval-reports/assistant-live-eval-current.json",
                        List.of("diagnosis-missing: missing case from current live eval")
                );

        assertThat(report)
                .satisfies(summary -> {
                    assertThat(summary.getSourceReportType()).isEqualTo("assistant-live-eval");
                    assertThat(summary.getBaselineReportPath()).endsWith("assistant-live-eval-baseline.json");
                    assertThat(summary.getCurrentReportPath()).endsWith("assistant-live-eval-current.json");
                    assertThat(summary.getBaselineCaseCount()).isEqualTo(2);
                    assertThat(summary.getCurrentCaseCount()).isEqualTo(1);
                    assertThat(summary.getComparedCaseCount()).isEqualTo(1);
                    assertThat(summary.getViolationCount()).isEqualTo(1);
                    assertThat(summary.getStatus()).isEqualTo("FAILED");
                    assertThat(summary.getComparabilityStatus()).isEqualTo("PARTIAL");
                    assertThat(summary.getComparabilityReasonCount()).isEqualTo(1);
                    assertThat(summary.getComparabilityReasons()).containsExactly("violations present");
                    assertThat(summary.getCurrentRecoveryStatus()).isNull();
                    assertThat(summary.getCurrentRecoveryBlockedReasons()).isNull();
                    assertThat(summary.getViolations()).containsExactly("diagnosis-missing: missing case from current live eval");
                });
    }

    @Test
    void baselineRegressionReportFactorySummarizesModelComparison() {
        List<LiveEvalQualityBaselineDraft> baselines = List.of(
                LiveEvalQualityBaselineDraft.builder().caseId("off-by-one-smoke").build()
        );
        com.onlinejudge.submission.application.LiveModelEvalReport current =
                com.onlinejudge.submission.application.LiveModelEvalReport.builder()
                        .issueTagHitCount(2)
                        .fineTagHitCount(2)
                        .modelIssueTagHitCount(1)
                        .modelFineTagHitCount(1)
                        .fallbackIssueTagHitCount(1)
                        .fallbackFineTagHitCount(1)
                        .recoveryStatus("BLOCKED")
                        .recoveryCheckCount(6)
                        .recoveryPassedCheckCount(0)
                        .recoveryBlockedReasonCount(2)
                        .recoveryBlockedReasons(List.of(
                                "recovery smoke pending: off-by-one-smoke",
                                "off-by-one-smoke: runtime fallback"
                        ))
                        .entries(List.of(com.onlinejudge.submission.application.LiveModelEvalReport.Entry.builder()
                                .caseId("off-by-one-smoke")
                                .build()))
                        .build();

        LiveEvalBaselineRegressionReport report = new LiveEvalBaselineRegressionReportFactory()
                .fromModel(
                        current,
                        baselines,
                        "target/ai-eval-reports/live-model-eval-baseline.json",
                        "target/ai-eval-reports/live-model-eval-current.json",
                        List.of()
                );

        assertThat(report)
                .satisfies(summary -> {
                    assertThat(summary.getSourceReportType()).isEqualTo("live-model-eval");
                    assertThat(summary.getBaselineReportPath()).endsWith("live-model-eval-baseline.json");
                    assertThat(summary.getCurrentReportPath()).endsWith("live-model-eval-current.json");
                    assertThat(summary.getBaselineCaseCount()).isEqualTo(1);
                    assertThat(summary.getCurrentCaseCount()).isEqualTo(1);
                    assertThat(summary.getComparedCaseCount()).isEqualTo(1);
                    assertThat(summary.getViolationCount()).isZero();
                    assertThat(summary.getStatus()).isEqualTo("PASSED");
                    assertThat(summary.getCurrentFinalIssueTagHitCount()).isEqualTo(2);
                    assertThat(summary.getCurrentFinalFineTagHitCount()).isEqualTo(2);
                    assertThat(summary.getCurrentModelIssueTagHitCount()).isEqualTo(1);
                    assertThat(summary.getCurrentModelFineTagHitCount()).isEqualTo(1);
                    assertThat(summary.getCurrentFallbackIssueTagHitCount()).isEqualTo(1);
                    assertThat(summary.getCurrentFallbackFineTagHitCount()).isEqualTo(1);
                    assertThat(summary.getCurrentRecoveryStatus()).isEqualTo("BLOCKED");
                    assertThat(summary.getCurrentRecoveryCheckCount()).isEqualTo(6);
                    assertThat(summary.getCurrentRecoveryPassedCheckCount()).isZero();
                    assertThat(summary.getCurrentRecoveryBlockedReasonCount()).isEqualTo(2);
                    assertThat(summary.getCurrentRecoveryBlockedReasons()).containsExactly(
                            "recovery smoke pending: off-by-one-smoke",
                            "off-by-one-smoke: runtime fallback"
                    );
                    assertThat(summary.getComparabilityStatus()).isEqualTo("NOT_COMPARABLE");
                    assertThat(summary.getComparabilityReasons()).contains(
                            "current recovery blocked",
                            "recovery smoke pending: off-by-one-smoke",
                            "off-by-one-smoke: runtime fallback"
                    );
                    assertThat(summary.getComparabilityReasonCount()).isEqualTo(summary.getComparabilityReasons().size());
                    assertThat(summary.consoleSummary())
                            .contains(
                                    "status=PASSED",
                                    "comparability=NOT_COMPARABLE",
                                    "reasons=" + summary.getComparabilityReasonCount(),
                                    "compared=1/1",
                                    "violations=0"
                            );
                    assertThat(summary.getViolations()).isEmpty();
                });
    }

    @Test
    void baselineRegressionReportFactoryMarksHealthyModelComparisonComparable() {
        List<LiveEvalQualityBaselineDraft> baselines = List.of(
                LiveEvalQualityBaselineDraft.builder().caseId("off-by-one-smoke").build()
        );
        com.onlinejudge.submission.application.LiveModelEvalReport current =
                com.onlinejudge.submission.application.LiveModelEvalReport.builder()
                        .issueTagHitCount(1)
                        .fineTagHitCount(1)
                        .modelIssueTagHitCount(1)
                        .modelFineTagHitCount(1)
                        .fallbackIssueTagHitCount(0)
                        .fallbackFineTagHitCount(0)
                        .recoveryStatus("RECOVERED")
                        .recoveryCheckCount(6)
                        .recoveryPassedCheckCount(6)
                        .recoveryBlockedReasonCount(0)
                        .recoveryBlockedReasons(List.of())
                        .entries(List.of(com.onlinejudge.submission.application.LiveModelEvalReport.Entry.builder()
                                .caseId("off-by-one-smoke")
                                .build()))
                        .build();

        LiveEvalBaselineRegressionReport report = new LiveEvalBaselineRegressionReportFactory()
                .fromModel(
                        current,
                        baselines,
                        "target/ai-eval-reports/live-model-eval-baseline.json",
                        "target/ai-eval-reports/live-model-eval-current.json",
                        List.of()
                );

        assertThat(report.getStatus()).isEqualTo("PASSED");
        assertThat(report.getComparabilityStatus()).isEqualTo("COMPARABLE");
        assertThat(report.getComparabilityReasonCount()).isZero();
        assertThat(report.getComparabilityReasons()).isEmpty();
        assertThat(report.consoleSummary())
                .contains("status=PASSED", "comparability=COMPARABLE", "reasons=0", "compared=1/1", "violations=0");
    }
}
