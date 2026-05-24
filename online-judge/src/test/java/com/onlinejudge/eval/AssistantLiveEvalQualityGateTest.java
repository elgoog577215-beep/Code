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
                .runtimeFailureCount(1)
                .build();

        List<String> violations = AssistantLiveEvalQualityGate.evaluate(
                report,
                new AssistantLiveEvalQualityGate.Thresholds(0.7, 0.8, 1.0, 0.3)
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
                .build();

        List<String> violations = AssistantLiveEvalQualityGate.evaluate(
                report,
                new AssistantLiveEvalQualityGate.Thresholds(0.7, 0.8, 1.0, 0.3)
        );

        assertThat(violations).containsExactly(
                "signalHitRate 0.25 < 0.70",
                "evidenceValidRate 0.50 < 0.80",
                "safetyPassRate 0.75 < 1.00",
                "fallbackRate 0.75 > 0.30"
        );
    }
}
