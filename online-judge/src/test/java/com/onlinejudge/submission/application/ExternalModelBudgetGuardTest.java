package com.onlinejudge.submission.application;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class ExternalModelBudgetGuardTest {

    @Test
    void opensAfterQuotaFailureAndClearsOnSuccess() {
        MutableClock clock = new MutableClock(Instant.parse("2026-05-24T06:00:00Z"));
        ExternalModelBudgetGuard guard = new ExternalModelBudgetGuard(clock);
        ReflectionTestUtils.setField(guard, "cooldownSeconds", 60L);

        assertThat(guard.check("ModelScope", "deepseek").allowed()).isTrue();

        guard.recordFailure("ModelScope", "deepseek", ModelStageFailureReason.INSUFFICIENT_QUOTA);

        ExternalModelBudgetGuard.Decision blocked = guard.check("ModelScope", "deepseek");
        assertThat(blocked.allowed()).isFalse();
        assertThat(blocked.reason()).isEqualTo(ModelStageFailureReason.INSUFFICIENT_QUOTA);
        assertThat(blocked.message()).isEqualTo("BUDGET_GUARD_OPEN:INSUFFICIENT_QUOTA");

        guard.recordSuccess("ModelScope", "deepseek");

        assertThat(guard.check("ModelScope", "deepseek").allowed()).isTrue();
    }

    @Test
    void expiresAfterCooldown() {
        MutableClock clock = new MutableClock(Instant.parse("2026-05-24T06:00:00Z"));
        ExternalModelBudgetGuard guard = new ExternalModelBudgetGuard(clock);
        ReflectionTestUtils.setField(guard, "cooldownSeconds", 10L);

        guard.recordFailure("ModelScope", "deepseek", ModelStageFailureReason.RATE_LIMITED);
        assertThat(guard.check("ModelScope", "deepseek").allowed()).isFalse();

        clock.advance(Duration.ofSeconds(11));

        assertThat(guard.check("ModelScope", "deepseek").allowed()).isTrue();
    }

    @Test
    void ignoresNonBudgetFailures() {
        ExternalModelBudgetGuard guard = new ExternalModelBudgetGuard();

        guard.recordFailure("ModelScope", "deepseek", ModelStageFailureReason.TIMEOUT);

        assertThat(guard.check("ModelScope", "deepseek").allowed()).isTrue();
    }

    private static class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
