package com.onlinejudge.submission.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ExternalModelBudgetGuard {

    private final Map<String, GuardState> states = new ConcurrentHashMap<>();
    private final Clock clock;

    @Value("${ai.budget-guard.enabled:true}")
    private boolean enabled = true;

    @Value("${ai.budget-guard.cooldown-seconds:60}")
    private long cooldownSeconds = 60;

    public ExternalModelBudgetGuard() {
        this(Clock.systemUTC());
    }

    ExternalModelBudgetGuard(Clock clock) {
        this.clock = clock;
    }

    public Decision check(String provider, String model) {
        if (!enabled) {
            return Decision.allow();
        }
        GuardState state = states.get(key(provider, model));
        if (state == null) {
            return Decision.allow();
        }
        Instant now = clock.instant();
        Duration cooldown = Duration.ofSeconds(Math.max(0, cooldownSeconds));
        if (cooldown.isZero() || !now.isBefore(state.recordedAt().plus(cooldown))) {
            states.remove(key(provider, model), state);
            return Decision.allow();
        }
        return Decision.block(state.reason(), "BUDGET_GUARD_OPEN:" + state.reason().name());
    }

    public void recordFailure(String provider, String model, ModelStageFailureReason reason) {
        if (!enabled || !opensGuard(reason)) {
            return;
        }
        states.put(key(provider, model), new GuardState(reason, clock.instant()));
    }

    public void recordSuccess(String provider, String model) {
        states.remove(key(provider, model));
    }

    public void clear() {
        states.clear();
    }

    private boolean opensGuard(ModelStageFailureReason reason) {
        return reason == ModelStageFailureReason.INSUFFICIENT_QUOTA
                || reason == ModelStageFailureReason.RATE_LIMITED;
    }

    private String key(String provider, String model) {
        return safe(provider) + "|" + safe(model);
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "unknown" : value.trim();
    }

    private record GuardState(ModelStageFailureReason reason, Instant recordedAt) {
    }

    public record Decision(boolean allowed, ModelStageFailureReason reason, String message) {

        static Decision allow() {
            return new Decision(true, ModelStageFailureReason.NONE, "");
        }

        static Decision block(ModelStageFailureReason reason, String message) {
            return new Decision(false, reason, message);
        }
    }
}
