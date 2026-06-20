package com.onlinejudge.submission.application;

import lombok.Builder;

@Builder
public record AdviceGenerationResult(
        boolean enabled,
        String status,
        String fallbackReason,
        int basicAdviceCount,
        int improvementAdviceCount,
        String promptVersion,
        AdviceGenerationOutput output
) {
    public static AdviceGenerationResult disabled() {
        return AdviceGenerationResult.builder()
                .enabled(false)
                .status("DISABLED")
                .fallbackReason("")
                .promptVersion("")
                .build();
    }

    public static AdviceGenerationResult fallback(String reason, String promptVersion) {
        return AdviceGenerationResult.builder()
                .enabled(true)
                .status("FALLBACK_USED")
                .fallbackReason(reason == null ? "" : reason)
                .promptVersion(promptVersion == null ? "" : promptVersion)
                .build();
    }

    public static AdviceGenerationResult success(AdviceGenerationOutput output, String promptVersion) {
        return AdviceGenerationResult.builder()
                .enabled(true)
                .status("SUCCESS")
                .fallbackReason("")
                .basicAdviceCount(output == null || output.getBasicLayerAdvice() == null
                        ? 0
                        : output.getBasicLayerAdvice().size())
                .improvementAdviceCount(output == null || output.getImprovementLayerAdvice() == null
                        ? 0
                        : output.getImprovementLayerAdvice().size())
                .promptVersion(promptVersion == null ? "" : promptVersion)
                .output(output)
                .build();
    }
}
