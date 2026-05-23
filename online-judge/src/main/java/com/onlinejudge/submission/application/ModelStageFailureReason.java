package com.onlinejudge.submission.application;

public enum ModelStageFailureReason {
    NONE,
    TIMEOUT,
    INSUFFICIENT_QUOTA,
    RATE_LIMITED,
    MODEL_UNSUPPORTED,
    EMPTY_RESPONSE,
    INVALID_JSON,
    INVALID_TAG,
    INVALID_EVIDENCE_REF,
    SAFETY_RISK,
    API_ERROR,
    UNKNOWN_ERROR
}
