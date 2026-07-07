package com.onlinejudge.submission.application;

import lombok.Builder;

@Builder
public record SearchLocationResult(
        boolean enabled,
        String status,
        String embeddingStatus,
        String failureReason,
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
                .failureReason("")
                .build();
    }

    public static SearchLocationResult localOnly(String embeddingStatus,
                                                 SearchLocationCandidatePack candidatePack,
                                                 StandardLibraryPack selectedPack) {
        return SearchLocationResult.builder()
                .enabled(false)
                .status("LOCAL_RECALL")
                .embeddingStatus(embeddingStatus == null || embeddingStatus.isBlank() ? "DISABLED" : embeddingStatus)
                .failureReason("")
                .candidateCount(candidatePack == null ? 0 : candidatePack.getCandidateCount())
                .selectedCount(0)
                .candidatePack(candidatePack)
                .selectedPack(selectedPack)
                .build();
    }

    public static SearchLocationResult failed(String status, String embeddingStatus, String reason,
                                              SearchLocationCandidatePack candidatePack) {
        return SearchLocationResult.builder()
                .enabled(true)
                .status(status == null || status.isBlank() ? "FAILED" : status)
                .embeddingStatus(embeddingStatus == null || embeddingStatus.isBlank() ? "UNKNOWN" : embeddingStatus)
                .failureReason(reason == null ? "" : reason)
                .candidateCount(candidatePack == null ? 0 : candidatePack.getCandidateCount())
                .candidatePack(candidatePack)
                .build();
    }
}
