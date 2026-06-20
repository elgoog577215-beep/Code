package com.onlinejudge.submission.application;

import lombok.Builder;

@Builder
public record SearchLocationResult(
        boolean enabled,
        String status,
        String embeddingStatus,
        String fallbackReason,
        int candidateCount,
        int selectedCount,
        SearchLocationCandidatePack candidatePack,
        SearchLocationOutput output,
        StandardLibraryPack selectedPack
) {
    public static SearchLocationResult disabled() {
        return SearchLocationResult.builder()
                .enabled(false)
                .status("DISABLED")
                .embeddingStatus("DISABLED")
                .fallbackReason("")
                .build();
    }

    public static SearchLocationResult fallback(String status, String embeddingStatus, String reason,
                                                SearchLocationCandidatePack candidatePack) {
        return SearchLocationResult.builder()
                .enabled(true)
                .status(status == null || status.isBlank() ? "FALLBACK_USED" : status)
                .embeddingStatus(embeddingStatus == null || embeddingStatus.isBlank() ? "UNKNOWN" : embeddingStatus)
                .fallbackReason(reason == null ? "" : reason)
                .candidateCount(candidatePack == null ? 0 : candidatePack.getCandidateCount())
                .candidatePack(candidatePack)
                .build();
    }
}
