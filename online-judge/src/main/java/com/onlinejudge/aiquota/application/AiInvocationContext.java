package com.onlinejudge.aiquota.application;

import java.util.UUID;

public record AiInvocationContext(
        UUID teacherId,
        Long studentProfileId,
        Long assignmentId,
        Long submissionId,
        String purpose,
        String idempotencyKey
) {
    public static AiInvocationContext anonymous(String purpose, String idempotencyKey) {
        return new AiInvocationContext(null, null, null, null, purpose, idempotencyKey);
    }
}
