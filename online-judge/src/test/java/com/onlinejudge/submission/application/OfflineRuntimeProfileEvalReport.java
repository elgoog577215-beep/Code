package com.onlinejudge.submission.application;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfflineRuntimeProfileEvalReport {

    private String reportType;
    private Integer totalCount;
    private Integer reducedCount;
    private Integer qualityPreservedCount;
    private Double averageCompressionRatio;
    private List<Entry> entries;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Entry {
        private String caseId;
        private String promptVersion;
        private Integer standardRequestBytes;
        private Integer lowLatencyRequestBytes;
        private Boolean requestBytesReduced;
        private Double compressionRatio;
        private String lowLatencyRuntimeProfile;
        private Boolean lowLatencyRequestCompact;
        private Integer candidateSignalCount;
        private Integer evidenceRefCount;
        private Integer issueTagCount;
        private Integer fineTagCount;
        private Integer teachingActionCount;
        private Boolean hiddenBoundaryPresent;
        private Boolean qualityPreserved;
        private List<String> failureReasons;
    }
}
