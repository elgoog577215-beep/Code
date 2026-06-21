package com.onlinejudge.submission.application;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public final class ExternalModelStagePayloads {

    private ExternalModelStagePayloads() {
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StageValidationResult {
        private boolean valid;
        private String stage;
        private ModelStageFailureReason failureReason;
        private String message;
    }
}
